@echo off
echo ========================================
echo   Smart Factory MES - Clean Utility
echo ========================================
echo.
echo Cleaning up unnecessary files...
echo.

echo [1/6] Removing .opencode folder...
rd /s /q ".opencode" 2>nul

echo [2/6] Removing Python cache files...
del /s /q "mes-ai-service\src\__pycache__\*.pyc" 2>nul
del /s /q "mes-ai-service\src\__pycache__\*.pyo" 2>nul
del /s /q "mes-ai-service\src\services\__pycache__\*.pyc" 2>nul
del /s /q "mes-ai-service\src\models\__pycache__\*.pyc" 2>nul
del /s /q "mes-ai-service\src\schemas\__pycache__\*.pyc" 2>nul
del /s /q "mes-ai-service\src\utils\__pycache__\*.pyc" 2>nul
rd /s /q "mes-ai-service\src\__pycache__" 2>nul
rd /s /q "mes-ai-service\src\services\__pycache__" 2>nul
rd /s /q "mes-ai-service\src\models\__pycache__" 2>nul
rd /s /q "mes-ai-service\src\schemas\__pycache__" 2>nul
rd /s /q "mes-ai-service\src\utils\__pycache__" 2>nul

echo [3/6] Removing .venv (recreate with: python -m venv .venv)...
rd /s /q "mes-ai-service\.venv" 2>nul

echo [4/6] Removing .idea folders...
rd /s /q "mes-ai-service\.idea" 2>nul

echo [5/6] Removing Java Maven target (keep JARs only)...
for /d %%d in (mes-auth\target mes-workorder\target mes-process\target mes-quality\target mes-dashboard\target mes-gateway\target mes-common\target) do (
    if exist "%%d" (
        del /s /q "%%d\*.jar.original" 2>nul
        rd /s /q "%%d\classes" 2>nul
        rd /s /q "%%d\generated-sources" 2>nul
        rd /s /q "%%d\maven-archiver" 2>nul
        rd /s /q "%%d\maven-status" 2>nul
    )
)

echo [6/6] Removing .NET build artifacts...
rd /s /q "mes-device-gateway\src\MesDeviceGateway\bin" 2>nul
rd /s /q "mes-device-gateway\src\MesDeviceGateway\obj" 2>nul

echo.
echo ========================================
echo Cleanup completed!
echo ========================================
echo.
echo Note: Run "mvn clean package" to rebuild Java
echo Note: Run "cd mes-ai-service && python -m venv .venv" to recreate Python venv
echo Note: Run "npm install" in mes-frontend to install Node modules
echo.
pause