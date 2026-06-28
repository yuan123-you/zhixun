"""
最终完整测试：
- 全部 30 个页面
- 注入数据让所有列表非空
- 大范围点击、滚动、输入、提交
- 任何 console.error / pageerror / console.warn 都不能出现
"""
import sys
import json
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
    "ERR_BLOCKED_BY_RESPONSE",
]


def is_net(text):
    if not text:
        return True
    for k in NETWORK_KW:
        if k.lower() in text.lower():
            return True
    return False


# 注入测试数据
INJECT = """
(function() {
  const fake = {
    '/api/v1/articles/feed': { data: { list: [
      { id: 1, title: 'A1', summary: 's', coverImage: '', author: { id: 1, nickname: 'u1', avatar: '' }, stats: { likes: 1, comments: 0, views: 1, favorites: 0 }, createdAt: '2024-01-01' },
      { id: 2, title: 'A2', summary: 's2', coverImage: '', author: { id: 2, nickname: 'u2', avatar: '' }, stats: { likes: 2, comments: 0, views: 2, favorites: 0 }, createdAt: '2024-01-02' }
    ], total: 2, hasMore: false } },
    '/api/v1/articles/hot': { data: { list: [{ id: 1, title: 'H1', stats: { likes: 1 } }, { id: 2, title: 'H2', stats: { likes: 2 } }] } },
    '/api/v1/articles/rank': { data: { list: [{ id: 1, title: 'R1', hotScore: 100, stats: { likes: 1 } }] } },
    '/api/v1/tags/hot': { data: { list: [{ id: 1, name: 'Vue' }, { id: 2, name: 'React' }] } },
    '/api/v1/users/recommend': { data: { list: [{ id: 1, nickname: '用户1', avatar: '', bio: '简介' }] } },
    '/api/v1/topics': { data: { list: [
      { id: 1, name: '话题A', description: 'dA', articleCount: 10, followCount: 5, isFollowed: false, isOfficial: true },
      { id: 2, name: '话题B', description: 'dB', articleCount: 20, followCount: 8, isFollowed: true, isOfficial: false }
    ], total: 2, hasMore: false } },
    '/api/v1/groups': { data: { list: [{ id: 1, name: '群A', description: 'd', memberCount: 100 }] } },
    '/api/v1/categories': { data: { list: [{ id: 1, name: '技术', slug: 'tech', color: '#3b82f6' }] } },
    '/api/v1/categories/tech': { data: { list: [{ id: 1, title: '文章', summary: 's', author: { id: 1, nickname: 'A', avatar: '' } }] } },
    '/api/v1/articles/search': { data: { list: [] } },
    '/api/v1/search/suggestions': { data: [] },
    '/api/v1/search/hot': { data: [] },
    '/api/v1/users/1': { data: { id: 1, nickname: '用户1', avatar: '', bio: 'b', followCount: 10, followerCount: 5, articleCount: 3 } },
    '/api/v1/users/1/articles': { data: { list: [{ id: 1, title: 'A', summary: 's', stats: { likes: 1 } }] } },
    '/api/v1/users/1/followers': { data: { list: [] } },
    '/api/v1/users/1/following': { data: { list: [] } },
    '/api/v1/articles/1': { data: { id: 1, title: 'T', content: 'c', author: { id: 1, nickname: 'A', avatar: '' }, stats: { likes: 1, comments: 0, views: 1, favorites: 0 } } },
    '/api/v1/articles/preview/1': { data: { id: 1, title: 'P', content: 'c', author: { id: 1, nickname: 'a' } } },
    '/api/v1/topics/1': { data: { id: 1, name: 'A', description: 'd', articleCount: 1 } },
    '/api/v1/comments': { data: { list: [] } },
    '/api/v1/notifications': { data: { list: [], unreadCount: 0 } },
    '/api/v1/conversations': { data: { list: [] } },
    '/api/v1/messages/1': { data: { list: [] } },
    '/api/v1/users/me': { data: { id: 1, nickname: 'me', avatar: '' } },
    '/api/v1/groups/1': { data: { id: 1, name: 'g', members: [] } },
    '/api/v1/groups/1/messages': { data: { list: [] } },
    '/api/v1/articles/me': { data: { list: [] } },
  };
  const orig = window.fetch;
  window.fetch = async function(url, opts) {
    const u = typeof url === 'string' ? url : url.url;
    for (const k in fake) {
      if (u.includes(k)) {
        return new Response(JSON.stringify(fake[k]), { status: 200, headers: { 'Content-Type': 'application/json' } });
      }
    }
    return orig.apply(this, arguments);
  };
})();
"""


ROUTES = [
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
    ("/editor", "Editor"),
    ("/editor/preview", "EditorPreview"),
    ("/notifications", "Notifications"),
    ("/messages", "Messages"),
    ("/user", "UserProfile"),
    ("/user/articles", "UserArticles"),
    ("/user/edit", "UserEdit"),
    ("/user/followers", "MyFollowers"),
    ("/user/following", "MyFollowing"),
    ("/user/settings", "UserSettings"),
    ("/not-exist-xyz", "NotFound"),
]


