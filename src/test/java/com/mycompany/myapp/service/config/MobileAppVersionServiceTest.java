package com.mycompany.myapp.service.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.mycompany.myapp.domain.MobileAppVersionPolicy;
import com.mycompany.myapp.repository.MobileAppVersionPolicyRepository;
import com.mycompany.myapp.service.dto.MobileAppVersionDTO;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MobileAppVersionServiceTest {

    @Mock
    private MobileAppVersionPolicyRepository repository;

    private MobileAppVersionService service;

    @BeforeEach
    void setUp() {
        service = new MobileAppVersionService(repository);
    }

    /** Bảng rỗng (môi trường mới, PROD chưa cấu hình) không được chặn ai. */
    @Test
    void emptyTableReturnsNonBlockingDefault() {
        when(repository.findAll()).thenReturn(List.of());

        MobileAppVersionDTO dto = service.getPolicy();

        assertThat(dto.getMinimumVersion()).isEqualTo("1.0.0");
        assertThat(dto.getMinimumAndroidVersionCode()).isNull();
        assertThat(dto.getMandatoryUpdateEnabled()).isTrue();
    }

    @Test
    void getReturnsFirstRow() {
        when(repository.findAll()).thenReturn(List.of(row("1.40.0", 40, false)));

        MobileAppVersionDTO dto = service.getPolicy();

        assertThat(dto.getMinimumVersion()).isEqualTo("1.40.0");
        assertThat(dto.getMinimumAndroidVersionCode()).isEqualTo(40);
        assertThat(dto.getMandatoryUpdateEnabled()).isFalse();
    }

    /** PUT thay toàn bộ: bỏ trống versionCode phải xóa ràng buộc cũ, không merge kiểu PATCH. */
    @Test
    void putClearsAndroidVersionCodeWhenNull() {
        when(repository.findAll()).thenReturn(List.of(row("1.40.0", 40, true)));
        when(repository.save(any(MobileAppVersionPolicy.class))).thenAnswer(inv -> inv.getArgument(0));

        MobileAppVersionDTO body = new MobileAppVersionDTO();
        body.setMinimumVersion(" 1.41.0 ");
        body.setMinimumAndroidVersionCode(null);
        body.setMandatoryUpdateEnabled(null);

        MobileAppVersionDTO saved = service.putPolicy(body);

        assertThat(saved.getMinimumVersion()).isEqualTo("1.41.0");
        assertThat(saved.getMinimumAndroidVersionCode()).isNull();
        assertThat(saved.getMandatoryUpdateEnabled()).isTrue();
    }

    @Test
    void putOnEmptyTableCreatesRow() {
        when(repository.findAll()).thenReturn(List.of());
        when(repository.save(any(MobileAppVersionPolicy.class))).thenAnswer(inv -> inv.getArgument(0));

        MobileAppVersionDTO body = new MobileAppVersionDTO();
        body.setMinimumVersion("2.0.0");
        body.setMinimumAndroidVersionCode(12);
        body.setMandatoryUpdateEnabled(false);

        MobileAppVersionDTO saved = service.putPolicy(body);

        assertThat(saved.getMinimumVersion()).isEqualTo("2.0.0");
        assertThat(saved.getMinimumAndroidVersionCode()).isEqualTo(12);
        assertThat(saved.getMandatoryUpdateEnabled()).isFalse();
    }

    private static MobileAppVersionPolicy row(String version, Integer code, boolean enabled) {
        MobileAppVersionPolicy p = new MobileAppVersionPolicy();
        p.setId(1L);
        p.setMinimumVersion(version);
        p.setMinimumAndroidVersionCode(code);
        p.setMandatoryUpdateEnabled(enabled);
        return p;
    }
}
