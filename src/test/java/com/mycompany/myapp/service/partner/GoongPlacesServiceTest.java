package com.mycompany.myapp.service.partner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.myapp.domain.IntegrationConfig;
import com.mycompany.myapp.repository.IntegrationConfigRepository;
import com.mycompany.myapp.web.rest.errors.BadRequestAlertException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GoongPlacesServiceTest {

    @Mock
    private IntegrationConfigRepository integrationConfigRepository;

    private GoongPlacesService service;

    @BeforeEach
    void setUp() {
        service = new GoongPlacesService(integrationConfigRepository, new ObjectMapper(), "https://rsapi.goong.io");
    }

    @Test
    void joinAddress_prefersFormattedWhenContainsName() {
        assertThat(GoongPlacesService.joinAddress("91 Trung Kính", "91 Trung Kính, Cầu Giấy, Hà Nội")).isEqualTo(
            "91 Trung Kính, Cầu Giấy, Hà Nội"
        );
    }

    @Test
    void joinAddress_concatenatesWhenDistinct() {
        assertThat(GoongPlacesService.joinAddress("VP HN", "Ba Đình, Hà Nội")).isEqualTo("VP HN, Ba Đình, Hà Nội");
    }

    @Test
    void autocomplete_requiresApiKey() {
        when(integrationConfigRepository.findAll()).thenReturn(List.of(new IntegrationConfig()));
        assertThatThrownBy(() -> service.autocomplete("hoan kiem"))
            .isInstanceOf(BadRequestAlertException.class)
            .hasMessageContaining("Goong");
    }

    @Test
    void autocomplete_shortQueryReturnsEmpty() {
        assertThat(service.autocomplete("a")).isEmpty();
    }

    @Test
    void placeDetail_requiresPlaceId() {
        assertThatThrownBy(() -> service.placeDetail(" ")).isInstanceOf(BadRequestAlertException.class);
    }
}
