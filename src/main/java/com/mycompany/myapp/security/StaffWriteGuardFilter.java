package com.mycompany.myapp.security;

import com.mycompany.myapp.service.config.DatabaseCutoverService;
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
    private final DatabaseCutoverService databaseCutoverService;

    public StaffWriteGuardFilter(StaffAccessService staffAccessService, DatabaseCutoverService databaseCutoverService) {
        this.staffAccessService = staffAccessService;
        this.databaseCutoverService = databaseCutoverService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
        String method = request.getMethod();
        String path = request.getRequestURI();
        if (WRITE_METHODS.contains(method) && databaseCutoverService.isCuttingOver()) {
            response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Đang đồng bộ database, thử lại sau");
            return;
        }
        if (
            WRITE_METHODS.contains(method) && path.startsWith("/api/") && !isPublicWrite(method, path) && !isSelfServiceWrite(method, path)
        ) {
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
        if (path.matches(".*/api/orders/[^/]+/ahamove/(dispatch|cancel|advance-in|advance-refund)/?$")) {
            staffAccessService.requireScreenWrite(ScreenKey.NHAP_KHO_LUAN_CHUYEN, ScreenKey.GIAO_TAN_NHA);
            return;
        }
        // Xuất HĐĐT MISA thủ công: màn Giao thành công hoặc kế toán (Quản lý hoá đơn)
        if (path.matches(".*/api/orders/[^/]+/invoice/issue/?$")) {
            staffAccessService.requireScreenWrite(ScreenKey.GIAO_THANH_CONG, ScreenKey.QUAN_LY_HOA_DON);
            return;
        }
        // Tích đã xuất HĐ cá nhân: chỉ kế toán (màn Quản lý hoá đơn)
        if (path.startsWith("/api/invoices/mark")) {
            staffAccessService.requireScreenWrite(ScreenKey.QUAN_LY_HOA_DON);
            return;
        }
        // Xuất bù HĐĐT: màn Quản lý hoá đơn hoặc Giao thành công
        if (path.startsWith("/api/invoices/")) {
            staffAccessService.requireScreenWrite(ScreenKey.QUAN_LY_HOA_DON, ScreenKey.GIAO_THANH_CONG);
            return;
        }
        // Master CRUD
        if (
            path.startsWith("/api/offices") ||
            path.startsWith("/api/routes") ||
            path.startsWith("/api/vehicles") ||
            path.startsWith("/api/drivers") ||
            path.startsWith("/api/shippers")
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
        // Tích hợp (gồm chính sách bắt buộc cập nhật app mobile — cùng màn Tích hợp trên web)
        if (path.startsWith("/api/integration-config") || path.startsWith("/api/admin/mobile-app-version")) {
            staffAccessService.requireScreenWrite(ScreenKey.TICH_HOP);
            return;
        }
        // Bảo trì hệ thống
        if (
            path.startsWith("/api/admin/maintenance") ||
            path.startsWith("/api/admin/session-policy") ||
            path.startsWith("/api/admin/invoice-auto-issue") ||
            path.startsWith("/api/admin/deposit-account") ||
            path.startsWith("/api/admin/track-lookup-policy") ||
            path.startsWith("/api/admin/office-screens") ||
            path.startsWith("/api/scan-voices")
        ) {
            staffAccessService.requireScreenWrite(ScreenKey.BAO_TRI);
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
            return;
        }
        // Bật/tắt bắt buộc app chụp ảnh khi báo xe rời
        if (path.startsWith("/api/vehicle-events/photo-policy")) {
            staffAccessService.requireScreenWrite(ScreenKey.BAO_TRI);
            return;
        }
        // Báo giờ xe đến/rời VP: app (Lên hàng / Xuống hàng) hoặc tab Chấm xe trên Báo giờ xe
        if (path.startsWith("/api/vehicle-events")) {
            staffAccessService.requireScreenWrite(ScreenKey.HANG_CHO_LEN_XE, ScreenKey.QUET_NHAP, ScreenKey.BAO_GIO_XE);
            return;
        }
        // Gọi Auto Call bù: tab Nhập kho giao
        if (path.startsWith("/api/auto-calls/catch-up")) {
            staffAccessService.requireScreenWrite(ScreenKey.NHAP_KHO_LUAN_CHUYEN);
            return;
        }
        // inventory-checks: mọi NV thao tác được (app Kiểm kho) — không chặn theo screen Y/R
    }

    /**
     * Chấm công: mọi NV kể cả nhóm quyền chỉ đọc — AttendanceService tự chặn hồ sơ inactive / khách hàng.
     * Nộp tiền: mọi NV, StaffDepositService chỉ cho thao tác phiếu của chính mình.
     */
    private static boolean isSelfServiceWrite(String method, String path) {
        if ("PUT".equals(method) && "/api/account/active-office".equals(path)) {
            return true;
        }
        return "POST".equals(method) && ("/api/attendance/check-in".equals(path) || path.startsWith("/api/my-deposits"));
    }

    private static boolean isPublicWrite(String method, String path) {
        if ("POST".equals(method) && "/api/authenticate".equals(path)) {
            return true;
        }
        if (
            "POST".equals(method) &&
            ("/api/orders/guest".equals(path) || "/api/orders/guest/sender-name".equals(path) || "/api/orders/drafts".equals(path))
        ) {
            return true;
        }
        if ("POST".equals(method) && "/api/ahamove/estimate-pickup-km".equals(path)) {
            return true;
        }
        if ("POST".equals(method) && ("/api/orders/track".equals(path) || path.startsWith("/api/orders/track/invoice"))) {
            return true;
        }
        if ("POST".equals(method) && path.startsWith("/api/public/office-screen/")) {
            return true;
        }
        if (
            "POST".equals(method) &&
            ("/api/public/hhvn/webhook".equals(path) ||
                "/api/public/vtech/webhook".equals(path) ||
                "/api/public/ahamove/webhook".equals(path))
        ) {
            return true;
        }
        if ("POST".equals(method) && path.startsWith("/api/account/reset-password")) {
            return true;
        }
        return "POST".equals(method) && "/api/register".equals(path);
    }
}
