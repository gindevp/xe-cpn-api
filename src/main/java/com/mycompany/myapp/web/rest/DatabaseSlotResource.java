package com.mycompany.myapp.web.rest;

import com.mycompany.myapp.service.config.DatabaseCutoverService;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DatabaseSlotResource {

    private final DatabaseCutoverService databaseCutoverService;

    public DatabaseSlotResource(DatabaseCutoverService databaseCutoverService) {
        this.databaseCutoverService = databaseCutoverService;
    }

    @GetMapping("/api/integration-config/databases")
    public Map<String, Object> board() {
        return databaseCutoverService.board();
    }

    @PutMapping("/api/integration-config/databases/{slot}")
    public Map<String, Object> save(@PathVariable String slot, @RequestBody Map<String, String> body) {
        Map<String, String> in = body == null ? Map.of() : body;
        return databaseCutoverService.save(slot, in.get("label"), in.get("jdbcUrl"), in.get("username"), in.get("password"));
    }

    @PostMapping("/api/integration-config/databases/{slot}/test")
    public Map<String, Object> test(@PathVariable String slot) {
        return databaseCutoverService.test(slot);
    }

    @PostMapping("/api/integration-config/databases/switch")
    public Map<String, Object> startSwitch(@RequestBody Map<String, String> body) {
        String target = body == null ? null : body.get("target");
        return databaseCutoverService.startSwitch(target);
    }

    @GetMapping("/api/integration-config/databases/switch")
    public Map<String, Object> switchStatus() {
        return databaseCutoverService.statusBody();
    }
}
