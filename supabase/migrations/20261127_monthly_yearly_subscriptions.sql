-- Vyapar Billing Pro: Monthly (₹499) & Yearly (₹1,500) Subscription + 48-Hour Trial + Legacy Lifetime Compatibility
-- Run this migration in Supabase SQL Editor or via Supabase CLI (`supabase db push`).

-- 1. PAYMENT REQUESTS TABLE UPDATES
ALTER TABLE IF EXISTS public.payment_requests
  ADD COLUMN IF NOT EXISTS plan_type text,
  ADD COLUMN IF NOT EXISTS amount_inr integer NOT NULL DEFAULT 499,
  ADD COLUMN IF NOT EXISTS payment_method text NOT NULL DEFAULT 'phonepe',
  ADD COLUMN IF NOT EXISTS customer_note text DEFAULT '',
  ADD COLUMN IF NOT EXISTS reviewed_at timestamptz,
  ADD COLUMN IF NOT EXISTS reviewed_by uuid;

-- Backfill historical payment_requests without breaking legacy ₹499 lifetime records
UPDATE public.payment_requests pr
SET plan_type = CASE
  WHEN pr.plan_type IS NOT NULL AND trim(pr.plan_type) <> '' THEN lower(trim(pr.plan_type))
  WHEN pr.amount_inr IN (999, 1500) THEN 'yearly'
  ELSE 'lifetime'
END
WHERE pr.plan_type IS NULL OR trim(pr.plan_type) = '';

-- Allow historical 'lifetime' rows in table history while blocking NEW lifetime / tampered payment inserts via trigger + RLS
ALTER TABLE IF EXISTS public.payment_requests
  DROP CONSTRAINT IF EXISTS payment_requests_plan_type_check;
ALTER TABLE IF EXISTS public.payment_requests
  ADD CONSTRAINT payment_requests_plan_type_check
  CHECK (plan_type IN ('monthly', 'yearly', 'lifetime'));

ALTER TABLE IF EXISTS public.payment_requests
  DROP CONSTRAINT IF EXISTS payment_requests_status_check;
ALTER TABLE IF EXISTS public.payment_requests
  ADD CONSTRAINT payment_requests_status_check
  CHECK (status IN ('pending', 'approved', 'rejected'));

-- Prevent duplicate UTR submissions (case-insensitive)
CREATE UNIQUE INDEX IF NOT EXISTS uq_payment_requests_utr_upper
  ON public.payment_requests (upper(trim(utr)));

-- Prevent multiple simultaneous pending payment requests per user
CREATE UNIQUE INDEX IF NOT EXISTS uq_payment_requests_one_pending_per_user
  ON public.payment_requests (user_id)
  WHERE status = 'pending';

-- Trigger enforcing strict Monthly (₹499) / Yearly (₹1,500) & pending-only rules on every NEW insert
CREATE OR REPLACE FUNCTION public.enforce_new_payment_request_rules()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
  NEW.utr := upper(trim(coalesce(NEW.utr, '')));
  NEW.plan_type := lower(trim(coalesce(NEW.plan_type, '')));
  NEW.payment_method := lower(trim(coalesce(NEW.payment_method, '')));
  NEW.status := lower(trim(coalesce(NEW.status, 'pending')));

  IF NEW.status <> 'pending' THEN
    RAISE EXCEPTION 'New payment requests must start with pending status';
  END IF;

  IF NEW.payment_method <> 'phonepe' THEN
    RAISE EXCEPTION 'Unsupported payment_method: %', NEW.payment_method;
  END IF;

  IF NEW.plan_type NOT IN ('monthly', 'yearly') THEN
    RAISE EXCEPTION 'Only monthly or yearly plans are available for new purchases';
  END IF;

  IF (NEW.plan_type = 'monthly' AND NEW.amount_inr <> 499)
     OR (NEW.plan_type = 'yearly' AND NEW.amount_inr <> 1500) THEN
    RAISE EXCEPTION 'Invalid amount % for plan %', NEW.amount_inr, NEW.plan_type;
  END IF;

  RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_enforce_new_payment_request_rules ON public.payment_requests;
CREATE TRIGGER trg_enforce_new_payment_request_rules
  BEFORE INSERT ON public.payment_requests
  FOR EACH ROW
  EXECUTE FUNCTION public.enforce_new_payment_request_rules();

-- 2. LICENSES TABLE UPDATES (MULTI-PLAN + RENEWAL HISTORY + LEGACY LIFETIME PRESERVATION)
ALTER TABLE IF EXISTS public.licenses
  ADD COLUMN IF NOT EXISTS plan_type text,
  ADD COLUMN IF NOT EXISTS license_type text,
  ADD COLUMN IF NOT EXISTS activated_at timestamptz DEFAULT now(),
  ADD COLUMN IF NOT EXISTS expires_at timestamptz,
  ADD COLUMN IF NOT EXISTS amount_inr integer,
  ADD COLUMN IF NOT EXISTS payment_request_id uuid REFERENCES public.payment_requests(id) ON DELETE SET NULL,
  ADD COLUMN IF NOT EXISTS utr text;

-- Preserve all existing active lifetime rows safely (never expire or convert legacy ₹499 lifetime users)
UPDATE public.licenses
SET
  plan_type = COALESCE(NULLIF(lower(trim(plan_type)), ''), 'lifetime'),
  license_type = COALESCE(NULLIF(lower(trim(license_type)), ''), 'lifetime'),
  amount_inr = COALESCE(amount_inr, 499)
