#!/bin/bash
# ============================================================
# 知讯系统 - 统一测试执行脚本
# 包含：安全测试、代码分析、性能压测
# ============================================================

set -e

PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
REPORT_DIR="${PROJECT_ROOT}/testing/reports"
REPORT_DATE=$(date +%Y%m%d_%H%M%S)

# 创建报告目录
mkdir -p "${REPORT_DIR}/zap"
mkdir -p "${REPORT_DIR}/sonarqube"
mkdir -p "${REPORT_DIR}/jmeter"
mkdir -p "${REPORT_DIR}/k6"

echo "========================================"
echo "知讯系统 - 统一测试执行"
echo "项目根目录: ${PROJECT_ROOT}"
echo "报告目录: ${REPORT_DIR}"
echo "========================================"

# ==================== 1. SonarQube 代码安全分析 ====================
run_sonarqube() {
    echo ""
    echo "========================================"
    echo "[1/4] SonarQube 代码安全静态分析"
    echo "========================================"

    # 检查 SonarQube 是否运行
    if curl -s http://localhost:9000 > /dev/null 2>&1; then
        echo "SonarQube 服务已启动"
    else
        echo "正在启动 SonarQube 服务..."
        cd "${PROJECT_ROOT}"
        docker compose up -d sonarqube sonarqube-db
        echo "等待 SonarQube 启动（约60秒）..."
        sleep 60
    fi

    # 执行代码分析
    echo "执行代码分析..."
    cd "${PROJECT_ROOT}/server"
    mvn clean verify sonar:sonar \
        -Dsonar.host.url=http://localhost:9000 \
        -Dsonar.login=${SONAR_TOKEN:-} \
        2>&1 | tee "${REPORT_DIR}/sonarqube/analysis_${REPORT_DATE}.log"

    echo "[1/4] SonarQube 分析完成"
    echo "查看结果: http://localhost:9000"
}

# ==================== 2. OWASP ZAP 安全扫描 ====================
run_zap_scan() {
    echo ""
    echo "========================================"
    echo "[2/4] OWASP ZAP 安全扫描"
    echo "========================================"

    bash "${PROJECT_ROOT}/testing/security/zap-scan.sh"
    echo "[2/4] OWASP ZAP 扫描完成"
}

# ==================== 3. JMeter 性能压测 ====================
run_jmeter() {
    echo ""
    echo "========================================"
    echo "[3/4] JMeter 性能压测"
    echo "========================================"

    # 检查 JMeter 是否安装
    if command -v jmeter &> /dev/null; then
        jmeter -n \
            -t "${PROJECT_ROOT}/testing/performance/jmeter-stress-test.jmx" \
            -l "${REPORT_DIR}/jmeter/results_${REPORT_DATE}.jtl" \
            -e -o "${REPORT_DIR}/jmeter/report_${REPORT_DATE}" \
            -JBASE_URL=localhost \
            -JPORT=80
        echo "[3/4] JMeter 压测完成"
        echo "HTML 报告: ${REPORT_DIR}/jmeter/report_${REPORT_DATE}/index.html"
    else
        echo "JMeter 未安装，使用 Docker 运行..."
        docker run --rm \
            -v "${PROJECT_ROOT}/testing/performance:/jmeter" \
            -v "${REPORT_DIR}/jmeter:/results" \
            justb4/jmeter \
            -n \
            -t /jmeter/jmeter-stress-test.jmx \
            -l /results/results_${REPORT_DATE}.jtl \
            -e -o /results/report_${REPORT_DATE} \
            -JBASE_URL=host.docker.internal \
            -JPORT=80
        echo "[3/4] JMeter 压测完成（Docker）"
    fi
}

# ==================== 4. k6 性能压测 ====================
run_k6() {
    echo ""
    echo "========================================"
    echo "[4/4] k6 性能压测"
    echo "========================================"

    if command -v k6 &> /dev/null; then
        k6 run \
            --out json="${REPORT_DIR}/k6/results_${REPORT_DATE}.json" \
            --summary-export="${REPORT_DIR}/k6/summary_${REPORT_DATE}.json" \
            "${PROJECT_ROOT}/testing/performance/k6-stress-test.js"
        echo "[4/4] k6 压测完成"
    else
        echo "k6 未安装，使用 Docker 运行..."
        docker run --rm \
            --add-host=host.docker.internal:host-gateway \
            -v "${PROJECT_ROOT}/testing/performance:/scripts" \
            -v "${REPORT_DIR}/k6:/results" \
            grafana/k6 run \
            --out json=/results/results_${REPORT_DATE}.json \
            --summary-export=/results/summary_${REPORT_DATE}.json \
            /scripts/k6-stress-test.js
        echo "[4/4] k6 压测完成（Docker）"
    fi
}

# ==================== 执行入口 ====================
case "${1:-all}" in
    sonarqube|sonar)
        run_sonarqube
        ;;
    zap)
        run_zap_scan
        ;;
    jmeter)
        run_jmeter
        ;;
    k6)
        run_k6
        ;;
    security)
        run_sonarqube
        run_zap_scan
        ;;
    performance|perf)
        run_jmeter
        run_k6
        ;;
    all)
        run_sonarqube
        run_zap_scan
        run_jmeter
        run_k6
        ;;
    *)
        echo "用法: $0 {all|sonarqube|zap|jmeter|k6|security|performance}"
        echo ""
        echo "  all         - 执行所有测试"
        echo "  sonarqube   - 仅执行 SonarQube 代码分析"
        echo "  zap         - 仅执行 OWASP ZAP 安全扫描"
        echo "  jmeter      - 仅执行 JMeter 性能压测"
        echo "  k6          - 仅执行 k6 性能压测"
        echo "  security    - 执行安全相关测试（SonarQube + ZAP）"
        echo "  performance - 执行性能压测（JMeter + k6）"
        exit 1
        ;;
esac

echo ""
echo "========================================"
echo "所有测试执行完毕！"
echo "报告目录: ${REPORT_DIR}"
echo "========================================"
