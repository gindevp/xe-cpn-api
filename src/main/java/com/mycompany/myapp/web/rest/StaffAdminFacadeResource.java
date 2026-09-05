package com.mycompany.myapp.web.rest;

import com.mycompany.myapp.service.staff.StaffAdminFacadeService;
import com.mycompany.myapp.service.staff.StaffAdminFacadeService.StaffUserDTO;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/staff-admin/users")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class StaffAdminFacadeResource {

    private final StaffAdminFacadeService staffAdminFacadeService;

    public StaffAdminFacadeResource(StaffAdminFacadeService staffAdminFacadeService) {
        this.staffAdminFacadeService = staffAdminFacadeService;
    }

    @GetMapping
    public List<StaffUserDTO> list() {
        return staffAdminFacadeService.list();
    }

    @PutMapping
    public StaffUserDTO upsert(@RequestBody StaffUserDTO body) {
        return staffAdminFacadeService.upsert(body);
    }

    @DeleteMapping("/{login}")
    public void delete(@PathVariable("login") String login) {
        staffAdminFacadeService.delete(login);
    }
}
