package com.rnd.app.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Cleaner;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Service;

/**
 * 富文本清洗：基于 jsoup 的真实 HTML 解析 + 标签/属性白名单。
 *
 * <p>历史实现用正则剥离标签，无法正确处理畸形标签：例如
 * {@code <img src=x onerror=alert(1)}（缺少结尾 ">"）会原样通过清洗，
 * 前端拼接进 innerHTML 后被浏览器补全，形成存储型 XSS。
 * 这里改用 jsoup 解析，白名单与历史 ALLOWED 集合保持一致。</p>
 */
@Service
public class RichTextSanitizer {

    /** 允许的标签集合（与历史实现保持一致，不含 img：正文图片使用 [[image:id]] 标记）。 */
    private static final Safelist SAFELIST = new Safelist()
            .addTags("p", "br", "strong", "em", "u", "h2", "h3", "ul", "ol", "li",
                    "blockquote", "code", "pre", "table", "thead", "tbody", "tr", "th", "td", "a")
            .addAttributes("a", "href")
            .addProtocols("a", "href", "http", "https", "mailto");

    private static final String IMAGE_MARKER = "\\[\\[image:\\d+]]";

    public String sanitize(String value) {
        if (value == null) return "";
        Document dirty = Jsoup.parseBodyFragment(value);
        Document clean = new Cleaner(SAFELIST).clean(dirty);
        clean.outputSettings(new Document.OutputSettings().prettyPrint(false));
        for (Element link : clean.body().select("a[href]")) {
            link.attr("rel", "noopener noreferrer");
            link.attr("target", "_blank");
        }
        return clean.body().html();
    }

    public boolean hasText(String html) {
        if (html == null) return false;
        return !Jsoup.parse(html).text().replaceAll(IMAGE_MARKER, "").trim().isEmpty();
    }
}
