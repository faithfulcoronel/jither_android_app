# How to Rebuild MediAlert in Android Studio

## ✅ EASIEST METHOD - Use Android Studio

Since the command-line script requires JAVA_HOME setup, it's much easier to rebuild directly in Android Studio.

---

## 📝 Step-by-Step Instructions

### **Step 1: Open Android Studio**
1. Open **Android Studio**
2. Wait for project to load
3. Let Gradle sync complete (watch progress bar at bottom)

---

### **Step 2: Clean Project**
1. Click **Build** in the top menu
2. Click **Clean Project**
3. Wait for "BUILD SUCCESSFUL" message (bottom right)

![Clean Project](https://developer.android.com/static/studio/images/build-menu.png)

---

### **Step 3: Rebuild Project**
1. Click **Build** in the top menu
2. Click **Rebuild Project**
3. Wait for build to complete (can take 1-3 minutes)
4. Look for "BUILD SUCCESSFUL" message

**Important:** If you see any errors:
- Red text in Build window = error
- Share the error message for help

---

### **Step 4: Uninstall Old App**

#### **Option A: On Device/Emulator**
1. Open **Settings**
2. Go to **Apps**
3. Find **MediAlert**
4. Tap **Uninstall**

#### **Option B: Via Android Studio**
1. Connect device/start emulator
2. In Android Studio, click **Run** menu
3. Click **Stop** (if running)
4. Manually uninstall on device

---

### **Step 5: Install Fresh Build**
1. Make sure device/emulator is connected
2. Click the green **Run** button (▶️) at the top
3. Or press **Shift+F10**
4. Wait for installation
5. App will launch automatically

---

## ✅ Success Indicators

You should see:
- ✅ Build window shows "BUILD SUCCESSFUL"
- ✅ App installs on device
- ✅ App launches to login screen
- ✅ **NO "keeps stopping" error**

---

## 🔍 If Build Fails

### **Common Error 1: Gradle Sync Failed**
**Fix:**
1. Click **File** → **Sync Project with Gradle Files**
2. Wait for sync
3. Try rebuild again

### **Common Error 2: SDK Not Found**
**Fix:**
1. Click **Tools** → **SDK Manager**
2. Install Android SDK 35 (or latest)
3. Try rebuild again

### **Common Error 3: Build Cache Issues**
**Fix:**
1. Click **File** → **Invalidate Caches / Restart**
2. Click **Invalidate and Restart**
3. Wait for restart
4. Try rebuild again

---

## 🧪 Testing After Rebuild

1. **App launches:** ✅ Should open without crash
2. **Shows login screen:** ✅ UI should appear
3. **No "keeps stopping":** ✅ No error dialog

If app crashes:
1. Open **Logcat** tab (bottom of Android Studio)
2. Filter by: `package:com.example.medialert_project`
3. Look for red error lines
4. Share the error message

---

## 📱 Quick Test Checklist

After installing:
- [ ] App launches successfully
- [ ] Login screen appears
- [ ] Can type in email field
- [ ] Can type in password field
- [ ] No crashes when switching to Sign Up mode

---

## 🆘 Still Crashing?

If app still crashes after rebuild:

### **Get Crash Log:**
1. In Android Studio, open **Logcat** tab
2. Reproduce the crash
3. Copy the red error text
4. Look for "FATAL EXCEPTION" or "AndroidRuntime"

### **Share This Info:**
- What were you doing when it crashed?
- Any error message shown?
- Logcat output (red text)

---

## ⚡ Quick Reference

| Action | Menu Path |
|--------|-----------|
| Clean | Build → Clean Project |
| Rebuild | Build → Rebuild Project |
| Sync Gradle | File → Sync Project with Gradle Files |
| Invalidate Caches | File → Invalidate Caches / Restart |
| Run App | Run → Run 'app' (or press ▶️) |
| View Logs | View → Tool Windows → Logcat |

---

## 💡 Pro Tips

1. **Always clean before rebuild** after code changes
2. **Check Build window** for errors (bottom of screen)
3. **Watch Logcat** when testing for warnings
4. **Uninstall old app** for cleanest install

---

## ✅ Expected Timeline

- Clean Project: 5-10 seconds
- Rebuild Project: 1-3 minutes (first time may be longer)
- Install: 10-30 seconds
- **Total: ~5 minutes**

---

## 🎯 Summary

**Instead of command line:**
1. Open Android Studio
2. Build → Clean Project
3. Build → Rebuild Project
4. Uninstall old app from device
5. Click Run (▶️)
6. Test the app

**That's it!** Android Studio handles all the Java/Gradle setup automatically.

---

**Need help at any step? Let me know which error you're seeing!**
