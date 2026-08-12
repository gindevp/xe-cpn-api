package com.mycompany.myapp.service.dto.trip;

import com.mycompany.myapp.domain.enumeration.TripStatus;

public class TripTransitionResponse {

    private boolean ok;
    private TripStatus status;
    private String tripCode;

    public TripTransitionResponse() {}

    public TripTransitionResponse(boolean ok, TripStatus status, String tripCode) {
        this.ok = ok;
        this.status = status;
        this.tripCode = tripCode;
    }

    public boolean isOk() {
        return ok;
    }

    public void setOk(boolean ok) {
        this.ok = ok;
    }

    public TripStatus getStatus() {
        return status;
    }

    public void setStatus(TripStatus status) {
        this.status = status;
    }

    public String getTripCode() {
        return tripCode;
    }

    public void setTripCode(String tripCode) {
        this.tripCode = tripCode;
    }
}
