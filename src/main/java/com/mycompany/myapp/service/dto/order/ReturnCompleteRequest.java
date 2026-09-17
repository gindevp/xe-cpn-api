package com.mycompany.myapp.service.dto.order;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;

/** Hoàn thành công — bắt buộc ≥1 ảnh chứng từ (giống POD giao). */
public class ReturnCompleteRequest {

    @Size(max = 100)
    private String actualRecipientName;

    @Size(max = 20)
    private String actualRecipientPhone;

    @NotEmpty
    @Size(max = 3)
    private List<@NotBlank @Size(max = 2_000_000) String> photos = new ArrayList<>();

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
}
