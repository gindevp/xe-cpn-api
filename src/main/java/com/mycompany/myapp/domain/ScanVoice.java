package com.mycompany.myapp.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.time.Instant;

/** Giọng người tùy chỉnh khi quét trên app (ok / err). */
@Entity
@Table(name = "scan_voice")
public class ScanVoice implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Size(max = 16)
    @Column(name = "voice_type", length = 16, nullable = false)
    private String voiceType;

    @NotNull
    @Size(max = 100)
    @Column(name = "content_type", length = 100, nullable = false)
    private String contentType;

    @NotNull
    @Size(max = 64)
    @Column(name = "etag", length = 64, nullable = false)
    private String etag;

    @Size(max = 255)
    @Column(name = "file_name", length = 255)
    private String fileName;

    @NotNull
    @Column(name = "byte_size", nullable = false)
    private Integer byteSize;

    @NotNull
    @Lob
    @Column(name = "content", nullable = false)
    private byte[] content;

    @NotNull
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public String getVoiceType() {
        return voiceType;
    }

    public void setVoiceType(String voiceType) {
        this.voiceType = voiceType;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getEtag() {
        return etag;
    }

    public void setEtag(String etag) {
        this.etag = etag;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Integer getByteSize() {
        return byteSize;
    }

    public void setByteSize(Integer byteSize) {
        this.byteSize = byteSize;
    }

    public byte[] getContent() {
        return content;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
