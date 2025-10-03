-- Supabase schema for MediAlert
-- Run via the Supabase SQL editor or `supabase db remote commit`.

-- Ensure UUID generation helpers are available
create extension if not exists "pgcrypto";

-- Enumerations ----------------------------------------------------------------
do $$
begin
  if not exists (select 1 from pg_type where typname = 'dose_status') then
    create type dose_status as enum ('taken', 'missed', 'skipped');
  end if;
end
$$;

-- Functions -------------------------------------------------------------------
create or replace function public.set_updated_at()
returns trigger as $$
begin
  new.updated_at = timezone('utc', now());
  return new;
end;
$$ language plpgsql;

-- Tables ----------------------------------------------------------------------
create table if not exists public.profiles (
  id uuid primary key references auth.users on delete cascade,
  email text not null unique,
  display_name text,
  created_at timestamptz not null default timezone('utc', now())
);

create table if not exists public.medicines (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references public.profiles(id) on delete cascade,
  name text not null,
  dosage text,
  instructions text,
  color_hex text check (color_hex ~ '^#[0-9A-Fa-f]{6}$'),
  created_at timestamptz not null default timezone('utc', now()),
  updated_at timestamptz not null default timezone('utc', now())
);

create table if not exists public.schedules (
  id uuid primary key default gen_random_uuid(),
  medicine_id uuid not null references public.medicines(id) on delete cascade,
  start_date date not null,
  end_date date,
  timezone text not null,
  repeat_rule jsonb not null default '{}'::jsonb,
  reminder_times jsonb not null default '[]'::jsonb,
  is_active boolean not null default true,
  created_at timestamptz not null default timezone('utc', now()),
  updated_at timestamptz not null default timezone('utc', now())
);

create table if not exists public.dose_logs (
  id uuid primary key default gen_random_uuid(),
  schedule_id uuid not null references public.schedules(id) on delete cascade,
  scheduled_at timestamptz not null,
  taken_at timestamptz,
  status dose_status not null,
  notes text,
  created_at timestamptz not null default timezone('utc', now()),
  updated_at timestamptz not null default timezone('utc', now())
);

create table if not exists public.device_tokens (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references public.profiles(id) on delete cascade,
  fcm_token text not null unique,
  platform text,
  updated_at timestamptz not null default timezone('utc', now())
);

-- Trigger wiring ---------------------------------------------------------------
do $$
begin
  if not exists (
    select 1 from pg_trigger where tgname = 'set_timestamp_medicines'
  ) then
    create trigger set_timestamp_medicines
    before update on public.medicines
    for each row execute procedure public.set_updated_at();
  end if;

  if not exists (
    select 1 from pg_trigger where tgname = 'set_timestamp_schedules'
  ) then
    create trigger set_timestamp_schedules
    before update on public.schedules
    for each row execute procedure public.set_updated_at();
  end if;

  if not exists (
    select 1 from pg_trigger where tgname = 'set_timestamp_dose_logs'
  ) then
    create trigger set_timestamp_dose_logs
    before update on public.dose_logs
    for each row execute procedure public.set_updated_at();
  end if;

  if not exists (
    select 1 from pg_trigger where tgname = 'set_timestamp_device_tokens'
  ) then
    create trigger set_timestamp_device_tokens
    before update on public.device_tokens
    for each row execute procedure public.set_updated_at();
  end if;
end
$$;

-- Row Level Security -----------------------------------------------------------
alter table public.profiles enable row level security;
alter table public.medicines enable row level security;
alter table public.schedules enable row level security;
alter table public.dose_logs enable row level security;
alter table public.device_tokens enable row level security;

drop policy if exists "Individuals can manage their profile" on public.profiles;
create policy "Individuals can manage their profile"
on public.profiles
for all
using (auth.uid() = id)
with check (auth.uid() = id);

drop policy if exists "Users can manage their medicines" on public.medicines;
create policy "Users can manage their medicines"
on public.medicines
for all
using (user_id = auth.uid())
with check (user_id = auth.uid());

drop policy if exists "Users can manage their schedules" on public.schedules;
create policy "Users can manage their schedules"
on public.schedules
for all
using (exists (
  select 1 from public.medicines m
  where m.id = schedules.medicine_id and m.user_id = auth.uid()
))
with check (exists (
  select 1 from public.medicines m
  where m.id = schedules.medicine_id and m.user_id = auth.uid()
));

drop policy if exists "Users can manage their dose logs" on public.dose_logs;
create policy "Users can manage their dose logs"
on public.dose_logs
for all
using (exists (
  select 1
  from public.schedules s
  join public.medicines m on m.id = s.medicine_id
  where s.id = dose_logs.schedule_id
    and m.user_id = auth.uid()
))
with check (exists (
  select 1
  from public.schedules s
  join public.medicines m on m.id = s.medicine_id
  where s.id = dose_logs.schedule_id
    and m.user_id = auth.uid()
));

drop policy if exists "Users can manage their device tokens" on public.device_tokens;
create policy "Users can manage their device tokens"
on public.device_tokens
for all
using (user_id = auth.uid())
with check (user_id = auth.uid());

-- Helpful indexes --------------------------------------------------------------
create index if not exists idx_medicines_user on public.medicines(user_id);
create index if not exists idx_schedules_medicine on public.schedules(medicine_id);
create index if not exists idx_dose_logs_schedule on public.dose_logs(schedule_id, scheduled_at);
create index if not exists idx_device_tokens_user on public.device_tokens(user_id);
