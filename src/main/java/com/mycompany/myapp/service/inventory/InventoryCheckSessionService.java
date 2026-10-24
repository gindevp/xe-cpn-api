package com.mycompany.myapp.service.inventory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.myapp.domain.InventoryCheck;
import com.mycompany.myapp.domain.InventoryCheckScan;
import com.mycompany.myapp.domain.InventoryCheckStatus;
import com.mycompany.myapp.domain.ShipmentOrder;
import com.mycompany.myapp.domain.StaffProfile;
import com.mycompany.myapp.domain.enumeration.ForwardStage;
import com.mycompany.myapp.domain.enumeration.OrderStatus;
import com.mycompany.myapp.repository.InventoryCheckRepository;
import com.mycompany.myapp.repository.InventoryCheckScanRepository;
import com.mycompany.myapp.repository.OrderEventRepository;
import com.mycompany.myapp.repository.ShipmentOrderRepository;
import com.mycompany.myapp.repository.StaffProfileRepository;
import com.mycompany.myapp.security.SecurityUtils;
import com.mycompany.myapp.security.StaffAccessService;
import com.mycompany.myapp.service.dto.inventory.InventoryCheckDTO;
import com.mycompany.myapp.web.rest.errors.BadRequestAlertException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Phiên kiểm kho dùng chung trong một VP: nhiều nhân viên quét song song vào cùng phiên OPEN,
 * bất kỳ ai cũng chốt được; phiên còn mở qua ngày (giờ VN) tự đóng.
 */
@Service
@Transactional
public class InventoryCheckSessionService {

    static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final String ENTITY = "inventoryCheck";

    private static final List<String> WAREHOUSE_IN_ACTIONS = List.of("WAREHOUSE_RECEIVE", "WH_IN", "CONFIRM", "CREATE", "TRANSIT_HUB_IN");
    private static final List<String> DEST_WAREHOUSE_IN_ACTIONS = List.of("SCAN_IN", "HUB_IN", "DEST_WH_IN");
    private static final List<String> STOCK_ENTRY_ACTIONS = java.util.stream.Stream.concat(
        WAREHOUSE_IN_ACTIONS.stream(),
        DEST_WAREHOUSE_IN_ACTIONS.stream()
    ).toList();

    private final InventoryCheckRepository checkRepository;
    private final InventoryCheckScanRepository scanRepository;
    private final ShipmentOrderRepository orderRepository;
    private final OrderEventRepository orderEventRepository;
    private final InventoryCheckService checkService;
    private final StaffAccessService staffAccessService;
    private final StaffProfileRepository staffProfileRepository;
    private final ObjectMapper objectMapper;

    public InventoryCheckSessionService(
        InventoryCheckRepository checkRepository,
        InventoryCheckScanRepository scanRepository,
        ShipmentOrderRepository orderRepository,
        OrderEventRepository orderEventRepository,
        InventoryCheckService checkService,
        StaffAccessService staffAccessService,
        StaffProfileRepository staffProfileRepository,
        ObjectMapper objectMapper
    ) {
        this.checkRepository = checkRepository;
        this.scanRepository = scanRepository;
        this.orderRepository = orderRepository;
        this.orderEventRepository = orderEventRepository;
        this.checkService = checkService;
        this.staffAccessService = staffAccessService;
        this.staffProfileRepository = staffProfileRepository;
        this.objectMapper = objectMapper;
    }

    public record ScanRequest(String orderCode, Integer packageSeq, String scannedAt) {}

    public record ScanDTO(Long id, String orderCode, int packageSeq, Instant scannedAt, String scannedBy, String scannedByName) {}

    /** {@code duplicate} = kiện đã có người quét trước; {@code scan} là lần quét đã lưu. */
    public record ScanResult(boolean duplicate, ScanDTO scan) {}

    public record ScansResponse(String status, String checkedByName, List<ScanDTO> scans) {}

    public record ExpectedOrder(String orderCode, List<Integer> packageSeqs) {}

    /**
     * Máy bấm Hoàn tất gửi tồn kho (đơn → các kiện phải có trong kho).
     * Máy chủ bỏ đơn vào kho (nhập/hạ) sau giờ mở phiên — không tính hàng người khác hạ lúc đang kiểm.
     */
    public record CompleteRequest(List<ExpectedOrder> expected) {}

