package com.mycompany.myapp.service.dto.order;

import com.mycompany.myapp.domain.enumeration.DeliveryPartner;
import com.mycompany.myapp.domain.enumeration.PaymentMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class PodRequest {

    /** COUNTER or HOME */
    @NotBlank
    private String channel;

    @NotBlank
    @Size(max = 100)
    private String actualRecipientName;

    @Size(max = 20)
    private String actualRecipientPhone;

    /** Bắt buộc ≥1, trừ Admin giao bù có {@link #catchUpReason}. */
    @Size(max = 3)
    private List<@NotBlank @Size(max = 2_000_000) String> photos = new ArrayList<>();

    private BigDecimal collectedAmount;

    /** Nhãn ảnh, ví dụ Nhận / Giao. Để trống với POD nhân viên. */
    @Size(max = 20)
    private String caption;

    private PaymentMethod paymentMethod = PaymentMethod.TM;

    /** Bắt buộc khi đơn đang giao thất bại: lý do chuyển sang giao thành công. */
    @Size(max = 200)
    private String failedReason;

    /** Admin: login nhân viên đang giữ tiền khách (chịu nợ phần giao). */
    @Size(max = 50)
    private String debtOwnerUsername;

    /** Admin giao thành công bù (quên quét giao / Ahamove mất callback): lý do, cho phép không ảnh. */
    @Size(max = 200)
    private String catchUpReason;

    public String getDebtOwnerUsername() {
        return debtOwnerUsername;
    }

    public void setDebtOwnerUsername(String debtOwnerUsername) {
        this.debtOwnerUsername = debtOwnerUsername;
    }

    public String getCatchUpReason() {
        return catchUpReason;
    }

    public void setCatchUpReason(String catchUpReason) {
        this.catchUpReason = catchUpReason;
    }

    public String getFailedReason() {
        return failedReason;
    }

    public void setFailedReason(String failedReason) {
        this.failedReason = failedReason;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getActualRecipientName() {
        return actualRecipientName;
    }

    public void setActualRecipientName(String actualRecipientName) {
        this.actualRecipientName = actualRecipientName;
    }

    public String getActualRecipientPhone() {
        return actualRecipientPhone;
    }

    public void setActualRecipientPhone(String actualRecipientPhone) {
        this.actualRecipientPhone = actualRecipientPhone;
    }

    public List<String> getPhotos() {
        return photos;
    }

    public void setPhotos(List<String> photos) {
        this.photos = photos;
    }

    public BigDecimal getCollectedAmount() {
        return collectedAmount;
    }

    public void setCollectedAmount(BigDecimal collectedAmount) {
        this.collectedAmount = collectedAmount;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getCaption() {
        return caption;
    }

    public void setCaption(String caption) {
        this.caption = caption;
    }
}
