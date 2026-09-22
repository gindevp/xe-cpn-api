package com.mycompany.myapp.web.rest;

import com.mycompany.myapp.service.partner.OsmNominatimService;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GeoFacadeResource {

    private final OsmNominatimService osmNominatimService;

    public GeoFacadeResource(OsmNominatimService osmNominatimService) {
        this.osmNominatimService = osmNominatimService;
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
}
