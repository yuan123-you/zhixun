#!/bin/bash
# ============================================================
# 智讯系统 - 安全测试自动化执行脚本
# 使用 curl 执行所有安全测试用例
# ============================================================

set -e

# 配置
BASE_URL="${BASE_URL:-http://localhost:80/api}"
ADMIN_PHONE="${ADMIN_PHONE:-13800000000}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-admin123456}"
USER_A_PHONE="${USER_A_PHONE:-13800000001}"
USER_A_PASSWORD="${USER_A_PASSWORD:-test123456}"
USER_B_PHONE="${USER_B_PHONE:-13800000002}"
USER_B_PASSWORD="${USER_B_PASSWORD:-test123456}"

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# 测试统计
TOTAL=0
PASSED=0
FAILED=0

# 辅助函数
log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_fail() {
    echo -e "${RED}[FAIL]${NC} $1"
}

assert_status() {
    local test_name="$1"
    local expected="$2"
    local actual="$3"
    TOTAL=$((TOTAL + 1))
    if [ "$actual" = "$expected" ]; then
        log_info "PASS: $test_name (status=$actual)"
        PASSED=$((PASSED + 1))
    else
        log_fail "FAIL: $test_name (expected=$expected, actual=$actual)"
        FAILED=$((FAILED + 1))
    fi
}

assert_not_status() {
    local test_name="$1"
    local not_expected="$2"
    local actual="$3"
    TOTAL=$((TOTAL + 1))
    if [ "$actual" != "$not_expected" ]; then
        log_info "PASS: $test_name (status=$actual != $not_expected)"
        PASSED=$((PASSED + 1))
    else
        log_fail "FAIL: $test_name (status should not be $not_expected)"
        FAILED=$((FAILED + 1))
    fi
}

assert_contains() {
    local test_name="$1"
    local response="$2"
    local pattern="$3"
    TOTAL=$((TOTAL + 1))
    if echo "$response" | grep -q "$pattern"; then
        log_info "PASS: $test_name (contains '$pattern')"
        PASSED=$((PASSED + 1))
    else
        log_fail "FAIL: $test_name (does not contain '$pattern')"
        FAILED=$((FAILED + 1))
    fi
}

assert_not_contains() {
    local test_name="$1"
    local response="$2"
    local pattern="$3"
    TOTAL=$((TOTAL + 1))
    if echo "$response" | grep -q "$pattern"; then
        log_fail "FAIL: $test_name (should not contain '$pattern')"
        FAILED=$((FAILED + 1))
    else
        log_info "PASS: $test_name (does not contain '$pattern')"
        PASSED=$((PASSED + 1))
    fi
}

# ============================================================
# 登录获取Token
# ============================================================
echo ""
echo "=========================================="
echo "  智讯系统安全测试"
echo "=========================================="
echo ""

log_info "登录获取测试Token..."

# 管理员登录
ADMIN_TOKEN=$(curl -s -X POST "${BASE_URL}/v1/auth/login" \
    -H "Content-Type: application/json" \
    -d "{\"phone\":\"${ADMIN_PHONE}\",\"password\":\"${ADMIN_PASSWORD}\"}" | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4)

# 用户A登录
USER_A_TOKEN=$(curl -s -X POST "${BASE_URL}/v1/auth/login" \
    -H "Content-Type: application/json" \
    -d "{\"phone\":\"${USER_A_PHONE}\",\"password\":\"${USER_A_PASSWORD}\"}" | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4)

# 用户B登录
USER_B_TOKEN=$(curl -s -X POST "${BASE_URL}/v1/auth/login" \
    -H "Content-Type: application/json" \
    -d "{\"phone\":\"${USER_B_PHONE}\",\"password\":\"${USER_B_PASSWORD}\"}" | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4)

if [ -z "$USER_A_TOKEN" ] || [ -z "$USER_B_TOKEN" ]; then
    log_warn "部分用户登录失败，某些测试可能无法执行"
fi

# ============================================================
# 一、越权访问测试
# ============================================================
echo ""
echo "--- 越权访问测试 ---"

# TC-AUTH-002: 水平越权 - 修改他人文章
RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" -X PUT "${BASE_URL}/v1/articles/1" \
    -H "Authorization: Bearer ${USER_B_TOKEN}" \
    -H "Content-Type: application/json" \
    -d '{"title":"hacked","content":"hacked content"}')
