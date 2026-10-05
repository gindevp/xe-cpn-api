package com.mycompany.myapp.web.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.myapp.service.dto.order.OrderDetailDTO;
import com.mycompany.myapp.service.partner.AhamoveDispatchService;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Giao tận nơi qua Ahamove (bàn giao shipper) + callback trạng thái. */
@RestController
public class AhamoveDispatchResource {

    private static final Logger LOG = LoggerFactory.getLogger(AhamoveDispatchResource.class);

    private final AhamoveDispatchService ahamoveDispatchService;
    private final ObjectMapper objectMapper;

    public AhamoveDispatchResource(AhamoveDispatchService ahamoveDispatchService, ObjectMapper objectMapper) {
        this.ahamoveDispatchService = ahamoveDispatchService;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/api/orders/{orderCode}/ahamove/dispatch")
    public OrderDetailDTO dispatch(@PathVariable String orderCode, @RequestBody AhamoveDispatchService.DispatchRequest request) {
        return ahamoveDispatchService.dispatch(orderCode, request);
    }

    @PostMapping("/api/orders/{orderCode}/ahamove/cancel")
    public OrderDetailDTO cancel(@PathVariable String orderCode, @RequestBody(required = false) Map<String, String> body) {
        return ahamoveDispatchService.cancel(orderCode, body != null ? body.get("reason") : null);
    }

    /** NV quầy xác nhận đã nhận tiền mặt tài xế Ahamove ứng. */
    @PostMapping("/api/orders/{orderCode}/ahamove/advance-in")
    public OrderDetailDTO advanceIn(@PathVariable String orderCode) {
        return ahamoveDispatchService.confirmAdvance(orderCode);
    }

    /** Trả lại tiền ứng cho tài xế khi giao không được. */
    @PostMapping("/api/orders/{orderCode}/ahamove/advance-refund")
    public OrderDetailDTO advanceRefund(@PathVariable String orderCode) {
        return ahamoveDispatchService.refundAdvance(orderCode);
    }

    @PostMapping("/api/public/ahamove/webhook")
    public ResponseEntity<Map<String, Object>> webhook(
        @RequestParam(value = "token", required = false) String token,
        @RequestHeader(value = "apikey", required = false) String apikey,
        @RequestBody(required = false) byte[] rawBody
    ) {
        if (!ahamoveDispatchService.webhookTokenValid(token, apikey)) {
            LOG.warn("Ahamove webhook rejected: bad/missing token");
            return ResponseEntity.status(401).build();
        }
        JsonNode body;
        try {
            body = objectMapper.readTree(rawBody == null ? new byte[0] : rawBody);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
        Optional<String> code = ahamoveDispatchService.applyWebhook(body);
        return ResponseEntity.ok(Map.of("ok", true, "matched", code.isPresent()));
    }
}
