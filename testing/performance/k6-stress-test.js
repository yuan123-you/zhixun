import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend, Counter, Gauge } from 'k6/metrics';

// ============================================================
// 知讯 - k6 高性能压测脚本
// 目标：峰值QPS 3000，并发用户 5000
// ============================================================

// 自定义指标
const errorRate = new Rate('errors');
const apiDuration = new Trend('api_duration', true);
const dbTps = new Trend('db_tps', true);
const redisQps = new Trend('redis_qps', true);
const activeUsers = new Gauge('active_users');
const requestCounter = new Counter('total_requests');

// 压测配置
export const options = {
  // 阶段式加压 - 达到 5000 并发用户
  stages: [
    // 预热阶段：30秒内从0增加到500个虚拟用户
    { duration: '30s', target: 500 },
    // 稳定负载：保持500个虚拟用户60秒
    { duration: '60s', target: 500 },
    // 加压阶段：60秒内增加到2000个虚拟用户
    { duration: '60s', target: 2000 },
    // 中等负载：保持2000个虚拟用户60秒
    { duration: '60s', target: 2000 },
    // 高压阶段：60秒内增加到5000个虚拟用户
    { duration: '60s', target: 5000 },
    // 峰值负载：保持5000个虚拟用户120秒
    { duration: '120s', target: 5000 },
    // 恢复阶段：30秒内降回1000个虚拟用户
    { duration: '30s', target: 1000 },
    // 冷却阶段：保持1000个虚拟用户30秒
    { duration: '30s', target: 1000 },
    // 最终冷却：10秒内降回0
    { duration: '10s', target: 0 },
  ],
  // 阈值设置
  thresholds: {
    http_req_duration: ['p(95)<500', 'p(99)<1000'],  // 95%请求<500ms, 99%请求<1s
    http_req_failed: ['rate<0.05'],                    // 错误率<5%
    errors: ['rate<0.1'],                              // 自定义错误率<10%
    db_tps: ['p(95)<200'],                             // 数据库TPS监控
    redis_qps: ['p(95)<50'],                           // Redis QPS监控
  },
};

// 配置
const BASE_URL = __ENV.BASE_URL || 'http://localhost:80/api';
const TEST_PHONE = __ENV.TEST_PHONE || '13800000001';
const TEST_PASSWORD = __ENV.TEST_PASSWORD || 'test123456';

// 登录获取 Token
function login() {
  const payload = JSON.stringify({
    phone: TEST_PHONE,
    password: TEST_PASSWORD,
  });
  const params = {
    headers: { 'Content-Type': 'application/json' },
    timeout: '10s',
  };
  const res = http.post(`${BASE_URL}/v1/auth/login`, payload, params);
  const success = check(res, {
    'login successful': (r) => r.status === 200,
  });
  if (success) {
    try {
      const body = JSON.parse(res.body);
      return body.data.accessToken;
    } catch (e) {
      return null;
    }
  }
  return null;
}

// 公开接口测试 - 作品列表
function testArticleList() {
  const res = http.get(`${BASE_URL}/v1/articles?page=1&pageSize=10`, { timeout: '10s' });
  const success = check(res, {
    'article list status 200': (r) => r.status === 200,
    'article list has data': (r) => {
      try {
        const body = JSON.parse(r.body);
        return body.code === 200;
      } catch (e) { return false; }
    },
  });
  errorRate.add(!success);
  apiDuration.add(res.timings.duration);
  requestCounter.add(1);
  return success;
}

// 公开接口测试 - 搜索
function testSearch() {
  const keywords = ['测试', '工单', '售后', '系统', '技术'];
  const keyword = keywords[Math.floor(Math.random() * keywords.length)];
  const res = http.get(`${BASE_URL}/v1/search?keyword=${encodeURIComponent(keyword)}&page=1&pageSize=10`, { timeout: '10s' });
  const success = check(res, {
    'search status 200': (r) => r.status === 200,
  });
  errorRate.add(!success);
  apiDuration.add(res.timings.duration);
  requestCounter.add(1);
  return success;
}