WHERE expires_at IS NULL
  AND (plan_type IS NULL OR lower(trim(plan_type)) = 'lifetime');

ALTER TABLE IF EXISTS public.licenses
  DROP CONSTRAINT IF EXISTS licenses_status_check;
ALTER TABLE IF EXISTS public.licenses
  ADD CONSTRAINT licenses_status_check
  CHECK (status IN ('active', 'expired', 'revoked', 'inactive'));

ALTER TABLE IF EXISTS public.licenses
  DROP CONSTRAINT IF EXISTS licenses_plan_type_check;
ALTER TABLE IF EXISTS public.licenses
  ADD CONSTRAINT licenses_plan_type_check
  CHECK (plan_type IS NULL OR plan_type IN ('monthly', 'yearly', 'lifetime'));

-- Guarantee at most ONE license activation per approved payment_request
CREATE UNIQUE INDEX IF NOT EXISTS uq_licenses_payment_request_id
  ON public.licenses (payment_request_id)
  WHERE payment_request_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_licenses_user_status_expiry
  ON public.licenses (user_id, status, expires_at DESC NULLS LAST);

-- 3. AUTOMATIC 48-HOUR SERVER-AUTHORITATIVE FREE TRIAL TABLE + RPC
CREATE TABLE IF NOT EXISTS public.user_trials (
  user_id uuid PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
  trial_started_at timestamptz NOT NULL DEFAULT now(),
  trial_expires_at timestamptz NOT NULL DEFAULT (now() + interval '48 hours'),
  created_at timestamptz NOT NULL DEFAULT now()
);

-- Pre-rollout cutoff: accounts created before trial launch do NOT receive an automatic trial
CREATE OR REPLACE FUNCTION public.activate_user_trial()
RETURNS public.user_trials
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, auth
AS $$
DECLARE
  v_uid uuid := auth.uid();
  v_existing public.user_trials;
  v_user_created_at timestamptz;
  v_has_history boolean;
BEGIN
  IF v_uid IS NULL THEN
    RAISE EXCEPTION 'Authentication required' USING ERRCODE = '42501';
  END IF;

  SELECT * INTO v_existing
  FROM public.user_trials
  WHERE user_id = v_uid;

  IF FOUND THEN
    RETURN v_existing;
  END IF;

  SELECT created_at INTO v_user_created_at
  FROM auth.users
  WHERE id = v_uid;

  IF v_user_created_at IS NULL OR v_user_created_at < '2026-11-26T00:00:00Z'::timestamptz THEN
    RAISE EXCEPTION 'Account is not eligible for new-user free trial' USING ERRCODE = '42501';
  END IF;

  SELECT (
    EXISTS (SELECT 1 FROM public.licenses WHERE user_id = v_uid)
    OR EXISTS (SELECT 1 FROM public.payment_requests WHERE user_id = v_uid)
  ) INTO v_has_history;

  IF v_has_history THEN
    RAISE EXCEPTION 'Trial unavailable for accounts with existing license or payment history' USING ERRCODE = '42501';
  END IF;

  INSERT INTO public.user_trials (user_id, trial_started_at, trial_expires_at)
  VALUES (v_uid, now(), now() + interval '48 hours')
  ON CONFLICT (user_id) DO NOTHING;

  SELECT * INTO v_existing
  FROM public.user_trials
  WHERE user_id = v_uid;

  RETURN v_existing;
END;
$$;

REVOKE ALL ON FUNCTION public.activate_user_trial() FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.activate_user_trial() TO authenticated;

-- 4. ZERO-TRUST ROW LEVEL SECURITY POLICIES
ALTER TABLE public.payment_requests ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.licenses ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.user_trials ENABLE ROW LEVEL SECURITY;

-- payment_requests policies
DROP POLICY IF EXISTS "Users read own payment requests" ON public.payment_requests;
CREATE POLICY "Users read own payment requests"
  ON public.payment_requests FOR SELECT
  TO authenticated
  USING (auth.uid() = user_id);

DROP POLICY IF EXISTS "Users insert valid monthly or yearly payment requests" ON public.payment_requests;
CREATE POLICY "Users insert valid monthly or yearly payment requests"
  ON public.payment_requests FOR INSERT
  TO authenticated
  WITH CHECK (
    auth.uid() = user_id
    AND status = 'pending'
    AND payment_method = 'phonepe'
    AND plan_type IN ('monthly', 'yearly')
    AND (
      (plan_type = 'monthly' AND amount_inr = 499)
      OR (plan_type = 'yearly' AND amount_inr = 1500)
    )
  );

-- Explicit deny on client updates/deletes to payment_requests & licenses
REVOKE UPDATE, DELETE ON public.payment_requests FROM anon, authenticated;
REVOKE INSERT, UPDATE, DELETE ON public.licenses FROM anon, authenticated;
REVOKE INSERT, UPDATE, DELETE ON public.user_trials FROM anon, authenticated;

-- licenses policies (SELECT own rows only; writes via Edge Functions / service_role only)
DROP POLICY IF EXISTS "Users read own licenses" ON public.licenses;
CREATE POLICY "Users read own licenses"
  ON public.licenses FOR SELECT
  TO authenticated
  USING (auth.uid() = user_id);

-- user_trials policies (SELECT own row only; writes strictly via SECURITY DEFINER RPC)
DROP POLICY IF EXISTS "Users read own trial" ON public.user_trials;
CREATE POLICY "Users read own trial"
  ON public.user_trials FOR SELECT
  TO authenticated
  USING (auth.uid() = user_id);
