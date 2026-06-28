package com.zhixun.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * HtmlWhitelistFilter 单元测试
 * 测试 OWASP HTML Sanitizer 的 XSS 防护能力
 */
@DisplayName("HtmlWhitelistFilter 测试")
class HtmlWhitelistFilterTest {

    // ========== filterRichText ==========

    @Nested
    @DisplayName("filterRichText - 合法 HTML 保留")
    class FilterRichTextKeepLegitimate {

        @Test
        @DisplayName("应保留段落和加粗标签")
        void shouldKeepParagraphAndBold() {
            String html = "<p>Hello <strong>World</strong></p>";
            String result = HtmlWhitelistFilter.filterRichText(html);
            assertTrue(result.contains("<p>"), "应保留 <p> 标签");
            assertTrue(result.contains("<strong>"), "应保留 <strong> 标签");
        }

        @Test
        @DisplayName("应保留链接标签")
        void shouldKeepAnchor() {
            String html = "<a href=\"https://example.com\" target=\"_blank\">Link</a>";
            String result = HtmlWhitelistFilter.filterRichText(html);
            assertTrue(result.contains("<a"), "应保留 <a> 标签");
            assertTrue(result.contains("Link"), "应保留链接文本");
        }

        @Test
        @DisplayName("应保留图片标签")
        void shouldKeepImage() {
            String html = "<img src=\"https://img.example.com/1.jpg\" alt=\"test\">";
            String result = HtmlWhitelistFilter.filterRichText(html);
            assertTrue(result.contains("<img"), "应保留 <img> 标签");
            assertTrue(result.contains("alt=\"test\""), "应保留 alt 属性");
        }

        @Test
        @DisplayName("应保留表格标签")
        void shouldKeepTable() {
            String html = "<table><thead><tr><th>Header</th></tr></thead><tbody><tr><td>Data</td></tr></tbody></table>";
            String result = HtmlWhitelistFilter.filterRichText(html);
            assertTrue(result.contains("<table>"), "应保留 <table> 标签");
            assertTrue(result.contains("<th>"), "应保留 <th> 标签");
            assertTrue(result.contains("<td>"), "应保留 <td> 标签");
        }

        @Test
        @DisplayName("应保留标题标签")
        void shouldKeepHeadings() {
            String html = "<h1>Title</h1><h2>Subtitle</h2>";
            String result = HtmlWhitelistFilter.filterRichText(html);
            assertTrue(result.contains("<h1>"), "应保留 <h1> 标签");
            assertTrue(result.contains("<h2>"), "应保留 <h2> 标签");
        }

        @Test
        @DisplayName("应保留列表标签")
        void shouldKeepLists() {
            String html = "<ul><li>Item 1</li><li>Item 2</li></ul>";
            String result = HtmlWhitelistFilter.filterRichText(html);
            assertTrue(result.contains("<ul>"), "应保留 <ul> 标签");
            assertTrue(result.contains("<li>"), "应保留 <li> 标签");
        }
    }

    @Nested
    @DisplayName("filterRichText - XSS 攻击防护")
    class FilterRichTextXssProtection {

        @Test
        @DisplayName("应移除 script 标签")
        void shouldRemoveScript() {
            String html = "<p>Safe</p><script>alert('xss')</script>";
            String result = HtmlWhitelistFilter.filterRichText(html);
            assertFalse(result.contains("<script>"), "应移除 <script> 标签");
            assertFalse(result.contains("alert"), "应移除脚本内容");
            assertTrue(result.contains("<p>Safe</p>"), "应保留安全内容");
        }

        @Test
        @DisplayName("应移除 onclick 事件属性")
        void shouldRemoveOnclick() {
            String html = "<p onclick=\"alert(1)\">Text</p>";
            String result = HtmlWhitelistFilter.filterRichText(html);
            assertFalse(result.contains("onclick"), "应移除 onclick 属性");
            assertTrue(result.contains("<p>"), "应保留 <p> 标签");
        }

        @Test
        @DisplayName("应移除 onerror 事件属性")
        void shouldRemoveOnerror() {
            String html = "<img src=\"x\" onerror=\"alert(1)\">";
            String result = HtmlWhitelistFilter.filterRichText(html);
            assertFalse(result.contains("onerror"), "应移除 onerror 属性");
        }

