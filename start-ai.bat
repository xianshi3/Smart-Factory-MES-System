@echo off
echo ========================================
echo   MES AI Service Quick Start
echo ========================================
echo.

cd /d "%~dp0mes-ai-service\src"

echo Starting AI Service on port 8086...
start "MES-AI-Service" python main.py

echo.
echo ========================================
echo   AI Service started: http://localhost:8086
echo   Health check: http://localhost:8086/api/v1/health
echo ========================================
echo.
pause
