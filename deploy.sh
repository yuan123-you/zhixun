#!/bin/bash
# ==============================================================================
# 知讯系统 - 全面自动化部署脚本
# ==============================================================================
# 使用方式：
#   一键部署：    bash deploy.sh
#   指定操作：    bash deploy.sh [命令] [选项]
#   查看帮助：    bash deploy.sh help
#
# 可用命令：
#   deploy        完整部署（默认，等同无参数执行）
#   update        仅更新代码
#   build         仅构建服务
#   start         仅启动服务
#   stop          停止所有服务
#   restart       重启所有服务
#   status        查看服务状态
#   logs          查看服务日志
#   health        健康检查
#   rollback      回滚到上一版本
#   nginx         配置宿主机 Nginx
#   fix           自动诊断并修复常见问题
#   init          首次初始化部署环境
#   backup        手动备份
#   clean         清理旧备份和悬空镜像
#
# 选项：
#   --skip-pull   跳过代码拉取
#   --skip-build  跳过镜像构建
#   --no-cache    构建时不使用缓存
#   --service=X   仅操作指定服务（如 --service=client）
#   --verbose     显示详细输出
#   --yes / -y    跳过确认提示
# ==============================================================================

set -euo pipefail

# ==================== 配置区 ====================
PROJECT_DIR="$HOME/zhixun"
BRANCH="master"
COMPOSE_FILE="docker-compose.yml"
BACKUP_DIR="$HOME/zhixun-backups"
LOG_DIR="$HOME/zhixun-logs"
HEALTH_CHECK_TIMEOUT=180
HEALTH_CHECK_INTERVAL=5
DOMAIN="glint.novo.ccwu.cc"
NGINX_SITE_CONF="nginx/glint.novo.ccwu.cc.conf"
NGINX_SITES_AVAILABLE="/etc/nginx/sites-available/${DOMAIN}"
NGINX_SITES_ENABLED="/etc/nginx/sites-enabled/${DOMAIN}"
GIT_REPO="https://github.com/yuan123-you/zhixun.git"
KEEP_BACKUPS=7

# Docker 服务列表（按启动依赖顺序）
INFRA_SERVICES=("mysql" "redis" "rabbitmq" "minio" "opensearch")
APP_SERVICES=("server" "server-2" "client" "admin")
ALL_SERVICES=("${INFRA_SERVICES[@]}" "${APP_SERVICES[@]}")

# ==================== 全局状态 ====================
SCRIPT_VERSION="2.0.0"
START_TIME=$(date +%s)
COMMAND="deploy"
SKIP_PULL=false
SKIP_BUILD=false
NO_CACHE=""
TARGET_SERVICE=""
VERBOSE=false
AUTO_YES=false
ROLLBACK_AVAILABLE=false
PREV_BACKUP=""
STASHED=false
OLD_COMMIT=""
COMPOSE_CMD=""

# ==================== 颜色输出 ====================
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
BOLD='\033[1m'
NC='\033[0m'

# ==================== 日志函数 ====================
log_info()    { echo -e "${GREEN}[INFO]${NC}  $(date '+%H:%M:%S') $1"; write_log "INFO" "$1"; }
log_warn()    { echo -e "${YELLOW}[WARN]${NC}  $(date '+%H:%M:%S') $1"; write_log "WARN" "$1"; }
log_error()   { echo -e "${RED}[ERROR]${NC} $(date '+%H:%M:%S') $1"; write_log "ERROR" "$1"; }
log_step()    { echo -e "\n${BLUE}${BOLD}====> $1${NC}"; write_log "STEP" "$1"; }
log_detail()  { [[ "$VERBOSE" == true ]] && echo -e "${CYAN}  └─${NC} $1"; write_log "DEBUG" "$1"; }
log_success() { echo -e "${GREEN}${BOLD}[OK]${NC}    $(date '+%H:%M:%S') $1"; write_log "OK" "$1"; }

write_log() {
    local level="$1"
    local message="$2"
    mkdir -p "$LOG_DIR"
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] [$level] $message" >> "$LOG_DIR/deploy-$(date '+%Y%m%d').log"
}

# ==================== 工具函数 ====================

# 检查命令是否存在
check_command() {
    if ! command -v "$1" &> /dev/null; then
        log_error "$1 未安装"
        return 1
    fi
}

# 确认提示
confirm_action() {
    local message="$1"
    if [[ "$AUTO_YES" == true ]]; then
        return 0
    fi
    echo -e "${YELLOW}[确认]${NC} $1 (y/N)"
    read -r response
    [[ "$response" =~ ^[Yy]$ ]]
}

