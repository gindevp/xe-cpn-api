package com.mycompany.myapp.web.rest;

import com.mycompany.myapp.service.order.CodPaymentRequestService;
import java.nio.charset.StandardCharsets;
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

    /** Giấy đề nghị thanh toán COD (mẫu BMTT-01) của 1 đơn — trang HTML để FE mở hộp thoại in. */
    @GetMapping("/payment-request")
    public ResponseEntity<byte[]> paymentRequest(@RequestParam String code) {
        return ResponseEntity.ok()
            .contentType(new MediaType(MediaType.TEXT_HTML, StandardCharsets.UTF_8))
            .body(codPaymentRequestService.buildHtml(code).getBytes(StandardCharsets.UTF_8));
    }
}
