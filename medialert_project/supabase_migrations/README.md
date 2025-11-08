# Supabase Database Migrations

This directory contains SQL migration scripts for setting up the MediAlert database in Supabase.

## How to Apply Migrations

### Option 1: Using Supabase SQL Editor (Recommended for first-time setup)

1. Go to your Supabase project dashboard: https://supabase.com/dashboard/project/fbcswdzuecfiodlpgmxq
2. Navigate to **SQL Editor** in the left sidebar
3. Execute the migration files in order:
   - `001_create_medicines_table.sql`
   - `002_create_medicine_schedules_table.sql`
   - `003_create_dose_logs_table.sql`
4. Copy the contents of each file and run them one by one

### Option 2: Using Supabase CLI

If you have the Supabase CLI installed:

```bash
# Install Supabase CLI (if not already installed)
npm install -g supabase

# Login to Supabase
supabase login

# Link to your project
supabase link --project-ref fbcswdzuecfiodlpgmxq

# Apply migrations
supabase db push
```

## Database Schema

### Tables Created

1. **medicines** - Stores medicine information
   - `id` (UUID, Primary Key)
   - `user_id` (UUID, Foreign Key to auth.users)
   - `name` (TEXT)
   - `dosage` (TEXT)
   - `instructions` (TEXT, nullable)
   - `color_hex` (TEXT, default: '#2196F3')
   - `is_active` (BOOLEAN, default: true)
   - `created_at` (TIMESTAMPTZ)
   - `updated_at` (TIMESTAMPTZ)

2. **medicine_schedules** - Stores medicine schedule details
   - `id` (UUID, Primary Key)
   - `medicine_id` (UUID, Foreign Key to medicines)
   - `start_date` (DATE)
   - `end_date` (DATE, nullable)
   - `reminder_times` (TEXT[])
   - `timezone` (TEXT, default: 'UTC')
   - `is_active` (BOOLEAN, default: true)
   - `created_at` (TIMESTAMPTZ)
   - `updated_at` (TIMESTAMPTZ)

3. **dose_logs** - Stores when medicines were taken
   - `id` (UUID, Primary Key)
   - `medicine_id` (UUID, Foreign Key to medicines)
   - `schedule_id` (UUID, Foreign Key to medicine_schedules)
   - `scheduled_time` (TIMESTAMPTZ)
   - `taken_time` (TIMESTAMPTZ, nullable)
   - `status` (TEXT: 'pending', 'taken', 'missed', 'skipped')
   - `notes` (TEXT, nullable)
   - `created_at` (TIMESTAMPTZ)
   - `updated_at` (TIMESTAMPTZ)

## Row Level Security (RLS)

All tables have Row Level Security enabled with policies that ensure:
- Users can only access their own data
- All CRUD operations (SELECT, INSERT, UPDATE, DELETE) are protected
- Data is isolated per user based on `auth.uid()`

## Triggers

Each table has an automatic `updated_at` trigger that updates the timestamp whenever a row is modified.

## Indexes

Indexes are created on:
- Foreign keys for faster joins
- Frequently queried columns (user_id, is_active, scheduled_time, status)
- Composite indexes for common query patterns

## Next Steps

After applying these migrations:
1. Test the authentication flow in your app
2. Create a test medicine entry
3. Verify data is being synced to Supabase
4. Check the Row Level Security policies are working correctly