# 等待服务健康
wait_for_healthy() {
    local service="$1"
    local timeout="${2:-$HEALTH_CHECK_TIMEOUT}"
    local elapsed=0
    log_info "等待 $service 健康检查通过..."
    while [ $elapsed -lt $timeout ]; do
        local status
        status=$(docker inspect --format='{{.State.Health.Status}}' "zhixun-$service" 2>/dev/null || echo "not_found")
        if [ "$status" = "healthy" ]; then
            log_success "$service 已健康"
            return 0
        elif [ "$status" = "unhealthy" ]; then
            # 检查容器是否还在运行
            local running
            running=$(docker inspect --format='{{.State.Running}}' "zhixun-$service" 2>/dev/null || echo "false")
            if [ "$running" = "false" ]; then
                log_error "$service 容器已停止"
                return 1
            fi
        fi
        sleep $HEALTH_CHECK_INTERVAL
        elapsed=$((elapsed + HEALTH_CHECK_INTERVAL))
        if (( elapsed % 15 == 0 )); then
            echo -n "."
        fi
    done
    echo ""
    local final_status
    final_status=$(docker inspect --format='{{.State.Health.Status}}' "zhixun-$service" 2>/dev/null || echo "not_found")
    log_warn "$service 在 ${timeout}s 内未通过健康检查（状态: $final_status）"
    return 1
}

# 获取服务状态
get_service_status() {
    local service="$1"
    local container="zhixun-$service"
    if ! docker ps -a --format '{{.Names}}' | grep -q "^${container}$"; then
        echo "not_created"
        return
    fi
    local running
    running=$(docker inspect --format='{{.State.Running}}' "$container" 2>/dev/null || echo "false")
    if [ "$running" = "false" ]; then
        echo "stopped"
        return
    fi
    local health
    health=$(docker inspect --format='{{.State.Health.Status}}' "$container" 2>/dev/null || echo "none")
    echo "$health"
}

# 安全执行（带错误处理）
safe_exec() {
    local description="$1"
    shift
    log_info "$description..."
    if "$@"; then
        log_success "$description - 完成"
        return 0
    else
        local exit_code=$?
        log_error "$description - 失败 (退出码: $exit_code)"
        return $exit_code
    fi
}

# ==================== 解析命令行参数 ====================
parse_args() {
    while [[ $# -gt 0 ]]; do
        case "$1" in
            deploy|update|build|start|stop|restart|status|logs|health|rollback|nginx|fix|init|backup|clean|help)
                COMMAND="$1"
                ;;
            --skip-pull)   SKIP_PULL=true ;;
            --skip-build)  SKIP_BUILD=true ;;
            --no-cache)    NO_CACHE="--no-cache" ;;
            --service=*)   TARGET_SERVICE="${1#*=}" ;;
            --verbose)     VERBOSE=true ;;
            --yes|-y)      AUTO_YES=true ;;
            --help)        COMMAND="help" ;;
            *)
                log_error "未知参数: $1"
                echo "运行 'bash deploy.sh help' 查看帮助"
                exit 1
                ;;
        esac
        shift
    done
}

# ==================== 前置检查 ====================
preflight_check() {
    log_step "前置环境检查"

    # 检查必要命令
    check_command git || { log_error "请安装 git: sudo apt install git"; exit 1; }
    check_command docker || { log_error "请安装 Docker: https://docs.docker.com/engine/install/"; exit 1; }

    # 检查 Docker Compose
    if docker compose version &> /dev/null; then
        COMPOSE_CMD="docker compose"
    elif docker-compose version &> /dev/null; then
        COMPOSE_CMD="docker-compose"
    else
        log_error "Docker Compose 未安装"
        exit 1
    fi
    log_detail "Compose 命令: $COMPOSE_CMD"

    # 检查 Docker 服务
    if ! docker info &> /dev/null; then
        log_error "Docker 服务未运行，请启动: sudo systemctl start docker"
        exit 1
    fi

    # 检查磁盘空间（至少需要 5GB）
    local available_kb
    available_kb=$(df -k "$HOME" | awk 'NR==2 {print $4}')
    local available_gb=$((available_kb / 1024 / 1024))
    if [ "$available_gb" -lt 5 ]; then
        log_warn "磁盘剩余空间不足 5GB（当前: ${available_gb}GB），部署可能失败"
    else
        log_detail "磁盘空间: ${available_gb}GB 可用"
    fi

    log_success "环境检查通过"
}

# ==================== 进入项目目录 ====================
enter_project_dir() {
    log_step "进入项目目录"
    if [ ! -d "$PROJECT_DIR" ]; then
        log_error "项目目录 $PROJECT_DIR 不存在"
        if confirm_action "是否从 Git 克隆项目？"; then
            git clone "$GIT_REPO" "$PROJECT_DIR"
            cd "$PROJECT_DIR"
        else
            log_info "请手动克隆: git clone $GIT_REPO $PROJECT_DIR"
            exit 1
        fi
    fi
    cd "$PROJECT_DIR"
    log_detail "当前目录: $(pwd)"
}

# ==================== 备份 ====================
do_backup() {
    log_step "备份当前版本"
    mkdir -p "$BACKUP_DIR"

    CURRENT_COMMIT=$(git rev-parse --short HEAD 2>/dev/null || echo "unknown")
    BACKUP_NAME="zhixun-backup-$(date +%Y%m%d-%H%M%S)-${CURRENT_COMMIT}"
    BACKUP_PATH="$BACKUP_DIR/$BACKUP_NAME"

    mkdir -p "$BACKUP_PATH"

    # 备份关键配置文件
    local backup_items=(".env" "nginx/nginx.conf" "nginx/${DOMAIN}.conf" "docker-compose.yml")
    for item in "${backup_items[@]}"; do
        if [ -f "$item" ]; then
            mkdir -p "$BACKUP_PATH/$(dirname "$item")"
            cp "$item" "$BACKUP_PATH/$item"
            log_detail "已备份 $item"
        fi
    done

    # 记录当前 commit
    echo "$CURRENT_COMMIT" > "$BACKUP_PATH/.commit"

    # 记录当前容器镜像版本
    docker ps --format '{{.Names}} {{.Image}}' | grep zhixun > "$BACKUP_PATH/.containers" 2>/dev/null || true

    log_success "备份完成: $BACKUP_PATH"

    # 标记可回滚
    ROLLBACK_AVAILABLE=true
    PREV_BACKUP="$BACKUP_PATH"
}

