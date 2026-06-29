# =====================================================
# 智讯后端本地启动脚本（dev profile）
# 自动注入 .env 中的 ZHIPU_API_KEY 等环境变量
# =====================================================

$ErrorActionPreference = "Stop"
$envFile = Join-Path $PSScriptRoot "..\.env"

if (-not (Test-Path $envFile)) {
    Write-Host "[ERROR] 未找到 .env 文件: $envFile" -ForegroundColor Red
    exit 1
}

# 读取 .env 并导出 ZHIPU_*/JWT_*/AES_*/MAIL_*/MYSQL_*/REDIS_*/RABBITMQ_*/MINIO_*/OPENSEARCH_*/SENTINEL_*/TENCENT_*/OFFICIAL_ACCOUNT_ID/WECHAT_*/QQ_*
Get-Content $envFile -Encoding UTF8 | ForEach-Object {
    $line = $_.Trim()
    if ($line -and -not $line.StartsWith("#") -and $line -match "^\s*([A-Z_][A-Z0-9_]*)\s*=\s*(.*)$") {
        $name = $matches[1]
        $value = $matches[2]
        # 跳过端口和容器名等不适合注入到 Spring 的变量
        if ($name -notmatch "^(CLIENT_PORT|ADMIN_PORT|NGINX_HTTP_PORT|NGINX_HTTPS_PORT|SERVER_2_PORT|SONARQUBE_PORT|RABBITMQ_MGMT_PORT|OPENSEARCH_HTTP_PORT|OPENSEARCH_TRANSPORT_PORT|MINIO_API_PORT|MINIO_CONSOLE_PORT|MYSQL_PORT|REDIS_PORT|API_BASE)$") {
            Set-Item -Path "Env:$name" -Value $value
        }
    }
}

Write-Host "[INFO] 已加载 .env 环境变量" -ForegroundColor Green
Write-Host "[INFO] ZHIPU_API_KEY = $($env:ZHIPU_API_KEY.Substring(0, [Math]::Min(8, $env:ZHIPU_API_KEY.Length)))..." -ForegroundColor Cyan
Write-Host "[INFO] ZHIPU_MODEL  = $env:ZHIPU_MODEL" -ForegroundColor Cyan
Write-Host "[INFO] ZHIPU_FALLBACK_MODEL = $env:ZHIPU_FALLBACK_MODEL" -ForegroundColor Cyan
Write-Host "[INFO] SPRING_PROFILES_ACTIVE = dev" -ForegroundColor Cyan
Write-Host "[INFO] 启动 Spring Boot..." -ForegroundColor Green

# 启动 jar（同原 IDE 启动参数）
Set-Location $PSScriptRoot
$jar = Join-Path $PSScriptRoot "target\zhixun-server-1.0.0.jar"
& "D:\Java\jdk\bin\java.exe" -jar -Xms512m -Xmx1024m $jar --spring.profiles.active=dev
