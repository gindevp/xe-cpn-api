package com.mycompany.myapp.service.config;

import com.mycompany.myapp.domain.Office;
import com.mycompany.myapp.domain.TransitHubPolicy;
import com.mycompany.myapp.repository.OfficeRepository;
import com.mycompany.myapp.repository.TransitHubPolicyRepository;
import com.mycompany.myapp.service.OfficeItineraryPoints;
import com.mycompany.myapp.service.dto.TransitHubPolicyDTO;
import java.text.Normalizer;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
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

    /** Tuyến tỉnh đi tỉnh: VP trung chuyển theo cấu hình; không phải tuyến tỉnh–tỉnh hoặc chưa có VP phù hợp → empty. */
    @Transactional(readOnly = true)
    public Optional<Office> resolveHub(Office from, Office to) {
        if (from == null || to == null || from.getId() == null || to.getId() == null || from.getId().equals(to.getId())) {
            return Optional.empty();
        }
        if (!isProvinceOffice(from) || !isProvinceOffice(to)) {
            return Optional.empty();
        }
        TransitHubPolicy row = current();
        Set<String> group = new LinkedHashSet<>(
            Arrays.asList(row.getGroupPoints() == null ? new String[0] : row.getGroupPoints().split(","))
        );
        boolean grouped = inPointGroup(from, group) && inPointGroup(to, group);
        Optional<Office> hub = grouped
            ? officeByHint(row.getGroupHubOfficeCode(), "72", "trannhantong")
            : officeByHint(row.getDefaultHubOfficeCode(), "31a", "nguyenquoctri");
        return hub.filter(h -> !h.getId().equals(from.getId()) && !h.getId().equals(to.getId()));
    }

    static boolean isProvinceOffice(Office office) {
        List<String> points = OfficeItineraryPoints.pointsOf(office);
        if (points.isEmpty()) return false;
        return (
            points.stream().anyMatch(OfficeItineraryPoints.PROVINCE::containsKey) &&
            points.stream().noneMatch(OfficeItineraryPoints.HANOI::containsKey)
        );
    }

    private static boolean inPointGroup(Office office, Set<String> group) {
        List<String> points = OfficeItineraryPoints.pointsOf(office).stream().filter(OfficeItineraryPoints.PROVINCE::containsKey).toList();
        return !points.isEmpty() && group.containsAll(points);
    }

    private Optional<Office> officeByHint(String code, String... hints) {
        if (code != null && !code.isBlank()) {
            Optional<Office> byCode = officeRepository.findOneByCodeIgnoreCase(code.trim());
            if (byCode.isPresent()) return byCode;
        }
        return officeRepository
            .findAll()
            .stream()
            .filter(o -> {
                String blob = foldKey(
                    (o.getName() == null ? "" : o.getName()) + " " + (o.getAddress() == null ? "" : o.getAddress()) + " " + o.getCode()
                );
                return Arrays.stream(hints).allMatch(blob::contains);
            })
            .findFirst();
    }

    static String foldKey(String raw) {
        return Normalizer.normalize(raw == null ? "" : raw, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "")
            .replace('Đ', 'D')
            .replace('đ', 'd')
            .toLowerCase()
            .replaceAll("[^a-z0-9]+", "");
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
