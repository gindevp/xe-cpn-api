package com.mycompany.myapp.web.rest;

import static com.mycompany.myapp.domain.OrderLegAsserts.*;
import static com.mycompany.myapp.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.myapp.IntegrationTest;
import com.mycompany.myapp.domain.Office;
import com.mycompany.myapp.domain.OrderLeg;
import com.mycompany.myapp.domain.ShipmentOrder;
import com.mycompany.myapp.domain.enumeration.LegStatus;
import com.mycompany.myapp.repository.OrderLegRepository;
import com.mycompany.myapp.service.OrderLegService;
import com.mycompany.myapp.service.dto.OrderLegDTO;
import com.mycompany.myapp.service.mapper.OrderLegMapper;
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
 * Integration tests for the {@link OrderLegResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser
class OrderLegResourceIT {

    private static final Integer DEFAULT_LEG_INDEX = 0;
    private static final Integer UPDATED_LEG_INDEX = 1;

    private static final LegStatus DEFAULT_STATUS = LegStatus.PENDING;
    private static final LegStatus UPDATED_STATUS = LegStatus.IN_TRANSIT;

    private static final Instant DEFAULT_DEPARTED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_DEPARTED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final Instant DEFAULT_ARRIVED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_ARRIVED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final String ENTITY_API_URL = "/api/order-legs";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static Random random = new Random();
    private static AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    @Autowired
    private ObjectMapper om;

    @Autowired
    private OrderLegRepository orderLegRepository;

    @Mock
    private OrderLegRepository orderLegRepositoryMock;

    @Autowired
    private OrderLegMapper orderLegMapper;

    @Mock
    private OrderLegService orderLegServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restOrderLegMockMvc;

    private OrderLeg orderLeg;

    private OrderLeg insertedOrderLeg;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static OrderLeg createEntity(EntityManager em) {
        OrderLeg orderLeg = new OrderLeg()
            .legIndex(DEFAULT_LEG_INDEX)
            .status(DEFAULT_STATUS)
            .departedAt(DEFAULT_DEPARTED_AT)
            .arrivedAt(DEFAULT_ARRIVED_AT);
        // Add required entity
        ShipmentOrder shipmentOrder;
        if (TestUtil.findAll(em, ShipmentOrder.class).isEmpty()) {
            shipmentOrder = ShipmentOrderResourceIT.createEntity(em);
            em.persist(shipmentOrder);
            em.flush();
        } else {
            shipmentOrder = TestUtil.findAll(em, ShipmentOrder.class).get(0);
        }
        orderLeg.setOrder(shipmentOrder);
        // Add required entity
        Office office;
        if (TestUtil.findAll(em, Office.class).isEmpty()) {
            office = OfficeResourceIT.createEntity();
            em.persist(office);
            em.flush();
        } else {
            office = TestUtil.findAll(em, Office.class).get(0);
        }
        orderLeg.setFromOffice(office);
        // Add required entity
        orderLeg.setToOffice(office);
        return orderLeg;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static OrderLeg createUpdatedEntity(EntityManager em) {
        OrderLeg updatedOrderLeg = new OrderLeg()
            .legIndex(UPDATED_LEG_INDEX)
            .status(UPDATED_STATUS)
            .departedAt(UPDATED_DEPARTED_AT)
            .arrivedAt(UPDATED_ARRIVED_AT);
        // Add required entity
        ShipmentOrder shipmentOrder;
        if (TestUtil.findAll(em, ShipmentOrder.class).isEmpty()) {
            shipmentOrder = ShipmentOrderResourceIT.createUpdatedEntity(em);
            em.persist(shipmentOrder);
            em.flush();
        } else {
            shipmentOrder = TestUtil.findAll(em, ShipmentOrder.class).get(0);
        }
        updatedOrderLeg.setOrder(shipmentOrder);
        // Add required entity
        Office office;
        if (TestUtil.findAll(em, Office.class).isEmpty()) {
            office = OfficeResourceIT.createUpdatedEntity();
            em.persist(office);
            em.flush();
        } else {
            office = TestUtil.findAll(em, Office.class).get(0);
        }
        updatedOrderLeg.setFromOffice(office);
        // Add required entity
        updatedOrderLeg.setToOffice(office);
        return updatedOrderLeg;
    }

    @BeforeEach
    public void initTest() {
        orderLeg = createEntity(em);
    }

    @AfterEach
    public void cleanup() {
        if (insertedOrderLeg != null) {
            orderLegRepository.delete(insertedOrderLeg);
            insertedOrderLeg = null;
        }
    }

    @Test
    @Transactional
    void createOrderLeg() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the OrderLeg
        OrderLegDTO orderLegDTO = orderLegMapper.toDto(orderLeg);
        var returnedOrderLegDTO = om.readValue(
            restOrderLegMockMvc
                .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(orderLegDTO)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            OrderLegDTO.class
        );

        // Validate the OrderLeg in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedOrderLeg = orderLegMapper.toEntity(returnedOrderLegDTO);
        assertOrderLegUpdatableFieldsEquals(returnedOrderLeg, getPersistedOrderLeg(returnedOrderLeg));

        insertedOrderLeg = returnedOrderLeg;
    }

    @Test
    @Transactional
    void createOrderLegWithExistingId() throws Exception {
        // Create the OrderLeg with an existing ID
        orderLeg.setId(1L);
        OrderLegDTO orderLegDTO = orderLegMapper.toDto(orderLeg);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restOrderLegMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(orderLegDTO)))
            .andExpect(status().isBadRequest());

        // Validate the OrderLeg in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkLegIndexIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        orderLeg.setLegIndex(null);

        // Create the OrderLeg, which fails.
        OrderLegDTO orderLegDTO = orderLegMapper.toDto(orderLeg);

        restOrderLegMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(orderLegDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkStatusIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        orderLeg.setStatus(null);

        // Create the OrderLeg, which fails.
        OrderLegDTO orderLegDTO = orderLegMapper.toDto(orderLeg);

        restOrderLegMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(orderLegDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllOrderLegs() throws Exception {
        // Initialize the database
        insertedOrderLeg = orderLegRepository.saveAndFlush(orderLeg);

        // Get all the orderLegList
        restOrderLegMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(orderLeg.getId().intValue())))
            .andExpect(jsonPath("$.[*].legIndex").value(hasItem(DEFAULT_LEG_INDEX)))
            .andExpect(jsonPath("$.[*].status").value(hasItem(DEFAULT_STATUS.toString())))
            .andExpect(jsonPath("$.[*].departedAt").value(hasItem(DEFAULT_DEPARTED_AT.toString())))
            .andExpect(jsonPath("$.[*].arrivedAt").value(hasItem(DEFAULT_ARRIVED_AT.toString())));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllOrderLegsWithEagerRelationshipsIsEnabled() throws Exception {
        when(orderLegServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restOrderLegMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(orderLegServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllOrderLegsWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(orderLegServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restOrderLegMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(orderLegRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getOrderLeg() throws Exception {
        // Initialize the database
        insertedOrderLeg = orderLegRepository.saveAndFlush(orderLeg);

        // Get the orderLeg
        restOrderLegMockMvc
            .perform(get(ENTITY_API_URL_ID, orderLeg.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(orderLeg.getId().intValue()))
            .andExpect(jsonPath("$.legIndex").value(DEFAULT_LEG_INDEX))
            .andExpect(jsonPath("$.status").value(DEFAULT_STATUS.toString()))
            .andExpect(jsonPath("$.departedAt").value(DEFAULT_DEPARTED_AT.toString()))
            .andExpect(jsonPath("$.arrivedAt").value(DEFAULT_ARRIVED_AT.toString()));
    }

    @Test
    @Transactional
    void getNonExistingOrderLeg() throws Exception {
        // Get the orderLeg
        restOrderLegMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingOrderLeg() throws Exception {
        // Initialize the database
        insertedOrderLeg = orderLegRepository.saveAndFlush(orderLeg);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the orderLeg
        OrderLeg updatedOrderLeg = orderLegRepository.findById(orderLeg.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedOrderLeg are not directly saved in db
        em.detach(updatedOrderLeg);
        updatedOrderLeg.legIndex(UPDATED_LEG_INDEX).status(UPDATED_STATUS).departedAt(UPDATED_DEPARTED_AT).arrivedAt(UPDATED_ARRIVED_AT);
        OrderLegDTO orderLegDTO = orderLegMapper.toDto(updatedOrderLeg);

        restOrderLegMockMvc
            .perform(
                put(ENTITY_API_URL_ID, orderLegDTO.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(orderLegDTO))
            )
            .andExpect(status().isOk());

        // Validate the OrderLeg in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedOrderLegToMatchAllProperties(updatedOrderLeg);
    }

    @Test
    @Transactional
    void putNonExistingOrderLeg() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        orderLeg.setId(longCount.incrementAndGet());

        // Create the OrderLeg
        OrderLegDTO orderLegDTO = orderLegMapper.toDto(orderLeg);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restOrderLegMockMvc
            .perform(
                put(ENTITY_API_URL_ID, orderLegDTO.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(orderLegDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the OrderLeg in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchOrderLeg() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        orderLeg.setId(longCount.incrementAndGet());

        // Create the OrderLeg
        OrderLegDTO orderLegDTO = orderLegMapper.toDto(orderLeg);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restOrderLegMockMvc
            .perform(
                put(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(orderLegDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the OrderLeg in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamOrderLeg() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        orderLeg.setId(longCount.incrementAndGet());

        // Create the OrderLeg
        OrderLegDTO orderLegDTO = orderLegMapper.toDto(orderLeg);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restOrderLegMockMvc
            .perform(put(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(orderLegDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the OrderLeg in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateOrderLegWithPatch() throws Exception {
        // Initialize the database
        insertedOrderLeg = orderLegRepository.saveAndFlush(orderLeg);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the orderLeg using partial update
        OrderLeg partialUpdatedOrderLeg = new OrderLeg();
        partialUpdatedOrderLeg.setId(orderLeg.getId());

        partialUpdatedOrderLeg.legIndex(UPDATED_LEG_INDEX).status(UPDATED_STATUS).departedAt(UPDATED_DEPARTED_AT);

        restOrderLegMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedOrderLeg.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedOrderLeg))
            )
            .andExpect(status().isOk());

        // Validate the OrderLeg in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertOrderLegUpdatableFieldsEquals(createUpdateProxyForBean(partialUpdatedOrderLeg, orderLeg), getPersistedOrderLeg(orderLeg));
    }

    @Test
    @Transactional
    void fullUpdateOrderLegWithPatch() throws Exception {
        // Initialize the database
        insertedOrderLeg = orderLegRepository.saveAndFlush(orderLeg);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the orderLeg using partial update
        OrderLeg partialUpdatedOrderLeg = new OrderLeg();
        partialUpdatedOrderLeg.setId(orderLeg.getId());

        partialUpdatedOrderLeg
            .legIndex(UPDATED_LEG_INDEX)
            .status(UPDATED_STATUS)
            .departedAt(UPDATED_DEPARTED_AT)
            .arrivedAt(UPDATED_ARRIVED_AT);

        restOrderLegMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedOrderLeg.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedOrderLeg))
            )
            .andExpect(status().isOk());

        // Validate the OrderLeg in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertOrderLegUpdatableFieldsEquals(partialUpdatedOrderLeg, getPersistedOrderLeg(partialUpdatedOrderLeg));
    }

    @Test
    @Transactional
    void patchNonExistingOrderLeg() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        orderLeg.setId(longCount.incrementAndGet());

        // Create the OrderLeg
        OrderLegDTO orderLegDTO = orderLegMapper.toDto(orderLeg);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restOrderLegMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, orderLegDTO.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(orderLegDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the OrderLeg in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchOrderLeg() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        orderLeg.setId(longCount.incrementAndGet());

        // Create the OrderLeg
        OrderLegDTO orderLegDTO = orderLegMapper.toDto(orderLeg);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restOrderLegMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(orderLegDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the OrderLeg in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamOrderLeg() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        orderLeg.setId(longCount.incrementAndGet());

        // Create the OrderLeg
        OrderLegDTO orderLegDTO = orderLegMapper.toDto(orderLeg);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restOrderLegMockMvc
            .perform(patch(ENTITY_API_URL).contentType("application/merge-patch+json").content(om.writeValueAsBytes(orderLegDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the OrderLeg in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteOrderLeg() throws Exception {
        // Initialize the database
        insertedOrderLeg = orderLegRepository.saveAndFlush(orderLeg);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the orderLeg
        restOrderLegMockMvc
            .perform(delete(ENTITY_API_URL_ID, orderLeg.getId()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return orderLegRepository.count();
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

    protected OrderLeg getPersistedOrderLeg(OrderLeg orderLeg) {
        return orderLegRepository.findById(orderLeg.getId()).orElseThrow();
    }

    protected void assertPersistedOrderLegToMatchAllProperties(OrderLeg expectedOrderLeg) {
        assertOrderLegAllPropertiesEquals(expectedOrderLeg, getPersistedOrderLeg(expectedOrderLeg));
    }

    protected void assertPersistedOrderLegToMatchUpdatableProperties(OrderLeg expectedOrderLeg) {
        assertOrderLegAllUpdatablePropertiesEquals(expectedOrderLeg, getPersistedOrderLeg(expectedOrderLeg));
    }
}
