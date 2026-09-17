package com.mycompany.myapp.service.inventory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.myapp.domain.InventoryCheck;
import com.mycompany.myapp.domain.StaffProfile;
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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class InventoryCheckService {

    private static final String ENTITY = "inventoryCheck";

    private final InventoryCheckRepository inventoryCheckRepository;
    private final OfficeRepository officeRepository;
    private final StaffAccessService staffAccessService;
    private final ObjectMapper objectMapper;

    public InventoryCheckService(
        InventoryCheckRepository inventoryCheckRepository,
        OfficeRepository officeRepository,
        StaffAccessService staffAccessService,
        ObjectMapper objectMapper
    ) {
        this.inventoryCheckRepository = inventoryCheckRepository;
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
        return toDto(inventoryCheckRepository.save(row));
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
