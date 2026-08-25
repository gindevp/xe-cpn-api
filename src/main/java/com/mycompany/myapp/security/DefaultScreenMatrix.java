package com.mycompany.myapp.security;

import com.mycompany.myapp.domain.enumeration.RoleCode;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * Built-in screen matrix, mirroring FE `src/lib/rbac.ts`.
 *
 * <p>Used for two things: seeding the built-in role groups on first start, and as fallback when a
 * staff profile has no role group yet. Editing a role group in DB overrides this at runtime.
 */
public final class DefaultScreenMatrix {

    private static final Map<ScreenKey, Map<RoleCode, ScreenPerm>> MATRIX = build();

    private static final Map<RoleCode, String> GROUP_LABELS = groupLabels();

    private DefaultScreenMatrix() {}

    /** Effective screen perms of a built-in role, defaulting to N. */
    public static Map<ScreenKey, ScreenPerm> forRole(RoleCode role) {
        Map<ScreenKey, ScreenPerm> out = new EnumMap<>(ScreenKey.class);
        for (ScreenKey screen : ScreenKey.values()) {
            out.put(screen, MATRIX.getOrDefault(screen, Map.of()).getOrDefault(role, ScreenPerm.N));
        }
        return out;
    }

    /** Vietnamese job-title label of a built-in group. */
    public static String labelOf(RoleCode role) {
        return GROUP_LABELS.getOrDefault(role, role.name());
    }

    /**
     * Job titles that get a built-in group. Only three remain in use: Admin, Điều phối, Kế toán.
     * The other RoleCode values stay in the enum so legacy rows keep resolving via the fallback matrix.
     */
    public static RoleCode[] builtinRoles() {
        return new RoleCode[] { RoleCode.AD, RoleCode.DH, RoleCode.KT };
    }

    private static Map<RoleCode, String> groupLabels() {
        Map<RoleCode, String> m = new EnumMap<>(RoleCode.class);
        m.put(RoleCode.KH, "Khách");
        m.put(RoleCode.Q, "Quầy");
        m.put(RoleCode.BX, "Bốc xếp");
        m.put(RoleCode.G, "Giao");
        m.put(RoleCode.KT, "Kế toán");
        m.put(RoleCode.TCN, "Trưởng CN");
        m.put(RoleCode.DH, "Điều phối");
        m.put(RoleCode.BL, "Ban lãnh đạo");
        m.put(RoleCode.AD, "Admin");
        return Collections.unmodifiableMap(m);
    }