    /** Phiên đang mở của VP (đã tự đóng nếu mở từ hôm trước). */
    public Optional<InventoryCheckDTO> findOpen(String officeCode) {
        String office = resolveViewOffice(officeCode);
        return currentOpen(office).map(checkService::toDto);
    }

    /** Mở phiên mới hoặc trả phiên đang mở của VP. Hai máy mở cùng lúc: unique index để lại một phiên. */
    public InventoryCheckDTO openOrJoin(String officeCode) {
        String office = checkService.resolveWriteOffice(officeCode);
        Optional<InventoryCheck> open = currentOpen(office);
        if (open.isPresent()) {
            return checkService.toDto(open.orElseThrow());
        }
        Instant now = Instant.now();
        String login = currentLogin();
        InventoryCheck row = new InventoryCheck();
        row.setOfficeCode(office);
        row.setStatus(InventoryCheckStatus.OPEN);
        row.setOpenOfficeCode(office);
        row.setOpenedAt(now);
        row.setOpenedByUsername(login);
        row.setCheckedAt(now);
        row.setCheckedByUsername(login);
        row.setCheckedByName(currentName(login));
        row.setSystemCount(0);
        row.setCheckedCount(0);
        row.setMissingCount(0);
        row.setSessionKey("inv-" + UUID.randomUUID());
        return checkService.toDto(checkRepository.saveAndFlush(row));
    }

    public ScanResult addScan(Long checkId, ScanRequest req) {
        staffAccessService.requireWritable();
        InventoryCheck check = checkRepository.findForShare(checkId).orElseThrow(InventoryCheckSessionService::notFound);
        checkService.assertCanView(check.getOfficeCode());
        requireOpen(check);
        String orderCode = req == null || req.orderCode() == null ? "" : req.orderCode().trim().toUpperCase(Locale.ROOT);
        if (orderCode.isEmpty() || orderCode.length() > 50) {
            throw new BadRequestAlertException("orderCode invalid", ENTITY, "orderCodeInvalid");
        }
        int seq = req.packageSeq() == null ? 0 : req.packageSeq();
        if (seq < 1 || seq > 999) {
            throw new BadRequestAlertException("packageSeq invalid", ENTITY, "packageSeqInvalid");
        }
        Optional<InventoryCheckScan> existing = scanRepository.findOneByCheckIdAndOrderCodeAndPackageSeq(checkId, orderCode, seq);
        if (existing.isPresent()) {
            return new ScanResult(true, toScanDtos(List.of(existing.orElseThrow())).get(0));
        }
        InventoryCheckScan scan = new InventoryCheckScan();
        scan.setCheckId(checkId);
        scan.setOrderCode(orderCode);
        scan.setPackageSeq(seq);
        scan.setScannedAt(parseInstant(req.scannedAt()));
        scan.setScannedByUsername(currentLogin());
        return new ScanResult(false, toScanDtos(List.of(scanRepository.saveAndFlush(scan))).get(0));
    }

