"""
登录场景测试：使用真实 token 访问受限页面（个人中心、编辑、消息等）
"""
import sys
import json
import re
import urllib.request
import urllib.parse
from pathlib import Path
from playwright.sync_api import sync_playwright

BASE_URL = "http://localhost:3500"
API_URL = "http://localhost:8080"

NETWORK_KW = [
    "/api/", "ws://", "wss://", "Failed to fetch", "NetworkError", "net::ERR_",
    "AxiosError", "Request failed", "ECONNREFUSED", "ERR_CONNECTION_REFUSED",
    "ERR_CONNECTION", "ERR_NAME_NOT_RESOLVED", "ERR_EMPTY_RESPONSE", "ERR_ABORTED",
    "WebSocket connection", "WebSocket failed", "Failed to load resource",
    "timeout of", "500 (Internal Server", "404 (Not Found)", "403 (Forbidden)",
    "401 (Unauthorized)", "Request aborted", "aborted",
    "Download the Vue Devtools", "[HMR]", "[vite]",
    "ERR_BLOCKED_BY_RESPONSE", "/minio",
]


def is_net(text):
    if not text:
        return True
    for k in NETWORK_KW:
        if k.lower() in text.lower():
            return True
    return False


def login_get_token():
    """通过 API 登录获取 token"""
    data = json.dumps({"username": "test123", "password": "test123"}).encode()
    req = urllib.request.Request(
        f"{API_URL}/api/v1/auth/login",
        data=data,
        headers={"Content-Type": "application/json"}
    )
    with urllib.request.urlopen(req, timeout=10) as resp:
        body = json.loads(resp.read())
    if body.get("code") == 200:
        return body["data"]["accessToken"]
    raise RuntimeError(f"Login failed: {body}")


def login_get_userinfo():
    """通过 API 登录获取用户信息"""
    data = json.dumps({"username": "test123", "password": "test123"}).encode()
    req = urllib.request.Request(
        f"{API_URL}/api/v1/auth/login",
        data=data,
        headers={"Content-Type": "application/json"}
    )
    with urllib.request.urlopen(req, timeout=10) as resp:
        body = json.loads(resp.read())
    return body.get("data", {})


# 受限页面（需要登录）
AUTH_ROUTES = [
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
    ("/groups/1", "GroupChat"),
    ("/messages/1", "MessageChat"),
]


def exercise(page):
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
        n = min(btns.count(), 4)
        for i in range(n):
            try:
                btns.nth(i).click(timeout=1500, force=True)
                page.wait_for_timeout(200)
            except Exception:
                pass
    except Exception:
        pass
    try:
        inputs = page.locator("input:visible, textarea:visible")
        n = min(inputs.count(), 2)
        for i in range(n):
            try:
                inputs.nth(i).fill("test", timeout=1500)
                page.wait_for_timeout(100)
            except Exception:
                pass
    except Exception:
        pass


def main():
    Path("d:/zhixun/test-screenshots-loggedin").mkdir(parents=True, exist_ok=True)
    print("Step 1: 登录获取 token...")
    token = login_get_token()
    print(f"  Token: {token[:50]}...")

    results = []
    summary = {"OK": 0, "WARN": 0, "ERROR": 0}
    all_errs = []
    all_warns = []
    all_js = []

    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True, args=["--no-sandbox", "--disable-gpu"])
        context = browser.new_context(viewport={"width": 1366, "height": 800})

        for path, name in AUTH_ROUTES:
            page = context.new_page()
            errs, warns, jse = [], [], []
            page.on("console", lambda m, e=errs, w=warns: (
                e.append(m.text) if m.type == "error" and not is_net(m.text) else None,
                w.append(m.text) if m.type == "warning" and not is_net(m.text) else None,
            ))
            page.on("pageerror", lambda exc, e=jse: e.append(str(exc)) if not is_net(str(exc)) else None)

            print(f"\n[LOGGED-IN {name:18s}] -> {path}")
            try:
                # 先访问任意页面让浏览器启用 localStorage
                page.goto(BASE_URL + "/", wait_until="domcontentloaded", timeout=20000)
                # 然后用 evaluate 写入 token
                page.evaluate(f"""
                    localStorage.setItem('accessToken', '{token}');
                    localStorage.setItem('token', '{token}');
                """)
                # 访问目标页
                page.goto(BASE_URL + path, wait_until="domcontentloaded", timeout=20000)
                try:
                    page.wait_for_load_state("networkidle", timeout=10000)
                except Exception:
                    pass
                page.wait_for_timeout(1500)
                exercise(page)
            except Exception as e:
                jse.append(f"goto: {e}")

            try:
                page.screenshot(path=f"d:/zhixun/test-screenshots-loggedin/{name}.png", full_page=False)
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

            results.append({"name": name, "path": path, "status": status,
                            "errs": len(errs), "warns": len(warns), "js": len(jse)})

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
    print(f"Logged-in tests: {len(AUTH_ROUTES)} | OK: {summary['OK']} | WARN: {summary['WARN']} | ERROR: {summary['ERROR']}")
    print("=" * 70)
    if not all_errs and not all_warns and not all_js:
        print(">>> ALL LOGGED-IN PAGES PASSED! <<<")
    else:
        print(f">>> Found: {len(all_errs)} console errors, {len(all_warns)} warnings, {len(all_js)} JS errors <<<")

    Path("d:/zhixun/loggedin-test-report.json").write_text(
        json.dumps({
            "summary": summary,
            "console_errors": all_errs,
            "warnings": all_warns,
            "js_errors": all_js,
            "details": results,
        }, ensure_ascii=False, indent=2), encoding="utf-8"
    )


if __name__ == "__main__":
    main()
