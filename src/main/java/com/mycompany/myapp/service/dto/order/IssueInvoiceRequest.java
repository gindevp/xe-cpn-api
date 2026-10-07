package com.mycompany.myapp.service.dto.order;

/** Thông tin người mua nhập tại màn Giao thành công trước khi xuất HĐĐT MISA. */
public class IssueInvoiceRequest {

    private String taxCode;

    private String companyName;

    private String address;

    private String email;

    /** Tên người ghi trên hóa đơn. Trống thì lấy tên người trả cước. */
    private String buyerName;

    /** CCCD/CMND (không bắt buộc). */
    private String buyerIdNumber;

    /** SĐT người trên hóa đơn (không bắt buộc). */
    private String buyerPhone;

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

    public String getBuyerName() {
        return buyerName;
    }

    public void setBuyerName(String buyerName) {
        this.buyerName = buyerName;
    }

    public String getBuyerIdNumber() {
        return buyerIdNumber;
    }

    public void setBuyerIdNumber(String buyerIdNumber) {
        this.buyerIdNumber = buyerIdNumber;
    }

    public String getBuyerPhone() {
        return buyerPhone;
    }

    public void setBuyerPhone(String buyerPhone) {
        this.buyerPhone = buyerPhone;
    }
}
