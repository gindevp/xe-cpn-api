package com.mycompany.myapp.web.rest;

import com.mycompany.myapp.domain.IntegrationConfig;
import com.mycompany.myapp.domain.SurchargePolicy;
import com.mycompany.myapp.service.config.ConfigFacadeService;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
public class ConfigFacadeResource {

    private final ConfigFacadeService configFacadeService;

    public ConfigFacadeResource(ConfigFacadeService configFacadeService) {
        this.configFacadeService = configFacadeService;
    }

    @GetMapping("/api/surcharge-policy")
    public SurchargePolicy getSurcharge() {
        return configFacadeService.getSurchargePolicy();
    }

    @PutMapping("/api/surcharge-policy")
    public SurchargePolicy putSurcharge(@RequestBody SurchargePolicy body) {
        return configFacadeService.putSurchargePolicy(body);
    }

    @GetMapping("/api/integration-config")
    public IntegrationConfig getIntegration() {
        return configFacadeService.getIntegrationConfig();
    }

    @PutMapping("/api/integration-config")
    public IntegrationConfig putIntegration(@RequestBody IntegrationConfig body) {
        return configFacadeService.putIntegrationConfig(body);
    }

    @PostMapping("/api/integration-config/test")
    public Map<String, Object> testIntegration() {
        return configFacadeService.testIntegration();
    }
}
