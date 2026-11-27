// Supabase Edge Function: license-api-
// Deploy with: supabase functions deploy license-api-
// Required Edge Function Secrets:
//   SUPABASE_URL, SUPABASE_ANON_KEY, SUPABASE_SERVICE_ROLE_KEY, LICENSE_API_SECRET

import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

function constantTimeEqual(a: string, b: string): boolean {
  if (a.length !== b.length || a.length === 0) return false;
  let diff = 0;
  for (let i = 0; i < a.length; i++) {
    diff |= a.charCodeAt(i) ^ b.charCodeAt(i);
  }
  return diff === 0;
}

Deno.serve(async (req: Request) => {
  if (req.method !== "GET" && req.method !== "POST") {
    return new Response(JSON.stringify({ error: "Method not allowed" }), {
      status: 405,
      headers: { "Content-Type": "application/json" },
    });
  }

  try {
    const supabaseUrl = Deno.env.get("SUPABASE_URL") ?? "";
    const anonKey = Deno.env.get("SUPABASE_ANON_KEY") ?? "";
    const serviceRoleKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? "";
    const configuredSecret = (Deno.env.get("LICENSE_API_SECRET") ?? "").trim();

    if (!supabaseUrl || !anonKey || !serviceRoleKey) {
      return new Response(JSON.stringify({ error: "Server configuration missing" }), {
        status: 500,
        headers: { "Content-Type": "application/json" },
      });
    }

    const adminClient = createClient(supabaseUrl, serviceRoleKey);
    const providedSecret = (req.headers.get("x-license-secret") ?? "").trim();
    const authHeader = req.headers.get("Authorization") ?? "";

    let targetUserId = "";
    if (configuredSecret && constantTimeEqual(providedSecret, configuredSecret)) {
      const url = new URL(req.url);
      targetUserId = (url.searchParams.get("user_id") ?? "").trim();
      if (!targetUserId && req.method === "POST") {
        const body = await req.json().catch(() => ({}));
        targetUserId = String(body?.user_id ?? "").trim();
      }
    } else if (authHeader.startsWith("Bearer ")) {
      const userClient = createClient(supabaseUrl, anonKey, {
        global: { headers: { Authorization: authHeader } },
      });
      const { data: userData, error: userErr } = await userClient.auth.getUser();
      if (userErr || !userData?.user) {
        return new Response(JSON.stringify({ error: "Unauthorized" }), {
          status: 401,
          headers: { "Content-Type": "application/json" },
        });
      }
      targetUserId = userData.user.id;
    } else {
      return new Response(JSON.stringify({ error: "Unauthorized" }), {
        status: 401,
        headers: { "Content-Type": "application/json" },
      });
    }

    if (!targetUserId) {
      return new Response(JSON.stringify({ error: "user_id required" }), {
        status: 400,
        headers: { "Content-Type": "application/json" },
      });
    }

    const nowMs = Date.now();
    const serverNowIso = new Date(nowMs).toISOString();

    const [{ data: licenses, error: licErr }, { data: trial, error: trialErr }] = await Promise.all([
      adminClient
        .from("licenses")
        .select("*")
        .eq("user_id", targetUserId)
        .order("expires_at", { ascending: false, nullsFirst: false }),
      adminClient
        .from("user_trials")
        .select("*")
        .eq("user_id", targetUserId)
        .maybeSingle(),
    ]);

    if (licErr) throw licErr;
    if (trialErr) throw trialErr;

    const allLicenses = licenses ?? [];
    const revokedRow = allLicenses.find((r) => String(r.status).toLowerCase() === "revoked");
    const legacyLifetime = allLicenses.find(
      (r) =>
        String(r.status).toLowerCase() === "active" &&
        (r.expires_at == null ||
          String(r.license_type ?? "").toLowerCase() === "lifetime" ||
          String(r.plan_type ?? "").toLowerCase() === "lifetime"),
    );

    if (legacyLifetime) {
      return new Response(
        JSON.stringify({
          user_id: targetUserId,
          server_now: serverNowIso,
          entitlement_status: "LIFETIME_ACTIVE",
          has_active_paid_plan: true,
          is_legacy_lifetime: true,
          license: legacyLifetime,
          trial: trial ?? null,
        }),
        { status: 200, headers: { "Content-Type": "application/json" } },
      );
    }

    const activeTimed = allLicenses.find((r) => {
      if (String(r.status).toLowerCase() !== "active") return false;
      const expMs = r.expires_at ? Date.parse(r.expires_at) : NaN;
      return Number.isFinite(expMs) && expMs > nowMs;
    });

    if (activeTimed) {
      const pType = String(activeTimed.plan_type ?? "").toLowerCase() === "yearly" ? "YEARLY_ACTIVE" : "MONTHLY_ACTIVE";
      return new Response(
        JSON.stringify({
          user_id: targetUserId,
          server_now: serverNowIso,
          entitlement_status: pType,
          has_active_paid_plan: true,
          is_legacy_lifetime: false,
          license: activeTimed,
          trial: trial ?? null,
        }),
        { status: 200, headers: { "Content-Type": "application/json" } },
      );
    }

    if (revokedRow) {
      return new Response(
        JSON.stringify({
          user_id: targetUserId,
          server_now: serverNowIso,
          entitlement_status: "REVOKED",
          has_active_paid_plan: false,
          license: revokedRow,
          trial: trial ?? null,
        }),
        { status: 200, headers: { "Content-Type": "application/json" } },
      );
    }

    const expiredTimed = allLicenses.find((r) => {
      const st = String(r.status).toLowerCase();
      if (st === "expired") return true;
      if (st === "active" && r.expires_at) {
        const expMs = Date.parse(r.expires_at);
        return Number.isFinite(expMs) && expMs <= nowMs;
      }
      return false;
    });

    if (expiredTimed) {
      const pType = String(expiredTimed.plan_type ?? "").toLowerCase() === "yearly" ? "YEARLY_EXPIRED" : "MONTHLY_EXPIRED";
      return new Response(
        JSON.stringify({
          user_id: targetUserId,
          server_now: serverNowIso,
          entitlement_status: pType,
          has_active_paid_plan: false,
          license: expiredTimed,
          trial: trial ?? null,
        }),
        { status: 200, headers: { "Content-Type": "application/json" } },
      );
    }

    const trialExpMs = trial?.trial_expires_at ? Date.parse(trial.trial_expires_at) : NaN;
    const trialActive = Boolean(trial && Number.isFinite(trialExpMs) && nowMs < trialExpMs);
    const entitlementStatus = trialActive
      ? "TRIAL_ACTIVE"
      : trial
        ? "TRIAL_EXPIRED"
        : "FREE";

    return new Response(
      JSON.stringify({
        user_id: targetUserId,
        server_now: serverNowIso,
        entitlement_status: entitlementStatus,
        has_active_paid_plan: false,
        has_active_trial: trialActive,
        license: null,
        trial: trial ?? null,
      }),
      { status: 200, headers: { "Content-Type": "application/json" } },
    );
  } catch (err) {
    const message = err instanceof Error ? err.message : "Internal server error";
    return new Response(JSON.stringify({ error: message }), {
      status: 500,
      headers: { "Content-Type": "application/json" },
    });
  }
});