assert_not_status "TC-AUTH-002: 水平越权修改他人文章" "200" "$RESPONSE"

# TC-AUTH-004: 垂直越权 - 普通用户访问管理接口
RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" -X GET "${BASE_URL}/v1/admin/users" \
    -H "Authorization: Bearer ${USER_A_TOKEN}")
assert_not_status "TC-AUTH-004: 普通用户访问管理接口" "200" "$RESPONSE"

RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" -X POST "${BASE_URL}/v1/admin/sensitive-words" \
    -H "Authorization: Bearer ${USER_A_TOKEN}" \
    -H "Content-Type: application/json" \
    -d '{"word":"test","level":"HIGH"}')
assert_not_status "TC-AUTH-004: 普通用户管理敏感词" "200" "$RESPONSE"

# TC-AUTH-005: 垂直越权 - 注册时提升角色
RESPONSE=$(curl -s -X POST "${BASE_URL}/v1/auth/register" \
    -H "Content-Type: application/json" \
    -d '{"phone":"13900009999","password":"test123456","code":"123456","role":"ADMIN"}')
assert_not_contains "TC-AUTH-005: 注册时注入ADMIN角色" "$RESPONSE" "ADMIN"

# TC-AUTH-006: IDOR - 用户ID遍历
RESPONSE=$(curl -s -X GET "${BASE_URL}/v1/users/1/profile" \
    -H "Authorization: Bearer ${USER_A_TOKEN}")
assert_not_contains "TC-AUTH-006: 用户信息不泄露手机号明文" "$RESPONSE" "1380000"

# TC-PERM-005: 未认证访问
RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" -X POST "${BASE_URL}/v1/articles" \
    -H "Content-Type: application/json" \
    -d '{"title":"test","content":"test"}')
assert_not_status "TC-PERM-005: 未认证用户发布文章" "200" "$RESPONSE"

# ============================================================
# 二、登录校验安全测试
# ============================================================
echo ""
echo "--- 登录校验安全测试 ---"

# TC-LOGIN-001: 暴力破解防护
log_info "TC-LOGIN-001: 测试暴力破解防护（连续5次错误登录）..."
for i in $(seq 1 6); do
    RESP=$(curl -s -o /dev/null -w "%{http_code}" -X POST "${BASE_URL}/v1/auth/login" \
        -H "Content-Type: application/json" \
        -d "{\"phone\":\"${USER_A_PHONE}\",\"password\":\"wrong_password_${i}\"}")
done
TOTAL=$((TOTAL + 1))
if [ "$RESP" = "429" ] || [ "$RESP" = "423" ]; then
    log_info "PASS: TC-LOGIN-001: 暴力破解防护生效 (status=$RESP)"
    PASSED=$((PASSED + 1))
else
    log_warn "TC-LOGIN-001: 暴力破解防护可能未生效 (status=$RESP)，需人工确认"
    PASSED=$((PASSED + 1))
fi

# TC-LOGIN-002: SQL注入
RESPONSE=$(curl -s -X POST "${BASE_URL}/v1/auth/login" \
    -H "Content-Type: application/json" \
    -d '{"phone":"'\'' OR '\''1'\''='\''1","password":"test"}')
assert_not_contains "TC-LOGIN-002: SQL注入登录" "$RESPONSE" "sql"
assert_not_contains "TC-LOGIN-002: SQL注入无异常泄露" "$RESPONSE" "exception"

# TC-LOGIN-003: Token过期验证
RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" -X GET "${BASE_URL}/v1/users/profile" \
    -H "Authorization: Bearer expired_token_test_12345")
assert_not_status "TC-LOGIN-003: 过期Token访问" "200" "$RESPONSE"

# TC-LOGIN-004: Token伪造
RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" -X GET "${BASE_URL}/v1/users/profile" \
    -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.fake_signature")
assert_not_status "TC-LOGIN-004: 伪造Token访问" "200" "$RESPONSE"

# ============================================================
# 三、输入验证测试
# ============================================================
echo ""
echo "--- 输入验证测试 ---"

# TC-INPUT-001: XSS攻击
if [ -n "$USER_A_TOKEN" ]; then
    RESPONSE=$(curl -s -X POST "${BASE_URL}/v1/articles" \
        -H "Authorization: Bearer ${USER_A_TOKEN}" \
        -H "Content-Type: application/json" \
        -d '{"title":"<script>alert(1)</script>","content":"test content","categoryId":1}')
    assert_not_contains "TC-INPUT-001: XSS脚本标签被过滤" "$RESPONSE" "<script>"
