@echo off
setlocal enabledelayedexpansion
set "ROOT=%~dp0"

:menu
cls
echo ========================================
echo   Smart Factory MES - Unified Launcher
echo ========================================
echo.
echo [1] Start All Services
echo [2] Start Docker (MySQL + Redis)
echo [3] Start Backend (Java)
echo [4] Start AI Service (Python)
echo [5] Start .NET Gateway
echo [6] Start Frontend (Vue)
echo [7] Start Device Simulator
echo [8] Clean Unnecessary Files
echo [9] Stop All Services
echo [10] View Status
echo.
echo [0] Exit
echo.
set /p choice=Select:

if "%choice%"=="1" goto start_all
if "%choice%"=="2" goto start_docker
if "%choice%"=="3" goto start_backend
if "%choice%"=="4" goto start_ai
if "%choice%"=="5" goto start_gateway
if "%choice%"=="6" goto start_frontend
if "%choice%"=="7" goto start_simulator
if "%choice%"=="8" goto clean
if "%choice%"=="9" goto stop_all
if "%choice%"=="10" goto status
if "%choice%"=="0" exit
goto menu

:start_all
echo Starting all services...
echo.
echo Starting Docker...
call :start_docker2
echo.
echo Starting Backend...
call :build_java
start "MES-Auth" cmd /k "cd /d "%ROOT%\mes-auth" && mvn spring-boot:run -DskipTests"
start "MES-Gateway" cmd /k "cd /d "%ROOT%\mes-gateway" && mvn spring-boot:run -DskipTests"
start "MES-WorkOrder" cmd /k "cd /d "%ROOT%\mes-workorder" && mvn spring-boot:run -DskipTests"
start "MES-Process" cmd /k "cd /d "%ROOT%\mes-process" && mvn spring-boot:run -DskipTests"
start "MES-Quality" cmd /k "cd /d "%ROOT%\mes-quality" && mvn spring-boot:run -DskipTests"
start "MES-Dashboard" cmd /k "cd /d "%ROOT%\mes-dashboard" && mvn spring-boot:run -DskipTests"
call :wait_java 8081 "Auth"
call :wait_java 9090 "Gateway"
call :wait_java 8082 "WorkOrder"
call :wait_java 8083 "Process"
call :wait_java 8084 "Quality"
call :wait_java 8085 "Dashboard"
echo.
call :start_ai_svc
call :start_gateway_svc
call :start_frontend_svc
call :start_simulator_svc
echo.
echo ========================================
echo All services started!
echo ========================================
echo Access: http://localhost:3000
echo.
pause
goto menu

:start_docker
call :start_docker2
pause
goto menu

:start_docker2
echo Checking existing containers...
docker ps -a --filter "name=mes-mysql" --filter "name=mes-redis" --format "{{.Names}}" > containers.txt 2>nul
findstr /C:"mes-mysql" containers.txt >nul 2>&1
if %errorlevel% equ 0 (
    echo Found existing containers, removing...
    docker rm -f mes-mysql mes-redis 2>nul
    timeout /t 2 /nobreak >nul
)
echo Starting MySQL and Redis...
docker compose up -d
echo Waiting for services...
timeout /t 15 /nobreak >nul
echo Initializing database...
docker exec mes-mysql mysql -uroot -proot -e "DROP DATABASE IF EXISTS mes_db; CREATE DATABASE mes_db" 2>nul
docker cp sql/init.sql mes-mysql:/tmp/init.sql 2>nul
docker exec mes-mysql mysql -uroot -proot mes_db -e "source /tmp/init.sql" 2>nul
del containers.txt 2>nul
echo [OK] Docker started
exit /b

:start_backend
echo Starting Backend...
call :build_java
start "MES-Auth" cmd /k "cd /d "%ROOT%\mes-auth" && mvn spring-boot:run -DskipTests"
start "MES-Gateway" cmd /k "cd /d "%ROOT%\mes-gateway" && mvn spring-boot:run -DskipTests"
start "MES-WorkOrder" cmd /k "cd /d "%ROOT%\mes-workorder" && mvn spring-boot:run -DskipTests"
start "MES-Process" cmd /k "cd /d "%ROOT%\mes-process" && mvn spring-boot:run -DskipTests"
start "MES-Quality" cmd /k "cd /d "%ROOT%\mes-quality" && mvn spring-boot:run -DskipTests"
start "MES-Dashboard" cmd /k "cd /d "%ROOT%\mes-dashboard" && mvn spring-boot:run -DskipTests"
call :wait_java 8081 "Auth"
call :wait_java 9090 "Gateway"
call :wait_java 8082 "WorkOrder"
call :wait_java 8083 "Process"
call :wait_java 8084 "Quality"
call :wait_java 8085 "Dashboard"
echo [OK] Backend started
pause
goto menu

