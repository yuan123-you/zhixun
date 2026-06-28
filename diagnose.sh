#!/usr/bin/env bash
# =============================================================================
# 智讯 (Zhixun) 项目 - 部署诊断脚本 v1.0
# 用法: bash diagnose.sh [--json] [--fix]
#   --json  以 JSON 格式输出（方便接入监控/告警系统）
#   --fix   尝试自动修复可修复的问题（需谨慎使用）
# =============================================================================

set -euo pipefail

# ---------------------------------------------------------------------------
# 全局配置区 —— 根据你的服务器实际情况修改以下变量
# ---------------------------------------------------------------------------

# 项目根目录（部署到服务器后的路径）
PROJECT_DIR="${PROJECT_DIR:-$HOME/zhixun}"

# Nginx 配置文件路径（宿主机 Nginx 的站点配置）
NGINX_SITE_CONF="${NGINX_SITE_CONF:-/etc/nginx/sites-enabled/glint.novo.ccwu.cc.conf}"
NGINX_INCLUDE_DIR="${NGINX_INCLUDE_DIR:-/etc/nginx/conf.d}"

# 需要检测的服务端口（宿主机监听端口）
PORTS_TO_CHECK=(80 443 3000 3001 8082 9000)

# 期望运行的 Docker 容器名（至少包含这些关键词）
REQUIRED_CONTAINERS=("mysql" "redis" "minio" "server" "client" "admin")

# CORS：预期的允许来源（来自 docker-compose.yml 中的环境变量）
# 端口来源：docs/PORTS.md → CLIENT_PORT=3500, ADMIN_PORT=3001
EXPECTED_CORS_ORIGINS=("https://glint.novo.ccwu.cc" "http://glint.novo.ccwu.cc" "http://localhost:3500" "http://localhost:3001")

# 静态资源目录（构建产物存放位置）
STATIC_DIRS=(
  "$PROJECT_DIR/client/.output/public"
  "$PROJECT_DIR/admin/dist"
)

# API 健康检查地址（通过 Nginx 反向代理访问）
API_HEALTH_URL="${API_HEALTH_URL:-https://glint.novo.ccwu.cc/api/v1/actuator/health}"

# 超时设置（秒）
TIMEOUT_CURL=10
TIMEOUT_NC=3

# 输出 JSON 模式
JSON_MODE=false
AUTO_FIX=false

# ---------------------------------------------------------------------------
# 工具函数
# ---------------------------------------------------------------------------

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

TOTAL_CHECKS=0
PASS_CHECKS=0
FAIL_CHECKS=0
WARN_CHECKS=0

JSON_RESULTS="[]"

ok()   { echo -e "${GREEN}[✓]${NC} $*"; }
fail() { echo -e "${RED}[✗]${NC} $*"; }
warn() { echo -e "${YELLOW}[!]${NC} $*"; }
info() { echo -e "${BLUE}[i]${NC} $*"; }
h2()   { echo -e "\n${CYAN}━━━ $* ━━━${NC}"; }

# 记录单项检查结果
record() {
  local status="$1" desc="$2" detail="${3:-}" fix="${4:-}"
  TOTAL_CHECKS=$((TOTAL_CHECKS + 1))
  case "$status" in
    pass) PASS_CHECKS=$((PASS_CHECKS + 1)) ;;
    fail) FAIL_CHECKS=$((FAIL_CHECKS + 1)) ;;
    warn) WARN_CHECKS=$((WARN_CHECKS + 1)) ;;
  esac
  if $JSON_MODE; then
    JSON_RESULTS=$(echo "$JSON_RESULTS" | \
      jq -c ". + [{status:\"$status\",description:\"$desc\",detail:\"$detail\",fix_suggestion:\"$fix\"}]")
  fi
}

# 检查命令是否存在
has_cmd() { command -v "$1" &>/dev/null; }

# 尝试 curl 请求，返回 HTTP 状态码
try_curl() {
  local url="$1"
  local extra_args="${2:-}"
  # shellcheck disable=SC2086
  curl -s -o /dev/null -w "%{http_code}" --connect-timeout "$TIMEOUT_CURL" --max-time "$TIMEOUT_CURL" $extra_args "$url" 2>/dev/null || echo "000"
}

# 尝试 curl 请求，返回完整响应体
try_curl_body() {
  local url="$1"
  local extra_args="${2:-}"
  # shellcheck disable=SC2086
  curl -s --connect-timeout "$TIMEOUT_CURL" --max-time "$TIMEOUT_CURL" $extra_args "$url" 2>/dev/null || echo ""
}

# TCP 端口连通性检查 (使用 bash 内置 /dev/tcp，无需 nc)
check_tcp_port() {
  local host="$1" port="$2"
  timeout "$TIMEOUT_NC" bash -c "echo >/dev/tcp/$host/$port" 2>/dev/null && return 0 || return 1
}

# ---------------------------------------------------------------------------
# 第 1 步：系统环境基线检查
# ---------------------------------------------------------------------------
baseline_check() {
  h2 "1. 系统环境基线检查"

  # 操作系统
  if [ -f /etc/os-release ]; then
    . /etc/os-release
    info "操作系统: $PRETTY_NAME"
    record pass "操作系统识别" "$PRETTY_NAME"
  else
    record fail "操作系统识别" "无法识别操作系统版本"
  fi

  # Docker 状态
  if has_cmd docker; then
    if docker info &>/dev/null; then
      local dver; dver=$(docker --version 2>/dev/null)
      ok "Docker 运行正常 ($dver)"
      record pass "Docker 服务状态" "$dver"
    else
      fail "Docker 已安装但无法连接守护进程"
      record fail "Docker 服务状态" "Docker 守护进程未运行或权限不足" \
        "执行 systemctl start docker 或 sudo usermod -aG docker \$USER 后重新登录"
    fi
  else
    fail "未检测到 Docker"
    record fail "Docker 服务状态" "Docker 未安装" "安装 Docker: curl -fsSL https://get.docker.com | sh"
  fi

  # Docker Compose
  if has_cmd docker-compose || docker compose version &>/dev/null; then
    ok "Docker Compose 可用"
    record pass "Docker Compose 可用性" "已安装"
  else
    warn "未检测到 docker-compose 命令"
    record warn "Docker Compose 可用性" "未安装" "安装: apt install docker-compose-plugin"
  fi

  # 磁盘空间
  local disk_usage
  disk_usage=$(df -h "$PROJECT_DIR" 2>/dev/null | awk 'NR==2 {print $5" used, "$4" available"}')
  local disk_pct
  disk_pct=$(df "$PROJECT_DIR" 2>/dev/null | awk 'NR==2 {print $5}' | tr -d '%')
  if [ -n "$disk_pct" ] && [ "$disk_pct" -gt 90 ]; then
    fail "磁盘空间不足: $disk_usage"
    record fail "磁盘空间" "$disk_usage (使用率 > 90%)" "清理日志或扩容磁盘"
  elif [ -n "$disk_pct" ] && [ "$disk_pct" -gt 80 ]; then
    warn "磁盘空间偏低: $disk_usage"
    record warn "磁盘空间" "$disk_usage (使用率 > 80%)" "建议清理旧日志和备份"
  else
    ok "磁盘空间: $disk_usage"
    record pass "磁盘空间" "$disk_usage"
  fi

  # 内存
  local mem_info; mem_info=$(free -h | awk 'NR==2{print $2" total, "$3" used, "$4" free"}')
  info "内存: $mem_info"
  local mem_avail; mem_avail=$(free -m | awk 'NR==2{print $7}')
  if [ "${mem_avail:-0}" -lt 512 ]; then
    warn "可用内存不足 512MB，可能影响服务稳定性"
    record warn "内存" "$mem_info (可用内存偏低)" "考虑升级服务器配置或减少运行服务"
  else
    record pass "内存" "$mem_info"
  fi
}

