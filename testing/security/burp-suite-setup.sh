#!/bin/bash
# ============================================================
# Burp Suite Community 自动化安全测试辅助脚本
# 用途：配合 Burp Suite Community Edition 进行手动渗透测试
# 说明：Burp Suite Community 版本不支持命令行自动化扫描，
#       本脚本提供环境准备和辅助工具
# ============================================================

set -e

echo "========================================"
echo "Burp Suite Community 安全测试辅助工具"
echo "========================================"

# ==================== 配置 ====================
TARGET_URL="${TARGET_URL:-http://localhost:80}"
PROXY_HOST="127.0.0.1"
PROXY_PORT="8080"
SCOPE_FILE="$(dirname "$0")/burp_scope.txt"
PAYLOADS_DIR="$(dirname "$0")/payloads"

# 创建目录
mkdir -p "${PAYLOADS_DIR}"

# ==================== 生成 Scope 文件 ====================
echo ""
echo "[1/4] 生成 Burp Suite Scope 配置..."
cat > "${SCOPE_FILE}" << EOF
# Burp Suite Target Scope
# 将以下内容添加到 Burp Suite -> Target -> Site map -> Scope
${TARGET_URL}/api/v1/auth
${TARGET_URL}/api/v1/articles
${TARGET_URL}/api/v1/comments
${TARGET_URL}/api/v1/search
${TARGET_URL}/api/v1/messages
${TARGET_URL}/api/v1/files
${TARGET_URL}/api/v1/users
${TARGET_URL}/api/v1/categories
${TARGET_URL}/api/v1/tags
${TARGET_URL}/api/v1/feed
${TARGET_URL}/api/v1/rank
${TARGET_URL}/api/v1/notifications
${TARGET_URL}/api/v1/admin
EOF
echo "Scope 配置已生成: ${SCOPE_FILE}"

# ==================== 生成测试 Payload ====================
echo ""
echo "[2/4] 生成安全测试 Payload..."

# SQL 注入测试 Payload
cat > "${PAYLOADS_DIR}/sqli.txt" << 'EOF'
'
"
' OR '1'='1
' OR '1'='1' --
' OR '1'='1' /*'
1' OR '1'='1
admin'--
admin' OR '1'='1
' UNION SELECT NULL--
' UNION SELECT NULL,NULL--
' UNION SELECT NULL,NULL,NULL--
1; DROP TABLE users--
1 AND 1=1
1 AND 1=2
1' AND '1'='1
1' AND '1'='2
EOF

# XSS 测试 Payload
cat > "${PAYLOADS_DIR}/xss.txt" << 'EOF'
<script>alert('XSS')</script>
<img src=x onerror=alert('XSS')>
<svg onload=alert('XSS')>
<iframe src="javascript:alert('XSS')">
"><script>alert('XSS')</script>
'><script>alert('XSS')</script>
javascript:alert('XSS')
%3Cscript%3Ealert('XSS')%3C/script%3E
< body onload=alert('XSS')>
<input onfocus=alert('XSS') autofocus>
<marquee onstart=alert('XSS')>
EOF

# 路径遍历测试 Payload
cat > "${PAYLOADS_DIR}/path-traversal.txt" << 'EOF'
../../../etc/passwd
..\\..\\..\\windows\\system32\\config\\sam
....//....//....//etc/passwd
%2e%2e%2f%2e%2e%2f%2e%2e%2fetc%2fpasswd
..%252f..%252f..%252fetc/passwd
/etc/passwd
C:\Windows\System32\drivers\etc\hosts
EOF

# 命令注入测试 Payload
cat > "${PAYLOADS_DIR}/command-injection.txt" << 'EOF'
; ls -la
| ls -la
`ls -la`
$(ls -la)
& dir
&& dir
|| ls -la
; cat /etc/passwd
| cat /etc/passwd
`id`
$(whoami)
EOF

echo "Payload 文件已生成到: ${PAYLOADS_DIR}/"

# ==================== 生成 Burp 配置提示 ====================
echo ""
echo "[3/4] 生成 Burp Suite 配置指南..."
cat > "$(dirname "$0")/burp_setup_guide.txt" << GUIDE
========================================
Burp Suite Community Edition 配置指南
========================================

1. 启动 Burp Suite Community Edition

2. 配置代理：
   - Proxy -> Options -> Proxy Listeners
   - 添加监听: ${PROXY_HOST}:${PROXY_PORT}
   - 勾选 "All interfaces"

3. 配置浏览器代理：
   - 设置 HTTP 代理为 ${PROXY_HOST}:${PROXY_PORT}

4. 导入 CA 证书（首次使用）：
   - 浏览器访问 http://burp
   - 下载并安装 CA 证书

5. 设置 Target Scope：
   - Target -> Site map -> 右键 -> Add to scope
   - 或手动添加 scope.txt 中的 URL

6. 推荐测试流程：
   a) 手动浏览网站各功能，Burp 自动记录请求
   b) 使用 Proxy -> HTTP history 查看请求
   c) 右键 -> Send to Intruder 进行参数爆破
   d) 右键 -> Send to Repeater 进行手动重放
   e) 使用 Scanner (Community版有限) 进行扫描

7. 重点测试接口：
   - POST /api/v1/auth/login (SQL注入、暴力破解)
   - POST /api/v1/auth/register (参数篡改)
   - GET /api/v1/search?keyword= (XSS、SQL注入)
   - POST /api/v1/articles (XSS、权限绕过)
   - POST /api/v1/articles/{id}/comments (XSS)
   - POST /api/v1/messages/send (XSS、IDOR)
   - POST /api/v1/files/upload (文件上传漏洞)
   - PUT /api/v1/auth/password (CSRF、权限绕过)

8. 安全检查清单：
   [ ] 认证与授权
       - JWT Token 是否可伪造
       - 越权访问（水平/垂直）
       - 会话管理是否安全
   [ ] 输入验证
       - SQL 注入
       - XSS（反射型/存储型）
       - 路径遍历
       - 命令注入
   [ ] 文件上传
       - 文件类型绕过
       - 文件名注入
       - 存储型 XSS
   [ ] API 安全
       - IDOR（不安全的直接对象引用）
       - 批量赋值
       - 速率限制绕过
   [ ] 配置安全
       - CORS 配置
       - HTTP 安全头
       - 敏感信息泄露
GUIDE
echo "配置指南已生成"

# ==================== 生成测试用登录凭据 ====================
echo ""
echo "[4/4] 生成测试辅助信息..."
echo ""
echo "========================================"
echo "Burp Suite 安全测试准备完成！"
echo "========================================"
echo ""
echo "下一步操作："
echo "  1. 启动 Burp Suite Community Edition"
echo "  2. 配置浏览器代理指向 ${PROXY_HOST}:${PROXY_PORT}"
echo "  3. 参考 burp_setup_guide.txt 进行配置"
echo "  4. 使用 payloads/ 目录下的 Payload 进行测试"
echo ""
echo "测试账号（开发环境）："
echo "  普通用户: phone=13800000001, password=test123456"
echo "  管理员:   phone=13800000000, password=admin123456"
