package com.mycompany.myapp.service.dto.trip;

import com.mycompany.myapp.domain.enumeration.TripStatus;
import jakarta.validation.constraints.NotNull;

public class TripTransitionRequest {

    @NotNull
    private TripStatus toStatus;

    private String detail;

    public TripStatus getToStatus() {
        return toStatus;
    }

    public void setToStatus(TripStatus toStatus) {
        this.toStatus = toStatus;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }
}
