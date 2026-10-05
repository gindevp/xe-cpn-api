package com.mycompany.myapp.service.order;

import com.mycompany.myapp.domain.ShipmentOrder;
import com.mycompany.myapp.domain.StaffProfile;
import com.mycompany.myapp.domain.enumeration.OrderStatus;
import com.mycompany.myapp.domain.enumeration.RoleCode;
import com.mycompany.myapp.repository.ShipmentOrderRepository;
import com.mycompany.myapp.security.SecurityUtils;
import com.mycompany.myapp.security.StaffAccessService;
import com.mycompany.myapp.web.rest.errors.BadRequestAlertException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Base64;
import java.util.Locale;
import java.util.Map;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Giấy đề nghị thanh toán COD cho người gửi — trang HTML theo mẫu BMTT-01 để in từ trình duyệt. */
@Service
@Transactional(readOnly = true)
public class CodPaymentRequestService {

    static final String LOGO = "templates/xe-logo.png";
    static final String DEPARTMENT_HEAD = "Nguyễn Tuấn Việt";
    private static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final Map<RoleCode, String> ROLE_LABELS = Map.of(
        RoleCode.Q,
        "Quầy",
        RoleCode.BX,
        "Bốc xếp",
        RoleCode.G,
        "Giao",
        RoleCode.KT,
        "Kế toán",
        RoleCode.TCN,
        "Trưởng CN",
        RoleCode.DH,
        "Điều phối",
        RoleCode.BL,
        "Ban lãnh đạo",
        RoleCode.AD,
        "Admin"
    );

    private static final String CSS =
        """
        @page{size:A4 portrait;margin:12mm 12mm 14mm}
        *{box-sizing:border-box}
        body{margin:0;font-family:"Times New Roman",Times,serif;font-size:13pt;color:#000}
        .head{display:flex;align-items:center;gap:10px}
        .head img{height:62px}
        .head .co{flex:1}
        .head .co b{font-size:13pt}
        .head .co div{font-size:10.5pt}
        .head .form{font-size:10.5pt;text-align:right;white-space:nowrap}
        h1{text-align:center;font-size:16pt;margin:18px 0 2px}
        .day{text-align:center;font-style:italic;margin-bottom:12px}
        p{margin:5px 0}
        .row2{display:flex}
        .row2>div:first-child{flex:1}
        .row2>div:last-child{width:38%}
        table.items{width:100%;border-collapse:collapse;margin:6px 0 8px;font-size:12pt}
        table.items th,table.items td{border:1px solid #000;padding:4px 5px;vertical-align:top}
        table.items th{font-weight:bold;text-align:center}
        table.items td.r{text-align:right;white-space:nowrap}
        table.items td.c{text-align:center}
        table.items tr.blank td{height:22px}
        table.items tr.total td{font-weight:bold}
        .sign{display:flex;margin-top:14px;text-align:center;page-break-inside:avoid}
        .sign>div{flex:1}
        .sign b{display:block}
        .sign i{display:block;font-size:11pt}
        .sign .name{margin-top:72px;font-weight:bold}
        """;

    private final ShipmentOrderRepository shipmentOrderRepository;
    private final StaffAccessService staffAccessService;

    public CodPaymentRequestService(ShipmentOrderRepository shipmentOrderRepository, StaffAccessService staffAccessService) {
        this.shipmentOrderRepository = shipmentOrderRepository;
        this.staffAccessService = staffAccessService;
    }

    public record Requester(String name, String position) {}

