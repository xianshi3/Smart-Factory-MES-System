@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

:menu
cls
echo ========================================
echo   Smart Factory MES - Unified Launcher
echo ========================================
echo.
echo [1] 启动所有服务 (Docker + Backend + AI + Gateway)
echo [2] 仅启动基础设施 (Docker)
echo [3] 仅启动后端服务 (Java)
echo [4] 仅启动AI服务 (Python)
echo [5] 仅启动.NET网关
echo [6] 停止所有服务
echo [7] 查看服务状态
echo.
echo [0] 退出
echo.
set /p choice=请选择:

if "%choice%"=="1" goto start_all
if "%choice%"=="2" goto start_docker
if "%choice%"=="3" goto start_backend
if "%choice%"=="4" goto start_ai
if "%choice%"=="5" goto start_gateway
if "%choice%"=="6" goto stop_all
if "%choice%"=="7" goto status
if "%choice%"=="0" exit
goto menu

:start_all
echo Starting all services...
call start-docker.bat
timeout /t 5 /nobreak >nul
call start-backend.bat
timeout /t 10 /nobreak >nul
call start-ai.bat
timeout /t 3 /nobreak >nul
call start-gateway-dotnet.bat
echo.
echo All services started!
pause
goto menu

:start_docker
echo Starting Docker...
call start-docker.bat
pause
goto menu

:start_backend
echo Starting Backend...
call start-backend.bat
pause
goto menu

:start_ai
echo Starting AI Service...
call start-ai.bat
pause
goto menu

:start_gateway
echo Starting .NET Gateway...
call start-gateway-dotnet.bat
pause
goto menu

:stop_all
echo Stopping all services...
call stop-backend.bat
call stop-docker.bat
echo.
echo All services stopped!
pause
goto menu

:status
echo.
echo === Service Status ===
echo.
echo Checking ports...
netstat -ano | findstr ":3306" >nul && echo [ONLINE] MySQL 3306 || echo [OFFLINE] MySQL 3306
netstat -ano | findstr ":6379" >nul && echo [ONLINE] Redis 6379 || echo [OFFLINE] Redis 6379
netstat -ano | findstr ":8081" >nul && echo [ONLINE] Auth 8081 || echo [OFFLINE] Auth 8081
netstat -ano | findstr ":8082" >nul && echo [ONLINE] WorkOrder 8082 || echo [OFFLINE] WorkOrder 8082
netstat -ano | findstr ":8083" >nul && echo [ONLINE] Process 8083 || echo [OFFLINE] Process 8083
netstat -ano | findstr ":8084" >nul && echo [ONLINE] Quality 8084 || echo [OFFLINE] Quality 8084
netstat -ano | findstr ":8085" >nul && echo [ONLINE] Dashboard 8085 || echo [OFFLINE] Dashboard 8085
netstat -ano | findstr ":8086" >nul && echo [ONLINE] AI Service 8086 || echo [OFFLINE] AI Service 8086
netstat -ano | findstr ":5000" >nul && echo [ONLINE] .NET Gateway 5000 || echo [OFFLINE] .NET Gateway 5000
netstat -ano | findstr ":3000" >nul && echo [ONLINE] Frontend 3000 || echo [OFFLINE] Frontend 3000
echo.
pause
goto menu
