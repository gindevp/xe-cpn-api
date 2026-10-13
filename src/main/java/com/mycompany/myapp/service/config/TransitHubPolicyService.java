package com.mycompany.myapp.service.config;

import com.mycompany.myapp.domain.TransitHubPolicy;
import com.mycompany.myapp.repository.OfficeRepository;
import com.mycompany.myapp.repository.TransitHubPolicyRepository;
import com.mycompany.myapp.service.dto.TransitHubPolicyDTO;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransitHubPolicyService {

    private static final Set<String> PROVINCE_POINTS = Set.of("ND", "TB", "YB", "PT", "TC", "NB", "VT");

    private final TransitHubPolicyRepository repository;
    private final OfficeRepository officeRepository;

    public TransitHubPolicyService(TransitHubPolicyRepository repository, OfficeRepository officeRepository) {
        this.repository = repository;
        this.officeRepository = officeRepository;
    }

    @Transactional(readOnly = true)
    public TransitHubPolicyDTO getPolicy() {
        return toDto(current());
    }

    @Transactional
    public TransitHubPolicyDTO putPolicy(TransitHubPolicyDTO incoming) {
        String points = normalizePoints(incoming != null ? incoming.getGroupPoints() : null);
        String groupHub = blankToNull(incoming != null ? incoming.getGroupHubOfficeCode() : null);
        String defaultHub = blankToNull(incoming != null ? incoming.getDefaultHubOfficeCode() : null);
        requireOffice(groupHub);
        requireOffice(defaultHub);
        TransitHubPolicy row = current();
        row.setGroupPoints(points);
        row.setGroupHubOfficeCode(groupHub);
        row.setDefaultHubOfficeCode(defaultHub);
        row.setUpdatedAt(Instant.now());
        return toDto(repository.save(row));
    }

    private void requireOffice(String code) {
        if (code == null) return;
        officeRepository.findOneByCodeIgnoreCase(code).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy văn phòng " + code));
    }

    private TransitHubPolicy current() {
        return repository.findAll().stream().findFirst().orElseGet(TransitHubPolicyService::defaultRow);
    }

    private static TransitHubPolicy defaultRow() {
        TransitHubPolicy row = new TransitHubPolicy();
        row.setGroupPoints("NB,ND,TB");
        return row;
    }

    static String normalizePoints(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Chọn ít nhất một điểm lộ trình trong nhóm");
        }
        LinkedHashSet<String> out = new LinkedHashSet<>();
        for (String part : raw.split("[,\\s]+")) {
            String code = part.trim().toUpperCase();
            if (code.isEmpty()) continue;
            if (!PROVINCE_POINTS.contains(code)) {
                throw new IllegalArgumentException("Điểm lộ trình không hợp lệ: " + code);
            }
            out.add(code);
        }
        if (out.isEmpty()) {
            throw new IllegalArgumentException("Chọn ít nhất một điểm lộ trình trong nhóm");
        }
        return String.join(",", out);
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }

    private static TransitHubPolicyDTO toDto(TransitHubPolicy row) {
        TransitHubPolicyDTO dto = new TransitHubPolicyDTO();
        dto.setGroupPoints(row.getGroupPoints());
        dto.setGroupHubOfficeCode(row.getGroupHubOfficeCode());
        dto.setDefaultHubOfficeCode(row.getDefaultHubOfficeCode());
        return dto;
    }
}
