// Supabase Edge Function: admin-approve-payment-
// Deploy with: supabase functions deploy admin-approve-payment-
// Required Edge Function Secrets:
//   SUPABASE_URL, SUPABASE_ANON_KEY, SUPABASE_SERVICE_ROLE_KEY, ADMIN_ALLOWED_ORIGIN

import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const IST_OFFSET_MS = (5 * 60 + 30) * 60 * 1000;

function getCorsHeaders(req: Request): Record<string, string> {
  const configuredOrigin = (Deno.env.get("ADMIN_ALLOWED_ORIGIN") ?? "").trim();
  const requestOrigin = (req.headers.get("origin") ?? "").trim();
  const allowedOrigin = configuredOrigin && requestOrigin === configuredOrigin
    ? configuredOrigin
    : configuredOrigin || "";

  const headers: Record<string, string> = {
    "Access-Control-Allow-Methods": "POST, OPTIONS",
    "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
    "Vary": "Origin",
  };
  if (allowedOrigin) {
    headers["Access-Control-Allow-Origin"] = allowedOrigin;
  }
  return headers;
}

function daysInMonthUtc(year: number, monthIndexZeroBased: number): number {
  return new Date(Date.UTC(year, monthIndexZeroBased + 1, 0)).getUTCDate();
}

/**
 * Adds 1 calendar month or 1 calendar year in Asia/Kolkata (+05:30),
 * preserving wall-clock time and clamping month-end dates (e.g., 31 Jan -> 28/29 Feb).
 */
function addCalendarPeriodIst(baseUtcMs: number, planType: "monthly" | "yearly"): string {
  const istShifted = new Date(baseUtcMs + IST_OFFSET_MS);
  const year = istShifted.getUTCFullYear();
  const month = istShifted.getUTCMonth();
  const day = istShifted.getUTCDate();
  const hours = istShifted.getUTCHours();
  const minutes = istShifted.getUTCMinutes();
  const seconds = istShifted.getUTCSeconds();
  const ms = istShifted.getUTCMilliseconds();

  const targetYear = planType === "yearly" ? year + 1 : year + Math.floor((month + 1) / 12);
  const targetMonth = planType === "yearly" ? month : (month + 1) % 12;
  const clampedDay = Math.min(day, daysInMonthUtc(targetYear, targetMonth));

  const targetIstMs = Date.UTC(targetYear, targetMonth, clampedDay, hours, minutes, seconds, ms);
  return new Date(targetIstMs - IST_OFFSET_MS).toISOString();
}

