package com.mycompany.myapp.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * A SurchargePolicy.
 */
@Entity
@Table(name = "surcharge_policy")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class SurchargePolicy implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotNull
    @Column(name = "home_delivery_enabled", nullable = false)
    private Boolean homeDeliveryEnabled;

    @NotNull
    @DecimalMin(value = "0")
    @Column(name = "default_home_delivery_amount", precision = 21, scale = 2, nullable = false)
    private BigDecimal defaultHomeDeliveryAmount;

    @NotNull
    @Column(name = "cod_enabled", nullable = false)
    private Boolean codEnabled;

    @NotNull
    @DecimalMin(value = "0")
    @DecimalMax(value = "100")
    @Column(name = "cod_percent", precision = 21, scale = 2, nullable = false)
    private BigDecimal codPercent;

    @NotNull
    @DecimalMin(value = "0")
    @Column(name = "cod_min_fee", precision = 21, scale = 2, nullable = false)
    private BigDecimal codMinFee;

    /**
     * JSON array of COD fee tiers:
     * [{minAmount, maxAmount|null, feeAmount|null, feePercent|null}, ...]
     * Band match: min=0 → [0, max]; min&gt;0 → (min, max]; max null → open-ended.
     */
    @Lob
    @Column(name = "cod_tiers_json")
    private String codTiersJson;

    @NotNull
    @Column(name = "storage_enabled", nullable = false)
    private Boolean storageEnabled;

    @NotNull
    @Min(value = 0)
    @Column(name = "storage_free_days", nullable = false)
    private Integer storageFreeDays;

    @NotNull
    @DecimalMin(value = "0")
    @Column(name = "storage_fee_per_day", precision = 21, scale = 2, nullable = false)
    private BigDecimal storageFeePerDay;

    @NotNull
    @Column(name = "insurance_enabled", nullable = false)
    private Boolean insuranceEnabled;

    @NotNull
    @DecimalMin(value = "0")
    @Column(name = "insurance_threshold", precision = 21, scale = 2, nullable = false)
    private BigDecimal insuranceThreshold;

    @NotNull
    @DecimalMin(value = "0")
    @DecimalMax(value = "100")
    @Column(name = "insurance_percent_under", precision = 21, scale = 2, nullable = false)
    private BigDecimal insurancePercentUnder;

    @NotNull
    @DecimalMin(value = "0")
    @DecimalMax(value = "100")
    @Column(name = "insurance_percent_over", precision = 21, scale = 2, nullable = false)
    private BigDecimal insurancePercentOver;

    @NotNull
    @Column(name = "refund_enabled", nullable = false)
    private Boolean refundEnabled;

    @NotNull
    @DecimalMin(value = "0")
    @DecimalMax(value = "100")
    @Column(name = "refund_percent", precision = 21, scale = 2, nullable = false)
    private BigDecimal refundPercent;

    @Column(name = "updated_at")
    private Instant updatedAt;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public SurchargePolicy id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Boolean getHomeDeliveryEnabled() {
        return this.homeDeliveryEnabled;
    }

    public SurchargePolicy homeDeliveryEnabled(Boolean homeDeliveryEnabled) {
        this.setHomeDeliveryEnabled(homeDeliveryEnabled);
        return this;
    }

    public void setHomeDeliveryEnabled(Boolean homeDeliveryEnabled) {
        this.homeDeliveryEnabled = homeDeliveryEnabled;
    }

    public BigDecimal getDefaultHomeDeliveryAmount() {
        return this.defaultHomeDeliveryAmount;
    }

    public SurchargePolicy defaultHomeDeliveryAmount(BigDecimal defaultHomeDeliveryAmount) {
        this.setDefaultHomeDeliveryAmount(defaultHomeDeliveryAmount);
        return this;
    }

    public void setDefaultHomeDeliveryAmount(BigDecimal defaultHomeDeliveryAmount) {
        this.defaultHomeDeliveryAmount = defaultHomeDeliveryAmount;
    }

    public Boolean getCodEnabled() {
        return this.codEnabled;
    }

    public SurchargePolicy codEnabled(Boolean codEnabled) {
        this.setCodEnabled(codEnabled);
        return this;
    }

    public void setCodEnabled(Boolean codEnabled) {
        this.codEnabled = codEnabled;
    }

    public BigDecimal getCodPercent() {
        return this.codPercent;
    }

    public SurchargePolicy codPercent(BigDecimal codPercent) {
        this.setCodPercent(codPercent);
        return this;
    }

    public void setCodPercent(BigDecimal codPercent) {
        this.codPercent = codPercent;
    }

    public BigDecimal getCodMinFee() {
        return this.codMinFee;
    }

    public SurchargePolicy codMinFee(BigDecimal codMinFee) {
        this.setCodMinFee(codMinFee);
        return this;
    }

    public void setCodMinFee(BigDecimal codMinFee) {
        this.codMinFee = codMinFee;
    }

    public String getCodTiersJson() {
        return this.codTiersJson;
    }

    public SurchargePolicy codTiersJson(String codTiersJson) {
        this.setCodTiersJson(codTiersJson);
        return this;
    }

    public void setCodTiersJson(String codTiersJson) {
        this.codTiersJson = codTiersJson;
    }

    public Boolean getStorageEnabled() {
        return this.storageEnabled;
    }

    public SurchargePolicy storageEnabled(Boolean storageEnabled) {
        this.setStorageEnabled(storageEnabled);
        return this;
    }

    public void setStorageEnabled(Boolean storageEnabled) {
        this.storageEnabled = storageEnabled;
    }

    public Integer getStorageFreeDays() {
        return this.storageFreeDays;
    }

    public SurchargePolicy storageFreeDays(Integer storageFreeDays) {
        this.setStorageFreeDays(storageFreeDays);
        return this;
    }

    public void setStorageFreeDays(Integer storageFreeDays) {
        this.storageFreeDays = storageFreeDays;
    }

    public BigDecimal getStorageFeePerDay() {
        return this.storageFeePerDay;
    }

    public SurchargePolicy storageFeePerDay(BigDecimal storageFeePerDay) {
        this.setStorageFeePerDay(storageFeePerDay);
        return this;
    }

    public void setStorageFeePerDay(BigDecimal storageFeePerDay) {
        this.storageFeePerDay = storageFeePerDay;
    }

    public Boolean getInsuranceEnabled() {
        return this.insuranceEnabled;
    }

    public SurchargePolicy insuranceEnabled(Boolean insuranceEnabled) {
        this.setInsuranceEnabled(insuranceEnabled);
        return this;
    }

    public void setInsuranceEnabled(Boolean insuranceEnabled) {
        this.insuranceEnabled = insuranceEnabled;
    }

    public BigDecimal getInsuranceThreshold() {
        return this.insuranceThreshold;
    }

    public SurchargePolicy insuranceThreshold(BigDecimal insuranceThreshold) {
        this.setInsuranceThreshold(insuranceThreshold);
        return this;
    }

    public void setInsuranceThreshold(BigDecimal insuranceThreshold) {
        this.insuranceThreshold = insuranceThreshold;
    }

    public BigDecimal getInsurancePercentUnder() {
        return this.insurancePercentUnder;
    }

    public SurchargePolicy insurancePercentUnder(BigDecimal insurancePercentUnder) {
        this.setInsurancePercentUnder(insurancePercentUnder);
        return this;
    }

    public void setInsurancePercentUnder(BigDecimal insurancePercentUnder) {
        this.insurancePercentUnder = insurancePercentUnder;
    }

    public BigDecimal getInsurancePercentOver() {
        return this.insurancePercentOver;
    }

    public SurchargePolicy insurancePercentOver(BigDecimal insurancePercentOver) {
        this.setInsurancePercentOver(insurancePercentOver);
        return this;
    }

    public void setInsurancePercentOver(BigDecimal insurancePercentOver) {
        this.insurancePercentOver = insurancePercentOver;
    }

    public Boolean getRefundEnabled() {
        return this.refundEnabled;
    }

    public SurchargePolicy refundEnabled(Boolean refundEnabled) {
        this.setRefundEnabled(refundEnabled);
        return this;
    }

    public void setRefundEnabled(Boolean refundEnabled) {
        this.refundEnabled = refundEnabled;
    }

    public BigDecimal getRefundPercent() {
        return this.refundPercent;
    }

    public SurchargePolicy refundPercent(BigDecimal refundPercent) {
        this.setRefundPercent(refundPercent);
        return this;
    }

    public void setRefundPercent(BigDecimal refundPercent) {
        this.refundPercent = refundPercent;
    }

    public Instant getUpdatedAt() {
        return this.updatedAt;
    }

    public SurchargePolicy updatedAt(Instant updatedAt) {
        this.setUpdatedAt(updatedAt);
        return this;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SurchargePolicy)) {
            return false;
        }
        return getId() != null && getId().equals(((SurchargePolicy) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "SurchargePolicy{" +
            "id=" + getId() +
            ", homeDeliveryEnabled='" + getHomeDeliveryEnabled() + "'" +
            ", defaultHomeDeliveryAmount=" + getDefaultHomeDeliveryAmount() +
            ", codEnabled='" + getCodEnabled() + "'" +
            ", codPercent=" + getCodPercent() +
            ", codMinFee=" + getCodMinFee() +
            ", storageEnabled='" + getStorageEnabled() + "'" +
            ", storageFreeDays=" + getStorageFreeDays() +
            ", storageFeePerDay=" + getStorageFeePerDay() +
            ", insuranceEnabled='" + getInsuranceEnabled() + "'" +
            ", insuranceThreshold=" + getInsuranceThreshold() +
            ", insurancePercentUnder=" + getInsurancePercentUnder() +
            ", insurancePercentOver=" + getInsurancePercentOver() +
            ", refundEnabled='" + getRefundEnabled() + "'" +
            ", refundPercent=" + getRefundPercent() +
            ", updatedAt='" + getUpdatedAt() + "'" +
            "}";
    }
}