    /** Kiện người khác vừa quét song song đã chiếm (unique) — trả lại lần quét đó. */
    @Transactional(readOnly = true)
    public ScanResult existingScan(Long checkId, ScanRequest req) {
        String orderCode = req.orderCode().trim().toUpperCase(Locale.ROOT);
        InventoryCheckScan scan = scanRepository
            .findOneByCheckIdAndOrderCodeAndPackageSeq(checkId, orderCode, req.packageSeq())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "Scan conflict"));
        return new ScanResult(true, toScanDtos(List.of(scan)).get(0));
    }

    public ScansResponse scans(Long checkId, Long sinceId) {
        InventoryCheck check = checkRepository.findById(checkId).orElseThrow(InventoryCheckSessionService::notFound);
        checkService.assertCanView(check.getOfficeCode());
        expireIfStale(check);
        List<InventoryCheckScan> rows = sinceId == null || sinceId <= 0
            ? scanRepository.findByCheckIdOrderByIdAsc(checkId)
            : scanRepository.findByCheckIdAndIdGreaterThanOrderByIdAsc(checkId, sinceId);
        return new ScansResponse(check.getStatus(), check.getCheckedByName(), toScanDtos(rows));
    }

    public InventoryCheckDTO complete(Long checkId, CompleteRequest req) {
        staffAccessService.requireWritable();
        InventoryCheck check = checkRepository.findForUpdate(checkId).orElseThrow(InventoryCheckSessionService::notFound);
        checkService.assertCanView(check.getOfficeCode());
        requireOpen(check);

        Map<String, Set<Integer>> expected = new LinkedHashMap<>();
        for (ExpectedOrder e : req == null || req.expected() == null ? List.<ExpectedOrder>of() : req.expected()) {
            if (e == null || e.orderCode() == null || e.orderCode().isBlank() || e.packageSeqs() == null) {
                continue;
            }
            Set<Integer> seqs = new HashSet<>();
            for (Integer s : e.packageSeqs()) {
                if (s != null && s > 0) seqs.add(s);
            }
            if (!seqs.isEmpty()) {
                expected.computeIfAbsent(e.orderCode().trim().toUpperCase(Locale.ROOT), k -> new HashSet<>()).addAll(seqs);
            }
        }
        dropOrdersOutsideSession(expected, check.getOpenedAt());
        Map<String, Set<Integer>> scanned = new LinkedHashMap<>();
        for (InventoryCheckScan s : scanRepository.findByCheckIdOrderByIdAsc(checkId)) {
            scanned.computeIfAbsent(s.getOrderCode().toUpperCase(Locale.ROOT), k -> new HashSet<>()).add(s.getPackageSeq());
        }
        Tally t = tally(expected, scanned);

        String login = currentLogin();
        check.setStatus(InventoryCheckStatus.COMPLETED);
        check.setOpenOfficeCode(null);
        check.setCheckedAt(Instant.now());
        check.setCheckedByUsername(login);
        check.setCheckedByName(currentName(login));
        check.setSystemCount(expected.size());
        check.setCheckedCount(scanned.size());
        check.setMissingCount(t.missingCodes().size());
        check.setSystemPkgCount(t.systemPkg());
        check.setCheckedPkgCount(t.checkedPkg());
        check.setMissingPkgCount(t.systemPkg() - t.checkedPkg());
        check.setExtraPkgCount(t.extraPkg());
        check.setSystemCodesJson(toJson(new ArrayList<>(expected.keySet())));
        check.setScannedCodesJson(toJson(new ArrayList<>(scanned.keySet())));
        check.setMissingCodesJson(toJson(t.missingCodes()));
        return checkService.toDto(checkRepository.save(check));
    }

    /**
     * Bỏ đơn không thuộc phiên: vào kho (nhập gửi / hạ nhận) sau giờ mở,
     * tạo sau giờ mở, hoặc chưa có hàng trong kho.
     */
    private void dropOrdersOutsideSession(Map<String, Set<Integer>> expected, Instant openedAt) {
        if (expected.isEmpty()) {
            return;
        }
        List<ShipmentOrder> orders = orderRepository.findWithOfficesByOrderCodeIn(expected.keySet());
        if (orders == null) {
            return;
        }
        Map<Long, Instant> stockEnteredAt = stockEnteredAtByOrderId(
            orders.stream().map(ShipmentOrder::getId).filter(java.util.Objects::nonNull).toList()
        );
        for (ShipmentOrder order : orders) {
            if (order.getOrderCode() == null) {
                continue;
            }
            String code = order.getOrderCode().trim().toUpperCase(Locale.ROOT);
            Instant entered = stockEnteredAt.get(order.getId());
            if (entered == null) {
                entered = order.getPickedUpAt() != null ? order.getPickedUpAt() : order.getCreatedAt();
            }
            boolean afterOpen = openedAt != null && entered != null && entered.isAfter(openedAt);
            if (afterOpen || !inWarehouseStock(order)) {
                expected.remove(code);
            }
        }
    }

    /**
     * orderId → lần vào kho sớm nhất (min các mốc WH_IN / DEST_WH_IN…).
     * Đơn đã nhập kho gửi trước phiên rồi hạ nhận trong lúc kiểm vẫn giữ (mốc sớm ≤ mở phiên).
     */
    private Map<Long, Instant> stockEnteredAtByOrderId(List<Long> orderIds) {
        if (orderIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, Instant> out = new HashMap<>();
        for (Object[] row : orderEventRepository.latestEventAtByOrderIdsAndAction(orderIds, STOCK_ENTRY_ACTIONS)) {
            if (row[0] == null || row[2] == null) {
                continue;
            }
            Long id = (Long) row[0];
            Instant at = (Instant) row[2];
            Instant prev = out.get(id);
            if (prev == null || at.isBefore(prev)) {
                out.put(id, at);
            }
        }
        return out;
    }

    /**
     * Hàng đang nằm ở Nhập kho gửi, Đợi trung chuyển hoặc Nhập kho giao.
     * Đơn tạo tại quầy (không lấy tận nơi, không QR mang đến) coi là đã có hàng.
     */
    static boolean inWarehouseStock(ShipmentOrder order) {
        ForwardStage stage = order.getForwardStage();
        if (
            stage == ForwardStage.WH_IN ||
            stage == ForwardStage.TRANSFER_PENDING ||
            stage == ForwardStage.TRANSFERRING ||
            stage == ForwardStage.DEST_WH_IN
        ) {
            return true;
        }
        if (stage == ForwardStage.PICKED) {
            return order.getPickedUpAt() != null;
        }
        if (stage == ForwardStage.DELIVERING || stage == ForwardStage.FAILED || stage == ForwardStage.REDELIVER_WAIT) {
            return false;
        }
        OrderStatus status = order.getStatus();
        if (status == OrderStatus.WAITING || status == OrderStatus.AT_DEST || status == OrderStatus.IN_TRANSIT) {
            return true;
        }
        if (
            status == OrderStatus.OUT_FOR_DELIVERY ||
            status == OrderStatus.DELIVERED ||
            status == OrderStatus.CANCELLED ||
            status == OrderStatus.RETURNED ||
            status == OrderStatus.FAILED_DELIVERY ||
            status == OrderStatus.DRAFT
        ) {
            return false;
        }
        if (order.getPickedUpAt() != null) {
            return true;
        }
        if (Boolean.TRUE.equals(order.getHomePickup()) || Boolean.TRUE.equals(order.getQrDropOff())) {
            return false;
        }
        return status == OrderStatus.CONFIRMED;
    }

    record Tally(int systemPkg, int checkedPkg, int extraPkg, List<String> missingCodes) {}

    static Tally tally(Map<String, Set<Integer>> expected, Map<String, Set<Integer>> scanned) {
        int systemPkg = 0;
        int checkedPkg = 0;
        int extraPkg = 0;
        List<String> missing = new ArrayList<>();
        for (Map.Entry<String, Set<Integer>> e : expected.entrySet()) {
            Set<Integer> hit = scanned.getOrDefault(e.getKey(), Set.of());
            int done = (int) e.getValue().stream().filter(hit::contains).count();
            systemPkg += e.getValue().size();
            checkedPkg += done;
            if (done < e.getValue().size()) missing.add(e.getKey());
        }
        for (Map.Entry<String, Set<Integer>> s : scanned.entrySet()) {
            Set<Integer> exp = expected.getOrDefault(s.getKey(), Set.of());
            extraPkg += (int) s.getValue().stream().filter(seq -> !exp.contains(seq)).count();
        }
        return new Tally(systemPkg, checkedPkg, extraPkg, missing);
    }

    /** Chỉ mở lại biên bản phiên dùng chung mới nhất của VP, hoàn tất trong hôm nay, khi VP chưa có phiên mở. */
    public InventoryCheckDTO reopen(Long checkId) {
        staffAccessService.requireWritable();
        InventoryCheck check = checkRepository.findForUpdate(checkId).orElseThrow(InventoryCheckSessionService::notFound);
        checkService.assertCanView(check.getOfficeCode());
        if (!InventoryCheckStatus.COMPLETED.equals(check.getStatus()) || check.getOpenedAt() == null) {
            throw new BadRequestAlertException("Chỉ mở lại được phiên kiểm kho đã hoàn tất", ENTITY, "reopenNotAllowed");
        }
        String office = check.getOfficeCode().toUpperCase(Locale.ROOT);
        Long latestId = checkRepository
            .findFirstByOfficeCodeIgnoreCaseAndStatusOrderByCheckedAtDesc(office, InventoryCheckStatus.COMPLETED)
            .map(InventoryCheck::getId)
            .orElse(null);
        if (!check.getId().equals(latestId)) {
            throw new BadRequestAlertException("Chỉ mở lại được phiên hoàn tất mới nhất của VP", ENTITY, "reopenNotLatest");
        }
        if (!vnDay(check.getCheckedAt()).equals(vnDay(Instant.now()))) {
            throw new BadRequestAlertException("Chỉ mở lại được phiên hoàn tất trong hôm nay", ENTITY, "reopenNotToday");
        }
        if (currentOpen(office).isPresent()) {
            throw new BadRequestAlertException("VP đang có phiên kiểm kho mở", ENTITY, "officeHasOpen");
        }
        check.setStatus(InventoryCheckStatus.OPEN);
        check.setOpenOfficeCode(office);
        check.setReopenedAt(Instant.now());
        check.setReopenedByUsername(currentLogin());
        return checkService.toDto(checkRepository.saveAndFlush(check));
    }

    /** Phòng khi job 23:59 bị lỡ (server khởi động lại): phiên mở từ hôm trước không quét/chốt tiếp được. Ngày mở lấy theo lần mở lại nếu có; phiên mở lại quá hạn trở về biên bản đã chốt trước đó. */
    boolean expireIfStale(InventoryCheck c) {
        return expireIfStale(c, Instant.now());
    }

    boolean expireIfStale(InventoryCheck c, Instant now) {
        if (!InventoryCheckStatus.OPEN.equals(c.getStatus())) {
            return false;
        }
        Instant since = c.getReopenedAt() != null ? c.getReopenedAt() : c.getOpenedAt();
        if (since == null || vnDay(since).equals(vnDay(now))) {
            return false;
        }
        c.setStatus(c.getReopenedAt() != null ? InventoryCheckStatus.COMPLETED : InventoryCheckStatus.ABANDONED);
        c.setOpenOfficeCode(null);
        checkRepository.saveAndFlush(c);
        return true;
    }

    /** 23:59 đóng mọi phiên còn mở trong ngày. */
    public int closeAllOpen() {
        int n = 0;
        for (InventoryCheck c : checkRepository.findByStatus(InventoryCheckStatus.OPEN)) {
            c.setStatus(c.getReopenedAt() != null ? InventoryCheckStatus.COMPLETED : InventoryCheckStatus.ABANDONED);
            c.setOpenOfficeCode(null);
            checkRepository.save(c);
            n++;
        }
        checkRepository.flush();
        return n;
    }

    private Optional<InventoryCheck> currentOpen(String office) {
        Optional<InventoryCheck> open = checkRepository.findOneByOpenOfficeCode(office);
        if (open.isPresent() && expireIfStale(open.orElseThrow())) {
            return Optional.empty();
        }
        return open;
    }

    private void requireOpen(InventoryCheck check) {
        if (expireIfStale(check) || !InventoryCheckStatus.OPEN.equals(check.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Phiên kiểm kho đã đóng");
        }
    }

    private String resolveViewOffice(String officeCode) {
        String scoped = staffAccessService.scopedOfficeCode().orElse(null);
        String office = scoped != null && !scoped.isBlank() ? scoped : officeCode;
        if (office == null || office.isBlank()) {
            throw new BadRequestAlertException("officeCode required", ENTITY, "officeRequired");
        }
        return office.trim().toUpperCase(Locale.ROOT);
    }

    private List<ScanDTO> toScanDtos(List<InventoryCheckScan> rows) {
        Map<String, Optional<StaffProfile>> staff = new HashMap<>();
        return rows
            .stream()
            .map(s -> {
                String login = s.getScannedByUsername();
                String name = staff
                    .computeIfAbsent(login.toLowerCase(Locale.ROOT), staffProfileRepository::findOneByUserLoginIgnoreCase)
                    .map(StaffProfile::getDisplayName)
                    .filter(n -> !n.isBlank())
                    .orElse(login);
                return new ScanDTO(s.getId(), s.getOrderCode(), s.getPackageSeq(), s.getScannedAt(), login, name);
            })
            .toList();
    }

    private String currentLogin() {
        return SecurityUtils.getCurrentUserLogin().orElse("system");
    }

    private String currentName(String login) {
        return staffAccessService.current().map(StaffProfile::getDisplayName).filter(n -> n != null && !n.isBlank()).orElse(login);
    }

    static LocalDate vnDay(Instant at) {
        return at.atZone(VN).toLocalDate();
    }

    private static Instant parseInstant(String raw) {
        if (raw == null || raw.isBlank()) return Instant.now();
        try {
            Instant at = Instant.parse(raw.trim());
            return at.isAfter(Instant.now()) ? Instant.now() : at;
        } catch (Exception e) {
            return Instant.now();
        }
    }

    private String toJson(List<String> codes) {
        try {
            return objectMapper.writeValueAsString(codes);
        } catch (Exception e) {
            return "[]";
        }
    }

    private static ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Inventory check not found");
    }
}
