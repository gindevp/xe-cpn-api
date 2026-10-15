package com.mycompany.myapp.web.rest;

import com.mycompany.myapp.security.ScreenKey;
import com.mycompany.myapp.security.StaffAccessService;
import com.mycompany.myapp.service.payment.bidv.BidvAdminService;
import com.mycompany.myapp.service.payment.bidv.BidvAdminService.ConfigUpdate;
import com.mycompany.myapp.service.payment.bidv.BidvAdminService.ConfigView;
import com.mycompany.myapp.service.payment.bidv.BidvAdminService.LogView;
import com.mycompany.myapp.service.payment.bidv.BidvAdminService.SampleRequest;
import com.mycompany.myapp.service.payment.bidv.BidvAdminService.TestBillCreate;
import com.mycompany.myapp.service.payment.bidv.BidvAdminService.TestBillView;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

/**
 * Tab "Thu hộ" màn Tích hợp. Ghi đã chặn ở StaffWriteGuardFilter (/api/integration-config → tich-hop);
 * đọc cũng chặn theo màn vì trả API key cấp cho BIDV.
 */
@RestController
@RequestMapping("/api/integration-config/bidv")
public class BidvAdminResource {

    private final BidvAdminService adminService;
    private final StaffAccessService staffAccessService;

    public BidvAdminResource(BidvAdminService adminService, StaffAccessService staffAccessService) {
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

    @PostMapping("/api-key/regenerate")
    public ConfigView regenerateApiKey() {
        return adminService.regenerateApiKey();
    }

    @GetMapping("/test-bills")
    public List<TestBillView> listTestBills() {
        staffAccessService.requireScreenRead(ScreenKey.TICH_HOP);
        return adminService.listTestBills();
    }

    @PostMapping("/test-bills")
    public List<TestBillView> addTestBill(@RequestBody TestBillCreate body) {
        return adminService.addTestBill(body);
    }

    @DeleteMapping("/test-bills/{id}")
    public List<TestBillView> deleteTestBill(@PathVariable("id") Long id) {
        return adminService.deleteTestBill(id);
    }

    @PostMapping("/test-bills/reset")
    public List<TestBillView> resetTestPayments() {
        return adminService.resetTestPayments();
    }

    @GetMapping("/logs")
    public List<LogView> logs(@RequestParam(value = "limit", required = false) Integer limit) {
        staffAccessService.requireScreenRead(ScreenKey.TICH_HOP);
        return adminService.logs(limit);
    }

    @PostMapping("/sample")
    public Map<String, Object> sample(@RequestBody SampleRequest body) {
        return adminService.sample(body);
    }
}
