package com.mycompany.myapp.web.rest;

import static com.mycompany.myapp.domain.ReceiptAsserts.*;
import static com.mycompany.myapp.web.rest.TestUtil.createUpdateProxyForBean;
import static com.mycompany.myapp.web.rest.TestUtil.sameNumber;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.myapp.IntegrationTest;
import com.mycompany.myapp.domain.Office;
import com.mycompany.myapp.domain.Receipt;
import com.mycompany.myapp.repository.ReceiptRepository;
import com.mycompany.myapp.service.ReceiptService;
import com.mycompany.myapp.service.dto.ReceiptDTO;
import com.mycompany.myapp.service.mapper.ReceiptMapper;
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
 * Integration tests for the {@link ReceiptResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser
class ReceiptResourceIT {

    private static final String DEFAULT_RECEIPT_CODE = "AAAAAAAAAA";
    private static final String UPDATED_RECEIPT_CODE = "BBBBBBBBBB";

    private static final String DEFAULT_PAYER_NAME = "AAAAAAAAAA";
    private static final String UPDATED_PAYER_NAME = "BBBBBBBBBB";

    private static final String DEFAULT_PAYER_CODE = "AAAAAAAAAA";
    private static final String UPDATED_PAYER_CODE = "BBBBBBBBBB";

    private static final BigDecimal DEFAULT_TOTAL_AMOUNT = new BigDecimal(0);
    private static final BigDecimal UPDATED_TOTAL_AMOUNT = new BigDecimal(1);
    private static final BigDecimal SMALLER_TOTAL_AMOUNT = new BigDecimal(0 - 1);

    private static final Instant DEFAULT_CREATED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_CREATED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final String DEFAULT_CREATED_BY_USERNAME = "AAAAAAAAAA";
    private static final String UPDATED_CREATED_BY_USERNAME = "BBBBBBBBBB";

    private static final String ENTITY_API_URL = "/api/receipt-entities";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static Random random = new Random();
    private static AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    @Autowired
    private ObjectMapper om;

    @Autowired
    private ReceiptRepository receiptRepository;

    @Mock
    private ReceiptRepository receiptRepositoryMock;

    @Autowired
    private ReceiptMapper receiptMapper;

    @Mock
    private ReceiptService receiptServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restReceiptMockMvc;

    private Receipt receipt;

    private Receipt insertedReceipt;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Receipt createEntity() {
        return new Receipt()
            .receiptCode(DEFAULT_RECEIPT_CODE)
            .payerName(DEFAULT_PAYER_NAME)
            .payerCode(DEFAULT_PAYER_CODE)
            .totalAmount(DEFAULT_TOTAL_AMOUNT)
            .createdAt(DEFAULT_CREATED_AT)
            .createdByUsername(DEFAULT_CREATED_BY_USERNAME);
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Receipt createUpdatedEntity() {
        return new Receipt()
            .receiptCode(UPDATED_RECEIPT_CODE)
            .payerName(UPDATED_PAYER_NAME)
            .payerCode(UPDATED_PAYER_CODE)
            .totalAmount(UPDATED_TOTAL_AMOUNT)
            .createdAt(UPDATED_CREATED_AT)
            .createdByUsername(UPDATED_CREATED_BY_USERNAME);
    }

    @BeforeEach
    public void initTest() {
        receipt = createEntity();
    }

    @AfterEach
    public void cleanup() {
        if (insertedReceipt != null) {
            receiptRepository.delete(insertedReceipt);
            insertedReceipt = null;
        }
    }

    @Test
    @Transactional
    void createReceipt() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the Receipt
        ReceiptDTO receiptDTO = receiptMapper.toDto(receipt);
        var returnedReceiptDTO = om.readValue(
            restReceiptMockMvc
                .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(receiptDTO)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            ReceiptDTO.class
        );

        // Validate the Receipt in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedReceipt = receiptMapper.toEntity(returnedReceiptDTO);
        assertReceiptUpdatableFieldsEquals(returnedReceipt, getPersistedReceipt(returnedReceipt));

        insertedReceipt = returnedReceipt;
    }

    @Test
    @Transactional
    void createReceiptWithExistingId() throws Exception {
        // Create the Receipt with an existing ID
        receipt.setId(1L);
        ReceiptDTO receiptDTO = receiptMapper.toDto(receipt);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restReceiptMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(receiptDTO)))
            .andExpect(status().isBadRequest());

        // Validate the Receipt in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkReceiptCodeIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        receipt.setReceiptCode(null);

        // Create the Receipt, which fails.
        ReceiptDTO receiptDTO = receiptMapper.toDto(receipt);

        restReceiptMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(receiptDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkPayerNameIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        receipt.setPayerName(null);

        // Create the Receipt, which fails.
        ReceiptDTO receiptDTO = receiptMapper.toDto(receipt);

        restReceiptMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(receiptDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkTotalAmountIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        receipt.setTotalAmount(null);

        // Create the Receipt, which fails.
        ReceiptDTO receiptDTO = receiptMapper.toDto(receipt);

        restReceiptMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(receiptDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkCreatedAtIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        receipt.setCreatedAt(null);

        // Create the Receipt, which fails.
        ReceiptDTO receiptDTO = receiptMapper.toDto(receipt);

        restReceiptMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(receiptDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkCreatedByUsernameIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        receipt.setCreatedByUsername(null);

        // Create the Receipt, which fails.
        ReceiptDTO receiptDTO = receiptMapper.toDto(receipt);

        restReceiptMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(receiptDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllReceipts() throws Exception {
        // Initialize the database
        insertedReceipt = receiptRepository.saveAndFlush(receipt);

        // Get all the receiptList
        restReceiptMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(receipt.getId().intValue())))
            .andExpect(jsonPath("$.[*].receiptCode").value(hasItem(DEFAULT_RECEIPT_CODE)))
            .andExpect(jsonPath("$.[*].payerName").value(hasItem(DEFAULT_PAYER_NAME)))
            .andExpect(jsonPath("$.[*].payerCode").value(hasItem(DEFAULT_PAYER_CODE)))
            .andExpect(jsonPath("$.[*].totalAmount").value(hasItem(sameNumber(DEFAULT_TOTAL_AMOUNT))))
            .andExpect(jsonPath("$.[*].createdAt").value(hasItem(DEFAULT_CREATED_AT.toString())))
            .andExpect(jsonPath("$.[*].createdByUsername").value(hasItem(DEFAULT_CREATED_BY_USERNAME)));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllReceiptsWithEagerRelationshipsIsEnabled() throws Exception {
        when(receiptServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restReceiptMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(receiptServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllReceiptsWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(receiptServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restReceiptMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(receiptRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getReceipt() throws Exception {
        // Initialize the database
        insertedReceipt = receiptRepository.saveAndFlush(receipt);

        // Get the receipt
        restReceiptMockMvc
            .perform(get(ENTITY_API_URL_ID, receipt.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(receipt.getId().intValue()))
            .andExpect(jsonPath("$.receiptCode").value(DEFAULT_RECEIPT_CODE))
            .andExpect(jsonPath("$.payerName").value(DEFAULT_PAYER_NAME))
            .andExpect(jsonPath("$.payerCode").value(DEFAULT_PAYER_CODE))
            .andExpect(jsonPath("$.totalAmount").value(sameNumber(DEFAULT_TOTAL_AMOUNT)))
            .andExpect(jsonPath("$.createdAt").value(DEFAULT_CREATED_AT.toString()))
            .andExpect(jsonPath("$.createdByUsername").value(DEFAULT_CREATED_BY_USERNAME));
    }

    @Test
    @Transactional
    void getReceiptsByIdFiltering() throws Exception {
        // Initialize the database
        insertedReceipt = receiptRepository.saveAndFlush(receipt);

        Long id = receipt.getId();

        defaultReceiptFiltering("id.equals=" + id, "id.notEquals=" + id);

        defaultReceiptFiltering("id.greaterThanOrEqual=" + id, "id.greaterThan=" + id);

        defaultReceiptFiltering("id.lessThanOrEqual=" + id, "id.lessThan=" + id);
    }

    @Test
    @Transactional
    void getAllReceiptsByReceiptCodeIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedReceipt = receiptRepository.saveAndFlush(receipt);

        // Get all the receiptList where receiptCode equals to
        defaultReceiptFiltering("receiptCode.equals=" + DEFAULT_RECEIPT_CODE, "receiptCode.equals=" + UPDATED_RECEIPT_CODE);
    }

    @Test
    @Transactional
    void getAllReceiptsByReceiptCodeIsInShouldWork() throws Exception {
        // Initialize the database
        insertedReceipt = receiptRepository.saveAndFlush(receipt);

        // Get all the receiptList where receiptCode in
        defaultReceiptFiltering(
            "receiptCode.in=" + DEFAULT_RECEIPT_CODE + "," + UPDATED_RECEIPT_CODE,
            "receiptCode.in=" + UPDATED_RECEIPT_CODE
        );
    }

    @Test
    @Transactional
    void getAllReceiptsByReceiptCodeIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedReceipt = receiptRepository.saveAndFlush(receipt);

        // Get all the receiptList where receiptCode is not null
        defaultReceiptFiltering("receiptCode.specified=true", "receiptCode.specified=false");
    }

    @Test
    @Transactional
    void getAllReceiptsByReceiptCodeContainsSomething() throws Exception {
        // Initialize the database
        insertedReceipt = receiptRepository.saveAndFlush(receipt);

        // Get all the receiptList where receiptCode contains
        defaultReceiptFiltering("receiptCode.contains=" + DEFAULT_RECEIPT_CODE, "receiptCode.contains=" + UPDATED_RECEIPT_CODE);
    }

    @Test
    @Transactional
    void getAllReceiptsByReceiptCodeNotContainsSomething() throws Exception {
        // Initialize the database
        insertedReceipt = receiptRepository.saveAndFlush(receipt);

        // Get all the receiptList where receiptCode does not contain
        defaultReceiptFiltering("receiptCode.doesNotContain=" + UPDATED_RECEIPT_CODE, "receiptCode.doesNotContain=" + DEFAULT_RECEIPT_CODE);
    }

    @Test
    @Transactional
    void getAllReceiptsByPayerNameIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedReceipt = receiptRepository.saveAndFlush(receipt);

        // Get all the receiptList where payerName equals to
        defaultReceiptFiltering("payerName.equals=" + DEFAULT_PAYER_NAME, "payerName.equals=" + UPDATED_PAYER_NAME);
    }

    @Test
    @Transactional
    void getAllReceiptsByPayerNameIsInShouldWork() throws Exception {
        // Initialize the database
        insertedReceipt = receiptRepository.saveAndFlush(receipt);

        // Get all the receiptList where payerName in
        defaultReceiptFiltering("payerName.in=" + DEFAULT_PAYER_NAME + "," + UPDATED_PAYER_NAME, "payerName.in=" + UPDATED_PAYER_NAME);
    }

    @Test
    @Transactional
    void getAllReceiptsByPayerNameIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedReceipt = receiptRepository.saveAndFlush(receipt);

        // Get all the receiptList where payerName is not null
        defaultReceiptFiltering("payerName.specified=true", "payerName.specified=false");
    }

    @Test
    @Transactional
    void getAllReceiptsByPayerNameContainsSomething() throws Exception {
        // Initialize the database
        insertedReceipt = receiptRepository.saveAndFlush(receipt);

        // Get all the receiptList where payerName contains
        defaultReceiptFiltering("payerName.contains=" + DEFAULT_PAYER_NAME, "payerName.contains=" + UPDATED_PAYER_NAME);
    }

    @Test
    @Transactional
    void getAllReceiptsByPayerNameNotContainsSomething() throws Exception {
        // Initialize the database
        insertedReceipt = receiptRepository.saveAndFlush(receipt);

        // Get all the receiptList where payerName does not contain
        defaultReceiptFiltering("payerName.doesNotContain=" + UPDATED_PAYER_NAME, "payerName.doesNotContain=" + DEFAULT_PAYER_NAME);
    }

    @Test
    @Transactional
    void getAllReceiptsByPayerCodeIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedReceipt = receiptRepository.saveAndFlush(receipt);

        // Get all the receiptList where payerCode equals to
        defaultReceiptFiltering("payerCode.equals=" + DEFAULT_PAYER_CODE, "payerCode.equals=" + UPDATED_PAYER_CODE);
    }

    @Test
    @Transactional
    void getAllReceiptsByPayerCodeIsInShouldWork() throws Exception {
        // Initialize the database
        insertedReceipt = receiptRepository.saveAndFlush(receipt);

        // Get all the receiptList where payerCode in
        defaultReceiptFiltering("payerCode.in=" + DEFAULT_PAYER_CODE + "," + UPDATED_PAYER_CODE, "payerCode.in=" + UPDATED_PAYER_CODE);
    }

    @Test
    @Transactional
    void getAllReceiptsByPayerCodeIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedReceipt = receiptRepository.saveAndFlush(receipt);

        // Get all the receiptList where payerCode is not null
        defaultReceiptFiltering("payerCode.specified=true", "payerCode.specified=false");
    }

    @Test
    @Transactional
    void getAllReceiptsByPayerCodeContainsSomething() throws Exception {
        // Initialize the database
        insertedReceipt = receiptRepository.saveAndFlush(receipt);

        // Get all the receiptList where payerCode contains
        defaultReceiptFiltering("payerCode.contains=" + DEFAULT_PAYER_CODE, "payerCode.contains=" + UPDATED_PAYER_CODE);
    }

    @Test
    @Transactional
    void getAllReceiptsByPayerCodeNotContainsSomething() throws Exception {
        // Initialize the database
        insertedReceipt = receiptRepository.saveAndFlush(receipt);

        // Get all the receiptList where payerCode does not contain
        defaultReceiptFiltering("payerCode.doesNotContain=" + UPDATED_PAYER_CODE, "payerCode.doesNotContain=" + DEFAULT_PAYER_CODE);
    }

    @Test
    @Transactional
    void getAllReceiptsByTotalAmountIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedReceipt = receiptRepository.saveAndFlush(receipt);

        // Get all the receiptList where totalAmount equals to
        defaultReceiptFiltering("totalAmount.equals=" + DEFAULT_TOTAL_AMOUNT, "totalAmount.equals=" + UPDATED_TOTAL_AMOUNT);
    }

    @Test
    @Transactional
    void getAllReceiptsByTotalAmountIsInShouldWork() throws Exception {
        // Initialize the database
        insertedReceipt = receiptRepository.saveAndFlush(receipt);

        // Get all the receiptList where totalAmount in
        defaultReceiptFiltering(
            "totalAmount.in=" + DEFAULT_TOTAL_AMOUNT + "," + UPDATED_TOTAL_AMOUNT,
            "totalAmount.in=" + UPDATED_TOTAL_AMOUNT
        );
    }

    @Test
    @Transactional
    void getAllReceiptsByTotalAmountIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedReceipt = receiptRepository.saveAndFlush(receipt);

        // Get all the receiptList where totalAmount is not null
        defaultReceiptFiltering("totalAmount.specified=true", "totalAmount.specified=false");
    }

    @Test
    @Transactional
    void getAllReceiptsByTotalAmountIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedReceipt = receiptRepository.saveAndFlush(receipt);

        // Get all the receiptList where totalAmount is greater than or equal to
        defaultReceiptFiltering(
            "totalAmount.greaterThanOrEqual=" + DEFAULT_TOTAL_AMOUNT,
            "totalAmount.greaterThanOrEqual=" + UPDATED_TOTAL_AMOUNT
        );
    }

    @Test
    @Transactional
    void getAllReceiptsByTotalAmountIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedReceipt = receiptRepository.saveAndFlush(receipt);

        // Get all the receiptList where totalAmount is less than or equal to
        defaultReceiptFiltering(
            "totalAmount.lessThanOrEqual=" + DEFAULT_TOTAL_AMOUNT,
            "totalAmount.lessThanOrEqual=" + SMALLER_TOTAL_AMOUNT
        );
    }

    @Test
    @Transactional
    void getAllReceiptsByTotalAmountIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedReceipt = receiptRepository.saveAndFlush(receipt);

        // Get all the receiptList where totalAmount is less than
        defaultReceiptFiltering("totalAmount.lessThan=" + UPDATED_TOTAL_AMOUNT, "totalAmount.lessThan=" + DEFAULT_TOTAL_AMOUNT);
    }

    @Test
    @Transactional
    void getAllReceiptsByTotalAmountIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedReceipt = receiptRepository.saveAndFlush(receipt);

        // Get all the receiptList where totalAmount is greater than
        defaultReceiptFiltering("totalAmount.greaterThan=" + SMALLER_TOTAL_AMOUNT, "totalAmount.greaterThan=" + DEFAULT_TOTAL_AMOUNT);
    }

    @Test
    @Transactional
    void getAllReceiptsByCreatedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedReceipt = receiptRepository.saveAndFlush(receipt);

        // Get all the receiptList where createdAt equals to
        defaultReceiptFiltering("createdAt.equals=" + DEFAULT_CREATED_AT, "createdAt.equals=" + UPDATED_CREATED_AT);
    }

    @Test
    @Transactional
    void getAllReceiptsByCreatedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedReceipt = receiptRepository.saveAndFlush(receipt);

        // Get all the receiptList where createdAt in
        defaultReceiptFiltering("createdAt.in=" + DEFAULT_CREATED_AT + "," + UPDATED_CREATED_AT, "createdAt.in=" + UPDATED_CREATED_AT);
    }

    @Test
    @Transactional
    void getAllReceiptsByCreatedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedReceipt = receiptRepository.saveAndFlush(receipt);

        // Get all the receiptList where createdAt is not null
        defaultReceiptFiltering("createdAt.specified=true", "createdAt.specified=false");
    }

    @Test
    @Transactional
    void getAllReceiptsByCreatedByUsernameIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedReceipt = receiptRepository.saveAndFlush(receipt);

        // Get all the receiptList where createdByUsername equals to
        defaultReceiptFiltering(
            "createdByUsername.equals=" + DEFAULT_CREATED_BY_USERNAME,
            "createdByUsername.equals=" + UPDATED_CREATED_BY_USERNAME
        );
    }

    @Test
    @Transactional
    void getAllReceiptsByCreatedByUsernameIsInShouldWork() throws Exception {
        // Initialize the database
        insertedReceipt = receiptRepository.saveAndFlush(receipt);

        // Get all the receiptList where createdByUsername in
        defaultReceiptFiltering(
            "createdByUsername.in=" + DEFAULT_CREATED_BY_USERNAME + "," + UPDATED_CREATED_BY_USERNAME,
            "createdByUsername.in=" + UPDATED_CREATED_BY_USERNAME
        );
    }

    @Test
    @Transactional
    void getAllReceiptsByCreatedByUsernameIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedReceipt = receiptRepository.saveAndFlush(receipt);

        // Get all the receiptList where createdByUsername is not null
        defaultReceiptFiltering("createdByUsername.specified=true", "createdByUsername.specified=false");
    }

    @Test
    @Transactional
    void getAllReceiptsByCreatedByUsernameContainsSomething() throws Exception {
        // Initialize the database
        insertedReceipt = receiptRepository.saveAndFlush(receipt);

        // Get all the receiptList where createdByUsername contains
        defaultReceiptFiltering(
            "createdByUsername.contains=" + DEFAULT_CREATED_BY_USERNAME,
            "createdByUsername.contains=" + UPDATED_CREATED_BY_USERNAME
        );
    }

    @Test
    @Transactional
    void getAllReceiptsByCreatedByUsernameNotContainsSomething() throws Exception {
        // Initialize the database
        insertedReceipt = receiptRepository.saveAndFlush(receipt);

        // Get all the receiptList where createdByUsername does not contain
        defaultReceiptFiltering(
            "createdByUsername.doesNotContain=" + UPDATED_CREATED_BY_USERNAME,
            "createdByUsername.doesNotContain=" + DEFAULT_CREATED_BY_USERNAME
        );
    }

    @Test
    @Transactional
    void getAllReceiptsByOfficeIsEqualToSomething() throws Exception {
        Office office;
        if (TestUtil.findAll(em, Office.class).isEmpty()) {
            receiptRepository.saveAndFlush(receipt);
            office = OfficeResourceIT.createEntity();
        } else {
            office = TestUtil.findAll(em, Office.class).get(0);
        }
        em.persist(office);
        em.flush();
        receipt.setOffice(office);
        receiptRepository.saveAndFlush(receipt);
        Long officeId = office.getId();
        // Get all the receiptList where office equals to officeId
        defaultReceiptShouldBeFound("officeId.equals=" + officeId);

        // Get all the receiptList where office equals to (officeId + 1)
        defaultReceiptShouldNotBeFound("officeId.equals=" + (officeId + 1));
    }

    private void defaultReceiptFiltering(String shouldBeFound, String shouldNotBeFound) throws Exception {
        defaultReceiptShouldBeFound(shouldBeFound);
        defaultReceiptShouldNotBeFound(shouldNotBeFound);
    }

    /**
     * Executes the search, and checks that the default entity is returned.
     */
    private void defaultReceiptShouldBeFound(String filter) throws Exception {
        restReceiptMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(receipt.getId().intValue())))
            .andExpect(jsonPath("$.[*].receiptCode").value(hasItem(DEFAULT_RECEIPT_CODE)))
            .andExpect(jsonPath("$.[*].payerName").value(hasItem(DEFAULT_PAYER_NAME)))
            .andExpect(jsonPath("$.[*].payerCode").value(hasItem(DEFAULT_PAYER_CODE)))
            .andExpect(jsonPath("$.[*].totalAmount").value(hasItem(sameNumber(DEFAULT_TOTAL_AMOUNT))))
            .andExpect(jsonPath("$.[*].createdAt").value(hasItem(DEFAULT_CREATED_AT.toString())))
            .andExpect(jsonPath("$.[*].createdByUsername").value(hasItem(DEFAULT_CREATED_BY_USERNAME)));

        // Check, that the count call also returns 1
        restReceiptMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("1"));
    }

    /**
     * Executes the search, and checks that the default entity is not returned.
     */
    private void defaultReceiptShouldNotBeFound(String filter) throws Exception {
        restReceiptMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());

        // Check, that the count call also returns 0
        restReceiptMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("0"));
    }

    @Test
    @Transactional
    void getNonExistingReceipt() throws Exception {
        // Get the receipt
        restReceiptMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingReceipt() throws Exception {
        // Initialize the database
        insertedReceipt = receiptRepository.saveAndFlush(receipt);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the receipt
        Receipt updatedReceipt = receiptRepository.findById(receipt.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedReceipt are not directly saved in db
        em.detach(updatedReceipt);
        updatedReceipt
            .receiptCode(UPDATED_RECEIPT_CODE)
            .payerName(UPDATED_PAYER_NAME)
            .payerCode(UPDATED_PAYER_CODE)
            .totalAmount(UPDATED_TOTAL_AMOUNT)
            .createdAt(UPDATED_CREATED_AT)
            .createdByUsername(UPDATED_CREATED_BY_USERNAME);
        ReceiptDTO receiptDTO = receiptMapper.toDto(updatedReceipt);

        restReceiptMockMvc
            .perform(
                put(ENTITY_API_URL_ID, receiptDTO.getId()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(receiptDTO))
            )
            .andExpect(status().isOk());

        // Validate the Receipt in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedReceiptToMatchAllProperties(updatedReceipt);
    }

    @Test
    @Transactional
    void putNonExistingReceipt() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        receipt.setId(longCount.incrementAndGet());

        // Create the Receipt
        ReceiptDTO receiptDTO = receiptMapper.toDto(receipt);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restReceiptMockMvc
            .perform(
                put(ENTITY_API_URL_ID, receiptDTO.getId()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(receiptDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Receipt in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchReceipt() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        receipt.setId(longCount.incrementAndGet());

        // Create the Receipt
        ReceiptDTO receiptDTO = receiptMapper.toDto(receipt);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restReceiptMockMvc
            .perform(
                put(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(receiptDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Receipt in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamReceipt() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        receipt.setId(longCount.incrementAndGet());

        // Create the Receipt
        ReceiptDTO receiptDTO = receiptMapper.toDto(receipt);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restReceiptMockMvc
            .perform(put(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(receiptDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Receipt in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateReceiptWithPatch() throws Exception {
        // Initialize the database
        insertedReceipt = receiptRepository.saveAndFlush(receipt);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the receipt using partial update
        Receipt partialUpdatedReceipt = new Receipt();
        partialUpdatedReceipt.setId(receipt.getId());

        partialUpdatedReceipt
            .receiptCode(UPDATED_RECEIPT_CODE)
            .totalAmount(UPDATED_TOTAL_AMOUNT)
            .createdAt(UPDATED_CREATED_AT)
            .createdByUsername(UPDATED_CREATED_BY_USERNAME);

        restReceiptMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedReceipt.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedReceipt))
            )
            .andExpect(status().isOk());

        // Validate the Receipt in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertReceiptUpdatableFieldsEquals(createUpdateProxyForBean(partialUpdatedReceipt, receipt), getPersistedReceipt(receipt));
    }

    @Test
    @Transactional
    void fullUpdateReceiptWithPatch() throws Exception {
        // Initialize the database
        insertedReceipt = receiptRepository.saveAndFlush(receipt);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the receipt using partial update
        Receipt partialUpdatedReceipt = new Receipt();
        partialUpdatedReceipt.setId(receipt.getId());

        partialUpdatedReceipt
            .receiptCode(UPDATED_RECEIPT_CODE)
            .payerName(UPDATED_PAYER_NAME)
            .payerCode(UPDATED_PAYER_CODE)
            .totalAmount(UPDATED_TOTAL_AMOUNT)
            .createdAt(UPDATED_CREATED_AT)
            .createdByUsername(UPDATED_CREATED_BY_USERNAME);

        restReceiptMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedReceipt.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedReceipt))
            )
            .andExpect(status().isOk());

        // Validate the Receipt in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertReceiptUpdatableFieldsEquals(partialUpdatedReceipt, getPersistedReceipt(partialUpdatedReceipt));
    }

    @Test
    @Transactional
    void patchNonExistingReceipt() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        receipt.setId(longCount.incrementAndGet());

        // Create the Receipt
        ReceiptDTO receiptDTO = receiptMapper.toDto(receipt);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restReceiptMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, receiptDTO.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(receiptDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Receipt in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchReceipt() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        receipt.setId(longCount.incrementAndGet());

        // Create the Receipt
        ReceiptDTO receiptDTO = receiptMapper.toDto(receipt);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restReceiptMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(receiptDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Receipt in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamReceipt() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        receipt.setId(longCount.incrementAndGet());

        // Create the Receipt
        ReceiptDTO receiptDTO = receiptMapper.toDto(receipt);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restReceiptMockMvc
            .perform(patch(ENTITY_API_URL).contentType("application/merge-patch+json").content(om.writeValueAsBytes(receiptDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Receipt in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteReceipt() throws Exception {
        // Initialize the database
        insertedReceipt = receiptRepository.saveAndFlush(receipt);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the receipt
        restReceiptMockMvc
            .perform(delete(ENTITY_API_URL_ID, receipt.getId()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return receiptRepository.count();
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

    protected Receipt getPersistedReceipt(Receipt receipt) {
        return receiptRepository.findById(receipt.getId()).orElseThrow();
    }

    protected void assertPersistedReceiptToMatchAllProperties(Receipt expectedReceipt) {
        assertReceiptAllPropertiesEquals(expectedReceipt, getPersistedReceipt(expectedReceipt));
    }

    protected void assertPersistedReceiptToMatchUpdatableProperties(Receipt expectedReceipt) {
        assertReceiptAllUpdatablePropertiesEquals(expectedReceipt, getPersistedReceipt(expectedReceipt));
    }
}
