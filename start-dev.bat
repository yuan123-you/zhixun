@echo off
chcp 65001 >nul
title Zhixun - Dev Start

echo ============================================
echo    Zhixun - Local Dev Quick Start
echo ============================================
echo.

:: ==================== Load .env ====================
echo [1/6] Loading .env ...
set "ENV_FILE=%~dp0.env"
if exist "%ENV_FILE%" (
    for /f "usebackq tokens=1,* delims==" %%a in ("%ENV_FILE%") do (
        set "LINE=%%a"
        if not "%%a"=="" (
            echo %%a | findstr /b "#" >nul 2>&1
            if errorlevel 1 (
                set "%%a=%%b"
            )
        )
    )
    echo [OK] .env loaded
) else (
    echo [WARN] .env not found, using application.yml defaults
)
echo.

:: ==================== Prerequisites ====================
echo [2/6] Checking prerequisites...

where java >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Java 17+ not found, please install JDK 17
    pause
    exit /b 1
)

where node >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Node.js not found, please install Node.js 18+
    pause
    exit /b 1
)

where mvn >nul 2>&1
if %errorlevel% neq 0 (
    echo [WARN] Maven not found, will try mvnw
)

echo [OK] Prerequisites check passed
echo.

:: ==================== MySQL ====================
echo [3/6] Checking MySQL...
mysql -u root -p123456 -e "SELECT 1" >nul 2>&1
if %errorlevel% neq 0 (
    echo [WARN] MySQL not running or password mismatch, trying Docker...
    docker run -d --name zhixun-mysql -p 3306:3306 -e MYSQL_ROOT_PASSWORD=123456 -e MYSQL_DATABASE=zhixun -v "%~dp0server\src\main\resources\db":/docker-entrypoint-initdb.d mysql:8.0 --character-set-server=utf8mb4 --collation-server=utf8mb4_unicode_ci >nul 2>&1
    if %errorlevel% neq 0 (
        echo [WARN] Docker MySQL failed, please ensure MySQL is available manually
    ) else (
        echo [OK] MySQL Docker container started, waiting for init...
        timeout /t 30 /nobreak >nul
    )
) else (
    echo [OK] MySQL connected
)
echo.

:: ==================== Redis ====================
echo [4/6] Checking Redis...
redis-cli ping >nul 2>&1
if %errorlevel% equ 0 (
    echo [OK] Redis connected
    goto :redis_done
)

:: Redis not running, try Docker
docker run -d --name zhixun-redis -p 6379:6379 redis:7-alpine redis-server --appendonly yes >nul 2>&1
if %errorlevel% equ 0 (
    echo [OK] Redis Docker container started
    goto :redis_done
)

:: Docker not available, try local embedded Redis
set "REDIS_DIR=%~dp0.redis"
if not exist "%REDIS_DIR%\redis-server.exe" (
    echo [INFO] Downloading Redis for Windows...
    if not exist "%REDIS_DIR%" mkdir "%REDIS_DIR%"
    powershell -Command "& { [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; Invoke-WebRequest -Uri 'https://github.com/tporadowski/redis/releases/download/v5.0.14.1/Redis-x64-5.0.14.1.zip' -OutFile '%REDIS_DIR%\redis.zip' }" 2>nul
    if exist "%REDIS_DIR%\redis.zip" (
        echo [INFO] Extracting Redis...
        powershell -Command "& { Expand-Archive -Path '%REDIS_DIR%\redis.zip' -DestinationPath '%REDIS_DIR%' -Force }" 2>nul
        del /q "%REDIS_DIR%\redis.zip" >nul 2>&1
        if not exist "%REDIS_DIR%\redis-server.exe" (
            for /d %%i in ("%REDIS_DIR%\Redis-*") do (
                move /y "%%i\*" "%REDIS_DIR%\" >nul 2>&1
                rd /q "%%i" >nul 2>&1
            )
        )
    )
)

if exist "%REDIS_DIR%\redis-server.exe" (
    echo [INFO] Starting local Redis...
    start "Zhixun-Redis" /min "%REDIS_DIR%\redis-server.exe" --appendonly yes --maxmemory 256mb
    timeout /t 3 /nobreak >nul
    "%REDIS_DIR%\redis-cli.exe" ping >nul 2>&1
    if %errorlevel% equ 0 (
        echo [OK] Redis started (local embedded mode)
    ) else (
        echo [WARN] Redis start failed, backend may not work properly
    )
) else (
    echo [WARN] Redis download failed, please install Redis manually or start Docker
    echo        Download: https://github.com/tporadowski/redis/releases
)

:redis_done
echo.

:: ==================== Backend ====================
echo [5/6] Starting Spring Boot backend (port 8080)...
start "Zhixun-Backend" cmd /k "cd /d "%~dp0server" && mvn spring-boot:run -Dspring-boot.run.profiles=dev"
echo [OK] Backend starting, please wait...
echo.

:: ==================== Frontend ====================
echo [6/6] Starting frontend services...

if not exist "%~dp0client\node_modules" (
    echo [INFO] Installing client dependencies...
    cd /d "%~dp0client" && npm install
)

if not exist "%~dp0admin\node_modules" (
    echo [INFO] Installing admin dependencies...
    cd /d "%~dp0admin" && npm install
)

echo [INFO] Starting client (port 3000)...
start "Zhixun-Client" cmd /k "cd /d "%~dp0client" && npm run dev"

echo [INFO] Starting admin (port 3001)...
start "Zhixun-Admin" cmd /k "cd /d "%~dp0admin" && npm run dev"

echo.
echo ============================================
echo    All services started!
echo ============================================
echo.
echo    Backend API:  http://localhost:8080/api/v1
echo    Client:       http://localhost:3000
echo    Admin:        http://localhost:3001
echo.
echo    Note: Backend takes ~30s on first start
echo ============================================
pause
