-- Create dose_logs table in Supabase
-- This table stores logs of when medicines were taken

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

-- Create indexes for faster queries
CREATE INDEX IF NOT EXISTS idx_dose_logs_medicine_id ON public.dose_logs(medicine_id);
CREATE INDEX IF NOT EXISTS idx_dose_logs_schedule_id ON public.dose_logs(schedule_id);
CREATE INDEX IF NOT EXISTS idx_dose_logs_scheduled_time ON public.dose_logs(scheduled_time);
CREATE INDEX IF NOT EXISTS idx_dose_logs_status ON public.dose_logs(status);

-- Composite index for common queries
CREATE INDEX IF NOT EXISTS idx_dose_logs_medicine_scheduled
    ON public.dose_logs(medicine_id, scheduled_time DESC);

-- Enable Row Level Security
ALTER TABLE public.dose_logs ENABLE ROW LEVEL SECURITY;

-- Create RLS policies
-- Users can view dose logs for their own medicines
CREATE POLICY "Users can view own dose logs"
    ON public.dose_logs
    FOR SELECT
    USING (
        EXISTS (
            SELECT 1 FROM public.medicines
            WHERE medicines.id = dose_logs.medicine_id
            AND medicines.user_id = auth.uid()
        )
    );

-- Users can insert dose logs for their own medicines
CREATE POLICY "Users can insert own dose logs"
    ON public.dose_logs
    FOR INSERT
    WITH CHECK (
        EXISTS (
            SELECT 1 FROM public.medicines
            WHERE medicines.id = dose_logs.medicine_id
            AND medicines.user_id = auth.uid()
        )
    );

-- Users can update dose logs for their own medicines
CREATE POLICY "Users can update own dose logs"
    ON public.dose_logs
    FOR UPDATE
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

-- Users can delete dose logs for their own medicines
CREATE POLICY "Users can delete own dose logs"
    ON public.dose_logs
    FOR DELETE
    USING (
        EXISTS (
            SELECT 1 FROM public.medicines
            WHERE medicines.id = dose_logs.medicine_id
            AND medicines.user_id = auth.uid()
        )
    );

-- Create function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_dose_logs_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create trigger to automatically update updated_at
CREATE TRIGGER dose_logs_updated_at
    BEFORE UPDATE ON public.dose_logs
    FOR EACH ROW
    EXECUTE FUNCTION update_dose_logs_updated_at();
