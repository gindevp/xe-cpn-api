package com.mycompany.myapp.web.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.mycompany.myapp.IntegrationTest;
import com.mycompany.myapp.repository.MaintenancePolicyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@IntegrationTest
@AutoConfigureMockMvc
class MaintenancePolicyResourceIT {

    private static final String PUBLIC_URL = "/api/maintenance";
    private static final String ADMIN_URL = "/api/admin/maintenance";

    @Autowired
    private MockMvc restMockMvc;

    @Autowired
    private MaintenancePolicyRepository repository;

    @BeforeEach
    @Transactional
    void clearPolicy() {
        repository.deleteAll();
    }

    @Test
    @Transactional
    @WithAnonymousUser
    void publicGetWorksWithoutToken() throws Exception {
        restMockMvc
            .perform(get(PUBLIC_URL))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.enabled").value(false))
            .andExpect(jsonPath("$.blockAll").value(false));
    }

    @Test
    @Transactional
    @WithMockUser(authorities = "ROLE_ADMIN")
    void adminPutThenPublicGetReturnsPolicy() throws Exception {
        restMockMvc
            .perform(
                put(ADMIN_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"enabled\":true,\"blockAll\":false,\"blockAppStaff\":true,\"blockAppCustomer\":false," +
                        "\"blockWebStaff\":true,\"blockWebCustomer\":false,\"title\":\"Bảo trì\",\"message\":\"Quay lại sau\"}"
                    )
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.enabled").value(true))
            .andExpect(jsonPath("$.blockAppStaff").value(true))
            .andExpect(jsonPath("$.blockWebStaff").value(true));

        restMockMvc
            .perform(get(PUBLIC_URL))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.enabled").value(true))
            .andExpect(jsonPath("$.title").value("Bảo trì"));
    }
}