# ==================== 清理旧备份 ====================
clean_old_backups() {
    log_step "清理旧备份"
    local count
    count=$(find "$BACKUP_DIR" -maxdepth 1 -name "zhixun-backup-*" -type d 2>/dev/null | wc -l)
    if [ "$count" -gt "$KEEP_BACKUPS" ]; then
        find "$BACKUP_DIR" -maxdepth 1 -name "zhixun-backup-*" -type d -mtime +${KEEP_BACKUPS} -exec rm -rf {} \; 2>/dev/null
        log_info "已清理超过 ${KEEP_BACKUPS} 天的旧备份"
    else
        log_detail "无需清理（当前 $count 个备份）"
    fi
}

# ==================== 代码更新 ====================
do_update() {
    log_step "更新代码"

    # 检查是否有未提交的更改
    if ! git diff --quiet 2>/dev/null || ! git diff --cached --quiet 2>/dev/null; then
        log_warn "检测到未提交的更改，暂存中..."
        git stash
        STASHED=true
    fi

    git fetch origin "$BRANCH"
    OLD_COMMIT=$(git rev-parse --short HEAD)

    if [[ "$SKIP_PULL" == true ]]; then
        log_info "跳过代码拉取（--skip-pull）"
    else
        git pull origin "$BRANCH"
    fi

    NEW_COMMIT=$(git rev-parse --short HEAD)

    if [ "$OLD_COMMIT" = "$NEW_COMMIT" ]; then
        log_info "代码已是最新版本 ($NEW_COMMIT)"
    else
        log_success "代码已更新: $OLD_COMMIT -> $NEW_COMMIT"
    fi

    # 恢复暂存的更改
    if [[ "$STASHED" == true ]]; then
        if git stash pop 2>/dev/null; then
            log_detail "已恢复暂存更改"
        else
            log_warn "恢复暂存更改失败，请手动检查: git stash pop"
        fi
    fi
}

# ==================== 检查环境配置 ====================
check_env() {
    log_step "检查环境配置"
    if [ ! -f ".env" ]; then
        if [ -f ".env.example" ]; then
            log_warn ".env 文件不存在，从 .env.example 复制"
            cp .env.example .env
            log_warn "请编辑 .env 设置生产环境配置！"
        else
            log_error ".env 和 .env.example 均不存在"
            exit 1
        fi
    else
        log_detail ".env 文件已存在"

        # 检查 .env 是否有更新
        if [ -f ".env.example" ]; then
            local new_vars
            new_vars=$(comm -23 <(grep -oP '^[A-Z_]+(?==)' .env.example | sort) <(grep -oP '^[A-Z_]+(?==)' .env | sort))
            if [ -n "$new_vars" ]; then
                log_warn ".env 中缺少以下变量（.env.example 中有定义）:"
                echo "$new_vars" | while read -r var; do
                    log_warn "  - $var"
                done
            fi
        fi
    fi
}

# ==================== 构建服务 ====================
do_build() {
    log_step "构建服务"

    if [[ "$SKIP_BUILD" == true ]]; then
        log_info "跳过镜像构建（--skip-build）"
        return 0
    fi

    local services=()
    if [ -n "$TARGET_SERVICE" ]; then
        services=("$TARGET_SERVICE")
    else
        services=("server" "server-2" "client" "admin")
    fi

    log_info "构建服务: ${services[*]}"
    local cache_flag="${NO_CACHE:---no-cache}"

    if $COMPOSE_CMD --env-file .env -f "$COMPOSE_FILE" build $cache_flag "${services[@]}"; then
        log_success "镜像构建完成"
    else
        log_error "镜像构建失败"
        log_info "尝试不使用 --no-cache 重新构建..."
        if $COMPOSE_CMD --env-file .env -f "$COMPOSE_FILE" build "${services[@]}"; then
            log_success "镜像构建完成（使用缓存）"
        else
            log_error "镜像构建再次失败，请检查 Dockerfile 和代码"
            return 1
        fi
    fi
}

# ==================== 启动服务 ====================
do_start() {
    log_step "启动服务"

    if [ -n "$TARGET_SERVICE" ]; then
        log_info "启动服务: $TARGET_SERVICE"
        $COMPOSE_CMD --env-file .env -f "$COMPOSE_FILE" up -d "$TARGET_SERVICE"
    else
        log_info "启动所有服务"
        $COMPOSE_CMD --env-file .env -f "$COMPOSE_FILE" up -d
    fi

    log_success "服务启动命令已执行"
}

