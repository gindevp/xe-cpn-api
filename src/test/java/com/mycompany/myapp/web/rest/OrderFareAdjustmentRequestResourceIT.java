package com.mycompany.myapp.web.rest;

import static com.mycompany.myapp.domain.OrderFareAdjustmentRequestAsserts.*;
import static com.mycompany.myapp.web.rest.TestUtil.createUpdateProxyForBean;
import static com.mycompany.myapp.web.rest.TestUtil.sameNumber;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.myapp.IntegrationTest;
import com.mycompany.myapp.domain.OrderFareAdjustmentRequest;
import com.mycompany.myapp.domain.enumeration.ApprovalStatus;
import com.mycompany.myapp.repository.OrderFareAdjustmentRequestRepository;
import com.mycompany.myapp.service.dto.OrderFareAdjustmentRequestDTO;
import com.mycompany.myapp.service.mapper.OrderFareAdjustmentRequestMapper;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
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
 * Integration tests for the {@link OrderFareAdjustmentRequestResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser
class OrderFareAdjustmentRequestResourceIT {

    private static final BigDecimal DEFAULT_REQUESTED_FARE_AMOUNT = new BigDecimal(0);
    private static final BigDecimal UPDATED_REQUESTED_FARE_AMOUNT = new BigDecimal(1);

    private static final String DEFAULT_REASON = "AAAAAAAAAA";
    private static final String UPDATED_REASON = "BBBBBBBBBB";

    private static final String DEFAULT_REQUESTED_BY_USERNAME = "AAAAAAAAAA";
    private static final String UPDATED_REQUESTED_BY_USERNAME = "BBBBBBBBBB";

    private static final Instant DEFAULT_REQUESTED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_REQUESTED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final ApprovalStatus DEFAULT_STATUS = ApprovalStatus.PENDING;
    private static final ApprovalStatus UPDATED_STATUS = ApprovalStatus.APPROVED;

    private static final String DEFAULT_APPROVED_BY_USERNAME = "AAAAAAAAAA";
    private static final String UPDATED_APPROVED_BY_USERNAME = "BBBBBBBBBB";

    private static final Instant DEFAULT_APPROVED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_APPROVED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final String DEFAULT_REJECTED_BY_USERNAME = "AAAAAAAAAA";
    private static final String UPDATED_REJECTED_BY_USERNAME = "BBBBBBBBBB";

    private static final Instant DEFAULT_REJECTED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_REJECTED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final String ENTITY_API_URL = "/api/order-fare-adjustment-requests";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static Random random = new Random();
    private static AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    @Autowired
    private ObjectMapper om;

    @Autowired
    private OrderFareAdjustmentRequestRepository orderFareAdjustmentRequestRepository;

    @Autowired
    private OrderFareAdjustmentRequestMapper orderFareAdjustmentRequestMapper;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restOrderFareAdjustmentRequestMockMvc;

    private OrderFareAdjustmentRequest orderFareAdjustmentRequest;

    private OrderFareAdjustmentRequest insertedOrderFareAdjustmentRequest;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static OrderFareAdjustmentRequest createEntity(EntityManager em) {
        OrderFareAdjustmentRequest orderFareAdjustmentRequest = new OrderFareAdjustmentRequest()
            .requestedFareAmount(DEFAULT_REQUESTED_FARE_AMOUNT)
            .reason(DEFAULT_REASON)
            .requestedByUsername(DEFAULT_REQUESTED_BY_USERNAME)
            .requestedAt(DEFAULT_REQUESTED_AT)
            .status(DEFAULT_STATUS)
            .approvedByUsername(DEFAULT_APPROVED_BY_USERNAME)
            .approvedAt(DEFAULT_APPROVED_AT)
            .rejectedByUsername(DEFAULT_REJECTED_BY_USERNAME)
            .rejectedAt(DEFAULT_REJECTED_AT);
        return orderFareAdjustmentRequest;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static OrderFareAdjustmentRequest createUpdatedEntity(EntityManager em) {
        OrderFareAdjustmentRequest updatedOrderFareAdjustmentRequest = new OrderFareAdjustmentRequest()
            .requestedFareAmount(UPDATED_REQUESTED_FARE_AMOUNT)
            .reason(UPDATED_REASON)
            .requestedByUsername(UPDATED_REQUESTED_BY_USERNAME)
            .requestedAt(UPDATED_REQUESTED_AT)
            .status(UPDATED_STATUS)
            .approvedByUsername(UPDATED_APPROVED_BY_USERNAME)
            .approvedAt(UPDATED_APPROVED_AT)
            .rejectedByUsername(UPDATED_REJECTED_BY_USERNAME)
            .rejectedAt(UPDATED_REJECTED_AT);
        return updatedOrderFareAdjustmentRequest;
    }

    @BeforeEach
    public void initTest() {
        orderFareAdjustmentRequest = createEntity(em);
    }

    @AfterEach
    public void cleanup() {
        if (insertedOrderFareAdjustmentRequest != null) {
            orderFareAdjustmentRequestRepository.delete(insertedOrderFareAdjustmentRequest);
            insertedOrderFareAdjustmentRequest = null;
        }
    }

    @Test
    @Transactional
    void createOrderFareAdjustmentRequest() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the OrderFareAdjustmentRequest
        OrderFareAdjustmentRequestDTO orderFareAdjustmentRequestDTO = orderFareAdjustmentRequestMapper.toDto(orderFareAdjustmentRequest);
        var returnedOrderFareAdjustmentRequestDTO = om.readValue(
            restOrderFareAdjustmentRequestMockMvc
                .perform(
                    post(ENTITY_API_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsBytes(orderFareAdjustmentRequestDTO))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            OrderFareAdjustmentRequestDTO.class
        );

        // Validate the OrderFareAdjustmentRequest in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedOrderFareAdjustmentRequest = orderFareAdjustmentRequestMapper.toEntity(returnedOrderFareAdjustmentRequestDTO);
        assertOrderFareAdjustmentRequestUpdatableFieldsEquals(
            returnedOrderFareAdjustmentRequest,
            getPersistedOrderFareAdjustmentRequest(returnedOrderFareAdjustmentRequest)
        );

        insertedOrderFareAdjustmentRequest = returnedOrderFareAdjustmentRequest;
    }

    @Test
    @Transactional
    void createOrderFareAdjustmentRequestWithExistingId() throws Exception {
        // Create the OrderFareAdjustmentRequest with an existing ID
        orderFareAdjustmentRequest.setId(1L);
        OrderFareAdjustmentRequestDTO orderFareAdjustmentRequestDTO = orderFareAdjustmentRequestMapper.toDto(orderFareAdjustmentRequest);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restOrderFareAdjustmentRequestMockMvc
            .perform(
                post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(orderFareAdjustmentRequestDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the OrderFareAdjustmentRequest in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkRequestedFareAmountIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        orderFareAdjustmentRequest.setRequestedFareAmount(null);

        // Create the OrderFareAdjustmentRequest, which fails.
        OrderFareAdjustmentRequestDTO orderFareAdjustmentRequestDTO = orderFareAdjustmentRequestMapper.toDto(orderFareAdjustmentRequest);

        restOrderFareAdjustmentRequestMockMvc
            .perform(
                post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(orderFareAdjustmentRequestDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkReasonIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        orderFareAdjustmentRequest.setReason(null);

        // Create the OrderFareAdjustmentRequest, which fails.
        OrderFareAdjustmentRequestDTO orderFareAdjustmentRequestDTO = orderFareAdjustmentRequestMapper.toDto(orderFareAdjustmentRequest);

        restOrderFareAdjustmentRequestMockMvc
            .perform(
                post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(orderFareAdjustmentRequestDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkRequestedByUsernameIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        orderFareAdjustmentRequest.setRequestedByUsername(null);

        // Create the OrderFareAdjustmentRequest, which fails.
        OrderFareAdjustmentRequestDTO orderFareAdjustmentRequestDTO = orderFareAdjustmentRequestMapper.toDto(orderFareAdjustmentRequest);

        restOrderFareAdjustmentRequestMockMvc
            .perform(
                post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(orderFareAdjustmentRequestDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkRequestedAtIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        orderFareAdjustmentRequest.setRequestedAt(null);

        // Create the OrderFareAdjustmentRequest, which fails.
        OrderFareAdjustmentRequestDTO orderFareAdjustmentRequestDTO = orderFareAdjustmentRequestMapper.toDto(orderFareAdjustmentRequest);

        restOrderFareAdjustmentRequestMockMvc
            .perform(
                post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(orderFareAdjustmentRequestDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkStatusIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        orderFareAdjustmentRequest.setStatus(null);

        // Create the OrderFareAdjustmentRequest, which fails.
        OrderFareAdjustmentRequestDTO orderFareAdjustmentRequestDTO = orderFareAdjustmentRequestMapper.toDto(orderFareAdjustmentRequest);

        restOrderFareAdjustmentRequestMockMvc
            .perform(
                post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(orderFareAdjustmentRequestDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllOrderFareAdjustmentRequests() throws Exception {
        // Initialize the database
        insertedOrderFareAdjustmentRequest = orderFareAdjustmentRequestRepository.saveAndFlush(orderFareAdjustmentRequest);

        // Get all the orderFareAdjustmentRequestList
        restOrderFareAdjustmentRequestMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(orderFareAdjustmentRequest.getId().intValue())))
            .andExpect(jsonPath("$.[*].requestedFareAmount").value(hasItem(sameNumber(DEFAULT_REQUESTED_FARE_AMOUNT))))
            .andExpect(jsonPath("$.[*].reason").value(hasItem(DEFAULT_REASON)))
            .andExpect(jsonPath("$.[*].requestedByUsername").value(hasItem(DEFAULT_REQUESTED_BY_USERNAME)))
            .andExpect(jsonPath("$.[*].requestedAt").value(hasItem(DEFAULT_REQUESTED_AT.toString())))
            .andExpect(jsonPath("$.[*].status").value(hasItem(DEFAULT_STATUS.toString())))
            .andExpect(jsonPath("$.[*].approvedByUsername").value(hasItem(DEFAULT_APPROVED_BY_USERNAME)))
            .andExpect(jsonPath("$.[*].approvedAt").value(hasItem(DEFAULT_APPROVED_AT.toString())))
            .andExpect(jsonPath("$.[*].rejectedByUsername").value(hasItem(DEFAULT_REJECTED_BY_USERNAME)))
            .andExpect(jsonPath("$.[*].rejectedAt").value(hasItem(DEFAULT_REJECTED_AT.toString())));
    }

    @Test
    @Transactional
    void getOrderFareAdjustmentRequest() throws Exception {
        // Initialize the database
        insertedOrderFareAdjustmentRequest = orderFareAdjustmentRequestRepository.saveAndFlush(orderFareAdjustmentRequest);

        // Get the orderFareAdjustmentRequest
        restOrderFareAdjustmentRequestMockMvc
            .perform(get(ENTITY_API_URL_ID, orderFareAdjustmentRequest.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(orderFareAdjustmentRequest.getId().intValue()))
            .andExpect(jsonPath("$.requestedFareAmount").value(sameNumber(DEFAULT_REQUESTED_FARE_AMOUNT)))
            .andExpect(jsonPath("$.reason").value(DEFAULT_REASON))
            .andExpect(jsonPath("$.requestedByUsername").value(DEFAULT_REQUESTED_BY_USERNAME))
            .andExpect(jsonPath("$.requestedAt").value(DEFAULT_REQUESTED_AT.toString()))
            .andExpect(jsonPath("$.status").value(DEFAULT_STATUS.toString()))
            .andExpect(jsonPath("$.approvedByUsername").value(DEFAULT_APPROVED_BY_USERNAME))
            .andExpect(jsonPath("$.approvedAt").value(DEFAULT_APPROVED_AT.toString()))
            .andExpect(jsonPath("$.rejectedByUsername").value(DEFAULT_REJECTED_BY_USERNAME))
            .andExpect(jsonPath("$.rejectedAt").value(DEFAULT_REJECTED_AT.toString()));
    }

    @Test
    @Transactional
    void getNonExistingOrderFareAdjustmentRequest() throws Exception {
        // Get the orderFareAdjustmentRequest
        restOrderFareAdjustmentRequestMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingOrderFareAdjustmentRequest() throws Exception {
        // Initialize the database
        insertedOrderFareAdjustmentRequest = orderFareAdjustmentRequestRepository.saveAndFlush(orderFareAdjustmentRequest);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the orderFareAdjustmentRequest
        OrderFareAdjustmentRequest updatedOrderFareAdjustmentRequest = orderFareAdjustmentRequestRepository
            .findById(orderFareAdjustmentRequest.getId())
            .orElseThrow();
        // Disconnect from session so that the updates on updatedOrderFareAdjustmentRequest are not directly saved in db
        em.detach(updatedOrderFareAdjustmentRequest);
        updatedOrderFareAdjustmentRequest
            .requestedFareAmount(UPDATED_REQUESTED_FARE_AMOUNT)
            .reason(UPDATED_REASON)
            .requestedByUsername(UPDATED_REQUESTED_BY_USERNAME)
            .requestedAt(UPDATED_REQUESTED_AT)
            .status(UPDATED_STATUS)
            .approvedByUsername(UPDATED_APPROVED_BY_USERNAME)
            .approvedAt(UPDATED_APPROVED_AT)
            .rejectedByUsername(UPDATED_REJECTED_BY_USERNAME)
            .rejectedAt(UPDATED_REJECTED_AT);
        OrderFareAdjustmentRequestDTO orderFareAdjustmentRequestDTO = orderFareAdjustmentRequestMapper.toDto(
            updatedOrderFareAdjustmentRequest
        );

        restOrderFareAdjustmentRequestMockMvc
            .perform(
                put(ENTITY_API_URL_ID, orderFareAdjustmentRequestDTO.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(orderFareAdjustmentRequestDTO))
            )
            .andExpect(status().isOk());

        // Validate the OrderFareAdjustmentRequest in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedOrderFareAdjustmentRequestToMatchAllProperties(updatedOrderFareAdjustmentRequest);
    }

    @Test
    @Transactional
    void putNonExistingOrderFareAdjustmentRequest() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        orderFareAdjustmentRequest.setId(longCount.incrementAndGet());

        // Create the OrderFareAdjustmentRequest
        OrderFareAdjustmentRequestDTO orderFareAdjustmentRequestDTO = orderFareAdjustmentRequestMapper.toDto(orderFareAdjustmentRequest);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restOrderFareAdjustmentRequestMockMvc
            .perform(
                put(ENTITY_API_URL_ID, orderFareAdjustmentRequestDTO.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(orderFareAdjustmentRequestDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the OrderFareAdjustmentRequest in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchOrderFareAdjustmentRequest() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        orderFareAdjustmentRequest.setId(longCount.incrementAndGet());

        // Create the OrderFareAdjustmentRequest
        OrderFareAdjustmentRequestDTO orderFareAdjustmentRequestDTO = orderFareAdjustmentRequestMapper.toDto(orderFareAdjustmentRequest);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restOrderFareAdjustmentRequestMockMvc
            .perform(
                put(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(orderFareAdjustmentRequestDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the OrderFareAdjustmentRequest in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamOrderFareAdjustmentRequest() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        orderFareAdjustmentRequest.setId(longCount.incrementAndGet());

        // Create the OrderFareAdjustmentRequest
        OrderFareAdjustmentRequestDTO orderFareAdjustmentRequestDTO = orderFareAdjustmentRequestMapper.toDto(orderFareAdjustmentRequest);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restOrderFareAdjustmentRequestMockMvc
            .perform(
                put(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(orderFareAdjustmentRequestDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the OrderFareAdjustmentRequest in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateOrderFareAdjustmentRequestWithPatch() throws Exception {
        // Initialize the database
        insertedOrderFareAdjustmentRequest = orderFareAdjustmentRequestRepository.saveAndFlush(orderFareAdjustmentRequest);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the orderFareAdjustmentRequest using partial update
        OrderFareAdjustmentRequest partialUpdatedOrderFareAdjustmentRequest = new OrderFareAdjustmentRequest();
        partialUpdatedOrderFareAdjustmentRequest.setId(orderFareAdjustmentRequest.getId());

        partialUpdatedOrderFareAdjustmentRequest
            .requestedFareAmount(UPDATED_REQUESTED_FARE_AMOUNT)
            .requestedAt(UPDATED_REQUESTED_AT)
            .approvedByUsername(UPDATED_APPROVED_BY_USERNAME);

        restOrderFareAdjustmentRequestMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedOrderFareAdjustmentRequest.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedOrderFareAdjustmentRequest))
            )
            .andExpect(status().isOk());

        // Validate the OrderFareAdjustmentRequest in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertOrderFareAdjustmentRequestUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedOrderFareAdjustmentRequest, orderFareAdjustmentRequest),
            getPersistedOrderFareAdjustmentRequest(orderFareAdjustmentRequest)
        );
    }

    @Test
    @Transactional
    void fullUpdateOrderFareAdjustmentRequestWithPatch() throws Exception {
        // Initialize the database
        insertedOrderFareAdjustmentRequest = orderFareAdjustmentRequestRepository.saveAndFlush(orderFareAdjustmentRequest);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the orderFareAdjustmentRequest using partial update
        OrderFareAdjustmentRequest partialUpdatedOrderFareAdjustmentRequest = new OrderFareAdjustmentRequest();
        partialUpdatedOrderFareAdjustmentRequest.setId(orderFareAdjustmentRequest.getId());

        partialUpdatedOrderFareAdjustmentRequest
            .requestedFareAmount(UPDATED_REQUESTED_FARE_AMOUNT)
            .reason(UPDATED_REASON)
            .requestedByUsername(UPDATED_REQUESTED_BY_USERNAME)
            .requestedAt(UPDATED_REQUESTED_AT)
            .status(UPDATED_STATUS)
            .approvedByUsername(UPDATED_APPROVED_BY_USERNAME)
            .approvedAt(UPDATED_APPROVED_AT)
            .rejectedByUsername(UPDATED_REJECTED_BY_USERNAME)
            .rejectedAt(UPDATED_REJECTED_AT);

        restOrderFareAdjustmentRequestMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedOrderFareAdjustmentRequest.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedOrderFareAdjustmentRequest))
            )
            .andExpect(status().isOk());

        // Validate the OrderFareAdjustmentRequest in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertOrderFareAdjustmentRequestUpdatableFieldsEquals(
            partialUpdatedOrderFareAdjustmentRequest,
            getPersistedOrderFareAdjustmentRequest(partialUpdatedOrderFareAdjustmentRequest)
        );
    }

    @Test
    @Transactional
    void patchNonExistingOrderFareAdjustmentRequest() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        orderFareAdjustmentRequest.setId(longCount.incrementAndGet());

        // Create the OrderFareAdjustmentRequest
        OrderFareAdjustmentRequestDTO orderFareAdjustmentRequestDTO = orderFareAdjustmentRequestMapper.toDto(orderFareAdjustmentRequest);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restOrderFareAdjustmentRequestMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, orderFareAdjustmentRequestDTO.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(orderFareAdjustmentRequestDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the OrderFareAdjustmentRequest in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchOrderFareAdjustmentRequest() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        orderFareAdjustmentRequest.setId(longCount.incrementAndGet());

        // Create the OrderFareAdjustmentRequest
        OrderFareAdjustmentRequestDTO orderFareAdjustmentRequestDTO = orderFareAdjustmentRequestMapper.toDto(orderFareAdjustmentRequest);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restOrderFareAdjustmentRequestMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(orderFareAdjustmentRequestDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the OrderFareAdjustmentRequest in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamOrderFareAdjustmentRequest() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        orderFareAdjustmentRequest.setId(longCount.incrementAndGet());

        // Create the OrderFareAdjustmentRequest
        OrderFareAdjustmentRequestDTO orderFareAdjustmentRequestDTO = orderFareAdjustmentRequestMapper.toDto(orderFareAdjustmentRequest);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restOrderFareAdjustmentRequestMockMvc
            .perform(
                patch(ENTITY_API_URL)
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(orderFareAdjustmentRequestDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the OrderFareAdjustmentRequest in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteOrderFareAdjustmentRequest() throws Exception {
        // Initialize the database
        insertedOrderFareAdjustmentRequest = orderFareAdjustmentRequestRepository.saveAndFlush(orderFareAdjustmentRequest);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the orderFareAdjustmentRequest
        restOrderFareAdjustmentRequestMockMvc
            .perform(delete(ENTITY_API_URL_ID, orderFareAdjustmentRequest.getId()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return orderFareAdjustmentRequestRepository.count();
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

    protected OrderFareAdjustmentRequest getPersistedOrderFareAdjustmentRequest(OrderFareAdjustmentRequest orderFareAdjustmentRequest) {
        return orderFareAdjustmentRequestRepository.findById(orderFareAdjustmentRequest.getId()).orElseThrow();
    }

    protected void assertPersistedOrderFareAdjustmentRequestToMatchAllProperties(
        OrderFareAdjustmentRequest expectedOrderFareAdjustmentRequest
    ) {
        assertOrderFareAdjustmentRequestAllPropertiesEquals(
            expectedOrderFareAdjustmentRequest,
            getPersistedOrderFareAdjustmentRequest(expectedOrderFareAdjustmentRequest)
        );
    }

    protected void assertPersistedOrderFareAdjustmentRequestToMatchUpdatableProperties(
        OrderFareAdjustmentRequest expectedOrderFareAdjustmentRequest
    ) {
        assertOrderFareAdjustmentRequestAllUpdatablePropertiesEquals(
            expectedOrderFareAdjustmentRequest,
            getPersistedOrderFareAdjustmentRequest(expectedOrderFareAdjustmentRequest)
        );
    }
}
