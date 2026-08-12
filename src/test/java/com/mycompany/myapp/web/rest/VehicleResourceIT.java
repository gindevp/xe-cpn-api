package com.mycompany.myapp.web.rest;

import static com.mycompany.myapp.domain.VehicleAsserts.*;
import static com.mycompany.myapp.web.rest.TestUtil.createUpdateProxyForBean;
import static com.mycompany.myapp.web.rest.TestUtil.sameNumber;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.myapp.IntegrationTest;
import com.mycompany.myapp.domain.Vehicle;
import com.mycompany.myapp.repository.VehicleRepository;
import com.mycompany.myapp.service.dto.VehicleDTO;
import com.mycompany.myapp.service.mapper.VehicleMapper;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
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
 * Integration tests for the {@link VehicleResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser
class VehicleResourceIT {

    private static final String DEFAULT_PLATE_NUMBER = "AAAAAAAAAA";
    private static final String UPDATED_PLATE_NUMBER = "BBBBBBBBBB";

    private static final BigDecimal DEFAULT_CAPACITY_KG = new BigDecimal(0);
    private static final BigDecimal UPDATED_CAPACITY_KG = new BigDecimal(1);

    private static final Boolean DEFAULT_ACTIVE = false;
    private static final Boolean UPDATED_ACTIVE = true;

    private static final String ENTITY_API_URL = "/api/vehicles";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static Random random = new Random();
    private static AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    @Autowired
    private ObjectMapper om;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private VehicleMapper vehicleMapper;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restVehicleMockMvc;

    private Vehicle vehicle;

    private Vehicle insertedVehicle;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Vehicle createEntity() {
        return new Vehicle().plateNumber(DEFAULT_PLATE_NUMBER).capacityKg(DEFAULT_CAPACITY_KG).active(DEFAULT_ACTIVE);
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Vehicle createUpdatedEntity() {
        return new Vehicle().plateNumber(UPDATED_PLATE_NUMBER).capacityKg(UPDATED_CAPACITY_KG).active(UPDATED_ACTIVE);
    }

    @BeforeEach
    public void initTest() {
        vehicle = createEntity();
    }

    @AfterEach
    public void cleanup() {
        if (insertedVehicle != null) {
            vehicleRepository.delete(insertedVehicle);
            insertedVehicle = null;
        }
    }

    @Test
    @Transactional
    void createVehicle() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the Vehicle
        VehicleDTO vehicleDTO = vehicleMapper.toDto(vehicle);
        var returnedVehicleDTO = om.readValue(
            restVehicleMockMvc
                .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(vehicleDTO)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            VehicleDTO.class
        );

        // Validate the Vehicle in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedVehicle = vehicleMapper.toEntity(returnedVehicleDTO);
        assertVehicleUpdatableFieldsEquals(returnedVehicle, getPersistedVehicle(returnedVehicle));

        insertedVehicle = returnedVehicle;
    }

    @Test
    @Transactional
    void createVehicleWithExistingId() throws Exception {
        // Create the Vehicle with an existing ID
        vehicle.setId(1L);
        VehicleDTO vehicleDTO = vehicleMapper.toDto(vehicle);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restVehicleMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(vehicleDTO)))
            .andExpect(status().isBadRequest());

        // Validate the Vehicle in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkPlateNumberIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        vehicle.setPlateNumber(null);

        // Create the Vehicle, which fails.
        VehicleDTO vehicleDTO = vehicleMapper.toDto(vehicle);

        restVehicleMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(vehicleDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkCapacityKgIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        vehicle.setCapacityKg(null);

        // Create the Vehicle, which fails.
        VehicleDTO vehicleDTO = vehicleMapper.toDto(vehicle);

        restVehicleMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(vehicleDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkActiveIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        vehicle.setActive(null);

        // Create the Vehicle, which fails.
        VehicleDTO vehicleDTO = vehicleMapper.toDto(vehicle);

        restVehicleMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(vehicleDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllVehicles() throws Exception {
        // Initialize the database
        insertedVehicle = vehicleRepository.saveAndFlush(vehicle);

        // Get all the vehicleList
        restVehicleMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(vehicle.getId().intValue())))
            .andExpect(jsonPath("$.[*].plateNumber").value(hasItem(DEFAULT_PLATE_NUMBER)))
            .andExpect(jsonPath("$.[*].capacityKg").value(hasItem(sameNumber(DEFAULT_CAPACITY_KG))))
            .andExpect(jsonPath("$.[*].active").value(hasItem(DEFAULT_ACTIVE)));
    }

    @Test
    @Transactional
    void getVehicle() throws Exception {
        // Initialize the database
        insertedVehicle = vehicleRepository.saveAndFlush(vehicle);

        // Get the vehicle
        restVehicleMockMvc
            .perform(get(ENTITY_API_URL_ID, vehicle.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(vehicle.getId().intValue()))
            .andExpect(jsonPath("$.plateNumber").value(DEFAULT_PLATE_NUMBER))
            .andExpect(jsonPath("$.capacityKg").value(sameNumber(DEFAULT_CAPACITY_KG)))
            .andExpect(jsonPath("$.active").value(DEFAULT_ACTIVE));
    }

    @Test
    @Transactional
    void getNonExistingVehicle() throws Exception {
        // Get the vehicle
        restVehicleMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingVehicle() throws Exception {
        // Initialize the database
        insertedVehicle = vehicleRepository.saveAndFlush(vehicle);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the vehicle
        Vehicle updatedVehicle = vehicleRepository.findById(vehicle.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedVehicle are not directly saved in db
        em.detach(updatedVehicle);
        updatedVehicle.plateNumber(UPDATED_PLATE_NUMBER).capacityKg(UPDATED_CAPACITY_KG).active(UPDATED_ACTIVE);
        VehicleDTO vehicleDTO = vehicleMapper.toDto(updatedVehicle);

        restVehicleMockMvc
            .perform(
                put(ENTITY_API_URL_ID, vehicleDTO.getId()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(vehicleDTO))
            )
            .andExpect(status().isOk());

        // Validate the Vehicle in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedVehicleToMatchAllProperties(updatedVehicle);
    }

    @Test
    @Transactional
    void putNonExistingVehicle() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        vehicle.setId(longCount.incrementAndGet());

        // Create the Vehicle
        VehicleDTO vehicleDTO = vehicleMapper.toDto(vehicle);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restVehicleMockMvc
            .perform(
                put(ENTITY_API_URL_ID, vehicleDTO.getId()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(vehicleDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Vehicle in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchVehicle() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        vehicle.setId(longCount.incrementAndGet());

        // Create the Vehicle
        VehicleDTO vehicleDTO = vehicleMapper.toDto(vehicle);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restVehicleMockMvc
            .perform(
                put(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(vehicleDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Vehicle in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamVehicle() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        vehicle.setId(longCount.incrementAndGet());

        // Create the Vehicle
        VehicleDTO vehicleDTO = vehicleMapper.toDto(vehicle);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restVehicleMockMvc
            .perform(put(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(vehicleDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Vehicle in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateVehicleWithPatch() throws Exception {
        // Initialize the database
        insertedVehicle = vehicleRepository.saveAndFlush(vehicle);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the vehicle using partial update
        Vehicle partialUpdatedVehicle = new Vehicle();
        partialUpdatedVehicle.setId(vehicle.getId());

        partialUpdatedVehicle.plateNumber(UPDATED_PLATE_NUMBER).active(UPDATED_ACTIVE);

        restVehicleMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedVehicle.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedVehicle))
            )
            .andExpect(status().isOk());

        // Validate the Vehicle in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertVehicleUpdatableFieldsEquals(createUpdateProxyForBean(partialUpdatedVehicle, vehicle), getPersistedVehicle(vehicle));
    }

    @Test
    @Transactional
    void fullUpdateVehicleWithPatch() throws Exception {
        // Initialize the database
        insertedVehicle = vehicleRepository.saveAndFlush(vehicle);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the vehicle using partial update
        Vehicle partialUpdatedVehicle = new Vehicle();
        partialUpdatedVehicle.setId(vehicle.getId());

        partialUpdatedVehicle.plateNumber(UPDATED_PLATE_NUMBER).capacityKg(UPDATED_CAPACITY_KG).active(UPDATED_ACTIVE);

        restVehicleMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedVehicle.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedVehicle))
            )
            .andExpect(status().isOk());

        // Validate the Vehicle in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertVehicleUpdatableFieldsEquals(partialUpdatedVehicle, getPersistedVehicle(partialUpdatedVehicle));
    }

    @Test
    @Transactional
    void patchNonExistingVehicle() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        vehicle.setId(longCount.incrementAndGet());

        // Create the Vehicle
        VehicleDTO vehicleDTO = vehicleMapper.toDto(vehicle);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restVehicleMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, vehicleDTO.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(vehicleDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Vehicle in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchVehicle() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        vehicle.setId(longCount.incrementAndGet());

        // Create the Vehicle
        VehicleDTO vehicleDTO = vehicleMapper.toDto(vehicle);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restVehicleMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(vehicleDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Vehicle in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamVehicle() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        vehicle.setId(longCount.incrementAndGet());

        // Create the Vehicle
        VehicleDTO vehicleDTO = vehicleMapper.toDto(vehicle);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restVehicleMockMvc
            .perform(patch(ENTITY_API_URL).contentType("application/merge-patch+json").content(om.writeValueAsBytes(vehicleDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Vehicle in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteVehicle() throws Exception {
        // Initialize the database
        insertedVehicle = vehicleRepository.saveAndFlush(vehicle);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the vehicle
        restVehicleMockMvc
            .perform(delete(ENTITY_API_URL_ID, vehicle.getId()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return vehicleRepository.count();
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

    protected Vehicle getPersistedVehicle(Vehicle vehicle) {
        return vehicleRepository.findById(vehicle.getId()).orElseThrow();
    }

    protected void assertPersistedVehicleToMatchAllProperties(Vehicle expectedVehicle) {
        assertVehicleAllPropertiesEquals(expectedVehicle, getPersistedVehicle(expectedVehicle));
    }

    protected void assertPersistedVehicleToMatchUpdatableProperties(Vehicle expectedVehicle) {
        assertVehicleAllUpdatablePropertiesEquals(expectedVehicle, getPersistedVehicle(expectedVehicle));
    }
}
