package com.mycompany.myapp.service.dto;

import java.io.Serializable;

public class SessionPolicyDTO implements Serializable {

    private boolean enabled = true;
    private String logoutTime = "21:00";
    /** Múi giờ áp dụng giờ đăng xuất. */
    private String timeZone = "Asia/Ho_Chi_Minh";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getLogoutTime() {
        return logoutTime;
    }

    public void setLogoutTime(String logoutTime) {
        this.logoutTime = logoutTime;
    }

    public String getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }
}
