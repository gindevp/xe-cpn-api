package com.mycompany.myapp.web.rest;

import com.mycompany.myapp.service.config.TransitHubPolicyService;
import com.mycompany.myapp.service.dto.TransitHubPolicyDTO;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class TransitHubPolicyResource {

    private final TransitHubPolicyService transitHubPolicyService;

    public TransitHubPolicyResource(TransitHubPolicyService transitHubPolicyService) {
        this.transitHubPolicyService = transitHubPolicyService;
    }

    @GetMapping("/api/transit-hub-policy")
    public TransitHubPolicyDTO getPolicy() {
        return transitHubPolicyService.getPolicy();
    }

    @PutMapping("/api/transit-hub-policy")
    public TransitHubPolicyDTO putPolicy(@RequestBody TransitHubPolicyDTO body) {
        try {
            return transitHubPolicyService.putPolicy(body);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }
}