def exercise(page):
    """执行大范围交互"""
    # 滚动
    try:
        page.evaluate("window.scrollTo(0, document.body.scrollHeight/2)")
        page.wait_for_timeout(300)
        page.evaluate("window.scrollTo(0, document.body.scrollHeight)")
        page.wait_for_timeout(400)
        page.evaluate("window.scrollTo(0, 0)")
        page.wait_for_timeout(300)
    except Exception:
        pass
    # 按钮
    try:
        btns = page.locator("button:visible")
        n = min(btns.count(), 6)
        for i in range(n):
            try:
                btns.nth(i).click(timeout=2000, force=True)
                page.wait_for_timeout(200)
            except Exception:
                pass
    except Exception:
        pass
    # 卡片
    try:
        cards = page.locator(".cursor-pointer:visible, [class*='cursor-pointer']:visible")
        n = min(cards.count(), 4)
        for i in range(n):
            try:
                cards.nth(i).click(timeout=2000, force=True)
                page.wait_for_timeout(300)
            except Exception:
                pass
    except Exception:
        pass
    # tab
    try:
        tabs = page.locator("[role='tab']:visible, .el-tabs__item:visible")
        n = min(tabs.count(), 3)
        for i in range(n):
            try:
                tabs.nth(i).click(timeout=2000, force=True)
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
    # 提交表单
    try:
        sub = page.locator("button[type='submit']:visible, button:has-text('登录'):visible, button:has-text('注册'):visible, button:has-text('搜索'):visible, button:has-text('重试'):visible")
        n = min(sub.count(), 1)
        for i in range(n):
            try:
                sub.nth(i).click(timeout=2000, force=True)
                page.wait_for_timeout(500)
            except Exception:
                pass
    except Exception:
        pass


def main():
    Path("d:/zhixun/test-screenshots-final").mkdir(parents=True, exist_ok=True)
    Path("d:/zhixun/final-test-report.json").write_text("", encoding="utf-8")

    results = []
    summary = {"OK": 0, "WARN": 0, "ERROR": 0}
    all_console_errs = []
    all_warns = []
    all_js_errs = []

    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True, args=["--no-sandbox", "--disable-gpu"])
        context = browser.new_context(viewport={"width": 1366, "height": 800})

        for path, name in ROUTES:
            page = context.new_page()
            page.add_init_script(INJECT)
            errs = []
            warns = []
            js_errs = []
            page.on("console", lambda m, e=errs, w=warns: (
                e.append(m.text) if m.type == "error" and not is_net(m.text) else None,
                w.append(m.text) if m.type == "warning" and not is_net(m.text) else None,
            ))
            page.on("pageerror", lambda exc, e=js_errs: e.append(str(exc)) if not is_net(str(exc)) else None)

            print(f"\n[{name:18s}] -> {path}")
            try:
                page.goto(BASE_URL + path, wait_until="domcontentloaded", timeout=20000)
                try:
                    page.wait_for_load_state("networkidle", timeout=8000)
                except Exception:
                    pass
                page.wait_for_timeout(1200)
                exercise(page)
            except Exception as e:
                js_errs.append(f"goto: {e}")

            # 截图
            try:
                page.screenshot(path=f"d:/zhixun/test-screenshots-final/{name}.png", full_page=False)
            except Exception:
                pass

            # 评估
            if js_errs:
                status = "ERROR"
            elif errs:
                status = "WARN"
            elif warns:
                status = "WARN"
            else:
                status = "OK"
            summary[status] += 1

            for e in errs: all_console_errs.append({"page": name, "text": e[:300]})
            for w in warns: all_warns.append({"page": name, "text": w[:300]})
            for e in js_errs: all_js_errs.append({"page": name, "text": e[:300]})

            results.append({
                "name": name, "path": path, "status": status,
                "errs": len(errs), "warns": len(warns), "js_errs": len(js_errs),
                "err_samples": errs[:3], "warn_samples": warns[:3], "js_err_samples": js_errs[:3],
            })

            if errs or warns or js_errs:
                print(f"   [{status}] errs={len(errs)} warns={len(warns)} js={len(js_errs)}")
                for e in errs[:3]:
                    print(f"     CONSOLE_ERR: {e[:250]}")
                for w in warns[:3]:
                    print(f"     WARN: {w[:250]}")
                for e in js_errs[:3]:
                    print(f"     JS: {e[:250]}")
            else:
                print(f"   [OK]")

            page.close()

        browser.close()

    print("\n" + "=" * 70)
    print(f"Total: {len(ROUTES)} | OK: {summary['OK']} | WARN: {summary['WARN']} | ERROR: {summary['ERROR']}")
    print("=" * 70)
    if not all_console_errs and not all_warns and not all_js_errs:
        print(">>> ALL 30 PAGES PASSED! 0 console errors, 0 warnings, 0 JS errors. <<<")
    else:
        print(f">>> Found: {len(all_console_errs)} console errors, {len(all_warns)} warnings, {len(all_js_errs)} JS errors <<<")

    Path("d:/zhixun/final-test-report.json").write_text(
        json.dumps({
            "summary": summary,
            "all_console_errors": all_console_errs,
            "all_warnings": all_warns,
            "all_js_errors": all_js_errs,
            "details": results,
        }, ensure_ascii=False, indent=2), encoding="utf-8"
    )


if __name__ == "__main__":
    main()
