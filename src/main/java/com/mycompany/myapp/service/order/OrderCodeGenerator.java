package com.mycompany.myapp.service.order;

import com.mycompany.myapp.repository.ShipmentOrderRepository;
import com.mycompany.myapp.web.rest.errors.BadRequestAlertException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Component;

/**
 * Business code generators aligned with FE {@code pricing.ts}.
 */
@Component
public class OrderCodeGenerator {

    /** Ngày trong mã: ddMM (không năm) — dễ đọc, tìm theo ngày trong tháng. */
    private static final DateTimeFormatter DDMM = DateTimeFormatter.ofPattern("ddMM");
    /** Số thứ tự / quota reset theo ngày làm việc VN, không theo timezone máy chủ. */
    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    /** Soft limit đơn/VP/ngày — quá mức cần xác nhận nhân viên. */
    private static final int DAILY_SOFT_LIMIT = 1000;
    private static final int SUFFIX_LEN = 4;
    /**
     * Alphabet 32 ký tự — bỏ 0/O/1/I/L để đọc/nói điện thoại ít nhầm.
     * 32^4 ≈ 1.05M hậu tố; kết hợp check trùng 4 ký tự cuối toàn DB.
     */
    private static final char[] SUFFIX_CHARS = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ".toCharArray();
    private static final int MAX_PROBES = 80;

    /** Mã đơn của VP/ngày đã vượt {@value #DAILY_SOFT_LIMIT} — FE bắt key này để hỏi xác nhận. */
    public static final String OVERFLOW_ERROR_KEY = "orderDailySequenceOverflow";

    private final ShipmentOrderRepository shipmentOrderRepository;

    public OrderCodeGenerator(ShipmentOrderRepository shipmentOrderRepository) {
        this.shipmentOrderRepository = shipmentOrderRepository;
    }

    /** Format: {@code N-{office}-{4 digits}} e.g. N-GP-1234 */
    public String nextDraftCode(String officeCode) {
        String office = normalizeOffice(officeCode);
        for (int i = 0; i < 20; i++) {
            int n = ThreadLocalRandom.current().nextInt(1000, 10000);
            String code = "N-" + office + "-" + n;
            if (!shipmentOrderRepository.existsByDraftCode(code)) {
                return code;
            }
        }
        return "N-" + office + "-" + (System.currentTimeMillis() % 10000);
    }

    public String nextOrderCode(String officeCode) {
        return nextOrderCode(officeCode, false);
    }

    /**
     * Format: {@code {office}{ddMM}{XXXX}} e.g. {@code YB2309K7M2}
     * <p>
     * XXXX = 4 ký tự chữ+số (không 0/O/1/I/L), random; ưu tiên không trùng 4 ký tự cuối với mọi mã đã có
     * để tìm đơn bằng đuôi 4 ký tự ít đụng. Soft limit 1000 đơn/VP/ngày vẫn giữ (confirmOverflow).
     */
    public String nextOrderCode(String officeCode, boolean confirmOverflow) {
        String office = normalizeOffice(officeCode);
        String prefix = office + LocalDate.now(VN_ZONE).format(DDMM);
        int usedToday = shipmentOrderRepository.findOrderCodesByPrefix(prefix).size();

        if (usedToday >= DAILY_SOFT_LIMIT && !confirmOverflow) {
            throw new BadRequestAlertException(
                "Office " + office + " reached " + usedToday + " orders today",
                "shipmentOrder",
                OVERFLOW_ERROR_KEY
            );
        }

        for (int i = 0; i < MAX_PROBES; i++) {
            String suffix = randomSuffix();
            String code = prefix + suffix;
            if (shipmentOrderRepository.existsByOrderCode(code)) {
                continue;
            }
            // Tránh trùng đuôi 4 ký tự với đơn khác (kể cả mã cũ có năm) — tìm kiếm tail chính xác hơn.
            if (shipmentOrderRepository.existsByOrderCodeEndingWithIgnoreCase(suffix)) {
                continue;
            }
            return code;
        }
        throw new BadRequestAlertException("Cannot allocate order code for " + prefix, "shipmentOrder", "ordercodeexhausted");
    }

    private static String randomSuffix() {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        char[] buf = new char[SUFFIX_LEN];
        for (int i = 0; i < SUFFIX_LEN; i++) {
            buf[i] = SUFFIX_CHARS[rnd.nextInt(SUFFIX_CHARS.length)];
        }
        return new String(buf);
    }

    /** Bỏ tiền tố VP / VP_ / VP- để mã ngắn (TDN thay vì VP_TDN). */
    private static String normalizeOffice(String officeCode) {
        if (officeCode == null || officeCode.isBlank()) {
            return "XX";
        }
        String o = officeCode.trim().toUpperCase().replaceAll("^VP[_\\s.-]*", "").trim();
        return o.isBlank() ? "XX" : o;
    }
}