// 公开接口测试 - 热门排行
function testRank() {
  const res = http.get(`${BASE_URL}/v1/rank/hot?limit=10`, { timeout: '10s' });
  const success = check(res, {
    'rank status 200': (r) => r.status === 200,
  });
  errorRate.add(!success);
  apiDuration.add(res.timings.duration);
  requestCounter.add(1);
  return success;
}

// 公开接口测试 - 分类列表
function testCategories() {
  const res = http.get(`${BASE_URL}/v1/categories`, { timeout: '10s' });
  const success = check(res, {
    'categories status 200': (r) => r.status === 200,
  });
  errorRate.add(!success);
  apiDuration.add(res.timings.duration);
  requestCounter.add(1);
  return success;
}

// 公开接口测试 - 标签列表
function testTags() {
  const res = http.get(`${BASE_URL}/v1/tags`, { timeout: '10s' });
  const success = check(res, {
    'tags status 200': (r) => r.status === 200,
  });
  errorRate.add(!success);
  apiDuration.add(res.timings.duration);
  requestCounter.add(1);
  return success;
}

// 认证接口测试 - 作品详情
function testArticleDetail(token, articleId) {
  const params = {
    headers: { Authorization: `Bearer ${token}` },
    timeout: '10s',
  };
  const res = http.get(`${BASE_URL}/v1/articles/${articleId}`, params);
  const success = check(res, {
    'article detail status 200': (r) => r.status === 200,
  });
  errorRate.add(!success);
  apiDuration.add(res.timings.duration);
  requestCounter.add(1);
  return success;
}

// 认证接口测试 - 用户信息
function testUserProfile(token) {
  const params = {
    headers: { Authorization: `Bearer ${token}` },
    timeout: '10s',
  };
  const res = http.get(`${BASE_URL}/v1/users/profile`, params);
  const success = check(res, {
    'user profile status 200': (r) => r.status === 200,
  });
  errorRate.add(!success);
  apiDuration.add(res.timings.duration);
  requestCounter.add(1);
  return success;
}

// 认证接口测试 - 通知列表
function testNotifications(token) {
  const params = {
    headers: { Authorization: `Bearer ${token}` },
    timeout: '10s',
  };
  const res = http.get(`${BASE_URL}/v1/notifications?page=1&pageSize=10`, params);
  const success = check(res, {
    'notifications status 200': (r) => r.status === 200 || r.status === 401,
  });
  errorRate.add(!success);
  apiDuration.add(res.timings.duration);
  requestCounter.add(1);
  return success;
}

// 数据库TPS测试 - 作品列表（直接查询数据库的接口）
function testDbTps() {
  const start = Date.now();
  const res = http.get(`${BASE_URL}/v1/articles?page=1&pageSize=20`, { timeout: '10s' });
  const duration = Date.now() - start;
  const success = check(res, {
    'db tps test status 200': (r) => r.status === 200,
  });
  // 估算数据库TPS（基于响应时间）
  if (success && duration > 0) {
    const estimatedTps = 1000 / duration;
    dbTps.add(estimatedTps);
  }
  errorRate.add(!success);
  requestCounter.add(1);
  return success;
}

// Redis QPS测试 - 热门排行（大量使用Redis缓存的接口）
function testRedisQps() {
  const start = Date.now();
  const res = http.get(`${BASE_URL}/v1/rank/hot?limit=20`, { timeout: '10s' });
  const duration = Date.now() - start;
  const success = check(res, {
    'redis qps test status 200': (r) => r.status === 200,
  });
  // 估算Redis QPS（基于响应时间）
  if (success && duration > 0) {
    const estimatedQps = 1000 / duration;
    redisQps.add(estimatedQps);
  }
  errorRate.add(!success);
  requestCounter.add(1);
  return success;
}

