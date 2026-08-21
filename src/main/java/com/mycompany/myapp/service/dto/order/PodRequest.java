package com.mycompany.myapp.service.dto.order;

import com.mycompany.myapp.domain.enumeration.DeliveryPartner;
import com.mycompany.myapp.domain.enumeration.PaymentMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
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

    @NotEmpty
    @Size(max = 3)
    private List<@NotBlank @Size(max = 2_000_000) String> photos = new ArrayList<>();

    private BigDecimal collectedAmount;

    private PaymentMethod paymentMethod = PaymentMethod.TM;

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
}