# ---------------------------------------------------------------------------
# 第 2 步：Docker 容器状态检查
# ---------------------------------------------------------------------------
container_check() {
  h2 "2. Docker 容器状态检查"

  if ! docker info &>/dev/null; then
    fail "Docker 不可用，跳过容器检查"
    record fail "Docker 容器检查" "Docker 不可用，无法检查"
    return
  fi

  for container in "${REQUIRED_CONTAINERS[@]}"; do
    local matching
    matching=$(docker ps --format '{{.Names}} {{.Status}}' 2>/dev/null | grep -i "$container" || true)
    if [ -n "$matching" ]; then
      local name; name=$(echo "$matching" | awk '{print $1}')
      local status; status=$(echo "$matching" | cut -d' ' -f2-)
      if echo "$status" | grep -qi "Up"; then
        ok "容器 $name: 运行中 ($status)"
        record pass "容器运行状态" "$name 运行正常 ($status)"
      else
        fail "容器 $name: 未运行 ($status)"
        record fail "容器运行状态" "$name 状态异常: $status" \
          "执行 docker start $name 或 docker-compose up -d"
      fi
    else
      fail "容器 '$container': 不存在或未启动"
      record fail "容器运行状态" "未找到匹配 '$container' 的容器" \
        "在 $PROJECT_DIR 执行 docker-compose up -d"
    fi
  done
}

# ---------------------------------------------------------------------------
# 第 3 步：端口监听与防火墙/安全组检查
# ---------------------------------------------------------------------------
port_firewall_check() {
  h2 "3. 端口监听、防火墙与安全组检查"

  # 3.1 检查端口监听
  info "检查必要端口是否在监听..."
  for port in "${PORTS_TO_CHECK[@]}"; do
    if has_cmd ss; then
      if ss -tlnp 2>/dev/null | grep -q ":$port "; then
        local svc; svc=$(ss -tlnp 2>/dev/null | grep ":$port " | awk '{print $NF}' | head -1)
        ok "端口 $port 正在监听 ($svc)"
        record pass "端口监听 $port" "服务: $svc"
      else
        fail "端口 $port 未监听"
        record fail "端口监听 $port" "端口未处于 LISTEN 状态" \
          "检查对应服务是否启动，执行: ss -tlnp | grep $port"
      fi
    else
      if check_tcp_port "127.0.0.1" "$port"; then
        ok "端口 $port 可达"
        record pass "端口监听 $port" "TCP 连接成功"
      else
        fail "端口 $port 不可达"
        record fail "端口监听 $port" "无法建立 TCP 连接" \
          "检查对应服务是否启动"
      fi
    fi
  done

  # 3.2 检查 iptables 防火墙规则
  info "检查 iptables 防火墙规则..."
  if has_cmd iptables; then
    # 检查 INPUT 链默认策略
    local input_policy; input_policy=$(iptables -L INPUT -n 2>/dev/null | head -1 | awk '{print $4}')
    if [ "$input_policy" = "DROP" ]; then
      warn "iptables INPUT 链默认策略为 DROP，请确认已放行必要端口"
      record warn "iptables 默认策略" "INPUT 链策略为 DROP，可能阻止入站流量" \
        "执行为关键端口添加放行规则: iptables -I INPUT -p tcp --dport 80 -j ACCEPT (重复 443 等)"
    else
      ok "iptables INPUT 链默认策略: $input_policy"
      record pass "iptables 默认策略" "INPUT 链策略: $input_policy"
    fi

    # 检查关键端口是否有显式放行
    for port in 80 443 3000 3001 8082; do
      if iptables -L INPUT -n 2>/dev/null | grep -q "dpt:$port"; then
        ok "iptables 已放行端口 $port"
      else
        if [ "$input_policy" = "DROP" ]; then
          fail "iptables 未显式放行端口 $port (默认 DROP)"
          record fail "iptables 端口放行" "端口 $port 未被显式放行" \
            "执行: iptables -I INPUT -p tcp --dport $port -j ACCEPT && iptables-save > /etc/iptables/rules.v4"
        fi
      fi
    done
  else
    warn "未安装 iptables 命令，跳过检查"
    record warn "iptables 检查" "iptables 命令不可用"
  fi

  # 3.3 检查 ufw (Ubuntu 防火墙)
  if has_cmd ufw; then
    local ufw_status; ufw_status=$(ufw status 2>/dev/null | head -1)
    info "ufw 状态: $ufw_status"
    if echo "$ufw_status" | grep -qi "active"; then
      for port in 80 443; do
        if ufw status 2>/dev/null | grep -q "$port"; then
          ok "ufw 已放行端口 $port"
        else
          fail "ufw 未放行端口 $port"
          record fail "ufw 端口放行" "ufw 未放行端口 $port" "执行: sudo ufw allow $port/tcp"
        fi
      done
    fi
  fi

  # 3.4 云厂商安全组提示
  info "云厂商安全组检查（提示）..."
  # 尝试检测是否在云环境
  local cloud_hint=""
  if [ -f /sys/class/dmi/id/product_name ]; then
    local product; product=$(cat /sys/class/dmi/id/product_name 2>/dev/null)
    case "$product" in
      *"Alibaba"*|*"ECS"*) cloud_hint="阿里云 ECS" ;;
      *"CVM"*|*"Tencent"*) cloud_hint="腾讯云 CVM" ;;
      *"Huawei"*) cloud_hint="华为云" ;;
      *"AWS"*|*"EC2"*) cloud_hint="AWS EC2" ;;
    esac
  fi
  if [ -n "$cloud_hint" ]; then
    warn "检测到可能运行在 ${cloud_hint}，请登录控制台确认安全组已放行端口: ${PORTS_TO_CHECK[*]}"
    record warn "云安全组" "检测到 $cloud_hint 环境，请检查安全组入站规则" \
      "登录云控制台 → 安全组 → 添加入站规则，放行 TCP 端口: ${PORTS_TO_CHECK[*]}"
  else
    # 尝试通过 curl 访问外网来判断
    if try_curl "https://glint.novo.ccwu.cc" "-k" | grep -qE "^(200|301|302)"; then
      ok "外网可访问 HTTPS 首页 (返回 $(try_curl 'https://glint.novo.ccwu.cc' '-k'))"
      record pass "外网可访问性" "HTTPS 首页可达"
    else
      warn "无法从本机访问 HTTPS 首页，可能是安全组/防火墙阻止外部访问"
      record warn "外网可访问性" "HTTPS 首页不可达，请检查安全组" \
        "确认云厂商安全组已放行入站规则: TCP 80, 443"
    fi
  fi
}

