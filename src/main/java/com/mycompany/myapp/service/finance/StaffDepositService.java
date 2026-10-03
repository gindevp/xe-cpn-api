package com.mycompany.myapp.service.finance;

import com.mycompany.myapp.domain.IntegrationConfig;
import com.mycompany.myapp.domain.Office;
import com.mycompany.myapp.domain.Receipt;
import com.mycompany.myapp.domain.ShipmentOrder;
import com.mycompany.myapp.domain.StaffProfile;
import com.mycompany.myapp.repository.IntegrationConfigRepository;
import com.mycompany.myapp.repository.ReceiptListRow;
import com.mycompany.myapp.repository.ReceiptOrderLineRepository;
import com.mycompany.myapp.repository.ReceiptRepository;
import com.mycompany.myapp.repository.ShipmentOrderRepository;
import com.mycompany.myapp.repository.StaffProfileRepository;
import com.mycompany.myapp.security.SecurityUtils;
import com.mycompany.myapp.service.audit.AuditRecorder;
import com.mycompany.myapp.service.finance.FinanceFacadeService.CandidateDTO;
import com.mycompany.myapp.service.finance.FinanceFacadeService.CreateReceiptRequest;
import com.mycompany.myapp.service.finance.FinanceFacadeService.ReceiptDTO;
import com.mycompany.myapp.service.finance.FinanceFacadeService.ReceiptLineRequest;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * NV tự nộp tiền trên app: chọn đơn mình đang giữ tiền → lập phiếu thu cho chính mình → chuyển khoản theo QR VietQR
 * → gửi ảnh chuyển khoản. KT xác nhận "Đã thu" trên web như phiếu thu thường.
 */
@Service
@Transactional
public class StaffDepositService {

    static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");
    static final String DEFAULT_TEMPLATE = "{MA_NV} NOP {MA_PHIEU}";
    /** VietQR addInfo: tối đa 50 ký tự, không dấu. */
    static final int MAX_CONTENT_LENGTH = 50;
    static final int MAX_IMAGE_CHARS = 2_500_000;
    static final int HISTORY_DAYS = 60;

    private final FinanceFacadeService financeFacadeService;
    private final ShipmentOrderRepository shipmentOrderRepository;
    private final ReceiptRepository receiptRepository;
    private final ReceiptOrderLineRepository receiptOrderLineRepository;
    private final StaffProfileRepository staffProfileRepository;
    private final IntegrationConfigRepository integrationConfigRepository;
    private final AuditRecorder auditRecorder;

    public StaffDepositService(
        FinanceFacadeService financeFacadeService,
        ShipmentOrderRepository shipmentOrderRepository,
        ReceiptRepository receiptRepository,
        ReceiptOrderLineRepository receiptOrderLineRepository,
        StaffProfileRepository staffProfileRepository,
        IntegrationConfigRepository integrationConfigRepository,
        AuditRecorder auditRecorder
    ) {
        this.financeFacadeService = financeFacadeService;
        this.shipmentOrderRepository = shipmentOrderRepository;
        this.receiptRepository = receiptRepository;
        this.receiptOrderLineRepository = receiptOrderLineRepository;
        this.staffProfileRepository = staffProfileRepository;
        this.integrationConfigRepository = integrationConfigRepository;
        this.auditRecorder = auditRecorder;
    }

    // ---------------------------------------------------------------- đơn cần nộp

    @Transactional(readOnly = true)
    public List<MyCandidate> myCandidates() {
        String login = currentLogin();
        Map<String, Merged> merged = mergeOwn(financeFacadeService.candidates(scopeOffice(login), null), login);
        if (merged.isEmpty()) {
            return List.of();
        }
        Map<String, ShipmentOrder> orders = shipmentOrderRepository
            .findWithOfficesByOrderCodeIn(merged.keySet())
            .stream()
            .collect(Collectors.toMap(ShipmentOrder::getOrderCode, Function.identity(), (a, b) -> a));
        List<MyCandidate> out = new ArrayList<>();
        for (Merged m : merged.values()) {
            ShipmentOrder o = orders.get(m.orderCode);
            Office from = o != null ? o.getFromOffice() : null;
            Office to = o == null ? null : o.getFinalToOffice() != null ? o.getFinalToOffice() : o.getToOffice();
            out.add(
                new MyCandidate(
                    m.orderCode,
                    m.amount,
                    m.portion,
                    m.collectedAt,
                    o != null && o.getGoodsType() != null ? o.getGoodsType().name() : null,
                    o != null ? o.getNote() : null,
                    officeView(from),
                    officeView(to)
                )
            );
        }
        out.sort(Comparator.comparing(MyCandidate::collectedAt, Comparator.nullsLast(Comparator.reverseOrder())));
        return out;
    }

