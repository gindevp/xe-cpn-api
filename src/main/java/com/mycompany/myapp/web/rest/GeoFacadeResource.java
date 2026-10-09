package com.mycompany.myapp.web.rest;

import com.mycompany.myapp.service.geo.GoogleMapsLinkService;
import com.mycompany.myapp.service.partner.OsmNominatimService;
import com.mycompany.myapp.web.rest.errors.BadRequestAlertException;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Tìm / định vị địa chỉ qua Photon (OpenStreetMap). Không gọi Goong. */
@RestController
public class GeoFacadeResource {

    private final OsmNominatimService osmNominatimService;
    private final GoogleMapsLinkService googleMapsLinkService;

    public GeoFacadeResource(OsmNominatimService osmNominatimService, GoogleMapsLinkService googleMapsLinkService) {
        this.osmNominatimService = osmNominatimService;
        this.googleMapsLinkService = googleMapsLinkService;
    }

    @GetMapping("/api/geo/autocomplete")
    public List<Map<String, String>> autocomplete(@RequestParam("q") String q) {
        return osmNominatimService.autocomplete(q);
    }

    @GetMapping("/api/geo/place")
    public Map<String, Object> place(@RequestParam("placeId") String placeId) {
        return osmNominatimService.placeDetail(placeId);
    }

    @GetMapping("/api/geo/reverse")
    public Map<String, Object> reverse(@RequestParam("lat") double lat, @RequestParam("lng") double lng) {
        return osmNominatimService.reverse(lat, lng);
    }

    /** Link rút gọn maps.app.goo.gl → GPS địa điểm sau khi redirect. */
    @GetMapping("/api/geo/maps-link")
    public Map<String, Object> mapsLink(@RequestParam("url") String url) {
        GoogleMapsLinkService.Pin pin = googleMapsLinkService.resolve(url);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("lat", pin.lat());
        out.put("lng", pin.lng());
        return out;
    }

    /** Định vị theo địa chỉ đầy đủ (số nhà + phường/xã + …) trên OSM. */
    @GetMapping("/api/geo/geocode")
    public Map<String, Object> geocode(@RequestParam("address") String address) {
        String addr = address == null ? "" : address.trim();
        if (addr.length() < 3) {
            throw new BadRequestAlertException("Địa chỉ quá ngắn để định vị", "geo", "geocodeShort");
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
