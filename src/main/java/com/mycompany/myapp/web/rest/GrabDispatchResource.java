package com.mycompany.myapp.web.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.myapp.service.dto.order.OrderDetailDTO;
import com.mycompany.myapp.service.partner.AhamoveDispatchService;
import com.mycompany.myapp.service.partner.GrabDispatchService;
import com.mycompany.myapp.service.partner.GrabExpressClient;
import com.mycompany.myapp.web.rest.errors.BadRequestAlertException;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GrabDispatchResource {

    private static final Logger LOG = LoggerFactory.getLogger(GrabDispatchResource.class);

    private final GrabDispatchService grabDispatchService;
    private final GrabExpressClient grabExpressClient;
    private final ObjectMapper objectMapper;

    public GrabDispatchResource(GrabDispatchService grabDispatchService, GrabExpressClient grabExpressClient, ObjectMapper objectMapper) {
        this.grabDispatchService = grabDispatchService;
        this.grabExpressClient = grabExpressClient;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/api/orders/{orderCode}/grab/dispatch")
    public OrderDetailDTO dispatch(@PathVariable String orderCode, @RequestBody AhamoveDispatchService.DispatchRequest request) {
        return grabDispatchService.dispatch(orderCode, request);
    }

    @PostMapping("/api/orders/{orderCode}/grab/dispatch-pickup")
    public OrderDetailDTO dispatchPickup(@PathVariable String orderCode, @RequestBody AhamoveDispatchService.DispatchRequest request) {
        return grabDispatchService.dispatchPickup(orderCode, request);
    }

    @PostMapping("/api/orders/{orderCode}/grab/cancel")
    public OrderDetailDTO cancel(@PathVariable String orderCode, @RequestBody(required = false) Map<String, String> body) {
        return grabDispatchService.cancel(orderCode, body != null ? body.get("reason") : null);
    }

    @PostMapping("/api/grab/estimate")
    public Map<String, Object> estimate(@RequestBody Map<String, Object> body) {
        Double officeLat = num(body, "officeLat");
        Double officeLng = num(body, "officeLng");
        Double pinLat = num(body, "pinLat");
        Double pinLng = num(body, "pinLng");
        String address = body.get("pinAddress") == null ? null : String.valueOf(body.get("pinAddress"));
        String bulky = body.get("bulkyTier") == null ? null : String.valueOf(body.get("bulkyTier"));
        String orderCode = body.get("orderCode") == null ? null : String.valueOf(body.get("orderCode"));
        GrabExpressClient.Quote quote = grabExpressClient == null
            ? null
            : grabDispatchService.estimate(officeLat, officeLng, pinLat, pinLng, address, bulky, orderCode);
        return Map.of(
            "totalPrice",
            quote.amount() == null ? 0 : quote.amount(),
            "distanceKm",
            quote.distanceKm() == null ? 0 : quote.distanceKm(),
            "vehicleType",
            GrabExpressClient.vehicleOf(bulky)
        );
    }

    @PostMapping("/api/public/grab/webhook")
    public ResponseEntity<Map<String, Object>> webhook(
        @RequestParam(value = "token", required = false) String token,
        @RequestHeader(value = "Authorization", required = false) String authorization,
        @RequestBody(required = false) byte[] rawBody
    ) {
        if (!grabDispatchService.webhookAuthorized(token, authorization)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("ok", false));
        }
        try {
            JsonNode body = rawBody == null || rawBody.length == 0 ? objectMapper.createObjectNode() : objectMapper.readTree(rawBody);
            grabDispatchService.applyWebhook(body);
            return ResponseEntity.ok(Map.of("ok", true));
        } catch (Exception e) {
            LOG.warn("Grab webhook: {}", e.getMessage());
            throw new BadRequestAlertException("Webhook Grab không đọc được", "grab", "grabWebhook");
        }
    }

    private static Double num(Map<String, Object> body, String key) {
        Object v = body == null ? null : body.get(key);
        if (v instanceof Number n) {
            return n.doubleValue();
        }
        if (v == null || String.valueOf(v).isBlank()) {
            return null;
        }
        try {
            return Double.valueOf(String.valueOf(v));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
