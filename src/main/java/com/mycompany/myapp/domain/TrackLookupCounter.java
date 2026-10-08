package com.mycompany.myapp.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.time.LocalDate;

@Entity
@Table(
    name = "track_lookup_counter",
    uniqueConstraints = @UniqueConstraint(name = "ux_track_lookup_counter_device_day", columnNames = { "device_key", "day_vn" })
)
public class TrackLookupCounter implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Size(max = 80)
    @Column(name = "device_key", length = 80, nullable = false)
    private String deviceKey;

    @NotNull
    @Column(name = "day_vn", nullable = false)
    private LocalDate dayVn;

    @NotNull
    @Column(name = "hits", nullable = false)
    private Integer hits = 0;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDeviceKey() {
        return deviceKey;
    }

    public void setDeviceKey(String deviceKey) {
        this.deviceKey = deviceKey;
    }

    public LocalDate getDayVn() {
        return dayVn;
    }

    public void setDayVn(LocalDate dayVn) {
        this.dayVn = dayVn;
    }

    public Integer getHits() {
        return hits;
    }

    public void setHits(Integer hits) {
        this.hits = hits;
    }
}
