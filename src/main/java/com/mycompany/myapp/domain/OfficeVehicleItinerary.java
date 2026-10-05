package com.mycompany.myapp.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;

/** Lộ trình mà văn phòng báo giờ xe đến/đi (màn Báo cáo giờ xe trên app). */
@Entity
@Table(name = "office_vehicle_itinerary")
public class OfficeVehicleItinerary implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotNull
    @Column(name = "office_id", nullable = false)
    private Long officeId;

    @NotNull
    @Size(max = 50)
    @Column(name = "itinerary_code", length = 50, nullable = false)
    private String itineraryCode;

    /** Giờ đón khách tại VP = giờ xe xuất bến + số phút này (âm = sớm hơn); null = trùng giờ xuất bến. */
    @Column(name = "offset_minutes")
    private Integer offsetMinutes;

    public OfficeVehicleItinerary() {}

    public OfficeVehicleItinerary(Long officeId, String itineraryCode) {
        this.officeId = officeId;
        this.itineraryCode = itineraryCode;
    }

    public OfficeVehicleItinerary(Long officeId, String itineraryCode, Integer offsetMinutes) {
        this(officeId, itineraryCode);
        this.offsetMinutes = offsetMinutes;
    }

    public Integer getOffsetMinutes() {
        return offsetMinutes;
    }

    public void setOffsetMinutes(Integer offsetMinutes) {
        this.offsetMinutes = offsetMinutes;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getOfficeId() {
        return officeId;
    }

    public void setOfficeId(Long officeId) {
        this.officeId = officeId;
    }

    public String getItineraryCode() {
        return itineraryCode;
    }

    public void setItineraryCode(String itineraryCode) {
        this.itineraryCode = itineraryCode;
    }
}