    /** Gộp 2 phần (phía gửi / khi giao) của cùng NV thành 1 dòng — giống màn Phiếu thu web. */
    static Map<String, Merged> mergeOwn(List<CandidateDTO> all, String login) {
        Map<String, Merged> out = new LinkedHashMap<>();
        for (CandidateDTO c : all) {
            if (c.debtOwnerUsername() == null || !c.debtOwnerUsername().trim().equalsIgnoreCase(login)) {
                continue;
            }
            BigDecimal due = c.dueAmount() == null ? BigDecimal.ZERO : c.dueAmount();
            if (due.signum() <= 0) {
                continue;
            }
            Merged prev = out.get(c.orderCode());
            if (prev == null) {
                out.put(c.orderCode(), new Merged(c.orderCode(), due, c.portion(), c.collectedAt()));
            } else {
                Instant at = prev.collectedAt == null
                    ? c.collectedAt()
                    : c.collectedAt() == null || prev.collectedAt.isBefore(c.collectedAt()) ? prev.collectedAt : c.collectedAt();
                out.put(c.orderCode(), new Merged(c.orderCode(), prev.amount.add(due), null, at));
            }
        }
        return out;
    }

    record Merged(String orderCode, BigDecimal amount, String portion, Instant collectedAt) {}

    // ---------------------------------------------------------------- lập phiếu

    public MyReceipt createMyReceipt(List<String> orderCodes) {
        if (orderCodes == null || orderCodes.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chưa chọn đơn để nộp");
        }
        String login = currentLogin();
        Map<String, Merged> own = mergeOwn(financeFacadeService.candidates(scopeOffice(login), null), login);
        List<ReceiptLineRequest> lines = new ArrayList<>();
        for (String raw : orderCodes.stream().filter(s -> s != null && !s.isBlank()).map(String::trim).distinct().toList()) {
            Merged m = own.get(raw);
            if (m == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Đơn " + raw + " không còn trong danh sách tiền bạn cần nộp");
            }
            lines.add(new ReceiptLineRequest(m.orderCode, m.amount, m.portion));
        }
        if (lines.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chưa chọn đơn để nộp");
        }
        StaffProfile me = staffProfileRepository.findOneByUserLoginIgnoreCase(login).orElse(null);
        String payerName = me != null && notBlank(me.getDisplayName()) ? me.getDisplayName().trim() : login;
        ReceiptDTO dto = financeFacadeService.createReceipt(
            new CreateReceiptRequest(payerName, login.toUpperCase(Locale.ROOT), null, lines)
        );
        Receipt receipt = requireOwnReceipt(dto.receiptCode(), login);
        return toMyReceipt(receipt, lines.size(), currentConfig(), me);
    }

    // ---------------------------------------------------------------- danh sách đã nộp

    @Transactional(readOnly = true)
    public List<MyReceipt> myReceipts() {
        String login = currentLogin();
        Instant from = Instant.now().minus(Duration.ofDays(HISTORY_DAYS));
        List<ReceiptListRow> rows = receiptRepository.findPayerRowsSince(login, from);
        if (rows.isEmpty()) {
            return List.of();
        }
        Map<Long, Long> counts = new LinkedHashMap<>();
        for (var l : receiptOrderLineRepository.findByReceiptIdsWithOrder(rows.stream().map(ReceiptListRow::id).toList())) {
            counts.merge(l.getReceipt().getId(), 1L, Long::sum);
        }
        IntegrationConfig cfg = currentConfig();
        StaffProfile me = staffProfileRepository.findOneByUserLoginIgnoreCase(login).orElse(null);
        List<MyReceipt> out = new ArrayList<>();
        for (ReceiptListRow r : rows) {
            out.add(
                new MyReceipt(
                    r.receiptCode(),
                    r.totalAmount(),
                    r.createdAt(),
                    counts.getOrDefault(r.id(), 0L).intValue(),
                    r.confirmedAt(),
                    r.confirmedByUsername(),
                    Boolean.TRUE.equals(r.hasTransferProof()),
                    Boolean.TRUE.equals(r.hasConfirmProof()),
                    transferInfo(cfg, me, login, r.receiptCode(), r.officeCode(), r.createdAt(), r.totalAmount())
                )
            );
        }
        return out;
    }

    // ---------------------------------------------------------------- ảnh chuyển khoản

