# Zhixun - Local Dev Quick Start (PowerShell)
$ErrorActionPreference = "Continue"
$root = Split-Path -Parent $MyInvocation.MyCommand.Path

Write-Host "============================================" -ForegroundColor Cyan
Write-Host "   Zhixun - Local Dev Quick Start" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""

# ==================== Load .env ====================
Write-Host "[1/9] Loading .env ..." -NoNewline
$envFile = Join-Path $root ".env"
if (Test-Path $envFile) {
    Get-Content $envFile | ForEach-Object {
        if ($_ -match '^\s*([^#][^=]+)=(.*)$') {
            $key = $matches[1].Trim()
            $value = $matches[2].Trim()
            [Environment]::SetEnvironmentVariable($key, $value, "Process")
        }
    }
    Write-Host " [OK] .env loaded" -ForegroundColor Green
} else {
    Write-Host " [WARN] .env not found" -ForegroundColor Yellow
}

# ==================== Prerequisites ====================
Write-Host "[2/9] Checking prerequisites..." -NoNewline
$ok = $true
if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
    Write-Host "`n[ERROR] Java 17+ not found" -ForegroundColor Red
    $ok = $false
}
if (-not (Get-Command node -ErrorAction SilentlyContinue)) {
    Write-Host "`n[ERROR] Node.js not found" -ForegroundColor Red
    $ok = $false
}
if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) {
    Write-Host "`n[WARN] Maven not found" -ForegroundColor Yellow
}
if (-not $ok) { Read-Host "Press Enter to exit"; exit 1 }
Write-Host " [OK] Prerequisites check passed" -ForegroundColor Green
Write-Host ""

# ==================== MySQL ====================
Write-Host "[3/9] Checking MySQL..." -NoNewline
$mysqlResult = & mysql -u root -p123456 -e "SELECT 1" 2>&1
if ($LASTEXITCODE -eq 0) {
    Write-Host " [OK] MySQL connected" -ForegroundColor Green
} else {
    Write-Host " [WARN] MySQL not running, trying Docker..." -ForegroundColor Yellow
    docker run -d --name zhixun-mysql -p 3306:3306 -e MYSQL_ROOT_PASSWORD=123456 -e MYSQL_DATABASE=zhixun -v "${root}\server\src\main\resources\db:/docker-entrypoint-initdb.d" mysql:8.0 --character-set-server=utf8mb4 --collation-server=utf8mb4_unicode_ci 2>&1 | Out-Null
    if ($LASTEXITCODE -eq 0) {
        Write-Host " [OK] MySQL Docker started, waiting 30s..." -ForegroundColor Green
        Start-Sleep -Seconds 30
    } else {
        Write-Host " [WARN] Docker MySQL failed" -ForegroundColor Yellow
    }
}
Write-Host ""

# ==================== MinIO ====================
Write-Host "[4/9] Checking MinIO port 9000..." -NoNewline
$port9000 = netstat -ano | Select-String ":9000.*LISTENING"
if ($port9000) {
    Write-Host " [OK] MinIO running" -ForegroundColor Green
} else {
    $minioExe = "D:\tools\minio\minio.exe"
    if (Test-Path $minioExe) {
        Write-Host " [WARN] MinIO not running, starting..." -ForegroundColor Yellow
        $minioData = "D:\tools\minio\data"
        if (-not (Test-Path $minioData)) { New-Item -ItemType Directory -Path $minioData -Force | Out-Null }
        $minioEnv = @{
            MINIO_ROOT_USER = "zhixun321"
            MINIO_ROOT_PASSWORD = "zhixun321"
        }
        # Clear deprecated vars
        $env:MINIO_ACCESS_KEY = ""
        $env:MINIO_SECRET_KEY = ""
        Start-Process -FilePath $minioExe -ArgumentList "server", $minioData, "--console-address", ":9001" -WindowStyle Minimized
        Write-Host " [OK] MinIO starting API 9000 Console 9001" -ForegroundColor Green
    } else {
        Write-Host " [WARN] MinIO not found at $minioExe" -ForegroundColor Yellow
    }
}
Write-Host ""

