-- ============================================================================
-- MediAlert Database - Complete Deployment Script
-- ============================================================================
-- Run this entire script in Supabase SQL Editor to create all tables
-- Project: https://supabase.com/dashboard/project/fbcswdzuecfiodlpgmxq
-- ============================================================================

-- ============================================================================
-- 1. MEDICINES TABLE
-- ============================================================================

-- Create medicines table in Supabase
CREATE TABLE IF NOT EXISTS public.medicines (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    dosage TEXT NOT NULL,
    instructions TEXT,
    color_hex TEXT NOT NULL DEFAULT '#2196F3',
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_medicines_user_id ON public.medicines(user_id);
CREATE INDEX IF NOT EXISTS idx_medicines_user_active ON public.medicines(user_id, is_active) WHERE is_active = true;

-- Enable Row Level Security
ALTER TABLE public.medicines ENABLE ROW LEVEL SECURITY;

-- Drop existing policies if they exist (to avoid conflicts)
DROP POLICY IF EXISTS "Users can view own medicines" ON public.medicines;
DROP POLICY IF EXISTS "Users can insert own medicines" ON public.medicines;
DROP POLICY IF EXISTS "Users can update own medicines" ON public.medicines;
DROP POLICY IF EXISTS "Users can delete own medicines" ON public.medicines;

-- Create RLS policies
CREATE POLICY "Users can view own medicines"
    ON public.medicines FOR SELECT
    USING (auth.uid() = user_id);

CREATE POLICY "Users can insert own medicines"
    ON public.medicines FOR INSERT
    WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can update own medicines"
    ON public.medicines FOR UPDATE
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can delete own medicines"
    ON public.medicines FOR DELETE
    USING (auth.uid() = user_id);

-- Create function and trigger for updated_at
CREATE OR REPLACE FUNCTION update_medicines_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS medicines_updated_at ON public.medicines;
CREATE TRIGGER medicines_updated_at
    BEFORE UPDATE ON public.medicines
    FOR EACH ROW
    EXECUTE FUNCTION update_medicines_updated_at();

-- ============================================================================
-- 2. MEDICINE SCHEDULES TABLE
-- ============================================================================

-- Create medicine_schedules table
CREATE TABLE IF NOT EXISTS public.medicine_schedules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    medicine_id UUID NOT NULL REFERENCES public.medicines(id) ON DELETE CASCADE,
    start_date DATE NOT NULL,
    end_date DATE,
    reminder_times TEXT[] NOT NULL,
    timezone TEXT NOT NULL DEFAULT 'UTC',
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_medicine_schedules_medicine_id ON public.medicine_schedules(medicine_id);
CREATE INDEX IF NOT EXISTS idx_medicine_schedules_active ON public.medicine_schedules(medicine_id, is_active) WHERE is_active = true;

-- Enable Row Level Security
ALTER TABLE public.medicine_schedules ENABLE ROW LEVEL SECURITY;

-- Drop existing policies if they exist
DROP POLICY IF EXISTS "Users can view own medicine schedules" ON public.medicine_schedules;
DROP POLICY IF EXISTS "Users can insert own medicine schedules" ON public.medicine_schedules;
DROP POLICY IF EXISTS "Users can update own medicine schedules" ON public.medicine_schedules;
DROP POLICY IF EXISTS "Users can delete own medicine schedules" ON public.medicine_schedules;

-- Create RLS policies
CREATE POLICY "Users can view own medicine schedules"
    ON public.medicine_schedules FOR SELECT
    USING (
        EXISTS (
            SELECT 1 FROM public.medicines
            WHERE medicines.id = medicine_schedules.medicine_id
            AND medicines.user_id = auth.uid()
        )
    );

CREATE POLICY "Users can insert own medicine schedules"
    ON public.medicine_schedules FOR INSERT
    WITH CHECK (
        EXISTS (
            SELECT 1 FROM public.medicines
            WHERE medicines.id = medicine_schedules.medicine_id
            AND medicines.user_id = auth.uid()
        )
    );

CREATE POLICY "Users can update own medicine schedules"
    ON public.medicine_schedules FOR UPDATE
    USING (
        EXISTS (
            SELECT 1 FROM public.medicines
            WHERE medicines.id = medicine_schedules.medicine_id
            AND medicines.user_id = auth.uid()
        )
    )
    WITH CHECK (
        EXISTS (
            SELECT 1 FROM public.medicines
            WHERE medicines.id = medicine_schedules.medicine_id
            AND medicines.user_id = auth.uid()
        )
    );

CREATE POLICY "Users can delete own medicine schedules"
    ON public.medicine_schedules FOR DELETE
    USING (
        EXISTS (
            SELECT 1 FROM public.medicines
            WHERE medicines.id = medicine_schedules.medicine_id
            AND medicines.user_id = auth.uid()
        )
    );

-- Create function and trigger for updated_at
CREATE OR REPLACE FUNCTION update_medicine_schedules_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS medicine_schedules_updated_at ON public.medicine_schedules;
CREATE TRIGGER medicine_schedules_updated_at
    BEFORE UPDATE ON public.medicine_schedules
    FOR EACH ROW
    EXECUTE FUNCTION update_medicine_schedules_updated_at();

-- ============================================================================
-- 3. DOSE LOGS TABLE
-- ============================================================================

-- Create dose_logs table
CREATE TABLE IF NOT EXISTS public.dose_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    medicine_id UUID NOT NULL REFERENCES public.medicines(id) ON DELETE CASCADE,
    schedule_id UUID NOT NULL REFERENCES public.medicine_schedules(id) ON DELETE CASCADE,
    scheduled_time TIMESTAMPTZ NOT NULL,
    taken_time TIMESTAMPTZ,
    status TEXT NOT NULL CHECK (status IN ('pending', 'taken', 'missed', 'skipped')),
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_dose_logs_medicine_id ON public.dose_logs(medicine_id);
CREATE INDEX IF NOT EXISTS idx_dose_logs_schedule_id ON public.dose_logs(schedule_id);
CREATE INDEX IF NOT EXISTS idx_dose_logs_scheduled_time ON public.dose_logs(scheduled_time);
CREATE INDEX IF NOT EXISTS idx_dose_logs_status ON public.dose_logs(status);
CREATE INDEX IF NOT EXISTS idx_dose_logs_medicine_scheduled ON public.dose_logs(medicine_id, scheduled_time DESC);

-- Enable Row Level Security
ALTER TABLE public.dose_logs ENABLE ROW LEVEL SECURITY;

-- Drop existing policies if they exist
DROP POLICY IF EXISTS "Users can view own dose logs" ON public.dose_logs;
DROP POLICY IF EXISTS "Users can insert own dose logs" ON public.dose_logs;
DROP POLICY IF EXISTS "Users can update own dose logs" ON public.dose_logs;
DROP POLICY IF EXISTS "Users can delete own dose logs" ON public.dose_logs;

-- Create RLS policies
CREATE POLICY "Users can view own dose logs"
    ON public.dose_logs FOR SELECT
    USING (
        EXISTS (
            SELECT 1 FROM public.medicines
            WHERE medicines.id = dose_logs.medicine_id
            AND medicines.user_id = auth.uid()
        )
    );

CREATE POLICY "Users can insert own dose logs"
    ON public.dose_logs FOR INSERT
    WITH CHECK (
        EXISTS (
            SELECT 1 FROM public.medicines
            WHERE medicines.id = dose_logs.medicine_id
            AND medicines.user_id = auth.uid()
        )
    );

CREATE POLICY "Users can update own dose logs"
    ON public.dose_logs FOR UPDATE
    USING (
        EXISTS (
            SELECT 1 FROM public.medicines
            WHERE medicines.id = dose_logs.medicine_id
            AND medicines.user_id = auth.uid()
        )
    )
    WITH CHECK (
        EXISTS (
            SELECT 1 FROM public.medicines
            WHERE medicines.id = dose_logs.medicine_id
            AND medicines.user_id = auth.uid()
        )
    );

CREATE POLICY "Users can delete own dose logs"
    ON public.dose_logs FOR DELETE
    USING (
        EXISTS (
            SELECT 1 FROM public.medicines
            WHERE medicines.id = dose_logs.medicine_id
            AND medicines.user_id = auth.uid()
        )
    );

-- Create function and trigger for updated_at
CREATE OR REPLACE FUNCTION update_dose_logs_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS dose_logs_updated_at ON public.dose_logs;
CREATE TRIGGER dose_logs_updated_at
    BEFORE UPDATE ON public.dose_logs
    FOR EACH ROW
    EXECUTE FUNCTION update_dose_logs_updated_at();

-- ============================================================================
-- DEPLOYMENT COMPLETE
-- ============================================================================
-- All tables, indexes, RLS policies, and triggers have been created!
--
-- Next steps:
-- 1. Verify tables in Supabase Table Editor
-- 2. Test authentication in your app
-- 3. Add test medicine data
-- ============================================================================