:build_java
echo Building Java services...
call mvn clean package -DskipTests -q
exit /b

:start_ai
call :start_ai_svc
pause
goto menu

:start_ai_svc
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":8086 " ^| findstr "LISTENING"') do (
    echo [SKIP] AI Service already running
    exit /b
)
echo Starting AI Service...
if exist "%ROOT%\mes-ai-service\.venv\Scripts\python.exe" (
    start "AI-Service" cmd /k "cd /d "%ROOT%\mes-ai-service" && "%ROOT%\mes-ai-service\.venv\Scripts\python.exe" -m src.main"
) else (
    start "AI-Service" cmd /k "cd /d "%ROOT%\mes-ai-service" && python -m src.main"
)
exit /b

:start_gateway_svc
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":5000 " ^| findstr "LISTENING"') do (
    echo [SKIP] .NET Gateway already running
    exit /b
)
echo Starting .NET Gateway...
start "NET-Gateway" cmd /k "cd /d "%ROOT%" && dotnet run --project mes-device-gateway\src\MesDeviceGateway\MesDeviceGateway.csproj"
exit /b

:start_gateway
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":5000 " ^| findstr "LISTENING"') do (
    echo [SKIP] .NET Gateway already running
    pause
    goto menu
)
echo Starting .NET Gateway...
start "NET-Gateway" cmd /k "cd /d "%ROOT%" && dotnet run --project mes-device-gateway\src\MesDeviceGateway\MesDeviceGateway.csproj"
pause
goto menu

:start_frontend
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":3000 " ^| findstr "LISTENING"') do (
    echo [SKIP] Frontend already running
    pause
    goto menu
)
echo Starting Frontend...
start "MES-Frontend" cmd /k "cd /d "%ROOT%\mes-frontend" && npm run dev"
pause
goto menu

:start_frontend_svc
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":3000 " ^| findstr "LISTENING"') do (
    echo [SKIP] Frontend already running
    exit /b
)
echo Starting Frontend...
start "MES-Frontend" cmd /k "cd /d "%ROOT%\mes-frontend" && npm run dev"
exit /b

:start_simulator
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":8883 " ^| findstr "LISTENING"') do (
    echo [SKIP] Device Simulator already running
    pause
    goto menu
)
echo Starting Device Simulator...
start "Device-Simulator" cmd /k "cd /d "%ROOT%\mes-device-simulator-wpf" && dotnet run"
pause
goto menu

:start_simulator_svc
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":8883 " ^| findstr "LISTENING"') do (
    echo [SKIP] Device Simulator already running
    exit /b
)
echo Starting Device Simulator...
start "Device-Simulator" cmd /k "cd /d "%ROOT%\mes-device-simulator-wpf" && dotnet run"
exit /b

:wait_java
set port=%1
set name=%2
echo Waiting for %name% on port %port%...
:wait_java_loop
ping -n 2 127.0.0.1 >nul
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":%port% " ^| findstr "LISTENING"') do goto wait_java_done
goto wait_java_loop
:wait_java_done
echo [OK] %name% started on port %port%
exit /b

:stop_all
echo Stopping all services...
taskkill /F /FI "WINDOWTITLE eq MES-Auth*" 2>nul
taskkill /F /FI "WINDOWTITLE eq MES-Gateway*" 2>nul
taskkill /F /FI "WINDOWTITLE eq MES-WorkOrder*" 2>nul
taskkill /F /FI "WINDOWTITLE eq MES-Process*" 2>nul
taskkill /F /FI "WINDOWTITLE eq MES-Quality*" 2>nul
taskkill /F /FI "WINDOWTITLE eq MES-Dashboard*" 2>nul
taskkill /F /FI "WINDOWTITLE eq AI-Service*" 2>nul
taskkill /F /FI "WINDOWTITLE eq NET-Gateway*" 2>nul
taskkill /F /FI "WINDOWTITLE eq MES-Frontend*" 2>nul
taskkill /F /FI "WINDOWTITLE eq Device-Simulator*" 2>nul
echo.
echo Note: Docker containers still running. Use "docker compose down" to stop.
echo [OK] Services stopped
pause
goto menu

