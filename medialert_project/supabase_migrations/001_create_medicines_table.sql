-- Create medicines table in Supabase
-- This table stores medicine information for all users

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

-- Create index on user_id for faster queries
CREATE INDEX IF NOT EXISTS idx_medicines_user_id ON public.medicines(user_id);

-- Create index on user_id and is_active for active medicines queries
CREATE INDEX IF NOT EXISTS idx_medicines_user_active ON public.medicines(user_id, is_active) WHERE is_active = true;

-- Enable Row Level Security
ALTER TABLE public.medicines ENABLE ROW LEVEL SECURITY;

-- Create RLS policies
-- Users can only see their own medicines
CREATE POLICY "Users can view own medicines"
    ON public.medicines
    FOR SELECT
    USING (auth.uid() = user_id);

-- Users can insert their own medicines
CREATE POLICY "Users can insert own medicines"
    ON public.medicines
    FOR INSERT
    WITH CHECK (auth.uid() = user_id);

-- Users can update their own medicines
CREATE POLICY "Users can update own medicines"
    ON public.medicines
    FOR UPDATE
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

-- Users can delete their own medicines
CREATE POLICY "Users can delete own medicines"
    ON public.medicines
    FOR DELETE
    USING (auth.uid() = user_id);

-- Create function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_medicines_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create trigger to automatically update updated_at
CREATE TRIGGER medicines_updated_at
    BEFORE UPDATE ON public.medicines
    FOR EACH ROW
    EXECUTE FUNCTION update_medicines_updated_at();