# ==================== 停止服务 ====================
do_stop() {
    log_step "停止服务"

    if [ -n "$TARGET_SERVICE" ]; then
        log_info "停止服务: $TARGET_SERVICE"
        $COMPOSE_CMD --env-file .env -f "$COMPOSE_FILE" stop "$TARGET_SERVICE"
    else
        if confirm_action "确认停止所有知讯服务？"; then
            $COMPOSE_CMD --env-file .env -f "$COMPOSE_FILE" down --remove-orphans
        else
            log_info "已取消"
            return 0
        fi
    fi

    log_success "服务已停止"
}

# ==================== 健康检查 ====================
do_health_check() {
    log_step "健康检查"

    local services=()
    if [ -n "$TARGET_SERVICE" ]; then
        services=("$TARGET_SERVICE")
    else
        services=("${ALL_SERVICES[@]}")
    fi

    local failed=()
    local passed=()

    for service in "${services[@]}"; do
        local status
        status=$(get_service_status "$service")
        case "$status" in
            healthy)
                log_success "$service: 健康"
                passed+=("$service")
                ;;
            starting|running)
                log_info "$service: 启动中，等待健康检查..."
                if wait_for_healthy "$service"; then
                    passed+=("$service")
                else
                    failed+=("$service")
                fi
                ;;
            stopped)
                log_error "$service: 已停止"
                failed+=("$service")
                ;;
            not_created)
                log_warn "$service: 未创建"
                failed+=("$service")
                ;;
            unhealthy)
                log_error "$service: 不健康"
                failed+=("$service")
                ;;
            *)
                log_warn "$service: 未知状态 ($status)"
                failed+=("$service")
                ;;
        esac
    done

    echo ""
    echo "========================================="
    echo "  健康检查结果"
    echo "  通过: ${#passed[@]}  失败: ${#failed[@]}"
    echo "========================================="

    if [ ${#failed[@]} -gt 0 ]; then
        log_warn "失败的服务: ${failed[*]}"
        log_info "查看日志: $COMPOSE_CMD logs ${failed[*]}"
        return 1
    fi
    return 0
}

# ==================== 配置宿主机 Nginx ====================
do_nginx_config() {
    log_step "配置宿主机 Nginx"

    if ! command -v nginx &> /dev/null; then
        log_warn "未检测到宿主机 Nginx"
        if confirm_action "是否安装 Nginx？"; then
            sudo apt update && sudo apt install -y nginx
        else
            log_info "跳过 Nginx 配置"
            return 0
        fi
    fi

    # 解除服务屏蔽
    if systemctl is-masked nginx &> /dev/null; then
        log_info "解除 Nginx 服务屏蔽..."
        sudo systemctl unmask nginx
    fi

    # 检查配置文件
    if [ ! -f "$NGINX_SITE_CONF" ]; then
        log_warn "未找到 Nginx 站点配置文件: $NGINX_SITE_CONF"
        log_info "将在项目 nginx/ 目录下生成配置文件..."

        mkdir -p nginx
        cat > "$NGINX_SITE_CONF" << NGINXEOF
# 知讯系统 - 宿主机 Nginx 站点配置
# 自动生成于 $(date '+%Y-%m-%d %H:%M:%S')

server {
    listen 80;
    server_name ${DOMAIN};

    location /.well-known/acme-challenge/ {
        root /var/www/certbot;
    }

    location / {
        return 301 https://\$host\$request_uri;
    }
}

server {
    listen 443 ssl;
    server_name ${DOMAIN};

    ssl_certificate /etc/letsencrypt/live/${DOMAIN}/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/${DOMAIN}/privkey.pem;

    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers ECDHE-ECDSA-AES128-GCM-SHA256:ECDHE-RSA-AES128-GCM-SHA256:ECDHE-ECDSA-AES256-GCM-SHA384:ECDHE-RSA-AES256-GCM-SHA384;
    ssl_prefer_server_ciphers off;
    ssl_session_cache shared:SSL:10m;
    ssl_session_timeout 10m;

    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;
    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-XSS-Protection "1; mode=block" always;
    add_header Referrer-Policy "strict-origin-when-cross-origin" always;

    client_max_body_size 20m;

    # 管理后台
    location /admin {
        rewrite ^/admin(/.*)\$ \$1 break;
        proxy_pass http://127.0.0.1:3001;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
    }

    location /admin/assets/ {
        rewrite ^/admin(/.*)\$ \$1 break;
        proxy_pass http://127.0.0.1:3001;
        proxy_set_header Host \$host;
        expires 1y;
        add_header Cache-Control "public, immutable";
    }

    # 后端 API
    location /api {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 120s;
        proxy_buffering on;
        proxy_buffer_size 4k;
        proxy_buffers 8 4k;
        proxy_next_upstream error timeout http_502 http_503 http_504;
        proxy_next_upstream_tries 2;
    }

    # WebSocket
    location /ws {
        proxy_pass http://127.0.0.1:8080;
        proxy_http_version 1.1;
        proxy_set_header Upgrade \$http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
        proxy_connect_timeout 7d;
        proxy_send_timeout 7d;
        proxy_read_timeout 7d;
    }

    # C端前端（默认路由）
    location / {
        proxy_pass http://127.0.0.1:3000;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
        proxy_connect_timeout 60s;
        proxy_read_timeout 60s;
    }

    # Nuxt 静态资源缓存
    location ~* ^/_nuxt/.*\.(js|css)\$ {
        proxy_pass http://127.0.0.1:3000;
        expires 1y;
        add_header Cache-Control "public, immutable";
        access_log off;
    }

    location ~* ^/_nuxt/.*\.(png|jpg|jpeg|gif|ico|svg|woff|woff2|ttf|eot)\$ {
        proxy_pass http://127.0.0.1:3000;
        expires 30d;
        add_header Cache-Control "public";
        access_log off;
    }

    location ~ /\. {
        deny all;
        access_log off;
        log_not_found off;
    }
}
NGINXEOF
        log_success "已生成 Nginx 配置文件: $NGINX_SITE_CONF"
    fi

    # 清理旧的冲突配置
    local old_configs
    old_configs=$(sudo grep -rl "$DOMAIN" /etc/nginx/sites-available/ 2>/dev/null | grep -v "$NGINX_SITES_AVAILABLE" || true)
    if [ -n "$old_configs" ]; then
        log_warn "发现旧的 $DOMAIN 配置文件，将清理:"
        echo "$old_configs" | while read -r f; do
            log_warn "  - $f"
            sudo rm -f "$f"
            # 同时清理 sites-enabled 中的软链接
            local basename
            basename=$(basename "$f")
            sudo rm -f "/etc/nginx/sites-enabled/$basename"
        done
    fi

    # 部署配置
    sudo cp "$NGINX_SITE_CONF" "$NGINX_SITES_AVAILABLE"
    log_detail "已复制配置到 $NGINX_SITES_AVAILABLE"

    if [ ! -L "$NGINX_SITES_ENABLED" ]; then
        sudo ln -s "$NGINX_SITES_AVAILABLE" "$NGINX_SITES_ENABLED"
        log_detail "已创建软链接"
    fi

    # 测试并重载
    if sudo nginx -t 2>&1; then
        if systemctl is-active --quiet nginx; then
            sudo systemctl reload nginx
            log_success "Nginx 配置已重载"
        else
            sudo systemctl start nginx
            log_success "Nginx 已启动"
        fi
    else
        log_error "Nginx 配置测试失败，请手动检查: sudo nginx -t"
        return 1
    fi
}

# ==================== 自动诊断修复 ====================
do_fix() {
    log_step "自动诊断与修复"

    local issues_found=0

    # 1. 检查 Docker 服务
    if ! docker info &> /dev/null; then
        log_error "Docker 服务未运行"
        log_info "尝试启动 Docker..."
        sudo systemctl start docker
        if docker info &> /dev/null; then
            log_success "Docker 已启动"
        else
            log_error "无法启动 Docker，请手动检查"
        fi
        ((issues_found++))
    fi

    # 2. 检查容器状态
    for service in "${ALL_SERVICES[@]}"; do
        local status
        status=$(get_service_status "$service")
        if [ "$status" = "stopped" ] || [ "$status" = "unhealthy" ]; then
            log_warn "$service 状态异常: $status"
            log_info "尝试重启 $service..."
            $COMPOSE_CMD --env-file .env -f "$COMPOSE_FILE" restart "$service"
            ((issues_found++))
        fi
    done

    # 3. 检查端口占用
    local ports=("3000" "3001" "8080" "3306" "6379")
    local port_names=("client" "admin" "server" "mysql" "redis")
    for i in "${!ports[@]}"; do
        local port="${ports[$i]}"
        local name="${port_names[$i]}"
        if ! ss -tlnp 2>/dev/null | grep -q ":${port} "; then
            # 检查对应容器是否在运行
            local container_status
            container_status=$(get_service_status "$name")
            if [ "$container_status" = "healthy" ] || [ "$container_status" = "running" ]; then
                log_warn "$name 容器运行中但端口 $port 未监听"
                ((issues_found++))
            fi
        fi
    done

    # 4. 检查 Nginx 配置
    if command -v nginx &> /dev/null; then
        if ! sudo nginx -t &> /dev/null; then
            log_error "Nginx 配置有误"
            sudo nginx -t 2>&1 | tail -5
            ((issues_found++))
        fi

        if ! systemctl is-active --quiet nginx; then
            log_warn "Nginx 服务未运行"
            if systemctl is-masked nginx &> /dev/null; then
                log_info "Nginx 服务被屏蔽，正在解除..."
                sudo systemctl unmask nginx
            fi
            sudo systemctl start nginx
            log_success "Nginx 已启动"
            ((issues_found++))
        fi

        # 检查站点配置是否存在
        if [ ! -f "$NGINX_SITES_AVAILABLE" ]; then
            log_warn "Nginx 站点配置缺失: $NGINX_SITES_AVAILABLE"
            log_info "执行 Nginx 配置..."
            do_nginx_config
            ((issues_found++))
        fi
    fi

    # 5. 检查 SSL 证书
    if [ ! -f "/etc/letsencrypt/live/${DOMAIN}/fullchain.pem" ]; then
        log_warn "SSL 证书不存在: /etc/letsencrypt/live/${DOMAIN}/fullchain.pem"
        log_info "如需 HTTPS，请安装证书: sudo certbot certonly --nginx -d $DOMAIN"
        ((issues_found++))
    fi

    # 6. 检查磁盘空间
    local available_kb
    available_kb=$(df -k "$HOME" | awk 'NR==2 {print $4}')
    local available_gb=$((available_kb / 1024 / 1024))
    if [ "$available_gb" -lt 2 ]; then
        log_error "磁盘空间严重不足（${available_gb}GB），可能导致服务异常"
        log_info "建议清理: docker system prune -a; sudo apt clean"
        ((issues_found++))
    fi

    # 7. 检查 .env 文件
    if [ ! -f ".env" ]; then
        log_warn ".env 文件不存在"
        check_env
        ((issues_found++))
    fi

    # 8. 检查 favicon
    if [ ! -f "client/public/favicon.svg" ]; then
        log_warn "缺少 favicon.svg 文件"
        mkdir -p client/public
        cat > client/public/favicon.svg << 'SVGEOF'
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 32 32">
  <rect width="32" height="32" rx="6" fill="#4F46E5"/>
  <text x="16" y="23" font-family="Arial, sans-serif" font-size="20" font-weight="bold" fill="white" text-anchor="middle">知</text>
</svg>
SVGEOF
        log_success "已生成 favicon.svg"
        ((issues_found++))
    fi

    # 9. 检查 Docker 悬空镜像
    local dangling
    dangling=$(docker images -f "dangling=true" -q | wc -l)
    if [ "$dangling" -gt 5 ]; then
        log_warn "存在 $dangling 个悬空镜像，建议清理: bash deploy.sh clean"
        ((issues_found++))
    fi

    # 结果汇总
    echo ""
    if [ "$issues_found" -eq 0 ]; then
        log_success "未发现明显问题，系统运行正常"
    else
        log_warn "发现并处理了 $issues_found 个问题"
    fi
}

# ==================== 回滚 ====================
do_rollback() {
    log_step "回滚操作"

    # 查找最近的备份
    local latest_backup
    latest_backup=$(find "$BACKUP_DIR" -maxdepth 1 -name "zhixun-backup-*" -type d -printf '%T@ %p\n' 2>/dev/null | sort -rn | head -1 | cut -d' ' -f2-)

    if [ -z "$latest_backup" ]; then
        log_error "未找到可用的备份，无法回滚"
        return 1
    fi

    local backup_commit
    backup_commit=$(cat "$latest_backup/.commit" 2>/dev/null || echo "unknown")
    local backup_time
    backup_time=$(basename "$latest_backup" | sed 's/zhixun-backup-//' | sed 's/-[a-f0-9]*$//')

    log_info "找到备份:"
    log_info "  路径: $latest_backup"
    log_info "  版本: $backup_commit"
    log_info "  时间: $backup_time"

    if ! confirm_action "确认回滚到此版本？"; then
        log_info "已取消回滚"
        return 0
    fi

    # 停止服务
    log_info "停止当前服务..."
    $COMPOSE_CMD --env-file .env -f "$COMPOSE_FILE" down --remove-orphans

    # 恢复配置文件
    for item in ".env" "nginx/nginx.conf" "nginx/${DOMAIN}.conf" "docker-compose.yml"; do
        if [ -f "$latest_backup/$item" ]; then
            cp "$latest_backup/$item" "$item"
            log_detail "已恢复 $item"
        fi
    done

    # 回滚代码
    if [ "$backup_commit" != "unknown" ]; then
        log_info "回滚代码到 $backup_commit..."
        git checkout "$backup_commit"
    fi

    # 重新构建并启动
    $COMPOSE_CMD --env-file .env -f "$COMPOSE_FILE" build
    $COMPOSE_CMD --env-file .env -f "$COMPOSE_FILE" up -d

    log_success "回滚完成"
}

# ==================== 查看状态 ====================
do_status() {
    log_step "服务状态"

    echo ""
    printf "${BOLD}%-20s %-12s %-15s%s${NC}\n" "服务" "状态" "健康" "端口"
    echo "------------------------------------------------------------"

    for service in "${ALL_SERVICES[@]}"; do
        local container="zhixun-$service"
        local status
        status=$(get_service_status "$service")

        local port=""
        case "$service" in
            mysql)        port="3306" ;;
            redis)        port="6379" ;;
            rabbitmq)     port="5672/15672" ;;
            minio)        port="9000/9001" ;;
            opensearch)   port="9200" ;;
            server)       port="8080" ;;
            server-2)     port="8081" ;;
            client)       port="3000" ;;
            admin)        port="3001" ;;
        esac

        local status_color="$NC"
        case "$status" in
            healthy)   status_color="$GREEN" ;;
            starting|running) status_color="$YELLOW" ;;
            stopped|unhealthy|not_created) status_color="$RED" ;;
        esac

        printf "%-20s ${status_color}%-12s${NC} %-15s %s\n" "$service" "$status" "" "$port"
    done

    echo ""

    # Nginx 状态
    if command -v nginx &> /dev/null; then
        local nginx_status
        if systemctl is-active --quiet nginx; then
            nginx_status="${GREEN}运行中${NC}"
        else
            nginx_status="${RED}已停止${NC}"
        fi
        printf "Nginx (宿主机)     %b\n" "$nginx_status"
    fi

    echo ""
    $COMPOSE_CMD --env-file .env -f "$COMPOSE_FILE" ps 2>/dev/null || true
}

