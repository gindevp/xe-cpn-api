package com.mycompany.myapp.service.criteria;

import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import org.springdoc.core.annotations.ParameterObject;
import tech.jhipster.service.Criteria;
import tech.jhipster.service.filter.*;

/**
 * Criteria class for the {@link com.mycompany.myapp.domain.Receipt} entity. This class is used
 * in {@link com.mycompany.myapp.web.rest.ReceiptResource} to receive all the possible filtering options from
 * the Http GET request parameters.
 * For example the following could be a valid request:
 * {@code /receipts?id.greaterThan=5&attr1.contains=something&attr2.specified=false}
 * As Spring is unable to properly convert the types, unless specific {@link Filter} class are used, we need to use
 * fix type specific filters.
 */
@ParameterObject
@SuppressWarnings("common-java:DuplicatedBlocks")
public class ReceiptCriteria implements Serializable, Criteria {

    private static final long serialVersionUID = 1L;

    private LongFilter id;

    private StringFilter receiptCode;

    private StringFilter payerName;

    private StringFilter payerCode;

    private BigDecimalFilter totalAmount;

    private InstantFilter createdAt;

    private StringFilter createdByUsername;

    private LongFilter officeId;

    private Boolean distinct;

    public ReceiptCriteria() {}

    public ReceiptCriteria(ReceiptCriteria other) {
        this.id = other.optionalId().map(LongFilter::copy).orElse(null);
        this.receiptCode = other.optionalReceiptCode().map(StringFilter::copy).orElse(null);
        this.payerName = other.optionalPayerName().map(StringFilter::copy).orElse(null);
        this.payerCode = other.optionalPayerCode().map(StringFilter::copy).orElse(null);
        this.totalAmount = other.optionalTotalAmount().map(BigDecimalFilter::copy).orElse(null);
        this.createdAt = other.optionalCreatedAt().map(InstantFilter::copy).orElse(null);
        this.createdByUsername = other.optionalCreatedByUsername().map(StringFilter::copy).orElse(null);
        this.officeId = other.optionalOfficeId().map(LongFilter::copy).orElse(null);
        this.distinct = other.distinct;
    }

    @Override
    public ReceiptCriteria copy() {
        return new ReceiptCriteria(this);
    }

    public LongFilter getId() {
        return id;
    }

    public Optional<LongFilter> optionalId() {
        return Optional.ofNullable(id);
    }

    public LongFilter id() {
        if (id == null) {
            setId(new LongFilter());
        }
        return id;
    }

    public void setId(LongFilter id) {
        this.id = id;
    }

    public StringFilter getReceiptCode() {
        return receiptCode;
    }

    public Optional<StringFilter> optionalReceiptCode() {
        return Optional.ofNullable(receiptCode);
    }

    public StringFilter receiptCode() {
        if (receiptCode == null) {
            setReceiptCode(new StringFilter());
        }
        return receiptCode;
    }

    public void setReceiptCode(StringFilter receiptCode) {
        this.receiptCode = receiptCode;
    }

    public StringFilter getPayerName() {
        return payerName;
    }

    public Optional<StringFilter> optionalPayerName() {
        return Optional.ofNullable(payerName);
    }

    public StringFilter payerName() {
        if (payerName == null) {
            setPayerName(new StringFilter());
        }
        return payerName;
    }

    public void setPayerName(StringFilter payerName) {
        this.payerName = payerName;
    }

    public StringFilter getPayerCode() {
        return payerCode;
    }

    public Optional<StringFilter> optionalPayerCode() {
        return Optional.ofNullable(payerCode);
    }

    public StringFilter payerCode() {
        if (payerCode == null) {
            setPayerCode(new StringFilter());
        }
        return payerCode;
    }

    public void setPayerCode(StringFilter payerCode) {
        this.payerCode = payerCode;
    }

    public BigDecimalFilter getTotalAmount() {
        return totalAmount;
    }

    public Optional<BigDecimalFilter> optionalTotalAmount() {
        return Optional.ofNullable(totalAmount);
    }

    public BigDecimalFilter totalAmount() {
        if (totalAmount == null) {
            setTotalAmount(new BigDecimalFilter());
        }
        return totalAmount;
    }

    public void setTotalAmount(BigDecimalFilter totalAmount) {
        this.totalAmount = totalAmount;
    }

    public InstantFilter getCreatedAt() {
        return createdAt;
    }

    public Optional<InstantFilter> optionalCreatedAt() {
        return Optional.ofNullable(createdAt);
    }

    public InstantFilter createdAt() {
        if (createdAt == null) {
            setCreatedAt(new InstantFilter());
        }
        return createdAt;
    }

    public void setCreatedAt(InstantFilter createdAt) {
        this.createdAt = createdAt;
    }

    public StringFilter getCreatedByUsername() {
        return createdByUsername;
    }

    public Optional<StringFilter> optionalCreatedByUsername() {
        return Optional.ofNullable(createdByUsername);
    }

    public StringFilter createdByUsername() {
        if (createdByUsername == null) {
            setCreatedByUsername(new StringFilter());
        }
        return createdByUsername;
    }

    public void setCreatedByUsername(StringFilter createdByUsername) {
        this.createdByUsername = createdByUsername;
    }

    public LongFilter getOfficeId() {
        return officeId;
    }

    public Optional<LongFilter> optionalOfficeId() {
        return Optional.ofNullable(officeId);
    }

    public LongFilter officeId() {
        if (officeId == null) {
            setOfficeId(new LongFilter());
        }
        return officeId;
    }

    public void setOfficeId(LongFilter officeId) {
        this.officeId = officeId;
    }

    public Boolean getDistinct() {
        return distinct;
    }

    public Optional<Boolean> optionalDistinct() {
        return Optional.ofNullable(distinct);
    }

    public Boolean distinct() {
        if (distinct == null) {
            setDistinct(true);
        }
        return distinct;
    }

    public void setDistinct(Boolean distinct) {
        this.distinct = distinct;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final ReceiptCriteria that = (ReceiptCriteria) o;
        return (
            Objects.equals(id, that.id) &&
            Objects.equals(receiptCode, that.receiptCode) &&
            Objects.equals(payerName, that.payerName) &&
            Objects.equals(payerCode, that.payerCode) &&
            Objects.equals(totalAmount, that.totalAmount) &&
            Objects.equals(createdAt, that.createdAt) &&
            Objects.equals(createdByUsername, that.createdByUsername) &&
            Objects.equals(officeId, that.officeId) &&
            Objects.equals(distinct, that.distinct)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, receiptCode, payerName, payerCode, totalAmount, createdAt, createdByUsername, officeId, distinct);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "ReceiptCriteria{" +
            optionalId().map(f -> "id=" + f + ", ").orElse("") +
            optionalReceiptCode().map(f -> "receiptCode=" + f + ", ").orElse("") +
            optionalPayerName().map(f -> "payerName=" + f + ", ").orElse("") +
            optionalPayerCode().map(f -> "payerCode=" + f + ", ").orElse("") +
            optionalTotalAmount().map(f -> "totalAmount=" + f + ", ").orElse("") +
            optionalCreatedAt().map(f -> "createdAt=" + f + ", ").orElse("") +
            optionalCreatedByUsername().map(f -> "createdByUsername=" + f + ", ").orElse("") +
            optionalOfficeId().map(f -> "officeId=" + f + ", ").orElse("") +
            optionalDistinct().map(f -> "distinct=" + f + ", ").orElse("") +
        "}";
    }
}
