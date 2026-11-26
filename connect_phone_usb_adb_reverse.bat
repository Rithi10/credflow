@echo off
:: Credflow USB Tethering / ADB Reverse Setup
:: If testing on a physical phone connected via USB cable:
:: This forwards phone port 80 to PC port 80, bypassing all Wi-Fi and firewall issues!

echo ========================================================
echo   Credflow USB Connection (adb reverse tcp:80 tcp:80)
echo ========================================================
echo.

set ADB="C:\Users\SCAD LAB\AppData\Local\Android\Sdk\platform-tools\adb.exe"

if not exist %ADB% (
    set ADB=adb
)

echo Checking connected devices...
%ADB% devices

echo.
echo Setting up reverse port forwarding...
%ADB% reverse tcp:80 tcp:80

if %errorLevel% equ 0 (
    echo.
    echo ========================================================
    echo   [SUCCESS] Port 80 forwarded over USB!
    echo ========================================================
    echo In the Credflow app, tap the Server icon on Login
    echo and select preset "USB / Cable" (http://127.0.0.1/credflow_api/)
) else (
    echo.
    echo [WARNING] Could not set up adb reverse.
    echo Please make sure your phone is connected via USB and USB Debugging is ON.
)

echo.
pause

