package com.mycompany.myapp.service.dto;

import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/**
 * A DTO for the {@link com.mycompany.myapp.domain.SurchargePolicy} entity.
 */
@SuppressWarnings("common-java:DuplicatedBlocks")
public class SurchargePolicyDTO implements Serializable {

    private Long id;

    @NotNull
    private Boolean homeDeliveryEnabled;

    @NotNull
    @DecimalMin(value = "0")
    private BigDecimal defaultHomeDeliveryAmount;

    @NotNull
    private Boolean codEnabled;

    @NotNull
    @DecimalMin(value = "0")
    @DecimalMax(value = "100")
    private BigDecimal codPercent;

    @NotNull
    @DecimalMin(value = "0")
    private BigDecimal codMinFee;

    private String codTiersJson;

    @NotNull
    private Boolean storageEnabled;

    @NotNull
    @Min(value = 0)
    private Integer storageFreeDays;

    @NotNull
    @DecimalMin(value = "0")
    private BigDecimal storageFeePerDay;

    @NotNull
    private Boolean insuranceEnabled;

    @NotNull
    @DecimalMin(value = "0")
    private BigDecimal insuranceThreshold;

    @NotNull
    @DecimalMin(value = "0")
    @DecimalMax(value = "100")
    private BigDecimal insurancePercentUnder;

    @NotNull
    @DecimalMin(value = "0")
    @DecimalMax(value = "100")
    private BigDecimal insurancePercentOver;

    @NotNull
    private Boolean refundEnabled;

    @NotNull
    @DecimalMin(value = "0")
    @DecimalMax(value = "100")
    private BigDecimal refundPercent;

    private Instant updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Boolean getHomeDeliveryEnabled() {
        return homeDeliveryEnabled;
    }

    public void setHomeDeliveryEnabled(Boolean homeDeliveryEnabled) {
        this.homeDeliveryEnabled = homeDeliveryEnabled;
    }

    public BigDecimal getDefaultHomeDeliveryAmount() {
        return defaultHomeDeliveryAmount;
    }

    public void setDefaultHomeDeliveryAmount(BigDecimal defaultHomeDeliveryAmount) {
        this.defaultHomeDeliveryAmount = defaultHomeDeliveryAmount;
    }

    public Boolean getCodEnabled() {
        return codEnabled;
    }

    public void setCodEnabled(Boolean codEnabled) {
        this.codEnabled = codEnabled;
    }

    public BigDecimal getCodPercent() {
        return codPercent;
    }

    public void setCodPercent(BigDecimal codPercent) {
        this.codPercent = codPercent;
    }

    public BigDecimal getCodMinFee() {
        return codMinFee;
    }

    public void setCodMinFee(BigDecimal codMinFee) {
        this.codMinFee = codMinFee;
    }

    public String getCodTiersJson() {
        return codTiersJson;
    }

    public void setCodTiersJson(String codTiersJson) {
        this.codTiersJson = codTiersJson;
    }

    public Boolean getStorageEnabled() {
        return storageEnabled;
    }

    public void setStorageEnabled(Boolean storageEnabled) {
        this.storageEnabled = storageEnabled;
    }

    public Integer getStorageFreeDays() {
        return storageFreeDays;
    }

    public void setStorageFreeDays(Integer storageFreeDays) {
        this.storageFreeDays = storageFreeDays;
    }

    public BigDecimal getStorageFeePerDay() {
        return storageFeePerDay;
    }

    public void setStorageFeePerDay(BigDecimal storageFeePerDay) {
        this.storageFeePerDay = storageFeePerDay;
    }

    public Boolean getInsuranceEnabled() {
        return insuranceEnabled;
    }

    public void setInsuranceEnabled(Boolean insuranceEnabled) {
        this.insuranceEnabled = insuranceEnabled;
    }

    public BigDecimal getInsuranceThreshold() {
        return insuranceThreshold;
    }

    public void setInsuranceThreshold(BigDecimal insuranceThreshold) {
        this.insuranceThreshold = insuranceThreshold;
    }

    public BigDecimal getInsurancePercentUnder() {
        return insurancePercentUnder;
    }

    public void setInsurancePercentUnder(BigDecimal insurancePercentUnder) {
        this.insurancePercentUnder = insurancePercentUnder;
    }

    public BigDecimal getInsurancePercentOver() {
        return insurancePercentOver;
    }

    public void setInsurancePercentOver(BigDecimal insurancePercentOver) {
        this.insurancePercentOver = insurancePercentOver;
    }

    public Boolean getRefundEnabled() {
        return refundEnabled;
    }

    public void setRefundEnabled(Boolean refundEnabled) {
        this.refundEnabled = refundEnabled;
    }

    public BigDecimal getRefundPercent() {
        return refundPercent;
    }

    public void setRefundPercent(BigDecimal refundPercent) {
        this.refundPercent = refundPercent;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SurchargePolicyDTO)) {
            return false;
        }

        SurchargePolicyDTO surchargePolicyDTO = (SurchargePolicyDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, surchargePolicyDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "SurchargePolicyDTO{" +
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