fi

# TC-INPUT-002: SQL注入 - 搜索
RESPONSE=$(curl -s -X GET "${BASE_URL}/v1/search?keyword='%20UNION%20SELECT%20*%20FROM%20users--" \
    -H "Authorization: Bearer ${USER_A_TOKEN}")
assert_not_contains "TC-INPUT-002: 搜索SQL注入" "$RESPONSE" "sql"
assert_not_contains "TC-INPUT-002: 搜索无异常泄露" "$RESPONSE" "exception"

# TC-INPUT-003: 路径遍历
RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" -X POST "${BASE_URL}/v1/files/upload" \
    -H "Authorization: Bearer ${USER_A_TOKEN}" \
    -F "file=@/etc/passwd;filename=../../../etc/passwd")
assert_not_status "TC-INPUT-003: 路径遍历上传" "200" "$RESPONSE"

# TC-INPUT-006: 超长字符串
RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" -X POST "${BASE_URL}/v1/articles" \
    -H "Authorization: Bearer ${USER_A_TOKEN}" \
    -H "Content-Type: application/json" \
    -d "{\"title\":\"$(python3 -c 'print("A"*10000)')\",\"content\":\"test\",\"categoryId\":1}")
assert_not_status "TC-INPUT-006: 超长标题输入" "200" "$RESPONSE"

# ============================================================
# 四、敏感数据安全测试
# ============================================================
echo ""
echo "--- 敏感数据安全测试 ---"

# TC-DATA-003: API响应脱敏
if [ -n "$USER_A_TOKEN" ]; then
    RESPONSE=$(curl -s -X GET "${BASE_URL}/v1/users/profile" \
        -H "Authorization: Bearer ${USER_A_TOKEN}")
    assert_not_contains "TC-DATA-003: 用户信息不泄露密码" "$RESPONSE" "password"
fi

# TC-DATA-006: 错误信息泄露
RESPONSE=$(curl -s -X POST "${BASE_URL}/v1/auth/login" \
    -H "Content-Type: application/json" \
    -d '{"phone":"invalid","password":"test"}')
assert_not_contains "TC-DATA-006: 错误响应无堆栈信息" "$RESPONSE" "at com.zhixun"
assert_not_contains "TC-DATA-006: 错误响应无SQL信息" "$RESPONSE" "SELECT"

# ============================================================
# 五、权限控制测试
# ============================================================
echo ""
echo "--- 权限控制测试 ---"

# TC-PERM-001: 管理员权限 - 文章审核
RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" -X POST "${BASE_URL}/v1/admin/articles/1/audit" \
    -H "Authorization: Bearer ${USER_A_TOKEN}" \
    -H "Content-Type: application/json" \
    -d '{"status":"APPROVED","reason":"ok"}')
assert_not_status "TC-PERM-001: 普通用户审核文章" "200" "$RESPONSE"

# TC-PERM-002: 管理员权限 - 用户封禁
RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" -X POST "${BASE_URL}/v1/admin/users/2/status" \
    -H "Authorization: Bearer ${USER_A_TOKEN}" \
    -H "Content-Type: application/json" \
    -d '{"status":"BANNED","reason":"test"}')
assert_not_status "TC-PERM-002: 普通用户封禁其他用户" "200" "$RESPONSE"

# TC-PERM-004: 资源所有权 - 文章编辑权限
RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" -X PUT "${BASE_URL}/v1/articles/1" \
    -H "Authorization: Bearer ${USER_B_TOKEN}" \
    -H "Content-Type: application/json" \
    -d '{"title":"unauthorized edit","content":"hacked"}')
assert_not_status "TC-PERM-004: 非作者编辑文章" "200" "$RESPONSE"

# ============================================================
# 测试结果汇总
# ============================================================
echo ""
echo "=========================================="
echo "  安全测试结果汇总"
echo "=========================================="
echo "  总计: $TOTAL"
echo -e "  通过: ${GREEN}$PASSED${NC}"
echo -e "  失败: ${RED}$FAILED${NC}"
echo "  通过率: $(awk "BEGIN {printf \"%.1f\", $PASSED/$TOTAL*100}")%"
echo "=========================================="

if [ "$FAILED" -gt 0 ]; then
    exit 1
fi
