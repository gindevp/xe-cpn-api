package com.mycompany.myapp.web.rest;

import static com.mycompany.myapp.domain.DayClosureAsserts.*;
import static com.mycompany.myapp.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.myapp.IntegrationTest;
import com.mycompany.myapp.domain.DayClosure;
import com.mycompany.myapp.domain.Office;
import com.mycompany.myapp.domain.enumeration.DayClosureStatus;
import com.mycompany.myapp.repository.DayClosureRepository;
import com.mycompany.myapp.service.DayClosureService;
import com.mycompany.myapp.service.dto.DayClosureDTO;
import com.mycompany.myapp.service.mapper.DayClosureMapper;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
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
 * Integration tests for the {@link DayClosureResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser
class DayClosureResourceIT {

    private static final LocalDate DEFAULT_BUSINESS_DATE = LocalDate.ofEpochDay(0L);
    private static final LocalDate UPDATED_BUSINESS_DATE = LocalDate.now(ZoneId.systemDefault());
    private static final LocalDate SMALLER_BUSINESS_DATE = LocalDate.ofEpochDay(-1L);

    private static final DayClosureStatus DEFAULT_STATUS = DayClosureStatus.OPEN;
    private static final DayClosureStatus UPDATED_STATUS = DayClosureStatus.CLOSED;

    private static final String DEFAULT_CONFIRMED_BY_USERNAME = "AAAAAAAAAA";
    private static final String UPDATED_CONFIRMED_BY_USERNAME = "BBBBBBBBBB";

    private static final Instant DEFAULT_CONFIRMED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_CONFIRMED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final String DEFAULT_REOPENED_BY_USERNAME = "AAAAAAAAAA";
    private static final String UPDATED_REOPENED_BY_USERNAME = "BBBBBBBBBB";

    private static final Instant DEFAULT_REOPENED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_REOPENED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final String ENTITY_API_URL = "/api/day-closure-entities";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static Random random = new Random();
    private static AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    @Autowired
    private ObjectMapper om;

    @Autowired
    private DayClosureRepository dayClosureRepository;

    @Mock
    private DayClosureRepository dayClosureRepositoryMock;

    @Autowired
    private DayClosureMapper dayClosureMapper;

    @Mock
    private DayClosureService dayClosureServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restDayClosureMockMvc;

    private DayClosure dayClosure;

    private DayClosure insertedDayClosure;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static DayClosure createEntity(EntityManager em) {
        DayClosure dayClosure = new DayClosure()
            .businessDate(DEFAULT_BUSINESS_DATE)
            .status(DEFAULT_STATUS)
            .confirmedByUsername(DEFAULT_CONFIRMED_BY_USERNAME)
            .confirmedAt(DEFAULT_CONFIRMED_AT)
            .reopenedByUsername(DEFAULT_REOPENED_BY_USERNAME)
            .reopenedAt(DEFAULT_REOPENED_AT);
        // Add required entity
        Office office;
        if (TestUtil.findAll(em, Office.class).isEmpty()) {
            office = OfficeResourceIT.createEntity();
            em.persist(office);
            em.flush();
        } else {
            office = TestUtil.findAll(em, Office.class).get(0);
        }
        dayClosure.setOffice(office);
        return dayClosure;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static DayClosure createUpdatedEntity(EntityManager em) {
        DayClosure updatedDayClosure = new DayClosure()
            .businessDate(UPDATED_BUSINESS_DATE)
            .status(UPDATED_STATUS)
            .confirmedByUsername(UPDATED_CONFIRMED_BY_USERNAME)
            .confirmedAt(UPDATED_CONFIRMED_AT)
            .reopenedByUsername(UPDATED_REOPENED_BY_USERNAME)
            .reopenedAt(UPDATED_REOPENED_AT);
        // Add required entity
        Office office;
        if (TestUtil.findAll(em, Office.class).isEmpty()) {
            office = OfficeResourceIT.createUpdatedEntity();
            em.persist(office);
            em.flush();
        } else {
            office = TestUtil.findAll(em, Office.class).get(0);
        }
        updatedDayClosure.setOffice(office);
        return updatedDayClosure;
    }

    @BeforeEach
    public void initTest() {
        dayClosure = createEntity(em);
    }

    @AfterEach
    public void cleanup() {
        if (insertedDayClosure != null) {
            dayClosureRepository.delete(insertedDayClosure);
            insertedDayClosure = null;
        }
    }

    @Test
    @Transactional
    void createDayClosure() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the DayClosure
        DayClosureDTO dayClosureDTO = dayClosureMapper.toDto(dayClosure);
        var returnedDayClosureDTO = om.readValue(
            restDayClosureMockMvc
                .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(dayClosureDTO)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            DayClosureDTO.class
        );

        // Validate the DayClosure in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedDayClosure = dayClosureMapper.toEntity(returnedDayClosureDTO);
        assertDayClosureUpdatableFieldsEquals(returnedDayClosure, getPersistedDayClosure(returnedDayClosure));

        insertedDayClosure = returnedDayClosure;
    }

    @Test
    @Transactional
    void createDayClosureWithExistingId() throws Exception {
        // Create the DayClosure with an existing ID
        dayClosure.setId(1L);
        DayClosureDTO dayClosureDTO = dayClosureMapper.toDto(dayClosure);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restDayClosureMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(dayClosureDTO)))
            .andExpect(status().isBadRequest());

        // Validate the DayClosure in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkBusinessDateIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        dayClosure.setBusinessDate(null);

        // Create the DayClosure, which fails.
        DayClosureDTO dayClosureDTO = dayClosureMapper.toDto(dayClosure);

        restDayClosureMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(dayClosureDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkStatusIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        dayClosure.setStatus(null);

        // Create the DayClosure, which fails.
        DayClosureDTO dayClosureDTO = dayClosureMapper.toDto(dayClosure);

        restDayClosureMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(dayClosureDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkConfirmedByUsernameIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        dayClosure.setConfirmedByUsername(null);

        // Create the DayClosure, which fails.
        DayClosureDTO dayClosureDTO = dayClosureMapper.toDto(dayClosure);

        restDayClosureMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(dayClosureDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkConfirmedAtIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        dayClosure.setConfirmedAt(null);

        // Create the DayClosure, which fails.
        DayClosureDTO dayClosureDTO = dayClosureMapper.toDto(dayClosure);

        restDayClosureMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(dayClosureDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllDayClosures() throws Exception {
        // Initialize the database
        insertedDayClosure = dayClosureRepository.saveAndFlush(dayClosure);

        // Get all the dayClosureList
        restDayClosureMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(dayClosure.getId().intValue())))
            .andExpect(jsonPath("$.[*].businessDate").value(hasItem(DEFAULT_BUSINESS_DATE.toString())))
            .andExpect(jsonPath("$.[*].status").value(hasItem(DEFAULT_STATUS.toString())))
            .andExpect(jsonPath("$.[*].confirmedByUsername").value(hasItem(DEFAULT_CONFIRMED_BY_USERNAME)))
            .andExpect(jsonPath("$.[*].confirmedAt").value(hasItem(DEFAULT_CONFIRMED_AT.toString())))
            .andExpect(jsonPath("$.[*].reopenedByUsername").value(hasItem(DEFAULT_REOPENED_BY_USERNAME)))
            .andExpect(jsonPath("$.[*].reopenedAt").value(hasItem(DEFAULT_REOPENED_AT.toString())));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllDayClosuresWithEagerRelationshipsIsEnabled() throws Exception {
        when(dayClosureServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restDayClosureMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(dayClosureServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllDayClosuresWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(dayClosureServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restDayClosureMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(dayClosureRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getDayClosure() throws Exception {
        // Initialize the database
        insertedDayClosure = dayClosureRepository.saveAndFlush(dayClosure);

        // Get the dayClosure
        restDayClosureMockMvc
            .perform(get(ENTITY_API_URL_ID, dayClosure.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(dayClosure.getId().intValue()))
            .andExpect(jsonPath("$.businessDate").value(DEFAULT_BUSINESS_DATE.toString()))
            .andExpect(jsonPath("$.status").value(DEFAULT_STATUS.toString()))
            .andExpect(jsonPath("$.confirmedByUsername").value(DEFAULT_CONFIRMED_BY_USERNAME))
            .andExpect(jsonPath("$.confirmedAt").value(DEFAULT_CONFIRMED_AT.toString()))
            .andExpect(jsonPath("$.reopenedByUsername").value(DEFAULT_REOPENED_BY_USERNAME))
            .andExpect(jsonPath("$.reopenedAt").value(DEFAULT_REOPENED_AT.toString()));
    }

    @Test
    @Transactional
    void getDayClosuresByIdFiltering() throws Exception {
        // Initialize the database
        insertedDayClosure = dayClosureRepository.saveAndFlush(dayClosure);

        Long id = dayClosure.getId();

        defaultDayClosureFiltering("id.equals=" + id, "id.notEquals=" + id);

        defaultDayClosureFiltering("id.greaterThanOrEqual=" + id, "id.greaterThan=" + id);

        defaultDayClosureFiltering("id.lessThanOrEqual=" + id, "id.lessThan=" + id);
    }

    @Test
    @Transactional
    void getAllDayClosuresByBusinessDateIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedDayClosure = dayClosureRepository.saveAndFlush(dayClosure);

        // Get all the dayClosureList where businessDate equals to
        defaultDayClosureFiltering("businessDate.equals=" + DEFAULT_BUSINESS_DATE, "businessDate.equals=" + UPDATED_BUSINESS_DATE);
    }

    @Test
    @Transactional
    void getAllDayClosuresByBusinessDateIsInShouldWork() throws Exception {
        // Initialize the database
        insertedDayClosure = dayClosureRepository.saveAndFlush(dayClosure);

        // Get all the dayClosureList where businessDate in
        defaultDayClosureFiltering(
            "businessDate.in=" + DEFAULT_BUSINESS_DATE + "," + UPDATED_BUSINESS_DATE,
            "businessDate.in=" + UPDATED_BUSINESS_DATE
        );
    }

    @Test
    @Transactional
    void getAllDayClosuresByBusinessDateIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedDayClosure = dayClosureRepository.saveAndFlush(dayClosure);

        // Get all the dayClosureList where businessDate is not null
        defaultDayClosureFiltering("businessDate.specified=true", "businessDate.specified=false");
    }

    @Test
    @Transactional
    void getAllDayClosuresByBusinessDateIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedDayClosure = dayClosureRepository.saveAndFlush(dayClosure);

        // Get all the dayClosureList where businessDate is greater than or equal to
        defaultDayClosureFiltering(
            "businessDate.greaterThanOrEqual=" + DEFAULT_BUSINESS_DATE,
            "businessDate.greaterThanOrEqual=" + UPDATED_BUSINESS_DATE
        );
    }

    @Test
    @Transactional
    void getAllDayClosuresByBusinessDateIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedDayClosure = dayClosureRepository.saveAndFlush(dayClosure);

        // Get all the dayClosureList where businessDate is less than or equal to
        defaultDayClosureFiltering(
            "businessDate.lessThanOrEqual=" + DEFAULT_BUSINESS_DATE,
            "businessDate.lessThanOrEqual=" + SMALLER_BUSINESS_DATE
        );
    }

    @Test
    @Transactional
    void getAllDayClosuresByBusinessDateIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedDayClosure = dayClosureRepository.saveAndFlush(dayClosure);

        // Get all the dayClosureList where businessDate is less than
        defaultDayClosureFiltering("businessDate.lessThan=" + UPDATED_BUSINESS_DATE, "businessDate.lessThan=" + DEFAULT_BUSINESS_DATE);
    }

    @Test
    @Transactional
    void getAllDayClosuresByBusinessDateIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedDayClosure = dayClosureRepository.saveAndFlush(dayClosure);

        // Get all the dayClosureList where businessDate is greater than
        defaultDayClosureFiltering(
            "businessDate.greaterThan=" + SMALLER_BUSINESS_DATE,
            "businessDate.greaterThan=" + DEFAULT_BUSINESS_DATE
        );
    }

    @Test
    @Transactional
    void getAllDayClosuresByStatusIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedDayClosure = dayClosureRepository.saveAndFlush(dayClosure);

        // Get all the dayClosureList where status equals to
        defaultDayClosureFiltering("status.equals=" + DEFAULT_STATUS, "status.equals=" + UPDATED_STATUS);
    }

    @Test
    @Transactional
    void getAllDayClosuresByStatusIsInShouldWork() throws Exception {
        // Initialize the database
        insertedDayClosure = dayClosureRepository.saveAndFlush(dayClosure);

        // Get all the dayClosureList where status in
        defaultDayClosureFiltering("status.in=" + DEFAULT_STATUS + "," + UPDATED_STATUS, "status.in=" + UPDATED_STATUS);
    }

    @Test
    @Transactional
    void getAllDayClosuresByStatusIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedDayClosure = dayClosureRepository.saveAndFlush(dayClosure);

        // Get all the dayClosureList where status is not null
        defaultDayClosureFiltering("status.specified=true", "status.specified=false");
    }

    @Test
    @Transactional
    void getAllDayClosuresByConfirmedByUsernameIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedDayClosure = dayClosureRepository.saveAndFlush(dayClosure);

        // Get all the dayClosureList where confirmedByUsername equals to
        defaultDayClosureFiltering(
            "confirmedByUsername.equals=" + DEFAULT_CONFIRMED_BY_USERNAME,
            "confirmedByUsername.equals=" + UPDATED_CONFIRMED_BY_USERNAME
        );
    }

    @Test
    @Transactional
    void getAllDayClosuresByConfirmedByUsernameIsInShouldWork() throws Exception {
        // Initialize the database
        insertedDayClosure = dayClosureRepository.saveAndFlush(dayClosure);

        // Get all the dayClosureList where confirmedByUsername in
        defaultDayClosureFiltering(
            "confirmedByUsername.in=" + DEFAULT_CONFIRMED_BY_USERNAME + "," + UPDATED_CONFIRMED_BY_USERNAME,
            "confirmedByUsername.in=" + UPDATED_CONFIRMED_BY_USERNAME
        );
    }

    @Test
    @Transactional
    void getAllDayClosuresByConfirmedByUsernameIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedDayClosure = dayClosureRepository.saveAndFlush(dayClosure);

        // Get all the dayClosureList where confirmedByUsername is not null
        defaultDayClosureFiltering("confirmedByUsername.specified=true", "confirmedByUsername.specified=false");
    }

    @Test
    @Transactional
    void getAllDayClosuresByConfirmedByUsernameContainsSomething() throws Exception {
        // Initialize the database
        insertedDayClosure = dayClosureRepository.saveAndFlush(dayClosure);

        // Get all the dayClosureList where confirmedByUsername contains
        defaultDayClosureFiltering(
            "confirmedByUsername.contains=" + DEFAULT_CONFIRMED_BY_USERNAME,
            "confirmedByUsername.contains=" + UPDATED_CONFIRMED_BY_USERNAME
        );
    }

    @Test
    @Transactional
    void getAllDayClosuresByConfirmedByUsernameNotContainsSomething() throws Exception {
        // Initialize the database
        insertedDayClosure = dayClosureRepository.saveAndFlush(dayClosure);

        // Get all the dayClosureList where confirmedByUsername does not contain
        defaultDayClosureFiltering(
            "confirmedByUsername.doesNotContain=" + UPDATED_CONFIRMED_BY_USERNAME,
            "confirmedByUsername.doesNotContain=" + DEFAULT_CONFIRMED_BY_USERNAME
        );
    }

    @Test
    @Transactional
    void getAllDayClosuresByConfirmedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedDayClosure = dayClosureRepository.saveAndFlush(dayClosure);

        // Get all the dayClosureList where confirmedAt equals to
        defaultDayClosureFiltering("confirmedAt.equals=" + DEFAULT_CONFIRMED_AT, "confirmedAt.equals=" + UPDATED_CONFIRMED_AT);
    }

    @Test
    @Transactional
    void getAllDayClosuresByConfirmedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedDayClosure = dayClosureRepository.saveAndFlush(dayClosure);

        // Get all the dayClosureList where confirmedAt in
        defaultDayClosureFiltering(
            "confirmedAt.in=" + DEFAULT_CONFIRMED_AT + "," + UPDATED_CONFIRMED_AT,
            "confirmedAt.in=" + UPDATED_CONFIRMED_AT
        );
    }

    @Test
    @Transactional
    void getAllDayClosuresByConfirmedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedDayClosure = dayClosureRepository.saveAndFlush(dayClosure);

        // Get all the dayClosureList where confirmedAt is not null
        defaultDayClosureFiltering("confirmedAt.specified=true", "confirmedAt.specified=false");
    }

    @Test
    @Transactional
    void getAllDayClosuresByReopenedByUsernameIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedDayClosure = dayClosureRepository.saveAndFlush(dayClosure);

        // Get all the dayClosureList where reopenedByUsername equals to
        defaultDayClosureFiltering(
            "reopenedByUsername.equals=" + DEFAULT_REOPENED_BY_USERNAME,
            "reopenedByUsername.equals=" + UPDATED_REOPENED_BY_USERNAME
        );
    }

    @Test
    @Transactional
    void getAllDayClosuresByReopenedByUsernameIsInShouldWork() throws Exception {
        // Initialize the database
        insertedDayClosure = dayClosureRepository.saveAndFlush(dayClosure);

        // Get all the dayClosureList where reopenedByUsername in
        defaultDayClosureFiltering(
            "reopenedByUsername.in=" + DEFAULT_REOPENED_BY_USERNAME + "," + UPDATED_REOPENED_BY_USERNAME,
            "reopenedByUsername.in=" + UPDATED_REOPENED_BY_USERNAME
        );
    }

    @Test
    @Transactional
    void getAllDayClosuresByReopenedByUsernameIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedDayClosure = dayClosureRepository.saveAndFlush(dayClosure);

        // Get all the dayClosureList where reopenedByUsername is not null
        defaultDayClosureFiltering("reopenedByUsername.specified=true", "reopenedByUsername.specified=false");
    }

    @Test
    @Transactional
    void getAllDayClosuresByReopenedByUsernameContainsSomething() throws Exception {
        // Initialize the database
        insertedDayClosure = dayClosureRepository.saveAndFlush(dayClosure);

        // Get all the dayClosureList where reopenedByUsername contains
        defaultDayClosureFiltering(
            "reopenedByUsername.contains=" + DEFAULT_REOPENED_BY_USERNAME,
            "reopenedByUsername.contains=" + UPDATED_REOPENED_BY_USERNAME
        );
    }

    @Test
    @Transactional
    void getAllDayClosuresByReopenedByUsernameNotContainsSomething() throws Exception {
        // Initialize the database
        insertedDayClosure = dayClosureRepository.saveAndFlush(dayClosure);

        // Get all the dayClosureList where reopenedByUsername does not contain
        defaultDayClosureFiltering(
            "reopenedByUsername.doesNotContain=" + UPDATED_REOPENED_BY_USERNAME,
            "reopenedByUsername.doesNotContain=" + DEFAULT_REOPENED_BY_USERNAME
        );
    }

    @Test
    @Transactional
    void getAllDayClosuresByReopenedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedDayClosure = dayClosureRepository.saveAndFlush(dayClosure);

        // Get all the dayClosureList where reopenedAt equals to
        defaultDayClosureFiltering("reopenedAt.equals=" + DEFAULT_REOPENED_AT, "reopenedAt.equals=" + UPDATED_REOPENED_AT);
    }

    @Test
    @Transactional
    void getAllDayClosuresByReopenedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedDayClosure = dayClosureRepository.saveAndFlush(dayClosure);

        // Get all the dayClosureList where reopenedAt in
        defaultDayClosureFiltering(
            "reopenedAt.in=" + DEFAULT_REOPENED_AT + "," + UPDATED_REOPENED_AT,
            "reopenedAt.in=" + UPDATED_REOPENED_AT
        );
    }

    @Test
    @Transactional
    void getAllDayClosuresByReopenedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedDayClosure = dayClosureRepository.saveAndFlush(dayClosure);

        // Get all the dayClosureList where reopenedAt is not null
        defaultDayClosureFiltering("reopenedAt.specified=true", "reopenedAt.specified=false");
    }

    @Test
    @Transactional
    void getAllDayClosuresByOfficeIsEqualToSomething() throws Exception {
        Office office;
        if (TestUtil.findAll(em, Office.class).isEmpty()) {
            dayClosureRepository.saveAndFlush(dayClosure);
            office = OfficeResourceIT.createEntity();
        } else {
            office = TestUtil.findAll(em, Office.class).get(0);
        }
        em.persist(office);
        em.flush();
        dayClosure.setOffice(office);
        dayClosureRepository.saveAndFlush(dayClosure);
        Long officeId = office.getId();
        // Get all the dayClosureList where office equals to officeId
        defaultDayClosureShouldBeFound("officeId.equals=" + officeId);

        // Get all the dayClosureList where office equals to (officeId + 1)
        defaultDayClosureShouldNotBeFound("officeId.equals=" + (officeId + 1));
    }

    private void defaultDayClosureFiltering(String shouldBeFound, String shouldNotBeFound) throws Exception {
        defaultDayClosureShouldBeFound(shouldBeFound);
        defaultDayClosureShouldNotBeFound(shouldNotBeFound);
    }

    /**
     * Executes the search, and checks that the default entity is returned.
     */
    private void defaultDayClosureShouldBeFound(String filter) throws Exception {
        restDayClosureMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(dayClosure.getId().intValue())))
            .andExpect(jsonPath("$.[*].businessDate").value(hasItem(DEFAULT_BUSINESS_DATE.toString())))
            .andExpect(jsonPath("$.[*].status").value(hasItem(DEFAULT_STATUS.toString())))
            .andExpect(jsonPath("$.[*].confirmedByUsername").value(hasItem(DEFAULT_CONFIRMED_BY_USERNAME)))
            .andExpect(jsonPath("$.[*].confirmedAt").value(hasItem(DEFAULT_CONFIRMED_AT.toString())))
            .andExpect(jsonPath("$.[*].reopenedByUsername").value(hasItem(DEFAULT_REOPENED_BY_USERNAME)))
            .andExpect(jsonPath("$.[*].reopenedAt").value(hasItem(DEFAULT_REOPENED_AT.toString())));

        // Check, that the count call also returns 1
        restDayClosureMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("1"));
    }

    /**
     * Executes the search, and checks that the default entity is not returned.
     */
    private void defaultDayClosureShouldNotBeFound(String filter) throws Exception {
        restDayClosureMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());

        // Check, that the count call also returns 0
        restDayClosureMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("0"));
    }

    @Test
    @Transactional
    void getNonExistingDayClosure() throws Exception {
        // Get the dayClosure
        restDayClosureMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingDayClosure() throws Exception {
        // Initialize the database
        insertedDayClosure = dayClosureRepository.saveAndFlush(dayClosure);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the dayClosure
        DayClosure updatedDayClosure = dayClosureRepository.findById(dayClosure.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedDayClosure are not directly saved in db
        em.detach(updatedDayClosure);
        updatedDayClosure
            .businessDate(UPDATED_BUSINESS_DATE)
            .status(UPDATED_STATUS)
            .confirmedByUsername(UPDATED_CONFIRMED_BY_USERNAME)
            .confirmedAt(UPDATED_CONFIRMED_AT)
            .reopenedByUsername(UPDATED_REOPENED_BY_USERNAME)
            .reopenedAt(UPDATED_REOPENED_AT);
        DayClosureDTO dayClosureDTO = dayClosureMapper.toDto(updatedDayClosure);

        restDayClosureMockMvc
            .perform(
                put(ENTITY_API_URL_ID, dayClosureDTO.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(dayClosureDTO))
            )
            .andExpect(status().isOk());

        // Validate the DayClosure in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedDayClosureToMatchAllProperties(updatedDayClosure);
    }

    @Test
    @Transactional
    void putNonExistingDayClosure() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        dayClosure.setId(longCount.incrementAndGet());

        // Create the DayClosure
        DayClosureDTO dayClosureDTO = dayClosureMapper.toDto(dayClosure);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restDayClosureMockMvc
            .perform(
                put(ENTITY_API_URL_ID, dayClosureDTO.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(dayClosureDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the DayClosure in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchDayClosure() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        dayClosure.setId(longCount.incrementAndGet());

        // Create the DayClosure
        DayClosureDTO dayClosureDTO = dayClosureMapper.toDto(dayClosure);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restDayClosureMockMvc
            .perform(
                put(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(dayClosureDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the DayClosure in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamDayClosure() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        dayClosure.setId(longCount.incrementAndGet());

        // Create the DayClosure
        DayClosureDTO dayClosureDTO = dayClosureMapper.toDto(dayClosure);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restDayClosureMockMvc
            .perform(put(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(dayClosureDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the DayClosure in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateDayClosureWithPatch() throws Exception {
        // Initialize the database
        insertedDayClosure = dayClosureRepository.saveAndFlush(dayClosure);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the dayClosure using partial update
        DayClosure partialUpdatedDayClosure = new DayClosure();
        partialUpdatedDayClosure.setId(dayClosure.getId());

        partialUpdatedDayClosure
            .businessDate(UPDATED_BUSINESS_DATE)
            .status(UPDATED_STATUS)
            .confirmedByUsername(UPDATED_CONFIRMED_BY_USERNAME)
            .confirmedAt(UPDATED_CONFIRMED_AT);

        restDayClosureMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedDayClosure.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedDayClosure))
            )
            .andExpect(status().isOk());

        // Validate the DayClosure in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertDayClosureUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedDayClosure, dayClosure),
            getPersistedDayClosure(dayClosure)
        );
    }

    @Test
    @Transactional
    void fullUpdateDayClosureWithPatch() throws Exception {
        // Initialize the database
        insertedDayClosure = dayClosureRepository.saveAndFlush(dayClosure);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the dayClosure using partial update
        DayClosure partialUpdatedDayClosure = new DayClosure();
        partialUpdatedDayClosure.setId(dayClosure.getId());

        partialUpdatedDayClosure
            .businessDate(UPDATED_BUSINESS_DATE)
            .status(UPDATED_STATUS)
            .confirmedByUsername(UPDATED_CONFIRMED_BY_USERNAME)
            .confirmedAt(UPDATED_CONFIRMED_AT)
            .reopenedByUsername(UPDATED_REOPENED_BY_USERNAME)
            .reopenedAt(UPDATED_REOPENED_AT);

        restDayClosureMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedDayClosure.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedDayClosure))
            )
            .andExpect(status().isOk());

        // Validate the DayClosure in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertDayClosureUpdatableFieldsEquals(partialUpdatedDayClosure, getPersistedDayClosure(partialUpdatedDayClosure));
    }

    @Test
    @Transactional
    void patchNonExistingDayClosure() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        dayClosure.setId(longCount.incrementAndGet());

        // Create the DayClosure
        DayClosureDTO dayClosureDTO = dayClosureMapper.toDto(dayClosure);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restDayClosureMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, dayClosureDTO.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(dayClosureDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the DayClosure in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchDayClosure() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        dayClosure.setId(longCount.incrementAndGet());

        // Create the DayClosure
        DayClosureDTO dayClosureDTO = dayClosureMapper.toDto(dayClosure);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restDayClosureMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(dayClosureDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the DayClosure in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamDayClosure() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        dayClosure.setId(longCount.incrementAndGet());

        // Create the DayClosure
        DayClosureDTO dayClosureDTO = dayClosureMapper.toDto(dayClosure);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restDayClosureMockMvc
            .perform(patch(ENTITY_API_URL).contentType("application/merge-patch+json").content(om.writeValueAsBytes(dayClosureDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the DayClosure in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteDayClosure() throws Exception {
        // Initialize the database
        insertedDayClosure = dayClosureRepository.saveAndFlush(dayClosure);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the dayClosure
        restDayClosureMockMvc
            .perform(delete(ENTITY_API_URL_ID, dayClosure.getId()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return dayClosureRepository.count();
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

    protected DayClosure getPersistedDayClosure(DayClosure dayClosure) {
        return dayClosureRepository.findById(dayClosure.getId()).orElseThrow();
    }

    protected void assertPersistedDayClosureToMatchAllProperties(DayClosure expectedDayClosure) {
        assertDayClosureAllPropertiesEquals(expectedDayClosure, getPersistedDayClosure(expectedDayClosure));
    }

    protected void assertPersistedDayClosureToMatchUpdatableProperties(DayClosure expectedDayClosure) {
        assertDayClosureAllUpdatablePropertiesEquals(expectedDayClosure, getPersistedDayClosure(expectedDayClosure));
    }
}
