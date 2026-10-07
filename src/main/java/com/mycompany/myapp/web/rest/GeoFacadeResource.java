package com.mycompany.myapp.web.rest;

import com.mycompany.myapp.service.partner.GoongPlacesService;
import com.mycompany.myapp.service.partner.OsmNominatimService;
import com.mycompany.myapp.web.rest.errors.BadRequestAlertException;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GeoFacadeResource {

    private static final Logger LOG = LoggerFactory.getLogger(GeoFacadeResource.class);

    private final OsmNominatimService osmNominatimService;
    private final GoongPlacesService goongPlacesService;

    public GeoFacadeResource(OsmNominatimService osmNominatimService, GoongPlacesService goongPlacesService) {
        this.osmNominatimService = osmNominatimService;
        this.goongPlacesService = goongPlacesService;
    }

    @GetMapping("/api/geo/autocomplete")
    public List<Map<String, String>> autocomplete(@RequestParam("q") String q) {
        if (goongPlacesService.hasApiKey()) {
            try {
                return goongPlacesService.autocomplete(q);
            } catch (Exception e) {
                LOG.warn("Goong autocomplete fallback Photon: {}", e.getMessage());
            }
        }
        return osmNominatimService.autocomplete(q);
    }

    @GetMapping("/api/geo/place")
    public Map<String, Object> place(@RequestParam("placeId") String placeId) {
        if (placeId != null && placeId.startsWith("photon:")) {
            return osmNominatimService.placeDetail(placeId);
        }
        if (goongPlacesService.hasApiKey()) {
            return goongPlacesService.placeDetail(placeId);
        }
        return osmNominatimService.placeDetail(placeId);
    }

    @GetMapping("/api/geo/reverse")
    public Map<String, Object> reverse(@RequestParam("lat") double lat, @RequestParam("lng") double lng) {
        return osmNominatimService.reverse(lat, lng);
    }

    /**
     * Định vị theo địa chỉ đầy đủ (số nhà + phường/xã + …).
     * Ưu tiên Goong khi đã cấu hình key; không có thì Photon với cùng chuỗi địa chỉ.
     */
    @GetMapping("/api/geo/geocode")
    public Map<String, Object> geocode(@RequestParam("address") String address) {
        String addr = address == null ? "" : address.trim();
        if (addr.length() < 3) {
            throw new BadRequestAlertException("Địa chỉ quá ngắn để định vị", "geo", "geocodeShort");
        }
        if (goongPlacesService.hasApiKey()) {
            try {
                return goongPlacesService.geocode(addr);
            } catch (BadRequestAlertException e) {
                LOG.warn("Goong geocode miss, fallback Photon: {}", e.getMessage());
            } catch (Exception e) {
                LOG.warn("Goong geocode error, fallback Photon: {}", e.getMessage());
            }
        }
        List<Map<String, String>> rows = osmNominatimService.autocomplete(addr);
        for (Map<String, String> row : rows) {
            BigDecimal lat = parseDecimal(row.get("lat"));
            BigDecimal lng = parseDecimal(row.get("lng"));
            if (lat != null && lng != null) {
                Map<String, Object> out = new LinkedHashMap<>();
                out.put("address", row.getOrDefault("description", addr));
                out.put("lat", lat);
                out.put("lng", lng);
                out.put("placeId", row.get("placeId"));
                return out;
            }
        }
        throw new BadRequestAlertException("Không định vị được địa chỉ", "geo", "geocodeEmpty");
    }

    private static BigDecimal parseDecimal(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(raw.trim());
        } catch (Exception e) {
            return null;
        }
    }
}