# ==================== 查看日志 ====================
do_logs() {
    local service="${TARGET_SERVICE:-}"
    if [ -z "$service" ]; then
        echo "可用服务: ${ALL_SERVICES[*]}"
        echo "用法: bash deploy.sh logs --service=<服务名>"
        echo "查看所有日志: $COMPOSE_CMD logs -f"
        return 0
    fi
    $COMPOSE_CMD --env-file .env -f "$COMPOSE_FILE" logs -f "$service"
}

# ==================== 首次初始化 ====================
do_init() {
    log_step "首次初始化部署环境"

    log_info "此命令将安装必要的系统依赖并初始化项目"

    # 1. 更新系统
    if confirm_action "是否更新系统包？"; then
        sudo apt update && sudo apt upgrade -y
    fi

    # 2. 安装 Docker
    if ! command -v docker &> /dev/null; then
        log_info "安装 Docker..."
        curl -fsSL https://get.docker.com | sudo sh
        sudo usermod -aG docker "$USER"
        log_success "Docker 安装完成（可能需要重新登录以生效用户组）"
    fi

    # 3. 安装 Docker Compose
    if ! docker compose version &> /dev/null && ! command -v docker-compose &> /dev/null; then
        log_info "安装 Docker Compose 插件..."
        sudo apt install -y docker-compose-plugin
    fi

    # 4. 安装 Git
    if ! command -v git &> /dev/null; then
        log_info "安装 Git..."
        sudo apt install -y git
    fi

    # 5. 安装 Nginx
    if ! command -v nginx &> /dev/null; then
        if confirm_action "是否安装 Nginx？"; then
            sudo apt install -y nginx
        fi
    fi

    # 6. 安装 Certbot
    if ! command -v certbot &> /dev/null; then
        if confirm_action "是否安装 Certbot（用于 SSL 证书）？"; then
            sudo apt install -y certbot python3-certbot-nginx
        fi
    fi

    # 7. 克隆项目
    if [ ! -d "$PROJECT_DIR" ]; then
        log_info "克隆项目..."
        git clone "$GIT_REPO" "$PROJECT_DIR"
        cd "$PROJECT_DIR"
    fi

    # 8. 配置环境
    check_env

    # 9. 配置防火墙
    if command -v ufw &> /dev/null; then
        log_info "配置防火墙规则..."
        sudo ufw allow 80/tcp
        sudo ufw allow 443/tcp
        sudo ufw allow 3000/tcp
        sudo ufw allow 3001/tcp
    fi

    log_success "初始化完成！请运行 'bash deploy.sh' 开始部署"
}

