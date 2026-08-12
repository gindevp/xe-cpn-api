package com.mycompany.myapp.web.rest;

import static com.mycompany.myapp.domain.OrderPaymentAsserts.*;
import static com.mycompany.myapp.web.rest.TestUtil.createUpdateProxyForBean;
import static com.mycompany.myapp.web.rest.TestUtil.sameNumber;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.myapp.IntegrationTest;
import com.mycompany.myapp.domain.OrderPayment;
import com.mycompany.myapp.domain.ShipmentOrder;
import com.mycompany.myapp.domain.enumeration.PaymentKind;
import com.mycompany.myapp.domain.enumeration.PaymentMethod;
import com.mycompany.myapp.repository.OrderPaymentRepository;
import com.mycompany.myapp.service.OrderPaymentService;
import com.mycompany.myapp.service.dto.OrderPaymentDTO;
import com.mycompany.myapp.service.mapper.OrderPaymentMapper;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
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
 * Integration tests for the {@link OrderPaymentResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser
class OrderPaymentResourceIT {

    private static final Instant DEFAULT_PAYMENT_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_PAYMENT_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final BigDecimal DEFAULT_AMOUNT = new BigDecimal(1);
    private static final BigDecimal UPDATED_AMOUNT = new BigDecimal(2);

    private static final PaymentMethod DEFAULT_METHOD = PaymentMethod.TM;
    private static final PaymentMethod UPDATED_METHOD = PaymentMethod.CK;

    private static final PaymentKind DEFAULT_PAYMENT_KIND = PaymentKind.TRUOC;
    private static final PaymentKind UPDATED_PAYMENT_KIND = PaymentKind.SAU;

    private static final String DEFAULT_NOTE = "AAAAAAAAAA";
    private static final String UPDATED_NOTE = "BBBBBBBBBB";

    private static final String DEFAULT_COLLECTOR_USERNAME = "AAAAAAAAAA";
    private static final String UPDATED_COLLECTOR_USERNAME = "BBBBBBBBBB";

    private static final String ENTITY_API_URL = "/api/order-payments";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static Random random = new Random();
    private static AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    @Autowired
    private ObjectMapper om;

    @Autowired
    private OrderPaymentRepository orderPaymentRepository;

    @Mock
    private OrderPaymentRepository orderPaymentRepositoryMock;

    @Autowired
    private OrderPaymentMapper orderPaymentMapper;

    @Mock
    private OrderPaymentService orderPaymentServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restOrderPaymentMockMvc;

    private OrderPayment orderPayment;

    private OrderPayment insertedOrderPayment;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static OrderPayment createEntity(EntityManager em) {
        OrderPayment orderPayment = new OrderPayment()
            .paymentAt(DEFAULT_PAYMENT_AT)
            .amount(DEFAULT_AMOUNT)
            .method(DEFAULT_METHOD)
            .paymentKind(DEFAULT_PAYMENT_KIND)
            .note(DEFAULT_NOTE)
            .collectorUsername(DEFAULT_COLLECTOR_USERNAME);
        // Add required entity
        ShipmentOrder shipmentOrder;
        if (TestUtil.findAll(em, ShipmentOrder.class).isEmpty()) {
            shipmentOrder = ShipmentOrderResourceIT.createEntity(em);
            em.persist(shipmentOrder);
            em.flush();
        } else {
            shipmentOrder = TestUtil.findAll(em, ShipmentOrder.class).get(0);
        }
        orderPayment.setOrder(shipmentOrder);
        return orderPayment;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static OrderPayment createUpdatedEntity(EntityManager em) {
        OrderPayment updatedOrderPayment = new OrderPayment()
            .paymentAt(UPDATED_PAYMENT_AT)
            .amount(UPDATED_AMOUNT)
            .method(UPDATED_METHOD)
            .paymentKind(UPDATED_PAYMENT_KIND)
            .note(UPDATED_NOTE)
            .collectorUsername(UPDATED_COLLECTOR_USERNAME);
        // Add required entity
        ShipmentOrder shipmentOrder;
        if (TestUtil.findAll(em, ShipmentOrder.class).isEmpty()) {
            shipmentOrder = ShipmentOrderResourceIT.createUpdatedEntity(em);
            em.persist(shipmentOrder);
            em.flush();
        } else {
            shipmentOrder = TestUtil.findAll(em, ShipmentOrder.class).get(0);
        }
        updatedOrderPayment.setOrder(shipmentOrder);
        return updatedOrderPayment;
    }

    @BeforeEach
    public void initTest() {
        orderPayment = createEntity(em);
    }

    @AfterEach
    public void cleanup() {
        if (insertedOrderPayment != null) {
            orderPaymentRepository.delete(insertedOrderPayment);
            insertedOrderPayment = null;
        }
    }

    @Test
    @Transactional
    void createOrderPayment() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the OrderPayment
        OrderPaymentDTO orderPaymentDTO = orderPaymentMapper.toDto(orderPayment);
        var returnedOrderPaymentDTO = om.readValue(
            restOrderPaymentMockMvc
                .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(orderPaymentDTO)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            OrderPaymentDTO.class
        );

        // Validate the OrderPayment in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedOrderPayment = orderPaymentMapper.toEntity(returnedOrderPaymentDTO);
        assertOrderPaymentUpdatableFieldsEquals(returnedOrderPayment, getPersistedOrderPayment(returnedOrderPayment));

        insertedOrderPayment = returnedOrderPayment;
    }

    @Test
    @Transactional
    void createOrderPaymentWithExistingId() throws Exception {
        // Create the OrderPayment with an existing ID
        orderPayment.setId(1L);
        OrderPaymentDTO orderPaymentDTO = orderPaymentMapper.toDto(orderPayment);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restOrderPaymentMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(orderPaymentDTO)))
            .andExpect(status().isBadRequest());

        // Validate the OrderPayment in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkPaymentAtIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        orderPayment.setPaymentAt(null);

        // Create the OrderPayment, which fails.
        OrderPaymentDTO orderPaymentDTO = orderPaymentMapper.toDto(orderPayment);

        restOrderPaymentMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(orderPaymentDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkAmountIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        orderPayment.setAmount(null);

        // Create the OrderPayment, which fails.
        OrderPaymentDTO orderPaymentDTO = orderPaymentMapper.toDto(orderPayment);

        restOrderPaymentMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(orderPaymentDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkMethodIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        orderPayment.setMethod(null);

        // Create the OrderPayment, which fails.
        OrderPaymentDTO orderPaymentDTO = orderPaymentMapper.toDto(orderPayment);

        restOrderPaymentMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(orderPaymentDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkPaymentKindIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        orderPayment.setPaymentKind(null);

        // Create the OrderPayment, which fails.
        OrderPaymentDTO orderPaymentDTO = orderPaymentMapper.toDto(orderPayment);

        restOrderPaymentMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(orderPaymentDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkCollectorUsernameIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        orderPayment.setCollectorUsername(null);

        // Create the OrderPayment, which fails.
        OrderPaymentDTO orderPaymentDTO = orderPaymentMapper.toDto(orderPayment);

        restOrderPaymentMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(orderPaymentDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllOrderPayments() throws Exception {
        // Initialize the database
        insertedOrderPayment = orderPaymentRepository.saveAndFlush(orderPayment);

        // Get all the orderPaymentList
        restOrderPaymentMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(orderPayment.getId().intValue())))
            .andExpect(jsonPath("$.[*].paymentAt").value(hasItem(DEFAULT_PAYMENT_AT.toString())))
            .andExpect(jsonPath("$.[*].amount").value(hasItem(sameNumber(DEFAULT_AMOUNT))))
            .andExpect(jsonPath("$.[*].method").value(hasItem(DEFAULT_METHOD.toString())))
            .andExpect(jsonPath("$.[*].paymentKind").value(hasItem(DEFAULT_PAYMENT_KIND.toString())))
            .andExpect(jsonPath("$.[*].note").value(hasItem(DEFAULT_NOTE)))
            .andExpect(jsonPath("$.[*].collectorUsername").value(hasItem(DEFAULT_COLLECTOR_USERNAME)));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllOrderPaymentsWithEagerRelationshipsIsEnabled() throws Exception {
        when(orderPaymentServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restOrderPaymentMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(orderPaymentServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllOrderPaymentsWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(orderPaymentServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restOrderPaymentMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(orderPaymentRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getOrderPayment() throws Exception {
        // Initialize the database
        insertedOrderPayment = orderPaymentRepository.saveAndFlush(orderPayment);

        // Get the orderPayment
        restOrderPaymentMockMvc
            .perform(get(ENTITY_API_URL_ID, orderPayment.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(orderPayment.getId().intValue()))
            .andExpect(jsonPath("$.paymentAt").value(DEFAULT_PAYMENT_AT.toString()))
            .andExpect(jsonPath("$.amount").value(sameNumber(DEFAULT_AMOUNT)))
            .andExpect(jsonPath("$.method").value(DEFAULT_METHOD.toString()))
            .andExpect(jsonPath("$.paymentKind").value(DEFAULT_PAYMENT_KIND.toString()))
            .andExpect(jsonPath("$.note").value(DEFAULT_NOTE))
            .andExpect(jsonPath("$.collectorUsername").value(DEFAULT_COLLECTOR_USERNAME));
    }

    @Test
    @Transactional
    void getNonExistingOrderPayment() throws Exception {
        // Get the orderPayment
        restOrderPaymentMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingOrderPayment() throws Exception {
        // Initialize the database
        insertedOrderPayment = orderPaymentRepository.saveAndFlush(orderPayment);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the orderPayment
        OrderPayment updatedOrderPayment = orderPaymentRepository.findById(orderPayment.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedOrderPayment are not directly saved in db
        em.detach(updatedOrderPayment);
        updatedOrderPayment
            .paymentAt(UPDATED_PAYMENT_AT)
            .amount(UPDATED_AMOUNT)
            .method(UPDATED_METHOD)
            .paymentKind(UPDATED_PAYMENT_KIND)
            .note(UPDATED_NOTE)
            .collectorUsername(UPDATED_COLLECTOR_USERNAME);
        OrderPaymentDTO orderPaymentDTO = orderPaymentMapper.toDto(updatedOrderPayment);

        restOrderPaymentMockMvc
            .perform(
                put(ENTITY_API_URL_ID, orderPaymentDTO.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(orderPaymentDTO))
            )
            .andExpect(status().isOk());

        // Validate the OrderPayment in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedOrderPaymentToMatchAllProperties(updatedOrderPayment);
    }

    @Test
    @Transactional
    void putNonExistingOrderPayment() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        orderPayment.setId(longCount.incrementAndGet());

        // Create the OrderPayment
        OrderPaymentDTO orderPaymentDTO = orderPaymentMapper.toDto(orderPayment);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restOrderPaymentMockMvc
            .perform(
                put(ENTITY_API_URL_ID, orderPaymentDTO.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(orderPaymentDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the OrderPayment in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchOrderPayment() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        orderPayment.setId(longCount.incrementAndGet());

        // Create the OrderPayment
        OrderPaymentDTO orderPaymentDTO = orderPaymentMapper.toDto(orderPayment);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restOrderPaymentMockMvc
            .perform(
                put(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(orderPaymentDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the OrderPayment in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamOrderPayment() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        orderPayment.setId(longCount.incrementAndGet());

        // Create the OrderPayment
        OrderPaymentDTO orderPaymentDTO = orderPaymentMapper.toDto(orderPayment);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restOrderPaymentMockMvc
            .perform(put(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(orderPaymentDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the OrderPayment in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateOrderPaymentWithPatch() throws Exception {
        // Initialize the database
        insertedOrderPayment = orderPaymentRepository.saveAndFlush(orderPayment);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the orderPayment using partial update
        OrderPayment partialUpdatedOrderPayment = new OrderPayment();
        partialUpdatedOrderPayment.setId(orderPayment.getId());

        partialUpdatedOrderPayment.paymentAt(UPDATED_PAYMENT_AT).method(UPDATED_METHOD).paymentKind(UPDATED_PAYMENT_KIND);

        restOrderPaymentMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedOrderPayment.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedOrderPayment))
            )
            .andExpect(status().isOk());

        // Validate the OrderPayment in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertOrderPaymentUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedOrderPayment, orderPayment),
            getPersistedOrderPayment(orderPayment)
        );
    }

    @Test
    @Transactional
    void fullUpdateOrderPaymentWithPatch() throws Exception {
        // Initialize the database
        insertedOrderPayment = orderPaymentRepository.saveAndFlush(orderPayment);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the orderPayment using partial update
        OrderPayment partialUpdatedOrderPayment = new OrderPayment();
        partialUpdatedOrderPayment.setId(orderPayment.getId());

        partialUpdatedOrderPayment
            .paymentAt(UPDATED_PAYMENT_AT)
            .amount(UPDATED_AMOUNT)
            .method(UPDATED_METHOD)
            .paymentKind(UPDATED_PAYMENT_KIND)
            .note(UPDATED_NOTE)
            .collectorUsername(UPDATED_COLLECTOR_USERNAME);

        restOrderPaymentMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedOrderPayment.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedOrderPayment))
            )
            .andExpect(status().isOk());

        // Validate the OrderPayment in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertOrderPaymentUpdatableFieldsEquals(partialUpdatedOrderPayment, getPersistedOrderPayment(partialUpdatedOrderPayment));
    }

    @Test
    @Transactional
    void patchNonExistingOrderPayment() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        orderPayment.setId(longCount.incrementAndGet());

        // Create the OrderPayment
        OrderPaymentDTO orderPaymentDTO = orderPaymentMapper.toDto(orderPayment);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restOrderPaymentMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, orderPaymentDTO.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(orderPaymentDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the OrderPayment in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchOrderPayment() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        orderPayment.setId(longCount.incrementAndGet());

        // Create the OrderPayment
        OrderPaymentDTO orderPaymentDTO = orderPaymentMapper.toDto(orderPayment);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restOrderPaymentMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(orderPaymentDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the OrderPayment in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamOrderPayment() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        orderPayment.setId(longCount.incrementAndGet());

        // Create the OrderPayment
        OrderPaymentDTO orderPaymentDTO = orderPaymentMapper.toDto(orderPayment);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restOrderPaymentMockMvc
            .perform(patch(ENTITY_API_URL).contentType("application/merge-patch+json").content(om.writeValueAsBytes(orderPaymentDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the OrderPayment in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteOrderPayment() throws Exception {
        // Initialize the database
        insertedOrderPayment = orderPaymentRepository.saveAndFlush(orderPayment);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the orderPayment
        restOrderPaymentMockMvc
            .perform(delete(ENTITY_API_URL_ID, orderPayment.getId()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return orderPaymentRepository.count();
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

    protected OrderPayment getPersistedOrderPayment(OrderPayment orderPayment) {
        return orderPaymentRepository.findById(orderPayment.getId()).orElseThrow();
    }

    protected void assertPersistedOrderPaymentToMatchAllProperties(OrderPayment expectedOrderPayment) {
        assertOrderPaymentAllPropertiesEquals(expectedOrderPayment, getPersistedOrderPayment(expectedOrderPayment));
    }

    protected void assertPersistedOrderPaymentToMatchUpdatableProperties(OrderPayment expectedOrderPayment) {
        assertOrderPaymentAllUpdatablePropertiesEquals(expectedOrderPayment, getPersistedOrderPayment(expectedOrderPayment));
    }
}
