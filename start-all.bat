@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

:menu
cls
echo ========================================
echo   Smart Factory MES - Unified Launcher
echo ========================================
echo.
echo [1] 启动所有服务
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
echo.
call :start_service "Docker" "docker ps >nul 2>&1" "echo   [SKIP] Docker already running" "call start-docker.bat"
call :check_port 3306 "MySQL"
call :check_port 6379 "Redis"
echo.
echo Starting Backend...
call start-backend.bat
call :wait_java 8081 "Auth"
call :wait_java 8082 "WorkOrder"
call :wait_java 8083 "Process"
call :wait_java 8084 "Quality"
call :wait_java 8085 "Dashboard"
echo.
call :start_service "AI Service" "netstat -ano | findstr ':8086' >nul" "echo   [SKIP] AI Service already running" "call start-ai.bat"
call :start_service ".NET Gateway" "netstat -ano | findstr ':5000' >nul" "echo   [SKIP] .NET Gateway already running" "call start-gateway-dotnet.bat"
echo.
echo ========================================
echo   All services started!
echo ========================================
pause
goto menu

:start_docker
echo Starting Docker...
call start-docker.bat
call :wait_port 3306 "MySQL"
call :wait_port 6379 "Redis"
pause
goto menu

:start_backend
echo Starting Backend...
call start-backend.bat
call :wait_java 8081 "Auth"
call :wait_java 8082 "WorkOrder"
call :wait_java 8083 "Process"
call :wait_java 8084 "Quality"
call :wait_java 8085 "Dashboard"
pause
goto menu

:start_ai
call :start_service "AI Service" "netstat -ano | findstr ':8086' >nul" "echo   [SKIP] AI Service already running" "call start-ai.bat"
pause
goto menu

:start_gateway
call :start_service ".NET Gateway" "netstat -ano | findstr ':5000' >nul" "echo   [SKIP] .NET Gateway already running" "call start-gateway-dotnet.bat"
pause
goto menu

:stop_all
echo Stopping all services...
call stop-backend.bat
call stop-docker.bat
echo.
echo ========================================
echo   All services stopped!
echo ========================================
pause
goto menu

:status
cls
echo ========================================
echo   Service Status
echo ========================================
echo.
echo ^| Port    ^| Service          ^| Status
echo ^+========^|==================^+========
call :show_status 3000 "Frontend"
call :show_status 3306 "MySQL"
call :show_status 6379 "Redis"
call :show_status 8081 "Auth"
call :show_status 8082 "WorkOrder"
call :show_status 8083 "Process"
call :show_status 8084 "Quality"
call :show_status 8085 "Dashboard"
call :show_status 8086 "AI Service"
call :show_status 5000 ".NET Gateway"
call :show_status 1883 "MQTT"
call :show_status 9092 "Kafka"
echo.
echo ========================================
echo   Last updated: %date% %time%
echo ========================================
pause
goto menu

:show_status
set port=%1
set name=%2
set result=OFFLINE
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":%port% " ^| findstr "LISTENING"') do set result=ONLINE
if "%result%"==ONLINE (
    echo ^| %port%     ^| %name%              ^| [GREEN] Running
) else (
    echo ^| %port%     ^| %name%              ^| [GRAY]  Stopped
)
exit /b

:start_service
set name=%1
set check=%2
set skip=%3
set start=%4
%check%
if !errorlevel!==0 (
    %skip%
) else (
    echo Starting !name!...
    %start%
)
exit /b

:check_port
set port=%1
set name=%2
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":%port% " ^| findstr "LISTENING"') do (
    echo   [SKIP] %name% already running
    exit /b
)
echo Starting %name%...
exit /b

:wait_port
set port=%1
set name=%2
echo Waiting for %name%...
:wait_port_loop
ping -n 2 127.0.0.1 >nul
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":%port% " ^| findstr "LISTENING"') do goto wait_port_done
goto wait_port_loop
:wait_port_done
echo   [OK] %name% started
exit /b

:wait_java
set port=%1
set name=%2
echo Waiting for %name%...
:wait_java_loop
ping -n 2 127.0.0.1 >nul
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":%port% " ^| findstr "LISTENING"') do goto wait_java_done
goto wait_java_loop
:wait_java_done
echo   [OK] %name% started
exit /b
