"""
专项测试：模拟注入数据后点击所有交互元素
- 覆盖所有页面
- 通过 store / 直接 patch 状态让列表有数据
- 然后执行点击、切换、提交
"""
import sys
from pathlib import Path
from playwright.sync_api import sync_playwright

BASE_URL = "http://localhost:3500"

NETWORK_KW = [
    "/api/", "ws://", "wss://", "Failed to fetch", "NetworkError", "net::ERR_",
    "AxiosError", "Request failed", "ECONNREFUSED", "ERR_CONNECTION_REFUSED",
    "ERR_CONNECTION", "ERR_NAME_NOT_RESOLVED", "ERR_EMPTY_RESPONSE", "ERR_ABORTED",
    "WebSocket connection", "WebSocket failed", "Failed to load resource",
    "timeout of", "500 (Internal Server", "404 (Not Found)", "403 (Forbidden)",
    "401 (Unauthorized)", "Request aborted", "aborted",
    "Download the Vue Devtools", "[HMR]", "[vite]",
]


def is_net(text):
    if not text:
        return True
    for k in NETWORK_KW:
        if k.lower() in text.lower():
            return True
    return False


# 注入测试数据，覆盖所有列表式组件的渲染分支
INJECT_PATCHES = """
// 拦截 fetch，让 API 返回测试数据
(function() {
  const fakeData = {
    '/api/v1/articles/feed': { data: { data: { list: [
      { id: 1, title: '测试文章1', summary: '摘要1', coverImage: '', author: { id: 1, nickname: '用户A', avatar: '' }, stats: { likes: 10, comments: 5, views: 100, favorites: 3 }, createdAt: '2024-01-01' },
      { id: 2, title: '测试文章2', summary: '摘要2', coverImage: '', author: { id: 2, nickname: '用户B', avatar: '' }, stats: { likes: 20, comments: 10, views: 200, favorites: 5 }, createdAt: '2024-01-02' }
    ], total: 2, hasMore: false } } },
    '/api/v1/articles/hot': { data: { data: { list: [
      { id: 1, title: '热门1', stats: { likes: 100, comments: 50, views: 1000 } },
      { id: 2, title: '热门2', stats: { likes: 200, comments: 60, views: 2000 } }
    ] } } },
    '/api/v1/articles/rank': { data: { data: { list: [
      { id: 1, title: '排行1', hotScore: 999, stats: { likes: 100 } },
      { id: 2, title: '排行2', hotScore: 888, stats: { likes: 80 } }
    ] } } },
    '/api/v1/tags/hot': { data: { data: { list: [{ id: 1, name: 'Vue', count: 100 }, { id: 2, name: 'React', count: 80 }] } } },
    '/api/v1/users/recommend': { data: { data: { list: [{ id: 1, nickname: '推荐用户1', avatar: '', bio: '简介1' }] } } },
    '/api/v1/topics': { data: { data: { list: [
      { id: 1, name: '话题A', description: '描述A', articleCount: 10, followCount: 5, isFollowed: false, isOfficial: true },
      { id: 2, name: '话题B', description: '描述B', articleCount: 20, followCount: 8, isFollowed: true, isOfficial: false }
    ], total: 2, hasMore: false } } },
    '/api/v1/groups': { data: { data: { list: [
      { id: 1, name: '群组A', description: '描述', memberCount: 100 }
    ] } } },
    '/api/v1/categories': { data: { data: { list: [
      { id: 1, name: '技术', slug: 'tech', color: '#3b82f6' }
    ] } } },
    '/api/v1/categories/tech': { data: { data: { list: [
      { id: 1, title: '技术文章', summary: '摘要', author: { id: 1, nickname: '作者', avatar: '' } }
    ] } } },
    '/api/v1/articles/search': { data: { data: { list: [] } } },
    '/api/v1/search/suggestions': { data: { data: [] } },
    '/api/v1/search/hot': { data: { data: [] } },
    '/api/v1/users/1': { data: { data: { id: 1, nickname: '用户1', avatar: '', bio: '简介', followCount: 10, followerCount: 5, articleCount: 3 } } },
    '/api/v1/users/1/articles': { data: { data: { list: [{ id: 1, title: '作品', summary: 's', stats: { likes: 1 } }] } } },
    '/api/v1/users/1/followers': { data: { data: { list: [] } } },
    '/api/v1/users/1/following': { data: { data: { list: [] } } },
    '/api/v1/articles/1': { data: { data: { id: 1, title: '作品详情', content: '内容', author: { id: 1, nickname: '作者', avatar: '' }, stats: { likes: 10, comments: 5, views: 100, favorites: 3 } } } },
    '/api/v1/articles/preview/1': { data: { data: { id: 1, title: '预览', content: 'c', author: { id: 1, nickname: 'a' } } } },
    '/api/v1/topics/1': { data: { data: { id: 1, name: '话题A', description: '描述A', articleCount: 10 } } },
    '/api/v1/comments': { data: { data: { list: [] } } },
    '/api/v1/notifications': { data: { data: { list: [], unreadCount: 0 } } },
    '/api/v1/conversations': { data: { data: { list: [] } } },
    '/api/v1/messages/1': { data: { data: { list: [] } } },
    '/api/v1/users/me': { data: { data: { id: 1, nickname: '我', avatar: '' } } },
    '/api/v1/groups/1': { data: { data: { id: 1, name: '群', members: [] } } },
    '/api/v1/groups/1/messages': { data: { data: { list: [] } } },
    '/api/v1/articles/me': { data: { data: { list: [] } } },
  };

  const origFetch = window.fetch;
  window.fetch = async function(url, opts) {
    const urlStr = typeof url === 'string' ? url : url.url;
    for (const key in fakeData) {
      if (urlStr.includes(key)) {
        return new Response(JSON.stringify(fakeData[key]), {
          status: 200, headers: { 'Content-Type': 'application/json' }
        });
      }
    }
    // 让其他请求进入真实流程（会失败）— 但这不会在控制台抛 JS 错误
    return origFetch.apply(this, arguments);
  };
})();
"""


