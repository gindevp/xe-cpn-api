package com.mycompany.myapp.web.rest;

import static com.mycompany.myapp.domain.TripAsserts.*;
import static com.mycompany.myapp.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.myapp.IntegrationTest;
import com.mycompany.myapp.domain.Driver;
import com.mycompany.myapp.domain.Office;
import com.mycompany.myapp.domain.Route;
import com.mycompany.myapp.domain.Trip;
import com.mycompany.myapp.domain.Vehicle;
import com.mycompany.myapp.domain.enumeration.TripStatus;
import com.mycompany.myapp.repository.TripRepository;
import com.mycompany.myapp.service.TripService;
import com.mycompany.myapp.service.dto.TripDTO;
import com.mycompany.myapp.service.mapper.TripMapper;
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
 * Integration tests for the {@link TripResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser
class TripResourceIT {

    private static final String DEFAULT_TRIP_CODE = "AAAAAAAAAA";
    private static final String UPDATED_TRIP_CODE = "BBBBBBBBBB";

    private static final TripStatus DEFAULT_STATUS = TripStatus.CREATED;
    private static final TripStatus UPDATED_STATUS = TripStatus.LOADING;

    private static final Instant DEFAULT_DEPART_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_DEPART_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final Integer DEFAULT_LOADED_COUNT = 0;
    private static final Integer UPDATED_LOADED_COUNT = 1;
    private static final Integer SMALLER_LOADED_COUNT = 0 - 1;

    private static final Integer DEFAULT_SCANNED_COUNT = 0;
    private static final Integer UPDATED_SCANNED_COUNT = 1;
    private static final Integer SMALLER_SCANNED_COUNT = 0 - 1;

    private static final Instant DEFAULT_CLOSED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_CLOSED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final Boolean DEFAULT_FORCE_CLOSED = false;
    private static final Boolean UPDATED_FORCE_CLOSED = true;

    private static final String DEFAULT_FORCE_CLOSE_REASON = "AAAAAAAAAA";
    private static final String UPDATED_FORCE_CLOSE_REASON = "BBBBBBBBBB";

    private static final String ENTITY_API_URL = "/api/trip-entities";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static Random random = new Random();
    private static AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    @Autowired
    private ObjectMapper om;

    @Autowired
    private TripRepository tripRepository;

    @Mock
    private TripRepository tripRepositoryMock;

    @Autowired
    private TripMapper tripMapper;

    @Mock
    private TripService tripServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restTripMockMvc;

    private Trip trip;

    private Trip insertedTrip;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Trip createEntity(EntityManager em) {
        Trip trip = new Trip()
            .tripCode(DEFAULT_TRIP_CODE)
            .status(DEFAULT_STATUS)
            .departAt(DEFAULT_DEPART_AT)
            .loadedCount(DEFAULT_LOADED_COUNT)
            .scannedCount(DEFAULT_SCANNED_COUNT)
            .closedAt(DEFAULT_CLOSED_AT)
            .forceClosed(DEFAULT_FORCE_CLOSED)
            .forceCloseReason(DEFAULT_FORCE_CLOSE_REASON);
        // Add required entity
        Office office;
        if (TestUtil.findAll(em, Office.class).isEmpty()) {
            office = OfficeResourceIT.createEntity();
            em.persist(office);
            em.flush();
        } else {
            office = TestUtil.findAll(em, Office.class).get(0);
        }
        trip.setOffice(office);
        // Add required entity
        Route route;
        if (TestUtil.findAll(em, Route.class).isEmpty()) {
            route = RouteResourceIT.createEntity(em);
            em.persist(route);
            em.flush();
        } else {
            route = TestUtil.findAll(em, Route.class).get(0);
        }
        trip.setRoute(route);
        // Add required entity
        Vehicle vehicle;
        if (TestUtil.findAll(em, Vehicle.class).isEmpty()) {
            vehicle = VehicleResourceIT.createEntity();
            em.persist(vehicle);
            em.flush();
        } else {
            vehicle = TestUtil.findAll(em, Vehicle.class).get(0);
        }
        trip.setVehicle(vehicle);
        // Add required entity
        Driver driver;
        if (TestUtil.findAll(em, Driver.class).isEmpty()) {
            driver = DriverResourceIT.createEntity();
            em.persist(driver);
            em.flush();
        } else {
            driver = TestUtil.findAll(em, Driver.class).get(0);
        }
        trip.setDriver(driver);
        return trip;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Trip createUpdatedEntity(EntityManager em) {
        Trip updatedTrip = new Trip()
            .tripCode(UPDATED_TRIP_CODE)
            .status(UPDATED_STATUS)
            .departAt(UPDATED_DEPART_AT)
            .loadedCount(UPDATED_LOADED_COUNT)
            .scannedCount(UPDATED_SCANNED_COUNT)
            .closedAt(UPDATED_CLOSED_AT)
            .forceClosed(UPDATED_FORCE_CLOSED)
            .forceCloseReason(UPDATED_FORCE_CLOSE_REASON);
        // Add required entity
        Office office;
        if (TestUtil.findAll(em, Office.class).isEmpty()) {
            office = OfficeResourceIT.createUpdatedEntity();
            em.persist(office);
            em.flush();
        } else {
            office = TestUtil.findAll(em, Office.class).get(0);
        }
        updatedTrip.setOffice(office);
        // Add required entity
        Route route;
        if (TestUtil.findAll(em, Route.class).isEmpty()) {
            route = RouteResourceIT.createUpdatedEntity(em);
            em.persist(route);
            em.flush();
        } else {
            route = TestUtil.findAll(em, Route.class).get(0);
        }
        updatedTrip.setRoute(route);
        // Add required entity
        Vehicle vehicle;
        if (TestUtil.findAll(em, Vehicle.class).isEmpty()) {
            vehicle = VehicleResourceIT.createUpdatedEntity();
            em.persist(vehicle);
            em.flush();
        } else {
            vehicle = TestUtil.findAll(em, Vehicle.class).get(0);
        }
        updatedTrip.setVehicle(vehicle);
        // Add required entity
        Driver driver;
        if (TestUtil.findAll(em, Driver.class).isEmpty()) {
            driver = DriverResourceIT.createUpdatedEntity();
            em.persist(driver);
            em.flush();
        } else {
            driver = TestUtil.findAll(em, Driver.class).get(0);
        }
        updatedTrip.setDriver(driver);
        return updatedTrip;
    }

    @BeforeEach
    public void initTest() {
        trip = createEntity(em);
    }

    @AfterEach
    public void cleanup() {
        if (insertedTrip != null) {
            tripRepository.delete(insertedTrip);
            insertedTrip = null;
        }
    }

    @Test
    @Transactional
    void createTrip() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the Trip
        TripDTO tripDTO = tripMapper.toDto(trip);
        var returnedTripDTO = om.readValue(
            restTripMockMvc
                .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(tripDTO)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            TripDTO.class
        );

        // Validate the Trip in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedTrip = tripMapper.toEntity(returnedTripDTO);
        assertTripUpdatableFieldsEquals(returnedTrip, getPersistedTrip(returnedTrip));

        insertedTrip = returnedTrip;
    }

    @Test
    @Transactional
    void createTripWithExistingId() throws Exception {
        // Create the Trip with an existing ID
        trip.setId(1L);
        TripDTO tripDTO = tripMapper.toDto(trip);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restTripMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(tripDTO)))
            .andExpect(status().isBadRequest());

        // Validate the Trip in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkTripCodeIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        trip.setTripCode(null);

        // Create the Trip, which fails.
        TripDTO tripDTO = tripMapper.toDto(trip);

        restTripMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(tripDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkStatusIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        trip.setStatus(null);

        // Create the Trip, which fails.
        TripDTO tripDTO = tripMapper.toDto(trip);

        restTripMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(tripDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkDepartAtIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        trip.setDepartAt(null);

        // Create the Trip, which fails.
        TripDTO tripDTO = tripMapper.toDto(trip);

        restTripMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(tripDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkForceClosedIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        trip.setForceClosed(null);

        // Create the Trip, which fails.
        TripDTO tripDTO = tripMapper.toDto(trip);

        restTripMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(tripDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllTrips() throws Exception {
        // Initialize the database
        insertedTrip = tripRepository.saveAndFlush(trip);

        // Get all the tripList
        restTripMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(trip.getId().intValue())))
            .andExpect(jsonPath("$.[*].tripCode").value(hasItem(DEFAULT_TRIP_CODE)))
            .andExpect(jsonPath("$.[*].status").value(hasItem(DEFAULT_STATUS.toString())))
            .andExpect(jsonPath("$.[*].departAt").value(hasItem(DEFAULT_DEPART_AT.toString())))
            .andExpect(jsonPath("$.[*].loadedCount").value(hasItem(DEFAULT_LOADED_COUNT)))
            .andExpect(jsonPath("$.[*].scannedCount").value(hasItem(DEFAULT_SCANNED_COUNT)))
            .andExpect(jsonPath("$.[*].closedAt").value(hasItem(DEFAULT_CLOSED_AT.toString())))
            .andExpect(jsonPath("$.[*].forceClosed").value(hasItem(DEFAULT_FORCE_CLOSED)))
            .andExpect(jsonPath("$.[*].forceCloseReason").value(hasItem(DEFAULT_FORCE_CLOSE_REASON)));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllTripsWithEagerRelationshipsIsEnabled() throws Exception {
        when(tripServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restTripMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(tripServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllTripsWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(tripServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restTripMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(tripRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getTrip() throws Exception {
        // Initialize the database
        insertedTrip = tripRepository.saveAndFlush(trip);

        // Get the trip
        restTripMockMvc
            .perform(get(ENTITY_API_URL_ID, trip.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(trip.getId().intValue()))
            .andExpect(jsonPath("$.tripCode").value(DEFAULT_TRIP_CODE))
            .andExpect(jsonPath("$.status").value(DEFAULT_STATUS.toString()))
            .andExpect(jsonPath("$.departAt").value(DEFAULT_DEPART_AT.toString()))
            .andExpect(jsonPath("$.loadedCount").value(DEFAULT_LOADED_COUNT))
            .andExpect(jsonPath("$.scannedCount").value(DEFAULT_SCANNED_COUNT))
            .andExpect(jsonPath("$.closedAt").value(DEFAULT_CLOSED_AT.toString()))
            .andExpect(jsonPath("$.forceClosed").value(DEFAULT_FORCE_CLOSED))
            .andExpect(jsonPath("$.forceCloseReason").value(DEFAULT_FORCE_CLOSE_REASON));
    }

    @Test
    @Transactional
    void getTripsByIdFiltering() throws Exception {
        // Initialize the database
        insertedTrip = tripRepository.saveAndFlush(trip);

        Long id = trip.getId();

        defaultTripFiltering("id.equals=" + id, "id.notEquals=" + id);

        defaultTripFiltering("id.greaterThanOrEqual=" + id, "id.greaterThan=" + id);

        defaultTripFiltering("id.lessThanOrEqual=" + id, "id.lessThan=" + id);
    }

    @Test
    @Transactional
    void getAllTripsByTripCodeIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedTrip = tripRepository.saveAndFlush(trip);

        // Get all the tripList where tripCode equals to
        defaultTripFiltering("tripCode.equals=" + DEFAULT_TRIP_CODE, "tripCode.equals=" + UPDATED_TRIP_CODE);
    }

    @Test
    @Transactional
    void getAllTripsByTripCodeIsInShouldWork() throws Exception {
        // Initialize the database
        insertedTrip = tripRepository.saveAndFlush(trip);

        // Get all the tripList where tripCode in
        defaultTripFiltering("tripCode.in=" + DEFAULT_TRIP_CODE + "," + UPDATED_TRIP_CODE, "tripCode.in=" + UPDATED_TRIP_CODE);
    }

    @Test
    @Transactional
    void getAllTripsByTripCodeIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedTrip = tripRepository.saveAndFlush(trip);

        // Get all the tripList where tripCode is not null
        defaultTripFiltering("tripCode.specified=true", "tripCode.specified=false");
    }

    @Test
    @Transactional
    void getAllTripsByTripCodeContainsSomething() throws Exception {
        // Initialize the database
        insertedTrip = tripRepository.saveAndFlush(trip);

        // Get all the tripList where tripCode contains
        defaultTripFiltering("tripCode.contains=" + DEFAULT_TRIP_CODE, "tripCode.contains=" + UPDATED_TRIP_CODE);
    }

    @Test
    @Transactional
    void getAllTripsByTripCodeNotContainsSomething() throws Exception {
        // Initialize the database
        insertedTrip = tripRepository.saveAndFlush(trip);

        // Get all the tripList where tripCode does not contain
        defaultTripFiltering("tripCode.doesNotContain=" + UPDATED_TRIP_CODE, "tripCode.doesNotContain=" + DEFAULT_TRIP_CODE);
    }

    @Test
    @Transactional
    void getAllTripsByStatusIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedTrip = tripRepository.saveAndFlush(trip);

        // Get all the tripList where status equals to
        defaultTripFiltering("status.equals=" + DEFAULT_STATUS, "status.equals=" + UPDATED_STATUS);
    }

    @Test
    @Transactional
    void getAllTripsByStatusIsInShouldWork() throws Exception {
        // Initialize the database
        insertedTrip = tripRepository.saveAndFlush(trip);

        // Get all the tripList where status in
        defaultTripFiltering("status.in=" + DEFAULT_STATUS + "," + UPDATED_STATUS, "status.in=" + UPDATED_STATUS);
    }

    @Test
    @Transactional
    void getAllTripsByStatusIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedTrip = tripRepository.saveAndFlush(trip);

        // Get all the tripList where status is not null
        defaultTripFiltering("status.specified=true", "status.specified=false");
    }

    @Test
    @Transactional
    void getAllTripsByDepartAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedTrip = tripRepository.saveAndFlush(trip);

        // Get all the tripList where departAt equals to
        defaultTripFiltering("departAt.equals=" + DEFAULT_DEPART_AT, "departAt.equals=" + UPDATED_DEPART_AT);
    }

    @Test
    @Transactional
    void getAllTripsByDepartAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedTrip = tripRepository.saveAndFlush(trip);

        // Get all the tripList where departAt in
        defaultTripFiltering("departAt.in=" + DEFAULT_DEPART_AT + "," + UPDATED_DEPART_AT, "departAt.in=" + UPDATED_DEPART_AT);
    }

    @Test
    @Transactional
    void getAllTripsByDepartAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedTrip = tripRepository.saveAndFlush(trip);

        // Get all the tripList where departAt is not null
        defaultTripFiltering("departAt.specified=true", "departAt.specified=false");
    }

    @Test
    @Transactional
    void getAllTripsByLoadedCountIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedTrip = tripRepository.saveAndFlush(trip);

        // Get all the tripList where loadedCount equals to
        defaultTripFiltering("loadedCount.equals=" + DEFAULT_LOADED_COUNT, "loadedCount.equals=" + UPDATED_LOADED_COUNT);
    }

    @Test
    @Transactional
    void getAllTripsByLoadedCountIsInShouldWork() throws Exception {
        // Initialize the database
        insertedTrip = tripRepository.saveAndFlush(trip);

        // Get all the tripList where loadedCount in
        defaultTripFiltering(
            "loadedCount.in=" + DEFAULT_LOADED_COUNT + "," + UPDATED_LOADED_COUNT,
            "loadedCount.in=" + UPDATED_LOADED_COUNT
        );
    }

    @Test
    @Transactional
    void getAllTripsByLoadedCountIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedTrip = tripRepository.saveAndFlush(trip);

        // Get all the tripList where loadedCount is not null
        defaultTripFiltering("loadedCount.specified=true", "loadedCount.specified=false");
    }

    @Test
    @Transactional
    void getAllTripsByLoadedCountIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedTrip = tripRepository.saveAndFlush(trip);

        // Get all the tripList where loadedCount is greater than or equal to
        defaultTripFiltering(
            "loadedCount.greaterThanOrEqual=" + DEFAULT_LOADED_COUNT,
            "loadedCount.greaterThanOrEqual=" + UPDATED_LOADED_COUNT
        );
    }

    @Test
    @Transactional
    void getAllTripsByLoadedCountIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedTrip = tripRepository.saveAndFlush(trip);

        // Get all the tripList where loadedCount is less than or equal to
        defaultTripFiltering("loadedCount.lessThanOrEqual=" + DEFAULT_LOADED_COUNT, "loadedCount.lessThanOrEqual=" + SMALLER_LOADED_COUNT);
    }

    @Test
    @Transactional
    void getAllTripsByLoadedCountIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedTrip = tripRepository.saveAndFlush(trip);

        // Get all the tripList where loadedCount is less than
        defaultTripFiltering("loadedCount.lessThan=" + UPDATED_LOADED_COUNT, "loadedCount.lessThan=" + DEFAULT_LOADED_COUNT);
    }

    @Test
    @Transactional
    void getAllTripsByLoadedCountIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedTrip = tripRepository.saveAndFlush(trip);

        // Get all the tripList where loadedCount is greater than
        defaultTripFiltering("loadedCount.greaterThan=" + SMALLER_LOADED_COUNT, "loadedCount.greaterThan=" + DEFAULT_LOADED_COUNT);
    }

    @Test
    @Transactional
    void getAllTripsByScannedCountIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedTrip = tripRepository.saveAndFlush(trip);

        // Get all the tripList where scannedCount equals to
        defaultTripFiltering("scannedCount.equals=" + DEFAULT_SCANNED_COUNT, "scannedCount.equals=" + UPDATED_SCANNED_COUNT);
    }

    @Test
    @Transactional
    void getAllTripsByScannedCountIsInShouldWork() throws Exception {
        // Initialize the database
        insertedTrip = tripRepository.saveAndFlush(trip);

        // Get all the tripList where scannedCount in
        defaultTripFiltering(
            "scannedCount.in=" + DEFAULT_SCANNED_COUNT + "," + UPDATED_SCANNED_COUNT,
            "scannedCount.in=" + UPDATED_SCANNED_COUNT
        );
    }

    @Test
    @Transactional
    void getAllTripsByScannedCountIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedTrip = tripRepository.saveAndFlush(trip);

        // Get all the tripList where scannedCount is not null
        defaultTripFiltering("scannedCount.specified=true", "scannedCount.specified=false");
    }

    @Test
    @Transactional
    void getAllTripsByScannedCountIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedTrip = tripRepository.saveAndFlush(trip);

        // Get all the tripList where scannedCount is greater than or equal to
        defaultTripFiltering(
            "scannedCount.greaterThanOrEqual=" + DEFAULT_SCANNED_COUNT,
            "scannedCount.greaterThanOrEqual=" + UPDATED_SCANNED_COUNT
        );
    }

    @Test
    @Transactional
    void getAllTripsByScannedCountIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedTrip = tripRepository.saveAndFlush(trip);

        // Get all the tripList where scannedCount is less than or equal to
        defaultTripFiltering(
            "scannedCount.lessThanOrEqual=" + DEFAULT_SCANNED_COUNT,
            "scannedCount.lessThanOrEqual=" + SMALLER_SCANNED_COUNT
        );
    }

    @Test
    @Transactional
    void getAllTripsByScannedCountIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedTrip = tripRepository.saveAndFlush(trip);

        // Get all the tripList where scannedCount is less than
        defaultTripFiltering("scannedCount.lessThan=" + UPDATED_SCANNED_COUNT, "scannedCount.lessThan=" + DEFAULT_SCANNED_COUNT);
    }

    @Test
    @Transactional
    void getAllTripsByScannedCountIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedTrip = tripRepository.saveAndFlush(trip);

        // Get all the tripList where scannedCount is greater than
        defaultTripFiltering("scannedCount.greaterThan=" + SMALLER_SCANNED_COUNT, "scannedCount.greaterThan=" + DEFAULT_SCANNED_COUNT);
    }

    @Test
    @Transactional
    void getAllTripsByClosedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedTrip = tripRepository.saveAndFlush(trip);

        // Get all the tripList where closedAt equals to
        defaultTripFiltering("closedAt.equals=" + DEFAULT_CLOSED_AT, "closedAt.equals=" + UPDATED_CLOSED_AT);
    }

    @Test
    @Transactional
    void getAllTripsByClosedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedTrip = tripRepository.saveAndFlush(trip);

        // Get all the tripList where closedAt in
        defaultTripFiltering("closedAt.in=" + DEFAULT_CLOSED_AT + "," + UPDATED_CLOSED_AT, "closedAt.in=" + UPDATED_CLOSED_AT);
    }

    @Test
    @Transactional
    void getAllTripsByClosedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedTrip = tripRepository.saveAndFlush(trip);

        // Get all the tripList where closedAt is not null
        defaultTripFiltering("closedAt.specified=true", "closedAt.specified=false");
    }

    @Test
    @Transactional
    void getAllTripsByForceClosedIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedTrip = tripRepository.saveAndFlush(trip);

        // Get all the tripList where forceClosed equals to
        defaultTripFiltering("forceClosed.equals=" + DEFAULT_FORCE_CLOSED, "forceClosed.equals=" + UPDATED_FORCE_CLOSED);
    }

    @Test
    @Transactional
    void getAllTripsByForceClosedIsInShouldWork() throws Exception {
        // Initialize the database
        insertedTrip = tripRepository.saveAndFlush(trip);

        // Get all the tripList where forceClosed in
        defaultTripFiltering(
            "forceClosed.in=" + DEFAULT_FORCE_CLOSED + "," + UPDATED_FORCE_CLOSED,
            "forceClosed.in=" + UPDATED_FORCE_CLOSED
        );
    }

    @Test
    @Transactional
    void getAllTripsByForceClosedIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedTrip = tripRepository.saveAndFlush(trip);

        // Get all the tripList where forceClosed is not null
        defaultTripFiltering("forceClosed.specified=true", "forceClosed.specified=false");
    }

    @Test
    @Transactional
    void getAllTripsByForceCloseReasonIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedTrip = tripRepository.saveAndFlush(trip);

        // Get all the tripList where forceCloseReason equals to
        defaultTripFiltering(
            "forceCloseReason.equals=" + DEFAULT_FORCE_CLOSE_REASON,
            "forceCloseReason.equals=" + UPDATED_FORCE_CLOSE_REASON
        );
    }

    @Test
    @Transactional
    void getAllTripsByForceCloseReasonIsInShouldWork() throws Exception {
        // Initialize the database
        insertedTrip = tripRepository.saveAndFlush(trip);

        // Get all the tripList where forceCloseReason in
        defaultTripFiltering(
            "forceCloseReason.in=" + DEFAULT_FORCE_CLOSE_REASON + "," + UPDATED_FORCE_CLOSE_REASON,
            "forceCloseReason.in=" + UPDATED_FORCE_CLOSE_REASON
        );
    }

    @Test
    @Transactional
    void getAllTripsByForceCloseReasonIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedTrip = tripRepository.saveAndFlush(trip);

        // Get all the tripList where forceCloseReason is not null
        defaultTripFiltering("forceCloseReason.specified=true", "forceCloseReason.specified=false");
    }

    @Test
    @Transactional
    void getAllTripsByForceCloseReasonContainsSomething() throws Exception {
        // Initialize the database
        insertedTrip = tripRepository.saveAndFlush(trip);

        // Get all the tripList where forceCloseReason contains
        defaultTripFiltering(
            "forceCloseReason.contains=" + DEFAULT_FORCE_CLOSE_REASON,
            "forceCloseReason.contains=" + UPDATED_FORCE_CLOSE_REASON
        );
    }

    @Test
    @Transactional
    void getAllTripsByForceCloseReasonNotContainsSomething() throws Exception {
        // Initialize the database
        insertedTrip = tripRepository.saveAndFlush(trip);

        // Get all the tripList where forceCloseReason does not contain
        defaultTripFiltering(
            "forceCloseReason.doesNotContain=" + UPDATED_FORCE_CLOSE_REASON,
            "forceCloseReason.doesNotContain=" + DEFAULT_FORCE_CLOSE_REASON
        );
    }

    @Test
    @Transactional
    void getAllTripsByOfficeIsEqualToSomething() throws Exception {
        Office office;
        if (TestUtil.findAll(em, Office.class).isEmpty()) {
            tripRepository.saveAndFlush(trip);
            office = OfficeResourceIT.createEntity();
        } else {
            office = TestUtil.findAll(em, Office.class).get(0);
        }
        em.persist(office);
        em.flush();
        trip.setOffice(office);
        tripRepository.saveAndFlush(trip);
        Long officeId = office.getId();
        // Get all the tripList where office equals to officeId
        defaultTripShouldBeFound("officeId.equals=" + officeId);

        // Get all the tripList where office equals to (officeId + 1)
        defaultTripShouldNotBeFound("officeId.equals=" + (officeId + 1));
    }

    @Test
    @Transactional
    void getAllTripsByRouteIsEqualToSomething() throws Exception {
        Route route;
        if (TestUtil.findAll(em, Route.class).isEmpty()) {
            tripRepository.saveAndFlush(trip);
            route = RouteResourceIT.createEntity(em);
        } else {
            route = TestUtil.findAll(em, Route.class).get(0);
        }
        em.persist(route);
        em.flush();
        trip.setRoute(route);
        tripRepository.saveAndFlush(trip);
        Long routeId = route.getId();
        // Get all the tripList where route equals to routeId
        defaultTripShouldBeFound("routeId.equals=" + routeId);

        // Get all the tripList where route equals to (routeId + 1)
        defaultTripShouldNotBeFound("routeId.equals=" + (routeId + 1));
    }

    @Test
    @Transactional
    void getAllTripsByVehicleIsEqualToSomething() throws Exception {
        Vehicle vehicle;
        if (TestUtil.findAll(em, Vehicle.class).isEmpty()) {
            tripRepository.saveAndFlush(trip);
            vehicle = VehicleResourceIT.createEntity();
        } else {
            vehicle = TestUtil.findAll(em, Vehicle.class).get(0);
        }
        em.persist(vehicle);
        em.flush();
        trip.setVehicle(vehicle);
        tripRepository.saveAndFlush(trip);
        Long vehicleId = vehicle.getId();
        // Get all the tripList where vehicle equals to vehicleId
        defaultTripShouldBeFound("vehicleId.equals=" + vehicleId);

        // Get all the tripList where vehicle equals to (vehicleId + 1)
        defaultTripShouldNotBeFound("vehicleId.equals=" + (vehicleId + 1));
    }

    @Test
    @Transactional
    void getAllTripsByDriverIsEqualToSomething() throws Exception {
        Driver driver;
        if (TestUtil.findAll(em, Driver.class).isEmpty()) {
            tripRepository.saveAndFlush(trip);
            driver = DriverResourceIT.createEntity();
        } else {
            driver = TestUtil.findAll(em, Driver.class).get(0);
        }
        em.persist(driver);
        em.flush();
        trip.setDriver(driver);
        tripRepository.saveAndFlush(trip);
        Long driverId = driver.getId();
        // Get all the tripList where driver equals to driverId
        defaultTripShouldBeFound("driverId.equals=" + driverId);

        // Get all the tripList where driver equals to (driverId + 1)
        defaultTripShouldNotBeFound("driverId.equals=" + (driverId + 1));
    }

    private void defaultTripFiltering(String shouldBeFound, String shouldNotBeFound) throws Exception {
        defaultTripShouldBeFound(shouldBeFound);
        defaultTripShouldNotBeFound(shouldNotBeFound);
    }

    /**
     * Executes the search, and checks that the default entity is returned.
     */
    private void defaultTripShouldBeFound(String filter) throws Exception {
        restTripMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(trip.getId().intValue())))
            .andExpect(jsonPath("$.[*].tripCode").value(hasItem(DEFAULT_TRIP_CODE)))
            .andExpect(jsonPath("$.[*].status").value(hasItem(DEFAULT_STATUS.toString())))
            .andExpect(jsonPath("$.[*].departAt").value(hasItem(DEFAULT_DEPART_AT.toString())))
            .andExpect(jsonPath("$.[*].loadedCount").value(hasItem(DEFAULT_LOADED_COUNT)))
            .andExpect(jsonPath("$.[*].scannedCount").value(hasItem(DEFAULT_SCANNED_COUNT)))
            .andExpect(jsonPath("$.[*].closedAt").value(hasItem(DEFAULT_CLOSED_AT.toString())))
            .andExpect(jsonPath("$.[*].forceClosed").value(hasItem(DEFAULT_FORCE_CLOSED)))
            .andExpect(jsonPath("$.[*].forceCloseReason").value(hasItem(DEFAULT_FORCE_CLOSE_REASON)));

        // Check, that the count call also returns 1
        restTripMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("1"));
    }

    /**
     * Executes the search, and checks that the default entity is not returned.
     */
    private void defaultTripShouldNotBeFound(String filter) throws Exception {
        restTripMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());

        // Check, that the count call also returns 0
        restTripMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("0"));
    }

    @Test
    @Transactional
    void getNonExistingTrip() throws Exception {
        // Get the trip
        restTripMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingTrip() throws Exception {
        // Initialize the database
        insertedTrip = tripRepository.saveAndFlush(trip);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the trip
        Trip updatedTrip = tripRepository.findById(trip.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedTrip are not directly saved in db
        em.detach(updatedTrip);
        updatedTrip
            .tripCode(UPDATED_TRIP_CODE)
            .status(UPDATED_STATUS)
            .departAt(UPDATED_DEPART_AT)
            .loadedCount(UPDATED_LOADED_COUNT)
            .scannedCount(UPDATED_SCANNED_COUNT)
            .closedAt(UPDATED_CLOSED_AT)
            .forceClosed(UPDATED_FORCE_CLOSED)
            .forceCloseReason(UPDATED_FORCE_CLOSE_REASON);
        TripDTO tripDTO = tripMapper.toDto(updatedTrip);

        restTripMockMvc
            .perform(put(ENTITY_API_URL_ID, tripDTO.getId()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(tripDTO)))
            .andExpect(status().isOk());

        // Validate the Trip in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedTripToMatchAllProperties(updatedTrip);
    }

    @Test
    @Transactional
    void putNonExistingTrip() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        trip.setId(longCount.incrementAndGet());

        // Create the Trip
        TripDTO tripDTO = tripMapper.toDto(trip);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restTripMockMvc
            .perform(put(ENTITY_API_URL_ID, tripDTO.getId()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(tripDTO)))
            .andExpect(status().isBadRequest());

        // Validate the Trip in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchTrip() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        trip.setId(longCount.incrementAndGet());

        // Create the Trip
        TripDTO tripDTO = tripMapper.toDto(trip);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restTripMockMvc
            .perform(
                put(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(tripDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Trip in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamTrip() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        trip.setId(longCount.incrementAndGet());

        // Create the Trip
        TripDTO tripDTO = tripMapper.toDto(trip);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restTripMockMvc
            .perform(put(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(tripDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Trip in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateTripWithPatch() throws Exception {
        // Initialize the database
        insertedTrip = tripRepository.saveAndFlush(trip);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the trip using partial update
        Trip partialUpdatedTrip = new Trip();
        partialUpdatedTrip.setId(trip.getId());

        partialUpdatedTrip.tripCode(UPDATED_TRIP_CODE).status(UPDATED_STATUS).closedAt(UPDATED_CLOSED_AT).forceClosed(UPDATED_FORCE_CLOSED);

        restTripMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedTrip.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedTrip))
            )
            .andExpect(status().isOk());

        // Validate the Trip in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertTripUpdatableFieldsEquals(createUpdateProxyForBean(partialUpdatedTrip, trip), getPersistedTrip(trip));
    }

    @Test
    @Transactional
    void fullUpdateTripWithPatch() throws Exception {
        // Initialize the database
        insertedTrip = tripRepository.saveAndFlush(trip);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the trip using partial update
        Trip partialUpdatedTrip = new Trip();
        partialUpdatedTrip.setId(trip.getId());

        partialUpdatedTrip
            .tripCode(UPDATED_TRIP_CODE)
            .status(UPDATED_STATUS)
            .departAt(UPDATED_DEPART_AT)
            .loadedCount(UPDATED_LOADED_COUNT)
            .scannedCount(UPDATED_SCANNED_COUNT)
            .closedAt(UPDATED_CLOSED_AT)
            .forceClosed(UPDATED_FORCE_CLOSED)
            .forceCloseReason(UPDATED_FORCE_CLOSE_REASON);

        restTripMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedTrip.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedTrip))
            )
            .andExpect(status().isOk());

        // Validate the Trip in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertTripUpdatableFieldsEquals(partialUpdatedTrip, getPersistedTrip(partialUpdatedTrip));
    }

    @Test
    @Transactional
    void patchNonExistingTrip() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        trip.setId(longCount.incrementAndGet());

        // Create the Trip
        TripDTO tripDTO = tripMapper.toDto(trip);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restTripMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, tripDTO.getId()).contentType("application/merge-patch+json").content(om.writeValueAsBytes(tripDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Trip in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchTrip() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        trip.setId(longCount.incrementAndGet());

        // Create the Trip
        TripDTO tripDTO = tripMapper.toDto(trip);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restTripMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(tripDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Trip in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamTrip() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        trip.setId(longCount.incrementAndGet());

        // Create the Trip
        TripDTO tripDTO = tripMapper.toDto(trip);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restTripMockMvc
            .perform(patch(ENTITY_API_URL).contentType("application/merge-patch+json").content(om.writeValueAsBytes(tripDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Trip in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteTrip() throws Exception {
        // Initialize the database
        insertedTrip = tripRepository.saveAndFlush(trip);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the trip
        restTripMockMvc
            .perform(delete(ENTITY_API_URL_ID, trip.getId()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return tripRepository.count();
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

    protected Trip getPersistedTrip(Trip trip) {
        return tripRepository.findById(trip.getId()).orElseThrow();
    }

    protected void assertPersistedTripToMatchAllProperties(Trip expectedTrip) {
        assertTripAllPropertiesEquals(expectedTrip, getPersistedTrip(expectedTrip));
    }

    protected void assertPersistedTripToMatchUpdatableProperties(Trip expectedTrip) {
        assertTripAllUpdatablePropertiesEquals(expectedTrip, getPersistedTrip(expectedTrip));
    }
}