# ---------------------------------------------------------------------------
# 第 4 步：Nginx 反向代理规则检查
# ---------------------------------------------------------------------------
nginx_check() {
  h2 "4. Nginx 反向代理规则检查"

  if ! has_cmd nginx; then
    fail "未检测到 Nginx"
    record fail "Nginx 安装" "Nginx 命令不存在" "安装: apt install nginx"
    return
  fi

  # Nginx 服务状态
  if systemctl is-active --quiet nginx 2>/dev/null; then
    ok "Nginx 服务正在运行"
    record pass "Nginx 服务状态" "运行中"
  else
    fail "Nginx 服务未运行"
    record fail "Nginx 服务状态" "Nginx 未运行" "执行: sudo systemctl start nginx"
    return
  fi

  # 检查配置文件是否存在
  if [ -f "$NGINX_SITE_CONF" ]; then
    ok "Nginx 站点配置文件存在: $NGINX_SITE_CONF"
    record pass "Nginx 配置文件" "$NGINX_SITE_CONF 存在"
  else
    warn "未找到预期站点配置: $NGINX_SITE_CONF，搜索其他配置..."
    # 尝试在 nginx 目录中搜索
    local found_conf
    found_conf=$(grep -rl "glint\|/api\|proxy_pass.*8082\|/admin" /etc/nginx/ 2>/dev/null | head -3 || true)
    if [ -n "$found_conf" ]; then
      info "找到相关 Nginx 配置: $found_conf"
      NGINX_SITE_CONF=$(echo "$found_conf" | head -1)
    else
      fail "未找到任何包含项目相关配置的 Nginx 文件"
      record fail "Nginx 配置文件" "未找到项目配置" \
        "将 nginx/glint.novo.ccwu.cc.conf 复制到 /etc/nginx/sites-enabled/ 并执行 nginx -s reload"
      return
    fi
  fi

  # 解析 Nginx 配置中的关键路由
  info "解析 Nginx 反向代理规则..."
  local conf_content
  conf_content=$(cat "$NGINX_SITE_CONF")

  # 检查 /api 代理
  local api_proxy; api_proxy=$(echo "$conf_content" | grep -A2 "location /api" | grep "proxy_pass" | awk '{print $2}' | tr -d ';')
  if [ -n "$api_proxy" ]; then
    ok "API 反向代理: /api → $api_proxy"
    record pass "API 反向代理配置" "/api → $api_proxy"
    # 检测目标地址是否可达
    local proxy_host; proxy_host=$(echo "$api_proxy" | sed -E 's|https?://([^:/]+).*|\1|')
    local proxy_port; proxy_port=$(echo "$api_proxy" | grep -oP ':\d+' | tr -d ':' || echo "80")
    if check_tcp_port "$proxy_host" "${proxy_port:-80}"; then
      ok "代理目标 $proxy_host:${proxy_port:-80} TCP 可达"
    else
      fail "代理目标 $proxy_host:${proxy_port:-80} 不可达！"
      record fail "API 代理目标可达性" "$api_proxy 不可达" \
        "检查后端 Java 服务是否启动，端口是否正确"
    fi
  else
    fail "Nginx 配置中未找到 /api 代理规则"
    record fail "API 反向代理配置" "缺少 /api 的 proxy_pass 规则" \
      "在 Nginx 配置中添加: location /api { proxy_pass http://127.0.0.1:8082; ... }"
  fi

  # 检查 /admin 代理
  local admin_proxy; admin_proxy=$(echo "$conf_content" | grep -A2 "location /admin" | grep "proxy_pass" | awk '{print $2}' | tr -d ';')
  if [ -n "$admin_proxy" ]; then
    ok "管理后台代理: /admin → $admin_proxy"
    record pass "管理后台反向代理" "/admin → $admin_proxy"
  else
    warn "Nginx 配置中未找到 /admin 代理规则"
    record warn "管理后台反向代理" "缺少 /admin 的 proxy_pass 规则"
  fi

  # 检查 / 根路径代理（默认前端）
  local root_proxy; root_proxy=$(echo "$conf_content" | grep -A2 'location /[ ]*{' | grep "proxy_pass" | awk '{print $2}' | tr -d ';')
  if [ -n "$root_proxy" ]; then
    ok "根路径代理: / → $root_proxy"
    record pass "根路径反向代理" "/ → $root_proxy"
  else
    warn "Nginx 配置中未找到根路径代理规则"
    record warn "根路径反向代理" "缺少 / 的 proxy_pass 规则"
  fi

  # 检查 /minio 代理
  local minio_proxy; minio_proxy=$(echo "$conf_content" | grep -A2 "location /minio" | grep "proxy_pass" | awk '{print $2}' | tr -d ';')
  if [ -n "$minio_proxy" ]; then
    ok "MinIO 代理: /minio → $minio_proxy"
    record pass "MinIO 反向代理" "/minio → $minio_proxy"
  else
    warn "Nginx 配置中未找到 /minio 代理规则"
    record warn "MinIO 反向代理" "缺少 /minio 的 proxy_pass 规则"
  fi

  # WebSocket 支持
  if echo "$conf_content" | grep -q "Upgrade\|websocket\|/ws"; then
    ok "WebSocket 反向代理已配置 (/ws)"
    record pass "WebSocket 代理" "已配置 /ws 代理头"
  else
    warn "WebSocket 代理规则未找到，实时功能可能不可用"
    record warn "WebSocket 代理" "缺少 Upgrade/Connection 头配置" \
      "添加: proxy_set_header Upgrade \$http_upgrade; proxy_set_header Connection 'upgrade';"
  fi

  # Nginx 配置语法检查
  if nginx -t &>/dev/null; then
    ok "Nginx 配置语法检查通过"
    record pass "Nginx 语法检查" "配置有效"
  else
    local nginx_err; nginx_err=$(nginx -t 2>&1)
    fail "Nginx 配置语法错误: $nginx_err"
    record fail "Nginx 语法检查" "$nginx_err" "根据错误提示修正配置文件后执行 nginx -s reload"
  fi

  # 检查 Nginx 错误日志中的异常
  if [ -f /var/log/nginx/error.log ]; then
    local recent_errors; recent_errors=$(tail -20 /var/log/nginx/error.log 2>/dev/null | grep -i "upstream\|connect\|timeout\|permission\|denied" | tail -5 || true)
    if [ -n "$recent_errors" ]; then
      warn "Nginx 近期错误日志发现异常:"
      echo "$recent_errors" | while IFS= read -r line; do echo "   $line"; done
      record warn "Nginx 错误日志" "发现近期 upstream/connect 错误" \
        "检查对应后端服务是否运行、端口是否正确、是否有权限问题"
    else
      ok "Nginx 错误日志近期无异常"
      record pass "Nginx 错误日志" "无最近的相关错误"
    fi
  fi
}

