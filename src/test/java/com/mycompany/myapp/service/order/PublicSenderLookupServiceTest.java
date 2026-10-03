package com.mycompany.myapp.service.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mycompany.myapp.repository.ShipmentOrderRepository;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class PublicSenderLookupServiceTest {

    private final ShipmentOrderRepository repo = mock(ShipmentOrderRepository.class);
    private final PublicSenderLookupService service = new PublicSenderLookupService(repo);

    @Test
    void returnsLatestSenderName() {
        when(repo.findLatestSenderNames(eq("0912345678"), any())).thenReturn(List.of(" NGUYEN VAN A "));
        Map<String, Object> r = service.senderName("+84 912.345.678", "1.1.1.1");
        assertThat(r).containsEntry("found", true).containsEntry("name", "NGUYEN VAN A");
    }

    @Test
    void notFoundWhenNoOrder() {
        when(repo.findLatestSenderNames(eq("0912345678"), any())).thenReturn(List.of());
        assertThat(service.senderName("0912345678", "1.1.1.1")).containsEntry("found", false).doesNotContainKey("name");
    }

    @Test
    void invalidPhoneSkipsQuery() {
        assertThat(service.senderName("12345", "1.1.1.1")).containsEntry("found", false);
        verify(repo, never()).findLatestSenderNames(any(), any());
    }

    @Test
    void throttlesPerIp() {
        when(repo.findLatestSenderNames(any(), any())).thenReturn(List.of());
        for (int i = 0; i < PublicSenderLookupService.MAX_PER_IP; i++) {
            service.senderName("0912345678", "2.2.2.2");
        }
        assertThatThrownBy(() -> service.senderName("0912345678", "2.2.2.2")).isInstanceOf(ResponseStatusException.class);
        assertThat(service.senderName("0912345678", "3.3.3.3")).containsEntry("found", false);
    }
}
