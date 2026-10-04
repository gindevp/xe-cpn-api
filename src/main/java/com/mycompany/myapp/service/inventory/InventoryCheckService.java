package com.mycompany.myapp.service.inventory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.myapp.domain.InventoryCheck;
import com.mycompany.myapp.domain.InventoryCheckPhoto;
import com.mycompany.myapp.domain.StaffProfile;
import com.mycompany.myapp.repository.InventoryCheckPhotoRepository;
import com.mycompany.myapp.repository.InventoryCheckRepository;
import com.mycompany.myapp.repository.OfficeRepository;
import com.mycompany.myapp.security.SecurityUtils;
import com.mycompany.myapp.security.StaffAccessService;
import com.mycompany.myapp.service.dto.inventory.CreateInventoryCheckRequest;
import com.mycompany.myapp.service.dto.inventory.InventoryCheckDTO;
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

    public InventoryCheckService(
        InventoryCheckRepository inventoryCheckRepository,
        InventoryCheckPhotoRepository photoRepository,
        OfficeRepository officeRepository,
        StaffAccessService staffAccessService,
        ObjectMapper objectMapper
    ) {
        this.inventoryCheckRepository = inventoryCheckRepository;
        this.photoRepository = photoRepository;
        this.officeRepository = officeRepository;
        this.staffAccessService = staffAccessService;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<InventoryCheckDTO> list(String officeCode) {
        String scoped = staffAccessService.scopedOfficeCode().orElse(null);
        String office = officeCode != null && !officeCode.isBlank() ? officeCode.trim().toUpperCase(Locale.ROOT) : null;
        if (scoped != null) {
            office = scoped;
        }
        List<InventoryCheck> rows;
        if (office == null || office.isBlank()) {
            rows = inventoryCheckRepository.findAllByOrderByCheckedAtDesc();
        } else {
            rows = inventoryCheckRepository.findByOfficeCodeIgnoreCaseOrderByCheckedAtDesc(office);
        }
        return rows.stream().limit(100).map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public InventoryCheckDTO get(Long id) {
        InventoryCheck row = inventoryCheckRepository
            .findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Inventory check not found"));
        assertCanView(row.getOfficeCode());
        return toDto(row);
    }

    public InventoryCheckDTO create(CreateInventoryCheckRequest req) {
        staffAccessService.requireWritable();
        String scoped = staffAccessService.scopedOfficeCode().orElse(null);
        String requested = req != null && req.getOfficeCode() != null ? req.getOfficeCode().trim() : "";
        // NV bó VP → luôn ghi theo VP tài khoản; AD/ALL dùng office gửi lên.
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
        office = office.toUpperCase(Locale.ROOT);

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

    public record PhotoOrderCount(String orderCode, long count) {}

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
        row.setPhotoUrl(photo);
        row.setCapturedAt(parseInstant(req.capturedAt()));
        row.setCapturedByUsername(SecurityUtils.getCurrentUserLogin().orElse("system"));
        photoRepository.save(row);
    }

    @Transactional(readOnly = true)
    public List<PhotoOrderCount> photoOrders(Long checkId) {
        InventoryCheck check = requireViewable(checkId);
        if (check.getSessionKey() == null) {
            return List.of();
        }
        return photoRepository
            .countByOrder(check.getSessionKey())
            .stream()
            .map(r -> new PhotoOrderCount((String) r[0], ((Number) r[1]).longValue()))
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
            .map(p -> new PhotoDTO(p.getId(), p.getPackageSeq(), p.getPhotoUrl(), p.getCapturedAt(), p.getCapturedByUsername()))
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

    private void assertCanView(String officeCode) {
        String scoped = staffAccessService.scopedOfficeCode().orElse(null);
        if (scoped != null && !scoped.equalsIgnoreCase(officeCode)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Office scope denied");
        }
    }

    private InventoryCheckDTO toDto(InventoryCheck row) {
        InventoryCheckDTO dto = new InventoryCheckDTO();
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
