package com.mycompany.myapp.service.finance;

import com.mycompany.myapp.domain.IntegrationConfig;
import com.mycompany.myapp.domain.Office;
import com.mycompany.myapp.domain.Receipt;
import com.mycompany.myapp.domain.ShipmentOrder;
import com.mycompany.myapp.domain.StaffProfile;
import com.mycompany.myapp.repository.IntegrationConfigRepository;
import com.mycompany.myapp.repository.OfficeRepository;
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
import com.mycompany.myapp.service.storage.StoredMedia;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
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
    private final OfficeRepository officeRepository;
    private final AuditRecorder auditRecorder;

    public StaffDepositService(
        FinanceFacadeService financeFacadeService,
        ShipmentOrderRepository shipmentOrderRepository,
        ReceiptRepository receiptRepository,
        ReceiptOrderLineRepository receiptOrderLineRepository,
        StaffProfileRepository staffProfileRepository,
        IntegrationConfigRepository integrationConfigRepository,
        OfficeRepository officeRepository,
        AuditRecorder auditRecorder
    ) {
        this.financeFacadeService = financeFacadeService;
        this.shipmentOrderRepository = shipmentOrderRepository;
        this.receiptRepository = receiptRepository;
        this.receiptOrderLineRepository = receiptOrderLineRepository;
        this.staffProfileRepository = staffProfileRepository;
        this.integrationConfigRepository = integrationConfigRepository;
        this.officeRepository = officeRepository;
        this.auditRecorder = auditRecorder;
    }

    // ---------------------------------------------------------------- đơn cần nộp

    @Transactional(readOnly = true)
    public List<MyCandidate> myCandidates() {
        String login = currentLogin();
        Map<String, Merged> merged = mergeOwn(financeFacadeService.candidatesInvolving(login), login);
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
        List<CandidateDTO> all = financeFacadeService.candidatesInvolving(login);
        Map<String, Merged> own = mergeOwn(all, login);
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
        int orderCount = lines.size();
        lines.addAll(partnerFeeLines(all, login));
        StaffProfile me = staffProfileRepository.findOneByUserLoginIgnoreCase(login).orElse(null);
        String payerName = me != null && notBlank(me.getDisplayName()) ? me.getDisplayName().trim() : login;
        ReceiptDTO dto = financeFacadeService.createReceipt(
            new CreateReceiptRequest(payerName, login.toUpperCase(Locale.ROOT), null, lines)
        );
        Receipt receipt = requireOwnReceipt(dto.receiptCode(), login);
        return toMyReceipt(receipt, orderCount, currentConfig(), me);
    }

    /** Phí Ahamove NV đã trả tài xế (chưa trừ) — tự trừ vào phiếu NV tự nộp, gộp theo đơn. */
    static List<ReceiptLineRequest> partnerFeeLines(List<CandidateDTO> all, String login) {
        Map<String, BigDecimal> byOrder = new LinkedHashMap<>();
        for (CandidateDTO c : all) {
            if (
                FinanceFacadeService.PARTNER_FEE.equals(c.portion()) &&
                c.debtOwnerUsername() != null &&
                c.debtOwnerUsername().trim().equalsIgnoreCase(login) &&
                c.dueAmount() != null
            ) {
                byOrder.merge(c.orderCode(), c.dueAmount(), BigDecimal::add);
            }
        }
        List<ReceiptLineRequest> out = new ArrayList<>();
        byOrder.forEach((code, amount) -> out.add(new ReceiptLineRequest(code, amount, FinanceFacadeService.PARTNER_FEE)));
        return out;
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
        Map<Long, Set<Long>> orderIdsByReceipt = new HashMap<>();
        for (var l : receiptOrderLineRepository.findByReceiptIdsWithOrder(rows.stream().map(ReceiptListRow::id).toList())) {
            counts.merge(l.getReceipt().getId(), 1L, Long::sum);
            if (l.getOrder() != null && l.getOrder().getId() != null) {
                orderIdsByReceipt.computeIfAbsent(l.getReceipt().getId(), k -> new HashSet<>()).add(l.getOrder().getId());
            }
        }
        Set<Long> allOrderIds = new HashSet<>();
        orderIdsByReceipt.values().forEach(allOrderIds::addAll);
        Map<Long, Instant> paidAt = financeFacadeService.customerPaidAtByOrderIds(allOrderIds);
        IntegrationConfig cfg = currentConfig();
        StaffProfile me = staffProfileRepository.findOneByUserLoginIgnoreCase(login).orElse(null);
        Map<String, String> officeIds = new HashMap<>();
        List<MyReceipt> out = new ArrayList<>();
        for (ReceiptListRow r : rows) {
            Instant receiptDate = receiptDate(orderIdsByReceipt.getOrDefault(r.id(), Set.of()), paidAt, r.createdAt());
            out.add(
                new MyReceipt(
                    r.receiptCode(),
                    r.totalAmount(),
                    r.createdAt(),
                    receiptDate,
                    counts.getOrDefault(r.id(), 0L).intValue(),
                    r.confirmedAt(),
                    r.confirmedByUsername(),
                    Boolean.TRUE.equals(r.hasTransferProof()),
                    Boolean.TRUE.equals(r.hasConfirmProof()),
                    transferInfo(cfg, me, login, r.receiptCode(), r.officeCode(), receiptDate, r.totalAmount(), officeIds)
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
        receipt.setTransferProofImage(storeMedia(image.trim(), "receipt"));
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
        String proof = notBlank(receipt.getTransferProofImage()) ? receipt.getTransferProofImage() : receipt.getConfirmProofImage();
        return showMedia(proof);
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

    /** Nội dung chuyển khoản từng phiếu (mã phiếu → nội dung) theo mẫu cấu hình, như QR trên app Nộp tiền của người nộp. */
    @Transactional(readOnly = true)
    public Map<String, String> transferContents(List<ReceiptDTO> receipts) {
        Map<String, String> out = new HashMap<>();
        if (receipts == null || receipts.isEmpty()) {
            return out;
        }
        String template = integrationConfigRepository
            .findAll()
            .stream()
            .findFirst()
            .map(IntegrationConfig::getDepositContentTemplate)
            .orElse(null);
        Map<String, java.util.Optional<StaffProfile>> payers = new HashMap<>();
        Map<String, String> officeIds = new HashMap<>();
        for (ReceiptDTO r : receipts) {
            String payerKey = nz(r.payerCode()).isEmpty() ? nz(r.payerName()) : nz(r.payerCode());
            StaffProfile payer = payers.computeIfAbsent(payerKey.toLowerCase(Locale.ROOT), k -> findStaff(payerKey)).orElse(null);
            String staffCode = payer != null && notBlank(payer.getStaffCode()) ? payer.getStaffCode().trim() : payerKey;
            String staffName = payer != null && notBlank(payer.getDisplayName())
                ? payer.getDisplayName().trim()
                : nz(r.payerDisplayName()).isEmpty() ? nz(r.payerName()) : nz(r.payerDisplayName());
            String office = officeIds.computeIfAbsent(nz(r.officeCode()), code -> officeIdText(code, null));
            Instant day = r.customerPaidAt() != null ? r.customerPaidAt() : r.createdAt();
            out.put(
                r.receiptCode(),
                renderContent(template, staffCode, staffName, r.receiptCode(), office, day == null ? Instant.now() : day)
            );
        }
        return out;
    }

    private java.util.Optional<StaffProfile> findStaff(String key) {
        if (!notBlank(key)) {
            return java.util.Optional.empty();
        }
        String k = key.trim();
        java.util.Optional<StaffProfile> p = staffProfileRepository.findOneByUserLoginIgnoreCase(k);
        return p.isPresent() ? p : staffProfileRepository.findOneByStaffCodeIgnoreCase(k);
    }

    // ---------------------------------------------------------------- helpers

    /**
     * "Ngày phiếu thu" như màn Danh sách phiếu thu web: lần khách trả tiền muộn nhất trong các đơn của phiếu, chưa có thì
     * ngày lập phiếu.
     */
    static Instant receiptDate(Set<Long> orderIds, Map<Long, Instant> paidAt, Instant createdAt) {
        Instant latest = null;
        for (Long id : orderIds) {
            Instant p = paidAt.get(id);
            if (p != null && (latest == null || p.isAfter(latest))) {
                latest = p;
            }
        }
        return latest != null ? latest : createdAt;
    }

    private MyReceipt toMyReceipt(Receipt r, int orderCount, IntegrationConfig cfg, StaffProfile me) {
        Set<Long> orderIds = new HashSet<>();
        for (var l : receiptOrderLineRepository.findByReceipt_Id(r.getId())) {
            if (l.getOrder() != null && l.getOrder().getId() != null) {
                orderIds.add(l.getOrder().getId());
            }
        }
        Instant receiptDate = receiptDate(orderIds, financeFacadeService.customerPaidAtByOrderIds(orderIds), r.getCreatedAt());
        return new MyReceipt(
            r.getReceiptCode(),
            r.getTotalAmount(),
            r.getCreatedAt(),
            receiptDate,
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
                receiptDate,
                r.getTotalAmount(),
                new HashMap<>()
            )
        );
    }

    /** {MA_VP} = ID văn phòng (cột ID ở Master, office.source_id); VP chưa có ID thì dùng mã VP. */
    private String officeIdText(String officeCode, StaffProfile me) {
        Office office = notBlank(officeCode) ? officeRepository.findOneByCode(officeCode).orElse(null) : me != null ? me.getOffice() : null;
        if (office == null) {
            return nz(officeCode);
        }
        return office.getSourceId() != null ? String.valueOf(office.getSourceId()) : nz(office.getCode());
    }

    private TransferInfo transferInfo(
        IntegrationConfig cfg,
        StaffProfile me,
        String login,
        String receiptCode,
        String officeCode,
        Instant createdAt,
        BigDecimal amount,
        Map<String, String> officeIds
    ) {
        if (!notBlank(cfg.getDepositBankBin()) || !notBlank(cfg.getDepositAccountNo())) {
            return null;
        }
        String staffCode = me != null && notBlank(me.getStaffCode()) ? me.getStaffCode().trim() : login;
        String staffName = me != null && notBlank(me.getDisplayName()) ? me.getDisplayName().trim() : login;
        String office = officeIds.computeIfAbsent(nz(officeCode), code -> officeIdText(code, me));
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

    private StoredMedia storedMedia;

    @Autowired(required = false)
    void setStoredMedia(StoredMedia storedMedia) {
        this.storedMedia = storedMedia;
    }

    private String storeMedia(String value, String folder) {
        return storedMedia == null || value == null ? value : storedMedia.store(value, folder);
    }

    private String showMedia(String value) {
        return storedMedia == null || value == null ? value : storedMedia.expose(value);
    }

    public record TransferInfo(String bankBin, String bankName, String accountNo, String accountName, String content, String qrUrl) {}

    public record MyReceipt(
        String receiptCode,
        BigDecimal totalAmount,
        Instant createdAt,
        /** Ngày phiếu thu như web (ngày khách trả tiền); dùng cho "Ngày nộp" trên app và {NGAY} trong nội dung CK. */
        Instant receiptDate,
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
