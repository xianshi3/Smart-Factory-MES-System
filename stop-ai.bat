@echo off
echo ========================================
echo   MES AI Service Quick Stop
echo ========================================
echo.

echo Stopping AI Service...
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":8086"') do (
    taskkill /F /PID %%a >nul 2>&1
)

echo.
echo ========================================
echo   AI Service stopped
echo ========================================
echo.
pause
