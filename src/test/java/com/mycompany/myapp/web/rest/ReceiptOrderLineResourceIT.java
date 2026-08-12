package com.mycompany.myapp.web.rest;

import static com.mycompany.myapp.domain.ReceiptOrderLineAsserts.*;
import static com.mycompany.myapp.web.rest.TestUtil.createUpdateProxyForBean;
import static com.mycompany.myapp.web.rest.TestUtil.sameNumber;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.myapp.IntegrationTest;
import com.mycompany.myapp.domain.Receipt;
import com.mycompany.myapp.domain.ReceiptOrderLine;
import com.mycompany.myapp.domain.ShipmentOrder;
import com.mycompany.myapp.repository.ReceiptOrderLineRepository;
import com.mycompany.myapp.service.ReceiptOrderLineService;
import com.mycompany.myapp.service.dto.ReceiptOrderLineDTO;
import com.mycompany.myapp.service.mapper.ReceiptOrderLineMapper;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
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
 * Integration tests for the {@link ReceiptOrderLineResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser
class ReceiptOrderLineResourceIT {

    private static final BigDecimal DEFAULT_AMOUNT_COLLECTED = new BigDecimal(0);
    private static final BigDecimal UPDATED_AMOUNT_COLLECTED = new BigDecimal(1);

    private static final String ENTITY_API_URL = "/api/receipt-order-lines";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static Random random = new Random();
    private static AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    @Autowired
    private ObjectMapper om;

    @Autowired
    private ReceiptOrderLineRepository receiptOrderLineRepository;

    @Mock
    private ReceiptOrderLineRepository receiptOrderLineRepositoryMock;

    @Autowired
    private ReceiptOrderLineMapper receiptOrderLineMapper;

    @Mock
    private ReceiptOrderLineService receiptOrderLineServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restReceiptOrderLineMockMvc;

    private ReceiptOrderLine receiptOrderLine;

    private ReceiptOrderLine insertedReceiptOrderLine;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static ReceiptOrderLine createEntity(EntityManager em) {
        ReceiptOrderLine receiptOrderLine = new ReceiptOrderLine().amountCollected(DEFAULT_AMOUNT_COLLECTED);
        // Add required entity
        Receipt receipt;
        if (TestUtil.findAll(em, Receipt.class).isEmpty()) {
            receipt = ReceiptResourceIT.createEntity();
            em.persist(receipt);
            em.flush();
        } else {
            receipt = TestUtil.findAll(em, Receipt.class).get(0);
        }
        receiptOrderLine.setReceipt(receipt);
        // Add required entity
        ShipmentOrder shipmentOrder;
        if (TestUtil.findAll(em, ShipmentOrder.class).isEmpty()) {
            shipmentOrder = ShipmentOrderResourceIT.createEntity(em);
            em.persist(shipmentOrder);
            em.flush();
        } else {
            shipmentOrder = TestUtil.findAll(em, ShipmentOrder.class).get(0);
        }
        receiptOrderLine.setOrder(shipmentOrder);
        return receiptOrderLine;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static ReceiptOrderLine createUpdatedEntity(EntityManager em) {
        ReceiptOrderLine updatedReceiptOrderLine = new ReceiptOrderLine().amountCollected(UPDATED_AMOUNT_COLLECTED);
        // Add required entity
        Receipt receipt;
        if (TestUtil.findAll(em, Receipt.class).isEmpty()) {
            receipt = ReceiptResourceIT.createUpdatedEntity();
            em.persist(receipt);
            em.flush();
        } else {
            receipt = TestUtil.findAll(em, Receipt.class).get(0);
        }
        updatedReceiptOrderLine.setReceipt(receipt);
        // Add required entity
        ShipmentOrder shipmentOrder;
        if (TestUtil.findAll(em, ShipmentOrder.class).isEmpty()) {
            shipmentOrder = ShipmentOrderResourceIT.createUpdatedEntity(em);
            em.persist(shipmentOrder);
            em.flush();
        } else {
            shipmentOrder = TestUtil.findAll(em, ShipmentOrder.class).get(0);
        }
        updatedReceiptOrderLine.setOrder(shipmentOrder);
        return updatedReceiptOrderLine;
    }

    @BeforeEach
    public void initTest() {
        receiptOrderLine = createEntity(em);
    }

    @AfterEach
    public void cleanup() {
        if (insertedReceiptOrderLine != null) {
            receiptOrderLineRepository.delete(insertedReceiptOrderLine);
            insertedReceiptOrderLine = null;
        }
    }

    @Test
    @Transactional
    void createReceiptOrderLine() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the ReceiptOrderLine
        ReceiptOrderLineDTO receiptOrderLineDTO = receiptOrderLineMapper.toDto(receiptOrderLine);
        var returnedReceiptOrderLineDTO = om.readValue(
            restReceiptOrderLineMockMvc
                .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(receiptOrderLineDTO)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            ReceiptOrderLineDTO.class
        );

        // Validate the ReceiptOrderLine in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedReceiptOrderLine = receiptOrderLineMapper.toEntity(returnedReceiptOrderLineDTO);
        assertReceiptOrderLineUpdatableFieldsEquals(returnedReceiptOrderLine, getPersistedReceiptOrderLine(returnedReceiptOrderLine));

        insertedReceiptOrderLine = returnedReceiptOrderLine;
    }

    @Test
    @Transactional
    void createReceiptOrderLineWithExistingId() throws Exception {
        // Create the ReceiptOrderLine with an existing ID
        receiptOrderLine.setId(1L);
        ReceiptOrderLineDTO receiptOrderLineDTO = receiptOrderLineMapper.toDto(receiptOrderLine);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restReceiptOrderLineMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(receiptOrderLineDTO)))
            .andExpect(status().isBadRequest());

        // Validate the ReceiptOrderLine in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkAmountCollectedIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        receiptOrderLine.setAmountCollected(null);

        // Create the ReceiptOrderLine, which fails.
        ReceiptOrderLineDTO receiptOrderLineDTO = receiptOrderLineMapper.toDto(receiptOrderLine);

        restReceiptOrderLineMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(receiptOrderLineDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllReceiptOrderLines() throws Exception {
        // Initialize the database
        insertedReceiptOrderLine = receiptOrderLineRepository.saveAndFlush(receiptOrderLine);

        // Get all the receiptOrderLineList
        restReceiptOrderLineMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(receiptOrderLine.getId().intValue())))
            .andExpect(jsonPath("$.[*].amountCollected").value(hasItem(sameNumber(DEFAULT_AMOUNT_COLLECTED))));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllReceiptOrderLinesWithEagerRelationshipsIsEnabled() throws Exception {
        when(receiptOrderLineServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restReceiptOrderLineMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(receiptOrderLineServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllReceiptOrderLinesWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(receiptOrderLineServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restReceiptOrderLineMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(receiptOrderLineRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getReceiptOrderLine() throws Exception {
        // Initialize the database
        insertedReceiptOrderLine = receiptOrderLineRepository.saveAndFlush(receiptOrderLine);

        // Get the receiptOrderLine
        restReceiptOrderLineMockMvc
            .perform(get(ENTITY_API_URL_ID, receiptOrderLine.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(receiptOrderLine.getId().intValue()))
            .andExpect(jsonPath("$.amountCollected").value(sameNumber(DEFAULT_AMOUNT_COLLECTED)));
    }

    @Test
    @Transactional
    void getNonExistingReceiptOrderLine() throws Exception {
        // Get the receiptOrderLine
        restReceiptOrderLineMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingReceiptOrderLine() throws Exception {
        // Initialize the database
        insertedReceiptOrderLine = receiptOrderLineRepository.saveAndFlush(receiptOrderLine);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the receiptOrderLine
        ReceiptOrderLine updatedReceiptOrderLine = receiptOrderLineRepository.findById(receiptOrderLine.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedReceiptOrderLine are not directly saved in db
        em.detach(updatedReceiptOrderLine);
        updatedReceiptOrderLine.amountCollected(UPDATED_AMOUNT_COLLECTED);
        ReceiptOrderLineDTO receiptOrderLineDTO = receiptOrderLineMapper.toDto(updatedReceiptOrderLine);

        restReceiptOrderLineMockMvc
            .perform(
                put(ENTITY_API_URL_ID, receiptOrderLineDTO.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(receiptOrderLineDTO))
            )
            .andExpect(status().isOk());

        // Validate the ReceiptOrderLine in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedReceiptOrderLineToMatchAllProperties(updatedReceiptOrderLine);
    }

    @Test
    @Transactional
    void putNonExistingReceiptOrderLine() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        receiptOrderLine.setId(longCount.incrementAndGet());

        // Create the ReceiptOrderLine
        ReceiptOrderLineDTO receiptOrderLineDTO = receiptOrderLineMapper.toDto(receiptOrderLine);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restReceiptOrderLineMockMvc
            .perform(
                put(ENTITY_API_URL_ID, receiptOrderLineDTO.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(receiptOrderLineDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the ReceiptOrderLine in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchReceiptOrderLine() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        receiptOrderLine.setId(longCount.incrementAndGet());

        // Create the ReceiptOrderLine
        ReceiptOrderLineDTO receiptOrderLineDTO = receiptOrderLineMapper.toDto(receiptOrderLine);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restReceiptOrderLineMockMvc
            .perform(
                put(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(receiptOrderLineDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the ReceiptOrderLine in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamReceiptOrderLine() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        receiptOrderLine.setId(longCount.incrementAndGet());

        // Create the ReceiptOrderLine
        ReceiptOrderLineDTO receiptOrderLineDTO = receiptOrderLineMapper.toDto(receiptOrderLine);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restReceiptOrderLineMockMvc
            .perform(put(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(receiptOrderLineDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the ReceiptOrderLine in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateReceiptOrderLineWithPatch() throws Exception {
        // Initialize the database
        insertedReceiptOrderLine = receiptOrderLineRepository.saveAndFlush(receiptOrderLine);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the receiptOrderLine using partial update
        ReceiptOrderLine partialUpdatedReceiptOrderLine = new ReceiptOrderLine();
        partialUpdatedReceiptOrderLine.setId(receiptOrderLine.getId());

        restReceiptOrderLineMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedReceiptOrderLine.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedReceiptOrderLine))
            )
            .andExpect(status().isOk());

        // Validate the ReceiptOrderLine in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertReceiptOrderLineUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedReceiptOrderLine, receiptOrderLine),
            getPersistedReceiptOrderLine(receiptOrderLine)
        );
    }

    @Test
    @Transactional
    void fullUpdateReceiptOrderLineWithPatch() throws Exception {
        // Initialize the database
        insertedReceiptOrderLine = receiptOrderLineRepository.saveAndFlush(receiptOrderLine);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the receiptOrderLine using partial update
        ReceiptOrderLine partialUpdatedReceiptOrderLine = new ReceiptOrderLine();
        partialUpdatedReceiptOrderLine.setId(receiptOrderLine.getId());

        partialUpdatedReceiptOrderLine.amountCollected(UPDATED_AMOUNT_COLLECTED);

        restReceiptOrderLineMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedReceiptOrderLine.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedReceiptOrderLine))
            )
            .andExpect(status().isOk());

        // Validate the ReceiptOrderLine in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertReceiptOrderLineUpdatableFieldsEquals(
            partialUpdatedReceiptOrderLine,
            getPersistedReceiptOrderLine(partialUpdatedReceiptOrderLine)
        );
    }

    @Test
    @Transactional
    void patchNonExistingReceiptOrderLine() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        receiptOrderLine.setId(longCount.incrementAndGet());

        // Create the ReceiptOrderLine
        ReceiptOrderLineDTO receiptOrderLineDTO = receiptOrderLineMapper.toDto(receiptOrderLine);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restReceiptOrderLineMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, receiptOrderLineDTO.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(receiptOrderLineDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the ReceiptOrderLine in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchReceiptOrderLine() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        receiptOrderLine.setId(longCount.incrementAndGet());

        // Create the ReceiptOrderLine
        ReceiptOrderLineDTO receiptOrderLineDTO = receiptOrderLineMapper.toDto(receiptOrderLine);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restReceiptOrderLineMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(receiptOrderLineDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the ReceiptOrderLine in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamReceiptOrderLine() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        receiptOrderLine.setId(longCount.incrementAndGet());

        // Create the ReceiptOrderLine
        ReceiptOrderLineDTO receiptOrderLineDTO = receiptOrderLineMapper.toDto(receiptOrderLine);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restReceiptOrderLineMockMvc
            .perform(patch(ENTITY_API_URL).contentType("application/merge-patch+json").content(om.writeValueAsBytes(receiptOrderLineDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the ReceiptOrderLine in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteReceiptOrderLine() throws Exception {
        // Initialize the database
        insertedReceiptOrderLine = receiptOrderLineRepository.saveAndFlush(receiptOrderLine);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the receiptOrderLine
        restReceiptOrderLineMockMvc
            .perform(delete(ENTITY_API_URL_ID, receiptOrderLine.getId()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return receiptOrderLineRepository.count();
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

    protected ReceiptOrderLine getPersistedReceiptOrderLine(ReceiptOrderLine receiptOrderLine) {
        return receiptOrderLineRepository.findById(receiptOrderLine.getId()).orElseThrow();
    }

    protected void assertPersistedReceiptOrderLineToMatchAllProperties(ReceiptOrderLine expectedReceiptOrderLine) {
        assertReceiptOrderLineAllPropertiesEquals(expectedReceiptOrderLine, getPersistedReceiptOrderLine(expectedReceiptOrderLine));
    }

    protected void assertPersistedReceiptOrderLineToMatchUpdatableProperties(ReceiptOrderLine expectedReceiptOrderLine) {
        assertReceiptOrderLineAllUpdatablePropertiesEquals(
            expectedReceiptOrderLine,
            getPersistedReceiptOrderLine(expectedReceiptOrderLine)
        );
    }
}