Deno.serve(async (req: Request) => {
  const corsHeaders = getCorsHeaders(req);
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }
  if (req.method !== "POST") {
    return new Response(JSON.stringify({ error: "Method not allowed" }), {
      status: 405,
      headers: { ...corsHeaders, "Content-Type": "application/json" },
    });
  }

  try {
    const supabaseUrl = Deno.env.get("SUPABASE_URL") ?? "";
    const anonKey = Deno.env.get("SUPABASE_ANON_KEY") ?? "";
    const serviceRoleKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? "";
    if (!supabaseUrl || !anonKey || !serviceRoleKey) {
      return new Response(JSON.stringify({ error: "Server configuration missing" }), {
        status: 500,
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      });
    }

    const authHeader = req.headers.get("Authorization") ?? "";
    if (!authHeader.startsWith("Bearer ")) {
      return new Response(JSON.stringify({ error: "Unauthorized" }), {
        status: 401,
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      });
    }

    const userClient = createClient(supabaseUrl, anonKey, {
      global: { headers: { Authorization: authHeader } },
    });
    const { data: userData, error: userErr } = await userClient.auth.getUser();
    if (userErr || !userData?.user) {
      return new Response(JSON.stringify({ error: "Invalid session" }), {
        status: 401,
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      });
    }

    const adminClient = createClient(supabaseUrl, serviceRoleKey);

    // Verify caller has admin privilege via admin_profiles table
    const { data: adminProfile } = await adminClient
      .from("admin_profiles")
      .select("user_id, is_active")
      .eq("user_id", userData.user.id)
      .maybeSingle();

    if (!adminProfile || adminProfile.is_active === false) {
      return new Response(JSON.stringify({ error: "Forbidden: admin access required" }), {
        status: 403,
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      });
    }

    const body = await req.json();
    const paymentRequestId = String(body?.payment_request_id ?? "").trim();
    const action = String(body?.action ?? "approve").trim().toLowerCase();
    if (!paymentRequestId || !["approve", "reject"].includes(action)) {
      return new Response(JSON.stringify({ error: "Invalid payment_request_id or action" }), {
        status: 400,
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      });
    }

    if (action === "reject") {
      const { data: rejected, error: rejectErr } = await adminClient
        .from("payment_requests")
        .update({
          status: "rejected",
          reviewed_at: new Date().toISOString(),
          reviewed_by: userData.user.id,
        })
        .eq("id", paymentRequestId)
        .eq("status", "pending")
        .select("*")
        .maybeSingle();

      if (rejectErr) throw rejectErr;
      if (!rejected) {
        return new Response(JSON.stringify({ error: "Payment request already processed or not found" }), {
          status: 409,
          headers: { ...corsHeaders, "Content-Type": "application/json" },
        });
      }

      return new Response(JSON.stringify({ success: true, status: "rejected", payment_request: rejected }), {
        status: 200,
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      });
    }

    // Fetch target request first to validate plan & amount before claiming
    const { data: reqRow, error: fetchErr } = await adminClient
      .from("payment_requests")
      .select("*")
      .eq("id", paymentRequestId)
      .maybeSingle();

    if (fetchErr) throw fetchErr;
    if (!reqRow) {
      return new Response(JSON.stringify({ error: "Payment request not found" }), {
        status: 404,
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      });
    }
    if (reqRow.status !== "pending") {
      return new Response(JSON.stringify({ error: "Payment request already processed" }), {
        status: 409,
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      });
    }

    const planType = String(reqRow.plan_type ?? "").trim().toLowerCase();
    const amountInr = Number(reqRow.amount_inr ?? 0);
    const isValidPlanAmount =
      (planType === "monthly" && amountInr === 499) ||
      (planType === "yearly" && amountInr === 1500);

    if (!isValidPlanAmount) {
      return new Response(
        JSON.stringify({
          error: "Only Monthly (₹499) and Yearly (₹1,500) plans are valid for approval.",
        }),
        { status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" } },
      );
    }

    // Guard against duplicate license activation for the same payment_request_id
    const { data: existingForPayment } = await adminClient
      .from("licenses")
      .select("id")
      .eq("payment_request_id", paymentRequestId)
      .maybeSingle();

    if (existingForPayment) {
      return new Response(JSON.stringify({ error: "License already activated for this payment request" }), {
        status: 409,
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      });
    }

    const serverNowMs = Date.now();
    const serverNowIso = new Date(serverNowMs).toISOString();

    // Atomic state transition from pending -> approved prevents double-approval races
    const { data: approvedReq, error: approveErr } = await adminClient
      .from("payment_requests")
      .update({
        status: "approved",
        reviewed_at: serverNowIso,
        reviewed_by: userData.user.id,
      })
      .eq("id", paymentRequestId)
      .eq("status", "pending")
      .select("*")
      .maybeSingle();

    if (approveErr) throw approveErr;
    if (!approvedReq) {
      return new Response(JSON.stringify({ error: "Payment request was already processed" }), {
        status: 409,
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      });
    }

    // Inspect existing active licenses for this user
    const { data: existingLicenses } = await adminClient
      .from("licenses")
      .select("*")
      .eq("user_id", approvedReq.user_id)
      .eq("status", "active")
      .order("expires_at", { ascending: false, nullsFirst: false });

    const legacyLifetime = (existingLicenses ?? []).find(
      (row) =>
        row.expires_at == null ||
        String(row.license_type ?? "").toLowerCase() === "lifetime" ||
        String(row.plan_type ?? "").toLowerCase() === "lifetime",
    );

    if (legacyLifetime) {
      // Never downgrade or expire legacy lifetime users
      return new Response(
        JSON.stringify({
          success: true,
          preserved_legacy_lifetime: true,
          license: legacyLifetime,
          payment_request: approvedReq,
        }),
        { status: 200, headers: { ...corsHeaders, "Content-Type": "application/json" } },
      );
    }

    // Renewal base: extend from highest unexpired active timed license, or serverNow if none/expired
    let baseTimestampMs = serverNowMs;
    const activeTimedIdsToSupersede: string[] = [];
    for (const row of existingLicenses ?? []) {
      const expMs = row.expires_at ? Date.parse(row.expires_at) : NaN;
      if (Number.isFinite(expMs) && expMs > baseTimestampMs) {
        baseTimestampMs = expMs;
      }
      if (row.id) activeTimedIdsToSupersede.push(row.id);
    }

    const newExpiresAtIso = addCalendarPeriodIst(baseTimestampMs, planType as "monthly" | "yearly");

    // Supersede old active timed rows so user's current active plan reflects the latest extended record
    if (activeTimedIdsToSupersede.length > 0) {
      await adminClient
        .from("licenses")
        .update({ status: "expired" })
        .in("id", activeTimedIdsToSupersede);
    }

    const { data: insertedLicense, error: insertLicErr } = await adminClient
      .from("licenses")
      .insert({
        user_id: approvedReq.user_id,
        plan_type: planType,
        license_type: planType,
        amount_inr: amountInr,
        status: "active",
        activated_at: serverNowIso,
        expires_at: newExpiresAtIso,
        payment_request_id: approvedReq.id,
        utr: approvedReq.utr,
      })
      .select("*")
      .single();

    if (insertLicErr) throw insertLicErr;

    return new Response(
      JSON.stringify({
        success: true,
        payment_request: approvedReq,
        license: insertedLicense,
        server_now: serverNowIso,
      }),
      { status: 200, headers: { ...corsHeaders, "Content-Type": "application/json" } },
    );
  } catch (err) {
    const message = err instanceof Error ? err.message : "Internal server error";
    return new Response(JSON.stringify({ error: message }), {
      status: 500,
      headers: { ...corsHeaders, "Content-Type": "application/json" },
    });
  }
});
