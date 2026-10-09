package com.mycompany.myapp.web.rest;

import com.mycompany.myapp.repository.ShipmentOrderRepository;
import com.mycompany.myapp.service.partner.AhamoveCargo;
import com.mycompany.myapp.service.partner.AhamoveOrderClient;
import com.mycompany.myapp.web.rest.errors.BadRequestAlertException;
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
    private final ShipmentOrderRepository shipmentOrderRepository;

    public AhamoveFacadeResource(AhamoveOrderClient ahamoveOrderClient, ShipmentOrderRepository shipmentOrderRepository) {
        this.ahamoveOrderClient = ahamoveOrderClient;
        this.shipmentOrderRepository = shipmentOrderRepository;
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
     * Body: officeLat, officeLng, officeAddress?, pinLat?, pinLng?, pinAddress? — thiếu pin GPS thì bắt buộc pinAddress.
     */
    @PostMapping("/api/ahamove/estimate-pickup-km")
    public Map<String, Object> estimatePickupKm(@RequestBody Map<String, Object> body) {
        double officeLat = toDouble(body.get("officeLat"));
        double officeLng = toDouble(body.get("officeLng"));
        Double pinLat = body.get("pinLat") != null ? toDouble(body.get("pinLat")) : null;
        Double pinLng = body.get("pinLng") != null ? toDouble(body.get("pinLng")) : null;
        String officeAddress = body.get("officeAddress") != null ? String.valueOf(body.get("officeAddress")) : null;
        String pinAddress = body.get("pinAddress") != null ? String.valueOf(body.get("pinAddress")).trim() : null;
        if ((pinLat == null || pinLng == null) && (pinAddress == null || pinAddress.isEmpty())) {
            throw new BadRequestAlertException("Cần GPS điểm giao hoặc địa chỉ giao", "ahamove", "ahamoveEstimateTarget");
        }
        if (pinLat == null || pinLng == null) {
            pinLat = null;
            pinLng = null;
        }
        String orderCode = body.get("orderCode") != null ? String.valueOf(body.get("orderCode")).trim() : "";
        AhamoveCargo cargo = orderCode.isEmpty()
            ? null
            : shipmentOrderRepository.findOneByOrderCodeOrDraftCode(orderCode).map(AhamoveCargo::from).orElse(null);
        if (body.containsKey("bulkyTier")) {
            String choice = body.get("bulkyTier") == null ? "" : String.valueOf(body.get("bulkyTier"));
            cargo = (cargo == null ? AhamoveCargo.from(null) : cargo).withTierChoice(choice);
        }
        return ahamoveOrderClient.estimatePickupDistance(officeLat, officeLng, officeAddress, pinLat, pinLng, pinAddress, cargo);
    }

    private static double toDouble(Object v) {
        if (v instanceof Number n) {
            return n.doubleValue();
        }
        return Double.parseDouble(String.valueOf(v));
    }
}