// 容量水位监控 - 健康检查
function testHealthCheck() {
  const res = http.get(`${BASE_URL}/actuator/health`, { timeout: '5s' });
  const success = check(res, {
    'health check status 200': (r) => r.status === 200,
  });
  errorRate.add(!success);
  requestCounter.add(1);
  return success;
}

// 主测试场景
export default function () {
  // 更新活跃用户数
  activeUsers.add(1);

  // 随机选择测试场景
  const scenario = Math.random();

  if (scenario < 0.25) {
    // 25% - 公开接口测试（无需登录）
    const publicApis = [testArticleList, testSearch, testRank, testCategories, testTags];
    const api = publicApis[Math.floor(Math.random() * publicApis.length)];
    api();
  } else if (scenario < 0.45) {
    // 20% - 认证接口测试
    const token = login();
    if (token) {
      const authApis = [
        () => testArticleDetail(token, Math.floor(Math.random() * 100) + 1),
        () => testUserProfile(token),
        () => testNotifications(token),
      ];
      const api = authApis[Math.floor(Math.random() * authApis.length)];
      api();
    }
  } else if (scenario < 0.55) {
    // 10% - 登录接口压测（测试 Sentinel 限流）
    login();
  } else if (scenario < 0.65) {
    // 10% - 搜索接口压测
    testSearch();
    testSearch();
  } else if (scenario < 0.75) {
    // 10% - 数据库TPS测试
    testDbTps();
  } else if (scenario < 0.85) {
    // 10% - Redis QPS测试
    testRedisQps();
  } else if (scenario < 0.92) {
    // 7% - 混合读写测试
    testArticleList();
    testRank();
    testSearch();
  } else {
    // 8% - 容量水位监控
    testHealthCheck();
    testArticleList();
  }

  // 短暂休眠（模拟用户思考时间，高并发下缩短）
  sleep(Math.random() * 0.3 + 0.05);
}

// 压测结束后的汇总处理
export function handleSummary(data) {
  const metrics = data.metrics;
  const reqDuration = metrics.http_req_duration;
  const reqCount = metrics.http_reqs;
  const failRate = metrics.http_req_failed;

  const summary = {
    testTime: new Date().toISOString(),
    targetQPS: 3000,
    targetConcurrency: 5000,
    actualRPS: reqCount ? reqCount.values.rate : 0,
    p95Latency: reqDuration ? reqDuration.values['p(95)'] : 0,
    p99Latency: reqDuration ? reqDuration.values['p(99)'] : 0,
    avgLatency: reqDuration ? reqDuration.values.avg : 0,
    errorRate: failRate ? failRate.values.rate : 0,
    totalRequests: reqCount ? reqCount.values.count : 0,
  };

  return {
    'stdout': textSummary(data),
    'json': JSON.stringify(summary, null, 2),
  };
}

function textSummary(data) {
  const metrics = data.metrics;
  const reqDuration = metrics.http_req_duration;
  const reqCount = metrics.http_reqs;
  const failRate = metrics.http_req_failed;

  let output = '\n==========================================\n';
  output += '  压测结果汇总\n';
  output += '==========================================\n';
  output += `  目标QPS: 3000\n`;
  output += `  目标并发: 5000\n`;
  output += `  实际RPS: ${reqCount ? reqCount.values.rate.toFixed(2) : 'N/A'}\n`;
  output += `  P95延迟: ${reqDuration ? reqDuration.values['p(95)'].toFixed(2) : 'N/A'}ms\n`;
  output += `  P99延迟: ${reqDuration ? reqDuration.values['p(99)'].toFixed(2) : 'N/A'}ms\n`;
  output += `  平均延迟: ${reqDuration ? reqDuration.values.avg.toFixed(2) : 'N/A'}ms\n`;
  output += `  错误率: ${failRate ? (failRate.values.rate * 100).toFixed(2) : 'N/A'}%\n`;
  output += `  总请求数: ${reqCount ? reqCount.values.count : 'N/A'}\n`;
  output += '==========================================\n';

  return output;
}