# ==================== Redis ====================
Write-Host "[5/9] Checking Redis..." -NoNewline
$redisDir = Join-Path $root ".redis"
$redisCli = Join-Path $redisDir "redis-cli.exe"
$redisServer = Join-Path $redisDir "redis-server.exe"

# Check if Redis is already running
$redisRunning = $false
if (Test-Path $redisCli) {
    $result = & $redisCli ping 2>&1
    if ($LASTEXITCODE -eq 0 -and $result -eq "PONG") { $redisRunning = $true }
}
if (-not $redisRunning -and (Get-Command redis-cli -ErrorAction SilentlyContinue)) {
    $result = & redis-cli ping 2>&1
    if ($LASTEXITCODE -eq 0 -and $result -eq "PONG") { $redisRunning = $true }
}

if ($redisRunning) {
    Write-Host " [OK] Redis connected" -ForegroundColor Green
} else {
    Write-Host " [WARN] Redis not running, trying Docker..." -ForegroundColor Yellow
    $dockerOk = $false
    if (Get-Command docker -ErrorAction SilentlyContinue) {
        docker run -d --name zhixun-redis -p 6379:6379 redis:7-alpine redis-server --appendonly yes 2>&1 | Out-Null
        if ($LASTEXITCODE -eq 0) { $dockerOk = $true }
    }
    if ($dockerOk) {
        Write-Host " [OK] Redis Docker started" -ForegroundColor Green
    } else {
        Write-Host " [WARN] Docker not available, trying local Redis..." -ForegroundColor Yellow
        if (Test-Path $redisServer) {
            Write-Host "[INFO] Starting local Redis..." -ForegroundColor Gray
            Start-Process -FilePath $redisServer -ArgumentList "--port", "6379", "--appendonly", "yes", "--maxmemory", "256mb" -WindowStyle Minimized
            Start-Sleep -Seconds 3
            if (Test-Path $redisCli) {
                $result = & $redisCli ping 2>&1
            } elseif (Get-Command redis-cli -ErrorAction SilentlyContinue) {
                $result = & redis-cli ping 2>&1
            }
            if ($LASTEXITCODE -eq 0 -and $result -eq "PONG") {
                Write-Host " [OK] Redis started local" -ForegroundColor Green
            } else {
                Write-Host " [WARN] Redis start failed" -ForegroundColor Yellow
            }
        } else {
            Write-Host " [WARN] Redis not found at $redisDir" -ForegroundColor Yellow
            Write-Host "        Download https://github.com/tporadowski/redis/releases" -ForegroundColor Gray
        }
    }
}
Write-Host ""

# ==================== RabbitMQ ====================
Write-Host "[6/9] Checking RabbitMQ port 5672..." -NoNewline
$port5672 = netstat -ano | Select-String ":5672.*LISTENING"
if ($port5672) {
    Write-Host " [OK] RabbitMQ running" -ForegroundColor Green
} else {
    Write-Host " [WARN] RabbitMQ not running, trying Docker..." -ForegroundColor Yellow
    $dockerOk = $false
    if (Get-Command docker -ErrorAction SilentlyContinue) {
        docker run -d --name zhixun-rabbitmq -p 5672:5672 -p 15672:15672 -e RABBITMQ_DEFAULT_USER=guest -e RABBITMQ_DEFAULT_PASS=guest rabbitmq:3-management-alpine 2>&1 | Out-Null
        if ($LASTEXITCODE -eq 0) { $dockerOk = $true }
    }
    if ($dockerOk) {
        Write-Host " [OK] RabbitMQ Docker started" -ForegroundColor Green
    } else {
        Write-Host " [WARN] RabbitMQ start failed" -ForegroundColor Yellow
    }
}
Write-Host ""

