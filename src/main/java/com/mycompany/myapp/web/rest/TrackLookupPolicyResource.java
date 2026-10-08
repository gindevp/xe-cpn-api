package com.mycompany.myapp.web.rest;

import com.mycompany.myapp.service.config.TrackLookupLimitService;
import com.mycompany.myapp.service.dto.TrackLookupPolicyDTO;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class TrackLookupPolicyResource {

    private final TrackLookupLimitService trackLookupLimitService;

    public TrackLookupPolicyResource(TrackLookupLimitService trackLookupLimitService) {
        this.trackLookupLimitService = trackLookupLimitService;
    }

    @GetMapping("/api/admin/track-lookup-policy")
    public TrackLookupPolicyDTO getPolicy() {
        return trackLookupLimitService.getPolicy();
    }

    @PutMapping("/api/admin/track-lookup-policy")
    public TrackLookupPolicyDTO putPolicy(@RequestBody TrackLookupPolicyDTO body) {
        try {
            return trackLookupLimitService.putPolicy(body);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }
}
