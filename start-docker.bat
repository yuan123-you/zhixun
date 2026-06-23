@echo off
chcp 65001 >nul
title 智讯系统 - Docker 一键启动

echo ============================================
echo    智讯系统 - Docker Compose 一键启动
echo ============================================
echo.

:: 检查 Docker
where docker >nul 2>&1
if %errorlevel% neq 0 (
    echo [错误] 未找到 Docker，请先安装 Docker Desktop
    pause
    exit /b 1
)

:: 检查 .env
if not exist ".env" (
    echo [配置] 复制 .env.example 为 .env...
    copy .env.example .env
    echo [提示] 请根据需要修改 .env 中的配置
)

:: 启动所有服务
echo [启动] 正在启动所有服务（首次启动需要构建镜像，请耐心等待）...
docker compose up -d --build

echo.
echo ============================================
echo    所有服务已启动！
echo ============================================
echo.
echo    后端 API:    http://localhost:8080/api/v1
echo    C端前端:      http://localhost:3000
echo    管理后台:      http://localhost:3001
echo    Nginx入口:    http://localhost
echo    RabbitMQ管理: http://localhost:15672
echo    MinIO控制台:  http://localhost:9001
echo.
echo    查看日志: docker compose logs -f
echo    停止服务: docker compose down
echo ============================================
pause
