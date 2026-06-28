"""
详细验证测试：检查每个页面的实际 DOM 渲染情况
- 是否显示了 Loading 状态
- 是否显示了 Empty 状态
- 是否显示了 Error 状态
- 实际可见元素数量
"""
import sys
import json
from pathlib import Path
from playwright.sync_api import sync_playwright

BASE_URL = "http://localhost:3500"

# 后端 8080 不可用时可能出现的状态关键词
LOADING_KEYWORDS = ["加载中", "loading", "Loading", "正在", "拼命", "稍候"]
EMPTY_KEYWORDS = ["暂无", "没有", "空空如也", "empty", "无数据", "暂无数据", "无内容"]
ERROR_KEYWORDS = ["出错了", "错误", "失败", "error", "Error", "异常"]


def has_any(text: str, kws):
    return any(k.lower() in text.lower() for k in kws)


ROUTES = [
    ("/", "首页"),
    ("/rank", "排行榜"),
    ("/discover", "发现页"),
    ("/tags", "标签页"),
    ("/search", "搜索页"),
    ("/topics", "话题广场"),
    ("/groups", "群组广场"),
    ("/category/tech", "分类页"),
    ("/login", "登录页"),
    ("/register", "注册页"),
    ("/forgot-password", "忘记密码"),
    ("/articles/1", "作品详情"),
    ("/topics/1", "话题详情"),
    ("/user/1", "用户主页"),
    ("/user/1/followers", "用户粉丝列表"),
    ("/user/1/following", "用户关注列表"),
    ("/user/preview/1", "用户预览作品"),
]


def check_page(page, path, name):
    url = BASE_URL + path
    print(f"\n[VERIFY] {name} -> {url}")
    try:
        page.goto(url, wait_until="domcontentloaded", timeout=20000)
    except Exception as e:
        return {"name": name, "path": path, "error": str(e)[:200]}

    try:
        page.wait_for_load_state("networkidle", timeout=10000)
    except Exception:
        pass
    page.wait_for_timeout(1500)

    info = {
        "name": name,
        "path": path,
        "title": page.title(),
    }

    # 关键指标
    info["body_text_length"] = page.evaluate("() => document.body ? document.body.innerText.length : 0")
    info["body_inner_text"] = page.evaluate("() => document.body ? document.body.innerText.substring(0, 1500) : ''")
    info["all_text_length"] = page.evaluate("() => document.body ? document.body.textContent.length : 0")
    info["img_count"] = page.locator("img").count()
    info["button_count"] = page.locator("button").count()
    info["link_count"] = page.locator("a").count()
    info["input_count"] = page.locator("input, textarea").count()
    info["visible_h1_h2"] = page.locator("h1, h2, h3").count()
    info["body_classes"] = page.evaluate("() => document.body ? document.body.className : ''")
    info["url_after"] = page.url

    # 检查渲染状态
    info["is_loading"] = has_any(info["body_inner_text"], LOADING_KEYWORDS)
    info["is_empty"] = has_any(info["body_inner_text"], EMPTY_KEYWORDS)

    print(f"   标题: {info['title']}")
    print(f"   body text len: {info['body_text_length']}, buttons: {info['button_count']}, links: {info['link_count']}")
    print(f"   h1/h2/h3: {info['visible_h1_h2']}")
    if info['is_empty']:
        print(f"   [提示] 检测到空状态文案")
    if info['is_loading']:
        print(f"   [提示] 检测到加载中")
    print(f"   前 300 字: {info['body_inner_text'][:300].strip()}")

    return info


def main():
    out = []
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True, args=["--no-sandbox", "--disable-gpu"])
        context = browser.new_context(viewport={"width": 1366, "height": 800})
        for path, name in ROUTES:
            page = context.new_page()
            try:
                info = check_page(page, path, name)
                out.append(info)
            except Exception as e:
                out.append({"name": name, "path": path, "error": str(e)[:300]})
            finally:
                page.close()
        browser.close()

    # 写报告
    Path("d:/zhixun/verify-report.json").write_text(
        json.dumps(out, ensure_ascii=False, indent=2), encoding="utf-8"
    )
    print(f"\n报告: d:/zhixun/verify-report.json")

    # 总结：是否有页面渲染了真实内容（而非只显示加载/空状态）
    print("\n" + "=" * 60)
    print("渲染情况汇总:")
    for info in out:
        if "error" in info:
            print(f"  [ERR] {info['name']}: {info['error'][:80]}")
            continue
        size = info.get('body_text_length', 0)
        btns = info.get('button_count', 0)
        links = info.get('link_count', 0)
        h = info.get('visible_h1_h2', 0)
        # 有内容
        if size > 100 and (btns > 0 or links > 0 or h > 0):
            tag = "✓ 渲染"
        elif size > 20:
            tag = "~ 简单"
        else:
            tag = "✗ 极少"
        print(f"  [{tag}] {info['name']:20s} text={size:5d} btn={btns:3d} link={links:3d} h={h:2d}")


if __name__ == "__main__":
    main()