    public String buildHtml(String orderCode) {
        ShipmentOrder order = shipmentOrderRepository
            .findOneByOrderCodeOrDraftCode(orderCode == null ? "" : orderCode.trim())
            .orElseThrow(() -> new BadRequestAlertException("Không tìm thấy đơn", "order", "notfound"));
        if (!OrderFacadeService.isCodOrder(order) || OrderMoney.nz(order.getCodAmount()).signum() <= 0) {
            throw new BadRequestAlertException("Đơn không có tiền thu hộ COD", "order", "notCod");
        }
        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw new BadRequestAlertException("Đơn chưa giao thành công", "order", "notDelivered");
        }
        return html(order, currentRequester(), LocalDate.now(VN), logoDataUrl());
    }

    private Requester currentRequester() {
        String login = SecurityUtils.getCurrentUserLogin().orElse("");
        StaffProfile p = staffAccessService.current().orElse(null);
        if (p == null) {
            return new Requester(login, "");
        }
        String name = p.getDisplayName() != null && !p.getDisplayName().isBlank() ? p.getDisplayName().trim() : login;
        return new Requester(name, ROLE_LABELS.getOrDefault(p.getRoleCode(), ""));
    }

    static String html(ShipmentOrder order, Requester requester, LocalDate day, String logo) {
        BigDecimal cod = OrderMoney.nz(order.getCodAmount());
        String code = esc(order.getOrderCode());
        String money = esc(money(cod));
        String sender = esc(join(" - ", order.getSenderName(), order.getSenderPhone()));
        boolean transfer = notBlank(order.getBankAccountNo());

        StringBuilder blanks = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            blanks.append("<tr class=\"blank\"><td></td><td></td><td></td><td></td><td></td><td></td><td></td><td></td></tr>");
        }

        return (
            "<!doctype html><html lang=\"vi\"><head><meta charset=\"utf-8\"><title>De nghi thanh toan COD " +
            code +
            "</title><style>" +
            CSS +
            "</style></head><body>" +
            "<div class=\"head\">" +
            (logo.isEmpty() ? "" : "<img alt=\"X.E\" src=\"" + logo + "\">") +
            "<div class=\"co\"><b>CÔNG TY TNHH X.E VIỆT NAM</b>" +
            "<div>Số 4 Đường Văn Chỉ Thôn Tam Đa, Xã Tam Hưng, TP Hà Nội</div>" +
            "<div>VPGD: Số 21 Đại Từ - Định Công - HN</div></div>" +
            "<div class=\"form\">Biểu mẫu: BMTT - 01<br>NBH: 01/09/2025</div></div>" +
            "<h1>GIẤY ĐỀ NGHỊ THANH TOÁN</h1>" +
            "<div class=\"day\">Ngày " +
            day.getDayOfMonth() +
            " Tháng " +
            day.getMonthValue() +
            " Năm " +
            day.getYear() +
            "</div>" +
            "<p>Kính gửi: Ban lãnh đạo công ty TNHH X.E Việt Nam</p>" +
            "<div class=\"row2\"><div>Tên tôi là: <b>" +
            esc(requester.name()) +
            "</b></div><div>Chức vụ: " +
            esc(requester.position()) +
            "</div></div>" +
            "<p>Lý do xin thanh toán: Thanh toán tiền thu hộ COD cho người gửi đơn " +
            code +
            "</p>" +
            "<p>Chi tiết theo bảng kê như sau:</p>" +
            "<table class=\"items\"><thead><tr><th style=\"width:6%\">STT</th><th>Nội dung</th><th style=\"width:13%\">Số CT</th>" +
            "<th style=\"width:6%\">ĐVT</th><th style=\"width:7%\">Số lượng</th><th style=\"width:11%\">Đơn giá</th>" +
            "<th style=\"width:12%\">Thành tiền</th><th style=\"width:18%\">Chi tiết đối tượng</th></tr></thead><tbody>" +
            "<tr><td class=\"c\">1</td><td>Thanh toán tiền thu hộ COD đơn " +
            code +
            "</td><td>" +
            code +
            "</td><td class=\"c\">Đơn</td><td class=\"c\">1</td><td class=\"r\">" +
            money +
            "</td><td class=\"r\">" +
            money +
            "</td><td>" +
            sender +
            "</td></tr>" +
            blanks +
            "<tr class=\"total\"><td></td><td>Tổng cộng</td><td></td><td></td><td></td><td></td><td class=\"r\">" +
            money +
            "</td><td></td></tr></tbody></table>" +
            "<p>Số tiền bằng chữ: <b><i>" +
            esc(VietnameseMoneyWords.of(cod)) +
            "</i></b></p>" +
            "<p>Hình thức thanh toán: " +
            (transfer ? "CK" : "TM") +
            "</p>" +
            "<p>Tên chủ tài khoản: " +
            esc(nz(order.getBankAccountName())) +
            "</p>" +
            "<div class=\"row2\"><div>Số tài khoản: " +
            esc(nz(order.getBankAccountNo())) +
            "</div><div>Mở tại: " +
            esc(nz(order.getBankName())) +
            "</div></div>" +
            "<div class=\"sign\">" +
            signer("Giám đốc", "") +
            signer("Kế toán trưởng", "") +
            signer("Trưởng Bộ phận", DEPARTMENT_HEAD) +
            signer("Người đề nghị", requester.name()) +
            "</div></body></html>"
        );
    }

    private static String signer(String title, String name) {
        return "<div><b>" + title + "</b><i>(Ký, ghi rõ họ tên)</i><div class=\"name\">" + esc(name) + "&nbsp;</div></div>";
    }

    private static String logoDataUrl() {
        try (InputStream in = new ClassPathResource(LOGO).getInputStream()) {
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(in.readAllBytes());
        } catch (IOException ex) {
            throw new UncheckedIOException("Không đọc được logo", ex);
        }
    }

    static String money(BigDecimal v) {
        DecimalFormatSymbols sym = new DecimalFormatSymbols(Locale.ROOT);
        sym.setGroupingSeparator('.');
        sym.setDecimalSeparator(',');
        return new DecimalFormat("#,##0.##", sym).format(v);
    }

    static String esc(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    private static String nz(String s) {
        return s == null ? "" : s.trim();
    }

    private static String join(String sep, String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (notBlank(p)) {
                if (!sb.isEmpty()) {
                    sb.append(sep);
                }
                sb.append(p.trim());
            }
        }
        return sb.toString();
    }
}