# ---------------------------------------------------------------------------
# 第 5 步：静态资源路径、Base URL 及权限检查
# ---------------------------------------------------------------------------
static_resource_check() {
  h2 "5. 静态资源路径、Base URL 及权限检查"

  # 5.1 检查静态资源目录
  for dir in "${STATIC_DIRS[@]}"; do
    if [ -d "$dir" ]; then
      local file_count; file_count=$(find "$dir" -type f 2>/dev/null | wc -l)
      ok "目录存在: $dir (包含 $file_count 个文件)"
      record pass "静态资源目录" "$dir 存在 ($file_count 文件)"

      # 检查权限
      local perms; perms=$(stat -c "%a %U:%G" "$dir" 2>/dev/null || stat -f "%p %u:%g" "$dir" 2>/dev/null)
      if [ -r "$dir" ]; then
        ok "  目录可读: 权限 $perms"
      else
        fail "  目录不可读: 权限 $perms"
        record fail "静态资源权限" "$dir 不可读 ($perms)" \
          "执行: chmod -R 755 $dir && chown -R www-data:www-data $dir"
      fi
    else
      fail "目录不存在: $dir"
      record fail "静态资源目录" "$dir 不存在" \
        "编译前端项目后确保构建产物上传到服务器正确路径"
    fi
  done

  # 5.2 检查 admin 构建配置中的 base 路径
  info "检查前端构建配置中的 Base URL..."
  local admin_vite_config="$PROJECT_DIR/admin/vite.config.ts"
  if [ -f "$admin_vite_config" ]; then
    local admin_base; admin_base=$(grep -oP "base:\s*['\"]([^'\"]+)['\"]" "$admin_vite_config" | grep -oP "['\"]([^'\"]+)['\"]" | tr -d "'\"")
    info "Admin 应用 base 路径: /${admin_base}"
    if [ "$admin_base" = "/admin/" ]; then
      ok "Admin base 路径与 Nginx 代理规则匹配"
      record pass "Admin Base URL" "base=/admin/ 与 Nginx 一致"
    else
      warn "Admin base 路径: /$admin_base (期望 /admin/)"
      record warn "Admin Base URL" "当前值: /$admin_base, 期望: /admin/" \
        "修改 vite.config.ts 中 base 为 '/admin/'"
    fi
  else
    warn "未找到 admin/vite.config.ts，可能尚未部署源码"
    record warn "Admin 构建配置" "未找到 vite.config.ts"
  fi

  # 5.3 检查 index.html 中的资源引用路径
  if [ -f "$PROJECT_DIR/admin/dist/index.html" ]; then
    local resource_paths
    resource_paths=$(grep -oP '(src|href)=["\x27]([^"'\'']+)["\x27]' "$PROJECT_DIR/admin/dist/index.html" | head -5 || true)
    if echo "$resource_paths" | grep -q "^/admin/"; then
      ok "Admin index.html 资源引用以 /admin/ 开头"
      record pass "静态资源引用路径" "资源路径前缀正确 (/admin/)"
    else
      warn "Admin index.html 资源引用可能缺少 /admin/ 前缀"
      info "  示例引用: $resource_paths"
      record warn "静态资源引用路径" "资源路径可能不以 /admin/ 开头" \
        "确保 vite.config.ts 中 base: '/admin/' 并重新编译"
    fi
  fi

  # 5.4 通过 HTTP 请求验证静态资源可访问
  info "通过 HTTP 验证静态资源可访问性..."
  local test_urls=(
    "https://glint.novo.ccwu.cc/admin/"
    "https://glint.novo.ccwu.cc/"
  )
  for url in "${test_urls[@]}"; do
    local http_code; http_code=$(try_curl "$url" "-k")
    case "$http_code" in
      200) ok "HTTP $url → $http_code OK" ;;
      301|302)
        local redirect; redirect=$(try_curl "$url" "-k -L -o /dev/null -w %{url_effective}")
        ok "HTTP $url → $http_code (重定向 → $redirect)"
        ;;
      404) fail "HTTP $url → 404 Not Found (前端资源未部署或路径错误)" ;;
      502) fail "HTTP $url → 502 Bad Gateway (后端服务不可用)" ;;
      000) fail "HTTP $url → 无法连接 (服务未启动或网络不通)" ;;
      *)   warn "HTTP $url → $http_code" ;;
    esac
  done

  # 5.5 验证 API 请求基础路径
  info "验证 API 请求路径..."
  local api_resp; api_resp=$(try_curl_body "https://glint.novo.ccwu.cc/api/v1/actuator/health" "-k")
  if [ -n "$api_resp" ]; then
    if echo "$api_resp" | grep -qi "UP\|ok\|status"; then
      ok "API 健康检查通过: /api/v1/actuator/health → $(echo $api_resp | head -c 200)"
      record pass "API 健康检查" "返回正常状态"
    else
      warn "API 响应异常: $api_resp"
      record warn "API 健康检查" "响应内容不符合预期" "检查后端服务日志"
    fi
  else
    local api_code; api_code=$(try_curl "https://glint.novo.ccwu.cc/api/v1/actuator/health" "-k")
    case "$api_code" in
      404) fail "API 路径返回 404: /api/v1/actuator/health (context-path 或路由配置错误)"
        record fail "API 路径可达性" "返回 404，检查 server.servlet.context-path" ;;
      502) fail "API 返回 502 Bad Gateway (后端 Java 服务不可用)"
        record fail "API 路径可达性" "502 Bad Gateway" \
          "检查 Docker 容器 server 是否运行: docker ps | grep server" ;;
      *)   fail "API 请求失败: HTTP $api_code"
        record fail "API 路径可达性" "HTTP $api_code" ;;
    esac
  fi
}

