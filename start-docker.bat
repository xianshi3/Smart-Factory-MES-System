@echo off
echo ========================================
echo   Smart Factory MES - Docker Quick Start
echo ========================================
echo.

echo Checking existing containers...
docker ps -a --filter "name=mes-mysql" --filter "name=mes-redis" --format "{{.Names}}" > containers.txt
findstr /C:"mes-mysql" containers.txt >nul
if %errorlevel% equ 0 (
    echo Found existing containers, removing...
    docker rm -f mes-mysql mes-redis 2>nul
    timeout /t 2 /nobreak > nul
)

echo Starting MySQL and Redis containers...
docker compose up -d
echo.
echo Waiting for services to be ready...
timeout /t 15 /nobreak > nul
echo.
echo Initializing database...
docker exec mes-mysql mysql -uroot -proot -e "DROP DATABASE IF EXISTS mes_db; CREATE DATABASE mes_db" 2>nul
powershell -Command "docker cp sql/init.sql mes-mysql:/tmp/init.sql" 2>nul
docker exec mes-mysql mysql -uroot -proot mes_db -e "source /tmp/init.sql" 2>nul
del containers.txt 2>nul
echo.
echo ========================================
echo   Services Status:
echo ========================================
docker compose ps
echo.
echo Access:
echo   - MySQL: localhost:3306 (root/root)
echo   - Redis: localhost:6379
echo   - Database: mes_db
echo   - Default Login: admin / admin123
echo.
