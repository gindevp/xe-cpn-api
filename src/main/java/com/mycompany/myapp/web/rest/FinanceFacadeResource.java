package com.mycompany.myapp.web.rest;

import com.mycompany.myapp.service.finance.FinanceFacadeService;
import com.mycompany.myapp.service.finance.FinanceFacadeService.CreateReceiptRequest;
import com.mycompany.myapp.service.finance.FinanceFacadeService.DayClosureDTO;
import com.mycompany.myapp.service.finance.FinanceFacadeService.ReceiptDTO;
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

    public FinanceFacadeResource(FinanceFacadeService financeFacadeService) {
        this.financeFacadeService = financeFacadeService;
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
        Pageable pageable
    ) {
        Page<ReceiptDTO> page = financeFacadeService.listReceipts(officeCode, createdBy, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok()
            .headers(headers)
            .body(new ListPage(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements()));
    }

    @PostMapping("/api/receipts")
    @ResponseStatus(HttpStatus.CREATED)
    public ReceiptDTO create(@RequestBody CreateReceiptRequest request) {
        return financeFacadeService.createReceipt(request);
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

    public record ListPage(List<ReceiptDTO> content, int page, int size, long totalElements) {}
}
