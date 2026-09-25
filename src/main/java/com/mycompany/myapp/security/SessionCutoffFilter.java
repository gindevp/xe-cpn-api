package com.mycompany.myapp.security;

import com.mycompany.myapp.service.config.SessionPolicyService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Cắt phiên đã qua giờ cấu hình, kể cả JWT remember-me còn hạn. */
@Component
@Order(Ordered.LOWEST_PRECEDENCE - 20)
public class SessionCutoffFilter extends OncePerRequestFilter {

    private final SessionPolicyService sessionPolicyService;

    public SessionCutoffFilter(SessionPolicyService sessionPolicyService) {
        this.sessionPolicyService = sessionPolicyService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            Instant issuedAt = jwt.getIssuedAt();
            if (sessionPolicyService.sessionEnded(issuedAt, Instant.now())) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                response.getWriter().write("{\"message\":\"error.sessionCutoff\"}");
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
}
