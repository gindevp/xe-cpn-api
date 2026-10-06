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
}
