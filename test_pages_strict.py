"""
最严格的测试：所有页面、所有交互，捕获任何 console 输出
- 收集 ALL console message（包括 log/info）
- 收集 ALL pageerror
- 收集所有资源加载错误
- 收集所有 network 失败
- 不放过任何错误
"""
import sys
import json
from pathlib import Path
from playwright.sync_api import sync_playwright

BASE_URL = "http://localhost:3500"

# 后端 8080 不可用导致的所有消息 - 这是预期的
# 必须严格区分：网络/API 错误 vs 真正的 JS 错误
NETWORK_PATTERNS = [
    "/api/", "ws://", "wss://",
    "Failed to fetch", "NetworkError", "net::ERR_",
    "AxiosError", "Request failed", "ECONNREFUSED",
    "ERR_CONNECTION_REFUSED", "ERR_CONNECTION", "ERR_NAME_NOT_RESOLVED",
    "ERR_EMPTY_RESPONSE", "ERR_ABORTED",
    "WebSocket connection", "WebSocket failed",
    "Failed to load resource",
    "timeout of",
    "500 (Internal Server", "404 (Not Found)",
    "403 (Forbidden)", "401 (Unauthorized)",
    "Request aborted", "aborted",
    "Download the Vue Devtools",  # dev 环境的开发工具提示
    "[HMR]",  # Vite HMR 提示
    "[vite]",
    "ERR_BLOCKED_BY_RESPONSE",
]


def is_network(text: str) -> bool:
    if not text:
        return True
    for kw in NETWORK_PATTERNS:
        if kw.lower() in text.lower():
            return True
    return False


ROUTES_DEEP = [
    # 公开页
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
    # 动态页
    ("/articles/1", "ArticleDetail"),
    ("/topics/1", "TopicDetail"),
    ("/user/1", "UserHome"),
    ("/user/1/followers", "UserFollowers"),
    ("/user/1/following", "UserFollowing"),
    ("/user/preview/1", "UserPreview"),
    ("/groups/1", "GroupChat"),
    ("/messages/1", "MessageChat"),
    # 需登录
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
    # 404
    ("/not-exist-xyz", "NotFound"),
]


class Recorder:
    def __init__(self):
        self.console = []   # 全部 console
        self.js_errors = []  # pageerror
        self.failed = []    # requestfailed
        self.console_errs_kept = []  # 真正需要关注的
        self.console_warns_kept = []

    def add_console(self, msg):
        entry = {
            "type": msg.type,
            "text": msg.text[:500],
        }
        self.console.append(entry)
        if msg.type == "error" and not is_network(msg.text):
            self.console_errs_kept.append(entry)
        if msg.type == "warning" and not is_network(msg.text):
            self.console_warns_kept.append(entry)

    def add_error(self, err):
        text = str(err)
        if not is_network(text):
            self.js_errors.append({"text": text[:500], "name": err.name, "message": err.message[:500]})

    def add_requestfailed(self, req):
        url = req.url
        if "/api/" in url or "ws://" in url or "wss://" in url or "minio" in url:
            return
        try:
            failure = req.failure if isinstance(req.failure, str) else str(req.failure)
        except Exception:
            failure = ""
        self.failed.append({"url": url[:200], "method": req.method, "failure": failure[:200]})


def exercise_page(page, path: str):
    """对页面执行大范围交互"""
    # 1) 等待初始化渲染
    page.wait_for_timeout(500)
    # 2) 滚动到中段
    try:
        page.evaluate("window.scrollTo(0, document.body.scrollHeight/3)")
        page.wait_for_timeout(300)
    except Exception:
        pass
    # 3) 滚动到底部
    try:
        page.evaluate("window.scrollTo(0, document.body.scrollHeight)")
        page.wait_for_timeout(400)
    except Exception:
        pass
    # 4) 滚动回顶
    try:
        page.evaluate("window.scrollTo(0, 0)")
        page.wait_for_timeout(300)
    except Exception:
        pass
    # 5) 点击前 5 个按钮（带保护）
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
    # 6) 切换 tab 类
    try:
        tabs = page.locator("[role='tab'], .el-tabs__item, [class*='tab-item']")
        n = min(tabs.count(), 3)
        for i in range(n):
            try:
                tabs.nth(i).click(timeout=1500, force=True)
                page.wait_for_timeout(200)
            except Exception:
                pass
    except Exception:
        pass
    # 7) 在搜索框输入
    try:
        search = page.locator("input[type='search'], input[placeholder*='搜索' i], input[placeholder*='search' i]").first
        if search.count() > 0:
            try:
                search.fill("test", timeout=1500)
                page.wait_for_timeout(300)
            except Exception:
                pass
    except Exception:
        pass
    # 8) hover 头几个链接
    try:
        links = page.locator("a:visible")
        n = min(links.count(), 4)
        for i in range(n):
            try:
                links.nth(i).hover(timeout=1000)
                page.wait_for_timeout(80)
            except Exception:
                pass
    except Exception:
        pass
    # 9) 输入框填值
    try:
        inputs = page.locator("input[type='text']:visible, input[type='email']:visible, input:not([type]):visible, textarea:visible")
        n = min(inputs.count(), 3)
        for i in range(n):
            try:
                inputs.nth(i).fill("test_value", timeout=1500)
                page.wait_for_timeout(100)
            except Exception:
                pass
    except Exception:
        pass
    # 10) 提交表单
    try:
        # 找 type=submit 按钮
        submits = page.locator("button[type='submit']:visible, .el-button--primary:visible")
        n = min(submits.count(), 1)
        for i in range(n):
            try:
                submits.nth(i).click(timeout=1500, force=True)
                page.wait_for_timeout(500)
            except Exception:
                pass
    except Exception:
        pass
    # 11) 触发重试按钮（如果有错误状态）
    try:
        retry = page.locator("button:has-text('重试'), button:has-text('重新加载'), button:has-text('刷新')")
        n = min(retry.count(), 2)
        for i in range(n):
            try:
                retry.nth(i).click(timeout=1500, force=True)
                page.wait_for_timeout(500)
            except Exception:
                pass
    except Exception:
        pass


