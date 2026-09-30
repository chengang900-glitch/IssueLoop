package com.rnd.app.config;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 安全响应头。
 *
 * <p>CSP 作为 XSS 的兜底：即使富文本清洗被绕过，内联脚本与外部脚本都会被浏览器拒绝。
 * 前端没有内联 &lt;script&gt; 与内联事件属性，资源全部自托管，因此可以收紧到
 * script-src 'self'；样式仍需要 'unsafe-inline'（表格/图表用 style 属性控制宽度），
 * 图片需要 blob:（描述图片与附件通过 URL.createObjectURL 展示）。
 * H2 控制台自带内联脚本，仅对它跳过 CSP，避免破坏本地调试。</p>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class SecurityHeadersFilter extends OncePerRequestFilter {

    static final String CONTENT_SECURITY_POLICY = String.join("; ",
            "default-src 'self'",
            "script-src 'self'",
            "style-src 'self' 'unsafe-inline'",
            "img-src 'self' data: blob:",
            "connect-src 'self'",
            "font-src 'self'",
            "object-src 'none'",
            "base-uri 'self'",
            "form-action 'self'",
            "frame-ancestors 'self'");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("Referrer-Policy", "no-referrer");
        String path = request.getRequestURI().substring(request.getContextPath().length());
        if (!path.startsWith("/h2-console")) {
            response.setHeader("Content-Security-Policy", CONTENT_SECURITY_POLICY);
        }
        chain.doFilter(request, response);
    }
}
