@echo off
echo ========================================
echo   Smart Factory MES - Backend Quick Start
echo ========================================
echo.

cd /d "%~dp0"

echo [1/3] Compiling project...
call mvn clean compile -q
if errorlevel 1 (
    echo ERROR: Compilation failed!
    pause
    exit /b 1
)

echo [2/3] Packaging services...
call mvn package -DskipTests -q
if errorlevel 1 (
    echo ERROR: Packaging failed!
    pause
    exit /b 1
)

echo [3/3] Starting microservices...
echo.

start "MES-Gateway" java -jar mes-gateway\target\mes-gateway-1.0.0-SNAPSHOT.jar
timeout /t 5 /nobreak > nul

start "MES-Auth" java -jar mes-auth\target\mes-auth-1.0.0-SNAPSHOT.jar
timeout /t 5 /nobreak > nul

start "MES-WorkOrder" java -jar mes-workorder\target\mes-workorder-1.0.0-SNAPSHOT.jar
timeout /t 5 /nobreak > nul

start "MES-Process" java -jar mes-process\target\mes-process-1.0.0-SNAPSHOT.jar
timeout /t 5 /nobreak > nul

start "MES-Quality" java -jar mes-quality\target\mes-quality-1.0.0-SNAPSHOT.jar
timeout /t 5 /nobreak > nul

start "MES-Dashboard" java -jar mes-dashboard\target\mes-dashboard-1.0.0-SNAPSHOT.jar

echo.
echo ========================================
echo   Services started:
echo   - MES-Gateway   (9090)
echo   - MES-Auth      (8081)
echo   - MES-WorkOrder (8082)
echo   - MES-Process   (8083)
echo   - MES-Quality   (8084)
echo   - MES-Dashboard (8085)
echo ========================================
echo.
echo Press any key to stop all services...
pause > nul

call stop-backend.bat