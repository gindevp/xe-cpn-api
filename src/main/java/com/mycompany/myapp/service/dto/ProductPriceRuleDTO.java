package com.mycompany.myapp.service.dto;

import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * A DTO for the {@link com.mycompany.myapp.domain.ProductPriceRule} entity.
 */
@SuppressWarnings("common-java:DuplicatedBlocks")
public class ProductPriceRuleDTO implements Serializable {

    private Long id;

    @NotNull
    @Size(max = 100)
    private String groupName;

    @NotNull
    @Size(max = 150)
    private String productName;

    @DecimalMin(value = "0")
    private BigDecimal currentPrice;

    @NotNull
    @DecimalMin(value = "0")
    private BigDecimal appliedPrice;

    @Size(max = 255)
    private String note;

    @NotNull
    private Boolean active;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public BigDecimal getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(BigDecimal currentPrice) {
        this.currentPrice = currentPrice;
    }

    public BigDecimal getAppliedPrice() {
        return appliedPrice;
    }

    public void setAppliedPrice(BigDecimal appliedPrice) {
        this.appliedPrice = appliedPrice;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ProductPriceRuleDTO)) {
            return false;
        }

        ProductPriceRuleDTO productPriceRuleDTO = (ProductPriceRuleDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, productPriceRuleDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "ProductPriceRuleDTO{" +
            "id=" + getId() +
            ", groupName='" + getGroupName() + "'" +
            ", productName='" + getProductName() + "'" +
            ", currentPrice=" + getCurrentPrice() +
            ", appliedPrice=" + getAppliedPrice() +
            ", note='" + getNote() + "'" +
            ", active='" + getActive() + "'" +
            "}";
    }
}
