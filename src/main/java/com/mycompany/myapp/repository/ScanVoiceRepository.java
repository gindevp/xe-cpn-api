package com.mycompany.myapp.repository;

import com.mycompany.myapp.domain.ScanVoice;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ScanVoiceRepository extends JpaRepository<ScanVoice, String> {
    /** Metadata không kèm blob — cho app so etag. */
    @Query(
        "select v.voiceType as voiceType, v.contentType as contentType, v.etag as etag, v.fileName as fileName, v.byteSize as byteSize, v.updatedAt as updatedAt from ScanVoice v"
    )
    List<ScanVoiceMeta> listMeta();

    interface ScanVoiceMeta {
        String getVoiceType();
        String getContentType();
        String getEtag();
        String getFileName();
        Integer getByteSize();
        java.time.Instant getUpdatedAt();
    }
}