    public MyReceipt uploadTransferProof(String receiptCode, String image) {
        String login = currentLogin();
        if (image == null || image.isBlank() || !image.trim().startsWith("data:image/")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ảnh chuyển khoản không hợp lệ");
        }
        if (image.length() > MAX_IMAGE_CHARS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ảnh quá lớn, vui lòng chọn ảnh khác");
        }
        Receipt receipt = requireOwnReceipt(receiptCode, login);
        if (receipt.getConfirmedAt() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Phiếu đã được kế toán xác nhận, không đổi ảnh được");
        }
        boolean replaced = notBlank(receipt.getTransferProofImage());
        receipt.setTransferProofImage(image.trim());
        receipt.setTransferProofAt(Instant.now());
        receipt = receiptRepository.save(receipt);
        auditRecorder.record(
            replaced ? "RECEIPT_TRANSFER_PROOF_REPLACE" : "RECEIPT_TRANSFER_PROOF",
            "Receipt",
            receipt.getReceiptCode(),
            "NV gửi ảnh chuyển khoản nộp tiền"
        );
        StaffProfile me = staffProfileRepository.findOneByUserLoginIgnoreCase(login).orElse(null);
        return toMyReceipt(receipt, receiptOrderLineRepository.findByReceipt_Id(receipt.getId()).size(), currentConfig(), me);
    }

    @Transactional(readOnly = true)
    public String myProofImage(String receiptCode) {
        Receipt receipt = requireOwnReceipt(receiptCode, currentLogin());
        return notBlank(receipt.getTransferProofImage()) ? receipt.getTransferProofImage() : receipt.getConfirmProofImage();
    }

    // ---------------------------------------------------------------- cấu hình tài khoản nhận

    @Transactional(readOnly = true)
    public DepositAccount getAccount() {
        IntegrationConfig c = currentConfig();
        return new DepositAccount(
            c.getDepositBankBin(),
            c.getDepositBankName(),
            c.getDepositAccountNo(),
            c.getDepositAccountName(),
            notBlank(c.getDepositContentTemplate()) ? c.getDepositContentTemplate() : DEFAULT_TEMPLATE
        );
    }

