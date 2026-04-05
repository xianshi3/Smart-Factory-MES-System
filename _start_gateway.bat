@echo off
chcp 65001 >nul
cd /d "D:\Engineering-Project\Smart-Factory-MES-System"
dotnet run --project mes-device-gateway\src\MesDeviceGateway\MesDeviceGateway.csproj
pause
