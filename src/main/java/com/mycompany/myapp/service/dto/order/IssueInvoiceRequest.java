package com.mycompany.myapp.service.dto.order;

/** Thông tin người mua nhập tại màn Giao thành công trước khi xuất HĐĐT MISA. */
public class IssueInvoiceRequest {

    private String taxCode;

    private String companyName;

    private String address;

    private String email;

    public String getTaxCode() {
        return taxCode;
    }

    public void setTaxCode(String taxCode) {
        this.taxCode = taxCode;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
