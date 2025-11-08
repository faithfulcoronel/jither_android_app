-- Create medicine_schedules table in Supabase
-- This table stores schedule information for each medicine

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

-- Create index on medicine_id for faster joins
CREATE INDEX IF NOT EXISTS idx_medicine_schedules_medicine_id ON public.medicine_schedules(medicine_id);

-- Create index for active schedules
CREATE INDEX IF NOT EXISTS idx_medicine_schedules_active ON public.medicine_schedules(medicine_id, is_active) WHERE is_active = true;

-- Enable Row Level Security
ALTER TABLE public.medicine_schedules ENABLE ROW LEVEL SECURITY;

-- Create RLS policies
-- Users can view schedules for their own medicines
CREATE POLICY "Users can view own medicine schedules"
    ON public.medicine_schedules
    FOR SELECT
    USING (
        EXISTS (
            SELECT 1 FROM public.medicines
            WHERE medicines.id = medicine_schedules.medicine_id
            AND medicines.user_id = auth.uid()
        )
    );

-- Users can insert schedules for their own medicines
CREATE POLICY "Users can insert own medicine schedules"
    ON public.medicine_schedules
    FOR INSERT
    WITH CHECK (
        EXISTS (
            SELECT 1 FROM public.medicines
            WHERE medicines.id = medicine_schedules.medicine_id
            AND medicines.user_id = auth.uid()
        )
    );

-- Users can update schedules for their own medicines
CREATE POLICY "Users can update own medicine schedules"
    ON public.medicine_schedules
    FOR UPDATE
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

-- Users can delete schedules for their own medicines
CREATE POLICY "Users can delete own medicine schedules"
    ON public.medicine_schedules
    FOR DELETE
    USING (
        EXISTS (
            SELECT 1 FROM public.medicines
            WHERE medicines.id = medicine_schedules.medicine_id
            AND medicines.user_id = auth.uid()
        )
    );

-- Create function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_medicine_schedules_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create trigger to automatically update updated_at
CREATE TRIGGER medicine_schedules_updated_at
    BEFORE UPDATE ON public.medicine_schedules
    FOR EACH ROW
    EXECUTE FUNCTION update_medicine_schedules_updated_at();