# ---------------------------------------------------------------------------
# 第 6 步：CORS 跨域策略检查
# ---------------------------------------------------------------------------
cors_check() {
  h2 "6. CORS 跨域策略检查"

  # 6.1 检查服务端 CORS 配置（代码层面）
  local cors_config="$PROJECT_DIR/server/src/main/java/com/zhixun/config/CorsConfig.java"
  if [ -f "$cors_config" ]; then
    ok "CORS 配置类存在: $cors_config"
    local cors_origins; cors_origins=$(grep "allowedOriginPatterns\|setAllowedOriginPatterns\|allowedOrigins" "$cors_config" | head -3)
    info "  CORS 配置: $cors_origins"
    record pass "CORS 配置类" "CorsConfig.java 存在"
  else
    warn "未在本地找到 CorsConfig.java，可能在编译后的 JAR 中"
    record warn "CORS 配置类" "本地源码不可用，部署环境中应在 JAR 内"
  fi

  # 6.2 通过 HTTP OPTIONS 预检请求测试 CORS
  info "发送 CORS 预检请求到 API..."
  local cors_headers; cors_headers=$(try_curl_body "https://glint.novo.ccwu.cc/api/v1/actuator/health" \
    "-k -X OPTIONS -H 'Origin: https://glint.novo.ccwu.cc' -H 'Access-Control-Request-Method: GET' -I -s" 2>/dev/null || true)

  if echo "$cors_headers" | grep -qi "access-control-allow-origin"; then
    local allowed_origin; allowed_origin=$(echo "$cors_headers" | grep -i "access-control-allow-origin" | awk -F': ' '{print $2}' | tr -d '\r')
    ok "CORS 预检通过，允许来源: $allowed_origin"
    record pass "CORS 预检响应" "Access-Control-Allow-Origin: $allowed_origin"
  else
    warn "OPTIONS 请求未返回 Access-Control-Allow-Origin 头"
    warn "  (可能 OPTIONS 被 Spring Security 预处理或 Nginx 代理解析)"
    record warn "CORS 预检响应" "未返回 CORS 头" \
      "检查 CorsConfig.java 中的 allowedOriginPatterns 是否包含 https://glint.novo.ccwu.cc"

    # 尝试直接访问后端（绕过 Nginx）
    info "尝试直接访问后端容器端口验证 CORS..."
    local direct_cors; direct_cors=$(try_curl_body "http://127.0.0.1:8082/api/v1/actuator/health" \
      "-X OPTIONS -H 'Origin: https://glint.novo.ccwu.cc' -H 'Access-Control-Request-Method: GET' -I -s" 2>/dev/null || true)
    if echo "$direct_cors" | grep -qi "access-control-allow-origin"; then
      local direct_origin; direct_origin=$(echo "$direct_cors" | grep -i "access-control-allow-origin" | awk -F': ' '{print $2}' | tr -d '\r')
      ok "直连后端 CORS 正常，允许来源: $direct_origin"
      warn "  问题可能在于 Nginx 未正确传递 CORS 头"
      record warn "Nginx CORS 传递" "Nginx 可能未透传后端 CORS 头" \
        "在 Nginx location /api 中添加: add_header Access-Control-Allow-Origin \$http_origin; 或确保 proxy_pass 正确"
    else
      fail "直连后端 CORS 也未响应，请检查 Java 应用日志"
      record fail "CORS 直连测试" "后端未返回 CORS 头" \
        "检查 application-prod.yml 中 cors.allowed-origins 环境变量 CORS_ALLOWED_ORIGINS 是否正确设置"
    fi
  fi

  # 6.3 检查 docker-compose 中的 CORS 环境变量
  if [ -f "$PROJECT_DIR/docker-compose.yml" ]; then
    local cors_env; cors_env=$(grep -A1 "CORS_ALLOWED_ORIGINS" "$PROJECT_DIR/docker-compose.yml" | head -2)
    info "docker-compose 中 CORS 环境变量: $cors_env"
    if echo "$cors_env" | grep -q "glint.novo.ccwu.cc"; then
      ok "docker-compose 中 CORS 配置包含生产域名"
      record pass "CORS 环境变量" "docker-compose.yml 配置正确"
    else
      fail "docker-compose 中 CORS 配置缺少生产域名"
      record fail "CORS 环境变量" "docker-compose.yml 缺少 glint.novo.ccwu.cc"
    fi
  fi

  # 6.4 验证 CORS 是否限制了当前前端域名
  info "使用不同来源测试 CORS 响应..."
  local test_origins=(
    "https://glint.novo.ccwu.cc"
    "http://glint.novo.ccwu.cc"
    "http://localhost:3500"
  )
  for origin in "${test_origins[@]}"; do
    local resp_origin
    resp_origin=$(try_curl_body "https://glint.novo.ccwu.cc/api/v1/actuator/health" \
      "-k -X OPTIONS -H 'Origin: $origin' -H 'Access-Control-Request-Method: GET' -I -s" 2>/dev/null \
      | grep -i "access-control-allow-origin" | awk -F': ' '{print $2}' | tr -d '\r' || echo "无")
    if [ "$resp_origin" != "无" ]; then
      ok "来源 $origin → CORS 允许 ($resp_origin)"
    else
      warn "来源 $origin → CORS 未返回允许头"
    fi
  done
}

