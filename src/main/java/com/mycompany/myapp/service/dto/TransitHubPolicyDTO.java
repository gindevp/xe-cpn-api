package com.mycompany.myapp.service.dto;

public class TransitHubPolicyDTO {

    private String groupPoints = "NB,ND,TB";
    private String groupHubOfficeCode;
    private String defaultHubOfficeCode;

    public String getGroupPoints() {
        return groupPoints;
    }

    public void setGroupPoints(String groupPoints) {
        this.groupPoints = groupPoints;
    }

    public String getGroupHubOfficeCode() {
        return groupHubOfficeCode;
    }

    public void setGroupHubOfficeCode(String groupHubOfficeCode) {
        this.groupHubOfficeCode = groupHubOfficeCode;
    }

    public String getDefaultHubOfficeCode() {
        return defaultHubOfficeCode;
    }

    public void setDefaultHubOfficeCode(String defaultHubOfficeCode) {
        this.defaultHubOfficeCode = defaultHubOfficeCode;
    }
}
