package com.mycompany.myapp.web.rest;

import com.mycompany.myapp.service.partner.AhamoveOrderClient;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AhamoveFacadeResource {

    private final AhamoveOrderClient ahamoveOrderClient;

    public AhamoveFacadeResource(AhamoveOrderClient ahamoveOrderClient) {
        this.ahamoveOrderClient = ahamoveOrderClient;
    }

    @GetMapping("/api/ahamove/services")
    public List<Map<String, Object>> services(
        @RequestParam("lat") double lat,
        @RequestParam("lng") double lng,
        @RequestParam(value = "deliveryType", defaultValue = "INSTANT") String deliveryType
    ) {
        return ahamoveOrderClient.listServices(lat, lng, deliveryType);
    }

    /**
     * KM giữa VP gửi và điểm lấy/giao (GPS pin).
     * Body: officeLat, officeLng, officeAddress?, pinLat, pinLng, pinAddress?
     */
    @PostMapping("/api/ahamove/estimate-pickup-km")
    public Map<String, Object> estimatePickupKm(@RequestBody Map<String, Object> body) {
        double officeLat = toDouble(body.get("officeLat"));
        double officeLng = toDouble(body.get("officeLng"));
        double pinLat = toDouble(body.get("pinLat"));
        double pinLng = toDouble(body.get("pinLng"));
        String officeAddress = body.get("officeAddress") != null ? String.valueOf(body.get("officeAddress")) : null;
        String pinAddress = body.get("pinAddress") != null ? String.valueOf(body.get("pinAddress")) : null;
        return ahamoveOrderClient.estimatePickupDistance(officeLat, officeLng, officeAddress, pinLat, pinLng, pinAddress);
    }

    private static double toDouble(Object v) {
        if (v instanceof Number n) {
            return n.doubleValue();
        }
        return Double.parseDouble(String.valueOf(v));
    }
}
