package com.mycompany.myapp.web.rest;

import com.mycompany.myapp.security.ScreenKey;
import com.mycompany.myapp.security.StaffAccessService;
import com.mycompany.myapp.service.config.OfficeQrScreenService;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
public class OfficeQrScreenResource {

    private final OfficeQrScreenService officeQrScreenService;
    private final StaffAccessService staffAccessService;

    public OfficeQrScreenResource(OfficeQrScreenService officeQrScreenService, StaffAccessService staffAccessService) {
        this.officeQrScreenService = officeQrScreenService;
        this.staffAccessService = staffAccessService;
    }

    @GetMapping("/api/admin/office-screens")
    public java.util.List<OfficeQrScreenService.ScreenLink> list() {
        staffAccessService.requireScreenRead(ScreenKey.BAO_TRI);
        return officeQrScreenService.listLinks();
    }

    @PostMapping("/api/admin/office-screens/{officeCode}/rotate")
    public OfficeQrScreenService.ScreenLink rotate(@PathVariable String officeCode) {
        return officeQrScreenService.rotate(officeCode);
    }

    @PostMapping("/api/public/office-screen/{key}/pulse")
    public Map<String, Object> pulse(@PathVariable String key, @RequestBody Map<String, String> body) {
        String deviceId = body == null ? null : body.get("deviceId");
        boolean holding = body != null && "true".equalsIgnoreCase(body.get("holding"));
        return OfficeQrScreenService.pulseBody(officeQrScreenService.pulse(key, deviceId, holding));
    }

    @PostMapping("/api/public/office-screen/lookup")
    public Map<String, Object> lookup(@RequestBody Map<String, String> body, jakarta.servlet.http.HttpServletRequest http) {
        String token = body == null ? null : body.get("token");
        String query = body == null ? null : body.get("query");
        String deviceId = http.getHeader("X-Device-Id");
        var rows = officeQrScreenService.lookup(token, query, deviceId, clientIp(http));
        return Map.of("orders", rows);
    }

    private static String clientIp(jakarta.servlet.http.HttpServletRequest http) {
        String fwd = http.getHeader("X-Forwarded-For");
        if (fwd != null && !fwd.isBlank()) {
            return fwd.split(",")[0].trim();
        }
        return http.getRemoteAddr();
    }
}
