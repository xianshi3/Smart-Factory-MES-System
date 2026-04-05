@echo off
echo ========================================
echo   Smart Factory MES - Backend Quick Stop
echo ========================================
echo.
echo Stopping all Java microservices...
echo.

taskkill /F /FI "WINDOWTITLE eq MES-Gateway*" >nul 2>&1
taskkill /F /FI "WINDOWTITLE eq MES-Auth*" >nul 2>&1
taskkill /F /FI "WINDOWTITLE eq MES-WorkOrder*" >nul 2>&1
taskkill /F /FI "WINDOWTITLE eq MES-Process*" >nul 2>&1
taskkill /F /FI "WINDOWTITLE eq MES-Quality*" >nul 2>&1
taskkill /F /FI "WINDOWTITLE eq MES-Dashboard*" >nul 2>&1

taskkill /F /IM "java.exe" >nul 2>&1

echo ========================================
echo   All backend services stopped!
echo ========================================