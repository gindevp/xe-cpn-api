package com.mycompany.myapp.service.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.mycompany.myapp.repository.IntegrationConfigRepository;
import java.util.List;
import org.junit.jupiter.api.Test;

class StoredMediaTest {

    @Test
    void storeKeepsDataUrlWhenMinioIsNotConfigured() {
        IntegrationConfigRepository repo = mock(IntegrationConfigRepository.class);
        when(repo.findAll()).thenReturn(List.of());
        StoredMedia media = new StoredMedia(repo, "", "", "", "", "", "test-secret");
        String photo = "data:image/png;base64,AAAA";
        assertThat(media.store(photo, "pod")).isEqualTo(photo);
        assertThat(media.expose(photo)).isEqualTo(photo);
        assertThat(media.configured()).isFalse();
    }

    @Test
    void expiredMediaLinkIsStoredAsKeyAndResignedOnRead() {
        IntegrationConfigRepository repo = mock(IntegrationConfigRepository.class);
        when(repo.findAll()).thenReturn(List.of());
        StoredMedia media = new StoredMedia(repo, "", "", "", "", "", "test-secret");
        String expired =
            "https://xe-cpn-api-production.up.railway.app/api/media/receipt/b937fb88-3128-443e-a90e-595dde7babf5.jpg?e=1&s=dead";
        assertThat(media.store(expired, "receipt")).isEqualTo("minio:receipt/b937fb88-3128-443e-a90e-595dde7babf5.jpg");
        String fresh = media.expose(expired);
        assertThat(fresh).startsWith("/api/media/receipt/b937fb88-3128-443e-a90e-595dde7babf5.jpg?s=");
        assertThat(fresh).doesNotContain("e=");
        assertThat(media.expose("https://example.com/other.jpg")).isEqualTo("https://example.com/other.jpg");
    }
}
