@echo off
setlocal enabledelayedexpansion
set "ROOT=D:\Engineering-Project\Smart-Factory-MES-System"

:menu
cls
echo ========================================
echo   Smart Factory MES - Unified Launcher
echo ========================================
echo.
echo [1] Start All Services
echo [2] Start Docker
echo [3] Start Backend
echo [4] Start AI Service
echo [5] Start .NET Gateway
echo [6] Stop All Services
echo [7] View Status
echo.
echo [0] Exit
echo.
set /p choice=Select:

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
echo Starting Docker...
call :start_docker2
echo.
echo Starting Backend...
call :build_java
start "MES-Auth" cmd /k "cd /d %ROOT%\mes-auth ^&^& mvn spring-boot:run -DskipTests"
start "MES-WorkOrder" cmd /k "cd /d %ROOT%\mes-workorder ^&^& mvn spring-boot:run -DskipTests"
start "MES-Process" cmd /k "cd /d %ROOT%\mes-process ^&^& mvn spring-boot:run -DskipTests"
start "MES-Quality" cmd /k "cd /d %ROOT%\mes-quality ^&^& mvn spring-boot:run -DskipTests"
start "MES-Dashboard" cmd /k "cd /d %ROOT%\mes-dashboard ^&^& mvn spring-boot:run -DskipTests"
call :wait_java 8081 "Auth"
call :wait_java 8082 "WorkOrder"
call :wait_java 8083 "Process"
call :wait_java 8084 "Quality"
call :wait_java 8085 "Dashboard"
echo.
call :start_svc ":8086" "AI Service" "python mes-ai-service/src/main.py"
call :start_svc ":5000" ".NET Gateway" "dotnet run --project mes-device-gateway/src/MesDeviceGateway/MesDeviceGateway.csproj"
echo.
echo ========================================
echo All services started!
echo ========================================
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
start "MES-Auth" cmd /k "cd /d %ROOT%\mes-auth ^&^& mvn spring-boot:run -DskipTests"
start "MES-WorkOrder" cmd /k "cd /d %ROOT%\mes-workorder ^&^& mvn spring-boot:run -DskipTests"
start "MES-Process" cmd /k "cd /d %ROOT%\mes-process ^&^& mvn spring-boot:run -DskipTests"
start "MES-Quality" cmd /k "cd /d %ROOT%\mes-quality ^&^& mvn spring-boot:run -DskipTests"
start "MES-Dashboard" cmd /k "cd /d %ROOT%\mes-dashboard ^&^& mvn spring-boot:run -DskipTests"
call :wait_java 8081 "Auth"
call :wait_java 8082 "WorkOrder"
call :wait_java 8083 "Process"
call :wait_java 8084 "Quality"
call :wait_java 8085 "Dashboard"
pause
goto menu

:build_java
echo Building Java services...
call mvn clean package -DskipTests -q
exit /b

:start_ai
call :start_svc ":8086" "AI Service" "python mes-ai-service/src/main.py"
pause
goto menu

:start_gateway
call :start_svc ":5000" ".NET Gateway" "dotnet run --project mes-device-gateway/src/MesDeviceGateway/MesDeviceGateway.csproj"
pause
goto menu

:stop_all
echo Stopping all services...
echo.
echo Stopping .NET Gateway...
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":5000 " ^| findstr "LISTENING"') do taskkill /F /PID %%a >nul 2>&1
echo Stopping AI Service...
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":8086 " ^| findstr "LISTENING"') do taskkill /F /PID %%a >nul 2>&1
echo Stopping Backend...
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":8081 " ^| findstr "LISTENING"') do taskkill /F /PID %%a >nul 2>&1
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":8082 " ^| findstr "LISTENING"') do taskkill /F /PID %%a >nul 2>&1
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":8083 " ^| findstr "LISTENING"') do taskkill /F /PID %%a >nul 2>&1
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":8084 " ^| findstr "LISTENING"') do taskkill /F /PID %%a >nul 2>&1
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":8085 " ^| findstr "LISTENING"') do taskkill /F /PID %%a >nul 2>&1
echo Stopping Docker...
docker compose down 2>nul
echo.
echo ========================================
echo All services stopped!
echo ========================================
pause
goto menu

:status
cls
echo ========================================
echo   Service Status
echo ========================================
echo.
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
echo.
pause
goto menu

:show_status
set port=%1
set name=%2
set result=OFFLINE
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":%port% " ^| findstr "LISTENING"') do set result=ONLINE
if "%result%"==ONLINE (
    echo %port%     %name%              [RUNNING]
) else (
    echo %port%     %name%              [STOPPED]
)
exit /b

:start_svc
set portcheck=%1
set svcname=%2
set cmd=%3
for /f "tokens=5" %%a in ('netstat -ano ^| findstr "%portcheck% " ^| findstr "LISTENING"') do (
    echo [SKIP] %svcname% already running
    exit /b
)
echo Starting %svcname%...
pushd "%ROOT%"
start "%svcname%" cmd /k "%cmd%"
popd
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
echo [OK] %name% started
exit /b
