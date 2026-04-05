@echo off
echo ========================================
echo   MES .NET Device Gateway Quick Start
echo ========================================
echo.

cd /d "%~dp0mes-device-gateway\src\MesDeviceGateway"

echo Starting .NET Device Gateway on port 5000...
start "MES-Device-Gateway" cmd /k "dotnet run"

echo.
echo ========================================
echo   Device Gateway started: http://localhost:5000
echo   MQTT: localhost:1883
echo   Kafka: localhost:9092
echo ========================================
echo.
pause
