package com.mycompany.myapp.security;

import java.util.Optional;
import java.util.stream.Stream;

/**
 * Screen catalog for RBAC. Keys must stay identical to FE `src/lib/rbac.ts` ScreenKey.
 *
 * <p>Label = tên màn đúng như hiển thị trong app, module = nơi mở màn đó, để admin nhận ra được khi
 * cấp quyền. {@code hidden} = màn đã có route nhưng chưa nối vào menu/luồng nào; vẫn cấp quyền được
 * nhưng mặc định không bày ra trong ma trận cho gọn.
 */
public enum ScreenKey {
    DASHBOARD("dashboard", "Tổng quan", "Tổng quan"),

    CHO_BAN_GIAO("cho-ban-giao", "Chờ bàn giao", "Hoạt động"),
    NHAP_KHO_LUAN_CHUYEN("nhap-kho-luan-chuyen", "Nhập kho - Luân chuyển - Đang giao", "Hoạt động"),
    GIAO_THANH_CONG("giao-thanh-cong", "Giao thành công", "Hoạt động"),
    CHO_GIAO_LAI("cho-giao-lai", "Chờ giao lại", "Hoạt động"),
    DON_HOAN("don-hoan", "Đơn hoàn", "Hoạt động"),
    DON_HUY("don-huy", "Đơn huỷ", "Hoạt động"),
    NGOAI_LE("ngoai-le", "Ngoại lệ - Thất lạc - Hư hỏng", "Hoạt động"),
    TON_KHO("ton-kho", "Tồn kho", "Hoạt động"),
    BAO_CAO_GIO("bao-cao-gio", "Báo cáo đơn theo giờ", "Hoạt động"),

    VAN_DON("van-don", "Vận đơn — đơn chờ gán xe & chi tiết đơn", "Vận đơn"),
    DUYET_HUY("duyet-huy", "Hàng trên xe — huỷ đơn", "Vận đơn"),
    DIEU_CHINH("dieu-chinh", "Đơn hàng đến — điều chỉnh", "Vận đơn"),
    HOAN_HANG("hoan-hang", "Hàng đã giao — hoàn hàng", "Vận đơn"),
    DAY_SHIP("day-ship", "Đẩy ship đối tác", "Vận đơn"),

    PHIEU_THU("phieu-thu", "Phiếu thu", "Tài chính"),
    DANH_SACH_PHIEU_THU("danh-sach-phieu-thu", "Danh sách phiếu thu", "Tài chính"),

    BANG_GIA("bang-gia", "Bảng giá", "Quản trị"),
    PHU_PHI("phu-phi", "Cài đặt phụ phí", "Quản trị"),
    MASTER("master", "Master dữ liệu", "Quản trị"),
    TAI_KHOAN("tai-khoan", "Tài khoản", "Quản trị"),
    NHOM_QUYEN("nhom-quyen", "Nhóm quyền", "Quản trị"),
    TICH_HOP("tich-hop", "Tích hợp", "Quản trị"),

    // 4 tác vụ trên app; dùng chung khoá quyền với màn web tương ứng.
    HANG_CHO_LEN_XE("hang-cho-len-xe", "Lên hàng — web: Hàng chờ lên xe", "Tác vụ app"),
    QUET_NHAP("quet-nhap", "Xuống hàng — web: Quét nhập", "Tác vụ app"),
    KIEM_KE("kiem-ke", "Kiểm kho — web: Thông tin kiểm kê", "Tác vụ app"),
    POD_QUAY("pod-quay", "Giao khách tại quầy (POD) — app + web", "Tác vụ app"),
    GIAO_TAN_NHA("giao-tan-nha", "Giao khách tận nhà (shipper)", "Tác vụ app"),

    // Suy ra từ 4 tác vụ ở trên: có bất kỳ tác vụ nào thì vào được trang tác vụ.
    TAC_VU("tac-vu", "Trang tác vụ (app)", "Tác vụ app", true, true),

    // Có route nhưng chưa nối vào menu/luồng nào — ẩn khỏi ma trận mặc định.
    CHUYEN("chuyen", "Chuyến vận chuyển", "Chưa mở trên menu", true),
    QUET_XUAT("quet-xuat", "Quét xuất chuyến", "Chưa mở trên menu", true),
    DOI_SOAT("doi-soat", "Đối soát chuyến", "Chưa mở trên menu", true),
    HANG_SAP_VE("hang-sap-ve", "Hàng sắp về", "Chưa mở trên menu", true),
    BAO_CAO_THU("bao-cao-thu", "Báo cáo thu ngày", "Chưa mở trên menu", true);

    private final String key;
    private final String label;
    private final String module;
    private final boolean hidden;
    private final boolean derived;

    ScreenKey(String key, String label, String module) {
        this(key, label, module, false, false);
    }

    ScreenKey(String key, String label, String module, boolean hidden) {
        this(key, label, module, hidden, false);
    }

    ScreenKey(String key, String label, String module, boolean hidden, boolean derived) {
        this.key = key;
        this.label = label;
        this.module = module;
        this.hidden = hidden;
        this.derived = derived;
    }

    public String key() {
        return key;
    }

    public String label() {
        return label;
    }

    public String module() {
        return module;
    }

    /** Màn chưa nối vào menu/luồng nào — ma trận quyền mặc định không bày ra. */
    public boolean hidden() {
        return hidden;
    }

    /** Quyền tính từ màn khác, không cấp trực tiếp → không hiện trong ma trận, không lưu grant. */
    public boolean derived() {
        return derived;
    }

    /** Các tác vụ trên app; có quyền ở bất kỳ tác vụ nào thì mở được trang tác vụ. */
    public static ScreenKey[] appTasks() {
        return new ScreenKey[] { HANG_CHO_LEN_XE, QUET_NHAP, KIEM_KE, POD_QUAY, GIAO_TAN_NHA };
    }

    public static Optional<ScreenKey> fromKey(String key) {
        if (key == null) {
            return Optional.empty();
        }
        String needle = key.trim();
        return Stream.of(values()).filter(s -> s.key.equalsIgnoreCase(needle)).findFirst();
    }
}
