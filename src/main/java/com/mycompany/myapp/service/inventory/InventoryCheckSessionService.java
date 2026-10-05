package com.mycompany.myapp.service.inventory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.myapp.domain.InventoryCheck;
import com.mycompany.myapp.domain.InventoryCheckScan;
import com.mycompany.myapp.domain.InventoryCheckStatus;
import com.mycompany.myapp.domain.StaffProfile;
import com.mycompany.myapp.repository.InventoryCheckRepository;
import com.mycompany.myapp.repository.InventoryCheckScanRepository;
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

    private final InventoryCheckRepository checkRepository;
    private final InventoryCheckScanRepository scanRepository;
    private final InventoryCheckService checkService;
    private final StaffAccessService staffAccessService;
    private final StaffProfileRepository staffProfileRepository;
    private final ObjectMapper objectMapper;

    public InventoryCheckSessionService(
        InventoryCheckRepository checkRepository,
        InventoryCheckScanRepository scanRepository,
        InventoryCheckService checkService,
        StaffAccessService staffAccessService,
        StaffProfileRepository staffProfileRepository,
        ObjectMapper objectMapper
    ) {
        this.checkRepository = checkRepository;
        this.scanRepository = scanRepository;
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

    /** Máy bấm Hoàn tất gửi tồn kho mới nhất (đơn → các kiện phải có trong kho). */
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
