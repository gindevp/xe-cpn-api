package com.mycompany.myapp.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * A ProductPriceRule.
 */
@Entity
@Table(name = "product_price_rule")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class ProductPriceRule implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotNull
    @Size(max = 100)
    @Column(name = "group_name", length = 100, nullable = false)
    private String groupName;

    @NotNull
    @Size(max = 150)
    @Column(name = "product_name", length = 150, nullable = false)
    private String productName;

    @DecimalMin(value = "0")
    @Column(name = "current_price", precision = 21, scale = 2)
    private BigDecimal currentPrice;

    @NotNull
    @DecimalMin(value = "0")
    @Column(name = "applied_price", precision = 21, scale = 2, nullable = false)
    private BigDecimal appliedPrice;

    @Size(max = 255)
    @Column(name = "note", length = 255)
    private String note;

    @NotNull
    @Column(name = "active", nullable = false)
    private Boolean active;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public ProductPriceRule id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getGroupName() {
        return this.groupName;
    }

    public ProductPriceRule groupName(String groupName) {
        this.setGroupName(groupName);
        return this;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getProductName() {
        return this.productName;
    }

    public ProductPriceRule productName(String productName) {
        this.setProductName(productName);
        return this;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public BigDecimal getCurrentPrice() {
        return this.currentPrice;
    }

    public ProductPriceRule currentPrice(BigDecimal currentPrice) {
        this.setCurrentPrice(currentPrice);
        return this;
    }

    public void setCurrentPrice(BigDecimal currentPrice) {
        this.currentPrice = currentPrice;
    }

    public BigDecimal getAppliedPrice() {
        return this.appliedPrice;
    }

    public ProductPriceRule appliedPrice(BigDecimal appliedPrice) {
        this.setAppliedPrice(appliedPrice);
        return this;
    }

    public void setAppliedPrice(BigDecimal appliedPrice) {
        this.appliedPrice = appliedPrice;
    }

    public String getNote() {
        return this.note;
    }

    public ProductPriceRule note(String note) {
        this.setNote(note);
        return this;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public Boolean getActive() {
        return this.active;
    }

    public ProductPriceRule active(Boolean active) {
        this.setActive(active);
        return this;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ProductPriceRule)) {
            return false;
        }
        return getId() != null && getId().equals(((ProductPriceRule) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "ProductPriceRule{" +
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
