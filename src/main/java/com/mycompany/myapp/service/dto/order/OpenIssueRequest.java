package com.mycompany.myapp.service.dto.order;

import java.util.ArrayList;
import java.util.List;

public class OpenIssueRequest {

    private String issueType;
    private String reason;
    private List<String> photos = new ArrayList<>();

    public String getIssueType() {
        return issueType;
    }

    public void setIssueType(String issueType) {
        this.issueType = issueType;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public List<String> getPhotos() {
        return photos;
    }

    public void setPhotos(List<String> photos) {
        this.photos = photos != null ? photos : new ArrayList<>();
    }
}