# ---------------------------------------------------------------------------
# 第 7 步：数据库与中间件连通性检查
# ---------------------------------------------------------------------------
middleware_check() {
  h2 "7. 数据库与中间件连通性检查"

  # MySQL
  if docker ps --format '{{.Names}}' 2>/dev/null | grep -qi mysql; then
    local mysql_container; mysql_container=$(docker ps --format '{{.Names}}' 2>/dev/null | grep -i mysql | head -1)
    if docker exec "$mysql_container" mysqladmin ping -h localhost -u root -p"${MYSQL_ROOT_PASSWORD:-123456}" &>/dev/null; then
      ok "MySQL 数据库连接正常 (容器: $mysql_container)"
      record pass "MySQL 连接" "$mysql_container 响应正常"
    else
      fail "MySQL ping 失败"
      record fail "MySQL 连接" "$mysql_container 无法连接" "检查容器日志: docker logs $mysql_container"
    fi
  else
    warn "MySQL 容器未运行"
    record warn "MySQL 连接" "容器未运行"
  fi

  # Redis
  if docker ps --format '{{.Names}}' 2>/dev/null | grep -qi redis; then
    local redis_container; redis_container=$(docker ps --format '{{.Names}}' 2>/dev/null | grep -i redis | head -1)
    if docker exec "$redis_container" redis-cli ping 2>/dev/null | grep -qi PONG; then
      ok "Redis 连接正常 (容器: $redis_container)"
      record pass "Redis 连接" "$redis_container 响应 PONG"
    else
      fail "Redis ping 失败"
      record fail "Redis 连接" "$redis_container 无响应" "检查: docker logs $redis_container"
    fi
  else
    warn "Redis 容器未运行"
    record warn "Redis 连接" "容器未运行"
  fi

  # MinIO
  if docker ps --format '{{.Names}}' 2>/dev/null | grep -qi minio; then
    local minio_resp; minio_resp=$(try_curl_body "http://127.0.0.1:9000/minio/health/live" "")
    if echo "$minio_resp" | grep -q "ok\|{}"; then
      ok "MinIO 对象存储连接正常"
      record pass "MinIO 连接" "健康检查通过"
    else
      warn "MinIO 健康检查异常"
      record warn "MinIO 连接" "健康检查未通过" "检查 MinIO 容器日志"
    fi
  else
    warn "MinIO 容器未运行，静态资源可能无法加载"
    record warn "MinIO 连接" "容器未运行，资源可能走本地 LocalResourceConfig 回退"
  fi

  # OpenSearch
  if docker ps --format '{{.Names}}' 2>/dev/null | grep -qi opensearch; then
    local os_resp; os_resp=$(try_curl_body "http://127.0.0.1:9200/_cluster/health" "")
    if echo "$os_resp" | grep -q '"status"'; then
      local os_status; os_status=$(echo "$os_resp" | grep -oP '"status"\s*:\s*"\K\w+')
      ok "OpenSearch 集群状态: $os_status"
      record pass "OpenSearch 连接" "集群状态: $os_status"
    else
      warn "OpenSearch 健康检查异常"
      record warn "OpenSearch 连接" "集群状态不可用"
    fi
  fi
}

# ---------------------------------------------------------------------------
# 第 8 步：SSL 证书检查（含移动端兼容性）
# ---------------------------------------------------------------------------
ssl_cert_check() {
  h2 "8. SSL/TLS 证书检查（含移动端兼容性）"

  # 8.1 基本证书信息
  local ssl_info; ssl_info=$(echo | openssl s_client -connect glint.novo.ccwu.cc:443 -servername glint.novo.ccwu.cc 2>/dev/null | openssl x509 -noout -dates -issuer -subject 2>/dev/null || true)
  if [ -n "$ssl_info" ]; then
    local not_after; not_after=$(echo "$ssl_info" | grep "notAfter" | cut -d= -f2)
    local issuer; issuer=$(echo "$ssl_info" | grep "issuer" | cut -d= -f2-)
    local subject; subject=$(echo "$ssl_info" | grep "subject" | cut -d= -f2-)
    local expire_epoch; expire_epoch=$(date -d "$not_after" +%s 2>/dev/null || echo 0)
    local now_epoch; now_epoch=$(date +%s)
    local days_left=$(( (expire_epoch - now_epoch) / 86400 ))

    ok "SSL 证书主题: $subject"
    ok "SSL 证书签发者: $issuer"

    if [ "$days_left" -gt 30 ]; then
      ok "SSL 证书有效，剩余 ${days_left} 天"
      record pass "SSL 证书" "有效期剩余 ${days_left} 天"
    elif [ "$days_left" -gt 0 ]; then
      warn "SSL 证书即将过期，剩余 ${days_left} 天"
      record warn "SSL 证书" "即将过期 (${days_left} 天)" "执行 certbot renew 或手动续签证书"
    else
      fail "SSL 证书已过期或无法解析有效期"
      record fail "SSL 证书" "已过期或无效" "立即续签 Let's Encrypt 证书: certbot renew"
    fi
  else
    fail "无法获取 SSL 证书信息"
    record fail "SSL 证书" "无法通过 openssl s_client 获取证书" \
      "检查 443 端口是否监听、域名 DNS 是否解析正确"
    return
  fi

  # 8.2 证书链完整性检查（移动端常见问题）
  info "检查 SSL 证书链完整性（移动端严格要求完整中间证书）..."
  local verify_result; verify_result=$(echo | openssl s_client -connect glint.novo.ccwu.cc:443 -servername glint.novo.ccwu.cc 2>/dev/null | openssl verify -verify_return_error 2>&1 || true)
  if echo "$verify_result" | grep -q "OK"; then
    ok "SSL 证书链验证通过（移动端兼容）"
    record pass "SSL 证书链" "证书链完整，移动端可正常验证"
  elif echo "$verify_result" | grep -q "unable to get local issuer certificate"; then
    fail "SSL 证书链不完整：缺少中间证书！这是移动端无法访问的常见原因"
    record fail "SSL 证书链" "缺少中间证书，移动端会拒绝连接" \
      "执行: sudo certbot renew --force-renewal 或手动拼接完整证书链: cat fullchain.pem privkey.pem > bundle.pem"
  else
    warn "SSL 证书链验证异常: $verify_result"
    record warn "SSL 证书链" "验证返回: $verify_result" "检查 /etc/letsencrypt/live/ 下的证书文件是否完整"
  fi

  # 8.3 TLS 版本兼容性检查（移动端兼容）
  info "检查 TLS 版本支持情况（移动端兼容性）..."
  for tls_ver in "tls1_2" "tls1_3"; do
    local tls_result; tls_result=$(echo | openssl s_client -connect glint.novo.ccwu.cc:443 -servername glint.novo.ccwu.cc "-${tls_ver}" 2>&1 | grep -i "verify return code\|CONNECTED\|Cipher" || true)
    if echo "$tls_result" | grep -qi "verify return code"; then
      local ver_name; ver_name=$(echo "$tls_ver" | sed 's/tls1_2/TLSv1.2/;s/tls1_3/TLSv1.3/')
      ok "${ver_name} 连接成功（移动端兼容）"
    else
      fail "${tls_ver} 连接失败"
      record fail "TLS 版本" "${tls_ver} 连接失败，移动端可能无法访问" \
        "在 Nginx 中添加 ssl_protocols 配置: ssl_protocols TLSv1.2 TLSv1.3;"
    fi
  done

  # 8.4 证书文件存在性检查
  local cert_path="/etc/letsencrypt/live/glint.novo.ccwu.cc"
  if [ -f "$cert_path/fullchain.pem" ] && [ -f "$cert_path/privkey.pem" ]; then
    ok "证书文件存在: $cert_path/"
    local cert_size; cert_size=$(wc -c < "$cert_path/fullchain.pem")
    local key_size; key_size=$(wc -c < "$cert_path/privkey.pem")
    info "  fullchain.pem: ${cert_size} bytes, privkey.pem: ${key_size} bytes"
    record pass "SSL 证书文件" "fullchain.pem + privkey.pem 存在"
  else
    fail "证书文件缺失: $cert_path/"
    record fail "SSL 证书文件" "fullchain.pem 或 privkey.pem 不存在" \
      "申请证书: sudo certbot certonly --webroot -w /var/www/certbot -d glint.novo.ccwu.cc"
  fi
}

