package com.rnd.app.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RichTextSanitizerTest {
    private final RichTextSanitizer sanitizer = new RichTextSanitizer();

    @Test
    void removesScriptsUnsafeLinksAndUnknownTags() {
        String result = sanitizer.sanitize("<p>说明</p><script>alert(1)</script><a href=\"javascript:alert(1)\">x</a><video>v</video>");
        assertEquals("<p>说明</p><a>x</a>v", result);
    }

    @Test
    void preservesAllowedFormattingAndImageMarker() {
        String result = sanitizer.sanitize("<p><strong>重点</strong> [[image:12]]</p>");
        assertEquals("<p><strong>重点</strong> [[image:12]]</p>", result);
        assertTrue(sanitizer.hasText(result));
        assertFalse(sanitizer.hasText("<p> </p>"));
    }

    /**
     * 回归用例：历史正则实现要求标签自带 ">"，因此缺少结尾 ">" 的 payload 会原样通过，
     * 前端把它拼进 innerHTML 时会被浏览器用后续的 ">" 补全，从而执行 onerror。
     */
    @Test
    void neutralizesUnterminatedTagsThatBypassedRegexFilter() {
        String payload = "<p>hi</p><img src=x onerror=alert(1)";

        String result = sanitizer.sanitize(payload);

        assertEquals("<p>hi</p>", result);
        assertFalse(result.toLowerCase().contains("onerror"));
        assertFalse(result.toLowerCase().contains("<img"));
    }

    @Test
    void stripsEventHandlersOnAllowedTags() {
        String result = sanitizer.sanitize("<p onclick=\"alert(1)\" onmouseover=alert(2)>x</p>");

        assertEquals("<p>x</p>", result);
    }

    @Test
    void removesDangerousLinkProtocols() {
        assertFalse(sanitizer.sanitize("<a href=\"  JaVaScRiPt:alert(1)\">y</a>").toLowerCase().contains("javascript"));
        assertFalse(sanitizer.sanitize("<a href=\"data:text/html,<script>alert(1)</script>\">z</a>").toLowerCase().contains("data:"));
        assertFalse(sanitizer.sanitize("<a href=\"vbscript:msgbox(1)\">z</a>").toLowerCase().contains("vbscript"));
    }

    @Test
    void keepsSafeLinksWithNoopenerAndBlankTarget() {
        String result = sanitizer.sanitize("<a href=\"https://example.com/a\">x</a>");

        assertTrue(result.contains("href=\"https://example.com/a\""));
        assertTrue(result.contains("rel=\"noopener noreferrer\""));
        assertTrue(result.contains("target=\"_blank\""));
    }

    @Test
    void removesScriptAndStyleContentEntirely() {
        String result = sanitizer.sanitize("<p>a</p><script>alert(1)</script><style>body{}</style><p>b</p>");

        assertEquals("<p>a</p><p>b</p>", result);
    }

    @Test
    void doesNotResurrectPayloadsFromEntitiesOrNesting() {
        assertFalse(sanitizer.sanitize("<p>&lt;script&gt;alert(1)&lt;/script&gt;</p>").contains("<script"));
        assertFalse(sanitizer.sanitize("<scr<script>ipt>alert(1)</script>").toLowerCase().contains("<script"));
        assertFalse(sanitizer.sanitize("<svg><script>alert(1)</script></svg>").toLowerCase().contains("<script"));
        assertFalse(sanitizer.sanitize("<iframe src=\"https://evil.example\"></iframe>").toLowerCase().contains("<iframe"));
    }

    @Test
    void hasTextIgnoresMarkupAndImageMarkers() {
        assertTrue(sanitizer.hasText("<p>内容</p>"));
        assertTrue(sanitizer.hasText("<p>[[image:3]]说明</p>"));
        assertFalse(sanitizer.hasText("[[image:3]]"));
        assertFalse(sanitizer.hasText("<p><br/></p>"));
        assertFalse(sanitizer.hasText(null));
        assertEquals("", sanitizer.sanitize(null));
    }
}
