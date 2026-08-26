package com.mycompany.myapp.security;

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
 * Read-only/inactive write block + screen-level write guard driven by the staff's permission group
 * (TASK-010, generalised in TASK-RBAC-GROUPS). Screen keys match FE rbac.ts.
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
        // POD: quầy giao khách hoặc giao tận nhà (web + mobile)
        if (path.matches(".*/api/orders/[^/]+/pod/?$")) {
            staffAccessService.requireScreenWrite(ScreenKey.POD_QUAY, ScreenKey.GIAO_TAN_NHA);
            return;
        }
        // Master CRUD
        if (
            path.startsWith("/api/offices") ||
            path.startsWith("/api/routes") ||
            path.startsWith("/api/vehicles") ||
            path.startsWith("/api/drivers")
        ) {
            staffAccessService.requireScreenWrite(ScreenKey.MASTER);
            return;
        }
        // Tài khoản
        if (path.startsWith("/api/staff-admin")) {
            staffAccessService.requireScreenWrite(ScreenKey.TAI_KHOAN);
            return;
        }
        // Nhóm quyền
        if (path.startsWith("/api/permission-groups")) {
            staffAccessService.requireScreenWrite(ScreenKey.NHOM_QUYEN);
            return;
        }
        // Bảng giá
        if (
            path.startsWith("/api/pricing-rules") || path.startsWith("/api/door-fee-rules") || path.startsWith("/api/product-price-rules")
        ) {
            staffAccessService.requireScreenWrite(ScreenKey.BANG_GIA);
            return;
        }
        // Phụ phí
        if (path.startsWith("/api/surcharge-policy")) {
            staffAccessService.requireScreenWrite(ScreenKey.PHU_PHI);
            return;
        }
        // Tích hợp
        if (path.startsWith("/api/integration-config")) {
            staffAccessService.requireScreenWrite(ScreenKey.TICH_HOP);
            return;
        }
        // Receipts
        if (path.startsWith("/api/receipts") && !path.contains("/candidates")) {
            staffAccessService.requireScreenWrite(ScreenKey.PHIEU_THU);
            return;
        }
        // COD export mark
        if (path.startsWith("/api/orders/cod/mark-exported")) {
            staffAccessService.requireScreenWrite(ScreenKey.QUAN_LY_DON_COD);
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
