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
}
