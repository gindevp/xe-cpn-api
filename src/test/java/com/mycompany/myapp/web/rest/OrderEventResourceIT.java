package com.mycompany.myapp.web.rest;

import static com.mycompany.myapp.domain.OrderEventAsserts.*;
import static com.mycompany.myapp.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.myapp.IntegrationTest;
import com.mycompany.myapp.domain.OrderEvent;
import com.mycompany.myapp.domain.ShipmentOrder;
import com.mycompany.myapp.repository.OrderEventRepository;
import com.mycompany.myapp.service.OrderEventService;
import com.mycompany.myapp.service.dto.OrderEventDTO;
import com.mycompany.myapp.service.mapper.OrderEventMapper;
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
 * Integration tests for the {@link OrderEventResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser
class OrderEventResourceIT {

    private static final Instant DEFAULT_EVENT_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_EVENT_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final String DEFAULT_ACTION = "AAAAAAAAAA";
    private static final String UPDATED_ACTION = "BBBBBBBBBB";

    private static final String DEFAULT_DETAIL = "AAAAAAAAAA";
    private static final String UPDATED_DETAIL = "BBBBBBBBBB";

    private static final String DEFAULT_ACTOR_USERNAME = "AAAAAAAAAA";
    private static final String UPDATED_ACTOR_USERNAME = "BBBBBBBBBB";

    private static final String ENTITY_API_URL = "/api/order-events";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static Random random = new Random();
    private static AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    @Autowired
    private ObjectMapper om;

    @Autowired
    private OrderEventRepository orderEventRepository;

    @Mock
    private OrderEventRepository orderEventRepositoryMock;

    @Autowired
    private OrderEventMapper orderEventMapper;

    @Mock
    private OrderEventService orderEventServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restOrderEventMockMvc;

    private OrderEvent orderEvent;

    private OrderEvent insertedOrderEvent;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static OrderEvent createEntity(EntityManager em) {
        OrderEvent orderEvent = new OrderEvent()
            .eventAt(DEFAULT_EVENT_AT)
            .action(DEFAULT_ACTION)
            .detail(DEFAULT_DETAIL)
            .actorUsername(DEFAULT_ACTOR_USERNAME);
        // Add required entity
        ShipmentOrder shipmentOrder;
        if (TestUtil.findAll(em, ShipmentOrder.class).isEmpty()) {
            shipmentOrder = ShipmentOrderResourceIT.createEntity(em);
            em.persist(shipmentOrder);
            em.flush();
        } else {
            shipmentOrder = TestUtil.findAll(em, ShipmentOrder.class).get(0);
        }
        orderEvent.setOrder(shipmentOrder);
        return orderEvent;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static OrderEvent createUpdatedEntity(EntityManager em) {
        OrderEvent updatedOrderEvent = new OrderEvent()
            .eventAt(UPDATED_EVENT_AT)
            .action(UPDATED_ACTION)
            .detail(UPDATED_DETAIL)
            .actorUsername(UPDATED_ACTOR_USERNAME);
        // Add required entity
        ShipmentOrder shipmentOrder;
        if (TestUtil.findAll(em, ShipmentOrder.class).isEmpty()) {
            shipmentOrder = ShipmentOrderResourceIT.createUpdatedEntity(em);
            em.persist(shipmentOrder);
            em.flush();
        } else {
            shipmentOrder = TestUtil.findAll(em, ShipmentOrder.class).get(0);
        }
        updatedOrderEvent.setOrder(shipmentOrder);
        return updatedOrderEvent;
    }

    @BeforeEach
    public void initTest() {
        orderEvent = createEntity(em);
    }

    @AfterEach
    public void cleanup() {
        if (insertedOrderEvent != null) {
            orderEventRepository.delete(insertedOrderEvent);
            insertedOrderEvent = null;
        }
    }

    @Test
    @Transactional
    void createOrderEvent() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the OrderEvent
        OrderEventDTO orderEventDTO = orderEventMapper.toDto(orderEvent);
        var returnedOrderEventDTO = om.readValue(
            restOrderEventMockMvc
                .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(orderEventDTO)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            OrderEventDTO.class
        );

        // Validate the OrderEvent in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedOrderEvent = orderEventMapper.toEntity(returnedOrderEventDTO);
        assertOrderEventUpdatableFieldsEquals(returnedOrderEvent, getPersistedOrderEvent(returnedOrderEvent));

        insertedOrderEvent = returnedOrderEvent;
    }

    @Test
    @Transactional
    void createOrderEventWithExistingId() throws Exception {
        // Create the OrderEvent with an existing ID
        orderEvent.setId(1L);
        OrderEventDTO orderEventDTO = orderEventMapper.toDto(orderEvent);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restOrderEventMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(orderEventDTO)))
            .andExpect(status().isBadRequest());

        // Validate the OrderEvent in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkEventAtIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        orderEvent.setEventAt(null);

        // Create the OrderEvent, which fails.
        OrderEventDTO orderEventDTO = orderEventMapper.toDto(orderEvent);

        restOrderEventMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(orderEventDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkActionIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        orderEvent.setAction(null);

        // Create the OrderEvent, which fails.
        OrderEventDTO orderEventDTO = orderEventMapper.toDto(orderEvent);

        restOrderEventMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(orderEventDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkActorUsernameIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        orderEvent.setActorUsername(null);

        // Create the OrderEvent, which fails.
        OrderEventDTO orderEventDTO = orderEventMapper.toDto(orderEvent);

        restOrderEventMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(orderEventDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllOrderEvents() throws Exception {
        // Initialize the database
        insertedOrderEvent = orderEventRepository.saveAndFlush(orderEvent);

        // Get all the orderEventList
        restOrderEventMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(orderEvent.getId().intValue())))
            .andExpect(jsonPath("$.[*].eventAt").value(hasItem(DEFAULT_EVENT_AT.toString())))
            .andExpect(jsonPath("$.[*].action").value(hasItem(DEFAULT_ACTION)))
            .andExpect(jsonPath("$.[*].detail").value(hasItem(DEFAULT_DETAIL)))
            .andExpect(jsonPath("$.[*].actorUsername").value(hasItem(DEFAULT_ACTOR_USERNAME)));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllOrderEventsWithEagerRelationshipsIsEnabled() throws Exception {
        when(orderEventServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restOrderEventMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(orderEventServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllOrderEventsWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(orderEventServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restOrderEventMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(orderEventRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getOrderEvent() throws Exception {
        // Initialize the database
        insertedOrderEvent = orderEventRepository.saveAndFlush(orderEvent);

        // Get the orderEvent
        restOrderEventMockMvc
            .perform(get(ENTITY_API_URL_ID, orderEvent.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(orderEvent.getId().intValue()))
            .andExpect(jsonPath("$.eventAt").value(DEFAULT_EVENT_AT.toString()))
            .andExpect(jsonPath("$.action").value(DEFAULT_ACTION))
            .andExpect(jsonPath("$.detail").value(DEFAULT_DETAIL))
            .andExpect(jsonPath("$.actorUsername").value(DEFAULT_ACTOR_USERNAME));
    }

    @Test
    @Transactional
    void getNonExistingOrderEvent() throws Exception {
        // Get the orderEvent
        restOrderEventMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingOrderEvent() throws Exception {
        // Initialize the database
        insertedOrderEvent = orderEventRepository.saveAndFlush(orderEvent);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the orderEvent
        OrderEvent updatedOrderEvent = orderEventRepository.findById(orderEvent.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedOrderEvent are not directly saved in db
        em.detach(updatedOrderEvent);
        updatedOrderEvent.eventAt(UPDATED_EVENT_AT).action(UPDATED_ACTION).detail(UPDATED_DETAIL).actorUsername(UPDATED_ACTOR_USERNAME);
        OrderEventDTO orderEventDTO = orderEventMapper.toDto(updatedOrderEvent);

        restOrderEventMockMvc
            .perform(
                put(ENTITY_API_URL_ID, orderEventDTO.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(orderEventDTO))
            )
            .andExpect(status().isOk());

        // Validate the OrderEvent in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedOrderEventToMatchAllProperties(updatedOrderEvent);
    }

    @Test
    @Transactional
    void putNonExistingOrderEvent() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        orderEvent.setId(longCount.incrementAndGet());

        // Create the OrderEvent
        OrderEventDTO orderEventDTO = orderEventMapper.toDto(orderEvent);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restOrderEventMockMvc
            .perform(
                put(ENTITY_API_URL_ID, orderEventDTO.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(orderEventDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the OrderEvent in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchOrderEvent() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        orderEvent.setId(longCount.incrementAndGet());

        // Create the OrderEvent
        OrderEventDTO orderEventDTO = orderEventMapper.toDto(orderEvent);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restOrderEventMockMvc
            .perform(
                put(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(orderEventDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the OrderEvent in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamOrderEvent() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        orderEvent.setId(longCount.incrementAndGet());

        // Create the OrderEvent
        OrderEventDTO orderEventDTO = orderEventMapper.toDto(orderEvent);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restOrderEventMockMvc
            .perform(put(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(orderEventDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the OrderEvent in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateOrderEventWithPatch() throws Exception {
        // Initialize the database
        insertedOrderEvent = orderEventRepository.saveAndFlush(orderEvent);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the orderEvent using partial update
        OrderEvent partialUpdatedOrderEvent = new OrderEvent();
        partialUpdatedOrderEvent.setId(orderEvent.getId());

        partialUpdatedOrderEvent.action(UPDATED_ACTION);

        restOrderEventMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedOrderEvent.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedOrderEvent))
            )
            .andExpect(status().isOk());

        // Validate the OrderEvent in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertOrderEventUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedOrderEvent, orderEvent),
            getPersistedOrderEvent(orderEvent)
        );
    }

    @Test
    @Transactional
    void fullUpdateOrderEventWithPatch() throws Exception {
        // Initialize the database
        insertedOrderEvent = orderEventRepository.saveAndFlush(orderEvent);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the orderEvent using partial update
        OrderEvent partialUpdatedOrderEvent = new OrderEvent();
        partialUpdatedOrderEvent.setId(orderEvent.getId());

        partialUpdatedOrderEvent
            .eventAt(UPDATED_EVENT_AT)
            .action(UPDATED_ACTION)
            .detail(UPDATED_DETAIL)
            .actorUsername(UPDATED_ACTOR_USERNAME);

        restOrderEventMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedOrderEvent.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedOrderEvent))
            )
            .andExpect(status().isOk());

        // Validate the OrderEvent in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertOrderEventUpdatableFieldsEquals(partialUpdatedOrderEvent, getPersistedOrderEvent(partialUpdatedOrderEvent));
    }

    @Test
    @Transactional
    void patchNonExistingOrderEvent() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        orderEvent.setId(longCount.incrementAndGet());

        // Create the OrderEvent
        OrderEventDTO orderEventDTO = orderEventMapper.toDto(orderEvent);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restOrderEventMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, orderEventDTO.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(orderEventDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the OrderEvent in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchOrderEvent() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        orderEvent.setId(longCount.incrementAndGet());

        // Create the OrderEvent
        OrderEventDTO orderEventDTO = orderEventMapper.toDto(orderEvent);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restOrderEventMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(orderEventDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the OrderEvent in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamOrderEvent() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        orderEvent.setId(longCount.incrementAndGet());

        // Create the OrderEvent
        OrderEventDTO orderEventDTO = orderEventMapper.toDto(orderEvent);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restOrderEventMockMvc
            .perform(patch(ENTITY_API_URL).contentType("application/merge-patch+json").content(om.writeValueAsBytes(orderEventDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the OrderEvent in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteOrderEvent() throws Exception {
        // Initialize the database
        insertedOrderEvent = orderEventRepository.saveAndFlush(orderEvent);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the orderEvent
        restOrderEventMockMvc
            .perform(delete(ENTITY_API_URL_ID, orderEvent.getId()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return orderEventRepository.count();
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

    protected OrderEvent getPersistedOrderEvent(OrderEvent orderEvent) {
        return orderEventRepository.findById(orderEvent.getId()).orElseThrow();
    }

    protected void assertPersistedOrderEventToMatchAllProperties(OrderEvent expectedOrderEvent) {
        assertOrderEventAllPropertiesEquals(expectedOrderEvent, getPersistedOrderEvent(expectedOrderEvent));
    }

    protected void assertPersistedOrderEventToMatchUpdatableProperties(OrderEvent expectedOrderEvent) {
        assertOrderEventAllUpdatablePropertiesEquals(expectedOrderEvent, getPersistedOrderEvent(expectedOrderEvent));
    }
}
