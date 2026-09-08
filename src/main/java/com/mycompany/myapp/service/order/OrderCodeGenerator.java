package com.mycompany.myapp.service.order;

import com.mycompany.myapp.repository.ShipmentOrderRepository;
import com.mycompany.myapp.web.rest.errors.BadRequestAlertException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Component;

/**
 * Business code generators aligned with FE {@code pricing.ts}.
 */
@Component
public class OrderCodeGenerator {

    private static final DateTimeFormatter DDMMYY = DateTimeFormatter.ofPattern("ddMMyy");
    /** Số thứ tự reset theo ngày làm việc VN, không theo timezone của máy chủ (Railway chạy UTC). */
    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    /** 000..999 = 1000 đơn/VP/ngày; quá mức này phải có xác nhận của nhân viên. */
    private static final int DAILY_SOFT_LIMIT = 1000;
    private static final int SEQ_DIGITS = 3;
    /** Chặn vòng lặp vô hạn nếu unique constraint và dữ liệu đọc được lệch nhau. */
    private static final int MAX_PROBES = 2000;

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
     * Format: {@code {office}{DDMMYY}{STT 3 chữ số}} e.g. YB1070926000
     * <p>
     * Số thứ tự đếm riêng từng VP, bắt đầu 000 mỗi ngày. Từ đơn thứ 1001 (STT 1000) mã dài thành 4 chữ số
     * và chỉ sinh được khi {@code confirmOverflow} — nhân viên phải xác nhận ở FE.
     * Mã cũ dạng random 5 ký tự cùng ngày không tham gia đếm (hậu tố không phải số) và không trùng được vì khác độ dài.
     */
    public String nextOrderCode(String officeCode, boolean confirmOverflow) {
        String office = normalizeOffice(officeCode);
        String prefix = office + LocalDate.now(VN_ZONE).format(DDMMYY);
        int next = nextSequence(prefix);

        if (next >= DAILY_SOFT_LIMIT && !confirmOverflow) {
            throw new BadRequestAlertException(
                "Office " + office + " reached " + next + " orders today",
                "shipmentOrder",
                OVERFLOW_ERROR_KEY
            );
        }

        for (int seq = next; seq < next + MAX_PROBES; seq++) {
            String code = prefix + formatSeq(seq);
            if (!shipmentOrderRepository.existsByOrderCode(code)) {
                return code;
            }
        }
        throw new BadRequestAlertException("Cannot allocate order code for " + prefix, "shipmentOrder", "ordercodeexhausted");
    }

    /** Số thứ tự đã dùng lớn nhất trong ngày của VP, + 1. Ngày mới / VP mới thì bắt đầu từ 0. */
    private int nextSequence(String prefix) {
        List<String> codes = shipmentOrderRepository.findOrderCodesByPrefix(prefix);
        int max = -1;
        for (String code : codes) {
            if (code == null || code.length() <= prefix.length()) {
                continue;
            }
            String suffix = code.substring(prefix.length());
            if (suffix.length() > 9 || !suffix.chars().allMatch(Character::isDigit)) {
                continue;
            }
            max = Math.max(max, Integer.parseInt(suffix));
        }
        return max + 1;
    }

    /** 000..999 rồi tự nới thành 1000, 1001… khi vượt ngưỡng. */
    private static String formatSeq(int seq) {
        return String.format("%0" + SEQ_DIGITS + "d", seq);
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