    public DepositAccount putAccount(DepositAccount in) {
        if (in == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thiếu dữ liệu cấu hình");
        }
        String bin = trimToNull(in.bankBin());
        String accountNo = trimToNull(in.accountNo());
        if (bin != null && !bin.matches("\\d{6}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã ngân hàng (BIN) phải gồm 6 chữ số");
        }
        if (accountNo != null && !accountNo.matches("[0-9A-Za-z]{4,30}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số tài khoản chỉ gồm chữ và số (4–30 ký tự)");
        }
        String template = trimToNull(in.contentTemplate());
        if (template != null && template.length() > 255) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mẫu nội dung quá dài");
        }
        IntegrationConfig c = integrationConfigRepository.findAll().stream().findFirst().orElseGet(IntegrationConfig::new);
        c.setDepositBankBin(bin);
        c.setDepositBankName(trimToNull(in.bankName()));
        c.setDepositAccountNo(accountNo);
        c.setDepositAccountName(trimToNull(in.accountName()) == null ? null : in.accountName().trim().toUpperCase(Locale.ROOT));
        c.setDepositContentTemplate(template);
        c.setUpdatedAt(Instant.now());
        integrationConfigRepository.save(c);
        auditRecorder.record(
            "DEPOSIT_ACCOUNT_UPDATE",
            "IntegrationConfig",
            "deposit",
            "TK nhận tiền nộp: " +
            (c.getDepositBankName() == null ? "—" : c.getDepositBankName()) +
            " · " +
            (accountNo == null ? "—" : accountNo)
        );
        return getAccount();
    }

    // ---------------------------------------------------------------- helpers

    private MyReceipt toMyReceipt(Receipt r, int orderCount, IntegrationConfig cfg, StaffProfile me) {
        return new MyReceipt(
            r.getReceiptCode(),
            r.getTotalAmount(),
            r.getCreatedAt(),
            orderCount,
            r.getConfirmedAt(),
            r.getConfirmedByUsername(),
            notBlank(r.getTransferProofImage()),
            notBlank(r.getConfirmProofImage()) || notBlank(r.getTransferProofImage()),
            transferInfo(
                cfg,
                me,
                currentLogin(),
                r.getReceiptCode(),
                r.getOffice() != null ? r.getOffice().getCode() : null,
                r.getCreatedAt(),
                r.getTotalAmount()
            )
        );
    }

    private TransferInfo transferInfo(
        IntegrationConfig cfg,
        StaffProfile me,
        String login,
        String receiptCode,
        String officeCode,
        Instant createdAt,
        BigDecimal amount
    ) {
        if (!notBlank(cfg.getDepositBankBin()) || !notBlank(cfg.getDepositAccountNo())) {
            return null;
        }
        String staffCode = me != null && notBlank(me.getStaffCode()) ? me.getStaffCode().trim() : login;
        String staffName = me != null && notBlank(me.getDisplayName()) ? me.getDisplayName().trim() : login;
        String office = notBlank(officeCode) ? officeCode : me != null && me.getOffice() != null ? me.getOffice().getCode() : "";
        String content = renderContent(
            cfg.getDepositContentTemplate(),
            staffCode,
            staffName,
            receiptCode,
            office,
            createdAt == null ? Instant.now() : createdAt
        );
        return new TransferInfo(
            cfg.getDepositBankBin(),
            cfg.getDepositBankName(),
            cfg.getDepositAccountNo(),
            cfg.getDepositAccountName(),
            content,
            vietQrUrl(cfg.getDepositBankBin(), cfg.getDepositAccountNo(), cfg.getDepositAccountName(), amount, content)
        );
    }

    /** Thay biến trong mẫu rồi bỏ dấu, chỉ giữ chữ/số/khoảng trắng (ngân hàng hay cắt ký tự đặc biệt), tối đa 50 ký tự. */
    static String renderContent(String template, String staffCode, String staffName, String receiptCode, String officeCode, Instant at) {
        String t = notBlank(template) ? template : DEFAULT_TEMPLATE;
        String day = DateTimeFormatter.ofPattern("ddMMyy").format(at.atZone(VN));
        String raw = t
            .replace("{MA_NV}", nz(staffCode))
            .replace("{TEN_NV}", nz(staffName))
            .replace("{MA_PHIEU}", nz(receiptCode))
            .replace("{MA_VP}", nz(officeCode))
            .replace("{NGAY}", day);
        String plain = Normalizer.normalize(raw, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "")
            .replace('đ', 'd')
            .replace('Đ', 'D')
            .toUpperCase(Locale.ROOT)
            .replaceAll("[^A-Z0-9 ]", "")
            .replaceAll("\\s+", " ")
            .trim();
        return plain.length() > MAX_CONTENT_LENGTH ? plain.substring(0, MAX_CONTENT_LENGTH).trim() : plain;
    }

    /** VietQR Quick Link — ảnh QR chỉ gồm mã (qr_only), quét bằng app ngân hàng điền sẵn số tiền + nội dung. */
    static String vietQrUrl(String bin, String accountNo, String accountName, BigDecimal amount, String content) {
        StringBuilder sb = new StringBuilder("https://img.vietqr.io/image/")
            .append(bin.trim())
            .append('-')
            .append(accountNo.trim())
            .append("-qr_only.png?");
        long amt = amount == null ? 0 : amount.longValue();
        if (amt > 0) {
            sb.append("amount=").append(amt).append('&');
        }
        sb.append("addInfo=").append(enc(content));
        if (notBlank(accountName)) {
            sb.append("&accountName=").append(enc(accountName.trim()));
        }
        return sb.toString();
    }

    private Receipt requireOwnReceipt(String receiptCode, String login) {
        if (receiptCode == null || receiptCode.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thiếu mã phiếu");
        }
        Receipt r = receiptRepository
            .findOneByReceiptCode(receiptCode.trim())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy phiếu"));
        if (r.getPayerCode() == null || !r.getPayerCode().trim().equalsIgnoreCase(login)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Phiếu không phải của bạn");
        }
        return r;
    }

    /** Giống màn Phiếu thu web của NV thường: chỉ đơn thuộc VP được gán; tài khoản toàn hệ thống thì không lọc. */
    private String scopeOffice(String login) {
        return staffProfileRepository
            .findOneByUserLoginIgnoreCase(login)
            .filter(p -> !Boolean.TRUE.equals(p.getScopeAllOffices()) && p.getOffice() != null)
            .map(p -> p.getOffice().getCode())
            .orElse(null);
    }

    private IntegrationConfig currentConfig() {
        return integrationConfigRepository.findAll().stream().findFirst().orElseGet(IntegrationConfig::new);
    }

    private static OfficeView officeView(Office o) {
        return o == null ? null : new OfficeView(o.getCode(), o.getName(), o.getAddress());
    }

    private static String currentLogin() {
        return SecurityUtils.getCurrentUserLogin()
            .filter(s -> !s.isBlank())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Chưa đăng nhập"));
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    private static String trimToNull(String s) {
        return notBlank(s) ? s.trim() : null;
    }

    public record OfficeView(String code, String name, String address) {}

    public record MyCandidate(
        String orderCode,
        BigDecimal dueAmount,
        /** SENDER | DELIVERY | null = cả hai phần */
        String portion,
        Instant collectedAt,
        String goodsType,
        String note,
        OfficeView fromOffice,
        OfficeView toOffice
    ) {}

    public record TransferInfo(String bankBin, String bankName, String accountNo, String accountName, String content, String qrUrl) {}

    public record MyReceipt(
        String receiptCode,
        BigDecimal totalAmount,
        Instant createdAt,
        int orderCount,
        Instant confirmedAt,
        String confirmedBy,
        boolean hasTransferProof,
        boolean hasProof,
        /** null khi chưa cấu hình tài khoản nhận tiền */
        TransferInfo transfer
    ) {}

    public record DepositAccount(String bankBin, String bankName, String accountNo, String accountName, String contentTemplate) {}
}
