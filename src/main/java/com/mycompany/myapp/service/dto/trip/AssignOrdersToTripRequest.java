package com.mycompany.myapp.service.dto.trip;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.ArrayList;
import java.util.List;

public class AssignOrdersToTripRequest {

    @NotBlank
    private String tripCode;

    @NotEmpty
    private List<String> orderCodes = new ArrayList<>();

    /** When assigning, refresh the trip's displayed VTHK tuyến/lộ trình. */
    private String itineraryLabel;

    public String getTripCode() {
        return tripCode;
    }

    public void setTripCode(String tripCode) {
        this.tripCode = tripCode;
    }

    public List<String> getOrderCodes() {
        return orderCodes;
    }

    public void setOrderCodes(List<String> orderCodes) {
        this.orderCodes = orderCodes;
    }

    public String getItineraryLabel() {
        return itineraryLabel;
    }

    public void setItineraryLabel(String itineraryLabel) {
        this.itineraryLabel = itineraryLabel;
    }
}
