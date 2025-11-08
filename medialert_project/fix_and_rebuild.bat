@echo off
echo ============================================
echo MediAlert - Fix and Rebuild Script
echo ============================================
echo.
echo This script will:
echo 1. Clean the project
echo 2. Rebuild with crash fixes
echo 3. Uninstall old app
echo 4. Install fresh build
echo.
echo ============================================
pause

echo.
echo [1/5] Cleaning project...
call gradlew clean
if errorlevel 1 (
    echo ERROR: Clean failed!
    pause
    exit /b 1
)
echo ✓ Project cleaned

echo.
echo [2/5] Building debug APK...
call gradlew assembleDebug
if errorlevel 1 (
    echo ERROR: Build failed! Check error messages above.
    pause
    exit /b 1
)
echo ✓ Build successful

echo.
echo [3/5] Checking for connected device...
adb devices
if errorlevel 1 (
    echo ERROR: ADB not found or no device connected
    echo.
    echo Please connect your device or start an emulator
    pause
    exit /b 1
)

echo.
echo [4/5] Uninstalling old app...
adb uninstall com.example.medialert_project
echo ✓ Old app uninstalled (or wasn't installed)

echo.
echo [5/5] Installing fresh build...
adb install -r app\build\outputs\apk\debug\app-debug.apk
if errorlevel 1 (
    echo ERROR: Installation failed!
    pause
    exit /b 1
)
echo ✓ App installed successfully

echo.
echo ============================================
echo SUCCESS! App rebuilt and installed
echo ============================================
echo.
echo The app should now launch without crashing.
echo.
echo If it still crashes:
echo 1. Clear app data: Settings → Apps → MediAlert → Clear Data
echo 2. Check logcat: adb logcat ^| findstr "medialert"
echo.
pause
