package com.mycompany.myapp.service.partner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mycompany.myapp.domain.IntegrationConfig;
import com.mycompany.myapp.repository.IntegrationConfigRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AhamoveTokenServiceTest {

    @Mock
    private IntegrationConfigRepository repository;

    @Mock
    private AhamoveAuthClient authClient;

    private AhamoveTokenService service;

    @BeforeEach
    void setUp() {
        service = new AhamoveTokenService(repository, authClient);
    }

    @Test
    void normalizeMobile_convertsLeadingZero() {
        assertThat(AhamoveAuthClient.normalizeMobile("0901234567")).isEqualTo("84901234567");
        assertThat(AhamoveAuthClient.normalizeMobile("+84901234567")).isEqualTo("84901234567");
        assertThat(AhamoveAuthClient.normalizeMobile("84901234567")).isEqualTo("84901234567");
        assertThat(AhamoveAuthClient.normalizeMobile("840901234567")).isEqualTo("84901234567");
        assertThat(AhamoveAuthClient.normalizeMobile("901234567")).isEqualTo("84901234567");
    }

    @Test
    void sanitizeApiKey_stripsQuotesAndBearer() {
        assertThat(AhamoveAuthClient.sanitizeApiKey("  \"abc123\"  ")).isEqualTo("abc123");
        assertThat(AhamoveAuthClient.sanitizeApiKey("Bearer abc123")).isEqualTo("abc123");
        assertThat(AhamoveAuthClient.sanitizeApiKey("")).isNull();
    }

    @Test
    void needsRefresh_whenOlderThanSevenDays() {
        IntegrationConfig cfg = new IntegrationConfig().ahamoveToken("t").ahamoveTokenFetchedAt(Instant.now().minus(8, ChronoUnit.DAYS));
        assertThat(service.needsRefresh(cfg)).isTrue();
    }

    @Test
    void needsRefresh_falseWhenFresh() {
        IntegrationConfig cfg = new IntegrationConfig().ahamoveToken("t").ahamoveTokenFetchedAt(Instant.now().minus(1, ChronoUnit.DAYS));
        assertThat(service.needsRefresh(cfg)).isFalse();
    }

    @Test
    void resolveAccessToken_refreshesWhenDue() {
        IntegrationConfig cfg = new IntegrationConfig()
            .id(1L)
            .ahamoveApiKey("key")
            .ahamoveMobile("84901234567")
            .ahamoveToken("old")
            .ahamoveTokenFetchedAt(Instant.now().minus(10, ChronoUnit.DAYS));
        when(repository.findAll()).thenReturn(List.of(cfg));
        when(authClient.fetchAccessToken("key", "84901234567")).thenReturn("new-token");
        when(repository.save(cfg)).thenReturn(cfg);

        Optional<String> token = service.resolveAccessToken();

        assertThat(token).contains("new-token");
        ArgumentCaptor<IntegrationConfig> cap = ArgumentCaptor.forClass(IntegrationConfig.class);
        verify(repository).save(cap.capture());
        assertThat(cap.getValue().getAhamoveToken()).isEqualTo("new-token");
        assertThat(cap.getValue().getAhamoveTokenFetchedAt()).isNotNull();
    }

    @Test
    void resolveAccessToken_usesLegacyTokenWithoutApiKey() {
        IntegrationConfig cfg = new IntegrationConfig().id(1L).ahamoveToken("legacy-jwt");
        when(repository.findAll()).thenReturn(List.of(cfg));

        assertThat(service.resolveAccessToken()).contains("legacy-jwt");
        verify(authClient, never()).fetchAccessToken(anyString(), anyString());
    }
}