def test_with_data(page, path, name):
    """访问页面 + 注入数据 + 点击交互"""
    print(f"\n[INJECT] {name} -> {path}")
    errors = []
    warns = []
    js_errs = []

    def on_console(msg):
        if msg.type == "error" and not is_net(msg.text):
            errors.append(msg.text)
        if msg.type == "warning" and not is_net(msg.text):
            warns.append(msg.text)
    def on_err(err):
        text = str(err)
        if not is_net(text):
            js_errs.append(text)
    page.on("console", on_console)
    page.on("pageerror", on_err)

    # 注入 fetch patcher
    page.add_init_script(INJECT_PATCHES)
    try:
        page.goto(BASE_URL + path, wait_until="domcontentloaded", timeout=20000)
    except Exception as e:
        errors.append(f"goto: {e}")
    try:
        page.wait_for_load_state("networkidle", timeout=10000)
    except Exception:
        pass
    page.wait_for_timeout(1500)

    # 滚动
    try:
        page.evaluate("window.scrollTo(0, document.body.scrollHeight)")
        page.wait_for_timeout(500)
        page.evaluate("window.scrollTo(0, 0)")
        page.wait_for_timeout(300)
    except Exception:
        pass

    # 点击所有可见按钮（带保护）
    try:
        btns = page.locator("button:visible")
        n = min(btns.count(), 6)
        for i in range(n):
            try:
                btns.nth(i).click(timeout=2000, force=True)
                page.wait_for_timeout(250)
            except Exception:
                pass
    except Exception:
        pass

    # 点击卡片（这是关键测试 - topics 卡片等）
    try:
        cards = page.locator(".cursor-pointer:visible, [class*='cursor-pointer']:visible, [class*='card']:visible")
        n = min(cards.count(), 4)
        for i in range(n):
            try:
                cards.nth(i).click(timeout=2000, force=True)
                page.wait_for_timeout(300)
            except Exception:
                pass
    except Exception:
        pass

    # tab 切换
    try:
        tabs = page.locator("[role='tab']:visible, .el-tabs__item:visible")
        n = min(tabs.count(), 3)
        for i in range(n):
            try:
                tabs.nth(i).click(timeout=2000, force=True)
                page.wait_for_timeout(250)
            except Exception:
                pass
    except Exception:
        pass

    # 链接点击
    try:
        links = page.locator("a:visible")
        n = min(links.count(), 5)
        for i in range(n):
            try:
                # 阻止真实跳转
                href = links.nth(i).get_attribute("href")
                if href and not href.startswith("http"):
                    links.nth(i).click(timeout=2000, force=True)
                    page.wait_for_timeout(200)
            except Exception:
                pass
    except Exception:
        pass

    # 输入
    try:
        inputs = page.locator("input:visible, textarea:visible")
        n = min(inputs.count(), 3)
        for i in range(n):
            try:
                inputs.nth(i).fill("test", timeout=1500)
                page.wait_for_timeout(100)
            except Exception:
                pass
    except Exception:
        pass

    page.wait_for_timeout(800)
    return errors, warns, js_errs


def main():
    routes = [
        ("/", "Home"),
        ("/rank", "Rank"),
        ("/discover", "Discover"),
        ("/tags", "Tags"),
        ("/search", "Search"),
        ("/topics", "TopicsSquare"),
        ("/groups", "GroupsSquare"),
        ("/category/tech", "Category"),
        ("/login", "Login"),
        ("/register", "Register"),
        ("/forgot-password", "ForgotPassword"),
        ("/articles/1", "ArticleDetail"),
        ("/topics/1", "TopicDetail"),
        ("/user/1", "UserHome"),
        ("/user/1/followers", "UserFollowers"),
        ("/user/1/following", "UserFollowing"),
        ("/user/preview/1", "UserPreview"),
        ("/groups/1", "GroupChat"),
        ("/messages/1", "MessageChat"),
    ]
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True, args=["--no-sandbox", "--disable-gpu"])
        context = browser.new_context(viewport={"width": 1366, "height": 800})
        any_problem = False
        for path, name in routes:
            page = context.new_page()
            errors, warns, js_errs = test_with_data(page, path, name)
            total = len(errors) + len(warns) + len(js_errs)
            if total:
                any_problem = True
                print(f"   FOUND {total} issues (err={len(errors)} warn={len(warns)} jse={len(js_errs)})")
                for e in errors[:3]:
                    print(f"     ERR: {e[:250]}")
                for w in warns[:3]:
                    print(f"     WARN: {w[:250]}")
                for e in js_errs[:3]:
                    print(f"     JS: {e[:250]}")
            else:
                print(f"   OK")
            page.close()
        browser.close()
        print("\n" + "=" * 60)
        if not any_problem:
            print(">>> ALL PAGES OK with simulated data! <<<")
        else:
            print(">>> Some issues found, see above <<<")


if __name__ == "__main__":
    main()
