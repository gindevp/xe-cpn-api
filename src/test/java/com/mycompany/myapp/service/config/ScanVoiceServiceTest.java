package com.mycompany.myapp.service.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mycompany.myapp.domain.ScanVoice;
import com.mycompany.myapp.repository.ScanVoiceRepository;
import com.mycompany.myapp.security.StaffAccessService;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ScanVoiceServiceTest {

    @Mock
    private ScanVoiceRepository repository;

    @Mock
    private StaffAccessService staffAccessService;

    private ScanVoiceService service;

    @BeforeEach
    void setUp() {
        service = new ScanVoiceService(repository, staffAccessService);
        doNothing().when(staffAccessService).requireScreenWrite(any());
        when(repository.listMeta()).thenReturn(List.of());
        when(repository.findById(any())).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void list_returnsBothTypesEvenWhenEmpty() {
        List<Map<String, Object>> rows = service.list();
        assertThat(rows).extracting(m -> m.get("type")).containsExactly("ok", "err");
        assertThat(rows).allSatisfy(m -> assertThat(m.get("configured")).isEqualTo(false));
    }

    @Test
    void upload_savesEtagAndBytes() {
        MockMultipartFile file = new MockMultipartFile("file", "ok.mp3", "audio/mpeg", "voice-bytes".getBytes(StandardCharsets.UTF_8));

        Map<String, Object> meta = service.upload("ok", file);

        ArgumentCaptor<ScanVoice> cap = ArgumentCaptor.forClass(ScanVoice.class);
        verify(repository).save(cap.capture());
        assertThat(cap.getValue().getVoiceType()).isEqualTo("ok");
        assertThat(cap.getValue().getEtag()).isNotBlank();
        assertThat(cap.getValue().getByteSize()).isEqualTo(11);
        assertThat(meta.get("configured")).isEqualTo(true);
        assertThat(meta.get("etag")).isEqualTo(cap.getValue().getEtag());
    }

    @Test
    void upload_rejectsUnknownType() {
        MockMultipartFile file = new MockMultipartFile("file", "x.mp3", "audio/mpeg", new byte[] { 1 });
        assertThatThrownBy(() -> service.upload("maybe", file)).isInstanceOf(ResponseStatusException.class);
    }
}
