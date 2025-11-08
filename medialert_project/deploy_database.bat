@echo off
echo ============================================
echo MediAlert Database Deployment Script
echo ============================================
echo.
echo This script will deploy your database to Supabase
echo Project: fbcswdzuecfiodlpgmxq
echo.

echo Checking Supabase CLI installation...
supabase --version >nul 2>&1
if errorlevel 1 (
    echo ERROR: Supabase CLI is not installed
    echo Installing Supabase CLI...
    npm install -g supabase
    if errorlevel 1 (
        echo Failed to install Supabase CLI
        echo Please install manually: npm install -g supabase
        pause
        exit /b 1
    )
)

echo.
echo Supabase CLI is installed!
echo.
echo ============================================
echo OPTION 1: Deploy via Supabase Dashboard (Manual)
echo ============================================
echo 1. Open: https://supabase.com/dashboard/project/fbcswdzuecfiodlpgmxq
echo 2. Go to SQL Editor
echo 3. Copy contents of: supabase_migrations\DEPLOY_ALL.sql
echo 4. Paste and Run
echo.
echo If that fails due to network issues, try these:
echo - Hard refresh browser (Ctrl+F5)
echo - Try different browser (Chrome/Edge/Firefox)
echo - Disable VPN/Proxy if enabled
echo - Check firewall settings
echo.
pause
echo.
echo ============================================
echo OPTION 2: Deploy via psql (Direct Connection)
echo ============================================
echo.
echo You'll need the database connection string from Supabase:
echo 1. Go to Project Settings ^> Database
echo 2. Copy the "Connection string" (under "Connection pooling")
echo 3. Paste it when prompted below
echo.
set /p "CONN_STRING=Enter your Supabase connection string (or press Enter to skip): "

if "%CONN_STRING%"=="" (
    echo.
    echo Skipping direct connection deployment
    echo Please use the Supabase Dashboard instead
    echo.
    pause
    exit /b 0
)

echo.
echo Deploying database schema...
psql "%CONN_STRING%" -f "supabase_migrations\DEPLOY_ALL.sql"

if errorlevel 1 (
    echo.
    echo Deployment failed. Possible reasons:
    echo - Invalid connection string
    echo - psql not installed
    echo - Network connectivity issue
    echo.
    echo Please use Option 1 (Supabase Dashboard) instead
) else (
    echo.
    echo ============================================
    echo SUCCESS! Database deployed successfully!
    echo ============================================
    echo.
    echo Tables created:
    echo - medicines
    echo - medicine_schedules
    echo - dose_logs
    echo.
    echo Verify at: https://supabase.com/dashboard/project/fbcswdzuecfiodlpgmxq/editor
)

echo.
pause
