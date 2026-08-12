package com.mycompany.myapp.web.rest;

import static com.mycompany.myapp.domain.IntegrationConfigAsserts.*;
import static com.mycompany.myapp.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.myapp.IntegrationTest;
import com.mycompany.myapp.domain.IntegrationConfig;
import com.mycompany.myapp.repository.IntegrationConfigRepository;
import com.mycompany.myapp.service.dto.IntegrationConfigDTO;
import com.mycompany.myapp.service.mapper.IntegrationConfigMapper;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for the {@link IntegrationConfigResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser
class IntegrationConfigResourceIT {

    private static final String DEFAULT_AHAMOVE_TOKEN = "AAAAAAAAAA";
    private static final String UPDATED_AHAMOVE_TOKEN = "BBBBBBBBBB";

    private static final String DEFAULT_GRAB_TOKEN = "AAAAAAAAAA";
    private static final String UPDATED_GRAB_TOKEN = "BBBBBBBBBB";

    private static final String DEFAULT_XANHSM_TOKEN = "AAAAAAAAAA";
    private static final String UPDATED_XANHSM_TOKEN = "BBBBBBBBBB";

    private static final String DEFAULT_DISTANCE_API_TOKEN = "AAAAAAAAAA";
    private static final String UPDATED_DISTANCE_API_TOKEN = "BBBBBBBBBB";

    private static final String DEFAULT_TELEGRAM_TOKEN = "AAAAAAAAAA";
    private static final String UPDATED_TELEGRAM_TOKEN = "BBBBBBBBBB";

    private static final String DEFAULT_TELEGRAM_CHAT_ID = "AAAAAAAAAA";
    private static final String UPDATED_TELEGRAM_CHAT_ID = "BBBBBBBBBB";

    private static final String DEFAULT_WEBHOOK_URL = "AAAAAAAAAA";
    private static final String UPDATED_WEBHOOK_URL = "BBBBBBBBBB";

    private static final String DEFAULT_WEBHOOK_SECRET = "AAAAAAAAAA";
    private static final String UPDATED_WEBHOOK_SECRET = "BBBBBBBBBB";

    private static final Instant DEFAULT_UPDATED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_UPDATED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final String ENTITY_API_URL = "/api/integration-configs";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static Random random = new Random();
    private static AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    @Autowired
    private ObjectMapper om;

    @Autowired
    private IntegrationConfigRepository integrationConfigRepository;

    @Autowired
    private IntegrationConfigMapper integrationConfigMapper;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restIntegrationConfigMockMvc;

    private IntegrationConfig integrationConfig;

    private IntegrationConfig insertedIntegrationConfig;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static IntegrationConfig createEntity() {
        return new IntegrationConfig()
            .ahamoveToken(DEFAULT_AHAMOVE_TOKEN)
            .grabToken(DEFAULT_GRAB_TOKEN)
            .xanhsmToken(DEFAULT_XANHSM_TOKEN)
            .distanceApiToken(DEFAULT_DISTANCE_API_TOKEN)
            .telegramToken(DEFAULT_TELEGRAM_TOKEN)
            .telegramChatId(DEFAULT_TELEGRAM_CHAT_ID)
            .webhookUrl(DEFAULT_WEBHOOK_URL)
            .webhookSecret(DEFAULT_WEBHOOK_SECRET)
            .updatedAt(DEFAULT_UPDATED_AT);
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static IntegrationConfig createUpdatedEntity() {
        return new IntegrationConfig()
            .ahamoveToken(UPDATED_AHAMOVE_TOKEN)
            .grabToken(UPDATED_GRAB_TOKEN)
            .xanhsmToken(UPDATED_XANHSM_TOKEN)
            .distanceApiToken(UPDATED_DISTANCE_API_TOKEN)
            .telegramToken(UPDATED_TELEGRAM_TOKEN)
            .telegramChatId(UPDATED_TELEGRAM_CHAT_ID)
            .webhookUrl(UPDATED_WEBHOOK_URL)
            .webhookSecret(UPDATED_WEBHOOK_SECRET)
            .updatedAt(UPDATED_UPDATED_AT);
    }

    @BeforeEach
    public void initTest() {
        integrationConfig = createEntity();
    }

    @AfterEach
    public void cleanup() {
        if (insertedIntegrationConfig != null) {
            integrationConfigRepository.delete(insertedIntegrationConfig);
            insertedIntegrationConfig = null;
        }
    }

    @Test
    @Transactional
    void createIntegrationConfig() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the IntegrationConfig
        IntegrationConfigDTO integrationConfigDTO = integrationConfigMapper.toDto(integrationConfig);
        var returnedIntegrationConfigDTO = om.readValue(
            restIntegrationConfigMockMvc
                .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(integrationConfigDTO)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            IntegrationConfigDTO.class
        );

        // Validate the IntegrationConfig in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedIntegrationConfig = integrationConfigMapper.toEntity(returnedIntegrationConfigDTO);
        assertIntegrationConfigUpdatableFieldsEquals(returnedIntegrationConfig, getPersistedIntegrationConfig(returnedIntegrationConfig));

        insertedIntegrationConfig = returnedIntegrationConfig;
    }

    @Test
    @Transactional
    void createIntegrationConfigWithExistingId() throws Exception {
        // Create the IntegrationConfig with an existing ID
        integrationConfig.setId(1L);
        IntegrationConfigDTO integrationConfigDTO = integrationConfigMapper.toDto(integrationConfig);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restIntegrationConfigMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(integrationConfigDTO)))
            .andExpect(status().isBadRequest());

        // Validate the IntegrationConfig in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void getAllIntegrationConfigs() throws Exception {
        // Initialize the database
        insertedIntegrationConfig = integrationConfigRepository.saveAndFlush(integrationConfig);

        // Get all the integrationConfigList
        restIntegrationConfigMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(integrationConfig.getId().intValue())))
            .andExpect(jsonPath("$.[*].ahamoveToken").value(hasItem(DEFAULT_AHAMOVE_TOKEN)))
            .andExpect(jsonPath("$.[*].grabToken").value(hasItem(DEFAULT_GRAB_TOKEN)))
            .andExpect(jsonPath("$.[*].xanhsmToken").value(hasItem(DEFAULT_XANHSM_TOKEN)))
            .andExpect(jsonPath("$.[*].distanceApiToken").value(hasItem(DEFAULT_DISTANCE_API_TOKEN)))
            .andExpect(jsonPath("$.[*].telegramToken").value(hasItem(DEFAULT_TELEGRAM_TOKEN)))
            .andExpect(jsonPath("$.[*].telegramChatId").value(hasItem(DEFAULT_TELEGRAM_CHAT_ID)))
            .andExpect(jsonPath("$.[*].webhookUrl").value(hasItem(DEFAULT_WEBHOOK_URL)))
            .andExpect(jsonPath("$.[*].webhookSecret").value(hasItem(DEFAULT_WEBHOOK_SECRET)))
            .andExpect(jsonPath("$.[*].updatedAt").value(hasItem(DEFAULT_UPDATED_AT.toString())));
    }

    @Test
    @Transactional
    void getIntegrationConfig() throws Exception {
        // Initialize the database
        insertedIntegrationConfig = integrationConfigRepository.saveAndFlush(integrationConfig);

        // Get the integrationConfig
        restIntegrationConfigMockMvc
            .perform(get(ENTITY_API_URL_ID, integrationConfig.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(integrationConfig.getId().intValue()))
            .andExpect(jsonPath("$.ahamoveToken").value(DEFAULT_AHAMOVE_TOKEN))
            .andExpect(jsonPath("$.grabToken").value(DEFAULT_GRAB_TOKEN))
            .andExpect(jsonPath("$.xanhsmToken").value(DEFAULT_XANHSM_TOKEN))
            .andExpect(jsonPath("$.distanceApiToken").value(DEFAULT_DISTANCE_API_TOKEN))
            .andExpect(jsonPath("$.telegramToken").value(DEFAULT_TELEGRAM_TOKEN))
            .andExpect(jsonPath("$.telegramChatId").value(DEFAULT_TELEGRAM_CHAT_ID))
            .andExpect(jsonPath("$.webhookUrl").value(DEFAULT_WEBHOOK_URL))
            .andExpect(jsonPath("$.webhookSecret").value(DEFAULT_WEBHOOK_SECRET))
            .andExpect(jsonPath("$.updatedAt").value(DEFAULT_UPDATED_AT.toString()));
    }

    @Test
    @Transactional
    void getNonExistingIntegrationConfig() throws Exception {
        // Get the integrationConfig
        restIntegrationConfigMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingIntegrationConfig() throws Exception {
        // Initialize the database
        insertedIntegrationConfig = integrationConfigRepository.saveAndFlush(integrationConfig);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the integrationConfig
        IntegrationConfig updatedIntegrationConfig = integrationConfigRepository.findById(integrationConfig.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedIntegrationConfig are not directly saved in db
        em.detach(updatedIntegrationConfig);
        updatedIntegrationConfig
            .ahamoveToken(UPDATED_AHAMOVE_TOKEN)
            .grabToken(UPDATED_GRAB_TOKEN)
            .xanhsmToken(UPDATED_XANHSM_TOKEN)
            .distanceApiToken(UPDATED_DISTANCE_API_TOKEN)
            .telegramToken(UPDATED_TELEGRAM_TOKEN)
            .telegramChatId(UPDATED_TELEGRAM_CHAT_ID)
            .webhookUrl(UPDATED_WEBHOOK_URL)
            .webhookSecret(UPDATED_WEBHOOK_SECRET)
            .updatedAt(UPDATED_UPDATED_AT);
        IntegrationConfigDTO integrationConfigDTO = integrationConfigMapper.toDto(updatedIntegrationConfig);

        restIntegrationConfigMockMvc
            .perform(
                put(ENTITY_API_URL_ID, integrationConfigDTO.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(integrationConfigDTO))
            )
            .andExpect(status().isOk());

        // Validate the IntegrationConfig in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedIntegrationConfigToMatchAllProperties(updatedIntegrationConfig);
    }

    @Test
    @Transactional
    void putNonExistingIntegrationConfig() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        integrationConfig.setId(longCount.incrementAndGet());

        // Create the IntegrationConfig
        IntegrationConfigDTO integrationConfigDTO = integrationConfigMapper.toDto(integrationConfig);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restIntegrationConfigMockMvc
            .perform(
                put(ENTITY_API_URL_ID, integrationConfigDTO.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(integrationConfigDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the IntegrationConfig in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchIntegrationConfig() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        integrationConfig.setId(longCount.incrementAndGet());

        // Create the IntegrationConfig
        IntegrationConfigDTO integrationConfigDTO = integrationConfigMapper.toDto(integrationConfig);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restIntegrationConfigMockMvc
            .perform(
                put(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(integrationConfigDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the IntegrationConfig in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamIntegrationConfig() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        integrationConfig.setId(longCount.incrementAndGet());

        // Create the IntegrationConfig
        IntegrationConfigDTO integrationConfigDTO = integrationConfigMapper.toDto(integrationConfig);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restIntegrationConfigMockMvc
            .perform(put(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(integrationConfigDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the IntegrationConfig in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateIntegrationConfigWithPatch() throws Exception {
        // Initialize the database
        insertedIntegrationConfig = integrationConfigRepository.saveAndFlush(integrationConfig);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the integrationConfig using partial update
        IntegrationConfig partialUpdatedIntegrationConfig = new IntegrationConfig();
        partialUpdatedIntegrationConfig.setId(integrationConfig.getId());

        partialUpdatedIntegrationConfig
            .ahamoveToken(UPDATED_AHAMOVE_TOKEN)
            .telegramChatId(UPDATED_TELEGRAM_CHAT_ID)
            .webhookSecret(UPDATED_WEBHOOK_SECRET)
            .updatedAt(UPDATED_UPDATED_AT);

        restIntegrationConfigMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedIntegrationConfig.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedIntegrationConfig))
            )
            .andExpect(status().isOk());

        // Validate the IntegrationConfig in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertIntegrationConfigUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedIntegrationConfig, integrationConfig),
            getPersistedIntegrationConfig(integrationConfig)
        );
    }

    @Test
    @Transactional
    void fullUpdateIntegrationConfigWithPatch() throws Exception {
        // Initialize the database
        insertedIntegrationConfig = integrationConfigRepository.saveAndFlush(integrationConfig);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the integrationConfig using partial update
        IntegrationConfig partialUpdatedIntegrationConfig = new IntegrationConfig();
        partialUpdatedIntegrationConfig.setId(integrationConfig.getId());

        partialUpdatedIntegrationConfig
            .ahamoveToken(UPDATED_AHAMOVE_TOKEN)
            .grabToken(UPDATED_GRAB_TOKEN)
            .xanhsmToken(UPDATED_XANHSM_TOKEN)
            .distanceApiToken(UPDATED_DISTANCE_API_TOKEN)
            .telegramToken(UPDATED_TELEGRAM_TOKEN)
            .telegramChatId(UPDATED_TELEGRAM_CHAT_ID)
            .webhookUrl(UPDATED_WEBHOOK_URL)
            .webhookSecret(UPDATED_WEBHOOK_SECRET)
            .updatedAt(UPDATED_UPDATED_AT);

        restIntegrationConfigMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedIntegrationConfig.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedIntegrationConfig))
            )
            .andExpect(status().isOk());

        // Validate the IntegrationConfig in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertIntegrationConfigUpdatableFieldsEquals(
            partialUpdatedIntegrationConfig,
            getPersistedIntegrationConfig(partialUpdatedIntegrationConfig)
        );
    }

    @Test
    @Transactional
    void patchNonExistingIntegrationConfig() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        integrationConfig.setId(longCount.incrementAndGet());

        // Create the IntegrationConfig
        IntegrationConfigDTO integrationConfigDTO = integrationConfigMapper.toDto(integrationConfig);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restIntegrationConfigMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, integrationConfigDTO.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(integrationConfigDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the IntegrationConfig in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchIntegrationConfig() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        integrationConfig.setId(longCount.incrementAndGet());

        // Create the IntegrationConfig
        IntegrationConfigDTO integrationConfigDTO = integrationConfigMapper.toDto(integrationConfig);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restIntegrationConfigMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(integrationConfigDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the IntegrationConfig in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamIntegrationConfig() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        integrationConfig.setId(longCount.incrementAndGet());

        // Create the IntegrationConfig
        IntegrationConfigDTO integrationConfigDTO = integrationConfigMapper.toDto(integrationConfig);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restIntegrationConfigMockMvc
            .perform(patch(ENTITY_API_URL).contentType("application/merge-patch+json").content(om.writeValueAsBytes(integrationConfigDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the IntegrationConfig in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteIntegrationConfig() throws Exception {
        // Initialize the database
        insertedIntegrationConfig = integrationConfigRepository.saveAndFlush(integrationConfig);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the integrationConfig
        restIntegrationConfigMockMvc
            .perform(delete(ENTITY_API_URL_ID, integrationConfig.getId()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return integrationConfigRepository.count();
    }

    protected void assertIncrementedRepositoryCount(long countBefore) {
        assertThat(countBefore + 1).isEqualTo(getRepositoryCount());
    }

    protected void assertDecrementedRepositoryCount(long countBefore) {
        assertThat(countBefore - 1).isEqualTo(getRepositoryCount());
    }

    protected void assertSameRepositoryCount(long countBefore) {
        assertThat(countBefore).isEqualTo(getRepositoryCount());
    }

    protected IntegrationConfig getPersistedIntegrationConfig(IntegrationConfig integrationConfig) {
        return integrationConfigRepository.findById(integrationConfig.getId()).orElseThrow();
    }

    protected void assertPersistedIntegrationConfigToMatchAllProperties(IntegrationConfig expectedIntegrationConfig) {
        assertIntegrationConfigAllPropertiesEquals(expectedIntegrationConfig, getPersistedIntegrationConfig(expectedIntegrationConfig));
    }

    protected void assertPersistedIntegrationConfigToMatchUpdatableProperties(IntegrationConfig expectedIntegrationConfig) {
        assertIntegrationConfigAllUpdatablePropertiesEquals(
            expectedIntegrationConfig,
            getPersistedIntegrationConfig(expectedIntegrationConfig)
        );
    }
}
