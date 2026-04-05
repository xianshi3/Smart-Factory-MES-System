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
call :start_ai_svc
call :start_gateway_svc
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
call :start_ai_svc
pause
goto menu

:start_ai_svc
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":8086 " ^| findstr "LISTENING"') do (
    echo [SKIP] AI Service already running
    exit /b
)
echo Starting AI Service...
start "AI-Service" cmd /k "cd /d D:\Engineering-Project\Smart-Factory-MES-System ^&^& python mes-ai-service/src/main.py"
exit /b

:start_gateway
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":5000 " ^| findstr "LISTENING"') do (
    echo [SKIP] .NET Gateway already running
    pause
    goto menu
)
echo Starting .NET Gateway...
start "" cmd /k "_start_gateway.bat"
pause
goto menu

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
