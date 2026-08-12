package com.mycompany.myapp.service.dto.trip;

import jakarta.validation.constraints.NotEmpty;
import java.util.ArrayList;
import java.util.List;

public class HandoverRequest {

    @NotEmpty
    private List<String> orderCodes = new ArrayList<>();

    public List<String> getOrderCodes() {
        return orderCodes;
    }

    public void setOrderCodes(List<String> orderCodes) {
        this.orderCodes = orderCodes;
    }
}
