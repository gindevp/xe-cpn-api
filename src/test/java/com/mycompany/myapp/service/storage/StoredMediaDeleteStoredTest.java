package com.mycompany.myapp.service.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.mycompany.myapp.repository.IntegrationConfigRepository;
import java.util.List;
import org.junit.jupiter.api.Test;

class StoredMediaDeleteStoredTest {

    @Test
    void deleteStored_skipsDataUrlAndBlank() {
        IntegrationConfigRepository repo = mock(IntegrationConfigRepository.class);
        when(repo.findAll()).thenReturn(List.of());
        StoredMedia media = new StoredMedia(repo, "", "cpn", "us-east-1", "", "", "dGVzdA==");
        assertThat(media.deleteStored(null)).isFalse();
        assertThat(media.deleteStored("")).isFalse();
        assertThat(media.deleteStored("data:image/jpeg;base64,abc")).isFalse();
        assertThat(media.deleteStored("https://example.com/x.jpg")).isFalse();
    }
}
