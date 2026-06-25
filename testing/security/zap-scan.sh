#!/bin/bash
# ============================================================
# OWASP ZAP 自动化安全扫描脚本
# 用途：对知讯系统进行自动化漏洞扫描
# 前置条件：已安装 Docker
# ============================================================

set -e

# ==================== 配置 ====================
TARGET_URL="${TARGET_URL:-http://localhost:80}"
REPORT_DIR="$(dirname "$0")/reports/zap"
REPORT_DATE=$(date +%Y%m%d_%H%M%S)

# 创建报告目录
mkdir -p "${REPORT_DIR}"

echo "========================================"
echo "OWASP ZAP 安全扫描"
echo "目标: ${TARGET_URL}"
echo "报告目录: ${REPORT_DIR}"
echo "========================================"

# ==================== 基线扫描（快速） ====================
echo ""
echo "[1/3] 执行基线扫描（Baseline Scan）..."
docker run -t --rm \
  -v "${REPORT_DIR}:/zap/reports:rw" \
  -u "$(id -u):$(id -g)" \
  zaproxy/zap-stable \
  zap-baseline.py \
  -t "${TARGET_URL}/api/v1" \
  -f "report" \
  -w "baseline_report_${REPORT_DATE}.md" \
  -r "baseline_report_${REPORT_DATE}.html" \
  -j \
  -a \
  || true

echo "[1/3] 基线扫描完成"

# ==================== 全面扫描（深度） ====================
echo ""
echo "[2/3] 执行全面扫描（Full Scan）..."
docker run -t --rm \
  -v "${REPORT_DIR}:/zap/reports:rw" \
  -u "$(id -u):$(id -g)" \
  zaproxy/zap-stable \
  zap-full-scan.py \
  -t "${TARGET_URL}/api/v1" \
  -f "report" \
  -w "full_report_${REPORT_DATE}.md" \
  -r "full_report_${REPORT_DATE}.html" \
  -j \
  -a \
  || true

echo "[2/3] 全面扫描完成"

# ==================== API 扫描 ====================
echo ""
echo "[3/3] 执行 API 扫描..."
# 先生成 OpenAPI 规范文件（如有的话）
docker run -t --rm \
  -v "${REPORT_DIR}:/zap/reports:rw" \
  -u "$(id -u):$(id -g)" \
  zaproxy/zap-stable \
  zap-api-scan.py \
  -t "${TARGET_URL}/api/v3/api-docs" \
  -f "openapi" \
  -d \
  -w "api_report_${REPORT_DATE}.md" \
  -r "api_report_${REPORT_DATE}.html" \
  || true

echo "[3/3] API 扫描完成"

# ==================== 结果汇总 ====================
echo ""
echo "========================================"
echo "安全扫描全部完成！"
echo "报告保存位置: ${REPORT_DIR}"
echo "========================================"
echo ""
echo "扫描报告说明："
echo "  - baseline_report: 基线扫描结果（快速，覆盖OWASP Top 10）"
echo "  - full_report: 全面扫描结果（深度爬取+主动扫描）"
echo "  - api_report: API安全扫描结果"
echo ""
echo "风险等级说明："
echo "  - High: 高危漏洞，必须修复"
echo "  - Medium: 中危漏洞，建议修复"
echo "  - Low: 低危漏洞，可选修复"
echo "  - Informational: 信息性提示"
