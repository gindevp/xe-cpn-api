package com.mycompany.myapp.security;

import com.mycompany.myapp.domain.enumeration.RoleCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.server.ResponseStatusException;

/**
 * BL/inactive write block + screen-level role map (TASK-010) aligned to FE rbac.ts.
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE - 10)
public class StaffWriteGuardFilter extends OncePerRequestFilter {

    private static final Set<String> WRITE_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");

    private final StaffAccessService staffAccessService;

    public StaffWriteGuardFilter(StaffAccessService staffAccessService) {
        this.staffAccessService = staffAccessService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
        String method = request.getMethod();
        String path = request.getRequestURI();
        if (WRITE_METHODS.contains(method) && path.startsWith("/api/") && !isPublicWrite(method, path)) {
            try {
                staffAccessService.requireWritable();
                enforceScreenWrite(path);
            } catch (ResponseStatusException ex) {
                response.sendError(ex.getStatusCode().value(), ex.getReason());
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

    private void enforceScreenWrite(String path) {
        // POD (pod-quay + giao VP / mobile giao khách): Q, G, TCN, DH, BX, AD — KT/BL blocked
        if (path.matches(".*/api/orders/[^/]+/pod/?$")) {
            staffAccessService.requireAnyRole(
                StaffAccessService.roles(RoleCode.Q, RoleCode.G, RoleCode.TCN, RoleCode.DH, RoleCode.BX, RoleCode.AD)
            );
            return;
        }
        // Master CRUD
        if (
            path.startsWith("/api/offices") ||
            path.startsWith("/api/routes") ||
            path.startsWith("/api/vehicles") ||
            path.startsWith("/api/drivers")
        ) {
            staffAccessService.requireAnyRole(StaffAccessService.roles(RoleCode.DH, RoleCode.AD));
            return;
        }
        // Tài khoản
        if (path.startsWith("/api/staff-admin")) {
            staffAccessService.requireAnyRole(StaffAccessService.roles(RoleCode.AD));
            return;
        }
        // Bảng giá / phụ phí / tích hợp config writes
        if (
            path.startsWith("/api/pricing-rules") ||
            path.startsWith("/api/door-fee-rules") ||
            path.startsWith("/api/product-price-rules") ||
            path.startsWith("/api/surcharge-policy") ||
            path.startsWith("/api/integration-config")
        ) {
            staffAccessService.requireAnyRole(StaffAccessService.roles(RoleCode.AD));
            return;
        }
        // Receipts
        if (path.startsWith("/api/receipts") && !path.contains("/candidates")) {
            staffAccessService.requireAnyRole(StaffAccessService.roles(RoleCode.Q, RoleCode.TCN, RoleCode.DH, RoleCode.KT, RoleCode.AD));
        }
    }

    private static boolean isPublicWrite(String method, String path) {
        if ("POST".equals(method) && "/api/authenticate".equals(path)) {
            return true;
        }
        if ("POST".equals(method) && "/api/orders/drafts".equals(path)) {
            return true;
        }
        if ("POST".equals(method) && "/api/orders/track".equals(path)) {
            return true;
        }
        if ("POST".equals(method) && path.startsWith("/api/account/reset-password")) {
            return true;
        }
        return "POST".equals(method) && "/api/register".equals(path);
    }
}
