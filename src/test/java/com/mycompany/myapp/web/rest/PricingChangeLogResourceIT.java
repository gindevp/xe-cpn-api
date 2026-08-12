package com.mycompany.myapp.web.rest;

import static com.mycompany.myapp.domain.PricingChangeLogAsserts.*;
import static com.mycompany.myapp.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.myapp.IntegrationTest;
import com.mycompany.myapp.domain.PricingChangeLog;
import com.mycompany.myapp.domain.PricingRule;
import com.mycompany.myapp.repository.PricingChangeLogRepository;
import com.mycompany.myapp.service.PricingChangeLogService;
import com.mycompany.myapp.service.dto.PricingChangeLogDTO;
import com.mycompany.myapp.service.mapper.PricingChangeLogMapper;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for the {@link PricingChangeLogResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser
class PricingChangeLogResourceIT {

    private static final Instant DEFAULT_CHANGED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_CHANGED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final String DEFAULT_CHANGED_BY_USERNAME = "AAAAAAAAAA";
    private static final String UPDATED_CHANGED_BY_USERNAME = "BBBBBBBBBB";

    private static final String DEFAULT_BEFORE_JSON = "AAAAAAAAAA";
    private static final String UPDATED_BEFORE_JSON = "BBBBBBBBBB";

    private static final String DEFAULT_AFTER_JSON = "AAAAAAAAAA";
    private static final String UPDATED_AFTER_JSON = "BBBBBBBBBB";

    private static final String ENTITY_API_URL = "/api/pricing-change-logs";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static Random random = new Random();
    private static AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    @Autowired
    private ObjectMapper om;

    @Autowired
    private PricingChangeLogRepository pricingChangeLogRepository;

    @Mock
    private PricingChangeLogRepository pricingChangeLogRepositoryMock;

    @Autowired
    private PricingChangeLogMapper pricingChangeLogMapper;

    @Mock
    private PricingChangeLogService pricingChangeLogServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restPricingChangeLogMockMvc;

    private PricingChangeLog pricingChangeLog;

    private PricingChangeLog insertedPricingChangeLog;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static PricingChangeLog createEntity(EntityManager em) {
        PricingChangeLog pricingChangeLog = new PricingChangeLog()
            .changedAt(DEFAULT_CHANGED_AT)
            .changedByUsername(DEFAULT_CHANGED_BY_USERNAME)
            .beforeJson(DEFAULT_BEFORE_JSON)
            .afterJson(DEFAULT_AFTER_JSON);
        // Add required entity
        PricingRule pricingRule;
        if (TestUtil.findAll(em, PricingRule.class).isEmpty()) {
            pricingRule = PricingRuleResourceIT.createEntity(em);
            em.persist(pricingRule);
            em.flush();
        } else {
            pricingRule = TestUtil.findAll(em, PricingRule.class).get(0);
        }
        pricingChangeLog.setPricingRule(pricingRule);
        return pricingChangeLog;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static PricingChangeLog createUpdatedEntity(EntityManager em) {
        PricingChangeLog updatedPricingChangeLog = new PricingChangeLog()
            .changedAt(UPDATED_CHANGED_AT)
            .changedByUsername(UPDATED_CHANGED_BY_USERNAME)
            .beforeJson(UPDATED_BEFORE_JSON)
            .afterJson(UPDATED_AFTER_JSON);
        // Add required entity
        PricingRule pricingRule;
        if (TestUtil.findAll(em, PricingRule.class).isEmpty()) {
            pricingRule = PricingRuleResourceIT.createUpdatedEntity(em);
            em.persist(pricingRule);
            em.flush();
        } else {
            pricingRule = TestUtil.findAll(em, PricingRule.class).get(0);
        }
        updatedPricingChangeLog.setPricingRule(pricingRule);
        return updatedPricingChangeLog;
    }

    @BeforeEach
    public void initTest() {
        pricingChangeLog = createEntity(em);
    }

    @AfterEach
    public void cleanup() {
        if (insertedPricingChangeLog != null) {
            pricingChangeLogRepository.delete(insertedPricingChangeLog);
            insertedPricingChangeLog = null;
        }
    }

    @Test
    @Transactional
    void createPricingChangeLog() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the PricingChangeLog
        PricingChangeLogDTO pricingChangeLogDTO = pricingChangeLogMapper.toDto(pricingChangeLog);
        var returnedPricingChangeLogDTO = om.readValue(
            restPricingChangeLogMockMvc
                .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(pricingChangeLogDTO)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            PricingChangeLogDTO.class
        );

        // Validate the PricingChangeLog in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedPricingChangeLog = pricingChangeLogMapper.toEntity(returnedPricingChangeLogDTO);
        assertPricingChangeLogUpdatableFieldsEquals(returnedPricingChangeLog, getPersistedPricingChangeLog(returnedPricingChangeLog));

        insertedPricingChangeLog = returnedPricingChangeLog;
    }

    @Test
    @Transactional
    void createPricingChangeLogWithExistingId() throws Exception {
        // Create the PricingChangeLog with an existing ID
        pricingChangeLog.setId(1L);
        PricingChangeLogDTO pricingChangeLogDTO = pricingChangeLogMapper.toDto(pricingChangeLog);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restPricingChangeLogMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(pricingChangeLogDTO)))
            .andExpect(status().isBadRequest());

        // Validate the PricingChangeLog in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkChangedAtIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        pricingChangeLog.setChangedAt(null);

        // Create the PricingChangeLog, which fails.
        PricingChangeLogDTO pricingChangeLogDTO = pricingChangeLogMapper.toDto(pricingChangeLog);

        restPricingChangeLogMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(pricingChangeLogDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkChangedByUsernameIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        pricingChangeLog.setChangedByUsername(null);

        // Create the PricingChangeLog, which fails.
        PricingChangeLogDTO pricingChangeLogDTO = pricingChangeLogMapper.toDto(pricingChangeLog);

        restPricingChangeLogMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(pricingChangeLogDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllPricingChangeLogs() throws Exception {
        // Initialize the database
        insertedPricingChangeLog = pricingChangeLogRepository.saveAndFlush(pricingChangeLog);

        // Get all the pricingChangeLogList
        restPricingChangeLogMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(pricingChangeLog.getId().intValue())))
            .andExpect(jsonPath("$.[*].changedAt").value(hasItem(DEFAULT_CHANGED_AT.toString())))
            .andExpect(jsonPath("$.[*].changedByUsername").value(hasItem(DEFAULT_CHANGED_BY_USERNAME)))
            .andExpect(jsonPath("$.[*].beforeJson").value(hasItem(DEFAULT_BEFORE_JSON)))
            .andExpect(jsonPath("$.[*].afterJson").value(hasItem(DEFAULT_AFTER_JSON)));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllPricingChangeLogsWithEagerRelationshipsIsEnabled() throws Exception {
        when(pricingChangeLogServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restPricingChangeLogMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(pricingChangeLogServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllPricingChangeLogsWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(pricingChangeLogServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restPricingChangeLogMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(pricingChangeLogRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getPricingChangeLog() throws Exception {
        // Initialize the database
        insertedPricingChangeLog = pricingChangeLogRepository.saveAndFlush(pricingChangeLog);

        // Get the pricingChangeLog
        restPricingChangeLogMockMvc
            .perform(get(ENTITY_API_URL_ID, pricingChangeLog.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(pricingChangeLog.getId().intValue()))
            .andExpect(jsonPath("$.changedAt").value(DEFAULT_CHANGED_AT.toString()))
            .andExpect(jsonPath("$.changedByUsername").value(DEFAULT_CHANGED_BY_USERNAME))
            .andExpect(jsonPath("$.beforeJson").value(DEFAULT_BEFORE_JSON))
            .andExpect(jsonPath("$.afterJson").value(DEFAULT_AFTER_JSON));
    }

    @Test
    @Transactional
    void getNonExistingPricingChangeLog() throws Exception {
        // Get the pricingChangeLog
        restPricingChangeLogMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingPricingChangeLog() throws Exception {
        // Initialize the database
        insertedPricingChangeLog = pricingChangeLogRepository.saveAndFlush(pricingChangeLog);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the pricingChangeLog
        PricingChangeLog updatedPricingChangeLog = pricingChangeLogRepository.findById(pricingChangeLog.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedPricingChangeLog are not directly saved in db
        em.detach(updatedPricingChangeLog);
        updatedPricingChangeLog
            .changedAt(UPDATED_CHANGED_AT)
            .changedByUsername(UPDATED_CHANGED_BY_USERNAME)
            .beforeJson(UPDATED_BEFORE_JSON)
            .afterJson(UPDATED_AFTER_JSON);
        PricingChangeLogDTO pricingChangeLogDTO = pricingChangeLogMapper.toDto(updatedPricingChangeLog);

        restPricingChangeLogMockMvc
            .perform(
                put(ENTITY_API_URL_ID, pricingChangeLogDTO.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(pricingChangeLogDTO))
            )
            .andExpect(status().isOk());

        // Validate the PricingChangeLog in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedPricingChangeLogToMatchAllProperties(updatedPricingChangeLog);
    }

    @Test
    @Transactional
    void putNonExistingPricingChangeLog() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        pricingChangeLog.setId(longCount.incrementAndGet());

        // Create the PricingChangeLog
        PricingChangeLogDTO pricingChangeLogDTO = pricingChangeLogMapper.toDto(pricingChangeLog);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restPricingChangeLogMockMvc
            .perform(
                put(ENTITY_API_URL_ID, pricingChangeLogDTO.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(pricingChangeLogDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the PricingChangeLog in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchPricingChangeLog() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        pricingChangeLog.setId(longCount.incrementAndGet());

        // Create the PricingChangeLog
        PricingChangeLogDTO pricingChangeLogDTO = pricingChangeLogMapper.toDto(pricingChangeLog);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restPricingChangeLogMockMvc
            .perform(
                put(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(pricingChangeLogDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the PricingChangeLog in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamPricingChangeLog() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        pricingChangeLog.setId(longCount.incrementAndGet());

        // Create the PricingChangeLog
        PricingChangeLogDTO pricingChangeLogDTO = pricingChangeLogMapper.toDto(pricingChangeLog);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restPricingChangeLogMockMvc
            .perform(put(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(pricingChangeLogDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the PricingChangeLog in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdatePricingChangeLogWithPatch() throws Exception {
        // Initialize the database
        insertedPricingChangeLog = pricingChangeLogRepository.saveAndFlush(pricingChangeLog);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the pricingChangeLog using partial update
        PricingChangeLog partialUpdatedPricingChangeLog = new PricingChangeLog();
        partialUpdatedPricingChangeLog.setId(pricingChangeLog.getId());

        restPricingChangeLogMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedPricingChangeLog.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedPricingChangeLog))
            )
            .andExpect(status().isOk());

        // Validate the PricingChangeLog in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPricingChangeLogUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedPricingChangeLog, pricingChangeLog),
            getPersistedPricingChangeLog(pricingChangeLog)
        );
    }

    @Test
    @Transactional
    void fullUpdatePricingChangeLogWithPatch() throws Exception {
        // Initialize the database
        insertedPricingChangeLog = pricingChangeLogRepository.saveAndFlush(pricingChangeLog);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the pricingChangeLog using partial update
        PricingChangeLog partialUpdatedPricingChangeLog = new PricingChangeLog();
        partialUpdatedPricingChangeLog.setId(pricingChangeLog.getId());

        partialUpdatedPricingChangeLog
            .changedAt(UPDATED_CHANGED_AT)
            .changedByUsername(UPDATED_CHANGED_BY_USERNAME)
            .beforeJson(UPDATED_BEFORE_JSON)
            .afterJson(UPDATED_AFTER_JSON);

        restPricingChangeLogMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedPricingChangeLog.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedPricingChangeLog))
            )
            .andExpect(status().isOk());

        // Validate the PricingChangeLog in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPricingChangeLogUpdatableFieldsEquals(
            partialUpdatedPricingChangeLog,
            getPersistedPricingChangeLog(partialUpdatedPricingChangeLog)
        );
    }

    @Test
    @Transactional
    void patchNonExistingPricingChangeLog() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        pricingChangeLog.setId(longCount.incrementAndGet());

        // Create the PricingChangeLog
        PricingChangeLogDTO pricingChangeLogDTO = pricingChangeLogMapper.toDto(pricingChangeLog);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restPricingChangeLogMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, pricingChangeLogDTO.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(pricingChangeLogDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the PricingChangeLog in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchPricingChangeLog() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        pricingChangeLog.setId(longCount.incrementAndGet());

        // Create the PricingChangeLog
        PricingChangeLogDTO pricingChangeLogDTO = pricingChangeLogMapper.toDto(pricingChangeLog);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restPricingChangeLogMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(pricingChangeLogDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the PricingChangeLog in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamPricingChangeLog() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        pricingChangeLog.setId(longCount.incrementAndGet());

        // Create the PricingChangeLog
        PricingChangeLogDTO pricingChangeLogDTO = pricingChangeLogMapper.toDto(pricingChangeLog);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restPricingChangeLogMockMvc
            .perform(patch(ENTITY_API_URL).contentType("application/merge-patch+json").content(om.writeValueAsBytes(pricingChangeLogDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the PricingChangeLog in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deletePricingChangeLog() throws Exception {
        // Initialize the database
        insertedPricingChangeLog = pricingChangeLogRepository.saveAndFlush(pricingChangeLog);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the pricingChangeLog
        restPricingChangeLogMockMvc
            .perform(delete(ENTITY_API_URL_ID, pricingChangeLog.getId()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return pricingChangeLogRepository.count();
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

    protected PricingChangeLog getPersistedPricingChangeLog(PricingChangeLog pricingChangeLog) {
        return pricingChangeLogRepository.findById(pricingChangeLog.getId()).orElseThrow();
    }

    protected void assertPersistedPricingChangeLogToMatchAllProperties(PricingChangeLog expectedPricingChangeLog) {
        assertPricingChangeLogAllPropertiesEquals(expectedPricingChangeLog, getPersistedPricingChangeLog(expectedPricingChangeLog));
    }

    protected void assertPersistedPricingChangeLogToMatchUpdatableProperties(PricingChangeLog expectedPricingChangeLog) {
        assertPricingChangeLogAllUpdatablePropertiesEquals(
            expectedPricingChangeLog,
            getPersistedPricingChangeLog(expectedPricingChangeLog)
        );
    }
}
