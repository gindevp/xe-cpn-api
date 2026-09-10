package com.mycompany.myapp.service.config;

import com.mycompany.myapp.domain.MobileAppVersionPolicy;
import com.mycompany.myapp.repository.MobileAppVersionPolicyRepository;
import com.mycompany.myapp.service.dto.MobileAppVersionDTO;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Singleton config giống ConfigFacadeService: hàng đầu tiên thắng, bảng rỗng thì trả mặc định.
 * Mặc định không chặn ai (minimum = 1.0.0) để môi trường mới chưa cấu hình không nhốt user.
 */
@Service
@Transactional
public class MobileAppVersionService {

    /** Bằng expo.version của bản app đầu tiên — không chặn ai cho tới khi admin nâng mức. */
    public static final String DEFAULT_MINIMUM_VERSION = "1.0.0";

    private final MobileAppVersionPolicyRepository repository;

    public MobileAppVersionService(MobileAppVersionPolicyRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public MobileAppVersionDTO getPolicy() {
        return toDto(repository.findAll().stream().findFirst().orElseGet(MobileAppVersionService::defaultPolicy));
    }

    /**
     * PUT thay toàn bộ chính sách (form admin gửi đủ 3 field). Khác với PATCH đơn hàng:
     * minimumAndroidVersionCode = null nghĩa là "bỏ ràng buộc versionCode", nên phải ghi đè chứ không bỏ qua.
     */
    public MobileAppVersionDTO putPolicy(MobileAppVersionDTO incoming) {
        MobileAppVersionPolicy current = repository.findAll().stream().findFirst().orElseGet(MobileAppVersionService::defaultPolicy);
        current.setMinimumVersion(incoming.getMinimumVersion().trim());
        current.setMinimumAndroidVersionCode(incoming.getMinimumAndroidVersionCode());
        current.setMandatoryUpdateEnabled(
            incoming.getMandatoryUpdateEnabled() == null ? Boolean.TRUE : incoming.getMandatoryUpdateEnabled()
        );
        current.setUpdatedAt(Instant.now());
        return toDto(repository.save(current));
    }

    private static MobileAppVersionPolicy defaultPolicy() {
        MobileAppVersionPolicy p = new MobileAppVersionPolicy();
        p.setMinimumVersion(DEFAULT_MINIMUM_VERSION);
        p.setMinimumAndroidVersionCode(null);
        p.setMandatoryUpdateEnabled(true);
        p.setUpdatedAt(Instant.now());
        return p;
    }

    private static MobileAppVersionDTO toDto(MobileAppVersionPolicy p) {
        MobileAppVersionDTO dto = new MobileAppVersionDTO();
        dto.setMinimumVersion(p.getMinimumVersion() == null ? DEFAULT_MINIMUM_VERSION : p.getMinimumVersion());
        dto.setMinimumAndroidVersionCode(p.getMinimumAndroidVersionCode());
        dto.setMandatoryUpdateEnabled(p.getMandatoryUpdateEnabled() == null ? Boolean.TRUE : p.getMandatoryUpdateEnabled());
        return dto;
    }
}