# ==================== 清理 ====================
do_clean() {
    log_step "清理"

    # 清理旧备份
    clean_old_backups

    # 清理 Docker 悬空镜像
    log_info "清理 Docker 悬空镜像..."
    local dangling_count
    dangling_count=$(docker images -f "dangling=true" -q | wc -l)
    if [ "$dangling_count" -gt 0 ]; then
        docker image prune -f
        log_success "已清理 $dangling_count 个悬空镜像"
    else
        log_detail "无需清理悬空镜像"
    fi

    # 清理旧日志（超过30天）
    if [ -d "$LOG_DIR" ]; then
        find "$LOG_DIR" -name "deploy-*.log" -mtime +30 -delete 2>/dev/null
        log_detail "已清理30天前的旧日志"
    fi

    log_success "清理完成"
}

# ==================== 完整部署 ====================
do_deploy() {
    echo ""
    echo -e "${BLUE}${BOLD}=========================================${NC}"
    echo -e "${BLUE}${BOLD}  知讯系统 - 自动化部署 v${SCRIPT_VERSION}${NC}"
    echo -e "${BLUE}${BOLD}=========================================${NC}"
    echo ""

    # 前置检查
    preflight_check

    # 进入项目目录
    enter_project_dir

    # 备份
    do_backup

    # 更新代码
    do_update

    # 检查环境配置
    check_env

    # 停止旧服务
    log_step "停止旧服务"
    $COMPOSE_CMD --env-file .env -f "$COMPOSE_FILE" down --remove-orphans 2>/dev/null || true
    log_success "旧服务已停止"

    # 构建
    do_build

    # 启动
    do_start

    # 健康检查
    do_health_check || true

    # 配置 Nginx
    do_nginx_config || true

    # 清理
    clean_old_backups

    # 部署结果
    local end_time=$(date +%s)
    local duration=$((end_time - START_TIME))
    local minutes=$((duration / 60))
    local seconds=$((duration % 60))

    NEW_COMMIT=$(git rev-parse --short HEAD 2>/dev/null || echo "unknown")

    echo ""
    echo -e "${GREEN}${BOLD}=========================================${NC}"
    echo -e "${GREEN}${BOLD}  部署完成！${NC}"
    echo -e "${GREEN}${BOLD}  版本: $NEW_COMMIT${NC}"
    echo -e "${GREEN}${BOLD}  耗时: ${minutes}分${seconds}秒${NC}"
    echo -e "${GREEN}${BOLD}  时间: $(date '+%Y-%m-%d %H:%M:%S')${NC}"
    echo -e "${GREEN}${BOLD}=========================================${NC}"
    echo ""
    echo -e "  访问地址:"
    echo -e "    C端前端:  ${CYAN}https://${DOMAIN}${NC}"
    echo -e "    管理后台:  ${CYAN}https://${DOMAIN}/admin${NC}"
    echo -e "    后端 API:  ${CYAN}https://${DOMAIN}/api${NC}"
    echo ""
    echo -e "  常用命令:"
    echo -e "    查看状态:   ${CYAN}bash deploy.sh status${NC}"
    echo -e "    查看日志:   ${CYAN}bash deploy.sh logs --service=client${NC}"
    echo -e "    健康检查:   ${CYAN}bash deploy.sh health${NC}"
    echo -e "    诊断修复:   ${CYAN}bash deploy.sh fix${NC}"
    echo -e "    回滚版本:   ${CYAN}bash deploy.sh rollback${NC}"
    echo ""

    # 写入部署记录
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] deploy $OLD_COMMIT -> $NEW_COMMIT duration=${minutes}m${seconds}s" >> "$LOG_DIR/deploy-history.log"
}

