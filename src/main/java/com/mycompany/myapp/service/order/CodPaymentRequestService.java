package com.mycompany.myapp.service.order;

import com.mycompany.myapp.domain.ShipmentOrder;
import com.mycompany.myapp.domain.StaffProfile;
import com.mycompany.myapp.domain.enumeration.OrderStatus;
import com.mycompany.myapp.domain.enumeration.RoleCode;
import com.mycompany.myapp.repository.ShipmentOrderRepository;
import com.mycompany.myapp.security.SecurityUtils;
import com.mycompany.myapp.security.StaffAccessService;
import com.mycompany.myapp.web.rest.errors.BadRequestAlertException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Giấy đề nghị thanh toán COD cho người gửi — điền vào mẫu BMTT-01 (templates/cod-payment-request.xlsx). */
@Service
@Transactional(readOnly = true)
public class CodPaymentRequestService {

    static final String TEMPLATE = "templates/cod-payment-request.xlsx";
    private static final String SHEET = "xl/worksheets/sheet1.xml";
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

    private final ShipmentOrderRepository shipmentOrderRepository;
    private final StaffAccessService staffAccessService;

    public CodPaymentRequestService(ShipmentOrderRepository shipmentOrderRepository, StaffAccessService staffAccessService) {
        this.shipmentOrderRepository = shipmentOrderRepository;
        this.staffAccessService = staffAccessService;
    }

    public record Requester(String name, String position, String department) {}

    public byte[] build(String orderCode) {
        ShipmentOrder order = shipmentOrderRepository
            .findOneByOrderCodeOrDraftCode(orderCode == null ? "" : orderCode.trim())
            .orElseThrow(() -> new BadRequestAlertException("Không tìm thấy đơn", "order", "notfound"));
        if (!OrderFacadeService.isCodOrder(order) || OrderMoney.nz(order.getCodAmount()).signum() <= 0) {
            throw new BadRequestAlertException("Đơn không có tiền thu hộ COD", "order", "notCod");
        }
        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw new BadRequestAlertException("Đơn chưa giao thành công", "order", "notDelivered");
        }
        return fill(order, currentRequester(), LocalDate.now(VN));
    }

    private Requester currentRequester() {
        String login = SecurityUtils.getCurrentUserLogin().orElse("");
        StaffProfile p = staffAccessService.current().orElse(null);
        if (p == null) {
            return new Requester(login, "", "");
        }
        String name = p.getDisplayName() != null && !p.getDisplayName().isBlank() ? p.getDisplayName().trim() : login;
        String dept = p.getOffice() != null && p.getOffice().getName() != null ? p.getOffice().getName() : "";
        return new Requester(name, ROLE_LABELS.getOrDefault(p.getRoleCode(), ""), dept);
    }

    static byte[] fill(ShipmentOrder order, Requester requester, LocalDate day) {
        BigDecimal cod = OrderMoney.nz(order.getCodAmount());
        String code = order.getOrderCode();
        String sender = join(" - ", order.getSenderName(), order.getSenderPhone());
        boolean transfer = notBlank(order.getBankAccountNo());

        Map<String, Cell> cells = new LinkedHashMap<>();
        cells.put("B6", Cell.text("Ngày " + day.getDayOfMonth() + " Tháng " + day.getMonthValue() + " Năm " + day.getYear()));
        cells.put("C8", Cell.text(requester.name()));
        cells.put("H8", Cell.text(requester.position()));
        cells.put("C9", Cell.text(requester.department()));
        cells.put("B10", Cell.text("Lý do xin thanh toán: Thanh toán tiền thu hộ COD cho người gửi đơn " + code));
        cells.put("B14", Cell.number(BigDecimal.ONE));
        cells.put("C14", Cell.text("Thanh toán tiền thu hộ COD đơn " + code));
        cells.put("D14", Cell.text(code));
        cells.put("E14", Cell.text("Đơn"));
        cells.put("F14", Cell.number(BigDecimal.ONE));
        cells.put("G14", Cell.number(cod));
        cells.put("H14", Cell.number(cod));
        cells.put("I14", Cell.text(sender));
        cells.put("H20", Cell.formula("SUM(H14:H19)", cod));
        cells.put("C22", Cell.text(VietnameseMoneyWords.of(cod)));
        cells.put("B23", Cell.text("Hình thức thanh toán: " + (transfer ? "CK" : "TM")));
        cells.put("C24", Cell.text(nz(order.getBankAccountName())));
        cells.put("C25", Cell.text(nz(order.getBankAccountNo())));
        cells.put("H25", Cell.text(nz(order.getBankName())));
        cells.put("I32", Cell.text(requester.name()));

        return rewriteTemplate(sheet -> applyCells(sheet, cells));
    }

    /** Thay nội dung các ô (giữ style s="…" của mẫu). Ô chưa có trong hàng thì không thêm — mẫu đã khai đủ ô cần điền. */
    static String applyCells(String sheetXml, Map<String, Cell> cells) {
        String xml = sheetXml;
        for (Map.Entry<String, Cell> e : cells.entrySet()) {
            Matcher m = Pattern.compile("<c r=\"" + e.getKey() + "\"([^>]*?)(?:/>|>.*?</c>)").matcher(xml);
            if (!m.find()) {
                throw new IllegalStateException("Mẫu đề nghị thanh toán thiếu ô " + e.getKey());
            }
            String attrs = m.group(1).replaceAll(" t=\"[^\"]*\"", "");
            xml = xml.substring(0, m.start()) + e.getValue().xml(e.getKey(), attrs) + xml.substring(m.end());
        }
        return xml;
    }

    private static byte[] rewriteTemplate(java.util.function.UnaryOperator<String> sheetEditor) {
        ClassPathResource res = new ClassPathResource(TEMPLATE);
        try (
            InputStream raw = res.getInputStream();
            ZipInputStream in = new ZipInputStream(raw, StandardCharsets.UTF_8);
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            ZipOutputStream out = new ZipOutputStream(buf, StandardCharsets.UTF_8)
        ) {
            ZipEntry e;
            while ((e = in.getNextEntry()) != null) {
                byte[] data = in.readAllBytes();
                if (SHEET.equals(e.getName())) {
                    data = sheetEditor.apply(new String(data, StandardCharsets.UTF_8)).getBytes(StandardCharsets.UTF_8);
                }
                out.putNextEntry(new ZipEntry(e.getName()));
                out.write(data);
                out.closeEntry();
            }
            out.finish();
            return buf.toByteArray();
        } catch (IOException ex) {
            throw new UncheckedIOException("Không đọc được mẫu đề nghị thanh toán", ex);
        }
    }

    record Cell(String kind, String text, BigDecimal number, String formula) {
        static Cell text(String v) {
            return new Cell("text", v == null ? "" : v, null, null);
        }

        static Cell number(BigDecimal v) {
            return new Cell("number", null, v, null);
        }

        static Cell formula(String f, BigDecimal cached) {
            return new Cell("formula", null, cached, f);
        }

        String xml(String ref, String attrs) {
            return switch (kind) {
                case "number" -> "<c r=\"" + ref + "\"" + attrs + "><v>" + number.toPlainString() + "</v></c>";
                case "formula" -> "<c r=\"" + ref + "\"" + attrs + "><f>" + esc(formula) + "</f><v>" + number.toPlainString() + "</v></c>";
                default -> "<c r=\"" + ref + "\"" + attrs + " t=\"inlineStr\"><is><t xml:space=\"preserve\">" + esc(text) + "</t></is></c>";
            };
        }
    }

    private static String esc(String s) {
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
