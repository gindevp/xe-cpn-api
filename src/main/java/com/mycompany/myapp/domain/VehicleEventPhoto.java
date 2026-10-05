package com.mycompany.myapp.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;

/** Ảnh xe chụp khi NV báo xe rời VP — 1 ảnh / lượt báo ({@link VehicleOfficeEvent}). */
@Entity
@Table(name = "vehicle_event_photo")
public class VehicleEventPhoto implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotNull
    @Column(name = "event_id", nullable = false, unique = true)
    private Long eventId;

    @NotNull
    @Lob
    @Column(name = "photo_url", nullable = false, columnDefinition = "LONGTEXT")
    private String photoUrl;

    @NotNull
    @Column(name = "captured_at", nullable = false)
    private Instant capturedAt;

    @Size(max = 50)
    @Column(name = "captured_by_username", length = 50)
    private String capturedByUsername;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getEventId() {
        return eventId;
    }

    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public Instant getCapturedAt() {
        return capturedAt;
    }

    public void setCapturedAt(Instant capturedAt) {
        this.capturedAt = capturedAt;
    }

    public String getCapturedByUsername() {
        return capturedByUsername;
    }

    public void setCapturedByUsername(String capturedByUsername) {
        this.capturedByUsername = capturedByUsername;
    }
}
