@echo off
chcp 65001 >nul
title 智讯系统 - 一键启动

echo ============================================
echo    智讯系统 - 本地开发一键启动脚本
echo ============================================
echo.

:: ==================== 前置检查 ====================
echo [1/5] 检查前置环境...

where java >nul 2>&1
if %errorlevel% neq 0 (
    echo [错误] 未找到 Java 17+，请先安装 JDK 17
    pause
    exit /b 1
)

where node >nul 2>&1
if %errorlevel% neq 0 (
    echo [错误] 未找到 Node.js，请先安装 Node.js 18+
    pause
    exit /b 1
)

where mvn >nul 2>&1
if %errorlevel% neq 0 (
    echo [警告] 未找到 Maven，将尝试使用 mvnw
)

echo [OK] 环境检查通过
echo.

:: ==================== 启动 MySQL ====================
echo [2/5] 检查 MySQL...
mysql -u root -p123456 -e "SELECT 1" >nul 2>&1
if %errorlevel% neq 0 (
    echo [警告] MySQL 未运行或密码不匹配，尝试用 Docker 启动...
    docker run -d --name zhixun-mysql -p 3306:3306 -e MYSQL_ROOT_PASSWORD=123456 -e MYSQL_DATABASE=zhixun -v "%~dp0server\src\main\resources\db":/docker-entrypoint-initdb.d mysql:8.0 --character-set-server=utf8mb4 --collation-server=utf8mb4_unicode_ci >nul 2>&1
    if %errorlevel% neq 0 (
        echo [警告] Docker 启动 MySQL 失败，请手动确保 MySQL 可用
    ) else (
        echo [OK] MySQL Docker 容器已启动，等待初始化...
        timeout /t 30 /nobreak >nul
    )
) else (
    echo [OK] MySQL 已连接
)
echo.

:: ==================== 启动 Redis ====================
echo [3/5] 检查 Redis...
redis-cli ping >nul 2>&1
if %errorlevel% neq 0 (
    echo [警告] Redis 未运行，尝试用 Docker 启动...
    docker run -d --name zhixun-redis -p 6379:6379 redis:7-alpine redis-server --appendonly yes >nul 2>&1
    if %errorlevel% neq 0 (
        echo [警告] Docker 启动 Redis 失败，请手动确保 Redis 可用
    ) else (
        echo [OK] Redis Docker 容器已启动
    )
) else (
    echo [OK] Redis 已连接
)
echo.

:: ==================== 启动后端 ====================
echo [4/5] 启动 Spring Boot 后端 (端口 8080)...
start "智讯-后端" cmd /k "cd /d "%~dp0server" && mvn spring-boot:run -Dspring-boot.run.profiles=dev"
echo [OK] 后端启动中，等待服务就绪...
echo.

:: ==================== 安装前端依赖并启动 ====================
echo [5/5] 启动前端服务...

:: C端前端
if not exist "%~dp0client\node_modules" (
    echo [安装] C端前端依赖中...
    cd /d "%~dp0client" && npm install
)

:: 管理后台
if not exist "%~dp0admin\node_modules" (
    echo [安装] 管理后台依赖中...
    cd /d "%~dp0admin" && npm install
)

echo [启动] C端前端 (端口 3000)...
start "智讯-C端前端" cmd /k "cd /d "%~dp0client" && npm run dev"

echo [启动] 管理后台 (端口 3001)...
start "智讯-管理后台" cmd /k "cd /d "%~dp0admin" && npm run dev"

echo.
echo ============================================
echo    所有服务已启动！
echo ============================================
echo.
echo    后端 API:    http://localhost:8080/api/v1
echo    C端前端:      http://localhost:3000
echo    管理后台:      http://localhost:3001
echo.
echo    注意: 后端首次启动需要约30秒，请耐心等待
echo ============================================
pause
