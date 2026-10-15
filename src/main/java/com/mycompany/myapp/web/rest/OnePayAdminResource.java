package com.mycompany.myapp.web.rest;

import com.mycompany.myapp.security.ScreenKey;
import com.mycompany.myapp.security.StaffAccessService;
import com.mycompany.myapp.service.payment.onepay.OnePayAdminService;
import com.mycompany.myapp.service.payment.onepay.OnePayAdminService.ConfigUpdate;
import com.mycompany.myapp.service.payment.onepay.OnePayAdminService.ConfigView;
import com.mycompany.myapp.service.payment.onepay.OnePayAdminService.InvoiceCreate;
import com.mycompany.myapp.service.payment.onepay.OnePayAdminService.InvoiceView;
import com.mycompany.myapp.service.payment.onepay.OnePayAdminService.LogView;
import com.mycompany.myapp.service.payment.onepay.OnePayAdminService.SampleIpnRequest;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

/** Mục OnePay trong tab "Thu hộ" màn Tích hợp. Ghi đã chặn ở StaffWriteGuardFilter (/api/integration-config → tich-hop). */
@RestController
@RequestMapping("/api/integration-config/onepay")
public class OnePayAdminResource {

    private final OnePayAdminService adminService;
    private final StaffAccessService staffAccessService;

    public OnePayAdminResource(OnePayAdminService adminService, StaffAccessService staffAccessService) {
        this.adminService = adminService;
        this.staffAccessService = staffAccessService;
    }

    @GetMapping("")
    public ConfigView getConfig() {
        staffAccessService.requireScreenRead(ScreenKey.TICH_HOP);
        return adminService.getConfig();
    }

    @PutMapping("")
    public ConfigView putConfig(@RequestBody ConfigUpdate body) {
        return adminService.updateConfig(body);
    }

    @PostMapping("/test-connection")
    public Map<String, Object> testConnection() {
        return adminService.testConnection();
    }

    @GetMapping("/invoices")
    public List<InvoiceView> listInvoices() {
        staffAccessService.requireScreenRead(ScreenKey.TICH_HOP);
        return adminService.listInvoices();
    }

    @PostMapping("/invoices")
    public InvoiceView createInvoice(@RequestBody InvoiceCreate body) {
        return adminService.createInvoice(body);
    }

    @PostMapping("/invoices/{id}/refresh")
    public InvoiceView refreshInvoice(@PathVariable("id") Long id) {
        return adminService.refreshInvoice(id);
    }

    @PostMapping("/invoices/{id}/account-state")
    public InvoiceView setAccountState(@PathVariable("id") Long id, @RequestParam("active") boolean active) {
        return adminService.setAccountState(id, active);
    }

    @DeleteMapping("/invoices/{id}")
    public List<InvoiceView> deleteInvoice(@PathVariable("id") Long id) {
        return adminService.deleteInvoice(id);
    }

    @PostMapping("/sample-ipn")
    public Map<String, Object> sampleIpn(@RequestBody SampleIpnRequest body) {
        return adminService.sampleIpn(body);
    }

    @GetMapping("/logs")
    public List<LogView> logs(@RequestParam(value = "limit", required = false) Integer limit) {
        staffAccessService.requireScreenRead(ScreenKey.TICH_HOP);
        return adminService.logs(limit);
    }
}
