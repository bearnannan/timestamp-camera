@echo off
echo ========================================
echo Timestamp Camera Pro - Build Fix
echo ========================================
echo.
echo Problem: AAPT2 requires elevation
echo Solution:
echo 1. Run PowerShell as Administrator
echo 2. Or create a local.properties file with the JDK path
echo.
echo Running PowerShell as Administrator...
echo.

REM Check if it's running as Administrator or not?
net session >nul 2>&1 | findstr /i "Elevated"
if errorlevel 1 (
echo ❌ Not running as Administrator
echo Please right-click PowerShell and select Run as Administrator
pause
exit /b 1
) else (
echo ✅ Running as Administrator
echo.
echo Building...
call gradlew.bat clean
call gradlew.bat assembleDebug
echo.
echo Build complete!
)

pause