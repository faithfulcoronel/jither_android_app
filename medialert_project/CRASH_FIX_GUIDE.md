# MediAlert "Keeps Stopping" - Crash Fix Guide

## Issue: App Crashes on Startup

**Symptom:** App shows "MediAlert keeps stopping" error immediately after launch

---

## Root Cause Analysis

Based on code inspection, the crash is most likely caused by **one of these issues**:

### 1. **Supabase Client Initialization Failure** (MOST LIKELY) ⚠️

**Location:** `app/src/main/java/.../di/SupabaseModule.kt:20`

**Problem:**
- If `BuildConfig.SUPABASE_URL` or `BuildConfig.SUPABASE_ANON_KEY` is empty/null
- The Supabase client creation will throw an exception
- This happens during Hilt initialization, crashing the entire app

**Fix Applied:** ✅
Added validation to provide better error messages:
```kotlin
// Lines 21-30
val url = BuildConfig.SUPABASE_URL
val key = BuildConfig.SUPABASE_ANON_KEY

require(url.isNotBlank()) {
    "SUPABASE_URL is not configured. Please check local.properties"
}
require(key.isNotBlank()) {
    "SUPABASE_ANON_KEY is not configured. Please check local.properties"
}
```

---

### 2. **BuildConfig Not Generated** ⚠️

**Problem:**
- After modifying `local.properties` or `build.gradle.kts`
- BuildConfig fields might not regenerate properly
- Results in empty/default values

**Solution:** Rebuild the project

```bash
# In Android Studio:
Build → Clean Project
Build → Rebuild Project

# Or via command line:
./gradlew clean
./gradlew build
```

---

### 3. **Invalid Refresh Token** ⚠️

**Location:** `AuthRepositoryImpl.kt:71`

**Problem:**
- Old/expired refresh token stored in DataStore
- App tries to restore session on startup
- Supabase rejects the token

**Solution:** Clear app data

```
Settings → Apps → MediAlert → Storage → Clear Data
```

---

### 4. **Network Issues** ⚠️

**Problem:**
- Supabase client tries to validate credentials on creation
- Network timeout or unreachable server causes crash

**Solution:**
- Check internet connection
- Try on different network (WiFi/Mobile data)
- Check Supabase status: https://status.supabase.com/

---

## Step-by-Step Fix Instructions

### **Fix 1: Verify Supabase Credentials** ✅

1. Check `local.properties` file:
```properties
SUPABASE_URL=https://fbcswdzuecfiodlpgmxq.supabase.co
SUPABASE_ANON_KEY=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

2. Make sure both values are NOT empty
3. No extra spaces or quotes around values
4. File is in project root directory

**Current Status:** ✅ Verified - Credentials are present

---

### **Fix 2: Clean and Rebuild** 🔨

**In Android Studio:**
1. Click **Build** menu
2. Click **Clean Project** (wait for completion)
3. Click **Build** menu again
4. Click **Rebuild Project**
5. Wait for build to complete

**Via Command Line:**
```bash
cd C:\Users\CortanatechSolutions\AndroidStudioProjects\medialert_project\jither_android_app\medialert_project

# Clean
./gradlew clean

