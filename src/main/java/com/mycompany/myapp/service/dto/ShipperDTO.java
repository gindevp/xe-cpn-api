package com.mycompany.myapp.service.dto;

import java.io.Serializable;

/** Shipper nội bộ + số đơn đang giao (busyCount) để hiển thị Sẵn sàng / Bận. */
public class ShipperDTO implements Serializable {

    private Long id;
    private String fullName;
    private String phone;
    private String officeCode;
    private String officeName;
    private Boolean active;
    private String note;
    private long busyCount;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getOfficeCode() {
        return officeCode;
    }

    public void setOfficeCode(String officeCode) {
        this.officeCode = officeCode;
    }

    public String getOfficeName() {
        return officeName;
    }

    public void setOfficeName(String officeName) {
        this.officeName = officeName;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public long getBusyCount() {
        return busyCount;
    }

    public void setBusyCount(long busyCount) {
        this.busyCount = busyCount;
    }
}
