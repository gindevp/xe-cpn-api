package com.mycompany.myapp.service.dto.trip;

public class CloseTripRequest {

    private boolean force;
    private String reason;

    public boolean isForce() {
        return force;
    }

    public void setForce(boolean force) {
        this.force = force;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
