package com.mycompany.myapp.web.rest;

import static com.mycompany.myapp.domain.OrderReturnRequestAsserts.*;
import static com.mycompany.myapp.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.myapp.IntegrationTest;
import com.mycompany.myapp.domain.OrderReturnRequest;
import com.mycompany.myapp.domain.enumeration.ApprovalStatus;
import com.mycompany.myapp.repository.OrderReturnRequestRepository;
import com.mycompany.myapp.service.dto.OrderReturnRequestDTO;
import com.mycompany.myapp.service.mapper.OrderReturnRequestMapper;
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
 * Integration tests for the {@link OrderReturnRequestResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser
class OrderReturnRequestResourceIT {

    private static final String DEFAULT_REASON = "AAAAAAAAAA";
    private static final String UPDATED_REASON = "BBBBBBBBBB";

    private static final String DEFAULT_REQUESTED_BY_USERNAME = "AAAAAAAAAA";
    private static final String UPDATED_REQUESTED_BY_USERNAME = "BBBBBBBBBB";

    private static final Instant DEFAULT_REQUESTED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_REQUESTED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final ApprovalStatus DEFAULT_STATUS = ApprovalStatus.PENDING;
    private static final ApprovalStatus UPDATED_STATUS = ApprovalStatus.APPROVED;

    private static final String DEFAULT_DECIDED_BY_USERNAME = "AAAAAAAAAA";
    private static final String UPDATED_DECIDED_BY_USERNAME = "BBBBBBBBBB";

    private static final Instant DEFAULT_DECIDED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_DECIDED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final String DEFAULT_DECISION_NOTE = "AAAAAAAAAA";
    private static final String UPDATED_DECISION_NOTE = "BBBBBBBBBB";

    private static final String ENTITY_API_URL = "/api/order-return-requests";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static Random random = new Random();
    private static AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    @Autowired
    private ObjectMapper om;

    @Autowired
    private OrderReturnRequestRepository orderReturnRequestRepository;

    @Autowired
    private OrderReturnRequestMapper orderReturnRequestMapper;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restOrderReturnRequestMockMvc;

    private OrderReturnRequest orderReturnRequest;

    private OrderReturnRequest insertedOrderReturnRequest;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static OrderReturnRequest createEntity(EntityManager em) {
        OrderReturnRequest orderReturnRequest = new OrderReturnRequest()
            .reason(DEFAULT_REASON)
            .requestedByUsername(DEFAULT_REQUESTED_BY_USERNAME)
            .requestedAt(DEFAULT_REQUESTED_AT)
            .status(DEFAULT_STATUS)
            .decidedByUsername(DEFAULT_DECIDED_BY_USERNAME)
            .decidedAt(DEFAULT_DECIDED_AT)
            .decisionNote(DEFAULT_DECISION_NOTE);
        return orderReturnRequest;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static OrderReturnRequest createUpdatedEntity(EntityManager em) {
        OrderReturnRequest updatedOrderReturnRequest = new OrderReturnRequest()
            .reason(UPDATED_REASON)
            .requestedByUsername(UPDATED_REQUESTED_BY_USERNAME)
            .requestedAt(UPDATED_REQUESTED_AT)
            .status(UPDATED_STATUS)
            .decidedByUsername(UPDATED_DECIDED_BY_USERNAME)
            .decidedAt(UPDATED_DECIDED_AT)
            .decisionNote(UPDATED_DECISION_NOTE);
        return updatedOrderReturnRequest;
    }

    @BeforeEach
    public void initTest() {
        orderReturnRequest = createEntity(em);
    }

    @AfterEach
    public void cleanup() {
        if (insertedOrderReturnRequest != null) {
            orderReturnRequestRepository.delete(insertedOrderReturnRequest);
            insertedOrderReturnRequest = null;
        }
    }

    @Test
    @Transactional
    void createOrderReturnRequest() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the OrderReturnRequest
        OrderReturnRequestDTO orderReturnRequestDTO = orderReturnRequestMapper.toDto(orderReturnRequest);
        var returnedOrderReturnRequestDTO = om.readValue(
            restOrderReturnRequestMockMvc
                .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(orderReturnRequestDTO)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            OrderReturnRequestDTO.class
        );

        // Validate the OrderReturnRequest in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedOrderReturnRequest = orderReturnRequestMapper.toEntity(returnedOrderReturnRequestDTO);
        assertOrderReturnRequestUpdatableFieldsEquals(
            returnedOrderReturnRequest,
            getPersistedOrderReturnRequest(returnedOrderReturnRequest)
        );

        insertedOrderReturnRequest = returnedOrderReturnRequest;
    }

    @Test
    @Transactional
    void createOrderReturnRequestWithExistingId() throws Exception {
        // Create the OrderReturnRequest with an existing ID
        orderReturnRequest.setId(1L);
        OrderReturnRequestDTO orderReturnRequestDTO = orderReturnRequestMapper.toDto(orderReturnRequest);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restOrderReturnRequestMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(orderReturnRequestDTO)))
            .andExpect(status().isBadRequest());

        // Validate the OrderReturnRequest in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkReasonIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        orderReturnRequest.setReason(null);

        // Create the OrderReturnRequest, which fails.
        OrderReturnRequestDTO orderReturnRequestDTO = orderReturnRequestMapper.toDto(orderReturnRequest);

        restOrderReturnRequestMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(orderReturnRequestDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkRequestedByUsernameIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        orderReturnRequest.setRequestedByUsername(null);

        // Create the OrderReturnRequest, which fails.
        OrderReturnRequestDTO orderReturnRequestDTO = orderReturnRequestMapper.toDto(orderReturnRequest);

        restOrderReturnRequestMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(orderReturnRequestDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkRequestedAtIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        orderReturnRequest.setRequestedAt(null);

        // Create the OrderReturnRequest, which fails.
        OrderReturnRequestDTO orderReturnRequestDTO = orderReturnRequestMapper.toDto(orderReturnRequest);

        restOrderReturnRequestMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(orderReturnRequestDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkStatusIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        orderReturnRequest.setStatus(null);

        // Create the OrderReturnRequest, which fails.
        OrderReturnRequestDTO orderReturnRequestDTO = orderReturnRequestMapper.toDto(orderReturnRequest);

        restOrderReturnRequestMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(orderReturnRequestDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllOrderReturnRequests() throws Exception {
        // Initialize the database
        insertedOrderReturnRequest = orderReturnRequestRepository.saveAndFlush(orderReturnRequest);

        // Get all the orderReturnRequestList
        restOrderReturnRequestMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(orderReturnRequest.getId().intValue())))
            .andExpect(jsonPath("$.[*].reason").value(hasItem(DEFAULT_REASON)))
            .andExpect(jsonPath("$.[*].requestedByUsername").value(hasItem(DEFAULT_REQUESTED_BY_USERNAME)))
            .andExpect(jsonPath("$.[*].requestedAt").value(hasItem(DEFAULT_REQUESTED_AT.toString())))
            .andExpect(jsonPath("$.[*].status").value(hasItem(DEFAULT_STATUS.toString())))
            .andExpect(jsonPath("$.[*].decidedByUsername").value(hasItem(DEFAULT_DECIDED_BY_USERNAME)))
            .andExpect(jsonPath("$.[*].decidedAt").value(hasItem(DEFAULT_DECIDED_AT.toString())))
            .andExpect(jsonPath("$.[*].decisionNote").value(hasItem(DEFAULT_DECISION_NOTE)));
    }

    @Test
    @Transactional
    void getOrderReturnRequest() throws Exception {
        // Initialize the database
        insertedOrderReturnRequest = orderReturnRequestRepository.saveAndFlush(orderReturnRequest);

        // Get the orderReturnRequest
        restOrderReturnRequestMockMvc
            .perform(get(ENTITY_API_URL_ID, orderReturnRequest.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(orderReturnRequest.getId().intValue()))
            .andExpect(jsonPath("$.reason").value(DEFAULT_REASON))
            .andExpect(jsonPath("$.requestedByUsername").value(DEFAULT_REQUESTED_BY_USERNAME))
            .andExpect(jsonPath("$.requestedAt").value(DEFAULT_REQUESTED_AT.toString()))
            .andExpect(jsonPath("$.status").value(DEFAULT_STATUS.toString()))
            .andExpect(jsonPath("$.decidedByUsername").value(DEFAULT_DECIDED_BY_USERNAME))
            .andExpect(jsonPath("$.decidedAt").value(DEFAULT_DECIDED_AT.toString()))
            .andExpect(jsonPath("$.decisionNote").value(DEFAULT_DECISION_NOTE));
    }

    @Test
    @Transactional
    void getNonExistingOrderReturnRequest() throws Exception {
        // Get the orderReturnRequest
        restOrderReturnRequestMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingOrderReturnRequest() throws Exception {
        // Initialize the database
        insertedOrderReturnRequest = orderReturnRequestRepository.saveAndFlush(orderReturnRequest);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the orderReturnRequest
        OrderReturnRequest updatedOrderReturnRequest = orderReturnRequestRepository.findById(orderReturnRequest.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedOrderReturnRequest are not directly saved in db
        em.detach(updatedOrderReturnRequest);
        updatedOrderReturnRequest
            .reason(UPDATED_REASON)
            .requestedByUsername(UPDATED_REQUESTED_BY_USERNAME)
            .requestedAt(UPDATED_REQUESTED_AT)
            .status(UPDATED_STATUS)
            .decidedByUsername(UPDATED_DECIDED_BY_USERNAME)
            .decidedAt(UPDATED_DECIDED_AT)
            .decisionNote(UPDATED_DECISION_NOTE);
        OrderReturnRequestDTO orderReturnRequestDTO = orderReturnRequestMapper.toDto(updatedOrderReturnRequest);

        restOrderReturnRequestMockMvc
            .perform(
                put(ENTITY_API_URL_ID, orderReturnRequestDTO.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(orderReturnRequestDTO))
            )
            .andExpect(status().isOk());

        // Validate the OrderReturnRequest in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedOrderReturnRequestToMatchAllProperties(updatedOrderReturnRequest);
    }

    @Test
    @Transactional
    void putNonExistingOrderReturnRequest() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        orderReturnRequest.setId(longCount.incrementAndGet());

        // Create the OrderReturnRequest
        OrderReturnRequestDTO orderReturnRequestDTO = orderReturnRequestMapper.toDto(orderReturnRequest);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restOrderReturnRequestMockMvc
            .perform(
                put(ENTITY_API_URL_ID, orderReturnRequestDTO.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(orderReturnRequestDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the OrderReturnRequest in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchOrderReturnRequest() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        orderReturnRequest.setId(longCount.incrementAndGet());

        // Create the OrderReturnRequest
        OrderReturnRequestDTO orderReturnRequestDTO = orderReturnRequestMapper.toDto(orderReturnRequest);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restOrderReturnRequestMockMvc
            .perform(
                put(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(orderReturnRequestDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the OrderReturnRequest in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamOrderReturnRequest() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        orderReturnRequest.setId(longCount.incrementAndGet());

        // Create the OrderReturnRequest
        OrderReturnRequestDTO orderReturnRequestDTO = orderReturnRequestMapper.toDto(orderReturnRequest);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restOrderReturnRequestMockMvc
            .perform(put(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(orderReturnRequestDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the OrderReturnRequest in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateOrderReturnRequestWithPatch() throws Exception {
        // Initialize the database
        insertedOrderReturnRequest = orderReturnRequestRepository.saveAndFlush(orderReturnRequest);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the orderReturnRequest using partial update
        OrderReturnRequest partialUpdatedOrderReturnRequest = new OrderReturnRequest();
        partialUpdatedOrderReturnRequest.setId(orderReturnRequest.getId());

        partialUpdatedOrderReturnRequest
            .requestedByUsername(UPDATED_REQUESTED_BY_USERNAME)
            .decidedAt(UPDATED_DECIDED_AT)
            .decisionNote(UPDATED_DECISION_NOTE);

        restOrderReturnRequestMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedOrderReturnRequest.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedOrderReturnRequest))
            )
            .andExpect(status().isOk());

        // Validate the OrderReturnRequest in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertOrderReturnRequestUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedOrderReturnRequest, orderReturnRequest),
            getPersistedOrderReturnRequest(orderReturnRequest)
        );
    }

    @Test
    @Transactional
    void fullUpdateOrderReturnRequestWithPatch() throws Exception {
        // Initialize the database
        insertedOrderReturnRequest = orderReturnRequestRepository.saveAndFlush(orderReturnRequest);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the orderReturnRequest using partial update
        OrderReturnRequest partialUpdatedOrderReturnRequest = new OrderReturnRequest();
        partialUpdatedOrderReturnRequest.setId(orderReturnRequest.getId());

        partialUpdatedOrderReturnRequest
            .reason(UPDATED_REASON)
            .requestedByUsername(UPDATED_REQUESTED_BY_USERNAME)
            .requestedAt(UPDATED_REQUESTED_AT)
            .status(UPDATED_STATUS)
            .decidedByUsername(UPDATED_DECIDED_BY_USERNAME)
            .decidedAt(UPDATED_DECIDED_AT)
            .decisionNote(UPDATED_DECISION_NOTE);

        restOrderReturnRequestMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedOrderReturnRequest.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedOrderReturnRequest))
            )
            .andExpect(status().isOk());

        // Validate the OrderReturnRequest in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertOrderReturnRequestUpdatableFieldsEquals(
            partialUpdatedOrderReturnRequest,
            getPersistedOrderReturnRequest(partialUpdatedOrderReturnRequest)
        );
    }

    @Test
    @Transactional
    void patchNonExistingOrderReturnRequest() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        orderReturnRequest.setId(longCount.incrementAndGet());

        // Create the OrderReturnRequest
        OrderReturnRequestDTO orderReturnRequestDTO = orderReturnRequestMapper.toDto(orderReturnRequest);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restOrderReturnRequestMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, orderReturnRequestDTO.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(orderReturnRequestDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the OrderReturnRequest in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchOrderReturnRequest() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        orderReturnRequest.setId(longCount.incrementAndGet());

        // Create the OrderReturnRequest
        OrderReturnRequestDTO orderReturnRequestDTO = orderReturnRequestMapper.toDto(orderReturnRequest);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restOrderReturnRequestMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(orderReturnRequestDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the OrderReturnRequest in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamOrderReturnRequest() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        orderReturnRequest.setId(longCount.incrementAndGet());

        // Create the OrderReturnRequest
        OrderReturnRequestDTO orderReturnRequestDTO = orderReturnRequestMapper.toDto(orderReturnRequest);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restOrderReturnRequestMockMvc
            .perform(patch(ENTITY_API_URL).contentType("application/merge-patch+json").content(om.writeValueAsBytes(orderReturnRequestDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the OrderReturnRequest in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteOrderReturnRequest() throws Exception {
        // Initialize the database
        insertedOrderReturnRequest = orderReturnRequestRepository.saveAndFlush(orderReturnRequest);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the orderReturnRequest
        restOrderReturnRequestMockMvc
            .perform(delete(ENTITY_API_URL_ID, orderReturnRequest.getId()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return orderReturnRequestRepository.count();
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

    protected OrderReturnRequest getPersistedOrderReturnRequest(OrderReturnRequest orderReturnRequest) {
        return orderReturnRequestRepository.findById(orderReturnRequest.getId()).orElseThrow();
    }

    protected void assertPersistedOrderReturnRequestToMatchAllProperties(OrderReturnRequest expectedOrderReturnRequest) {
        assertOrderReturnRequestAllPropertiesEquals(expectedOrderReturnRequest, getPersistedOrderReturnRequest(expectedOrderReturnRequest));
    }

    protected void assertPersistedOrderReturnRequestToMatchUpdatableProperties(OrderReturnRequest expectedOrderReturnRequest) {
        assertOrderReturnRequestAllUpdatablePropertiesEquals(
            expectedOrderReturnRequest,
            getPersistedOrderReturnRequest(expectedOrderReturnRequest)
        );
    }
}
