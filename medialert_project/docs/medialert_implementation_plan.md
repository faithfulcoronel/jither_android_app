# MediAlert Implementation Plan

## Overview

MediAlert is an Android medicine reminder and adherence tracker targeting students, teachers, and community members who need simple, reliable medication prompts. The application will use Supabase for remote storage and synchronization while delivering a responsive, offline-capable experience on Android.

## Current Baseline Assessment

- The project currently matches the default Navigation Drawer template. `MainActivity` wires the drawer with placeholder destinations (home, gallery, slideshow).
- `HomeFragment` displays a static `TextView`, and the floating action button only shows a snackbar prompting replacement, indicating core screens and features still need to be built.

## Architectural Direction

- **Language**: Kotlin for all new code, maintaining interoperability with legacy Java if present.
- **Pattern**: MVVM with Jetpack ViewModels, Kotlin Coroutines/StateFlow, and Navigation Component for screen routing.
- **Local Data**: Room database providing offline-first storage for medicines, schedules, and dose logs.
- **Remote Data**: Supabase (PostgREST + Realtime) for cloud persistence and synchronization across devices.
- **Scheduling**: `AlarmManager` for exact reminders backed by broadcast receivers and foreground services. `WorkManager` provides fallbacks for API restrictions.
- **Dependency Injection**: Hilt for DI graph management.
- **Notifications**: Android Notification channels with actionable prompts for adherence tracking.

## Supabase Data Model

| Table | Columns | Purpose |
| --- | --- | --- |
| `profiles` | `id (uuid)`, `email`, `display_name`, `created_at` | Authenticated user metadata; managed via Supabase Auth. |
| `medicines` | `id (uuid)`, `user_id (uuid)`, `name`, `dosage`, `instructions`, `color_hex`, `created_at`, `updated_at` | Stores core medicine details. |
| `schedules` | `id (uuid)`, `medicine_id (uuid)`, `start_date`, `end_date`, `timezone`, `repeat_rule (jsonb)`, `reminder_times (jsonb)`, `is_active` | Encodes recurrence and reminder configuration. |
| `dose_logs` | `id (uuid)`, `schedule_id (uuid)`, `scheduled_at (timestamptz)`, `taken_at (timestamptz)`, `status (enum: taken/missed/skipped)`, `notes` | Tracks adherence outcomes per reminder. |
| `device_tokens` (optional) | `user_id`, `fcm_token`, `platform`, `updated_at` | Supports future push-notification escalation. |

## Application Workstreams

### 1. Project Setup & Infrastructure
- Configure Gradle dependencies for Kotlin, Coroutines, Hilt, Room, Supabase Kotlin client, and Paging 3.
- Surface Supabase credentials via `local.properties` and `BuildConfig` for secure access.
- Establish package structure: `data/local`, `data/remote`, `data/repository`, `domain/model`, `domain/usecase`, `ui/...`.
- Create an `Application` class annotated with `@HiltAndroidApp` to bootstrap dependency injection.

### 2. Authentication & Session Management
- Implement Supabase email/password login and sign-up flows, optionally supporting magic links.
- Persist session tokens using EncryptedSharedPreferences or DataStore.
- Guard the main navigation graph with an authenticated flow and refresh sessions on app start.

### 3. Medicine & Schedule Management
- Replace the placeholder Home destination with a dashboard showing today’s medicines and an action to add a new medicine.
- Reuse the FAB to navigate to an `AddEditMedicineFragment` via Navigation Component actions.
- Build forms capturing medicine metadata, schedule builder (start date, frequency, multiple times per day), validation, and optional instructions/color pickers.
- Persist data locally (Room) and sync upstream to Supabase through repository orchestration.

### 4. Reminder Scheduling & Notifications
- Generate future reminder occurrences on schedule save/update and store them in Room.
- Register alarms with `AlarmManager.setExactAndAllowWhileIdle`; handle Android 12+ exact alarm permission flow.
- Implement `BroadcastReceiver` and foreground service to post notifications with actions for "Taken", "Snooze", and "Skip".
- Handle snooze actions by rescheduling single reminders and updating Room/Supabase records accordingly.

### 5. Adherence Tracking
- Provide notification actions and in-app controls to mark doses as taken, missed, or skipped with optional notes.
- Implement a background worker to mark overdue doses as missed after a configurable grace period.
- Propagate adherence updates to Supabase with conflict resolution based on timestamps.

### 6. History & Insights
- Build a `HistoryFragment` presenting a chronological list of doses grouped by day with status chips and filters.
- Utilize Paging 3 on Room queries and subscribe to Supabase realtime channels to reflect updates.
- Consider value-added analytics such as adherence streaks or upcoming reminders.

### 7. Settings & Quality Enhancements
- Add a settings screen using DataStore-backed preferences for notification sounds, snooze duration, and timezone adjustments.
- Support exporting history (CSV) via local generation or Supabase Edge Functions.
- Ensure accessibility with proper content descriptions, large text support, and color contrast.

