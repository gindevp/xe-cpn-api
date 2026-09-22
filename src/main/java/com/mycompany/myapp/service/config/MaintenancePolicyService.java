package com.mycompany.myapp.service.config;

import com.mycompany.myapp.domain.MaintenancePolicy;
import com.mycompany.myapp.repository.MaintenancePolicyRepository;
import com.mycompany.myapp.service.dto.MaintenancePolicyDTO;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Singleton config: hàng đầu tiên thắng; bảng rỗng → mặc định tắt bảo trì (không chặn ai).
 */
@Service
@Transactional
public class MaintenancePolicyService {

    /** Giới hạn data-URL ảnh (ký tự) — cùng bậc với POD. */
    public static final int MAX_IMAGE_CHARS = 2_000_000;

    private final MaintenancePolicyRepository repository;

    public MaintenancePolicyService(MaintenancePolicyRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public MaintenancePolicyDTO getPolicy() {
        return toDto(repository.findAll().stream().findFirst().orElseGet(MaintenancePolicyService::defaultPolicy));
    }

    /** PUT thay toàn bộ form admin (kể cả null imageUrl = xóa ảnh). */
    public MaintenancePolicyDTO putPolicy(MaintenancePolicyDTO incoming) {
        MaintenancePolicy current = repository.findAll().stream().findFirst().orElseGet(MaintenancePolicyService::defaultPolicy);
        current.setEnabled(Boolean.TRUE.equals(incoming.getEnabled()));
        current.setBlockAll(Boolean.TRUE.equals(incoming.getBlockAll()));
        current.setBlockAppStaff(Boolean.TRUE.equals(incoming.getBlockAppStaff()));
        current.setBlockAppCustomer(Boolean.TRUE.equals(incoming.getBlockAppCustomer()));
        current.setBlockWebStaff(Boolean.TRUE.equals(incoming.getBlockWebStaff()));
        current.setBlockWebCustomer(Boolean.TRUE.equals(incoming.getBlockWebCustomer()));
        current.setTitle(trimToNull(incoming.getTitle(), 200));
        current.setMessage(trimToNull(incoming.getMessage(), 2000));
        String image = incoming.getImageUrl();
        if (image != null && image.length() > MAX_IMAGE_CHARS) {
            throw new IllegalArgumentException("Ảnh bảo trì quá lớn (tối đa ~2MB data-URL)");
        }
        current.setImageUrl(image == null || image.isBlank() ? null : image.trim());
        current.setUpdatedAt(Instant.now());
        return toDto(repository.save(current));
    }

    private static MaintenancePolicy defaultPolicy() {
        MaintenancePolicy p = new MaintenancePolicy();
        p.setEnabled(false);
        p.setBlockAll(false);
        p.setBlockAppStaff(false);
        p.setBlockAppCustomer(false);
        p.setBlockWebStaff(false);
        p.setBlockWebCustomer(false);
        p.setTitle("Hệ thống đang bảo trì");
        p.setMessage("Vui lòng quay lại sau. Xin cảm ơn.");
        p.setUpdatedAt(Instant.now());
        return p;
    }

    private static MaintenancePolicyDTO toDto(MaintenancePolicy p) {
        MaintenancePolicyDTO dto = new MaintenancePolicyDTO();
        dto.setEnabled(Boolean.TRUE.equals(p.getEnabled()));
        dto.setBlockAll(Boolean.TRUE.equals(p.getBlockAll()));
        dto.setBlockAppStaff(Boolean.TRUE.equals(p.getBlockAppStaff()));
        dto.setBlockAppCustomer(Boolean.TRUE.equals(p.getBlockAppCustomer()));
        dto.setBlockWebStaff(Boolean.TRUE.equals(p.getBlockWebStaff()));
        dto.setBlockWebCustomer(Boolean.TRUE.equals(p.getBlockWebCustomer()));
        dto.setTitle(p.getTitle());
        dto.setMessage(p.getMessage());
        dto.setImageUrl(p.getImageUrl());
        return dto;
    }

    private static String trimToNull(String raw, int max) {
        if (raw == null) {
            return null;
        }
        String t = raw.trim();
        if (t.isEmpty()) {
            return null;
        }
        return t.length() <= max ? t : t.substring(0, max);
    }
}
