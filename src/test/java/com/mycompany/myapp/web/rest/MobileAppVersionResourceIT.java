package com.mycompany.myapp.web.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.mycompany.myapp.IntegrationTest;
import com.mycompany.myapp.repository.MobileAppVersionPolicyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for {@link MobileAppVersionResource} — chính sách bắt buộc cập nhật app mobile.
 */
@IntegrationTest
@AutoConfigureMockMvc
class MobileAppVersionResourceIT {

    private static final String PUBLIC_URL = "/api/mobile/app-version";
    private static final String ADMIN_URL = "/api/admin/mobile-app-version";

    @Autowired
    private MockMvc restMockMvc;

    @Autowired
    private MobileAppVersionPolicyRepository repository;

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
            .andExpect(jsonPath("$.minimumVersion").value("1.0.0"))
            .andExpect(jsonPath("$.minimumAndroidVersionCode").isEmpty())
            .andExpect(jsonPath("$.mandatoryUpdateEnabled").value(true));
    }

    @Test
    @Transactional
    @WithMockUser(authorities = "ROLE_ADMIN")
    void adminPutThenPublicGetReturnsNewPolicy() throws Exception {
        restMockMvc
            .perform(
                put(ADMIN_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"minimumVersion\":\"1.40.0\",\"minimumAndroidVersionCode\":40,\"mandatoryUpdateEnabled\":true}")
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.minimumVersion").value("1.40.0"));

        restMockMvc
            .perform(get(PUBLIC_URL))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.minimumVersion").value("1.40.0"))
            .andExpect(jsonPath("$.minimumAndroidVersionCode").value(40))
            .andExpect(jsonPath("$.mandatoryUpdateEnabled").value(true));
    }

    /** PUT thay toàn bộ: bỏ versionCode phải xóa được ràng buộc, không giữ giá trị cũ. */
    @Test
    @Transactional
    @WithMockUser(authorities = "ROLE_ADMIN")
    void adminPutClearsAndroidVersionCodeWhenOmitted() throws Exception {
        restMockMvc
            .perform(
                put(ADMIN_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"minimumVersion\":\"1.40.0\",\"minimumAndroidVersionCode\":40,\"mandatoryUpdateEnabled\":true}")
            )
            .andExpect(status().isOk());

        restMockMvc
            .perform(put(ADMIN_URL).contentType(MediaType.APPLICATION_JSON).content("{\"minimumVersion\":\"1.41.0\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.minimumAndroidVersionCode").isEmpty())
            .andExpect(jsonPath("$.mandatoryUpdateEnabled").value(true));
    }

    @Test
    @Transactional
    @WithMockUser(authorities = "ROLE_ADMIN")
    void adminPutRejectsMissingMinimumVersion() throws Exception {
        restMockMvc
            .perform(put(ADMIN_URL).contentType(MediaType.APPLICATION_JSON).content("{\"mandatoryUpdateEnabled\":true}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @Transactional
    @WithMockUser(authorities = "ROLE_ADMIN")
    void adminPutRejectsNonNumericVersion() throws Exception {
        restMockMvc
            .perform(put(ADMIN_URL).contentType(MediaType.APPLICATION_JSON).content("{\"minimumVersion\":\"v1.40-beta\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @Transactional
    @WithMockUser(authorities = "ROLE_USER")
    void nonAdminCannotPut() throws Exception {
        restMockMvc
            .perform(put(ADMIN_URL).contentType(MediaType.APPLICATION_JSON).content("{\"minimumVersion\":\"1.40.0\"}"))
            .andExpect(status().isForbidden());
    }
}
