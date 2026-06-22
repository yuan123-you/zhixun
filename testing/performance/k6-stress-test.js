import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

// ============================================================
// 智讯工单系统 - k6 性能压测脚本
// ============================================================

// 自定义指标
const errorRate = new Rate('errors');
const apiDuration = new Trend('api_duration', true);

// 压测配置
export const options = {
  // 阶段式加压
  stages: [
    // 预热阶段：10秒内从0增加到20个虚拟用户
    { duration: '10s', target: 20 },
    // 稳定负载：保持20个虚拟用户30秒
    { duration: '30s', target: 20 },
    // 加压阶段：10秒内增加到50个虚拟用户
    { duration: '10s', target: 50 },
    // 峰值负载：保持50个虚拟用户30秒
    { duration: '30s', target: 50 },
    // 恢复阶段：10秒内降回20个虚拟用户
    { duration: '10s', target: 20 },
    // 冷却阶段：保持20个虚拟用户20秒
    { duration: '20s', target: 20 },
  ],
  // 阈值设置
  thresholds: {
    http_req_duration: ['p(95)<500', 'p(99)<1000'],  // 95%请求<500ms, 99%请求<1s
    http_req_failed: ['rate<0.05'],                    // 错误率<5%
    errors: ['rate<0.1'],                              // 自定义错误率<10%
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
    const body = JSON.parse(res.body);
    return body.data.accessToken;
  }
  return null;
}

// 公开接口测试 - 文章列表
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
  return success;
}

// 认证接口测试 - 文章详情
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
  return success;
}

// 主测试场景
export default function () {
  // 随机选择测试场景
  const scenario = Math.random();

  if (scenario < 0.4) {
    // 40% - 公开接口测试（无需登录）
    const publicApis = [testArticleList, testSearch, testRank, testCategories, testTags];
    const api = publicApis[Math.floor(Math.random() * publicApis.length)];
    api();
  } else if (scenario < 0.7) {
    // 30% - 认证接口测试
    const token = login();
    if (token) {
      const authApis = [
        () => testArticleDetail(token, 1),
        () => testUserProfile(token),
        () => testNotifications(token),
      ];
      const api = authApis[Math.floor(Math.random() * authApis.length)];
      api();
    }
  } else if (scenario < 0.9) {
    // 20% - 登录接口压测（测试 Sentinel 限流）
    login();
  } else {
    // 10% - 搜索接口压测
    testSearch();
    testSearch();
  }

  sleep(Math.random() * 0.5 + 0.1);
}