    private static Map<ScreenKey, Map<RoleCode, ScreenPerm>> build() {
        Map<ScreenKey, Map<RoleCode, ScreenPerm>> m = new EnumMap<>(ScreenKey.class);
        put(m, ScreenKey.DASHBOARD, "BL=R,DH=Y,TCN=Y,KT=R,AD=Y");
        put(m, ScreenKey.VAN_DON, "Q=Y,TCN=Y,DH=Y,KT=R,BL=R,BX=R,G=R,AD=Y");
        put(m, ScreenKey.TAC_VU, "Q=Y,BX=Y,G=Y,TCN=Y,DH=Y,KT=Y,BL=R,AD=Y");
        put(m, ScreenKey.NHAP_KHO_LUAN_CHUYEN, "Q=Y,TCN=Y,DH=Y,G=Y,BX=Y,KT=R,BL=R,AD=Y");
        put(m, ScreenKey.GIAO_THANH_CONG, "Q=Y,TCN=Y,DH=Y,G=Y,BX=Y,KT=R,BL=R,AD=Y");
        put(m, ScreenKey.CHO_GIAO_LAI, "Q=Y,TCN=Y,DH=Y,G=Y,KT=R,BL=R,AD=Y");
        put(m, ScreenKey.CHO_BAN_GIAO, "Q=Y,TCN=Y,DH=Y,G=Y,KT=R,BL=R,AD=Y");
        put(m, ScreenKey.DON_HUY, "Q=R,TCN=Y,DH=Y,KT=R,BL=R,AD=Y");
        put(m, ScreenKey.NGOAI_LE, "Q=Y,TCN=Y,DH=Y,G=R,KT=R,BL=R,AD=Y");
        put(m, ScreenKey.PHIEU_THU, "Q=Y,TCN=Y,DH=Y,KT=Y,BL=R,AD=Y");
        put(m, ScreenKey.DANH_SACH_PHIEU_THU, "Q=R,TCN=Y,DH=Y,KT=Y,BL=R,AD=Y");
        put(m, ScreenKey.DON_HOAN, "Q=Y,TCN=Y,DH=Y,G=Y,BX=Y,KT=R,BL=R,AD=Y");
        put(m, ScreenKey.HANG_CHO_LEN_XE, "Q=Y,TCN=Y,DH=Y,BX=Y,AD=Y");
        put(m, ScreenKey.DUYET_HUY, "Q=Y,TCN=Y,DH=Y,AD=Y");
        put(m, ScreenKey.HANG_SAP_VE, "Q=Y,TCN=Y,DH=Y,BX=R,BL=R,AD=Y");
        put(m, ScreenKey.DIEU_CHINH, "TCN=Y,DH=Y,AD=Y");
        put(m, ScreenKey.HOAN_HANG, "Q=Y,TCN=Y,DH=Y,AD=Y");
        put(m, ScreenKey.CHUYEN, "BX=Y,TCN=Y,DH=Y,BL=R,AD=Y");
        put(m, ScreenKey.QUET_XUAT, "BX=Y,DH=Y,AD=Y");
        put(m, ScreenKey.QUET_NHAP, "BX=Y,Q=Y,DH=Y,AD=Y");
        put(m, ScreenKey.DOI_SOAT, "BX=Y,TCN=Y,DH=Y,BL=R,AD=Y");
        put(m, ScreenKey.POD_QUAY, "Q=Y,TCN=Y,DH=Y,BX=Y,AD=Y");
        put(m, ScreenKey.GIAO_TAN_NHA, "G=Y,BX=Y,TCN=R,DH=Y,AD=Y");
        put(m, ScreenKey.DAY_SHIP, "Q=Y,DH=Y,AD=Y");
        put(m, ScreenKey.BAO_CAO_THU, "KT=Y,Q=R,TCN=R,DH=R,BL=R,AD=Y");
        put(m, ScreenKey.BANG_GIA, "AD=Y,TCN=R,DH=R,BL=R");
        put(m, ScreenKey.MASTER, "DH=Y,AD=Y");
        put(m, ScreenKey.TAI_KHOAN, "AD=Y");
        put(m, ScreenKey.TICH_HOP, "AD=Y");
        put(m, ScreenKey.NHOM_QUYEN, "AD=Y");
        put(m, ScreenKey.PHU_PHI, "AD=Y,DH=R");
        put(m, ScreenKey.TON_KHO, "Q=Y,TCN=Y,DH=Y,BX=R,KT=R,BL=R,AD=Y");
        put(m, ScreenKey.KIEM_KE, "Q=Y,TCN=Y,DH=Y,BX=R,KT=R,BL=R,AD=Y");
        put(m, ScreenKey.BAO_CAO_GIO, "Q=R,TCN=Y,DH=Y,BX=R,KT=R,BL=R,AD=Y");
        return Collections.unmodifiableMap(m);
    }

    private static void put(Map<ScreenKey, Map<RoleCode, ScreenPerm>> target, ScreenKey screen, String spec) {
        Map<RoleCode, ScreenPerm> row = new EnumMap<>(RoleCode.class);
        for (String part : spec.split(",")) {
            String[] kv = part.split("=");
            row.put(RoleCode.valueOf(kv[0].trim()), ScreenPerm.fromCode(kv[1]));
        }
        target.put(screen, Collections.unmodifiableMap(row));
    }
}
