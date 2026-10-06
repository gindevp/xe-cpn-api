package com.mycompany.myapp.service.inventory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.myapp.domain.InventoryCheck;
import com.mycompany.myapp.domain.InventoryCheckPhoto;
import com.mycompany.myapp.domain.InventoryCheckScan;
import com.mycompany.myapp.domain.InventoryCheckStatus;
import com.mycompany.myapp.domain.StaffProfile;
import com.mycompany.myapp.repository.InventoryCheckPhotoRepository;
import com.mycompany.myapp.repository.InventoryCheckRepository;
import com.mycompany.myapp.repository.InventoryCheckScanRepository;
import com.mycompany.myapp.repository.OfficeRepository;
import com.mycompany.myapp.repository.StaffProfileRepository;
import com.mycompany.myapp.security.SecurityUtils;
import com.mycompany.myapp.security.StaffAccessService;
import com.mycompany.myapp.service.dto.inventory.CreateInventoryCheckRequest;
import com.mycompany.myapp.service.dto.inventory.InventoryCheckDTO;
import com.mycompany.myapp.service.storage.StoredMedia;
import com.mycompany.myapp.web.rest.errors.BadRequestAlertException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class InventoryCheckService {

    private static final String ENTITY = "inventoryCheck";
    static final int MAX_PHOTO_LENGTH = 4_000_000;
    private static final Pattern SESSION_KEY = Pattern.compile("[A-Za-z0-9-]{8,64}");

    private final InventoryCheckRepository inventoryCheckRepository;
    private final InventoryCheckPhotoRepository photoRepository;
    private final OfficeRepository officeRepository;
    private final StaffAccessService staffAccessService;
    private final ObjectMapper objectMapper;
    private final StaffProfileRepository staffProfileRepository;
    private final InventoryCheckScanRepository scanRepository;
    private StoredMedia storedMedia;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    void setStoredMedia(StoredMedia storedMedia) {
        this.storedMedia = storedMedia;
    }

    private String storeMedia(String value, String folder) {
        return storedMedia == null || value == null ? value : storedMedia.store(value, folder);
    }

    private String showMedia(String value) {
        return storedMedia == null || value == null ? value : storedMedia.expose(value);
    }

    public InventoryCheckService(
        InventoryCheckRepository inventoryCheckRepository,
        InventoryCheckPhotoRepository photoRepository,
        OfficeRepository officeRepository,
        StaffAccessService staffAccessService,
        ObjectMapper objectMapper,
        StaffProfileRepository staffProfileRepository,
        InventoryCheckScanRepository scanRepository
    ) {
        this.inventoryCheckRepository = inventoryCheckRepository;
        this.photoRepository = photoRepository;
        this.officeRepository = officeRepository;
        this.staffAccessService = staffAccessService;
        this.objectMapper = objectMapper;
        this.staffProfileRepository = staffProfileRepository;
        this.scanRepository = scanRepository;
    }

    /** Phiên Bỏ dở mặc định ẩn — không phải biên bản. */
    @Transactional(readOnly = true)
    public List<InventoryCheckDTO> list(String officeCode, boolean includeAbandoned) {
        String scoped = staffAccessService.scopedOfficeCode().orElse(null);
        String office = officeCode != null && !officeCode.isBlank() ? officeCode.trim().toUpperCase(Locale.ROOT) : null;
        if (scoped != null) {
            office = scoped;
        }
        List<InventoryCheck> rows;
        if (office == null || office.isBlank()) {
            rows = includeAbandoned
                ? inventoryCheckRepository.findAllByOrderByCheckedAtDesc()
                : inventoryCheckRepository.findByStatusNotOrderByCheckedAtDesc(InventoryCheckStatus.ABANDONED);
        } else {
            rows = includeAbandoned
                ? inventoryCheckRepository.findByOfficeCodeIgnoreCaseOrderByCheckedAtDesc(office)
                : inventoryCheckRepository.findByOfficeCodeIgnoreCaseAndStatusNotOrderByCheckedAtDesc(
                    office,
                    InventoryCheckStatus.ABANDONED
                );
        }
        return toDtos(rows.stream().limit(100).toList());
    }

    @Transactional(readOnly = true)
    public InventoryCheckDTO get(Long id) {
        InventoryCheck row = inventoryCheckRepository
            .findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Inventory check not found"));
        assertCanView(row.getOfficeCode());
        return toDto(row);
    }

    /** NV bó VP → luôn ghi theo VP tài khoản; AD/ALL dùng office gửi lên. */
    String resolveWriteOffice(String requestedOffice) {
        staffAccessService.requireWritable();
        String scoped = staffAccessService.scopedOfficeCode().orElse(null);
        String requested = requestedOffice != null ? requestedOffice.trim() : "";
        String office = scoped != null && !scoped.isBlank() ? scoped.trim().toUpperCase(Locale.ROOT) : requested.toUpperCase(Locale.ROOT);
        if (office.isBlank()) {
            throw new BadRequestAlertException("officeCode required", ENTITY, "officeRequired");
        }
        if (officeRepository.findOneByCodeIgnoreCase(office).isEmpty()) {
            // Vẫn lưu nếu mã trùng VP staff (tránh lệch chữ hoa/thường / VP mới chưa sync master).
            if (scoped == null || !scoped.equalsIgnoreCase(office)) {
                throw new BadRequestAlertException("Unknown office: " + office, ENTITY, "officeUnknown");
            }
        }
        return office.toUpperCase(Locale.ROOT);
    }

    public InventoryCheckDTO create(CreateInventoryCheckRequest req) {
        String office = resolveWriteOffice(req != null ? req.getOfficeCode() : null);

        List<String> system = nzList(req.getSystemCodes());
        List<String> scanned = nzList(req.getScannedCodes());
        List<String> missing = nzList(req.getMissingCodes());
        if (missing.isEmpty() && !system.isEmpty()) {
            missing = system.stream().filter(c -> scanned.stream().noneMatch(s -> s.equalsIgnoreCase(c))).toList();
        }

        InventoryCheck row = new InventoryCheck();
        row.setOfficeCode(office);
        row.setCheckedAt(parseInstant(req.getCheckedAt()));
        String login = SecurityUtils.getCurrentUserLogin().orElse("system");
        row.setCheckedByUsername(login);
        row.setCheckedByName(
            staffAccessService.current().map(StaffProfile::getDisplayName).filter(n -> n != null && !n.isBlank()).orElse(login)
        );
        row.setSystemCount(system.size());
        row.setCheckedCount(scanned.size());
        row.setMissingCount(missing.size());
        row.setSystemPkgCount(req.getSystemPkgCount());
        row.setCheckedPkgCount(req.getCheckedPkgCount());
        row.setMissingPkgCount(req.getMissingPkgCount());
        row.setExtraPkgCount(req.getExtraPkgCount());
        row.setSystemCodesJson(toJson(system));
        row.setScannedCodesJson(toJson(scanned));
        row.setMissingCodesJson(toJson(missing));
        row.setSessionKey(validSessionKeyOrNull(req.getSessionKey()));
        return toDto(inventoryCheckRepository.save(row));
    }

    public record UploadPhotoRequest(
        String sessionKey,
        String officeCode,
        String orderCode,
        Integer packageSeq,
        String photo,
        String capturedAt
    ) {}

    public record PhotoDTO(Long id, int packageSeq, String photo, Instant capturedAt, String capturedBy) {}

    /** Số ảnh + lần quét cuối (giờ, tài khoản, mã NV, họ tên) của từng đơn trong phiên. */
    public record PhotoOrderCount(
        String orderCode,
        long count,
        Instant lastScannedAt,
        String lastScannedBy,
        String lastScannedByCode,
        String lastScannedByName
    ) {}

    public record ThumbnailDTO(String orderCode, String photo) {}

    /** Ảnh kiện chụp khi quét — app gửi từng ảnh ngay lúc quét, trước khi có biên bản. */
    public void uploadPhoto(UploadPhotoRequest req) {
        staffAccessService.requireWritable();
        if (req == null) {
            throw new BadRequestAlertException("body required", ENTITY, "photoInvalid");
        }
        String sessionKey = validSessionKeyOrNull(req.sessionKey());
        if (sessionKey == null) {
            throw new BadRequestAlertException("sessionKey invalid", ENTITY, "sessionKeyInvalid");
        }
        String scoped = staffAccessService.scopedOfficeCode().orElse(null);
        String office = scoped != null && !scoped.isBlank() ? scoped : req.officeCode();
        if (office == null || office.isBlank()) {
            throw new BadRequestAlertException("officeCode required", ENTITY, "officeRequired");
        }
        String orderCode = req.orderCode() == null ? "" : req.orderCode().trim().toUpperCase(Locale.ROOT);
        if (orderCode.isEmpty() || orderCode.length() > 50) {
            throw new BadRequestAlertException("orderCode invalid", ENTITY, "orderCodeInvalid");
        }
        int seq = req.packageSeq() == null ? 0 : req.packageSeq();
        if (seq < 1 || seq > 999) {
            throw new BadRequestAlertException("packageSeq invalid", ENTITY, "packageSeqInvalid");
        }
        String photo = req.photo() == null ? "" : req.photo().trim();
        if (!photo.startsWith("data:image/") || !photo.contains(";base64,")) {
            throw new BadRequestAlertException("photo must be an image", ENTITY, "photoInvalid");
        }
        if (photo.length() > MAX_PHOTO_LENGTH) {
            throw new BadRequestAlertException("photo too large", ENTITY, "photoTooLarge");
        }
        InventoryCheckPhoto row = new InventoryCheckPhoto();
        row.setSessionKey(sessionKey);
        row.setOfficeCode(office.trim().toUpperCase(Locale.ROOT));
        row.setOrderCode(orderCode);
        row.setPackageSeq(seq);
        row.setPhotoUrl(storeMedia(photo, "inventory"));
        row.setCapturedAt(parseInstant(req.capturedAt()));
        row.setCapturedByUsername(SecurityUtils.getCurrentUserLogin().orElse("system"));
        photoRepository.save(row);
    }

    /** Phiên dùng chung lấy lần quét cuối từ bảng quét; biên bản cũ suy từ ảnh. */
    @Transactional(readOnly = true)
    public List<PhotoOrderCount> photoOrders(Long checkId) {
        InventoryCheck check = requireViewable(checkId);
        java.util.Map<String, long[]> counts = new java.util.LinkedHashMap<>();
        java.util.Map<String, Object[]> last = new java.util.HashMap<>();
        if (check.getSessionKey() != null) {
            for (Object[] r : photoRepository.scanMetaBySession(check.getSessionKey())) {
                if (r[0] == null) {
                    continue;
                }
                String code = ((String) r[0]).trim().toUpperCase(Locale.ROOT);
                counts.computeIfAbsent(code, k -> new long[1])[0]++;
                keepLatest(last, code, (Instant) r[1], (String) r[2]);
            }
        }
        List<InventoryCheckScan> scans = check.getId() == null ? List.of() : scanRepository.findByCheckIdOrderByIdAsc(check.getId());
        if (!scans.isEmpty()) {
            last.clear();
            for (InventoryCheckScan s : scans) {
                String code = s.getOrderCode().trim().toUpperCase(Locale.ROOT);
                counts.computeIfAbsent(code, k -> new long[1]);
                keepLatest(last, code, s.getScannedAt(), s.getScannedByUsername());
            }
        }
        java.util.Map<String, java.util.Optional<StaffProfile>> staff = new java.util.HashMap<>();
        List<PhotoOrderCount> out = new ArrayList<>();
        counts.forEach((code, n) -> {
            Object[] l = last.get(code);
            String by = l == null ? null : (String) l[1];
            StaffProfile sp = profile(staff, by);
            out.add(
                new PhotoOrderCount(
                    code,
                    n[0],
                    l == null ? null : (Instant) l[0],
                    by,
                    sp == null ? null : sp.getStaffCode(),
                    displayName(sp)
                )
            );
        });
        return out;
    }

    private static void keepLatest(java.util.Map<String, Object[]> last, String code, Instant at, String by) {
        Object[] prev = last.get(code);
        if (prev == null || (at != null && (prev[0] == null || at.isAfter((Instant) prev[0])))) {
            last.put(code, new Object[] { at, by });
        }
    }

    private StaffProfile profile(java.util.Map<String, java.util.Optional<StaffProfile>> cache, String login) {
        if (login == null || staffProfileRepository == null) {
            return null;
        }
        return cache.computeIfAbsent(login.toLowerCase(Locale.ROOT), staffProfileRepository::findOneByUserLoginIgnoreCase).orElse(null);
    }

    private static String displayName(StaffProfile sp) {
        return sp == null || sp.getDisplayName() == null || sp.getDisplayName().isBlank() ? null : sp.getDisplayName().trim();
    }

    /** Ảnh đầu tiên của từng đơn (ảnh đại diện trong bảng chi tiết phiên); tối đa 100 đơn một lần. */
    @Transactional(readOnly = true)
    public List<ThumbnailDTO> thumbnails(Long checkId, List<String> orderCodes) {
        InventoryCheck check = requireViewable(checkId);
        if (check.getSessionKey() == null || orderCodes == null || orderCodes.isEmpty()) {
            return List.of();
        }
        List<String> codes = orderCodes
            .stream()
            .filter(c -> c != null && !c.isBlank())
            .map(c -> c.trim().toUpperCase(Locale.ROOT))
            .distinct()
            .limit(100)
            .toList();
        if (codes.isEmpty()) {
            return List.of();
        }
        List<Long> ids = photoRepository.firstPhotoIds(check.getSessionKey(), codes);
        if (ids.isEmpty()) {
            return List.of();
        }
        return photoRepository
            .findAllById(ids)
            .stream()
            .map(p -> new ThumbnailDTO(p.getOrderCode().trim().toUpperCase(Locale.ROOT), showMedia(p.getPhotoUrl())))
            .toList();
    }

    @Transactional(readOnly = true)
    public List<PhotoDTO> photos(Long checkId, String orderCode) {
        InventoryCheck check = requireViewable(checkId);
        if (check.getSessionKey() == null || orderCode == null || orderCode.isBlank()) {
            return List.of();
        }
        return photoRepository
            .findBySessionKeyAndOrderCodeIgnoreCaseOrderByPackageSeqAscCapturedAtAsc(check.getSessionKey(), orderCode.trim())
            .stream()
            .map(p -> new PhotoDTO(p.getId(), p.getPackageSeq(), showMedia(p.getPhotoUrl()), p.getCapturedAt(), p.getCapturedByUsername()))
            .toList();
    }

    private InventoryCheck requireViewable(Long id) {
        InventoryCheck row = inventoryCheckRepository
            .findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Inventory check not found"));
        assertCanView(row.getOfficeCode());
        return row;
    }

    static String validSessionKeyOrNull(String raw) {
        if (raw == null) return null;
        String key = raw.trim();
        return SESSION_KEY.matcher(key).matches() ? key : null;
    }

    void assertCanView(String officeCode) {
        String scoped = staffAccessService.scopedOfficeCode().orElse(null);
        if (scoped != null && !scoped.equalsIgnoreCase(officeCode)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Office scope denied");
        }
    }

    InventoryCheckDTO toDto(InventoryCheck row) {
        return toDtos(List.of(row)).get(0);
    }

    List<InventoryCheckDTO> toDtos(List<InventoryCheck> rows) {
        List<Long> ids = rows.stream().map(InventoryCheck::getId).filter(java.util.Objects::nonNull).toList();
        java.util.Map<Long, List<Object[]>> byCheck = new java.util.HashMap<>();
        if (!ids.isEmpty() && scanRepository != null) {
            for (Object[] r : scanRepository.participants(ids)) {
                byCheck.computeIfAbsent((Long) r[0], k -> new ArrayList<>()).add(r);
            }
        }
        java.util.Map<String, java.util.Optional<StaffProfile>> staff = new java.util.HashMap<>();
        return rows
            .stream()
            .map(row -> {
                InventoryCheckDTO dto = baseDto(row);
                long total = 0;
                List<InventoryCheckDTO.Participant> people = new ArrayList<>();
                for (Object[] r : byCheck.getOrDefault(row.getId(), List.of())) {
                    String login = (String) r[1];
                    long n = ((Number) r[2]).longValue();
                    total += n;
                    StaffProfile sp = profile(staff, login);
                    people.add(new InventoryCheckDTO.Participant(login, displayName(sp), sp == null ? null : sp.getStaffCode(), n));
                }
                people.sort((a, b) -> Long.compare(b.scanCount(), a.scanCount()));
                dto.setScanCount(total);
                dto.setParticipants(people);
                if (row.getOpenedByUsername() != null) {
                    String name = displayName(profile(staff, row.getOpenedByUsername()));
                    dto.setOpenedByName(name != null ? name : row.getOpenedByUsername());
                }
                return dto;
            })
            .toList();
    }

    private InventoryCheckDTO baseDto(InventoryCheck row) {
        InventoryCheckDTO dto = new InventoryCheckDTO();
        dto.setStatus(row.getStatus() != null ? row.getStatus() : InventoryCheckStatus.COMPLETED);
        dto.setSessionKey(row.getSessionKey());
        dto.setOpenedAt(row.getOpenedAt());
        dto.setOpenedByUsername(row.getOpenedByUsername());
        dto.setReopenedAt(row.getReopenedAt());
        dto.setReopenedByUsername(row.getReopenedByUsername());
        dto.setId(row.getId());
        dto.setOfficeCode(row.getOfficeCode());
        dto.setCheckedAt(row.getCheckedAt());
        dto.setCheckedByUsername(row.getCheckedByUsername());
        dto.setCheckedByName(row.getCheckedByName());
        dto.setSystemCount(row.getSystemCount());
        dto.setCheckedCount(row.getCheckedCount());
        dto.setMissingCount(row.getMissingCount());
        dto.setSystemPkgCount(row.getSystemPkgCount());
        dto.setCheckedPkgCount(row.getCheckedPkgCount());
        dto.setMissingPkgCount(row.getMissingPkgCount());
        dto.setExtraPkgCount(row.getExtraPkgCount());
        dto.setSystemCodes(fromJson(row.getSystemCodesJson()));
        dto.setScannedCodes(fromJson(row.getScannedCodesJson()));
        dto.setMissingCodes(fromJson(row.getMissingCodesJson()));
        return dto;
    }

    private static List<String> nzList(List<String> in) {
        if (in == null) return new ArrayList<>();
        return in.stream().filter(s -> s != null && !s.isBlank()).map(String::trim).distinct().toList();
    }

    private Instant parseInstant(String raw) {
        if (raw == null || raw.isBlank()) return Instant.now();
        try {
            return Instant.parse(raw.trim());
        } catch (Exception e) {
            return Instant.now();
        }
    }

    private String toJson(List<String> codes) {
        try {
            return objectMapper.writeValueAsString(codes != null ? codes : List.of());
        } catch (Exception e) {
            return "[]";
        }
    }

    private List<String> fromJson(String json) {
        if (json == null || json.isBlank()) return new ArrayList<>();
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}
