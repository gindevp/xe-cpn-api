package com.mycompany.myapp.web.rest;

import static com.mycompany.myapp.domain.OrderDeliveryAttemptAsserts.*;
import static com.mycompany.myapp.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.myapp.IntegrationTest;
import com.mycompany.myapp.domain.OrderDeliveryAttempt;
import com.mycompany.myapp.domain.ShipmentOrder;
import com.mycompany.myapp.domain.enumeration.DeliveryAttemptResult;
import com.mycompany.myapp.domain.enumeration.DeliveryPartner;
import com.mycompany.myapp.repository.OrderDeliveryAttemptRepository;
import com.mycompany.myapp.service.OrderDeliveryAttemptService;
import com.mycompany.myapp.service.dto.OrderDeliveryAttemptDTO;
import com.mycompany.myapp.service.mapper.OrderDeliveryAttemptMapper;
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
 * Integration tests for the {@link OrderDeliveryAttemptResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser
class OrderDeliveryAttemptResourceIT {

    private static final Integer DEFAULT_ATTEMPT_NO = 1;
    private static final Integer UPDATED_ATTEMPT_NO = 2;

    private static final Instant DEFAULT_ATTEMPT_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_ATTEMPT_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final DeliveryAttemptResult DEFAULT_RESULT = DeliveryAttemptResult.SUCCESS;
    private static final DeliveryAttemptResult UPDATED_RESULT = DeliveryAttemptResult.FAILED;

    private static final String DEFAULT_REASON = "AAAAAAAAAA";
    private static final String UPDATED_REASON = "BBBBBBBBBB";

    private static final String DEFAULT_HANDLED_BY_USERNAME = "AAAAAAAAAA";
    private static final String UPDATED_HANDLED_BY_USERNAME = "BBBBBBBBBB";

    private static final DeliveryPartner DEFAULT_DELIVERY_PARTNER = DeliveryPartner.AHAMOVE;
    private static final DeliveryPartner UPDATED_DELIVERY_PARTNER = DeliveryPartner.GRAB;

    private static final String ENTITY_API_URL = "/api/order-delivery-attempts";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static Random random = new Random();
    private static AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    @Autowired
    private ObjectMapper om;

    @Autowired
    private OrderDeliveryAttemptRepository orderDeliveryAttemptRepository;

    @Mock
    private OrderDeliveryAttemptRepository orderDeliveryAttemptRepositoryMock;

    @Autowired
    private OrderDeliveryAttemptMapper orderDeliveryAttemptMapper;

    @Mock
    private OrderDeliveryAttemptService orderDeliveryAttemptServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restOrderDeliveryAttemptMockMvc;

    private OrderDeliveryAttempt orderDeliveryAttempt;

    private OrderDeliveryAttempt insertedOrderDeliveryAttempt;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static OrderDeliveryAttempt createEntity(EntityManager em) {
        OrderDeliveryAttempt orderDeliveryAttempt = new OrderDeliveryAttempt()
            .attemptNo(DEFAULT_ATTEMPT_NO)
            .attemptAt(DEFAULT_ATTEMPT_AT)
            .result(DEFAULT_RESULT)
            .reason(DEFAULT_REASON)
            .handledByUsername(DEFAULT_HANDLED_BY_USERNAME)
            .deliveryPartner(DEFAULT_DELIVERY_PARTNER);
        // Add required entity
        ShipmentOrder shipmentOrder;
        if (TestUtil.findAll(em, ShipmentOrder.class).isEmpty()) {
            shipmentOrder = ShipmentOrderResourceIT.createEntity(em);
            em.persist(shipmentOrder);
            em.flush();
        } else {
            shipmentOrder = TestUtil.findAll(em, ShipmentOrder.class).get(0);
        }
        orderDeliveryAttempt.setOrder(shipmentOrder);
        return orderDeliveryAttempt;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static OrderDeliveryAttempt createUpdatedEntity(EntityManager em) {
        OrderDeliveryAttempt updatedOrderDeliveryAttempt = new OrderDeliveryAttempt()
            .attemptNo(UPDATED_ATTEMPT_NO)
            .attemptAt(UPDATED_ATTEMPT_AT)
            .result(UPDATED_RESULT)
            .reason(UPDATED_REASON)
            .handledByUsername(UPDATED_HANDLED_BY_USERNAME)
            .deliveryPartner(UPDATED_DELIVERY_PARTNER);
        // Add required entity
        ShipmentOrder shipmentOrder;
        if (TestUtil.findAll(em, ShipmentOrder.class).isEmpty()) {
            shipmentOrder = ShipmentOrderResourceIT.createUpdatedEntity(em);
            em.persist(shipmentOrder);
            em.flush();
        } else {
            shipmentOrder = TestUtil.findAll(em, ShipmentOrder.class).get(0);
        }
        updatedOrderDeliveryAttempt.setOrder(shipmentOrder);
        return updatedOrderDeliveryAttempt;
    }

    @BeforeEach
    public void initTest() {
        orderDeliveryAttempt = createEntity(em);
    }

    @AfterEach
    public void cleanup() {
        if (insertedOrderDeliveryAttempt != null) {
            orderDeliveryAttemptRepository.delete(insertedOrderDeliveryAttempt);
            insertedOrderDeliveryAttempt = null;
        }
    }

    @Test
    @Transactional
    void createOrderDeliveryAttempt() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the OrderDeliveryAttempt
        OrderDeliveryAttemptDTO orderDeliveryAttemptDTO = orderDeliveryAttemptMapper.toDto(orderDeliveryAttempt);
        var returnedOrderDeliveryAttemptDTO = om.readValue(
            restOrderDeliveryAttemptMockMvc
                .perform(
                    post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(orderDeliveryAttemptDTO))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            OrderDeliveryAttemptDTO.class
        );

        // Validate the OrderDeliveryAttempt in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedOrderDeliveryAttempt = orderDeliveryAttemptMapper.toEntity(returnedOrderDeliveryAttemptDTO);
        assertOrderDeliveryAttemptUpdatableFieldsEquals(
            returnedOrderDeliveryAttempt,
            getPersistedOrderDeliveryAttempt(returnedOrderDeliveryAttempt)
        );

        insertedOrderDeliveryAttempt = returnedOrderDeliveryAttempt;
    }

    @Test
    @Transactional
    void createOrderDeliveryAttemptWithExistingId() throws Exception {
        // Create the OrderDeliveryAttempt with an existing ID
        orderDeliveryAttempt.setId(1L);
        OrderDeliveryAttemptDTO orderDeliveryAttemptDTO = orderDeliveryAttemptMapper.toDto(orderDeliveryAttempt);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restOrderDeliveryAttemptMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(orderDeliveryAttemptDTO)))
            .andExpect(status().isBadRequest());

        // Validate the OrderDeliveryAttempt in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkAttemptNoIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        orderDeliveryAttempt.setAttemptNo(null);

        // Create the OrderDeliveryAttempt, which fails.
        OrderDeliveryAttemptDTO orderDeliveryAttemptDTO = orderDeliveryAttemptMapper.toDto(orderDeliveryAttempt);

        restOrderDeliveryAttemptMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(orderDeliveryAttemptDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkAttemptAtIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        orderDeliveryAttempt.setAttemptAt(null);

        // Create the OrderDeliveryAttempt, which fails.
        OrderDeliveryAttemptDTO orderDeliveryAttemptDTO = orderDeliveryAttemptMapper.toDto(orderDeliveryAttempt);

        restOrderDeliveryAttemptMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(orderDeliveryAttemptDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkResultIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        orderDeliveryAttempt.setResult(null);

        // Create the OrderDeliveryAttempt, which fails.
        OrderDeliveryAttemptDTO orderDeliveryAttemptDTO = orderDeliveryAttemptMapper.toDto(orderDeliveryAttempt);

        restOrderDeliveryAttemptMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(orderDeliveryAttemptDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkHandledByUsernameIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        orderDeliveryAttempt.setHandledByUsername(null);

        // Create the OrderDeliveryAttempt, which fails.
        OrderDeliveryAttemptDTO orderDeliveryAttemptDTO = orderDeliveryAttemptMapper.toDto(orderDeliveryAttempt);

        restOrderDeliveryAttemptMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(orderDeliveryAttemptDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllOrderDeliveryAttempts() throws Exception {
        // Initialize the database
        insertedOrderDeliveryAttempt = orderDeliveryAttemptRepository.saveAndFlush(orderDeliveryAttempt);

        // Get all the orderDeliveryAttemptList
        restOrderDeliveryAttemptMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(orderDeliveryAttempt.getId().intValue())))
            .andExpect(jsonPath("$.[*].attemptNo").value(hasItem(DEFAULT_ATTEMPT_NO)))
            .andExpect(jsonPath("$.[*].attemptAt").value(hasItem(DEFAULT_ATTEMPT_AT.toString())))
            .andExpect(jsonPath("$.[*].result").value(hasItem(DEFAULT_RESULT.toString())))
            .andExpect(jsonPath("$.[*].reason").value(hasItem(DEFAULT_REASON)))
            .andExpect(jsonPath("$.[*].handledByUsername").value(hasItem(DEFAULT_HANDLED_BY_USERNAME)))
            .andExpect(jsonPath("$.[*].deliveryPartner").value(hasItem(DEFAULT_DELIVERY_PARTNER.toString())));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllOrderDeliveryAttemptsWithEagerRelationshipsIsEnabled() throws Exception {
        when(orderDeliveryAttemptServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restOrderDeliveryAttemptMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(orderDeliveryAttemptServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllOrderDeliveryAttemptsWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(orderDeliveryAttemptServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restOrderDeliveryAttemptMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(orderDeliveryAttemptRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getOrderDeliveryAttempt() throws Exception {
        // Initialize the database
        insertedOrderDeliveryAttempt = orderDeliveryAttemptRepository.saveAndFlush(orderDeliveryAttempt);

        // Get the orderDeliveryAttempt
        restOrderDeliveryAttemptMockMvc
            .perform(get(ENTITY_API_URL_ID, orderDeliveryAttempt.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(orderDeliveryAttempt.getId().intValue()))
            .andExpect(jsonPath("$.attemptNo").value(DEFAULT_ATTEMPT_NO))
            .andExpect(jsonPath("$.attemptAt").value(DEFAULT_ATTEMPT_AT.toString()))
            .andExpect(jsonPath("$.result").value(DEFAULT_RESULT.toString()))
            .andExpect(jsonPath("$.reason").value(DEFAULT_REASON))
            .andExpect(jsonPath("$.handledByUsername").value(DEFAULT_HANDLED_BY_USERNAME))
            .andExpect(jsonPath("$.deliveryPartner").value(DEFAULT_DELIVERY_PARTNER.toString()));
    }

    @Test
    @Transactional
    void getNonExistingOrderDeliveryAttempt() throws Exception {
        // Get the orderDeliveryAttempt
        restOrderDeliveryAttemptMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingOrderDeliveryAttempt() throws Exception {
        // Initialize the database
        insertedOrderDeliveryAttempt = orderDeliveryAttemptRepository.saveAndFlush(orderDeliveryAttempt);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the orderDeliveryAttempt
        OrderDeliveryAttempt updatedOrderDeliveryAttempt = orderDeliveryAttemptRepository
            .findById(orderDeliveryAttempt.getId())
            .orElseThrow();
        // Disconnect from session so that the updates on updatedOrderDeliveryAttempt are not directly saved in db
        em.detach(updatedOrderDeliveryAttempt);
        updatedOrderDeliveryAttempt
            .attemptNo(UPDATED_ATTEMPT_NO)
            .attemptAt(UPDATED_ATTEMPT_AT)
            .result(UPDATED_RESULT)
            .reason(UPDATED_REASON)
            .handledByUsername(UPDATED_HANDLED_BY_USERNAME)
            .deliveryPartner(UPDATED_DELIVERY_PARTNER);
        OrderDeliveryAttemptDTO orderDeliveryAttemptDTO = orderDeliveryAttemptMapper.toDto(updatedOrderDeliveryAttempt);

        restOrderDeliveryAttemptMockMvc
            .perform(
                put(ENTITY_API_URL_ID, orderDeliveryAttemptDTO.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(orderDeliveryAttemptDTO))
            )
            .andExpect(status().isOk());

        // Validate the OrderDeliveryAttempt in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedOrderDeliveryAttemptToMatchAllProperties(updatedOrderDeliveryAttempt);
    }

    @Test
    @Transactional
    void putNonExistingOrderDeliveryAttempt() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        orderDeliveryAttempt.setId(longCount.incrementAndGet());

        // Create the OrderDeliveryAttempt
        OrderDeliveryAttemptDTO orderDeliveryAttemptDTO = orderDeliveryAttemptMapper.toDto(orderDeliveryAttempt);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restOrderDeliveryAttemptMockMvc
            .perform(
                put(ENTITY_API_URL_ID, orderDeliveryAttemptDTO.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(orderDeliveryAttemptDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the OrderDeliveryAttempt in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchOrderDeliveryAttempt() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        orderDeliveryAttempt.setId(longCount.incrementAndGet());

        // Create the OrderDeliveryAttempt
        OrderDeliveryAttemptDTO orderDeliveryAttemptDTO = orderDeliveryAttemptMapper.toDto(orderDeliveryAttempt);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restOrderDeliveryAttemptMockMvc
            .perform(
                put(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(orderDeliveryAttemptDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the OrderDeliveryAttempt in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamOrderDeliveryAttempt() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        orderDeliveryAttempt.setId(longCount.incrementAndGet());

        // Create the OrderDeliveryAttempt
        OrderDeliveryAttemptDTO orderDeliveryAttemptDTO = orderDeliveryAttemptMapper.toDto(orderDeliveryAttempt);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restOrderDeliveryAttemptMockMvc
            .perform(put(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(orderDeliveryAttemptDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the OrderDeliveryAttempt in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateOrderDeliveryAttemptWithPatch() throws Exception {
        // Initialize the database
        insertedOrderDeliveryAttempt = orderDeliveryAttemptRepository.saveAndFlush(orderDeliveryAttempt);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the orderDeliveryAttempt using partial update
        OrderDeliveryAttempt partialUpdatedOrderDeliveryAttempt = new OrderDeliveryAttempt();
        partialUpdatedOrderDeliveryAttempt.setId(orderDeliveryAttempt.getId());

        partialUpdatedOrderDeliveryAttempt
            .attemptNo(UPDATED_ATTEMPT_NO)
            .attemptAt(UPDATED_ATTEMPT_AT)
            .reason(UPDATED_REASON)
            .deliveryPartner(UPDATED_DELIVERY_PARTNER);

        restOrderDeliveryAttemptMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedOrderDeliveryAttempt.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedOrderDeliveryAttempt))
            )
            .andExpect(status().isOk());

        // Validate the OrderDeliveryAttempt in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertOrderDeliveryAttemptUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedOrderDeliveryAttempt, orderDeliveryAttempt),
            getPersistedOrderDeliveryAttempt(orderDeliveryAttempt)
        );
    }

    @Test
    @Transactional
    void fullUpdateOrderDeliveryAttemptWithPatch() throws Exception {
        // Initialize the database
        insertedOrderDeliveryAttempt = orderDeliveryAttemptRepository.saveAndFlush(orderDeliveryAttempt);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the orderDeliveryAttempt using partial update
        OrderDeliveryAttempt partialUpdatedOrderDeliveryAttempt = new OrderDeliveryAttempt();
        partialUpdatedOrderDeliveryAttempt.setId(orderDeliveryAttempt.getId());

        partialUpdatedOrderDeliveryAttempt
            .attemptNo(UPDATED_ATTEMPT_NO)
            .attemptAt(UPDATED_ATTEMPT_AT)
            .result(UPDATED_RESULT)
            .reason(UPDATED_REASON)
            .handledByUsername(UPDATED_HANDLED_BY_USERNAME)
            .deliveryPartner(UPDATED_DELIVERY_PARTNER);

        restOrderDeliveryAttemptMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedOrderDeliveryAttempt.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedOrderDeliveryAttempt))
            )
            .andExpect(status().isOk());

        // Validate the OrderDeliveryAttempt in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertOrderDeliveryAttemptUpdatableFieldsEquals(
            partialUpdatedOrderDeliveryAttempt,
            getPersistedOrderDeliveryAttempt(partialUpdatedOrderDeliveryAttempt)
        );
    }

    @Test
    @Transactional
    void patchNonExistingOrderDeliveryAttempt() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        orderDeliveryAttempt.setId(longCount.incrementAndGet());

        // Create the OrderDeliveryAttempt
        OrderDeliveryAttemptDTO orderDeliveryAttemptDTO = orderDeliveryAttemptMapper.toDto(orderDeliveryAttempt);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restOrderDeliveryAttemptMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, orderDeliveryAttemptDTO.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(orderDeliveryAttemptDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the OrderDeliveryAttempt in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchOrderDeliveryAttempt() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        orderDeliveryAttempt.setId(longCount.incrementAndGet());

        // Create the OrderDeliveryAttempt
        OrderDeliveryAttemptDTO orderDeliveryAttemptDTO = orderDeliveryAttemptMapper.toDto(orderDeliveryAttempt);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restOrderDeliveryAttemptMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(orderDeliveryAttemptDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the OrderDeliveryAttempt in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamOrderDeliveryAttempt() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        orderDeliveryAttempt.setId(longCount.incrementAndGet());

        // Create the OrderDeliveryAttempt
        OrderDeliveryAttemptDTO orderDeliveryAttemptDTO = orderDeliveryAttemptMapper.toDto(orderDeliveryAttempt);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restOrderDeliveryAttemptMockMvc
            .perform(
                patch(ENTITY_API_URL).contentType("application/merge-patch+json").content(om.writeValueAsBytes(orderDeliveryAttemptDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the OrderDeliveryAttempt in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteOrderDeliveryAttempt() throws Exception {
        // Initialize the database
        insertedOrderDeliveryAttempt = orderDeliveryAttemptRepository.saveAndFlush(orderDeliveryAttempt);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the orderDeliveryAttempt
        restOrderDeliveryAttemptMockMvc
            .perform(delete(ENTITY_API_URL_ID, orderDeliveryAttempt.getId()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return orderDeliveryAttemptRepository.count();
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

    protected OrderDeliveryAttempt getPersistedOrderDeliveryAttempt(OrderDeliveryAttempt orderDeliveryAttempt) {
        return orderDeliveryAttemptRepository.findById(orderDeliveryAttempt.getId()).orElseThrow();
    }

    protected void assertPersistedOrderDeliveryAttemptToMatchAllProperties(OrderDeliveryAttempt expectedOrderDeliveryAttempt) {
        assertOrderDeliveryAttemptAllPropertiesEquals(
            expectedOrderDeliveryAttempt,
            getPersistedOrderDeliveryAttempt(expectedOrderDeliveryAttempt)
        );
    }

    protected void assertPersistedOrderDeliveryAttemptToMatchUpdatableProperties(OrderDeliveryAttempt expectedOrderDeliveryAttempt) {
        assertOrderDeliveryAttemptAllUpdatablePropertiesEquals(
            expectedOrderDeliveryAttempt,
            getPersistedOrderDeliveryAttempt(expectedOrderDeliveryAttempt)
        );
    }
}