### 8. Synchronization Strategy
- Treat Room as the single source of truth; synchronize with Supabase on demand and via realtime updates.
- Implement a `SyncWorker` to push pending local changes and fetch remote updates using `updated_at` cursors.
- Merge conflicts using last-write-wins semantics and surface notable conflicts to the user.

### 9. Navigation & UI Overhaul
- Redesign `mobile_navigation.xml` to include `DashboardFragment`, `AddEditMedicineFragment`, `HistoryFragment`, and `SettingsFragment`.
- Update the navigation drawer (or switch to bottom navigation) and adjust menu strings and theming.
- Replace placeholder layouts with RecyclerView/Compose implementations for dynamic content.

### 10. Notifications & Foreground Services
- Create notification channel(s) such as `MEDICATION_REMINDERS`.
- Handle Doze mode with `SCHEDULE_EXACT_ALARM` permission requests or `setAlarmClock` fallback.
- Add boot receiver to restore alarms after device restarts and communicate when alarms are disabled.

### 11. Error Handling & Observability
- Centralize Supabase and repository errors into user-friendly toasts or snackbars.
- Integrate Timber for structured logging; optionally add Crashlytics for crash reporting.
- Capture telemetry events (medicine created, reminder fired, adherence marked) for QA feedback.

### 12. Testing & QA Strategy
- Unit test repositories, schedule generators, and use cases with JUnit and coroutine test tools.
- Instrument Fragment flows and notification intents with Espresso/FragmentScenario.
- Execute end-to-end smoke tests against a Supabase staging project.
- Manually verify timezone changes, DST transitions, offline behaviors, and OEM-specific alarm reliability.

## Incremental Delivery Plan with User Stories

### Progress Tracker

| Phase | Status | Notes |
| --- | --- | --- |
| Phase 1: Foundation & Authentication | ✅ Complete | Dependency graph migrated to Hilt, Supabase auth flows implemented, and session restore wired through DataStore-backed persistence. |
| Phase 2: Medicine CRUD & Dashboard | ✅ Complete | Dashboard lists medicines from Room, add/edit form supports CRUD with validation, and repositories sync local data. |
| Phase 3: Reminders & Adherence | ⏳ Not Started | Alarm scheduling, adherence actions, and notification workflows pending implementation. |
| Phase 4: History, Sync, and Settings | ⏳ Not Started | History screen, realtime sync refinements, and settings UI still outstanding. |
| Phase 5: Polish & Release Prep | ⏳ Not Started | Accessibility, exports, and final QA automation remain in backlog. |

#### Phase 1: Foundation & Authentication (Weeks 1–2)
- [x] **Story 1:** As a developer, I want project dependencies and DI configured so that feature modules can rely on a consistent architecture.
- [x] **Story 2:** As a user, I want to sign up and log in with my email so that my medication data syncs securely.
- [x] **Story 3:** As a returning user, I want my session restored automatically so that I can access the dashboard without repeated logins.

#### Phase 2: Medicine CRUD & Dashboard (Weeks 3–4)
- [x] **Story 4:** As a user, I want to add a medicine with dosage and schedule details so that I can receive reminders at the right times.
- [x] **Story 5:** As a user, I want to view today’s scheduled medicines on the dashboard so that I know what's upcoming.
- [x] **Story 6:** As a user, I want to edit or delete existing medicines so that I can keep my regimen accurate.

#### Phase 3: Reminders & Adherence (Weeks 5–6)
- [ ] **Story 7:** As a user, I want to receive timely notifications for each dose so that I don’t miss my medication.
- [ ] **Story 8:** As a user, I want to mark a dose as taken, snoozed, or skipped directly from the notification so that adherence tracking is quick.
- [ ] **Story 9:** As a user, I want the system to mark overdue doses as missed so that my history reflects reality even if I forget to respond.

#### Phase 4: History, Sync, and Settings (Week 7)
- [ ] **Story 10:** As a user, I want to review a history of my taken and missed doses so that I can discuss adherence with my healthcare provider.
- [ ] **Story 11:** As a user, I want my data to stay consistent across devices so that I can trust the information shown.
- [ ] **Story 12:** As a user, I want to adjust notification and snooze preferences so that reminders fit my routine.

#### Phase 5: Polish & Release Prep (Week 8)
- [ ] **Story 13:** As a user with accessibility needs, I want the app to respect large fonts and screen readers so that I can use it comfortably.
- [ ] **Story 14:** As a QA engineer, I want comprehensive tests and logging so that we can catch issues before release.
- [ ] **Story 15:** As a user, I want the app to provide exportable adherence data so that I can share it with caregivers.

## Milestone Summary

1. **Foundation:** Infrastructure, authentication, base navigation.
2. **Medicine CRUD:** Dashboard, form flows, local persistence, Supabase sync.
3. **Reminders & Actions:** Alarm scheduling, notifications, adherence actions.
4. **Sync & History:** History views, realtime sync, settings, QA hardening.
5. **Polish:** Accessibility, exports, release readiness.

## Testing Overview

- Unit, integration, and instrumentation tests accompany each phase.
- Dedicated smoke tests validate Supabase integration before major releases.
- Manual regression across key device types ensures reminder reliability under varying OEM constraints.

