@echo off
:: Credflow Windows Firewall Setup
:: This script configures Windows Firewall to allow inbound connections on port 80 for Apache / Credflow API.
:: Run as Administrator.

echo ========================================================
echo   Credflow Server - Windows Firewall ^& Port 80 Setup
echo ========================================================
echo.

net session >nul 2>&1
if %errorLevel% neq 0 (
    echo [ERROR] This script requires Administrator privileges.
    echo Please right-click this file and choose "Run as administrator".
    echo.
    pause
    exit /b 1
)

echo Adding Windows Firewall rule for Apache HTTP Port 80...
netsh advfirewall firewall delete rule name="Apache HTTP Server (Credflow Port 80)" >nul 2>&1
netsh advfirewall firewall add rule name="Apache HTTP Server (Credflow Port 80)" dir=in action=allow protocol=TCP localport=80

echo Adding Windows Firewall rule for Apache Binary (httpd.exe)...
netsh advfirewall firewall delete rule name="Apache HTTP Server (Credflow Binary)" >nul 2>&1
netsh advfirewall firewall add rule name="Apache HTTP Server (Credflow Binary)" dir=in action=allow program="C:\xampp\apache\bin\httpd.exe" enable=yes

echo.
echo ========================================================
echo   Firewall Rules Added Successfully!
echo ========================================================
echo.
echo Your PC IP Address(es):
ipconfig | findstr /i "IPv4"
echo.
echo Default Credflow API URL: http://172.23.176.211/credflow_api/
echo.
pause

