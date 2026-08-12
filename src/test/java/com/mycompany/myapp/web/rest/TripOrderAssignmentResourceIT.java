package com.mycompany.myapp.web.rest;

import static com.mycompany.myapp.domain.TripOrderAssignmentAsserts.*;
import static com.mycompany.myapp.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.myapp.IntegrationTest;
import com.mycompany.myapp.domain.ShipmentOrder;
import com.mycompany.myapp.domain.Trip;
import com.mycompany.myapp.domain.TripOrderAssignment;
import com.mycompany.myapp.domain.enumeration.AssignmentStatus;
import com.mycompany.myapp.repository.TripOrderAssignmentRepository;
import com.mycompany.myapp.service.TripOrderAssignmentService;
import com.mycompany.myapp.service.dto.TripOrderAssignmentDTO;
import com.mycompany.myapp.service.mapper.TripOrderAssignmentMapper;
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
 * Integration tests for the {@link TripOrderAssignmentResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser
class TripOrderAssignmentResourceIT {

    private static final AssignmentStatus DEFAULT_ASSIGNMENT_STATUS = AssignmentStatus.SCANNED;
    private static final AssignmentStatus UPDATED_ASSIGNMENT_STATUS = AssignmentStatus.LOADED;

    private static final Instant DEFAULT_SCANNED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_SCANNED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final Instant DEFAULT_LOADED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_LOADED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final Instant DEFAULT_REMOVED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_REMOVED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final String DEFAULT_REMARK = "AAAAAAAAAA";
    private static final String UPDATED_REMARK = "BBBBBBBBBB";

    private static final String ENTITY_API_URL = "/api/trip-order-assignments";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static Random random = new Random();
    private static AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    @Autowired
    private ObjectMapper om;

    @Autowired
    private TripOrderAssignmentRepository tripOrderAssignmentRepository;

    @Mock
    private TripOrderAssignmentRepository tripOrderAssignmentRepositoryMock;

    @Autowired
    private TripOrderAssignmentMapper tripOrderAssignmentMapper;

    @Mock
    private TripOrderAssignmentService tripOrderAssignmentServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restTripOrderAssignmentMockMvc;

    private TripOrderAssignment tripOrderAssignment;

    private TripOrderAssignment insertedTripOrderAssignment;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static TripOrderAssignment createEntity(EntityManager em) {
        TripOrderAssignment tripOrderAssignment = new TripOrderAssignment()
            .assignmentStatus(DEFAULT_ASSIGNMENT_STATUS)
            .scannedAt(DEFAULT_SCANNED_AT)
            .loadedAt(DEFAULT_LOADED_AT)
            .removedAt(DEFAULT_REMOVED_AT)
            .remark(DEFAULT_REMARK);
        // Add required entity
        Trip trip;
        if (TestUtil.findAll(em, Trip.class).isEmpty()) {
            trip = TripResourceIT.createEntity(em);
            em.persist(trip);
            em.flush();
        } else {
            trip = TestUtil.findAll(em, Trip.class).get(0);
        }
        tripOrderAssignment.setTrip(trip);
        // Add required entity
        ShipmentOrder shipmentOrder;
        if (TestUtil.findAll(em, ShipmentOrder.class).isEmpty()) {
            shipmentOrder = ShipmentOrderResourceIT.createEntity(em);
            em.persist(shipmentOrder);
            em.flush();
        } else {
            shipmentOrder = TestUtil.findAll(em, ShipmentOrder.class).get(0);
        }
        tripOrderAssignment.setOrder(shipmentOrder);
        return tripOrderAssignment;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static TripOrderAssignment createUpdatedEntity(EntityManager em) {
        TripOrderAssignment updatedTripOrderAssignment = new TripOrderAssignment()
            .assignmentStatus(UPDATED_ASSIGNMENT_STATUS)
            .scannedAt(UPDATED_SCANNED_AT)
            .loadedAt(UPDATED_LOADED_AT)
            .removedAt(UPDATED_REMOVED_AT)
            .remark(UPDATED_REMARK);
        // Add required entity
        Trip trip;
        if (TestUtil.findAll(em, Trip.class).isEmpty()) {
            trip = TripResourceIT.createUpdatedEntity(em);
            em.persist(trip);
            em.flush();
        } else {
            trip = TestUtil.findAll(em, Trip.class).get(0);
        }
        updatedTripOrderAssignment.setTrip(trip);
        // Add required entity
        ShipmentOrder shipmentOrder;
        if (TestUtil.findAll(em, ShipmentOrder.class).isEmpty()) {
            shipmentOrder = ShipmentOrderResourceIT.createUpdatedEntity(em);
            em.persist(shipmentOrder);
            em.flush();
        } else {
            shipmentOrder = TestUtil.findAll(em, ShipmentOrder.class).get(0);
        }
        updatedTripOrderAssignment.setOrder(shipmentOrder);
        return updatedTripOrderAssignment;
    }

    @BeforeEach
    public void initTest() {
        tripOrderAssignment = createEntity(em);
    }

    @AfterEach
    public void cleanup() {
        if (insertedTripOrderAssignment != null) {
            tripOrderAssignmentRepository.delete(insertedTripOrderAssignment);
            insertedTripOrderAssignment = null;
        }
    }

    @Test
    @Transactional
    void createTripOrderAssignment() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the TripOrderAssignment
        TripOrderAssignmentDTO tripOrderAssignmentDTO = tripOrderAssignmentMapper.toDto(tripOrderAssignment);
        var returnedTripOrderAssignmentDTO = om.readValue(
            restTripOrderAssignmentMockMvc
                .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(tripOrderAssignmentDTO)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            TripOrderAssignmentDTO.class
        );

        // Validate the TripOrderAssignment in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedTripOrderAssignment = tripOrderAssignmentMapper.toEntity(returnedTripOrderAssignmentDTO);
        assertTripOrderAssignmentUpdatableFieldsEquals(
            returnedTripOrderAssignment,
            getPersistedTripOrderAssignment(returnedTripOrderAssignment)
        );

        insertedTripOrderAssignment = returnedTripOrderAssignment;
    }

    @Test
    @Transactional
    void createTripOrderAssignmentWithExistingId() throws Exception {
        // Create the TripOrderAssignment with an existing ID
        tripOrderAssignment.setId(1L);
        TripOrderAssignmentDTO tripOrderAssignmentDTO = tripOrderAssignmentMapper.toDto(tripOrderAssignment);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restTripOrderAssignmentMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(tripOrderAssignmentDTO)))
            .andExpect(status().isBadRequest());

        // Validate the TripOrderAssignment in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkAssignmentStatusIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        tripOrderAssignment.setAssignmentStatus(null);

        // Create the TripOrderAssignment, which fails.
        TripOrderAssignmentDTO tripOrderAssignmentDTO = tripOrderAssignmentMapper.toDto(tripOrderAssignment);

        restTripOrderAssignmentMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(tripOrderAssignmentDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllTripOrderAssignments() throws Exception {
        // Initialize the database
        insertedTripOrderAssignment = tripOrderAssignmentRepository.saveAndFlush(tripOrderAssignment);

        // Get all the tripOrderAssignmentList
        restTripOrderAssignmentMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(tripOrderAssignment.getId().intValue())))
            .andExpect(jsonPath("$.[*].assignmentStatus").value(hasItem(DEFAULT_ASSIGNMENT_STATUS.toString())))
            .andExpect(jsonPath("$.[*].scannedAt").value(hasItem(DEFAULT_SCANNED_AT.toString())))
            .andExpect(jsonPath("$.[*].loadedAt").value(hasItem(DEFAULT_LOADED_AT.toString())))
            .andExpect(jsonPath("$.[*].removedAt").value(hasItem(DEFAULT_REMOVED_AT.toString())))
            .andExpect(jsonPath("$.[*].remark").value(hasItem(DEFAULT_REMARK)));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllTripOrderAssignmentsWithEagerRelationshipsIsEnabled() throws Exception {
        when(tripOrderAssignmentServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restTripOrderAssignmentMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(tripOrderAssignmentServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllTripOrderAssignmentsWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(tripOrderAssignmentServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restTripOrderAssignmentMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(tripOrderAssignmentRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getTripOrderAssignment() throws Exception {
        // Initialize the database
        insertedTripOrderAssignment = tripOrderAssignmentRepository.saveAndFlush(tripOrderAssignment);

        // Get the tripOrderAssignment
        restTripOrderAssignmentMockMvc
            .perform(get(ENTITY_API_URL_ID, tripOrderAssignment.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(tripOrderAssignment.getId().intValue()))
            .andExpect(jsonPath("$.assignmentStatus").value(DEFAULT_ASSIGNMENT_STATUS.toString()))
            .andExpect(jsonPath("$.scannedAt").value(DEFAULT_SCANNED_AT.toString()))
            .andExpect(jsonPath("$.loadedAt").value(DEFAULT_LOADED_AT.toString()))
            .andExpect(jsonPath("$.removedAt").value(DEFAULT_REMOVED_AT.toString()))
            .andExpect(jsonPath("$.remark").value(DEFAULT_REMARK));
    }

    @Test
    @Transactional
    void getNonExistingTripOrderAssignment() throws Exception {
        // Get the tripOrderAssignment
        restTripOrderAssignmentMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingTripOrderAssignment() throws Exception {
        // Initialize the database
        insertedTripOrderAssignment = tripOrderAssignmentRepository.saveAndFlush(tripOrderAssignment);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the tripOrderAssignment
        TripOrderAssignment updatedTripOrderAssignment = tripOrderAssignmentRepository.findById(tripOrderAssignment.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedTripOrderAssignment are not directly saved in db
        em.detach(updatedTripOrderAssignment);
        updatedTripOrderAssignment
            .assignmentStatus(UPDATED_ASSIGNMENT_STATUS)
            .scannedAt(UPDATED_SCANNED_AT)
            .loadedAt(UPDATED_LOADED_AT)
            .removedAt(UPDATED_REMOVED_AT)
            .remark(UPDATED_REMARK);
        TripOrderAssignmentDTO tripOrderAssignmentDTO = tripOrderAssignmentMapper.toDto(updatedTripOrderAssignment);

        restTripOrderAssignmentMockMvc
            .perform(
                put(ENTITY_API_URL_ID, tripOrderAssignmentDTO.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(tripOrderAssignmentDTO))
            )
            .andExpect(status().isOk());

        // Validate the TripOrderAssignment in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedTripOrderAssignmentToMatchAllProperties(updatedTripOrderAssignment);
    }

    @Test
    @Transactional
    void putNonExistingTripOrderAssignment() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        tripOrderAssignment.setId(longCount.incrementAndGet());

        // Create the TripOrderAssignment
        TripOrderAssignmentDTO tripOrderAssignmentDTO = tripOrderAssignmentMapper.toDto(tripOrderAssignment);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restTripOrderAssignmentMockMvc
            .perform(
                put(ENTITY_API_URL_ID, tripOrderAssignmentDTO.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(tripOrderAssignmentDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the TripOrderAssignment in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchTripOrderAssignment() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        tripOrderAssignment.setId(longCount.incrementAndGet());

        // Create the TripOrderAssignment
        TripOrderAssignmentDTO tripOrderAssignmentDTO = tripOrderAssignmentMapper.toDto(tripOrderAssignment);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restTripOrderAssignmentMockMvc
            .perform(
                put(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(tripOrderAssignmentDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the TripOrderAssignment in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamTripOrderAssignment() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        tripOrderAssignment.setId(longCount.incrementAndGet());

        // Create the TripOrderAssignment
        TripOrderAssignmentDTO tripOrderAssignmentDTO = tripOrderAssignmentMapper.toDto(tripOrderAssignment);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restTripOrderAssignmentMockMvc
            .perform(put(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(tripOrderAssignmentDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the TripOrderAssignment in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateTripOrderAssignmentWithPatch() throws Exception {
        // Initialize the database
        insertedTripOrderAssignment = tripOrderAssignmentRepository.saveAndFlush(tripOrderAssignment);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the tripOrderAssignment using partial update
        TripOrderAssignment partialUpdatedTripOrderAssignment = new TripOrderAssignment();
        partialUpdatedTripOrderAssignment.setId(tripOrderAssignment.getId());

        partialUpdatedTripOrderAssignment.removedAt(UPDATED_REMOVED_AT);

        restTripOrderAssignmentMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedTripOrderAssignment.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedTripOrderAssignment))
            )
            .andExpect(status().isOk());

        // Validate the TripOrderAssignment in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertTripOrderAssignmentUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedTripOrderAssignment, tripOrderAssignment),
            getPersistedTripOrderAssignment(tripOrderAssignment)
        );
    }

    @Test
    @Transactional
    void fullUpdateTripOrderAssignmentWithPatch() throws Exception {
        // Initialize the database
        insertedTripOrderAssignment = tripOrderAssignmentRepository.saveAndFlush(tripOrderAssignment);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the tripOrderAssignment using partial update
        TripOrderAssignment partialUpdatedTripOrderAssignment = new TripOrderAssignment();
        partialUpdatedTripOrderAssignment.setId(tripOrderAssignment.getId());

        partialUpdatedTripOrderAssignment
            .assignmentStatus(UPDATED_ASSIGNMENT_STATUS)
            .scannedAt(UPDATED_SCANNED_AT)
            .loadedAt(UPDATED_LOADED_AT)
            .removedAt(UPDATED_REMOVED_AT)
            .remark(UPDATED_REMARK);

        restTripOrderAssignmentMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedTripOrderAssignment.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedTripOrderAssignment))
            )
            .andExpect(status().isOk());

        // Validate the TripOrderAssignment in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertTripOrderAssignmentUpdatableFieldsEquals(
            partialUpdatedTripOrderAssignment,
            getPersistedTripOrderAssignment(partialUpdatedTripOrderAssignment)
        );
    }

    @Test
    @Transactional
    void patchNonExistingTripOrderAssignment() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        tripOrderAssignment.setId(longCount.incrementAndGet());

        // Create the TripOrderAssignment
        TripOrderAssignmentDTO tripOrderAssignmentDTO = tripOrderAssignmentMapper.toDto(tripOrderAssignment);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restTripOrderAssignmentMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, tripOrderAssignmentDTO.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(tripOrderAssignmentDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the TripOrderAssignment in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchTripOrderAssignment() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        tripOrderAssignment.setId(longCount.incrementAndGet());

        // Create the TripOrderAssignment
        TripOrderAssignmentDTO tripOrderAssignmentDTO = tripOrderAssignmentMapper.toDto(tripOrderAssignment);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restTripOrderAssignmentMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(tripOrderAssignmentDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the TripOrderAssignment in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamTripOrderAssignment() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        tripOrderAssignment.setId(longCount.incrementAndGet());

        // Create the TripOrderAssignment
        TripOrderAssignmentDTO tripOrderAssignmentDTO = tripOrderAssignmentMapper.toDto(tripOrderAssignment);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restTripOrderAssignmentMockMvc
            .perform(
                patch(ENTITY_API_URL).contentType("application/merge-patch+json").content(om.writeValueAsBytes(tripOrderAssignmentDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the TripOrderAssignment in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteTripOrderAssignment() throws Exception {
        // Initialize the database
        insertedTripOrderAssignment = tripOrderAssignmentRepository.saveAndFlush(tripOrderAssignment);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the tripOrderAssignment
        restTripOrderAssignmentMockMvc
            .perform(delete(ENTITY_API_URL_ID, tripOrderAssignment.getId()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return tripOrderAssignmentRepository.count();
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

    protected TripOrderAssignment getPersistedTripOrderAssignment(TripOrderAssignment tripOrderAssignment) {
        return tripOrderAssignmentRepository.findById(tripOrderAssignment.getId()).orElseThrow();
    }

    protected void assertPersistedTripOrderAssignmentToMatchAllProperties(TripOrderAssignment expectedTripOrderAssignment) {
        assertTripOrderAssignmentAllPropertiesEquals(
            expectedTripOrderAssignment,
            getPersistedTripOrderAssignment(expectedTripOrderAssignment)
        );
    }

    protected void assertPersistedTripOrderAssignmentToMatchUpdatableProperties(TripOrderAssignment expectedTripOrderAssignment) {
        assertTripOrderAssignmentAllUpdatablePropertiesEquals(
            expectedTripOrderAssignment,
            getPersistedTripOrderAssignment(expectedTripOrderAssignment)
        );
    }
}
