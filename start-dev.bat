@echo off
chcp 65001 >nul
title Zhixun - Dev Start

echo ============================================
echo    Zhixun - Local Dev Quick Start
echo ============================================
echo.

:: ==================== Load .env ====================
echo [1/9] Loading .env ...
set "ENV_FILE=%~dp0.env"
if exist "%ENV_FILE%" (
    for /f "usebackq tokens=1,* delims==" %%a in ("%ENV_FILE%") do (
        if not "%%a"=="" (
            echo %%a | findstr /b "#" >nul 2>&1
            if errorlevel 1 set "%%a=%%b"
        )
    )
    echo [OK] .env loaded
) else (
    echo [WARN] .env not found
)
echo.

:: ==================== Prerequisites ====================
echo [2/9] Checking prerequisites...
where java >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Java 17+ not found
    pause
    exit /b 1
)
where node >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Node.js not found
    pause
    exit /b 1
)
where mvn >nul 2>&1 || echo [WARN] Maven not found
echo [OK] Prerequisites check passed
echo.

:: ==================== MySQL ====================
echo [3/9] Checking MySQL...
mysql -u root -p123456 -e "SELECT 1" >nul 2>&1
if errorlevel 1 (
    echo [WARN] MySQL not running, trying Docker...
    docker run -d --name zhixun-mysql -p 3306:3306 -e MYSQL_ROOT_PASSWORD=123456 -e MYSQL_DATABASE=zhixun -v "%~dp0server\src\main\resources\db":/docker-entrypoint-initdb.d mysql:8.0 --character-set-server=utf8mb4 --collation-server=utf8mb4_unicode_ci >nul 2>&1
    if errorlevel 1 (
        echo [WARN] Docker MySQL failed
    ) else (
        echo [OK] MySQL Docker started, waiting 30s...
        timeout /t 30 /nobreak >nul
    )
) else (
    echo [OK] MySQL connected
)
echo.

:: ==================== MinIO ====================
echo [4/9] Checking MinIO port 9000...
powershell -Command "if (Get-NetTCPConnection -LocalPort 9000 -ErrorAction SilentlyContinue | Where-Object {$_.State -eq 'Listen'}) { exit 0 } else { exit 1 }" >nul 2>&1
if not errorlevel 1 (
    echo [OK] MinIO running
) else (
    if exist "D:\tools\minio\minio.exe" (
        echo [WARN] MinIO not running, starting...
        if not exist "D:\tools\minio\data" mkdir "D:\tools\minio\data"
        start "Zhixun-MinIO" /min cmd /c "set MINIO_ROOT_USER=zhixun321 && set MINIO_ROOT_PASSWORD=zhixun321 && D:\tools\minio\minio.exe server D:\tools\minio\data --console-address :9001"
        echo [OK] MinIO starting API 9000 Console 9001
    ) else (
        echo [WARN] MinIO not found at D:\tools\minio\minio.exe
    )
)
echo.

:: ==================== Redis ====================
echo [5/9] Checking Redis...
redis-cli ping >nul 2>&1
if not errorlevel 1 (
    echo [OK] Redis connected
    goto :redis_ok
)

echo [WARN] Redis not running, trying Docker...
docker run -d --name zhixun-redis -p 6379:6379 redis:7-alpine redis-server --appendonly yes >nul 2>&1
if not errorlevel 1 (
    echo [OK] Redis Docker started
    goto :redis_ok
)

echo [WARN] Docker not available, trying local Redis...
set "RDIR=%~dp0.redis"
if not exist "%RDIR%\redis-server.exe" (
    echo [INFO] Downloading Redis for Windows...
    if not exist "%RDIR%" mkdir "%RDIR%"
    powershell -Command "$ProgressPreference='SilentlyContinue'; [Net.ServicePointManager]::SecurityProtocol=[Net.SecurityProtocolType]::Tls12; Invoke-WebRequest -Uri 'https://github.com/tporadowski/redis/releases/download/v5.0.14.1/Redis-x64-5.0.14.1.zip' -OutFile '%RDIR%\redis.zip'" 2>nul
    if exist "%RDIR%\redis.zip" (
        echo [INFO] Extracting Redis...
        powershell -Command "Expand-Archive -Path '%RDIR%\redis.zip' -DestinationPath '%RDIR%' -Force" 2>nul
        del /q "%RDIR%\redis.zip" >nul 2>&1
        if not exist "%RDIR%\redis-server.exe" (
            for /d %%i in ("%RDIR%\Redis-*") do (
                move /y "%%i\*" "%RDIR%\" >nul 2>&1
                rd /q "%%i" >nul 2>&1
            )
        )
    )
)

