package com.mycompany.myapp.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.time.Instant;

/** VP trung chuyển khi đơn đi từ tỉnh sang tỉnh — một dòng. */
@Entity
@Table(name = "transit_hub_policy")
public class TransitHubPolicy implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Điểm lộ trình giao cho nhau thì dùng group hub. Ví dụ NB,ND,TB. */
    @NotNull
    @Size(max = 200)
    @Column(name = "group_points", length = 200, nullable = false)
    private String groupPoints = "NB,ND,TB";

    @Size(max = 50)
    @Column(name = "group_hub_office_code", length = 50)
    private String groupHubOfficeCode;

    @Size(max = 50)
    @Column(name = "default_hub_office_code", length = 50)
    private String defaultHubOfficeCode;

    @Column(name = "updated_at")
    private Instant updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getGroupPoints() {
        return groupPoints;
    }

    public void setGroupPoints(String groupPoints) {
        this.groupPoints = groupPoints;
    }

    public String getGroupHubOfficeCode() {
        return groupHubOfficeCode;
    }

    public void setGroupHubOfficeCode(String groupHubOfficeCode) {
        this.groupHubOfficeCode = groupHubOfficeCode;
    }

    public String getDefaultHubOfficeCode() {
        return defaultHubOfficeCode;
    }

    public void setDefaultHubOfficeCode(String defaultHubOfficeCode) {
        this.defaultHubOfficeCode = defaultHubOfficeCode;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