# Rebuild
./gradlew assembleDebug
```

---

### **Fix 3: Clear App Data** 🗑️

**On Device/Emulator:**
1. Go to **Settings**
2. **Apps** or **Applications**
3. Find **MediAlert**
4. Click **Storage**
5. Click **Clear Data** (NOT Clear Cache)
6. Confirm

**Via ADB:**
```bash
adb shell pm clear com.example.medialert_project
```

---

### **Fix 4: Sync Gradle Files** 🔄

1. Open **Android Studio**
2. Click **File** → **Sync Project with Gradle Files**
3. Wait for sync to complete
4. Look for errors in Build window

---

### **Fix 5: Invalidate Caches** 🔄

1. Open **Android Studio**
2. Click **File** → **Invalidate Caches / Restart**
3. Select **Invalidate and Restart**
4. Wait for IDE to restart
5. Let Gradle sync complete

---

## Checking Logcat for Actual Error

To see the exact crash reason:

### **Method 1: Android Studio Logcat**
1. Connect device/start emulator
2. Open **Logcat** tab (bottom of Android Studio)
3. Filter: `package:com.example.medialert_project`
4. Run the app
5. Look for red error messages

### **Method 2: Command Line**
```bash
# Connect device first
adb logcat | grep "medialert\|AndroidRuntime\|FATAL"
```

### **Common Error Messages:**

**Error 1: Missing BuildConfig**
```
java.lang.IllegalStateException: SUPABASE_URL is not configured
```
**Fix:** Clean and rebuild project

**Error 2: Supabase Client Failure**
```
io.github.jan.supabase.exceptions.SupabaseEncodingException
```
**Fix:** Check network, verify credentials

**Error 3: Hilt Injection Failure**
```
dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper cannot be cast to...
```
**Fix:** Clean project, invalidate caches

---

## Prevention Checklist

Before running the app, verify:

- [ ] `local.properties` has SUPABASE_URL
- [ ] `local.properties` has SUPABASE_ANON_KEY
- [ ] Project has been cleaned and rebuilt
- [ ] Gradle sync completed successfully
- [ ] No build errors in Build window
- [ ] Internet connection is working
- [ ] Device/emulator has internet access

---

## Quick Test

After applying fixes, test with this:

1. **Uninstall** old app completely
2. **Clean project**: `Build → Clean Project`
3. **Rebuild**: `Build → Rebuild Project`
4. **Install**: Run button or `./gradlew installDebug`
5. **Launch** app
6. **Check Logcat** for any errors

---

## Still Crashing?

If the app still crashes after all fixes:

### **Get Full Stack Trace:**
```bash
adb logcat *:E | grep -A 50 "FATAL EXCEPTION"
```

### **Common Additional Issues:**

#### **Issue: Kotlin Serialization**
```
kotlinx.serialization.SerializationException
```
**Fix:**
Add to `build.gradle.kts`:
```kotlin
plugins {
    id("kotlinx-serialization")
}
```

#### **Issue: Multidex**
```
java.lang.NoClassDefFoundError
```
**Fix:**
Add to `build.gradle.kts`:
```kotlin
defaultConfig {
    multiDexEnabled = true
}
```

#### **Issue: Duplicate Classes**
```
Duplicate class found in modules
```
**Fix:**
Check for conflicting dependencies

---

## Modified Files

### Files Changed in This Fix:

1. **SupabaseModule.kt** ✅
   - Added credential validation
   - Better error messages
   - Prevents silent failures

---

## Success Indicators

After fix, you should see:

✅ App launches without crash
✅ Login screen appears
✅ No "keeps stopping" error
✅ Logcat shows "D/MediAlertApplication: onCreate"

---

## Build Command Reference

```bash
# Full clean rebuild
./gradlew clean build

# Install debug version
./gradlew installDebug

# Run app (if device connected)
./gradlew installDebug && adb shell am start -n com.example.medialert_project/.ui.main.MainActivity

# View logs
adb logcat -c && adb logcat *:E
```

---

## Emergency: Rollback Changes

If the fix makes it worse:

```bash
git diff HEAD~1 HEAD -- app/src/main/java/com/example/medialert_project/di/SupabaseModule.kt
git checkout HEAD~1 -- app/src/main/java/com/example/medialert_project/di/SupabaseModule.kt
```

---

## Summary

**Most Likely Cause:** BuildConfig not generated or Supabase client initialization

**Quick Fix:**
1. ✅ Verify `local.properties` has credentials
2. ✅ Clean project
3. ✅ Rebuild project
4. ✅ Clear app data
5. ✅ Reinstall app

**Fix Applied:** Added validation to SupabaseModule to provide clear error messages

---

**Next Steps:**
1. Clean and rebuild the project
2. Uninstall old app from device
3. Install fresh build
4. Check Logcat for any errors
5. Test login functionality

If crash persists, share the Logcat output for more specific diagnosis!