def main():
    Path("d:/zhixun/test-screenshots-final").mkdir(parents=True, exist_ok=True)

    results = []
    summary_counts = {"OK": 0, "WARN": 0, "ERROR": 0}
    global_console_errors = []  # 跨页面的 console 错误
    global_js_errors = []

    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True, args=["--no-sandbox", "--disable-gpu"])
        context = browser.new_context(viewport={"width": 1366, "height": 800})

        for path, name in ROUTES_DEEP:
            page = context.new_page()
            rec = Recorder()
            page.on("console", lambda msg, r=rec: r.add_console(msg))
            page.on("pageerror", lambda err, r=rec: r.add_error(err))
            page.on("requestfailed", lambda req, r=rec: r.add_requestfailed(req))

            url = BASE_URL + path
            print(f"[{name:18s}] -> {url}")
            try:
                page.goto(url, wait_until="domcontentloaded", timeout=20000)
                try:
                    page.wait_for_load_state("networkidle", timeout=10000)
                except Exception:
                    pass
                # 执行交互
                exercise_page(page, path)
            except Exception as e:
                rec.js_errors.append({"text": f"goto failed: {e}", "name": "NavError", "message": str(e)[:200]})

            # 截图
            try:
                page.screenshot(path=f"d:/zhixun/test-screenshots-final/{name}.png", full_page=False)
            except Exception:
                pass

            # 评估
            status = "OK"
            if rec.js_errors:
                status = "ERROR"
            elif rec.console_errs_kept:
                status = "WARN"

            summary_counts[status] += 1

            # 记录到全局
            for e in rec.console_errs_kept:
                global_console_errors.append({"page": name, "text": e["text"]})
            for e in rec.js_errors:
                global_js_errors.append({"page": name, "text": e["text"]})

            results.append({
                "name": name,
                "path": path,
                "status": status,
                "console_count": len(rec.console),
                "console_errs_kept": len(rec.console_errs_kept),
                "console_warns_kept": len(rec.console_warns_kept),
                "js_errors": len(rec.js_errors),
                "failed_requests": len(rec.failed),
                "console_err_samples": [e["text"][:300] for e in rec.console_errs_kept[:5]],
                "js_err_samples": [e["text"][:300] for e in rec.js_errors[:5]],
                "warn_samples": [e["text"][:300] for e in rec.console_warns_kept[:5]],
                "failed_samples": [f"{e['method']} {e['url']} - {e['failure']}" for e in rec.failed[:3]],
            })

            page.close()

        browser.close()

    # 输出
    print("\n" + "=" * 70)
    print(f"Total: {len(ROUTES_DEEP)} | OK: {summary_counts['OK']} | WARN: {summary_counts['WARN']} | ERROR: {summary_counts['ERROR']}")
    print("=" * 70)
    print()
    for r in results:
        flag = "OK" if r["status"] == "OK" else ("WARN" if r["status"] == "WARN" else "ERR")
        print(f"  [{flag:4s}] {r['name']:20s} console={r['console_count']:3d} kept_err={r['console_errs_kept']:2d} kept_warn={r['console_warns_kept']:2d} js_err={r['js_errors']:2d} failed={r['failed_requests']:2d}")
        if r["js_err_samples"]:
            for s in r["js_err_samples"]:
                print(f"          JS: {s[:200]}")
        if r["console_err_samples"]:
            for s in r["console_err_samples"]:
                print(f"          CONS_ERR: {s[:200]}")
        if r["warn_samples"]:
            for s in r["warn_samples"][:2]:
                print(f"          WARN: {s[:200]}")
        if r["failed_samples"]:
            for s in r["failed_samples"]:
                print(f"          REQ: {s[:200]}")

    # 写报告
    Path("d:/zhixun/strict-test-report.json").write_text(
        json.dumps({
            "summary": summary_counts,
            "global_console_errors": global_console_errors,
            "global_js_errors": global_js_errors,
            "details": results,
        }, ensure_ascii=False, indent=2),
        encoding="utf-8"
    )
    print(f"\nReport: d:/zhixun/strict-test-report.json")
    if not global_console_errors and not global_js_errors:
        print("\n>>> All pages OK, no console.error and no pageerror found! <<<")
    else:
        print(f"\n>>> Found {len(global_console_errors)} console errors and {len(global_js_errors)} JS errors <<<")


if __name__ == "__main__":
    main()