# ---------------------------------------------------------------------------
# 第 8.5 步：移动端 HTTP 头兼容性检查
# ---------------------------------------------------------------------------
mobile_header_check() {
  h2 "8.5 移动端 HTTP 头兼容性检查"

  info "检查 Nginx 返回的响应头（模拟移动端 User-Agent）..."
  local mobile_ua="Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1"
  local resp_headers; resp_headers=$(curl -sI -k -A "$mobile_ua" --connect-timeout 10 "https://glint.novo.ccwu.cc/" 2>/dev/null || true)

  if [ -z "$resp_headers" ]; then
    fail "移动端模拟请求失败：无法获取 HTTPS 响应头"
    record fail "移动端请求" "无法获取响应，可能是 SSL 证书链问题" \
      "优先检查 SSL 证书链完整性（见第 8 步）"
    return
  fi

  # 检查 HTTP 状态码
  local http_status; http_status=$(echo "$resp_headers" | head -1 | awk '{print $2}')
  case "$http_status" in
    200) ok "HTTP 状态码: 200 OK (移动端可用)" 
         record pass "移动端 HTTP 状态" "200 OK" ;;
    301|302) 
      local redirect; redirect=$(echo "$resp_headers" | grep -i "^location:" | awk '{print $2}' | tr -d '\r')
      if echo "$redirect" | grep -qi "https"; then
        ok "HTTP 重定向到 HTTPS: $redirect (正常)"
        record pass "移动端 HTTP 状态" "HTTPS 重定向正常"
      else
        warn "HTTP 重定向异常: $redirect"
        record warn "移动端 HTTP 状态" "重定向: $redirect" "检查 Nginx 重定向规则"
      fi
      ;;
    403) fail "HTTP 403 Forbidden - 移动端被拒绝访问"
         record fail "移动端 HTTP 状态" "403 Forbidden" \
           "检查安全组/防火墙/WAF 是否误拦截移动端 User-Agent" ;;
    502) fail "HTTP 502 Bad Gateway - 后端服务不可用"
         record fail "移动端 HTTP 状态" "502 Bad Gateway" "检查 Docker 容器运行状态" ;;
    *)   warn "HTTP 状态码: $http_status"
         record warn "移动端 HTTP 状态" "状态码: $http_status" ;;
  esac

  # 检查 Content-Security-Policy 是否对移动端友好
  local csp; csp=$(echo "$resp_headers" | grep -i "content-security-policy:" | tr -d '\r' || true)
  if [ -n "$csp" ]; then
    info "检测到 CSP 策略: ${csp:0:100}..."
    if echo "$csp" | grep -q "script-src.*'self'"; then
      if echo "$csp" | grep -q "script-src.*https:"; then
        ok "CSP 允许 HTTPS 脚本加载（移动端兼容）"
        record pass "CSP 移动端" "HTTPS 脚本加载已允许"
      else
        warn "CSP script-src 仅允许 'self'，外部 HTTPS 脚本可能被阻止"
        record warn "CSP 移动端" "script-src 未包含 https:" \
          "建议在 CSP 中添加 https: 以兼容移动端浏览器"
      fi
    fi
    if echo "$csp" | grep -q "connect-src"; then
      if echo "$csp" | grep -q "connect-src.*wss:"; then
        ok "CSP 允许安全 WebSocket 连接（移动端兼容）"
        record pass "CSP WebSocket" "wss: 已允许"
      else
        warn "CSP 未包含 wss:，移动端 WebSocket 可能被阻止"
        record warn "CSP WebSocket" "缺少 wss:" \
          "在 CSP connect-src 中添加 wss:"
      fi
    fi
  else
    info "未检测到 CSP 策略（无 CSP 通常不影响移动端访问）"
    record pass "CSP 移动端" "无 CSP 策略，不影响访问"
  fi

  # 检查 HSTS 是否过于严格
  local hsts; hsts=$(echo "$resp_headers" | grep -i "strict-transport-security:" | tr -d '\r' || true)
  if echo "$hsts" | grep -qi "includeSubDomains"; then
    warn "HSTS 包含 includeSubDomains，可能导致子域名证书问题影响移动端"
    record warn "HSTS includeSubDomains" "过于严格的 HSTS 策略可能影响移动端" \
      "若子域名无 HTTPS，建议移除 includeSubDomains"
  else
    ok "HSTS 策略适中（无 includeSubDomains）"
    record pass "HSTS 策略" "移动端兼容"
  fi

  # 检查是否有 viewport 等移动端友好的 meta 标签（需实际请求页面内容）
  info "获取移动端页面 HTML 检查..."
  local html_content; html_content=$(curl -s -k -A "$mobile_ua" --connect-timeout 10 "https://glint.novo.ccwu.cc/" 2>/dev/null | head -50 || true)
  if echo "$html_content" | grep -q "viewport"; then
    ok "HTML 包含 viewport meta 标签（移动端适配）"
    record pass "移动端 viewport" "viewport meta 标签存在"
    if echo "$html_content" | grep -q "viewport-fit=cover"; then
      ok "viewport 包含 viewport-fit=cover（刘海屏兼容）"
      record pass "移动端刘海屏" "viewport-fit=cover 已配置"
    else
      warn "viewport 缺少 viewport-fit=cover（刘海屏可能显示异常）"
      record warn "移动端刘海屏" "缺少 viewport-fit=cover"
    fi
  else
    warn "HTML 未检测到 viewport 标签"
    record warn "移动端 viewport" "缺少 viewport meta 标签" \
      "在页面 <head> 中添加: <meta name=\"viewport\" content=\"width=device-width, initial-scale=1, viewport-fit=cover\">"
  fi
}