# ==================== OpenSearch ====================
Write-Host "[7/9] Checking OpenSearch port 9200..." -NoNewline
$port9200 = netstat -ano | Select-String ":9200.*LISTENING"
if ($port9200) {
    Write-Host " [OK] OpenSearch running" -ForegroundColor Green
} else {
    Write-Host " [WARN] OpenSearch not running, trying Docker..." -ForegroundColor Yellow
    $dockerOk = $false
    if (Get-Command docker -ErrorAction SilentlyContinue) {
        docker run -d --name zhixun-opensearch -p 9200:9200 -p 9600:9600 -e "discovery.type=single-node" -e "OPENSEARCH_JAVA_OPTS=-Xms512m -Xmx512m" -e "DISABLE_SECURITY_PLUGIN=true" -e "bootstrap.memory_lock=true" --ulimit memlock=-1:-1 --ulimit nofile=65536:65536 opensearchproject/opensearch:2.11.0 2>&1 | Out-Null
        if ($LASTEXITCODE -eq 0) { $dockerOk = $true }
    }
    if ($dockerOk) {
        Write-Host " [OK] OpenSearch Docker started" -ForegroundColor Green
        Write-Host "[INFO] First run may take 30-60s..." -ForegroundColor Gray
    } else {
        Write-Host " [WARN] Docker not available, trying local OpenSearch..." -ForegroundColor Yellow
        $osHome = Join-Path $root ".opensearch\opensearch-2.11.0"
        $osBat = Join-Path $osHome "bin\opensearch.bat"
        if (Test-Path $osBat) {
            Write-Host "[INFO] Starting local OpenSearch..." -ForegroundColor Gray
            $env:JAVA_HOME = Join-Path $osHome "jdk"
            $env:OPENSEARCH_JAVA_HOME = Join-Path $osHome "jdk"
            Start-Process -FilePath "cmd.exe" -ArgumentList "/k", "cd /d $osHome && bin\opensearch.bat" -WindowStyle Minimized
            Write-Host " [OK] OpenSearch starting (first run may take 30-60s)..." -ForegroundColor Green
        } else {
            Write-Host " [WARN] OpenSearch not found at $osHome" -ForegroundColor Yellow
            Write-Host "        Install OpenSearch 2.11.0 to .opensearch\opensearch-2.11.0" -ForegroundColor Gray
        }
    }
}
Write-Host ""

# ==================== Backend ====================
Write-Host "[8/9] Starting Spring Boot backend port 8080..."
$serverDir = Join-Path $root "server"
Start-Process -FilePath "cmd.exe" -ArgumentList "/k", "cd /d $serverDir && mvn spring-boot:run -Dspring-boot.run.profiles=local"
Write-Host " [OK] Backend starting, please wait..." -ForegroundColor Green
Write-Host ""

# ==================== Frontend ====================
Write-Host "[9/9] Starting frontend services..."

$clientDir = Join-Path $root "client"
$adminDir = Join-Path $root "admin"

if (-not (Test-Path (Join-Path $clientDir "node_modules"))) {
    Write-Host "[INFO] Installing client dependencies..." -ForegroundColor Gray
    Set-Location $clientDir
    npm install 2>&1 | Out-Null
    Set-Location $root
}

if (-not (Test-Path (Join-Path $adminDir "node_modules"))) {
    Write-Host "[INFO] Installing admin dependencies..." -ForegroundColor Gray
    Set-Location $adminDir
    npm install 2>&1 | Out-Null
    Set-Location $root
}

Write-Host "[INFO] Starting client port 3500..." -ForegroundColor Gray
Start-Process -FilePath "cmd.exe" -ArgumentList "/k", "cd /d $clientDir && npm run dev"

Write-Host "[INFO] Starting admin port 3001..." -ForegroundColor Gray
Start-Process -FilePath "cmd.exe" -ArgumentList "/k", "cd /d $adminDir && npm run dev"

Write-Host ""
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "   All services started!" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "   Backend API   http://localhost:8080/api/v1" -ForegroundColor Gray
Write-Host "   Client        http://localhost:3500" -ForegroundColor Gray
Write-Host "   Admin         http://localhost:3001" -ForegroundColor Gray
Write-Host ""
Write-Host "   Port mapping reference: docs/PORTS.md" -ForegroundColor Gray
Write-Host "   Note: Backend takes ~30s on first start" -ForegroundColor Gray
Write-Host "============================================" -ForegroundColor Cyan
Read-Host "Press Enter to exit"