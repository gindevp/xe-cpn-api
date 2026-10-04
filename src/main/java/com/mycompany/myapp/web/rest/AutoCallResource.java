package com.mycompany.myapp.web.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.myapp.security.SecurityUtils;
import com.mycompany.myapp.security.StaffAccessService;
import com.mycompany.myapp.service.autocall.AutoCallConsoleService;
import com.mycompany.myapp.service.autocall.AutoCallService;
import com.mycompany.myapp.service.autocall.AutoCallService.AutoCallView;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class AutoCallResource {

    private static final Logger LOG = LoggerFactory.getLogger(AutoCallResource.class);

    private final AutoCallService autoCallService;
    private final AutoCallConsoleService consoleService;
    private final ObjectMapper objectMapper;
    private final StaffAccessService staffAccessService;

    public AutoCallResource(
        AutoCallService autoCallService,
        AutoCallConsoleService consoleService,
        ObjectMapper objectMapper,
        StaffAccessService staffAccessService
    ) {
        this.autoCallService = autoCallService;
        this.consoleService = consoleService;
        this.objectMapper = objectMapper;
        this.staffAccessService = staffAccessService;
    }

    public record CatchUpRequest(List<String> orderCodes, Boolean dryRun) {}

    /** Gọi bù Auto Call cho đơn nhập kho giao chưa gọi được. {@code dryRun=true} → chỉ trả đơn đủ điều kiện. */
    @PostMapping("/api/auto-calls/catch-up")
    public AutoCallService.CatchUpResult catchUp(@RequestBody CatchUpRequest req) {
        String scoped = staffAccessService.isSystemAdmin() ? null : staffAccessService.scopedOfficeCode().orElse(null);
        return autoCallService.catchUp(
            req != null ? req.orderCodes() : List.of(),
            scoped,
            req != null && Boolean.TRUE.equals(req.dryRun()),
            SecurityUtils.getCurrentUserLogin().orElse(null)
        );
    }

    @GetMapping("/api/orders/{code}/auto-calls")
    public List<AutoCallView> list(@PathVariable("code") String code) {
        return autoCallService.list(code);
    }

    @PostMapping("/api/orders/{code}/auto-calls/sync")
    public List<AutoCallView> sync(@PathVariable("code") String code) {
        return autoCallService.sync(code);
    }

    @PostMapping("/api/orders/{code}/auto-calls/{id}/cancel")
    public List<AutoCallView> cancel(@PathVariable("code") String code, @PathVariable("id") Long id) {
        return autoCallService.cancel(code, id);
    }

    @PostMapping("/api/orders/{code}/auto-calls/{id}/stop-retry")
    public List<AutoCallView> stopRetry(@PathVariable("code") String code, @PathVariable("id") Long id) {
        return autoCallService.stopRetry(code, id);
    }

    @GetMapping("/api/integration-config/autocall/calls")
    public Map<String, Object> listCalls(
        @RequestParam(value = "from", required = false) String from,
        @RequestParam(value = "to", required = false) String to,
        @RequestParam(value = "type", required = false) String type,
        @RequestParam(value = "status", required = false) String status,
        @RequestParam(value = "result", required = false) String result,
        @RequestParam(value = "phone", required = false) String phone,
        @RequestParam(value = "page", required = false) Integer page,
        @RequestParam(value = "limit", required = false) Integer limit
    ) {
        return consoleService.listCalls(from, to, type, status, result, phone, page, limit);
    }

    /** {@code q} = callId ({@code call_…}) hoặc refId. */
    @GetMapping("/api/integration-config/autocall/calls/lookup")
    public Map<String, Object> lookupCall(@RequestParam("q") String q) {
        return consoleService.getCall(q);
    }

    @PostMapping("/api/integration-config/autocall/calls/{callId}/cancel")
    public Map<String, Object> cancelCall(@PathVariable("callId") String callId) {
        return consoleService.cancel(callId);
    }

    @PostMapping("/api/integration-config/autocall/test-call")
    public Map<String, Object> testCall(@RequestBody Map<String, Object> body) {
        String phone = body.get("phone") != null ? body.get("phone").toString() : null;
        String type = body.get("type") != null ? body.get("type").toString() : null;
        boolean confirmLive = Boolean.TRUE.equals(body.get("confirmLive"));
        return consoleService.testCall(phone, type, confirmLive);
    }

    /**
     * Webhook call.finished của HHVN — không cần đăng nhập, bắt buộc chữ ký HMAC.
     * Body giữ nguyên dạng byte để tính chữ ký trên đúng chuỗi HHVN đã ký.
     */
    @PostMapping("/api/public/hhvn/webhook")
    public ResponseEntity<Void> webhook(
        @RequestHeader(value = "X-HHVN-Timestamp", required = false) String timestamp,
        @RequestHeader(value = "X-HHVN-Signature", required = false) String signature,
        @RequestHeader(value = "X-HHVN-Delivery", required = false) String delivery,
        @RequestBody(required = false) byte[] rawBody
    ) {
        String secret = autoCallService.webhookSecret();
        if (secret == null) {
            LOG.warn("HHVN webhook {} rejected: webhook secret not configured", delivery);
            return ResponseEntity.status(503).build();
        }
        if (!AutoCallService.verifySignature(secret, timestamp, rawBody, signature, Instant.now().getEpochSecond())) {
            LOG.warn("HHVN webhook {} rejected: bad signature or timestamp", delivery);
            return ResponseEntity.status(401).build();
        }
        try {
            JsonNode event = objectMapper.readTree(rawBody);
            if ("call.finished".equals(event.path("event").asText())) {
                boolean found = autoCallService.applyCallObject(event.path("data"));
                if (!found) {
                    LOG.warn("HHVN webhook {}: unknown call refId={}", delivery, event.path("data").path("refId").asText());
                }
            }
        } catch (Exception e) {
            LOG.warn("HHVN webhook {} processing failed: {}", delivery, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok().build();
    }

    /**
     * Webhook {@code call.completed} của Vtech — không ký; token CPN sinh gửi kèm {@code ?token=} hoặc trường
     * {@code cpn_token} trong body_extra.
     */
    @PostMapping("/api/public/vtech/webhook")
    public ResponseEntity<Void> vtechWebhook(
        @RequestParam(value = "token", required = false) String token,
        @RequestBody(required = false) byte[] rawBody
    ) {
        String expected = autoCallService.vtechWebhookToken();
        if (expected == null) {
            LOG.warn("Vtech webhook rejected: webhook token not configured");
            return ResponseEntity.status(503).build();
        }
        JsonNode event;
        try {
            event = objectMapper.readTree(rawBody == null ? new byte[0] : rawBody);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
        String given = token != null ? token : event != null ? event.path("cpn_token").asText(null) : null;
        if (!AutoCallService.tokenMatches(expected, given)) {
            LOG.warn("Vtech webhook rejected: bad token");
            return ResponseEntity.status(401).build();
        }
        try {
            AutoCallService.VtechWebhookOutcome outcome = autoCallService.applyVtechWebhook(event);
            if (outcome == AutoCallService.VtechWebhookOutcome.NOT_FOUND) {
                JsonNode c = event.path("call");
                LOG.warn(
                    "Vtech webhook: unknown call id={} ref={} phone={}",
                    c.path("id").asText(),
                    c.path("contact").path("extra_data").path("ref_id").asText(),
                    c.path("contact").path("phone_number").asText()
                );
            }
        } catch (Exception e) {
            LOG.warn("Vtech webhook processing failed: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok().build();
    }
}