if exist "%RDIR%\redis-server.exe" (
    echo [INFO] Starting local Redis...
    start "Zhixun-Redis" /min "%RDIR%\redis-server.exe" --port 6379 --appendonly yes --maxmemory 256mb
    timeout /t 3 /nobreak >nul
    redis-cli ping >nul 2>&1
    if not errorlevel 1 (
        echo [OK] Redis started local
    ) else (
        echo [WARN] Redis start failed
    )
) else (
    echo [WARN] Redis download failed, install manually or start Docker
    echo        Download https://github.com/tporadowski/redis/releases
)

:redis_ok
echo.

:: ==================== RabbitMQ ====================
echo [6/9] Checking RabbitMQ port 5672...
powershell -Command "if (Get-NetTCPConnection -LocalPort 5672 -ErrorAction SilentlyContinue | Where-Object {$_.State -eq 'Listen'}) { exit 0 } else { exit 1 }" >nul 2>&1
if not errorlevel 1 (
    echo [OK] RabbitMQ running
) else (
    echo [WARN] RabbitMQ not running, trying Docker...
    docker run -d --name zhixun-rabbitmq -p 5672:5672 -p 15672:15672 -e RABBITMQ_DEFAULT_USER=guest -e RABBITMQ_DEFAULT_PASS=guest rabbitmq:3-management-alpine >nul 2>&1
    if not errorlevel 1 (echo [OK] RabbitMQ Docker started) else (echo [WARN] RabbitMQ start failed)
)
echo.

:: ==================== OpenSearch ====================
echo [7/9] Checking OpenSearch port 9200...
powershell -Command "if (Get-NetTCPConnection -LocalPort 9200 -ErrorAction SilentlyContinue | Where-Object {$_.State -eq 'Listen'}) { exit 0 } else { exit 1 }" >nul 2>&1
if not errorlevel 1 (
    echo [OK] OpenSearch running
) else (
    echo [WARN] OpenSearch not running, trying Docker...
    docker run -d --name zhixun-opensearch -p 9200:9200 -p 9600:9600 -e "discovery.type=single-node" -e "OPENSEARCH_JAVA_OPTS=-Xms512m -Xmx512m" -e "DISABLE_SECURITY_PLUGIN=true" -e "bootstrap.memory_lock=true" --ulimit memlock=-1:-1 --ulimit nofile=65536:65536 opensearchproject/opensearch:2.11.0 >nul 2>&1
    if not errorlevel 1 (
        echo [OK] OpenSearch Docker started
        echo [INFO] First run may take 30-60s...
    ) else (
        echo [WARN] OpenSearch start failed
    )
)
echo.

:: ==================== Backend ====================
echo [8/9] Starting Spring Boot backend port 8080...
start "Zhixun-Backend" cmd /k "cd /d "%~dp0server" && mvn spring-boot:run -Dspring-boot.run.profiles=local"
echo [OK] Backend starting, please wait...
echo.

:: ==================== Frontend ====================
echo [9/9] Starting frontend services...

if not exist "%~dp0client\node_modules" (
    echo [INFO] Installing client dependencies...
    cd /d "%~dp0client" && npm install
)

if not exist "%~dp0admin\node_modules" (
    echo [INFO] Installing admin dependencies...
    cd /d "%~dp0admin" && npm install
)

echo [INFO] Starting client port 3500...
start "Zhixun-Client" cmd /k "cd /d "%~dp0client" && npm run dev"

echo [INFO] Starting admin port 3001...
start "Zhixun-Admin" cmd /k "cd /d "%~dp0admin" && npm run dev"

echo.
echo ============================================
echo    All services started!
echo ============================================
echo.
echo    Backend API   http://localhost:8080/api/v1
echo    Client        http://localhost:3500
echo    Admin         http://localhost:3001
echo.
echo    Note: Backend takes ~30s on first start
echo ============================================
pause
