package com.mycompany.myapp.web.rest;

import com.mycompany.myapp.security.ScreenKey;
import com.mycompany.myapp.security.SecurityUtils;
import com.mycompany.myapp.security.StaffAccessService;
import com.mycompany.myapp.service.invoice.InvoiceAutoIssueService;
import com.mycompany.myapp.service.invoice.MeInvoiceIssueService;
import com.mycompany.myapp.web.rest.errors.BadRequestAlertException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Màn Quản lý hoá đơn (kế toán): danh sách, tích đã xuất cá nhân, xuất bù; tự điền thông tin công ty theo SĐT. */
@RestController
@RequestMapping("/api/invoices")
public class InvoiceManagementResource {

    private final InvoiceAutoIssueService invoiceAutoIssueService;
    private final MeInvoiceIssueService meInvoiceIssueService;
    private final StaffAccessService staffAccessService;

    public InvoiceManagementResource(
        InvoiceAutoIssueService invoiceAutoIssueService,
        MeInvoiceIssueService meInvoiceIssueService,
        StaffAccessService staffAccessService
    ) {
        this.invoiceAutoIssueService = invoiceAutoIssueService;
        this.meInvoiceIssueService = meInvoiceIssueService;
        this.staffAccessService = staffAccessService;
    }

    @GetMapping
    public List<InvoiceAutoIssueService.InvoiceRow> list(
        @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        staffAccessService.requireScreenRead(ScreenKey.QUAN_LY_HOA_DON, ScreenKey.GIAO_THANH_CONG);
        return invoiceAutoIssueService.list(from, to, staffAccessService.scopedOfficeCode());
    }

    /** Tích / bỏ tích "đã xuất HĐ cá nhân" cho nhiều đơn; trả kết quả từng đơn (lỗi không chặn đơn khác). */
    @PostMapping("/mark")
    public Map<String, String> mark(@RequestBody MarkRequest request) {
        if (request == null || request.orderCodes() == null || request.orderCodes().isEmpty()) {
            throw new BadRequestAlertException("Chưa chọn đơn nào", "meInvoice", "markEmpty");
        }
        if (request.orderCodes().size() > 2000) {
            throw new BadRequestAlertException("Tối đa 2000 đơn mỗi lần", "meInvoice", "markTooMany");
        }
        String actor = SecurityUtils.getCurrentUserLogin().orElse("system");
        boolean marked = !Boolean.FALSE.equals(request.marked());
        Map<String, String> out = new LinkedHashMap<>();
        for (String code : new ArrayList<>(request.orderCodes())) {
            if (code == null || code.isBlank()) {
                continue;
            }
            try {
                meInvoiceIssueService.markPersonalIssued(code, marked, actor);
                out.put(code, "OK");
            } catch (BadRequestAlertException e) {
                out.put(code, e.getBody().getTitle());
            } catch (RuntimeException e) {
                out.put(code, e.getMessage());
            }
        }
        return out;
    }

    @PostMapping("/backfill")
    public InvoiceAutoIssueService.BackfillStatus backfill(@RequestBody BackfillRequest request) {
        String actor = SecurityUtils.getCurrentUserLogin().orElse("system");
        return invoiceAutoIssueService.startBackfill(request == null ? null : request.orderCodes(), actor);
    }

    @GetMapping("/backfill")
    public InvoiceAutoIssueService.BackfillStatus backfillStatus() {
        return invoiceAutoIssueService.backfillStatus();
    }

    /** SĐT và các MST trên hóa đơn doanh nghiệp đã xuất. Có {@code q} thì lọc một số. */
    @GetMapping("/buyer-directory")
    public List<InvoiceAutoIssueService.BuyerDirectoryEntry> buyerDirectory(@RequestParam(value = "q", required = false) String query) {
        staffAccessService.requireScreenRead(ScreenKey.QUAN_LY_HOA_DON);
        return invoiceAutoIssueService.buyerDirectory(query);
    }

    /** MST trên hóa đơn doanh nghiệp đã xuất của SĐT; không có → 204. */
    @GetMapping("/buyer-profile")
    public ResponseEntity<Map<String, String>> buyerProfile(@RequestParam("phone") String phone) {
        return invoiceAutoIssueService.buyerProfile(phone).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping("/buyer-profiles")
    public List<Map<String, String>> buyerProfiles(@RequestParam("phone") String phone) {
        return invoiceAutoIssueService.buyerProfiles(phone);
    }

    public record MarkRequest(List<String> orderCodes, Boolean marked) {}

    public record BackfillRequest(List<String> orderCodes) {}
}
