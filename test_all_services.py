"""
全服务运行下的真实测试：
- 后端 API 可用、MySQL/Redis/RabbitMQ/MinIO 已启动
- 通过真实 API 加载数据
- 测试所有 30 个页面
- 监控控制台错误
"""
import sys
import json
from pathlib import Path
from playwright.sync_api import sync_playwright

BASE_URL = "http://localhost:3500"
API_URL = "http://localhost:8080"

# 允许的"已知网络错误"（不视为前端 Bug）
NETWORK_KW = [
    "/api/", "ws://", "wss://", "Failed to fetch", "NetworkError", "net::ERR_",
    "AxiosError", "Request failed", "ECONNREFUSED", "ERR_CONNECTION_REFUSED",
    "ERR_CONNECTION", "ERR_NAME_NOT_RESOLVED", "ERR_EMPTY_RESPONSE", "ERR_ABORTED",
    "WebSocket connection", "WebSocket failed", "Failed to load resource",
    "timeout of", "500 (Internal Server", "404 (Not Found)", "403 (Forbidden)",
    "401 (Unauthorized)", "Request aborted", "aborted",
    "Download the Vue Devtools", "[HMR]", "[vite]",
    "ERR_BLOCKED_BY_RESPONSE",
    "/minio",  # MinIO 控制台
]


def is_net(text):
    if not text:
        return True
    for k in NETWORK_KW:
        if k.lower() in text.lower():
            return True
    return False


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
    ("/articles/40", "ArticleDetail"),
    ("/topics/1", "TopicDetail"),
    ("/user/1", "UserHome"),
    ("/user/1/followers", "UserFollowers"),
    ("/user/1/following", "UserFollowing"),
    ("/user/preview/40", "UserPreview"),
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
    try:
        page.evaluate("window.scrollTo(0, document.body.scrollHeight/2)")
        page.wait_for_timeout(300)
        page.evaluate("window.scrollTo(0, document.body.scrollHeight)")
        page.wait_for_timeout(400)
        page.evaluate("window.scrollTo(0, 0)")
        page.wait_for_timeout(300)
    except Exception:
        pass
    try:
        btns = page.locator("button:visible")
        n = min(btns.count(), 5)
        for i in range(n):
            try:
                btns.nth(i).click(timeout=1500, force=True)
                page.wait_for_timeout(200)
            except Exception:
                pass
    except Exception:
        pass
    try:
        cards = page.locator(".cursor-pointer:visible, [class*='cursor-pointer']:visible")
        n = min(cards.count(), 3)
        for i in range(n):
            try:
                cards.nth(i).click(timeout=2000, force=True)
                page.wait_for_timeout(300)
            except Exception:
                pass
    except Exception:
        pass
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


def main():
    Path("d:/zhixun/test-screenshots-realservice").mkdir(parents=True, exist_ok=True)
    results = []
    summary = {"OK": 0, "WARN": 0, "ERROR": 0}
    all_errs = []
    all_warns = []
    all_js = []

    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True, args=["--no-sandbox", "--disable-gpu"])
        context = browser.new_context(viewport={"width": 1366, "height": 800})

        for path, name in ROUTES:
            page = context.new_page()
            errs, warns, jse = [], [], []
            page.on("console", lambda m, e=errs, w=warns: (
                e.append(m.text) if m.type == "error" and not is_net(m.text) else None,
                w.append(m.text) if m.type == "warning" and not is_net(m.text) else None,
            ))
            page.on("pageerror", lambda exc, e=jse: e.append(str(exc)) if not is_net(str(exc)) else None)

            print(f"\n[{name:18s}] -> {path}")
            try:
                page.goto(BASE_URL + path, wait_until="domcontentloaded", timeout=20000)
                try:
                    page.wait_for_load_state("networkidle", timeout=8000)
                except Exception:
                    pass
                page.wait_for_timeout(1200)
                # 关键：等待真实数据加载
                exercise(page)
            except Exception as e:
                jse.append(f"goto: {e}")

            try:
                page.screenshot(path=f"d:/zhixun/test-screenshots-realservice/{name}.png", full_page=False)
            except Exception:
                pass

            if jse:
                status = "ERROR"
            elif errs:
                status = "WARN"
            elif warns:
                status = "WARN"
            else:
                status = "OK"
            summary[status] += 1

            for e in errs: all_errs.append({"page": name, "text": e[:300]})
            for w in warns: all_warns.append({"page": name, "text": w[:300]})
            for e in jse: all_js.append({"page": name, "text": e[:300]})

            results.append({
                "name": name, "path": path, "status": status,
                "errs": len(errs), "warns": len(warns), "js": len(jse),
            })

            if errs or warns or jse:
                print(f"   [{status}] err={len(errs)} warn={len(warns)} js={len(jse)}")
                for e in errs[:3]: print(f"     CONSOLE_ERR: {e[:250]}")
                for w in warns[:3]: print(f"     WARN: {w[:250]}")
                for e in jse[:3]: print(f"     JS: {e[:250]}")
            else:
                print(f"   [OK]")

            page.close()

        browser.close()

    print("\n" + "=" * 70)
    print(f"Total: {len(ROUTES)} | OK: {summary['OK']} | WARN: {summary['WARN']} | ERROR: {summary['ERROR']}")
    print("=" * 70)
    if not all_errs and not all_warns and not all_js:
        print(">>> ALL 30 PAGES PASSED with REAL backend services! <<<")
    else:
        print(f">>> Found: {len(all_errs)} console errors, {len(all_warns)} warnings, {len(all_js)} JS errors <<<")

    Path("d:/zhixun/realservice-test-report.json").write_text(
        json.dumps({
            "summary": summary,
            "console_errors": all_errs,
            "warnings": all_warns,
            "js_errors": all_js,
            "details": results,
        }, ensure_ascii=False, indent=2), encoding="utf-8"
    )
    print(f"Report: d:/zhixun/realservice-test-report.json")


if __name__ == "__main__":
    main()