# ---------------------------------------------------------------------------
# 汇总报告
# ---------------------------------------------------------------------------
print_summary() {
  h2 "诊断汇总报告"

  echo ""
  echo "  ┌─────────────────────────────────────────┐"
  echo "  │         智讯部署诊断报告 v1.0           │"
  echo "  ├─────────────────────────────────────────┤"
  printf "  │  检查项总数: %-27d │\n" "$TOTAL_CHECKS"
  printf "  │  通过:     ${GREEN}%-27d${NC} │\n" "$PASS_CHECKS"
  printf "  │  失败:     ${RED}%-27d${NC} │\n" "$FAIL_CHECKS"
  printf "  │  警告:     ${YELLOW}%-27d${NC} │\n" "$WARN_CHECKS"
  echo "  └─────────────────────────────────────────┘"
  echo ""

  if [ "$FAIL_CHECKS" -gt 0 ]; then
    echo -e "${RED}⚠ 发现 ${FAIL_CHECKS} 个严重问题，请优先处理！${NC}"
    echo ""
    echo "============================================"
    echo "  常见问题快速修复指南"
    echo "============================================"
    echo ""
    echo "1. 端口未监听 / 502 Bad Gateway"
    echo "   → 检查 Docker 容器: cd $PROJECT_DIR && docker-compose ps"
    echo "   → 重启服务: docker-compose up -d"
    echo ""
    echo "2. Nginx 配置错误"
    echo "   → 检查语法: nginx -t"
    echo "   → 重载配置: nginx -s reload"
    echo ""
    echo "3. 跨域 (CORS) 错误"
    echo "   → 确认 docker-compose.yml 中 CORS_ALLOWED_ORIGINS 包含正确域名"
    echo "   → 重启 server 容器使环境变量生效: docker-compose restart server server-2"
    echo ""
    echo "4. 静态资源 404 / 路径错误"
    echo "   → Admin: 确认 admin/vite.config.ts 中 base: '/admin/' 后重新编译"
    echo "   → 重新构建: cd admin && npm run build && 上传 dist 到服务器"
    echo ""
    echo "5. API 请求路径不匹配"
    echo "   → 确认 server.servlet.context-path=/api (application.yml)"
    echo "   → 确认 Nginx 中 proxy_pass 指向正确的后端端口 (当前: 8082)"
    echo "   → 确认 Nginx location /api 后不应再拼接 /api 前缀"
    echo ""
    echo "6. 云安全组未放行端口"
    echo "   → 阿里云: ECS 控制台 → 安全组 → 添加入方向规则"
    echo "   → 腾讯云: 控制台 → 安全组 → 添加规则"
    echo "   → 放行端口: TCP 80, 443, 22（及业务所需端口）"
    echo ""
    echo "7. 静态资源权限问题 (403 Forbidden)"
    echo "   → chmod -R 755 $PROJECT_DIR/admin/dist $PROJECT_DIR/client/.output/public"
    echo "   → chown -R www-data:www-data $PROJECT_DIR/admin/dist (适配 Nginx 用户)"
    echo ""
  else
    echo -e "${GREEN}✓ 所有检查项均已通过，系统运行正常！${NC}"
  fi
  echo ""
}

# ---------------------------------------------------------------------------
# 自动修复（仅当指定 --fix 时执行）
# ---------------------------------------------------------------------------
auto_fix() {
  if ! $AUTO_FIX; then return; fi

  h2 "自动修复模式 (实验性)"

  warn "自动修复可能会修改系统配置，请确认已做好备份"

  # 修复 1: Nginx 配置重载
  if nginx -t &>/dev/null && systemctl is-active --quiet nginx 2>/dev/null; then
    info "重载 Nginx 配置..."
    nginx -s reload && ok "Nginx 重载成功" || fail "Nginx 重载失败"
  fi

  # 修复 2: 尝试重启已停止的容器
  for container in "${REQUIRED_CONTAINERS[@]}"; do
    local matching; matching=$(docker ps -a --format '{{.Names}} {{.Status}}' 2>/dev/null | grep -i "$container" | head -1 || true)
    if [ -n "$matching" ]; then
      local cname; cname=$(echo "$matching" | awk '{print $1}')
      if echo "$matching" | grep -qi "Exited\|Stopped"; then
        info "尝试启动已停止的容器: $cname"
        docker start "$cname" && ok "容器 $cname 已启动" || fail "容器 $cname 启动失败"
      fi
    fi
  done

  # 修复 3: 静态资源目录权限
  for dir in "${STATIC_DIRS[@]}"; do
    if [ -d "$dir" ] && [ ! -r "$dir" ]; then
      info "修复目录权限: $dir"
      chmod -R 755 "$dir" && chown -R www-data:www-data "$dir" 2>/dev/null || true
      ok "已尝试修复 $dir 权限"
    fi
  done

  # 修复 4: 清理 Docker 悬空镜像释放磁盘
  if [ "${disk_pct:-0}" -gt 85 ] 2>/dev/null; then
    info "磁盘使用率过高，清理 Docker 悬空镜像..."
    docker image prune -f && ok "已清理悬空镜像"
  fi
}

# ---------------------------------------------------------------------------
# 主入口
# ---------------------------------------------------------------------------
main() {
  # 解析参数
  for arg in "$@"; do
    case "$arg" in
      --json) JSON_MODE=true ;;
      --fix)  AUTO_FIX=true ;;
      -h|--help)
        echo "用法: bash diagnose.sh [--json] [--fix]"
        echo "  --json  以 JSON 格式输出结果"
        echo "  --fix   尝试自动修复可修复的问题"
        exit 0
        ;;
    esac
  done

  # 切换到项目目录
  if [ -d "$PROJECT_DIR" ]; then
    cd "$PROJECT_DIR" || true
  else
    warn "项目目录 $PROJECT_DIR 不存在，部分检查可能受限"
  fi

  echo ""
  echo "╔══════════════════════════════════════════════════════════╗"
  echo "║        智讯 (Zhixun) 部署自动诊断工具 v1.0              ║"
  echo "║        诊断时间: $(date '+%Y-%m-%d %H:%M:%S')                  ║"
  echo "╚══════════════════════════════════════════════════════════╝"

  baseline_check
  container_check
  port_firewall_check
  nginx_check
  static_resource_check
  cors_check
  middleware_check
  ssl_cert_check
  mobile_header_check
  auto_fix
  print_summary

  # JSON 输出
  if $JSON_MODE; then
    echo "$JSON_RESULTS" | jq '.'
  fi
}

main "$@"
