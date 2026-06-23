package com.zhixun.security;

import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;

/**
 * HTML 白名单过滤器 — 用于富文本内容的 XSS 防护
 * <p>
 * 使用 OWASP Java HTML Sanitizer 替代正则实现，提供更安全可靠的 XSS 防护
 * <p>
 * 策略：
 * 1. 允许的安全标签：p, br, b, strong, i, em, u, s, a, img, ul, ol, li, h1-h6, blockquote, pre, code, hr, table, thead, tbody, tr, th, td, span, div, video, audio, details, summary
 * 2. 允许的安全属性：href（a标签）, src/alt/width/height（img标签）, class, target, rel, id（a标签，用于锚点链接）, data-language（code标签，用于代码语言提示）, data-*（pre/code标签，用于代码高亮）
 * 3. video/audio 标签允许 controls/width/height/src 属性
 * 4. details/summary 标签用于可折叠内容，details 允许 open 属性
 * 5. 强制 a 标签的 rel="noopener noreferrer"
 * 6. img标签允许 src/alt/width/height
 * 7. 移除所有 script/iframe/object/embed/form/input/style 等危险标签
 * 8. 移除事件处理属性（onclick/onerror 等）
 * 9. 移除 javascript: 协议链接
 */
public class HtmlWhitelistFilter {

    /** OWASP 策略工厂，线程安全，初始化一次 */
    private static final PolicyFactory POLICY = buildPolicy();

    /**
     * 构建 OWASP HTML Sanitizer 策略
     */
    private static PolicyFactory buildPolicy() {
        return new HtmlPolicyBuilder()
                // 允许的通用标签
                .allowElements("p", "br", "b", "strong", "i", "em", "u", "s",
                        "ul", "ol", "li", "blockquote", "pre", "code", "hr", "span", "div")
                // 允许标题标签
                .allowElements("h1", "h2", "h3", "h4", "h5", "h6")
                // 允许 a 标签，强制 rel="noopener noreferrer"，允许 id 属性用于锚点链接
                .allowElements("a")
                .allowAttributes("href").onElements("a")
                .allowAttributes("target").onElements("a")
                .allowAttributes("id").onElements("a")
                .requireRelNofollowOnLinks()
                // 允许 img 标签
                .allowElements("img")
                .allowAttributes("src", "alt", "width", "height").onElements("img")
                // 允许 table 相关标签
                .allowElements("table", "thead", "tbody", "tr", "th", "td")
                .allowAttributes("colspan", "rowspan", "align", "valign").onElements("td", "th")
                .allowAttributes("align", "valign").onElements("tr", "table")
                // 允许 video 标签：controls/width/height/src
                .allowElements("video")
                .allowAttributes("controls", "width", "height", "src").onElements("video")
                // 允许 audio 标签：controls/src
                .allowElements("audio")
                .allowAttributes("controls", "src").onElements("audio")
                // 允许 source 标签（video/audio 子元素）：src/type
                .allowElements("source")
                .allowAttributes("src", "type").onElements("source")
                // 允许 details/summary 标签（可折叠内容）
                .allowElements("details", "summary")
                .allowAttributes("open").onElements("details")
                // 允许 code 标签的 data-language 属性（代码语言提示）
                .allowAttributes("data-language").onElements("code")
                // 允许 pre/code 标签的 data-* 属性（代码高亮，如 data-highlight, data-line 等）
                .allowAttributes("data-highlight", "data-line", "data-start", "data-line-offset").onElements("pre", "code")
                // 允许 class 属性
                .allowAttributes("class").globally()
                .toFactory();
    }

    /**
     * 对富文本内容进行白名单过滤
     * 使用 OWASP 库替代正则实现，保留允许的安全标签和属性，移除所有危险内容
     *
     * @param html 原始 HTML 内容
     * @return 过滤后的安全 HTML
     */
    public static String filterRichText(String html) {
        if (html == null || html.isEmpty()) {
            return html;
        }
        return POLICY.sanitize(html);
    }

    /**
     * 对纯文本字段进行 HTML 转义（标题、摘要等非富文本字段）
     */
    public static String escapePlainText(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        return input
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