# ==================== 帮助信息 ====================
show_help() {
    echo ""
    echo -e "${BOLD}知讯系统 - 自动化部署脚本 v${SCRIPT_VERSION}${NC}"
    echo ""
    echo "用法: bash deploy.sh [命令] [选项]"
    echo ""
    echo -e "${BOLD}命令:${NC}"
    echo "  deploy        完整部署（默认）"
    echo "  update        仅更新代码"
    echo "  build         仅构建服务"
    echo "  start         仅启动服务"
    echo "  stop          停止所有服务"
    echo "  restart       重启所有服务"
    echo "  status        查看服务状态"
    echo "  logs          查看服务日志（需 --service=）"
    echo "  health        健康检查"
    echo "  rollback      回滚到上一版本"
    echo "  nginx         配置宿主机 Nginx"
    echo "  fix           自动诊断并修复常见问题"
    echo "  init          首次初始化部署环境"
    echo "  backup        手动备份"
    echo "  clean         清理旧备份和悬空镜像"
    echo "  help          显示此帮助信息"
    echo ""
    echo -e "${BOLD}选项:${NC}"
    echo "  --skip-pull   跳过代码拉取"
    echo "  --skip-build  跳过镜像构建"
    echo "  --no-cache    构建时不使用缓存"
    echo "  --service=X   仅操作指定服务"
    echo "  --verbose     显示详细输出"
    echo "  --yes / -y    跳过确认提示"
    echo ""
    echo -e "${BOLD}示例:${NC}"
    echo "  bash deploy.sh                          # 一键完整部署"
    echo "  bash deploy.sh build --no-cache         # 无缓存重新构建"
    echo "  bash deploy.sh logs --service=server    # 查看后端日志"
    echo "  bash deploy.sh restart --service=client # 重启前端"
    echo "  bash deploy.sh fix                      # 诊断并修复问题"
    echo "  bash deploy.sh rollback                 # 回滚到上一版本"
    echo "  bash deploy.sh status                   # 查看服务状态"
    echo ""
    echo -e "${BOLD}配置参数（脚本顶部可修改）:${NC}"
    echo "  PROJECT_DIR          项目目录（默认: ~/zhixun）"
    echo "  BRANCH               Git 分支（默认: master）"
    echo "  DOMAIN               域名（默认: glint.novo.ccwu.cc）"
    echo "  HEALTH_CHECK_TIMEOUT 健康检查超时秒数（默认: 180）"
    echo "  KEEP_BACKUPS         保留备份数（默认: 7天）"
    echo ""
    echo -e "${BOLD}日志位置:${NC}"
    echo "  部署日志: ~/zhixun-logs/deploy-YYYYMMDD.log"
    echo "  部署历史: ~/zhixun-logs/deploy-history.log"
    echo ""
}

