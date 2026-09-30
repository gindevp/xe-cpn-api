package com.mycompany.myapp.web.rest;

import com.mycompany.myapp.domain.IntegrationConfig;
import com.mycompany.myapp.domain.SurchargePolicy;
import com.mycompany.myapp.service.config.AutoCallConfigService;
import com.mycompany.myapp.service.config.ConfigFacadeService;
import com.mycompany.myapp.service.partner.HhvnAutoCallClient.FileResult;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class ConfigFacadeResource {

    private final ConfigFacadeService configFacadeService;
    private final AutoCallConfigService autoCallConfigService;

    public ConfigFacadeResource(ConfigFacadeService configFacadeService, AutoCallConfigService autoCallConfigService) {
        this.configFacadeService = configFacadeService;
        this.autoCallConfigService = autoCallConfigService;
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

    @PostMapping("/api/integration-config/test-ahamove")
    public Map<String, Object> testAhamove(@RequestBody(required = false) IntegrationConfig body) {
        return configFacadeService.testAhamove(body);
    }

    @PostMapping("/api/integration-config/test-autocall")
    public Map<String, Object> testAutoCall(@RequestBody(required = false) Map<String, String> body) {
        return autoCallConfigService.test(body);
    }

    @GetMapping("/api/integration-config/autocall/audios")
    public Map<String, Object> listAutoCallAudios() {
        return autoCallConfigService.listAudios();
    }

    @PutMapping(value = "/api/integration-config/autocall/audios/{type}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> uploadAutoCallAudio(@PathVariable("type") String type, @RequestParam("file") MultipartFile file) {
        return autoCallConfigService.uploadAudio(type, file);
    }

    @DeleteMapping("/api/integration-config/autocall/audios/{type}")
    public Map<String, Object> deleteAutoCallAudio(@PathVariable("type") String type) {
        return autoCallConfigService.deleteAudio(type);
    }

    @GetMapping("/api/integration-config/autocall/audios/{type}/file")
    public ResponseEntity<byte[]> downloadAutoCallAudio(@PathVariable("type") String type) {
        FileResult f = autoCallConfigService.downloadAudio(type);
        if (!f.result().ok()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_GATEWAY,
                AutoCallConfigService.humanMessage(f.result().code(), f.result().message())
            );
        }
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(f.contentType()))
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + type + ".wav\"")
            .body(f.content());
    }
}