:clean
echo Cleaning up unnecessary files...
echo.
echo Removing .opencode folder...
rd /s /q ".opencode" 2>nul
echo Removing Python cache files...
del /s /q "mes-ai-service\src\__pycache__\*.pyc" 2>nul
rd /s /q "mes-ai-service\src\__pycache__" 2>nul
rd /s /q "mes-ai-service\src\services\__pycache__" 2>nul
rd /s /q "mes-ai-service\src\models\__pycache__" 2>nul
rd /s /q "mes-ai-service\src\schemas\__pycache__" 2>nul
rd /s /q "mes-ai-service\src\utils\__pycache__" 2>nul
rd /s /q "mes-ai-service\src\router\__pycache__" 2>nul
echo Removing .venv (recreate with: python -m venv .venv)...
rd /s /q "mes-ai-service\.venv" 2>nul
echo Removing .idea folders...
rd /s /q "mes-ai-service\.idea" 2>nul
echo Removing Java Maven target (keep JARs)...
rd /s /q "mes-auth\target\classes" 2>nul
rd /s /q "mes-auth\target\generated-sources" 2>nul
rd /s /q "mes-auth\target\maven-archiver" 2>nul
rd /s /q "mes-auth\target\maven-status" 2>nul
rd /s /q "mes-workorder\target\classes" 2>nul
rd /s /q "mes-workorder\target\generated-sources" 2>nul
rd /s /q "mes-workorder\target\maven-archiver" 2>nul
rd /s /q "mes-workorder\target\maven-status" 2>nul
rd /s /q "mes-process\target\classes" 2>nul
rd /s /q "mes-process\target\generated-sources" 2>nul
rd /s /q "mes-process\target\maven-archiver" 2>nul
rd /s /q "mes-process\target\maven-status" 2>nul
rd /s /q "mes-quality\target\classes" 2>nul
rd /s /q "mes-quality\target\generated-sources" 2>nul
rd /s /q "mes-quality\target\maven-archiver" 2>nul
rd /s /q "mes-quality\target\maven-status" 2>nul
rd /s /q "mes-dashboard\target\classes" 2>nul
rd /s /q "mes-dashboard\target\generated-sources" 2>nul
rd /s /q "mes-dashboard\target\maven-archiver" 2>nul
rd /s /q "mes-dashboard\target\maven-status" 2>nul
rd /s /q "mes-gateway\target\classes" 2>nul
rd /s /q "mes-gateway\target\generated-sources" 2>nul
rd /s /q "mes-gateway\target\maven-archiver" 2>nul
rd /s /q "mes-gateway\target\maven-status" 2>nul
rd /s /q "mes-common\target\classes" 2>nul
rd /s /q "mes-common\target\generated-sources" 2>nul
rd /s /q "mes-common\target\maven-archiver" 2>nul
rd /s /q "mes-common\target\maven-status" 2>nul
echo Removing .NET build artifacts...
rd /s /q "mes-device-gateway\src\MesDeviceGateway\bin" 2>nul
rd /s /q "mes-device-gateway\src\MesDeviceGateway\obj" 2>nul
echo.
echo [OK] Cleanup completed!
echo.
pause
goto menu

:status
echo ========================================
echo   Service Status
echo ========================================
echo.
echo === Backend (Java) ===
netstat -ano | findstr ":8081 " | findstr "LISTENING" >nul && echo [8081] Auth: RUNNING || echo [8081] Auth: STOPPED
netstat -ano | findstr ":8082 " | findstr "LISTENING" >nul && echo [8082] WorkOrder: RUNNING || echo [8082] WorkOrder: STOPPED
netstat -ano | findstr ":8083 " | findstr "LISTENING" >nul && echo [8083] Process: RUNNING || echo [8083] Process: STOPPED
netstat -ano | findstr ":8084 " | findstr "LISTENING" >nul && echo [8084] Quality: RUNNING || echo [8084] Quality: STOPPED
netstat -ano | findstr ":8085 " | findstr "LISTENING" >nul && echo [8085] Dashboard: RUNNING || echo [8085] Dashboard: STOPPED
netstat -ano | findstr ":9090 " | findstr "LISTENING" >nul && echo [9090] Gateway: RUNNING || echo [9090] Gateway: STOPPED
echo.
echo === AI Service (Python) ===
netstat -ano | findstr ":8086 " | findstr "LISTENING" >nul && echo [8086] AI Service: RUNNING || echo [8086] AI Service: STOPPED
echo.
echo === .NET Gateway ===
netstat -ano | findstr ":5000 " | findstr "LISTENING" >nul && echo [5000] Device Gateway: RUNNING || echo [5000] Device Gateway: STOPPED
echo.
echo === Frontend (Node.js) ===
netstat -ano | findstr ":3000 " | findstr "LISTENING" >nul && echo [3000] Frontend: RUNNING || echo [3000] Frontend: STOPPED
echo.
echo === Device Simulator (Node.js) ===
netstat -ano | findstr ":8883 " | findstr "LISTENING" >nul && echo [8883] Device Simulator: RUNNING || echo [8883] Device Simulator: STOPPED
echo.
echo === Docker ===
docker ps --filter "name=mes-" --format "table {{.Names}}\t{{.Status}}"
echo.
pause
goto menu