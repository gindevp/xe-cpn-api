package com.mycompany.myapp.service.dto;

import java.io.Serializable;

public class TrackLookupPolicyDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private boolean enabled = true;
    private int dailyLimit = 30;
    private int qrRefreshSeconds = 60;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getDailyLimit() {
        return dailyLimit;
    }

    public void setDailyLimit(int dailyLimit) {
        this.dailyLimit = dailyLimit;
    }

    public int getQrRefreshSeconds() {
        return qrRefreshSeconds;
    }

    public void setQrRefreshSeconds(int qrRefreshSeconds) {
        this.qrRefreshSeconds = qrRefreshSeconds;
    }
}
