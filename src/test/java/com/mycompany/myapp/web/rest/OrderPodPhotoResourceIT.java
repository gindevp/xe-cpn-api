package com.mycompany.myapp.web.rest;

import static com.mycompany.myapp.domain.OrderPodPhotoAsserts.*;
import static com.mycompany.myapp.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.myapp.IntegrationTest;
import com.mycompany.myapp.domain.OrderPodPhoto;
import com.mycompany.myapp.domain.ShipmentOrder;
import com.mycompany.myapp.repository.OrderPodPhotoRepository;
import com.mycompany.myapp.service.OrderPodPhotoService;
import com.mycompany.myapp.service.dto.OrderPodPhotoDTO;
import com.mycompany.myapp.service.mapper.OrderPodPhotoMapper;
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
 * Integration tests for the {@link OrderPodPhotoResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser
class OrderPodPhotoResourceIT {

    private static final String DEFAULT_PHOTO_URL = "AAAAAAAAAA";
    private static final String UPDATED_PHOTO_URL = "BBBBBBBBBB";

    private static final Instant DEFAULT_CAPTURED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_CAPTURED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final String DEFAULT_CAPTURED_BY_USERNAME = "AAAAAAAAAA";
    private static final String UPDATED_CAPTURED_BY_USERNAME = "BBBBBBBBBB";

    private static final Integer DEFAULT_SEQUENCE_NO = 1;
    private static final Integer UPDATED_SEQUENCE_NO = 2;

    private static final String ENTITY_API_URL = "/api/order-pod-photos";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static Random random = new Random();
    private static AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    @Autowired
    private ObjectMapper om;

    @Autowired
    private OrderPodPhotoRepository orderPodPhotoRepository;

    @Mock
    private OrderPodPhotoRepository orderPodPhotoRepositoryMock;

    @Autowired
    private OrderPodPhotoMapper orderPodPhotoMapper;

    @Mock
    private OrderPodPhotoService orderPodPhotoServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restOrderPodPhotoMockMvc;

    private OrderPodPhoto orderPodPhoto;

    private OrderPodPhoto insertedOrderPodPhoto;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static OrderPodPhoto createEntity(EntityManager em) {
        OrderPodPhoto orderPodPhoto = new OrderPodPhoto()
            .photoUrl(DEFAULT_PHOTO_URL)
            .capturedAt(DEFAULT_CAPTURED_AT)
            .capturedByUsername(DEFAULT_CAPTURED_BY_USERNAME)
            .sequenceNo(DEFAULT_SEQUENCE_NO);
        // Add required entity
        ShipmentOrder shipmentOrder;
        if (TestUtil.findAll(em, ShipmentOrder.class).isEmpty()) {
            shipmentOrder = ShipmentOrderResourceIT.createEntity(em);
            em.persist(shipmentOrder);
            em.flush();
        } else {
            shipmentOrder = TestUtil.findAll(em, ShipmentOrder.class).get(0);
        }
        orderPodPhoto.setOrder(shipmentOrder);
        return orderPodPhoto;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static OrderPodPhoto createUpdatedEntity(EntityManager em) {
        OrderPodPhoto updatedOrderPodPhoto = new OrderPodPhoto()
            .photoUrl(UPDATED_PHOTO_URL)
            .capturedAt(UPDATED_CAPTURED_AT)
            .capturedByUsername(UPDATED_CAPTURED_BY_USERNAME)
            .sequenceNo(UPDATED_SEQUENCE_NO);
        // Add required entity
        ShipmentOrder shipmentOrder;
        if (TestUtil.findAll(em, ShipmentOrder.class).isEmpty()) {
            shipmentOrder = ShipmentOrderResourceIT.createUpdatedEntity(em);
            em.persist(shipmentOrder);
            em.flush();
        } else {
            shipmentOrder = TestUtil.findAll(em, ShipmentOrder.class).get(0);
        }
        updatedOrderPodPhoto.setOrder(shipmentOrder);
        return updatedOrderPodPhoto;
    }

    @BeforeEach
    public void initTest() {
        orderPodPhoto = createEntity(em);
    }

    @AfterEach
    public void cleanup() {
        if (insertedOrderPodPhoto != null) {
            orderPodPhotoRepository.delete(insertedOrderPodPhoto);
            insertedOrderPodPhoto = null;
        }
    }

    @Test
    @Transactional
    void createOrderPodPhoto() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the OrderPodPhoto
        OrderPodPhotoDTO orderPodPhotoDTO = orderPodPhotoMapper.toDto(orderPodPhoto);
        var returnedOrderPodPhotoDTO = om.readValue(
            restOrderPodPhotoMockMvc
                .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(orderPodPhotoDTO)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            OrderPodPhotoDTO.class
        );

        // Validate the OrderPodPhoto in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedOrderPodPhoto = orderPodPhotoMapper.toEntity(returnedOrderPodPhotoDTO);
        assertOrderPodPhotoUpdatableFieldsEquals(returnedOrderPodPhoto, getPersistedOrderPodPhoto(returnedOrderPodPhoto));

        insertedOrderPodPhoto = returnedOrderPodPhoto;
    }

    @Test
    @Transactional
    void createOrderPodPhotoWithExistingId() throws Exception {
        // Create the OrderPodPhoto with an existing ID
        orderPodPhoto.setId(1L);
        OrderPodPhotoDTO orderPodPhotoDTO = orderPodPhotoMapper.toDto(orderPodPhoto);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restOrderPodPhotoMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(orderPodPhotoDTO)))
            .andExpect(status().isBadRequest());

        // Validate the OrderPodPhoto in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkPhotoUrlIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        orderPodPhoto.setPhotoUrl(null);

        // Create the OrderPodPhoto, which fails.
        OrderPodPhotoDTO orderPodPhotoDTO = orderPodPhotoMapper.toDto(orderPodPhoto);

        restOrderPodPhotoMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(orderPodPhotoDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkCapturedAtIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        orderPodPhoto.setCapturedAt(null);

        // Create the OrderPodPhoto, which fails.
        OrderPodPhotoDTO orderPodPhotoDTO = orderPodPhotoMapper.toDto(orderPodPhoto);

        restOrderPodPhotoMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(orderPodPhotoDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkCapturedByUsernameIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        orderPodPhoto.setCapturedByUsername(null);

        // Create the OrderPodPhoto, which fails.
        OrderPodPhotoDTO orderPodPhotoDTO = orderPodPhotoMapper.toDto(orderPodPhoto);

        restOrderPodPhotoMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(orderPodPhotoDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllOrderPodPhotos() throws Exception {
        // Initialize the database
        insertedOrderPodPhoto = orderPodPhotoRepository.saveAndFlush(orderPodPhoto);

        // Get all the orderPodPhotoList
        restOrderPodPhotoMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(orderPodPhoto.getId().intValue())))
            .andExpect(jsonPath("$.[*].photoUrl").value(hasItem(DEFAULT_PHOTO_URL)))
            .andExpect(jsonPath("$.[*].capturedAt").value(hasItem(DEFAULT_CAPTURED_AT.toString())))
            .andExpect(jsonPath("$.[*].capturedByUsername").value(hasItem(DEFAULT_CAPTURED_BY_USERNAME)))
            .andExpect(jsonPath("$.[*].sequenceNo").value(hasItem(DEFAULT_SEQUENCE_NO)));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllOrderPodPhotosWithEagerRelationshipsIsEnabled() throws Exception {
        when(orderPodPhotoServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restOrderPodPhotoMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(orderPodPhotoServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllOrderPodPhotosWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(orderPodPhotoServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restOrderPodPhotoMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(orderPodPhotoRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getOrderPodPhoto() throws Exception {
        // Initialize the database
        insertedOrderPodPhoto = orderPodPhotoRepository.saveAndFlush(orderPodPhoto);

        // Get the orderPodPhoto
        restOrderPodPhotoMockMvc
            .perform(get(ENTITY_API_URL_ID, orderPodPhoto.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(orderPodPhoto.getId().intValue()))
            .andExpect(jsonPath("$.photoUrl").value(DEFAULT_PHOTO_URL))
            .andExpect(jsonPath("$.capturedAt").value(DEFAULT_CAPTURED_AT.toString()))
            .andExpect(jsonPath("$.capturedByUsername").value(DEFAULT_CAPTURED_BY_USERNAME))
            .andExpect(jsonPath("$.sequenceNo").value(DEFAULT_SEQUENCE_NO));
    }

    @Test
    @Transactional
    void getNonExistingOrderPodPhoto() throws Exception {
        // Get the orderPodPhoto
        restOrderPodPhotoMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingOrderPodPhoto() throws Exception {
        // Initialize the database
        insertedOrderPodPhoto = orderPodPhotoRepository.saveAndFlush(orderPodPhoto);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the orderPodPhoto
        OrderPodPhoto updatedOrderPodPhoto = orderPodPhotoRepository.findById(orderPodPhoto.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedOrderPodPhoto are not directly saved in db
        em.detach(updatedOrderPodPhoto);
        updatedOrderPodPhoto
            .photoUrl(UPDATED_PHOTO_URL)
            .capturedAt(UPDATED_CAPTURED_AT)
            .capturedByUsername(UPDATED_CAPTURED_BY_USERNAME)
            .sequenceNo(UPDATED_SEQUENCE_NO);
        OrderPodPhotoDTO orderPodPhotoDTO = orderPodPhotoMapper.toDto(updatedOrderPodPhoto);

        restOrderPodPhotoMockMvc
            .perform(
                put(ENTITY_API_URL_ID, orderPodPhotoDTO.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(orderPodPhotoDTO))
            )
            .andExpect(status().isOk());

        // Validate the OrderPodPhoto in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedOrderPodPhotoToMatchAllProperties(updatedOrderPodPhoto);
    }

    @Test
    @Transactional
    void putNonExistingOrderPodPhoto() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        orderPodPhoto.setId(longCount.incrementAndGet());

        // Create the OrderPodPhoto
        OrderPodPhotoDTO orderPodPhotoDTO = orderPodPhotoMapper.toDto(orderPodPhoto);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restOrderPodPhotoMockMvc
            .perform(
                put(ENTITY_API_URL_ID, orderPodPhotoDTO.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(orderPodPhotoDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the OrderPodPhoto in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchOrderPodPhoto() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        orderPodPhoto.setId(longCount.incrementAndGet());

        // Create the OrderPodPhoto
        OrderPodPhotoDTO orderPodPhotoDTO = orderPodPhotoMapper.toDto(orderPodPhoto);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restOrderPodPhotoMockMvc
            .perform(
                put(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(orderPodPhotoDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the OrderPodPhoto in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamOrderPodPhoto() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        orderPodPhoto.setId(longCount.incrementAndGet());

        // Create the OrderPodPhoto
        OrderPodPhotoDTO orderPodPhotoDTO = orderPodPhotoMapper.toDto(orderPodPhoto);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restOrderPodPhotoMockMvc
            .perform(put(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(orderPodPhotoDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the OrderPodPhoto in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateOrderPodPhotoWithPatch() throws Exception {
        // Initialize the database
        insertedOrderPodPhoto = orderPodPhotoRepository.saveAndFlush(orderPodPhoto);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the orderPodPhoto using partial update
        OrderPodPhoto partialUpdatedOrderPodPhoto = new OrderPodPhoto();
        partialUpdatedOrderPodPhoto.setId(orderPodPhoto.getId());

        partialUpdatedOrderPodPhoto.photoUrl(UPDATED_PHOTO_URL);

        restOrderPodPhotoMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedOrderPodPhoto.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedOrderPodPhoto))
            )
            .andExpect(status().isOk());

        // Validate the OrderPodPhoto in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertOrderPodPhotoUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedOrderPodPhoto, orderPodPhoto),
            getPersistedOrderPodPhoto(orderPodPhoto)
        );
    }

    @Test
    @Transactional
    void fullUpdateOrderPodPhotoWithPatch() throws Exception {
        // Initialize the database
        insertedOrderPodPhoto = orderPodPhotoRepository.saveAndFlush(orderPodPhoto);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the orderPodPhoto using partial update
        OrderPodPhoto partialUpdatedOrderPodPhoto = new OrderPodPhoto();
        partialUpdatedOrderPodPhoto.setId(orderPodPhoto.getId());

        partialUpdatedOrderPodPhoto
            .photoUrl(UPDATED_PHOTO_URL)
            .capturedAt(UPDATED_CAPTURED_AT)
            .capturedByUsername(UPDATED_CAPTURED_BY_USERNAME)
            .sequenceNo(UPDATED_SEQUENCE_NO);

        restOrderPodPhotoMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedOrderPodPhoto.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedOrderPodPhoto))
            )
            .andExpect(status().isOk());

        // Validate the OrderPodPhoto in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertOrderPodPhotoUpdatableFieldsEquals(partialUpdatedOrderPodPhoto, getPersistedOrderPodPhoto(partialUpdatedOrderPodPhoto));
    }

    @Test
    @Transactional
    void patchNonExistingOrderPodPhoto() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        orderPodPhoto.setId(longCount.incrementAndGet());

        // Create the OrderPodPhoto
        OrderPodPhotoDTO orderPodPhotoDTO = orderPodPhotoMapper.toDto(orderPodPhoto);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restOrderPodPhotoMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, orderPodPhotoDTO.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(orderPodPhotoDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the OrderPodPhoto in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchOrderPodPhoto() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        orderPodPhoto.setId(longCount.incrementAndGet());

        // Create the OrderPodPhoto
        OrderPodPhotoDTO orderPodPhotoDTO = orderPodPhotoMapper.toDto(orderPodPhoto);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restOrderPodPhotoMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(orderPodPhotoDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the OrderPodPhoto in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamOrderPodPhoto() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        orderPodPhoto.setId(longCount.incrementAndGet());

        // Create the OrderPodPhoto
        OrderPodPhotoDTO orderPodPhotoDTO = orderPodPhotoMapper.toDto(orderPodPhoto);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restOrderPodPhotoMockMvc
            .perform(patch(ENTITY_API_URL).contentType("application/merge-patch+json").content(om.writeValueAsBytes(orderPodPhotoDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the OrderPodPhoto in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteOrderPodPhoto() throws Exception {
        // Initialize the database
        insertedOrderPodPhoto = orderPodPhotoRepository.saveAndFlush(orderPodPhoto);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the orderPodPhoto
        restOrderPodPhotoMockMvc
            .perform(delete(ENTITY_API_URL_ID, orderPodPhoto.getId()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return orderPodPhotoRepository.count();
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

    protected OrderPodPhoto getPersistedOrderPodPhoto(OrderPodPhoto orderPodPhoto) {
        return orderPodPhotoRepository.findById(orderPodPhoto.getId()).orElseThrow();
    }

    protected void assertPersistedOrderPodPhotoToMatchAllProperties(OrderPodPhoto expectedOrderPodPhoto) {
        assertOrderPodPhotoAllPropertiesEquals(expectedOrderPodPhoto, getPersistedOrderPodPhoto(expectedOrderPodPhoto));
    }

    protected void assertPersistedOrderPodPhotoToMatchUpdatableProperties(OrderPodPhoto expectedOrderPodPhoto) {
        assertOrderPodPhotoAllUpdatablePropertiesEquals(expectedOrderPodPhoto, getPersistedOrderPodPhoto(expectedOrderPodPhoto));
    }
}
