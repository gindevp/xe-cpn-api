package com.mycompany.myapp.web.rest;

import static com.mycompany.myapp.domain.OrderIssueAsserts.*;
import static com.mycompany.myapp.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.myapp.IntegrationTest;
import com.mycompany.myapp.domain.OrderIssue;
import com.mycompany.myapp.domain.enumeration.IssueStatus;
import com.mycompany.myapp.domain.enumeration.IssueType;
import com.mycompany.myapp.repository.OrderIssueRepository;
import com.mycompany.myapp.service.dto.OrderIssueDTO;
import com.mycompany.myapp.service.mapper.OrderIssueMapper;
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
 * Integration tests for the {@link OrderIssueResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser
class OrderIssueResourceIT {

    private static final IssueType DEFAULT_ISSUE_TYPE = IssueType.EXCEPTION;
    private static final IssueType UPDATED_ISSUE_TYPE = IssueType.LOST;

    private static final IssueStatus DEFAULT_ISSUE_STATUS = IssueStatus.OPEN;
    private static final IssueStatus UPDATED_ISSUE_STATUS = IssueStatus.RESOLVED;

    private static final String DEFAULT_REASON = "AAAAAAAAAA";
    private static final String UPDATED_REASON = "BBBBBBBBBB";

    private static final Instant DEFAULT_OPENED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_OPENED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final String DEFAULT_OPENED_BY_USERNAME = "AAAAAAAAAA";
    private static final String UPDATED_OPENED_BY_USERNAME = "BBBBBBBBBB";

    private static final Instant DEFAULT_RESOLVED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_RESOLVED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final String DEFAULT_RESOLVED_BY_USERNAME = "AAAAAAAAAA";
    private static final String UPDATED_RESOLVED_BY_USERNAME = "BBBBBBBBBB";

    private static final String DEFAULT_RESOLUTION_NOTE = "AAAAAAAAAA";
    private static final String UPDATED_RESOLUTION_NOTE = "BBBBBBBBBB";

    private static final String ENTITY_API_URL = "/api/order-issues";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static Random random = new Random();
    private static AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    @Autowired
    private ObjectMapper om;

    @Autowired
    private OrderIssueRepository orderIssueRepository;

    @Autowired
    private OrderIssueMapper orderIssueMapper;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restOrderIssueMockMvc;

    private OrderIssue orderIssue;

    private OrderIssue insertedOrderIssue;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static OrderIssue createEntity(EntityManager em) {
        OrderIssue orderIssue = new OrderIssue()
            .issueType(DEFAULT_ISSUE_TYPE)
            .issueStatus(DEFAULT_ISSUE_STATUS)
            .reason(DEFAULT_REASON)
            .openedAt(DEFAULT_OPENED_AT)
            .openedByUsername(DEFAULT_OPENED_BY_USERNAME)
            .resolvedAt(DEFAULT_RESOLVED_AT)
            .resolvedByUsername(DEFAULT_RESOLVED_BY_USERNAME)
            .resolutionNote(DEFAULT_RESOLUTION_NOTE);
        return orderIssue;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static OrderIssue createUpdatedEntity(EntityManager em) {
        OrderIssue updatedOrderIssue = new OrderIssue()
            .issueType(UPDATED_ISSUE_TYPE)
            .issueStatus(UPDATED_ISSUE_STATUS)
            .reason(UPDATED_REASON)
            .openedAt(UPDATED_OPENED_AT)
            .openedByUsername(UPDATED_OPENED_BY_USERNAME)
            .resolvedAt(UPDATED_RESOLVED_AT)
            .resolvedByUsername(UPDATED_RESOLVED_BY_USERNAME)
            .resolutionNote(UPDATED_RESOLUTION_NOTE);
        return updatedOrderIssue;
    }

    @BeforeEach
    public void initTest() {
        orderIssue = createEntity(em);
    }

    @AfterEach
    public void cleanup() {
        if (insertedOrderIssue != null) {
            orderIssueRepository.delete(insertedOrderIssue);
            insertedOrderIssue = null;
        }
    }

    @Test
    @Transactional
    void createOrderIssue() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the OrderIssue
        OrderIssueDTO orderIssueDTO = orderIssueMapper.toDto(orderIssue);
        var returnedOrderIssueDTO = om.readValue(
            restOrderIssueMockMvc
                .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(orderIssueDTO)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            OrderIssueDTO.class
        );

        // Validate the OrderIssue in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedOrderIssue = orderIssueMapper.toEntity(returnedOrderIssueDTO);
        assertOrderIssueUpdatableFieldsEquals(returnedOrderIssue, getPersistedOrderIssue(returnedOrderIssue));

        insertedOrderIssue = returnedOrderIssue;
    }

    @Test
    @Transactional
    void createOrderIssueWithExistingId() throws Exception {
        // Create the OrderIssue with an existing ID
        orderIssue.setId(1L);
        OrderIssueDTO orderIssueDTO = orderIssueMapper.toDto(orderIssue);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restOrderIssueMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(orderIssueDTO)))
            .andExpect(status().isBadRequest());

        // Validate the OrderIssue in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkIssueTypeIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        orderIssue.setIssueType(null);

        // Create the OrderIssue, which fails.
        OrderIssueDTO orderIssueDTO = orderIssueMapper.toDto(orderIssue);

        restOrderIssueMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(orderIssueDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkIssueStatusIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        orderIssue.setIssueStatus(null);

        // Create the OrderIssue, which fails.
        OrderIssueDTO orderIssueDTO = orderIssueMapper.toDto(orderIssue);

        restOrderIssueMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(orderIssueDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkOpenedAtIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        orderIssue.setOpenedAt(null);

        // Create the OrderIssue, which fails.
        OrderIssueDTO orderIssueDTO = orderIssueMapper.toDto(orderIssue);

        restOrderIssueMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(orderIssueDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkOpenedByUsernameIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        orderIssue.setOpenedByUsername(null);

        // Create the OrderIssue, which fails.
        OrderIssueDTO orderIssueDTO = orderIssueMapper.toDto(orderIssue);

        restOrderIssueMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(orderIssueDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllOrderIssues() throws Exception {
        // Initialize the database
        insertedOrderIssue = orderIssueRepository.saveAndFlush(orderIssue);

        // Get all the orderIssueList
        restOrderIssueMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(orderIssue.getId().intValue())))
            .andExpect(jsonPath("$.[*].issueType").value(hasItem(DEFAULT_ISSUE_TYPE.toString())))
            .andExpect(jsonPath("$.[*].issueStatus").value(hasItem(DEFAULT_ISSUE_STATUS.toString())))
            .andExpect(jsonPath("$.[*].reason").value(hasItem(DEFAULT_REASON)))
            .andExpect(jsonPath("$.[*].openedAt").value(hasItem(DEFAULT_OPENED_AT.toString())))
            .andExpect(jsonPath("$.[*].openedByUsername").value(hasItem(DEFAULT_OPENED_BY_USERNAME)))
            .andExpect(jsonPath("$.[*].resolvedAt").value(hasItem(DEFAULT_RESOLVED_AT.toString())))
            .andExpect(jsonPath("$.[*].resolvedByUsername").value(hasItem(DEFAULT_RESOLVED_BY_USERNAME)))
            .andExpect(jsonPath("$.[*].resolutionNote").value(hasItem(DEFAULT_RESOLUTION_NOTE)));
    }

    @Test
    @Transactional
    void getOrderIssue() throws Exception {
        // Initialize the database
        insertedOrderIssue = orderIssueRepository.saveAndFlush(orderIssue);

        // Get the orderIssue
        restOrderIssueMockMvc
            .perform(get(ENTITY_API_URL_ID, orderIssue.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(orderIssue.getId().intValue()))
            .andExpect(jsonPath("$.issueType").value(DEFAULT_ISSUE_TYPE.toString()))
            .andExpect(jsonPath("$.issueStatus").value(DEFAULT_ISSUE_STATUS.toString()))
            .andExpect(jsonPath("$.reason").value(DEFAULT_REASON))
            .andExpect(jsonPath("$.openedAt").value(DEFAULT_OPENED_AT.toString()))
            .andExpect(jsonPath("$.openedByUsername").value(DEFAULT_OPENED_BY_USERNAME))
            .andExpect(jsonPath("$.resolvedAt").value(DEFAULT_RESOLVED_AT.toString()))
            .andExpect(jsonPath("$.resolvedByUsername").value(DEFAULT_RESOLVED_BY_USERNAME))
            .andExpect(jsonPath("$.resolutionNote").value(DEFAULT_RESOLUTION_NOTE));
    }

    @Test
    @Transactional
    void getOrderIssuesByIdFiltering() throws Exception {
        // Initialize the database
        insertedOrderIssue = orderIssueRepository.saveAndFlush(orderIssue);

        Long id = orderIssue.getId();

        defaultOrderIssueFiltering("id.equals=" + id, "id.notEquals=" + id);

        defaultOrderIssueFiltering("id.greaterThanOrEqual=" + id, "id.greaterThan=" + id);

        defaultOrderIssueFiltering("id.lessThanOrEqual=" + id, "id.lessThan=" + id);
    }

    @Test
    @Transactional
    void getAllOrderIssuesByIssueTypeIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedOrderIssue = orderIssueRepository.saveAndFlush(orderIssue);

        // Get all the orderIssueList where issueType equals to
        defaultOrderIssueFiltering("issueType.equals=" + DEFAULT_ISSUE_TYPE, "issueType.equals=" + UPDATED_ISSUE_TYPE);
    }

    @Test
    @Transactional
    void getAllOrderIssuesByIssueTypeIsInShouldWork() throws Exception {
        // Initialize the database
        insertedOrderIssue = orderIssueRepository.saveAndFlush(orderIssue);

        // Get all the orderIssueList where issueType in
        defaultOrderIssueFiltering("issueType.in=" + DEFAULT_ISSUE_TYPE + "," + UPDATED_ISSUE_TYPE, "issueType.in=" + UPDATED_ISSUE_TYPE);
    }

    @Test
    @Transactional
    void getAllOrderIssuesByIssueTypeIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedOrderIssue = orderIssueRepository.saveAndFlush(orderIssue);

        // Get all the orderIssueList where issueType is not null
        defaultOrderIssueFiltering("issueType.specified=true", "issueType.specified=false");
    }

    @Test
    @Transactional
    void getAllOrderIssuesByIssueStatusIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedOrderIssue = orderIssueRepository.saveAndFlush(orderIssue);

        // Get all the orderIssueList where issueStatus equals to
        defaultOrderIssueFiltering("issueStatus.equals=" + DEFAULT_ISSUE_STATUS, "issueStatus.equals=" + UPDATED_ISSUE_STATUS);
    }

    @Test
    @Transactional
    void getAllOrderIssuesByIssueStatusIsInShouldWork() throws Exception {
        // Initialize the database
        insertedOrderIssue = orderIssueRepository.saveAndFlush(orderIssue);

        // Get all the orderIssueList where issueStatus in
        defaultOrderIssueFiltering(
            "issueStatus.in=" + DEFAULT_ISSUE_STATUS + "," + UPDATED_ISSUE_STATUS,
            "issueStatus.in=" + UPDATED_ISSUE_STATUS
        );
    }

    @Test
    @Transactional
    void getAllOrderIssuesByIssueStatusIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedOrderIssue = orderIssueRepository.saveAndFlush(orderIssue);

        // Get all the orderIssueList where issueStatus is not null
        defaultOrderIssueFiltering("issueStatus.specified=true", "issueStatus.specified=false");
    }

    @Test
    @Transactional
    void getAllOrderIssuesByReasonIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedOrderIssue = orderIssueRepository.saveAndFlush(orderIssue);

        // Get all the orderIssueList where reason equals to
        defaultOrderIssueFiltering("reason.equals=" + DEFAULT_REASON, "reason.equals=" + UPDATED_REASON);
    }

    @Test
    @Transactional
    void getAllOrderIssuesByReasonIsInShouldWork() throws Exception {
        // Initialize the database
        insertedOrderIssue = orderIssueRepository.saveAndFlush(orderIssue);

        // Get all the orderIssueList where reason in
        defaultOrderIssueFiltering("reason.in=" + DEFAULT_REASON + "," + UPDATED_REASON, "reason.in=" + UPDATED_REASON);
    }

    @Test
    @Transactional
    void getAllOrderIssuesByReasonIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedOrderIssue = orderIssueRepository.saveAndFlush(orderIssue);

        // Get all the orderIssueList where reason is not null
        defaultOrderIssueFiltering("reason.specified=true", "reason.specified=false");
    }

    @Test
    @Transactional
    void getAllOrderIssuesByReasonContainsSomething() throws Exception {
        // Initialize the database
        insertedOrderIssue = orderIssueRepository.saveAndFlush(orderIssue);

        // Get all the orderIssueList where reason contains
        defaultOrderIssueFiltering("reason.contains=" + DEFAULT_REASON, "reason.contains=" + UPDATED_REASON);
    }

    @Test
    @Transactional
    void getAllOrderIssuesByReasonNotContainsSomething() throws Exception {
        // Initialize the database
        insertedOrderIssue = orderIssueRepository.saveAndFlush(orderIssue);

        // Get all the orderIssueList where reason does not contain
        defaultOrderIssueFiltering("reason.doesNotContain=" + UPDATED_REASON, "reason.doesNotContain=" + DEFAULT_REASON);
    }

    @Test
    @Transactional
    void getAllOrderIssuesByOpenedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedOrderIssue = orderIssueRepository.saveAndFlush(orderIssue);

        // Get all the orderIssueList where openedAt equals to
        defaultOrderIssueFiltering("openedAt.equals=" + DEFAULT_OPENED_AT, "openedAt.equals=" + UPDATED_OPENED_AT);
    }

    @Test
    @Transactional
    void getAllOrderIssuesByOpenedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedOrderIssue = orderIssueRepository.saveAndFlush(orderIssue);

        // Get all the orderIssueList where openedAt in
        defaultOrderIssueFiltering("openedAt.in=" + DEFAULT_OPENED_AT + "," + UPDATED_OPENED_AT, "openedAt.in=" + UPDATED_OPENED_AT);
    }

    @Test
    @Transactional
    void getAllOrderIssuesByOpenedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedOrderIssue = orderIssueRepository.saveAndFlush(orderIssue);

        // Get all the orderIssueList where openedAt is not null
        defaultOrderIssueFiltering("openedAt.specified=true", "openedAt.specified=false");
    }

    @Test
    @Transactional
    void getAllOrderIssuesByOpenedByUsernameIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedOrderIssue = orderIssueRepository.saveAndFlush(orderIssue);

        // Get all the orderIssueList where openedByUsername equals to
        defaultOrderIssueFiltering(
            "openedByUsername.equals=" + DEFAULT_OPENED_BY_USERNAME,
            "openedByUsername.equals=" + UPDATED_OPENED_BY_USERNAME
        );
    }

    @Test
    @Transactional
    void getAllOrderIssuesByOpenedByUsernameIsInShouldWork() throws Exception {
        // Initialize the database
        insertedOrderIssue = orderIssueRepository.saveAndFlush(orderIssue);

        // Get all the orderIssueList where openedByUsername in
        defaultOrderIssueFiltering(
            "openedByUsername.in=" + DEFAULT_OPENED_BY_USERNAME + "," + UPDATED_OPENED_BY_USERNAME,
            "openedByUsername.in=" + UPDATED_OPENED_BY_USERNAME
        );
    }

    @Test
    @Transactional
    void getAllOrderIssuesByOpenedByUsernameIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedOrderIssue = orderIssueRepository.saveAndFlush(orderIssue);

        // Get all the orderIssueList where openedByUsername is not null
        defaultOrderIssueFiltering("openedByUsername.specified=true", "openedByUsername.specified=false");
    }

    @Test
    @Transactional
    void getAllOrderIssuesByOpenedByUsernameContainsSomething() throws Exception {
        // Initialize the database
        insertedOrderIssue = orderIssueRepository.saveAndFlush(orderIssue);

        // Get all the orderIssueList where openedByUsername contains
        defaultOrderIssueFiltering(
            "openedByUsername.contains=" + DEFAULT_OPENED_BY_USERNAME,
            "openedByUsername.contains=" + UPDATED_OPENED_BY_USERNAME
        );
    }

    @Test
    @Transactional
    void getAllOrderIssuesByOpenedByUsernameNotContainsSomething() throws Exception {
        // Initialize the database
        insertedOrderIssue = orderIssueRepository.saveAndFlush(orderIssue);

        // Get all the orderIssueList where openedByUsername does not contain
        defaultOrderIssueFiltering(
            "openedByUsername.doesNotContain=" + UPDATED_OPENED_BY_USERNAME,
            "openedByUsername.doesNotContain=" + DEFAULT_OPENED_BY_USERNAME
        );
    }

    @Test
    @Transactional
    void getAllOrderIssuesByResolvedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedOrderIssue = orderIssueRepository.saveAndFlush(orderIssue);

        // Get all the orderIssueList where resolvedAt equals to
        defaultOrderIssueFiltering("resolvedAt.equals=" + DEFAULT_RESOLVED_AT, "resolvedAt.equals=" + UPDATED_RESOLVED_AT);
    }

    @Test
    @Transactional
    void getAllOrderIssuesByResolvedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedOrderIssue = orderIssueRepository.saveAndFlush(orderIssue);

        // Get all the orderIssueList where resolvedAt in
        defaultOrderIssueFiltering(
            "resolvedAt.in=" + DEFAULT_RESOLVED_AT + "," + UPDATED_RESOLVED_AT,
            "resolvedAt.in=" + UPDATED_RESOLVED_AT
        );
    }

    @Test
    @Transactional
    void getAllOrderIssuesByResolvedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedOrderIssue = orderIssueRepository.saveAndFlush(orderIssue);

        // Get all the orderIssueList where resolvedAt is not null
        defaultOrderIssueFiltering("resolvedAt.specified=true", "resolvedAt.specified=false");
    }

    @Test
    @Transactional
    void getAllOrderIssuesByResolvedByUsernameIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedOrderIssue = orderIssueRepository.saveAndFlush(orderIssue);

        // Get all the orderIssueList where resolvedByUsername equals to
        defaultOrderIssueFiltering(
            "resolvedByUsername.equals=" + DEFAULT_RESOLVED_BY_USERNAME,
            "resolvedByUsername.equals=" + UPDATED_RESOLVED_BY_USERNAME
        );
    }

    @Test
    @Transactional
    void getAllOrderIssuesByResolvedByUsernameIsInShouldWork() throws Exception {
        // Initialize the database
        insertedOrderIssue = orderIssueRepository.saveAndFlush(orderIssue);

        // Get all the orderIssueList where resolvedByUsername in
        defaultOrderIssueFiltering(
            "resolvedByUsername.in=" + DEFAULT_RESOLVED_BY_USERNAME + "," + UPDATED_RESOLVED_BY_USERNAME,
            "resolvedByUsername.in=" + UPDATED_RESOLVED_BY_USERNAME
        );
    }

    @Test
    @Transactional
    void getAllOrderIssuesByResolvedByUsernameIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedOrderIssue = orderIssueRepository.saveAndFlush(orderIssue);

        // Get all the orderIssueList where resolvedByUsername is not null
        defaultOrderIssueFiltering("resolvedByUsername.specified=true", "resolvedByUsername.specified=false");
    }

    @Test
    @Transactional
    void getAllOrderIssuesByResolvedByUsernameContainsSomething() throws Exception {
        // Initialize the database
        insertedOrderIssue = orderIssueRepository.saveAndFlush(orderIssue);

        // Get all the orderIssueList where resolvedByUsername contains
        defaultOrderIssueFiltering(
            "resolvedByUsername.contains=" + DEFAULT_RESOLVED_BY_USERNAME,
            "resolvedByUsername.contains=" + UPDATED_RESOLVED_BY_USERNAME
        );
    }

    @Test
    @Transactional
    void getAllOrderIssuesByResolvedByUsernameNotContainsSomething() throws Exception {
        // Initialize the database
        insertedOrderIssue = orderIssueRepository.saveAndFlush(orderIssue);

        // Get all the orderIssueList where resolvedByUsername does not contain
        defaultOrderIssueFiltering(
            "resolvedByUsername.doesNotContain=" + UPDATED_RESOLVED_BY_USERNAME,
            "resolvedByUsername.doesNotContain=" + DEFAULT_RESOLVED_BY_USERNAME
        );
    }

    @Test
    @Transactional
    void getAllOrderIssuesByResolutionNoteIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedOrderIssue = orderIssueRepository.saveAndFlush(orderIssue);

        // Get all the orderIssueList where resolutionNote equals to
        defaultOrderIssueFiltering("resolutionNote.equals=" + DEFAULT_RESOLUTION_NOTE, "resolutionNote.equals=" + UPDATED_RESOLUTION_NOTE);
    }

    @Test
    @Transactional
    void getAllOrderIssuesByResolutionNoteIsInShouldWork() throws Exception {
        // Initialize the database
        insertedOrderIssue = orderIssueRepository.saveAndFlush(orderIssue);

        // Get all the orderIssueList where resolutionNote in
        defaultOrderIssueFiltering(
            "resolutionNote.in=" + DEFAULT_RESOLUTION_NOTE + "," + UPDATED_RESOLUTION_NOTE,
            "resolutionNote.in=" + UPDATED_RESOLUTION_NOTE
        );
    }

    @Test
    @Transactional
    void getAllOrderIssuesByResolutionNoteIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedOrderIssue = orderIssueRepository.saveAndFlush(orderIssue);

        // Get all the orderIssueList where resolutionNote is not null
        defaultOrderIssueFiltering("resolutionNote.specified=true", "resolutionNote.specified=false");
    }

    @Test
    @Transactional
    void getAllOrderIssuesByResolutionNoteContainsSomething() throws Exception {
        // Initialize the database
        insertedOrderIssue = orderIssueRepository.saveAndFlush(orderIssue);

        // Get all the orderIssueList where resolutionNote contains
        defaultOrderIssueFiltering(
            "resolutionNote.contains=" + DEFAULT_RESOLUTION_NOTE,
            "resolutionNote.contains=" + UPDATED_RESOLUTION_NOTE
        );
    }

    @Test
    @Transactional
    void getAllOrderIssuesByResolutionNoteNotContainsSomething() throws Exception {
        // Initialize the database
        insertedOrderIssue = orderIssueRepository.saveAndFlush(orderIssue);

        // Get all the orderIssueList where resolutionNote does not contain
        defaultOrderIssueFiltering(
            "resolutionNote.doesNotContain=" + UPDATED_RESOLUTION_NOTE,
            "resolutionNote.doesNotContain=" + DEFAULT_RESOLUTION_NOTE
        );
    }

    private void defaultOrderIssueFiltering(String shouldBeFound, String shouldNotBeFound) throws Exception {
        defaultOrderIssueShouldBeFound(shouldBeFound);
        defaultOrderIssueShouldNotBeFound(shouldNotBeFound);
    }

    /**
     * Executes the search, and checks that the default entity is returned.
     */
    private void defaultOrderIssueShouldBeFound(String filter) throws Exception {
        restOrderIssueMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(orderIssue.getId().intValue())))
            .andExpect(jsonPath("$.[*].issueType").value(hasItem(DEFAULT_ISSUE_TYPE.toString())))
            .andExpect(jsonPath("$.[*].issueStatus").value(hasItem(DEFAULT_ISSUE_STATUS.toString())))
            .andExpect(jsonPath("$.[*].reason").value(hasItem(DEFAULT_REASON)))
            .andExpect(jsonPath("$.[*].openedAt").value(hasItem(DEFAULT_OPENED_AT.toString())))
            .andExpect(jsonPath("$.[*].openedByUsername").value(hasItem(DEFAULT_OPENED_BY_USERNAME)))
            .andExpect(jsonPath("$.[*].resolvedAt").value(hasItem(DEFAULT_RESOLVED_AT.toString())))
            .andExpect(jsonPath("$.[*].resolvedByUsername").value(hasItem(DEFAULT_RESOLVED_BY_USERNAME)))
            .andExpect(jsonPath("$.[*].resolutionNote").value(hasItem(DEFAULT_RESOLUTION_NOTE)));

        // Check, that the count call also returns 1
        restOrderIssueMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("1"));
    }

    /**
     * Executes the search, and checks that the default entity is not returned.
     */
    private void defaultOrderIssueShouldNotBeFound(String filter) throws Exception {
        restOrderIssueMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());

        // Check, that the count call also returns 0
        restOrderIssueMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("0"));
    }

    @Test
    @Transactional
    void getNonExistingOrderIssue() throws Exception {
        // Get the orderIssue
        restOrderIssueMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingOrderIssue() throws Exception {
        // Initialize the database
        insertedOrderIssue = orderIssueRepository.saveAndFlush(orderIssue);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the orderIssue
        OrderIssue updatedOrderIssue = orderIssueRepository.findById(orderIssue.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedOrderIssue are not directly saved in db
        em.detach(updatedOrderIssue);
        updatedOrderIssue
            .issueType(UPDATED_ISSUE_TYPE)
            .issueStatus(UPDATED_ISSUE_STATUS)
            .reason(UPDATED_REASON)
            .openedAt(UPDATED_OPENED_AT)
            .openedByUsername(UPDATED_OPENED_BY_USERNAME)
            .resolvedAt(UPDATED_RESOLVED_AT)
            .resolvedByUsername(UPDATED_RESOLVED_BY_USERNAME)
            .resolutionNote(UPDATED_RESOLUTION_NOTE);
        OrderIssueDTO orderIssueDTO = orderIssueMapper.toDto(updatedOrderIssue);

        restOrderIssueMockMvc
            .perform(
                put(ENTITY_API_URL_ID, orderIssueDTO.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(orderIssueDTO))
            )
            .andExpect(status().isOk());

        // Validate the OrderIssue in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedOrderIssueToMatchAllProperties(updatedOrderIssue);
    }

    @Test
    @Transactional
    void putNonExistingOrderIssue() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        orderIssue.setId(longCount.incrementAndGet());

        // Create the OrderIssue
        OrderIssueDTO orderIssueDTO = orderIssueMapper.toDto(orderIssue);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restOrderIssueMockMvc
            .perform(
                put(ENTITY_API_URL_ID, orderIssueDTO.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(orderIssueDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the OrderIssue in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchOrderIssue() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        orderIssue.setId(longCount.incrementAndGet());

        // Create the OrderIssue
        OrderIssueDTO orderIssueDTO = orderIssueMapper.toDto(orderIssue);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restOrderIssueMockMvc
            .perform(
                put(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(orderIssueDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the OrderIssue in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamOrderIssue() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        orderIssue.setId(longCount.incrementAndGet());

        // Create the OrderIssue
        OrderIssueDTO orderIssueDTO = orderIssueMapper.toDto(orderIssue);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restOrderIssueMockMvc
            .perform(put(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(orderIssueDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the OrderIssue in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateOrderIssueWithPatch() throws Exception {
        // Initialize the database
        insertedOrderIssue = orderIssueRepository.saveAndFlush(orderIssue);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the orderIssue using partial update
        OrderIssue partialUpdatedOrderIssue = new OrderIssue();
        partialUpdatedOrderIssue.setId(orderIssue.getId());

        partialUpdatedOrderIssue
            .issueType(UPDATED_ISSUE_TYPE)
            .issueStatus(UPDATED_ISSUE_STATUS)
            .reason(UPDATED_REASON)
            .openedAt(UPDATED_OPENED_AT)
            .resolvedByUsername(UPDATED_RESOLVED_BY_USERNAME)
            .resolutionNote(UPDATED_RESOLUTION_NOTE);

        restOrderIssueMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedOrderIssue.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedOrderIssue))
            )
            .andExpect(status().isOk());

        // Validate the OrderIssue in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertOrderIssueUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedOrderIssue, orderIssue),
            getPersistedOrderIssue(orderIssue)
        );
    }

    @Test
    @Transactional
    void fullUpdateOrderIssueWithPatch() throws Exception {
        // Initialize the database
        insertedOrderIssue = orderIssueRepository.saveAndFlush(orderIssue);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the orderIssue using partial update
        OrderIssue partialUpdatedOrderIssue = new OrderIssue();
        partialUpdatedOrderIssue.setId(orderIssue.getId());

        partialUpdatedOrderIssue
            .issueType(UPDATED_ISSUE_TYPE)
            .issueStatus(UPDATED_ISSUE_STATUS)
            .reason(UPDATED_REASON)
            .openedAt(UPDATED_OPENED_AT)
            .openedByUsername(UPDATED_OPENED_BY_USERNAME)
            .resolvedAt(UPDATED_RESOLVED_AT)
            .resolvedByUsername(UPDATED_RESOLVED_BY_USERNAME)
            .resolutionNote(UPDATED_RESOLUTION_NOTE);

        restOrderIssueMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedOrderIssue.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedOrderIssue))
            )
            .andExpect(status().isOk());

        // Validate the OrderIssue in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertOrderIssueUpdatableFieldsEquals(partialUpdatedOrderIssue, getPersistedOrderIssue(partialUpdatedOrderIssue));
    }

    @Test
    @Transactional
    void patchNonExistingOrderIssue() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        orderIssue.setId(longCount.incrementAndGet());

        // Create the OrderIssue
        OrderIssueDTO orderIssueDTO = orderIssueMapper.toDto(orderIssue);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restOrderIssueMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, orderIssueDTO.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(orderIssueDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the OrderIssue in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchOrderIssue() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        orderIssue.setId(longCount.incrementAndGet());

        // Create the OrderIssue
        OrderIssueDTO orderIssueDTO = orderIssueMapper.toDto(orderIssue);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restOrderIssueMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(orderIssueDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the OrderIssue in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamOrderIssue() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        orderIssue.setId(longCount.incrementAndGet());

        // Create the OrderIssue
        OrderIssueDTO orderIssueDTO = orderIssueMapper.toDto(orderIssue);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restOrderIssueMockMvc
            .perform(patch(ENTITY_API_URL).contentType("application/merge-patch+json").content(om.writeValueAsBytes(orderIssueDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the OrderIssue in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteOrderIssue() throws Exception {
        // Initialize the database
        insertedOrderIssue = orderIssueRepository.saveAndFlush(orderIssue);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the orderIssue
        restOrderIssueMockMvc
            .perform(delete(ENTITY_API_URL_ID, orderIssue.getId()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return orderIssueRepository.count();
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

    protected OrderIssue getPersistedOrderIssue(OrderIssue orderIssue) {
        return orderIssueRepository.findById(orderIssue.getId()).orElseThrow();
    }

    protected void assertPersistedOrderIssueToMatchAllProperties(OrderIssue expectedOrderIssue) {
        assertOrderIssueAllPropertiesEquals(expectedOrderIssue, getPersistedOrderIssue(expectedOrderIssue));
    }

    protected void assertPersistedOrderIssueToMatchUpdatableProperties(OrderIssue expectedOrderIssue) {
        assertOrderIssueAllUpdatablePropertiesEquals(expectedOrderIssue, getPersistedOrderIssue(expectedOrderIssue));
    }
}
