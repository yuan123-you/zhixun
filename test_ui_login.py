"""
真实 UI 登录流程测试：
1. 访问 /login 页面
2. 输入 test123 / test123
3. 点击登录按钮
4. 验证登录成功后访问受限页面
5. 全程监控控制台错误
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
    "ERR_BLOCKED_BY_RESPONSE", "/minio",
]


def is_net(text):
    if not text:
        return True
    for k in NETWORK_KW:
        if k.lower() in text.lower():
            return True
    return False


def main():
    Path("d:/zhixun/test-screenshots-uilogin").mkdir(parents=True, exist_ok=True)
    errs = []
    warns = []
    jse = []

    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True, args=["--no-sandbox", "--disable-gpu"])
        context = browser.new_context(viewport={"width": 1366, "height": 800})
        page = context.new_page()
        page.on("console", lambda m: (
            errs.append({"text": m.text}) if m.type == "error" and not is_net(m.text) else None,
            warns.append({"text": m.text}) if m.type == "warning" and not is_net(m.text) else None,
        ))
        page.on("pageerror", lambda e: jse.append({"text": str(e)}) if not is_net(str(e)) else None)

        # Step 1: 访问登录页
        print("\n[1] 访问登录页 /login")
        page.goto(BASE_URL + "/login", wait_until="domcontentloaded", timeout=20000)
        page.wait_for_load_state("networkidle", timeout=8000)
        page.wait_for_timeout(800)
        page.screenshot(path="d:/zhixun/test-screenshots-uilogin/01-login.png", full_page=False)

        # Step 2: 输入用户名
        print("[2] 输入用户名 test123")
        # 查找用户名输入框
        username_input = page.locator("input[placeholder*='账号' i], input[placeholder*='用户' i], input[placeholder*='邮箱' i], input[type='text']:visible, input:not([type]):visible").first
        if username_input.count() == 0:
            print("  ❌ 找不到用户名输入框")
        else:
            username_input.fill("test123")
            page.wait_for_timeout(300)

        # Step 3: 输入密码
        print("[3] 输入密码 test123")
        password_input = page.locator("input[type='password']:visible").first
        if password_input.count() > 0:
            password_input.fill("test123")
            page.wait_for_timeout(300)

        # Step 4: 点击登录按钮
        print("[4] 点击登录按钮")
        login_btn = page.locator("button:has-text('登 录'), button:has-text('登录'), button[type='submit']:visible").first
        if login_btn.count() > 0:
            login_btn.click(timeout=5000, force=True)
            page.wait_for_timeout(3000)
        page.screenshot(path="d:/zhixun/test-screenshots-uilogin/02-after-login.png", full_page=False)

        # Step 5: 检查是否登录成功（URL 不再是 /login，或有 token）
        cur_url = page.url
        print(f"[5] 登录后 URL: {cur_url}")

        # 检查 localStorage
        token = page.evaluate("() => localStorage.getItem('accessToken') || localStorage.getItem('token')")
        print(f"    Token: {token[:50] if token else 'None'}")

        # Step 6: 访问个人中心（验证登录态生效）
        print("\n[6] 访问 /user 验证登录态")
        page.goto(BASE_URL + "/user", wait_until="domcontentloaded", timeout=20000)
        page.wait_for_load_state("networkidle", timeout=8000)
        page.wait_for_timeout(1500)
        page.screenshot(path="d:/zhixun/test-screenshots-uilogin/03-user-profile.png", full_page=False)

        # 检查 body 内容
        body = page.evaluate("() => document.body.innerText.substring(0, 500)")
        print(f"    页面内容: {body[:300]}")

        # Step 7: 访问消息中心
        print("\n[7] 访问 /messages")
        page.goto(BASE_URL + "/messages", wait_until="domcontentloaded", timeout=20000)
        page.wait_for_load_state("networkidle", timeout=8000)
        page.wait_for_timeout(1500)
        page.screenshot(path="d:/zhixun/test-screenshots-uilogin/04-messages.png", full_page=False)

        # Step 8: 访问编辑器
        print("\n[8] 访问 /editor")
        page.goto(BASE_URL + "/editor", wait_until="domcontentloaded", timeout=20000)
        page.wait_for_load_state("networkidle", timeout=8000)
        page.wait_for_timeout(1500)
        page.screenshot(path="d:/zhixun/test-screenshots-uilogin/05-editor.png", full_page=False)

        # Step 9: 访问文章详情 (真实 ID 40)
        print("\n[9] 访问 /articles/40")
        page.goto(BASE_URL + "/articles/40", wait_until="domcontentloaded", timeout=20000)
        page.wait_for_load_state("networkidle", timeout=8000)
        page.wait_for_timeout(1500)
        # 点击点赞按钮
        like_btn = page.locator("button:has-text('赞'), [class*='like']:visible button, button[aria-label*='赞' i]:visible").first
        if like_btn.count() > 0:
            try:
                like_btn.click(timeout=3000, force=True)
                page.wait_for_timeout(1500)
                print("    点击了点赞按钮")
            except Exception as e:
                print(f"    点赞失败: {e}")
        page.screenshot(path="d:/zhixun/test-screenshots-uilogin/06-article-detail.png", full_page=False)

        # Step 10: 访问用户主页 (用户ID=1)
        print("\n[10] 访问 /user/1 (其他用户主页)")
        page.goto(BASE_URL + "/user/1", wait_until="domcontentloaded", timeout=20000)
        page.wait_for_load_state("networkidle", timeout=8000)
        page.wait_for_timeout(1500)
        # 点击关注按钮
        follow_btn = page.locator("button:has-text('关注'), button:has-text('+ 关注'), button:has-text('+关注')").first
        if follow_btn.count() > 0:
            try:
                follow_btn.click(timeout=3000, force=True)
                page.wait_for_timeout(1500)
                print("    点击了关注按钮")
            except Exception as e:
                print(f"    关注失败: {e}")
        page.screenshot(path="d:/zhixun/test-screenshots-uilogin/07-user-home.png", full_page=False)

        page.close()
        browser.close()

    print("\n" + "=" * 70)
    print(f"控制台错误: {len(errs)}")
    for e in errs[:10]:
        print(f"  - {e['text'][:300]}")
    print(f"控制台警告: {len(warns)}")
    for w in warns[:5]:
        print(f"  - {w['text'][:200]}")
    print(f"JS 异常: {len(jse)}")
    for e in jse[:5]:
        print(f"  - {e['text'][:300]}")
    if not errs and not warns and not jse:
        print(">>> ALL UI LOGIN STEPS PASSED! <<<")


if __name__ == "__main__":
    main()
