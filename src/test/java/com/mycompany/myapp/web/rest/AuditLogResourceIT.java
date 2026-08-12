package com.mycompany.myapp.web.rest;

import static com.mycompany.myapp.domain.AuditLogAsserts.*;
import static com.mycompany.myapp.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.myapp.IntegrationTest;
import com.mycompany.myapp.domain.AuditLog;
import com.mycompany.myapp.repository.AuditLogRepository;
import com.mycompany.myapp.service.dto.AuditLogDTO;
import com.mycompany.myapp.service.mapper.AuditLogMapper;
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
 * Integration tests for the {@link AuditLogResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser
class AuditLogResourceIT {

    private static final String DEFAULT_ACTION = "AAAAAAAAAA";
    private static final String UPDATED_ACTION = "BBBBBBBBBB";

    private static final String DEFAULT_ENTITY_TYPE = "AAAAAAAAAA";
    private static final String UPDATED_ENTITY_TYPE = "BBBBBBBBBB";

    private static final String DEFAULT_ENTITY_ID = "AAAAAAAAAA";
    private static final String UPDATED_ENTITY_ID = "BBBBBBBBBB";

    private static final String DEFAULT_DETAIL = "AAAAAAAAAA";
    private static final String UPDATED_DETAIL = "BBBBBBBBBB";

    private static final Instant DEFAULT_ACTED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_ACTED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final String DEFAULT_ACTED_BY_USERNAME = "AAAAAAAAAA";
    private static final String UPDATED_ACTED_BY_USERNAME = "BBBBBBBBBB";

    private static final String ENTITY_API_URL = "/api/audit-logs";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static Random random = new Random();
    private static AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    @Autowired
    private ObjectMapper om;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private AuditLogMapper auditLogMapper;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restAuditLogMockMvc;

    private AuditLog auditLog;

    private AuditLog insertedAuditLog;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static AuditLog createEntity() {
        return new AuditLog()
            .action(DEFAULT_ACTION)
            .entityType(DEFAULT_ENTITY_TYPE)
            .entityId(DEFAULT_ENTITY_ID)
            .detail(DEFAULT_DETAIL)
            .actedAt(DEFAULT_ACTED_AT)
            .actedByUsername(DEFAULT_ACTED_BY_USERNAME);
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static AuditLog createUpdatedEntity() {
        return new AuditLog()
            .action(UPDATED_ACTION)
            .entityType(UPDATED_ENTITY_TYPE)
            .entityId(UPDATED_ENTITY_ID)
            .detail(UPDATED_DETAIL)
            .actedAt(UPDATED_ACTED_AT)
            .actedByUsername(UPDATED_ACTED_BY_USERNAME);
    }

    @BeforeEach
    public void initTest() {
        auditLog = createEntity();
    }

    @AfterEach
    public void cleanup() {
        if (insertedAuditLog != null) {
            auditLogRepository.delete(insertedAuditLog);
            insertedAuditLog = null;
        }
    }

    @Test
    @Transactional
    void createAuditLog() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the AuditLog
        AuditLogDTO auditLogDTO = auditLogMapper.toDto(auditLog);
        var returnedAuditLogDTO = om.readValue(
            restAuditLogMockMvc
                .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(auditLogDTO)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            AuditLogDTO.class
        );

        // Validate the AuditLog in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedAuditLog = auditLogMapper.toEntity(returnedAuditLogDTO);
        assertAuditLogUpdatableFieldsEquals(returnedAuditLog, getPersistedAuditLog(returnedAuditLog));

        insertedAuditLog = returnedAuditLog;
    }

    @Test
    @Transactional
    void createAuditLogWithExistingId() throws Exception {
        // Create the AuditLog with an existing ID
        auditLog.setId(1L);
        AuditLogDTO auditLogDTO = auditLogMapper.toDto(auditLog);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restAuditLogMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(auditLogDTO)))
            .andExpect(status().isBadRequest());

        // Validate the AuditLog in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkActionIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        auditLog.setAction(null);

        // Create the AuditLog, which fails.
        AuditLogDTO auditLogDTO = auditLogMapper.toDto(auditLog);

        restAuditLogMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(auditLogDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkEntityTypeIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        auditLog.setEntityType(null);

        // Create the AuditLog, which fails.
        AuditLogDTO auditLogDTO = auditLogMapper.toDto(auditLog);

        restAuditLogMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(auditLogDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkEntityIdIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        auditLog.setEntityId(null);

        // Create the AuditLog, which fails.
        AuditLogDTO auditLogDTO = auditLogMapper.toDto(auditLog);

        restAuditLogMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(auditLogDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkActedAtIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        auditLog.setActedAt(null);

        // Create the AuditLog, which fails.
        AuditLogDTO auditLogDTO = auditLogMapper.toDto(auditLog);

        restAuditLogMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(auditLogDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkActedByUsernameIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        auditLog.setActedByUsername(null);

        // Create the AuditLog, which fails.
        AuditLogDTO auditLogDTO = auditLogMapper.toDto(auditLog);

        restAuditLogMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(auditLogDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllAuditLogs() throws Exception {
        // Initialize the database
        insertedAuditLog = auditLogRepository.saveAndFlush(auditLog);

        // Get all the auditLogList
        restAuditLogMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(auditLog.getId().intValue())))
            .andExpect(jsonPath("$.[*].action").value(hasItem(DEFAULT_ACTION)))
            .andExpect(jsonPath("$.[*].entityType").value(hasItem(DEFAULT_ENTITY_TYPE)))
            .andExpect(jsonPath("$.[*].entityId").value(hasItem(DEFAULT_ENTITY_ID)))
            .andExpect(jsonPath("$.[*].detail").value(hasItem(DEFAULT_DETAIL)))
            .andExpect(jsonPath("$.[*].actedAt").value(hasItem(DEFAULT_ACTED_AT.toString())))
            .andExpect(jsonPath("$.[*].actedByUsername").value(hasItem(DEFAULT_ACTED_BY_USERNAME)));
    }

    @Test
    @Transactional
    void getAuditLog() throws Exception {
        // Initialize the database
        insertedAuditLog = auditLogRepository.saveAndFlush(auditLog);

        // Get the auditLog
        restAuditLogMockMvc
            .perform(get(ENTITY_API_URL_ID, auditLog.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(auditLog.getId().intValue()))
            .andExpect(jsonPath("$.action").value(DEFAULT_ACTION))
            .andExpect(jsonPath("$.entityType").value(DEFAULT_ENTITY_TYPE))
            .andExpect(jsonPath("$.entityId").value(DEFAULT_ENTITY_ID))
            .andExpect(jsonPath("$.detail").value(DEFAULT_DETAIL))
            .andExpect(jsonPath("$.actedAt").value(DEFAULT_ACTED_AT.toString()))
            .andExpect(jsonPath("$.actedByUsername").value(DEFAULT_ACTED_BY_USERNAME));
    }

    @Test
    @Transactional
    void getAuditLogsByIdFiltering() throws Exception {
        // Initialize the database
        insertedAuditLog = auditLogRepository.saveAndFlush(auditLog);

        Long id = auditLog.getId();

        defaultAuditLogFiltering("id.equals=" + id, "id.notEquals=" + id);

        defaultAuditLogFiltering("id.greaterThanOrEqual=" + id, "id.greaterThan=" + id);

        defaultAuditLogFiltering("id.lessThanOrEqual=" + id, "id.lessThan=" + id);
    }

    @Test
    @Transactional
    void getAllAuditLogsByActionIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedAuditLog = auditLogRepository.saveAndFlush(auditLog);

        // Get all the auditLogList where action equals to
        defaultAuditLogFiltering("action.equals=" + DEFAULT_ACTION, "action.equals=" + UPDATED_ACTION);
    }

    @Test
    @Transactional
    void getAllAuditLogsByActionIsInShouldWork() throws Exception {
        // Initialize the database
        insertedAuditLog = auditLogRepository.saveAndFlush(auditLog);

        // Get all the auditLogList where action in
        defaultAuditLogFiltering("action.in=" + DEFAULT_ACTION + "," + UPDATED_ACTION, "action.in=" + UPDATED_ACTION);
    }

    @Test
    @Transactional
    void getAllAuditLogsByActionIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedAuditLog = auditLogRepository.saveAndFlush(auditLog);

        // Get all the auditLogList where action is not null
        defaultAuditLogFiltering("action.specified=true", "action.specified=false");
    }

    @Test
    @Transactional
    void getAllAuditLogsByActionContainsSomething() throws Exception {
        // Initialize the database
        insertedAuditLog = auditLogRepository.saveAndFlush(auditLog);

        // Get all the auditLogList where action contains
        defaultAuditLogFiltering("action.contains=" + DEFAULT_ACTION, "action.contains=" + UPDATED_ACTION);
    }

    @Test
    @Transactional
    void getAllAuditLogsByActionNotContainsSomething() throws Exception {
        // Initialize the database
        insertedAuditLog = auditLogRepository.saveAndFlush(auditLog);

        // Get all the auditLogList where action does not contain
        defaultAuditLogFiltering("action.doesNotContain=" + UPDATED_ACTION, "action.doesNotContain=" + DEFAULT_ACTION);
    }

    @Test
    @Transactional
    void getAllAuditLogsByEntityTypeIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedAuditLog = auditLogRepository.saveAndFlush(auditLog);

        // Get all the auditLogList where entityType equals to
        defaultAuditLogFiltering("entityType.equals=" + DEFAULT_ENTITY_TYPE, "entityType.equals=" + UPDATED_ENTITY_TYPE);
    }

    @Test
    @Transactional
    void getAllAuditLogsByEntityTypeIsInShouldWork() throws Exception {
        // Initialize the database
        insertedAuditLog = auditLogRepository.saveAndFlush(auditLog);

        // Get all the auditLogList where entityType in
        defaultAuditLogFiltering(
            "entityType.in=" + DEFAULT_ENTITY_TYPE + "," + UPDATED_ENTITY_TYPE,
            "entityType.in=" + UPDATED_ENTITY_TYPE
        );
    }

    @Test
    @Transactional
    void getAllAuditLogsByEntityTypeIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedAuditLog = auditLogRepository.saveAndFlush(auditLog);

        // Get all the auditLogList where entityType is not null
        defaultAuditLogFiltering("entityType.specified=true", "entityType.specified=false");
    }

    @Test
    @Transactional
    void getAllAuditLogsByEntityTypeContainsSomething() throws Exception {
        // Initialize the database
        insertedAuditLog = auditLogRepository.saveAndFlush(auditLog);

        // Get all the auditLogList where entityType contains
        defaultAuditLogFiltering("entityType.contains=" + DEFAULT_ENTITY_TYPE, "entityType.contains=" + UPDATED_ENTITY_TYPE);
    }

    @Test
    @Transactional
    void getAllAuditLogsByEntityTypeNotContainsSomething() throws Exception {
        // Initialize the database
        insertedAuditLog = auditLogRepository.saveAndFlush(auditLog);

        // Get all the auditLogList where entityType does not contain
        defaultAuditLogFiltering("entityType.doesNotContain=" + UPDATED_ENTITY_TYPE, "entityType.doesNotContain=" + DEFAULT_ENTITY_TYPE);
    }

    @Test
    @Transactional
    void getAllAuditLogsByEntityIdIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedAuditLog = auditLogRepository.saveAndFlush(auditLog);

        // Get all the auditLogList where entityId equals to
        defaultAuditLogFiltering("entityId.equals=" + DEFAULT_ENTITY_ID, "entityId.equals=" + UPDATED_ENTITY_ID);
    }

    @Test
    @Transactional
    void getAllAuditLogsByEntityIdIsInShouldWork() throws Exception {
        // Initialize the database
        insertedAuditLog = auditLogRepository.saveAndFlush(auditLog);

        // Get all the auditLogList where entityId in
        defaultAuditLogFiltering("entityId.in=" + DEFAULT_ENTITY_ID + "," + UPDATED_ENTITY_ID, "entityId.in=" + UPDATED_ENTITY_ID);
    }

    @Test
    @Transactional
    void getAllAuditLogsByEntityIdIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedAuditLog = auditLogRepository.saveAndFlush(auditLog);

        // Get all the auditLogList where entityId is not null
        defaultAuditLogFiltering("entityId.specified=true", "entityId.specified=false");
    }

    @Test
    @Transactional
    void getAllAuditLogsByEntityIdContainsSomething() throws Exception {
        // Initialize the database
        insertedAuditLog = auditLogRepository.saveAndFlush(auditLog);

        // Get all the auditLogList where entityId contains
        defaultAuditLogFiltering("entityId.contains=" + DEFAULT_ENTITY_ID, "entityId.contains=" + UPDATED_ENTITY_ID);
    }

    @Test
    @Transactional
    void getAllAuditLogsByEntityIdNotContainsSomething() throws Exception {
        // Initialize the database
        insertedAuditLog = auditLogRepository.saveAndFlush(auditLog);

        // Get all the auditLogList where entityId does not contain
        defaultAuditLogFiltering("entityId.doesNotContain=" + UPDATED_ENTITY_ID, "entityId.doesNotContain=" + DEFAULT_ENTITY_ID);
    }

    @Test
    @Transactional
    void getAllAuditLogsByDetailIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedAuditLog = auditLogRepository.saveAndFlush(auditLog);

        // Get all the auditLogList where detail equals to
        defaultAuditLogFiltering("detail.equals=" + DEFAULT_DETAIL, "detail.equals=" + UPDATED_DETAIL);
    }

    @Test
    @Transactional
    void getAllAuditLogsByDetailIsInShouldWork() throws Exception {
        // Initialize the database
        insertedAuditLog = auditLogRepository.saveAndFlush(auditLog);

        // Get all the auditLogList where detail in
        defaultAuditLogFiltering("detail.in=" + DEFAULT_DETAIL + "," + UPDATED_DETAIL, "detail.in=" + UPDATED_DETAIL);
    }

    @Test
    @Transactional
    void getAllAuditLogsByDetailIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedAuditLog = auditLogRepository.saveAndFlush(auditLog);

        // Get all the auditLogList where detail is not null
        defaultAuditLogFiltering("detail.specified=true", "detail.specified=false");
    }

    @Test
    @Transactional
    void getAllAuditLogsByDetailContainsSomething() throws Exception {
        // Initialize the database
        insertedAuditLog = auditLogRepository.saveAndFlush(auditLog);

        // Get all the auditLogList where detail contains
        defaultAuditLogFiltering("detail.contains=" + DEFAULT_DETAIL, "detail.contains=" + UPDATED_DETAIL);
    }

    @Test
    @Transactional
    void getAllAuditLogsByDetailNotContainsSomething() throws Exception {
        // Initialize the database
        insertedAuditLog = auditLogRepository.saveAndFlush(auditLog);

        // Get all the auditLogList where detail does not contain
        defaultAuditLogFiltering("detail.doesNotContain=" + UPDATED_DETAIL, "detail.doesNotContain=" + DEFAULT_DETAIL);
    }

    @Test
    @Transactional
    void getAllAuditLogsByActedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedAuditLog = auditLogRepository.saveAndFlush(auditLog);

        // Get all the auditLogList where actedAt equals to
        defaultAuditLogFiltering("actedAt.equals=" + DEFAULT_ACTED_AT, "actedAt.equals=" + UPDATED_ACTED_AT);
    }

    @Test
    @Transactional
    void getAllAuditLogsByActedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedAuditLog = auditLogRepository.saveAndFlush(auditLog);

        // Get all the auditLogList where actedAt in
        defaultAuditLogFiltering("actedAt.in=" + DEFAULT_ACTED_AT + "," + UPDATED_ACTED_AT, "actedAt.in=" + UPDATED_ACTED_AT);
    }

    @Test
    @Transactional
    void getAllAuditLogsByActedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedAuditLog = auditLogRepository.saveAndFlush(auditLog);

        // Get all the auditLogList where actedAt is not null
        defaultAuditLogFiltering("actedAt.specified=true", "actedAt.specified=false");
    }

    @Test
    @Transactional
    void getAllAuditLogsByActedByUsernameIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedAuditLog = auditLogRepository.saveAndFlush(auditLog);

        // Get all the auditLogList where actedByUsername equals to
        defaultAuditLogFiltering(
            "actedByUsername.equals=" + DEFAULT_ACTED_BY_USERNAME,
            "actedByUsername.equals=" + UPDATED_ACTED_BY_USERNAME
        );
    }

    @Test
    @Transactional
    void getAllAuditLogsByActedByUsernameIsInShouldWork() throws Exception {
        // Initialize the database
        insertedAuditLog = auditLogRepository.saveAndFlush(auditLog);

        // Get all the auditLogList where actedByUsername in
        defaultAuditLogFiltering(
            "actedByUsername.in=" + DEFAULT_ACTED_BY_USERNAME + "," + UPDATED_ACTED_BY_USERNAME,
            "actedByUsername.in=" + UPDATED_ACTED_BY_USERNAME
        );
    }

    @Test
    @Transactional
    void getAllAuditLogsByActedByUsernameIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedAuditLog = auditLogRepository.saveAndFlush(auditLog);

        // Get all the auditLogList where actedByUsername is not null
        defaultAuditLogFiltering("actedByUsername.specified=true", "actedByUsername.specified=false");
    }

    @Test
    @Transactional
    void getAllAuditLogsByActedByUsernameContainsSomething() throws Exception {
        // Initialize the database
        insertedAuditLog = auditLogRepository.saveAndFlush(auditLog);

        // Get all the auditLogList where actedByUsername contains
        defaultAuditLogFiltering(
            "actedByUsername.contains=" + DEFAULT_ACTED_BY_USERNAME,
            "actedByUsername.contains=" + UPDATED_ACTED_BY_USERNAME
        );
    }

    @Test
    @Transactional
    void getAllAuditLogsByActedByUsernameNotContainsSomething() throws Exception {
        // Initialize the database
        insertedAuditLog = auditLogRepository.saveAndFlush(auditLog);

        // Get all the auditLogList where actedByUsername does not contain
        defaultAuditLogFiltering(
            "actedByUsername.doesNotContain=" + UPDATED_ACTED_BY_USERNAME,
            "actedByUsername.doesNotContain=" + DEFAULT_ACTED_BY_USERNAME
        );
    }

    private void defaultAuditLogFiltering(String shouldBeFound, String shouldNotBeFound) throws Exception {
        defaultAuditLogShouldBeFound(shouldBeFound);
        defaultAuditLogShouldNotBeFound(shouldNotBeFound);
    }

    /**
     * Executes the search, and checks that the default entity is returned.
     */
    private void defaultAuditLogShouldBeFound(String filter) throws Exception {
        restAuditLogMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(auditLog.getId().intValue())))
            .andExpect(jsonPath("$.[*].action").value(hasItem(DEFAULT_ACTION)))
            .andExpect(jsonPath("$.[*].entityType").value(hasItem(DEFAULT_ENTITY_TYPE)))
            .andExpect(jsonPath("$.[*].entityId").value(hasItem(DEFAULT_ENTITY_ID)))
            .andExpect(jsonPath("$.[*].detail").value(hasItem(DEFAULT_DETAIL)))
            .andExpect(jsonPath("$.[*].actedAt").value(hasItem(DEFAULT_ACTED_AT.toString())))
            .andExpect(jsonPath("$.[*].actedByUsername").value(hasItem(DEFAULT_ACTED_BY_USERNAME)));

        // Check, that the count call also returns 1
        restAuditLogMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("1"));
    }

    /**
     * Executes the search, and checks that the default entity is not returned.
     */
    private void defaultAuditLogShouldNotBeFound(String filter) throws Exception {
        restAuditLogMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());

        // Check, that the count call also returns 0
        restAuditLogMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("0"));
    }

    @Test
    @Transactional
    void getNonExistingAuditLog() throws Exception {
        // Get the auditLog
        restAuditLogMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingAuditLog() throws Exception {
        // Initialize the database
        insertedAuditLog = auditLogRepository.saveAndFlush(auditLog);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the auditLog
        AuditLog updatedAuditLog = auditLogRepository.findById(auditLog.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedAuditLog are not directly saved in db
        em.detach(updatedAuditLog);
        updatedAuditLog
            .action(UPDATED_ACTION)
            .entityType(UPDATED_ENTITY_TYPE)
            .entityId(UPDATED_ENTITY_ID)
            .detail(UPDATED_DETAIL)
            .actedAt(UPDATED_ACTED_AT)
            .actedByUsername(UPDATED_ACTED_BY_USERNAME);
        AuditLogDTO auditLogDTO = auditLogMapper.toDto(updatedAuditLog);

        restAuditLogMockMvc
            .perform(
                put(ENTITY_API_URL_ID, auditLogDTO.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(auditLogDTO))
            )
            .andExpect(status().isOk());

        // Validate the AuditLog in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedAuditLogToMatchAllProperties(updatedAuditLog);
    }

    @Test
    @Transactional
    void putNonExistingAuditLog() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        auditLog.setId(longCount.incrementAndGet());

        // Create the AuditLog
        AuditLogDTO auditLogDTO = auditLogMapper.toDto(auditLog);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restAuditLogMockMvc
            .perform(
                put(ENTITY_API_URL_ID, auditLogDTO.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(auditLogDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the AuditLog in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchAuditLog() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        auditLog.setId(longCount.incrementAndGet());

        // Create the AuditLog
        AuditLogDTO auditLogDTO = auditLogMapper.toDto(auditLog);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restAuditLogMockMvc
            .perform(
                put(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(auditLogDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the AuditLog in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamAuditLog() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        auditLog.setId(longCount.incrementAndGet());

        // Create the AuditLog
        AuditLogDTO auditLogDTO = auditLogMapper.toDto(auditLog);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restAuditLogMockMvc
            .perform(put(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(auditLogDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the AuditLog in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateAuditLogWithPatch() throws Exception {
        // Initialize the database
        insertedAuditLog = auditLogRepository.saveAndFlush(auditLog);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the auditLog using partial update
        AuditLog partialUpdatedAuditLog = new AuditLog();
        partialUpdatedAuditLog.setId(auditLog.getId());

        partialUpdatedAuditLog.detail(UPDATED_DETAIL).actedByUsername(UPDATED_ACTED_BY_USERNAME);

        restAuditLogMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedAuditLog.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedAuditLog))
            )
            .andExpect(status().isOk());

        // Validate the AuditLog in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertAuditLogUpdatableFieldsEquals(createUpdateProxyForBean(partialUpdatedAuditLog, auditLog), getPersistedAuditLog(auditLog));
    }

    @Test
    @Transactional
    void fullUpdateAuditLogWithPatch() throws Exception {
        // Initialize the database
        insertedAuditLog = auditLogRepository.saveAndFlush(auditLog);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the auditLog using partial update
        AuditLog partialUpdatedAuditLog = new AuditLog();
        partialUpdatedAuditLog.setId(auditLog.getId());

        partialUpdatedAuditLog
            .action(UPDATED_ACTION)
            .entityType(UPDATED_ENTITY_TYPE)
            .entityId(UPDATED_ENTITY_ID)
            .detail(UPDATED_DETAIL)
            .actedAt(UPDATED_ACTED_AT)
            .actedByUsername(UPDATED_ACTED_BY_USERNAME);

        restAuditLogMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedAuditLog.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedAuditLog))
            )
            .andExpect(status().isOk());

        // Validate the AuditLog in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertAuditLogUpdatableFieldsEquals(partialUpdatedAuditLog, getPersistedAuditLog(partialUpdatedAuditLog));
    }

    @Test
    @Transactional
    void patchNonExistingAuditLog() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        auditLog.setId(longCount.incrementAndGet());

        // Create the AuditLog
        AuditLogDTO auditLogDTO = auditLogMapper.toDto(auditLog);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restAuditLogMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, auditLogDTO.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(auditLogDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the AuditLog in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchAuditLog() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        auditLog.setId(longCount.incrementAndGet());

        // Create the AuditLog
        AuditLogDTO auditLogDTO = auditLogMapper.toDto(auditLog);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restAuditLogMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(auditLogDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the AuditLog in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamAuditLog() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        auditLog.setId(longCount.incrementAndGet());

        // Create the AuditLog
        AuditLogDTO auditLogDTO = auditLogMapper.toDto(auditLog);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restAuditLogMockMvc
            .perform(patch(ENTITY_API_URL).contentType("application/merge-patch+json").content(om.writeValueAsBytes(auditLogDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the AuditLog in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteAuditLog() throws Exception {
        // Initialize the database
        insertedAuditLog = auditLogRepository.saveAndFlush(auditLog);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the auditLog
        restAuditLogMockMvc
            .perform(delete(ENTITY_API_URL_ID, auditLog.getId()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return auditLogRepository.count();
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

    protected AuditLog getPersistedAuditLog(AuditLog auditLog) {
        return auditLogRepository.findById(auditLog.getId()).orElseThrow();
    }

    protected void assertPersistedAuditLogToMatchAllProperties(AuditLog expectedAuditLog) {
        assertAuditLogAllPropertiesEquals(expectedAuditLog, getPersistedAuditLog(expectedAuditLog));
    }

    protected void assertPersistedAuditLogToMatchUpdatableProperties(AuditLog expectedAuditLog) {
        assertAuditLogAllUpdatablePropertiesEquals(expectedAuditLog, getPersistedAuditLog(expectedAuditLog));
    }
}
