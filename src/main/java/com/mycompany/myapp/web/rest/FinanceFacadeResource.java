package com.mycompany.myapp.web.rest;

import com.mycompany.myapp.service.finance.FinanceFacadeService;
import com.mycompany.myapp.service.finance.FinanceFacadeService.CreateReceiptRequest;
import com.mycompany.myapp.service.finance.FinanceFacadeService.DayClosureDTO;
import com.mycompany.myapp.service.finance.FinanceFacadeService.ReceiptDTO;
import com.mycompany.myapp.service.finance.StaffDepositService;
import com.mycompany.myapp.web.rest.errors.BadRequestAlertException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tech.jhipster.web.util.PaginationUtil;

@RestController
public class FinanceFacadeResource {

    private final FinanceFacadeService financeFacadeService;
    private final StaffDepositService staffDepositService;

    public FinanceFacadeResource(FinanceFacadeService financeFacadeService, StaffDepositService staffDepositService) {
        this.financeFacadeService = financeFacadeService;
        this.staffDepositService = staffDepositService;
    }

    @GetMapping("/api/receipts/candidates")
    public List<FinanceFacadeService.CandidateDTO> candidates(
        @RequestParam(required = false) String officeCode,
        @RequestParam(required = false) String keyword
    ) {
        return financeFacadeService.candidates(officeCode, keyword);
    }

    @GetMapping("/api/receipts")
    public ResponseEntity<ListPage> list(
        @RequestParam(required = false) String officeCode,
        @RequestParam(required = false) String createdBy,
        @RequestParam(required = false) String code,
        @RequestParam(required = false) String payer,
        @RequestParam(required = false) String creator,
        @RequestParam(required = false) String day,
        @RequestParam(required = false) String status,
        Pageable pageable
    ) {
        var filter = new FinanceFacadeService.ReceiptListFilter(code, payer, creator, day, status);
        Page<ReceiptDTO> page = financeFacadeService.listReceipts(officeCode, createdBy, filter, pageable);
        java.math.BigDecimal sum = financeFacadeService.sumReceipts(officeCode, createdBy, filter);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok()
            .headers(headers)
            .body(
                new ListPage(
                    page.getContent(),
                    page.getNumber(),
                    page.getSize(),
                    page.getTotalElements(),
                    sum,
                    staffDepositService.transferContents(page.getContent())
                )
            );
    }

    @GetMapping("/api/receipts/{receiptCode}/proof-image")
    public Map<String, String> proofImage(@PathVariable String receiptCode) {
        String image = financeFacadeService.receiptProofImage(receiptCode);
        return image == null ? Map.of() : Map.of("image", image);
    }

    @PostMapping("/api/receipts")
    @ResponseStatus(HttpStatus.CREATED)
    public ReceiptDTO create(@RequestBody CreateReceiptRequest request) {
        return financeFacadeService.createReceipt(request);
    }

    @PostMapping("/api/receipts/{receiptCode}/confirm")
    public ReceiptDTO confirm(
        @PathVariable String receiptCode,
        @RequestBody(required = false) FinanceFacadeService.ConfirmReceiptRequest body
    ) {
        return financeFacadeService.confirmReceipt(receiptCode, body);
    }

    @PostMapping("/api/receipts/{receiptCode}/unconfirm")
    public ReceiptDTO unconfirm(@PathVariable String receiptCode) {
        return financeFacadeService.unconfirmReceipt(receiptCode);
    }

    public record CancelReceiptRequest(String reason) {}

    @PostMapping("/api/receipts/{receiptCode}/cancel")
    public Map<String, Boolean> cancel(@PathVariable String receiptCode, @RequestBody(required = false) CancelReceiptRequest body) {
        financeFacadeService.cancelReceipt(receiptCode, body == null ? null : body.reason());
        return Map.of("ok", true);
    }

    @PostMapping("/api/receipts/waive")
    public FinanceFacadeService.WaiveResult waive(@RequestBody FinanceFacadeService.WaiveRequest request) {
        return financeFacadeService.waiveDues(request);
    }

    @GetMapping("/api/receipts/history")
    public List<FinanceFacadeService.HistoryDTO> history(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return financeFacadeService.history(from, to);
    }

    @GetMapping("/api/day-closures")
    public DayClosureDTO getDay(
        @RequestParam String officeCode,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate businessDate
    ) {
        return financeFacadeService.getDay(officeCode, businessDate);
    }

    @PostMapping("/api/day-closures")
    public DayClosureDTO closeDay(@RequestBody(required = false) Map<String, String> body) {
        if (body == null) {
            throw new BadRequestAlertException("Request body required", "finance", "bodyRequired");
        }
        String officeCode = body.get("officeCode");
        LocalDate date = parseBusinessDate(body.get("businessDate"));
        return financeFacadeService.closeDay(officeCode, date);
    }

    @PostMapping("/api/day-closures/reopen")
    public DayClosureDTO reopenDay(@RequestBody(required = false) Map<String, String> body) {
        if (body == null) {
            throw new BadRequestAlertException("Request body required", "finance", "bodyRequired");
        }
        String officeCode = body.get("officeCode");
        LocalDate date = parseBusinessDate(body.get("businessDate"));
        return financeFacadeService.reopenDay(officeCode, date);
    }

    private static LocalDate parseBusinessDate(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(raw.trim());
        } catch (Exception ex) {
            throw new BadRequestAlertException("Invalid businessDate: " + raw, "finance", "invalidBusinessDate");
        }
    }

    /** {@code transferContents}: mã phiếu → nội dung chuyển khoản theo mẫu cấu hình QR nộp tiền. */
    public record ListPage(
        List<ReceiptDTO> content,
        int page,
        int size,
        long totalElements,
        java.math.BigDecimal totalAmount,
        java.util.Map<String, String> transferContents
    ) {}
}
