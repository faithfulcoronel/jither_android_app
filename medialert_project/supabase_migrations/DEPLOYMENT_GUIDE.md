# Supabase Database Deployment Guide

## Quick Deploy (Recommended)

Follow these simple steps to deploy your database schema to Supabase:

### Step 1: Open Supabase SQL Editor

1. Go to your Supabase Dashboard: **https://supabase.com/dashboard/project/fbcswdzuecfiodlpgmxq**
2. Click on **SQL Editor** in the left sidebar (icon looks like `</>`)

### Step 2: Run the Deployment Script

1. Click **"New query"** button (top right)
2. Open the file `DEPLOY_ALL.sql` in this directory
3. **Copy the entire contents** of `DEPLOY_ALL.sql`
4. **Paste** into the Supabase SQL Editor
5. Click **"Run"** button (or press `Ctrl+Enter` / `Cmd+Enter`)

### Step 3: Verify Deployment

After running the script, you should see a success message. Now verify:

1. Go to **Table Editor** in the left sidebar
2. You should see 3 new tables:
   - `medicines`
   - `medicine_schedules`
   - `dose_logs`

---

## Detailed Verification Steps

### Check Tables Created

In the **Table Editor**, verify each table has the correct columns:

#### `medicines` table:
- ✅ id (uuid)
- ✅ user_id (uuid)
- ✅ name (text)
- ✅ dosage (text)
- ✅ instructions (text, nullable)
- ✅ color_hex (text)
- ✅ is_active (bool)
- ✅ created_at (timestamptz)
- ✅ updated_at (timestamptz)

#### `medicine_schedules` table:
- ✅ id (uuid)
- ✅ medicine_id (uuid)
- ✅ start_date (date)
- ✅ end_date (date, nullable)
- ✅ reminder_times (text[])
- ✅ timezone (text)
- ✅ is_active (bool)
- ✅ created_at (timestamptz)
- ✅ updated_at (timestamptz)

#### `dose_logs` table:
- ✅ id (uuid)
- ✅ medicine_id (uuid)
- ✅ schedule_id (uuid)
- ✅ scheduled_time (timestamptz)
- ✅ taken_time (timestamptz, nullable)
- ✅ status (text)
- ✅ notes (text, nullable)
- ✅ created_at (timestamptz)
- ✅ updated_at (timestamptz)

### Verify Row Level Security (RLS)

1. Click on any table in **Table Editor**
2. Click the **shield icon** or go to **"Policies"** tab
3. You should see 4 policies per table:
   - Users can view own [table]
   - Users can insert own [table]
   - Users can update own [table]
   - Users can delete own [table]

### Test RLS Policies

To ensure RLS is working:

1. Go to **SQL Editor**
2. Run this test query:

```sql
-- This should return 0 rows (no medicines yet)
SELECT * FROM public.medicines;
```

3. If you're not logged in, you should see **no data** (RLS is protecting it!)

---

## Alternative: Run Individual Migration Files

If you prefer to run migrations one at a time:

### Migration 1: Medicines Table
```sql
-- Copy contents of: 001_create_medicines_table.sql
-- Paste and run in SQL Editor
```

### Migration 2: Medicine Schedules Table
```sql
-- Copy contents of: 002_create_medicine_schedules_table.sql
-- Paste and run in SQL Editor
```

### Migration 3: Dose Logs Table
```sql
-- Copy contents of: 003_create_dose_logs_table.sql
-- Paste and run in SQL Editor
```

---

## Troubleshooting

### Error: "relation already exists"
**Solution:** The tables are already created. You can safely ignore this error or drop and recreate:
```sql
DROP TABLE IF EXISTS public.dose_logs CASCADE;
DROP TABLE IF EXISTS public.medicine_schedules CASCADE;
DROP TABLE IF EXISTS public.medicines CASCADE;
-- Then run DEPLOY_ALL.sql again
```

### Error: "policy already exists"
**Solution:** The script handles this automatically with `DROP POLICY IF EXISTS`. If you still see errors, they're safe to ignore.

### Error: "permission denied"
**Solution:** Make sure you're logged into the correct Supabase project and have owner/admin permissions.

### Can't find SQL Editor
**Solution:**
1. Make sure you're logged into Supabase
2. Navigate to: https://supabase.com/dashboard/project/fbcswdzuecfiodlpgmxq
3. Look for the `</>` icon in the left sidebar

---

## Testing the Database

After deployment, test the database with your Android app:

### Step 1: Build and Run the App
```bash
cd /path/to/medialert_project
./gradlew clean
./gradlew installDebug
```

### Step 2: Create a Test Account
1. Open the app
2. Click "Sign Up"
3. Enter email: `test@example.com`
4. Enter password: `test123456`
5. Sign up and verify the account (check email if email confirmation is enabled)

### Step 3: Add a Test Medicine
1. After logging in, click the **+** button
2. Fill in:
   - Name: Aspirin
   - Dosage: 500mg
   - Start Date: Today
   - Times: 08:00,20:00
3. Save

### Step 4: Verify in Supabase
1. Go to Supabase **Table Editor**
2. Click on `medicines` table
3. You should see your test medicine!

---

## Database Schema Overview

```
┌─────────────────┐
│    medicines    │
│                 │
│  - id (PK)      │
│  - user_id (FK) │◄────────┐
│  - name         │         │
│  - dosage       │         │
│  - ...          │         │
└─────────────────┘         │
        │                   │
        │ 1:N               │
        ▼                   │
┌─────────────────────┐     │
│ medicine_schedules  │     │
│                     │     │
│  - id (PK)          │     │
│  - medicine_id (FK) │─────┘
│  - start_date       │
│  - reminder_times   │
│  - ...              │
└─────────────────────┘
        │
        │ 1:N
        ▼
┌─────────────────┐
│   dose_logs     │
│                 │
│  - id (PK)      │
│  - medicine_id  │
│  - schedule_id  │
│  - status       │
│  - ...          │
└─────────────────┘
```

---

## Security Features

### Row Level Security (RLS)
- ✅ **Enabled** on all tables
- ✅ Users can ONLY see their own data
- ✅ No user can access another user's medicines
- ✅ All CRUD operations are protected

### Cascading Deletes
- ✅ Deleting a medicine automatically deletes its schedules
- ✅ Deleting a schedule automatically deletes its dose logs
- ✅ Data integrity is maintained

### Automatic Timestamps
- ✅ `created_at` set automatically on insert
- ✅ `updated_at` updated automatically on every change
- ✅ Triggers handle this automatically

---

## Next Steps After Deployment

1. ✅ Database deployed
2. ⏭️ Test app authentication
3. ⏭️ Add test medicine data
4. ⏭️ Verify data appears in Supabase
5. ⏭️ Implement cloud sync (optional enhancement)

---

## Support

If you encounter any issues:
1. Check the **Logs** section in Supabase Dashboard
2. Verify your project URL and anon key in `local.properties`
3. Ensure you have internet connection
4. Check Supabase project status: https://status.supabase.com/

---

## Summary

**What You Just Deployed:**
- ✅ 3 database tables (medicines, schedules, dose_logs)
- ✅ 12 RLS policies (4 per table)
- ✅ 9 indexes for performance
- ✅ 3 automatic timestamp triggers
- ✅ Foreign key relationships
- ✅ Data validation constraints

**Your database is now ready for production use!** 🎉
