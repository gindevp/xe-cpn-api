package com.mycompany.myapp.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Màn tạo đơn của khách (không đăng nhập) được gọi geo + ước tính KM Ahamove — các API này gọi dịch vụ ngoài
 * (Ahamove tính theo lượt), nên khách ẩn danh bị giới hạn số lượt/phút theo IP. Nhân viên đã đăng nhập không bị giới hạn.
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE - 18)
public class PublicPartnerRateLimitFilter extends OncePerRequestFilter {

    private static final long WINDOW_MS = 60_000;
    private static final int GEO_PER_MINUTE = 60;
    private static final int AHAMOVE_PER_MINUTE = 20;
    private static final int MAX_TRACKED_KEYS = 20_000;

    private static final class Window {

        long start;
        int count;
    }

    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return !(uri.startsWith("/api/geo/") || uri.equals("/api/ahamove/estimate-pickup-km"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean anonymous = auth == null || auth instanceof AnonymousAuthenticationToken || !auth.isAuthenticated();
        if (anonymous) {
            boolean ahamove = request.getRequestURI().startsWith("/api/ahamove/");
            String key = (ahamove ? "aha:" : "geo:") + ClientIpResolver.resolve(request);
            if (!allow(key, ahamove ? AHAMOVE_PER_MINUTE : GEO_PER_MINUTE)) {
                response.setStatus(429);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                response
                    .getWriter()
                    .write("{\"message\":\"error.tooManyRequests\",\"title\":\"Thao tác quá nhanh, vui lòng thử lại sau ít phút\"}");
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

    boolean allow(String key, int limit) {
        long now = System.currentTimeMillis();
        if (windows.size() > MAX_TRACKED_KEYS) {
            windows.entrySet().removeIf(e -> now - e.getValue().start >= WINDOW_MS);
        }
        Window w = windows.computeIfAbsent(key, k -> new Window());
        synchronized (w) {
            if (now - w.start >= WINDOW_MS) {
                w.start = now;
                w.count = 0;
            }
            return ++w.count <= limit;
        }
    }
}
