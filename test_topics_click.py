"""
专项测试：点击话题广场中的话题卡片，验证 navigateTo bug
"""
import sys
from pathlib import Path
from playwright.sync_api import sync_playwright

BASE_URL = "http://localhost:3500"

# 必须保留的 JS 错误（不计）
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


def main():
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True, args=["--no-sandbox", "--disable-gpu"])
        context = browser.new_context(viewport={"width": 1366, "height": 800})
        page = context.new_page()
        errors = []
        warns = []
        page.on("console", lambda m: (
            errors.append(m.text) if m.type == "error" and not is_net(m.text) else None,
            warns.append(m.text) if m.type == "warning" and not is_net(m.text) else None,
        ))
        page.on("pageerror", lambda e: errors.append(f"pageerror: {e}") if not is_net(str(e)) else None)

        page.goto(BASE_URL + "/topics", wait_until="domcontentloaded", timeout=20000)
        try:
            page.wait_for_load_state("networkidle", timeout=10000)
        except Exception:
            pass
        page.wait_for_timeout(1500)

        # 检查 navigateTo 函数是否在全局
        nav_exists = page.evaluate("typeof window.navigateTo")
        has_globals = page.evaluate("Object.keys(window).filter(k => k.toLowerCase().includes('nav')).join(',')")
        print(f"window.navigateTo type: {nav_exists}")
        print(f"window keys with 'nav': {has_globals}")

        # 如果 topics 列表为空，注入测试数据让列表非空
        # 我们直接测试 handleClick 函数
        # 在 Vue setup 上下文中 navigateTo 是没定义的

        # 模拟点击（但需有话题才能点）
        # 我们直接评估 click handler - 找所有 div[role=button] 或 div class 包含 cursor-pointer
        # 这里更稳妥的是用 evaluate 主动调用
        try:
            res = page.evaluate("""() => {
                // 尝试找一个 div 触发 click，看是否抛错
                const cards = document.querySelectorAll('div.cursor-pointer, [class*="cursor-pointer"]');
                if (cards.length > 0) {
                    cards[0].click();
                    return 'clicked, count=' + cards.length;
                }
                return 'no cards, count=0';
            }""")
            print(f"Click test: {res}")
            page.wait_for_timeout(1500)
        except Exception as e:
            print(f"click test exception: {e}")

        print(f"\nErrors: {len(errors)}")
        for e in errors:
            print(f"  - {e[:300]}")
        print(f"\nWarns: {len(warns)}")
        for w in warns:
            print(f"  - {w[:300]}")

        browser.close()


if __name__ == "__main__":
    main()
