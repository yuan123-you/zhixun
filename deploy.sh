#!/bin/bash
# ==================== 智讯系统部署更新脚本 ====================
# 使用方式：在服务器上执行 bash deploy.sh
# 前提：服务器已安装 git、docker、docker compose
# ============================================================

set -e

# ==================== 配置区 ====================
PROJECT_DIR="/root/zhixun"              # 服务器上项目目录
BRANCH="master"                         # 部署分支
COMPOSE_FILE="docker-compose.yml"       # Docker Compose 配置文件
BACKUP_DIR="/root/zhixun-backups"       # 备份目录
HEALTH_CHECK_TIMEOUT=120                # 健康检查超时时间（秒）
HEALTH_CHECK_INTERVAL=5                 # 健康检查间隔（秒）

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# ==================== 工具函数 ====================
log_info()  { echo -e "${GREEN}[INFO]${NC}  $1"; }
log_warn()  { echo -e "${YELLOW}[WARN]${NC}  $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }
log_step()  { echo -e "\n${BLUE}====> $1${NC}"; }

# 检查命令是否存在
check_command() {
    if ! command -v "$1" &> /dev/null; then
        log_error "$1 未安装，请先安装后重试"
        exit 1
    fi
}

# 等待服务健康
wait_for_healthy() {
    local service=$1
    local elapsed=0
    log_info "等待 $service 健康检查通过..."
    while [ $elapsed -lt $HEALTH_CHECK_TIMEOUT ]; do
        local status=$(docker inspect --format='{{.State.Health.Status}}' "zhixun-$service" 2>/dev/null || echo "not_found")
        if [ "$status" = "healthy" ]; then
            log_info "$service 已健康"
            return 0
        fi
        sleep $HEALTH_CHECK_INTERVAL
        elapsed=$((elapsed + HEALTH_CHECK_INTERVAL))
        echo -n "."
    done
    echo ""
    log_warn "$service 在 ${HEALTH_CHECK_TIMEOUT}s 内未通过健康检查（当前状态: $status）"
    return 1
}

# ==================== 前置检查 ====================
log_step "前置环境检查"
check_command git
check_command docker

# 检查 docker compose 命令
if docker compose version &> /dev/null; then
    COMPOSE_CMD="docker compose"
elif docker-compose version &> /dev/null; then
    COMPOSE_CMD="docker-compose"
else
    log_error "Docker Compose 未安装"
    exit 1
fi
log_info "使用 Compose 命令: $COMPOSE_CMD"

# ==================== 进入项目目录 ====================
log_step "进入项目目录"
if [ ! -d "$PROJECT_DIR" ]; then
    log_error "项目目录 $PROJECT_DIR 不存在"
    log_info "请先克隆项目: git clone https://github.com/yuan123-you/zhixun.git $PROJECT_DIR"
    exit 1
fi
cd "$PROJECT_DIR"
log_info "当前目录: $(pwd)"

# ==================== 备份当前版本 ====================
log_step "备份当前版本"
mkdir -p "$BACKUP_DIR"
CURRENT_COMMIT=$(git rev-parse --short HEAD 2>/dev/null || echo "unknown")
BACKUP_NAME="zhixun-backup-$(date +%Y%m%d-%H%M%S)-${CURRENT_COMMIT}"
log_info "当前版本: $CURRENT_COMMIT"
log_info "备份到: $BACKUP_DIR/$BACKUP_NAME"

# 只备份关键配置文件
if [ -f ".env" ]; then
    mkdir -p "$BACKUP_DIR/$BACKUP_NAME"
    cp .env "$BACKUP_DIR/$BACKUP_NAME/.env"
    log_info "已备份 .env"
fi
if [ -f "nginx/nginx.conf" ]; then
    mkdir -p "$BACKUP_DIR/$BACKUP_NAME/nginx"
    cp nginx/nginx.conf "$BACKUP_DIR/$BACKUP_NAME/nginx/"
    log_info "已备份 nginx.conf"
fi

# 清理超过7天的旧备份
find "$BACKUP_DIR" -maxdepth 1 -name "zhixun-backup-*" -type d -mtime +7 -exec rm -rf {} \; 2>/dev/null
log_info "已清理7天前的旧备份"

# ==================== 拉取最新代码 ====================
log_step "拉取最新代码"

# 检查是否有未提交的更改
if ! git diff --quiet 2>/dev/null || ! git diff --cached --quiet 2>/dev/null; then
    log_warn "检测到未提交的更改，暂存中..."
    git stash
    STASHED=true
else
    STASHED=false
fi

git fetch origin "$BRANCH"
OLD_COMMIT=$(git rev-parse --short HEAD)
git pull origin "$BRANCH"
NEW_COMMIT=$(git rev-parse --short HEAD)

if [ "$OLD_COMMIT" = "$NEW_COMMIT" ]; then
    log_info "代码已是最新版本 ($NEW_COMMIT)"
else
    log_info "代码已更新: $OLD_COMMIT -> $NEW_COMMIT"
fi

# 恢复暂存的更改
if [ "$STASHED" = true ]; then
    git stash pop 2>/dev/null || log_warn "恢复暂存更改失败，请手动检查"
fi

# ==================== 检查 .env 文件 ====================
log_step "检查环境配置"
if [ ! -f ".env" ]; then
    if [ -f ".env.example" ]; then
        log_warn ".env 文件不存在，从 .env.example 复制"
        cp .env.example .env
        log_warn "请编辑 .env 设置生产环境配置！"
    else
        log_error ".env 和 .env.example 均不存在，无法继续"
        exit 1
    fi
else
    log_info ".env 文件已存在"
fi

# ==================== 停止旧服务 ====================
log_step "停止旧服务"
$COMPOSE_CMD --env-file .env -f "$COMPOSE_FILE" down --remove-orphans
log_info "旧服务已停止"

# ==================== 构建并启动新服务 ====================
log_step "构建并启动新服务"

# 重新构建镜像（不使用缓存以确保更新）
$COMPOSE_CMD --env-file .env -f "$COMPOSE_FILE" build --no-cache server server-2 client admin
log_info "镜像构建完成"

# 启动所有服务
$COMPOSE_CMD --env-file .env -f "$COMPOSE_FILE" up -d
log_info "服务启动中..."

# ==================== 等待服务健康 ====================
log_step "等待服务健康检查"

SERVICES=("mysql" "redis" "rabbitmq" "minio" "opensearch" "server" "server-2" "client" "admin" "nginx")
FAILED_SERVICES=()

for service in "${SERVICES[@]}"; do
    if ! wait_for_healthy "$service"; then
        FAILED_SERVICES+=("$service")
    fi
done

# ==================== 部署结果 ====================
log_step "部署结果"

if [ ${#FAILED_SERVICES[@]} -eq 0 ]; then
    log_info "所有服务部署成功！"
else
    log_warn "以下服务未通过健康检查: ${FAILED_SERVICES[*]}"
    log_info "查看日志: $COMPOSE_CMD logs <service_name>"
fi

echo ""
echo "========================================="
echo "  智讯系统部署完成"
echo "  版本: $NEW_COMMIT"
echo "  时间: $(date '+%Y-%m-%d %H:%M:%S')"
echo "========================================="
echo ""

# ==================== 显示服务状态 ====================
log_step "服务状态"
$COMPOSE_CMD --env-file .env -f "$COMPOSE_FILE" ps

echo ""
log_info "常用命令:"
echo "  查看日志:     $COMPOSE_CMD logs -f <service>"
echo "  重启服务:     $COMPOSE_CMD restart <service>"
echo "  停止所有:     $COMPOSE_CMD down"
echo "  查看状态:     $COMPOSE_CMD ps"