# ==================== 主入口 ====================
main() {
    parse_args "$@"

    case "$COMMAND" in
        deploy)
            do_deploy
            ;;
        update)
            preflight_check
            enter_project_dir
            do_update
            ;;
        build)
            preflight_check
            enter_project_dir
            check_env
            do_build
            ;;
        start)
            preflight_check
            enter_project_dir
            check_env
            do_start
            ;;
        stop)
            enter_project_dir
            do_stop
            ;;
        restart)
            preflight_check
            enter_project_dir
            check_env
            do_stop
            do_start
            do_health_check || true
            ;;
        status)
            enter_project_dir
            do_status
            ;;
        logs)
            enter_project_dir
            do_logs
            ;;
        health)
            enter_project_dir
            do_health_check
            ;;
        rollback)
            preflight_check
            enter_project_dir
            do_rollback
            ;;
        nginx)
            enter_project_dir
            do_nginx_config
            ;;
        fix)
            preflight_check
            enter_project_dir
            check_env
            do_fix
            ;;
        init)
            do_init
            ;;
        backup)
            enter_project_dir
            do_backup
            ;;
        clean)
            do_clean
            ;;
        help)
            show_help
            ;;
        *)
            log_error "未知命令: $COMMAND"
            show_help
            exit 1
            ;;
    esac
}

main "$@"
