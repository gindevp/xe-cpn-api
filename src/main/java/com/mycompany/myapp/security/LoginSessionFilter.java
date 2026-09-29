package com.mycompany.myapp.security;

import com.mycompany.myapp.service.auth.LoginControlService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Từ chối JWT của phiên đã bị thu hồi (đăng nhập nơi khác / admin đăng xuất từ xa / thu hồi thiết bị). */
@Component
@Order(Ordered.LOWEST_PRECEDENCE - 19)
public class LoginSessionFilter extends OncePerRequestFilter {

    private final LoginControlService loginControlService;
    private final boolean requireSid;

    public LoginSessionFilter(LoginControlService loginControlService, @Value("${cpn.login-control.require-sid:true}") boolean requireSid) {
        this.loginControlService = loginControlService;
        this.requireSid = requireSid;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            String sid = jwt.getClaimAsString(SecurityUtils.SID_CLAIM);
            boolean ok = sid == null ? !requireSid : loginControlService.isActive(sid);
            if (!ok) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                response.getWriter().write("{\"message\":\"error.sessionRevoked\"}");
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
}
