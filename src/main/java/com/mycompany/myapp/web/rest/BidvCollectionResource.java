package com.mycompany.myapp.web.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.myapp.security.ClientIpResolver;
import com.mycompany.myapp.service.payment.bidv.BidvCallLogService;
import com.mycompany.myapp.service.payment.bidv.BidvCollectionService;
import com.mycompany.myapp.service.payment.bidv.BidvCollectionService.CallContext;
import com.mycompany.myapp.service.payment.bidv.BidvCollectionService.Outcome;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.function.BiFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * BIDV gọi vào (không đăng nhập): xác thực bằng IP + API key + checksum trong {@link BidvCollectionService}.
 * Luôn trả HTTP 200 kèm {@code result_code} theo bảng mã BIDV.
 */
@RestController
public class BidvCollectionResource {

    private static final Logger LOG = LoggerFactory.getLogger(BidvCollectionResource.class);

    private final BidvCollectionService collectionService;
    private final BidvCallLogService callLogService;
    private final ObjectMapper objectMapper;

    public BidvCollectionResource(BidvCollectionService collectionService, BidvCallLogService callLogService, ObjectMapper objectMapper) {
        this.collectionService = collectionService;
        this.callLogService = callLogService;
        this.objectMapper = objectMapper;
    }

    // /api/payment/bidv/*: cùng đường dẫn cổng vé xe XEVN, phòng BIDV chỉ đăng ký domain và giữ nguyên path.
    @PostMapping(
        value = { "/api/public/bidv/getbill", "/api/public/bidv/get-bill", "/api/payment/bidv/getbill" },
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<Map<String, Object>> getBill(@RequestBody(required = false) byte[] rawBody, HttpServletRequest http) {
        return handle("GETBILL", rawBody, http, collectionService::getBill);
    }

    @PostMapping(
        value = { "/api/public/bidv/paybill", "/api/public/bidv/pay-bill", "/api/payment/bidv/paybill" },
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<Map<String, Object>> payBill(@RequestBody(required = false) byte[] rawBody, HttpServletRequest http) {
        return handle("PAYBILL", rawBody, http, collectionService::payBill);
    }

    private ResponseEntity<Map<String, Object>> handle(
        String endpoint,
        byte[] rawBody,
        HttpServletRequest http,
        BiFunction<JsonNode, CallContext, Outcome> action
    ) {
        long started = System.currentTimeMillis();
        String ip = ClientIpResolver.resolve(http);
        String raw = rawBody == null ? "" : new String(rawBody, StandardCharsets.UTF_8);
        CallContext ctx = new CallContext(ip, apiKey(http));
        JsonNode req;
        try {
            req = raw.isBlank() ? null : objectMapper.readTree(raw);
        } catch (Exception e) {
            req = null;
        }
        Outcome outcome;
        if (req == null || !req.isObject()) {
            outcome = BidvCollectionService.fail(BidvCollectionService.MISSING_PARAM, "Request JSON khong hop le", null, null, null, null);
        } else {
            try {
                outcome = action.apply(req, ctx);
            } catch (RuntimeException e) {
                LOG.warn("BIDV {} failed: {}", endpoint, e.getMessage());
                outcome = BidvCollectionService.fail(
                    BidvCollectionService.SYSTEM_ERROR,
                    "Co loi phat sinh tu he thong",
                    null,
                    null,
                    null,
                    null
                );
            }
        }
        try {
            callLogService.record(
                new BidvCallLogService.Entry(
                    endpoint,
                    ip,
                    outcome.customerId(),
                    outcome.transId(),
                    outcome.amount(),
                    outcome.resultCode(),
                    outcome.resultDesc(),
                    outcome.checksumVariant(),
                    System.currentTimeMillis() - started,
                    raw,
                    objectMapper.writeValueAsString(outcome.body())
                )
            );
        } catch (Exception e) {
            LOG.warn("BIDV {} log failed: {}", endpoint, e.getMessage());
        }
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(outcome.body());
    }

    private static String apiKey(HttpServletRequest http) {
        String key = http.getHeader("X-API-KEY");
        if (key != null && !key.isBlank()) {
            return key.trim();
        }
        String auth = http.getHeader("Authorization");
        if (auth != null && auth.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return auth.substring(7).trim();
        }
        return null;
    }
}
