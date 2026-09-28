package com.rnd.app.service;

import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class RichTextSanitizer {
    private static final Set<String> ALLOWED = Set.of("p", "br", "strong", "em", "u", "h2", "h3", "ul", "ol", "li", "blockquote", "code", "pre", "table", "thead", "tbody", "tr", "th", "td", "a");
    private static final Pattern TAG = Pattern.compile("</?([a-zA-Z0-9]+)([^>]*)>");
    private static final Pattern SCRIPT = Pattern.compile("(?is)<(script|style)[^>]*>.*?</\\1\\s*>");
    private static final Pattern HREF = Pattern.compile("(?i)\\s+href\\s*=\\s*(['\"])(.*?)\\1");

    public String sanitize(String value) {
        if (value == null) return "";
        String withoutExecutable = SCRIPT.matcher(value).replaceAll("");
        Matcher matcher = TAG.matcher(withoutExecutable);
        StringBuffer out = new StringBuffer();
        while (matcher.find()) {
            String name = matcher.group(1).toLowerCase();
            String raw = matcher.group();
            if (!ALLOWED.contains(name)) {
                matcher.appendReplacement(out, "");
                continue;
            }
            if (raw.startsWith("</")) {
                matcher.appendReplacement(out, "</" + name + ">");
                continue;
            }
            String attributes = "";
            if ("a".equals(name)) {
                Matcher href = HREF.matcher(matcher.group(2));
                if (href.find()) {
                    String url = href.group(2).trim();
                    if (url.startsWith("https://") || url.startsWith("http://") || url.startsWith("mailto:")) {
                        attributes = " href=\"" + url.replace("\"", "&quot;") + "\" rel=\"noopener noreferrer\" target=\"_blank\"";
                    }
                }
            }
            matcher.appendReplacement(out, "<" + name + attributes + ">");
        }
        matcher.appendTail(out);
        return out.toString().replaceAll("(?i)on[a-z]+\\s*=\\s*(['\"]).*?\\1", "");
    }

    public boolean hasText(String html) {
        return html != null && html.replaceAll("<[^>]+>", "").replaceAll("\\[\\[image:[^]]+]]", "").trim().length() > 0;
    }
}
