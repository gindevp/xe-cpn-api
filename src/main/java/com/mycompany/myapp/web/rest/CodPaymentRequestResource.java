package com.mycompany.myapp.web.rest;

import com.mycompany.myapp.service.order.CodPaymentRequestService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders/cod")
public class CodPaymentRequestResource {

    private final CodPaymentRequestService codPaymentRequestService;

    public CodPaymentRequestResource(CodPaymentRequestService codPaymentRequestService) {
        this.codPaymentRequestService = codPaymentRequestService;
    }

    /** Giấy đề nghị thanh toán COD (mẫu BMTT-01) của 1 đơn, dạng .xlsx. */
    @GetMapping("/payment-request")
    public ResponseEntity<byte[]> paymentRequest(@RequestParam String code) {
        byte[] body = codPaymentRequestService.build(code);
        String file = "de-nghi-thanh-toan-cod-" + code.trim().replaceAll("[^A-Za-z0-9_-]", "") + ".xlsx";
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(file).build().toString())
            .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .body(body);
    }
}
