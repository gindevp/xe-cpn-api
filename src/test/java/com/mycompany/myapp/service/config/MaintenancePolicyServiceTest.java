package com.mycompany.myapp.service.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.mycompany.myapp.domain.MaintenancePolicy;
import com.mycompany.myapp.repository.MaintenancePolicyRepository;
import com.mycompany.myapp.service.dto.MaintenancePolicyDTO;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MaintenancePolicyServiceTest {

    @Mock
    private MaintenancePolicyRepository repository;

    private MaintenancePolicyService service;

    @BeforeEach
    void setUp() {
        service = new MaintenancePolicyService(repository);
    }

    @Test
    void emptyTableReturnsDisabledDefault() {
        when(repository.findAll()).thenReturn(List.of());

        MaintenancePolicyDTO dto = service.getPolicy();

        assertThat(dto.getEnabled()).isFalse();
        assertThat(dto.getBlockAll()).isFalse();
        assertThat(dto.getBlockAppStaff()).isFalse();
        assertThat(dto.getBlockWebCustomer()).isFalse();
    }

    @Test
    void putOnEmptyTableCreatesRow() {
        when(repository.findAll()).thenReturn(List.of());
        when(repository.save(any(MaintenancePolicy.class))).thenAnswer(inv -> inv.getArgument(0));

        MaintenancePolicyDTO body = new MaintenancePolicyDTO();
        body.setEnabled(true);
        body.setBlockAll(true);
        body.setTitle("Bảo trì");
        body.setMessage("Quay lại sau");

        MaintenancePolicyDTO saved = service.putPolicy(body);

        assertThat(saved.getEnabled()).isTrue();
        assertThat(saved.getBlockAll()).isTrue();
        assertThat(saved.getTitle()).isEqualTo("Bảo trì");
    }

    @Test
    void putClearsImageWhenBlank() {
        MaintenancePolicy existing = new MaintenancePolicy();
        existing.setId(1L);
        existing.setEnabled(true);
        existing.setImageUrl("data:image/jpeg;base64,abc");
        when(repository.findAll()).thenReturn(List.of(existing));
        when(repository.save(any(MaintenancePolicy.class))).thenAnswer(inv -> inv.getArgument(0));

        MaintenancePolicyDTO body = new MaintenancePolicyDTO();
        body.setEnabled(false);
        body.setImageUrl("  ");

        MaintenancePolicyDTO saved = service.putPolicy(body);

        assertThat(saved.getEnabled()).isFalse();
        assertThat(saved.getImageUrl()).isNull();
    }

    @Test
    void putRejectsOversizedImage() {
        when(repository.findAll()).thenReturn(List.of());
        MaintenancePolicyDTO body = new MaintenancePolicyDTO();
        body.setEnabled(true);
        body.setImageUrl("x".repeat(MaintenancePolicyService.MAX_IMAGE_CHARS + 1));

        assertThatThrownBy(() -> service.putPolicy(body)).isInstanceOf(IllegalArgumentException.class);
    }
}