        @Test
        @DisplayName("应移除 iframe 标签")
        void shouldRemoveIframe() {
            String html = "<p>Content</p><iframe src=\"evil.com\"></iframe>";
            String result = HtmlWhitelistFilter.filterRichText(html);
            assertFalse(result.contains("<iframe>"), "应移除 <iframe> 标签");
            assertTrue(result.contains("<p>Content</p>"), "应保留安全内容");
        }

        @Test
        @DisplayName("应移除 javascript: 协议链接")
        void shouldRemoveJavascriptProtocol() {
            String html = "<a href=\"javascript:alert(1)\">Click</a>";
            String result = HtmlWhitelistFilter.filterRichText(html);
            assertFalse(result.contains("javascript:"), "应移除 javascript: 协议");
        }

        @Test
        @DisplayName("应移除 style 标签")
        void shouldRemoveStyle() {
            String html = "<style>body{color:red}</style><p>Text</p>";
            String result = HtmlWhitelistFilter.filterRichText(html);
            assertFalse(result.contains("<style>"), "应移除 <style> 标签");
        }
    }

    @Nested
    @DisplayName("filterRichText - 边界情况")
    class FilterRichTextEdgeCases {

        @Test
        @DisplayName("null 输入应返回 null")
        void shouldReturnNullForNullInput() {
            assertNull(HtmlWhitelistFilter.filterRichText(null));
        }

        @Test
        @DisplayName("空字符串应返回空字符串")
        void shouldReturnEmptyForEmptyInput() {
            assertEquals("", HtmlWhitelistFilter.filterRichText(""));
        }

        @Test
        @DisplayName("纯文本应原样返回")
        void shouldReturnPlainText() {
            String text = "Hello World, this is plain text.";
            String result = HtmlWhitelistFilter.filterRichText(text);
            assertEquals(text, result);
        }

        @Test
        @DisplayName("嵌套复杂 HTML 应安全处理")
        void shouldHandleComplexNestedHtml() {
            String html = "<div><p>Hello <b>World</b></p><script>evil()</script></div>";
            String result = HtmlWhitelistFilter.filterRichText(html);
            assertTrue(result.contains("<p>"), "应保留 <p> 标签");
            assertTrue(result.contains("<b>"), "应保留 <b> 标签");
            assertFalse(result.contains("<script>"), "应移除 <script> 标签");
        }
    }

    // ========== escapePlainText ==========

    @Nested
    @DisplayName("escapePlainText - HTML 转义")
    class EscapePlainTextTests {

        @Test
        @DisplayName("应转义 < 和 >")
        void shouldEscapeAngleBrackets() {
            String input = "<script>alert('xss')</script>";
            String result = HtmlWhitelistFilter.escapePlainText(input);
            assertTrue(result.contains("&lt;"), "应包含 &lt; 转义");
            assertTrue(result.contains("&gt;"), "应包含 &gt; 转义");
            // 原始 < 和 > 已被转义，不应再直接出现（排除 &lt; &gt; 中的 < >）
            assertTrue(result.startsWith("&lt;"), "开头应为 &lt;");
        }

        @Test
        @DisplayName("应转义引号")
        void shouldEscapeQuotes() {
            String input = "He said \"hello\"";
            String result = HtmlWhitelistFilter.escapePlainText(input);
            assertTrue(result.contains("&quot;"), "应转义双引号");
        }

        @Test
        @DisplayName("应转义 & 符号")
        void shouldEscapeAmpersand() {
            String input = "A & B";
            String result = HtmlWhitelistFilter.escapePlainText(input);
            assertTrue(result.contains("&amp;"), "应转义 & 符号");
        }

        @Test
        @DisplayName("null 输入应返回 null")
        void shouldReturnNullForNullInput() {
            assertNull(HtmlWhitelistFilter.escapePlainText(null));
        }

        @Test
        @DisplayName("空字符串应返回空字符串")
        void shouldReturnEmptyForEmptyInput() {
            assertEquals("", HtmlWhitelistFilter.escapePlainText(""));
        }

        @Test
        @DisplayName("纯文本应正常转义")
        void shouldEscapeNormalText() {
            String input = "Hello World";
            String result = HtmlWhitelistFilter.escapePlainText(input);
            assertEquals("Hello World", result);
        }
    }
}