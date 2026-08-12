package com.mycompany.myapp.web.rest;

import static com.mycompany.myapp.domain.SurchargePolicyAsserts.*;
import static com.mycompany.myapp.web.rest.TestUtil.createUpdateProxyForBean;
import static com.mycompany.myapp.web.rest.TestUtil.sameNumber;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.myapp.IntegrationTest;
import com.mycompany.myapp.domain.SurchargePolicy;
import com.mycompany.myapp.repository.SurchargePolicyRepository;
import com.mycompany.myapp.service.dto.SurchargePolicyDTO;
import com.mycompany.myapp.service.mapper.SurchargePolicyMapper;
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
 * Integration tests for the {@link SurchargePolicyResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser
class SurchargePolicyResourceIT {

    private static final Boolean DEFAULT_HOME_DELIVERY_ENABLED = false;
    private static final Boolean UPDATED_HOME_DELIVERY_ENABLED = true;

    private static final BigDecimal DEFAULT_DEFAULT_HOME_DELIVERY_AMOUNT = new BigDecimal(0);
    private static final BigDecimal UPDATED_DEFAULT_HOME_DELIVERY_AMOUNT = new BigDecimal(1);

    private static final Boolean DEFAULT_COD_ENABLED = false;
    private static final Boolean UPDATED_COD_ENABLED = true;

    private static final BigDecimal DEFAULT_COD_PERCENT = new BigDecimal(0);
    private static final BigDecimal UPDATED_COD_PERCENT = new BigDecimal(1);

    private static final BigDecimal DEFAULT_COD_MIN_FEE = new BigDecimal(0);
    private static final BigDecimal UPDATED_COD_MIN_FEE = new BigDecimal(1);

    private static final Boolean DEFAULT_STORAGE_ENABLED = false;
    private static final Boolean UPDATED_STORAGE_ENABLED = true;

    private static final Integer DEFAULT_STORAGE_FREE_DAYS = 0;
    private static final Integer UPDATED_STORAGE_FREE_DAYS = 1;

    private static final BigDecimal DEFAULT_STORAGE_FEE_PER_DAY = new BigDecimal(0);
    private static final BigDecimal UPDATED_STORAGE_FEE_PER_DAY = new BigDecimal(1);

    private static final Boolean DEFAULT_INSURANCE_ENABLED = false;
    private static final Boolean UPDATED_INSURANCE_ENABLED = true;

    private static final BigDecimal DEFAULT_INSURANCE_THRESHOLD = new BigDecimal(0);
    private static final BigDecimal UPDATED_INSURANCE_THRESHOLD = new BigDecimal(1);

    private static final BigDecimal DEFAULT_INSURANCE_PERCENT_UNDER = new BigDecimal(0);
    private static final BigDecimal UPDATED_INSURANCE_PERCENT_UNDER = new BigDecimal(1);

    private static final BigDecimal DEFAULT_INSURANCE_PERCENT_OVER = new BigDecimal(0);
    private static final BigDecimal UPDATED_INSURANCE_PERCENT_OVER = new BigDecimal(1);

    private static final Boolean DEFAULT_REFUND_ENABLED = false;
    private static final Boolean UPDATED_REFUND_ENABLED = true;

    private static final BigDecimal DEFAULT_REFUND_PERCENT = new BigDecimal(0);
    private static final BigDecimal UPDATED_REFUND_PERCENT = new BigDecimal(1);

    private static final Instant DEFAULT_UPDATED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_UPDATED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final String ENTITY_API_URL = "/api/surcharge-policies";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static Random random = new Random();
    private static AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    @Autowired
    private ObjectMapper om;

    @Autowired
    private SurchargePolicyRepository surchargePolicyRepository;

    @Autowired
    private SurchargePolicyMapper surchargePolicyMapper;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restSurchargePolicyMockMvc;

    private SurchargePolicy surchargePolicy;

    private SurchargePolicy insertedSurchargePolicy;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static SurchargePolicy createEntity() {
        return new SurchargePolicy()
            .homeDeliveryEnabled(DEFAULT_HOME_DELIVERY_ENABLED)
            .defaultHomeDeliveryAmount(DEFAULT_DEFAULT_HOME_DELIVERY_AMOUNT)
            .codEnabled(DEFAULT_COD_ENABLED)
            .codPercent(DEFAULT_COD_PERCENT)
            .codMinFee(DEFAULT_COD_MIN_FEE)
            .storageEnabled(DEFAULT_STORAGE_ENABLED)
            .storageFreeDays(DEFAULT_STORAGE_FREE_DAYS)
            .storageFeePerDay(DEFAULT_STORAGE_FEE_PER_DAY)
            .insuranceEnabled(DEFAULT_INSURANCE_ENABLED)
            .insuranceThreshold(DEFAULT_INSURANCE_THRESHOLD)
            .insurancePercentUnder(DEFAULT_INSURANCE_PERCENT_UNDER)
            .insurancePercentOver(DEFAULT_INSURANCE_PERCENT_OVER)
            .refundEnabled(DEFAULT_REFUND_ENABLED)
            .refundPercent(DEFAULT_REFUND_PERCENT)
            .updatedAt(DEFAULT_UPDATED_AT);
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static SurchargePolicy createUpdatedEntity() {
        return new SurchargePolicy()
            .homeDeliveryEnabled(UPDATED_HOME_DELIVERY_ENABLED)
            .defaultHomeDeliveryAmount(UPDATED_DEFAULT_HOME_DELIVERY_AMOUNT)
            .codEnabled(UPDATED_COD_ENABLED)
            .codPercent(UPDATED_COD_PERCENT)
            .codMinFee(UPDATED_COD_MIN_FEE)
            .storageEnabled(UPDATED_STORAGE_ENABLED)
            .storageFreeDays(UPDATED_STORAGE_FREE_DAYS)
            .storageFeePerDay(UPDATED_STORAGE_FEE_PER_DAY)
            .insuranceEnabled(UPDATED_INSURANCE_ENABLED)
            .insuranceThreshold(UPDATED_INSURANCE_THRESHOLD)
            .insurancePercentUnder(UPDATED_INSURANCE_PERCENT_UNDER)
            .insurancePercentOver(UPDATED_INSURANCE_PERCENT_OVER)
            .refundEnabled(UPDATED_REFUND_ENABLED)
            .refundPercent(UPDATED_REFUND_PERCENT)
            .updatedAt(UPDATED_UPDATED_AT);
    }

    @BeforeEach
    public void initTest() {
        surchargePolicy = createEntity();
    }

    @AfterEach
    public void cleanup() {
        if (insertedSurchargePolicy != null) {
            surchargePolicyRepository.delete(insertedSurchargePolicy);
            insertedSurchargePolicy = null;
        }
    }

    @Test
    @Transactional
    void createSurchargePolicy() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the SurchargePolicy
        SurchargePolicyDTO surchargePolicyDTO = surchargePolicyMapper.toDto(surchargePolicy);
        var returnedSurchargePolicyDTO = om.readValue(
            restSurchargePolicyMockMvc
                .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(surchargePolicyDTO)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            SurchargePolicyDTO.class
        );

        // Validate the SurchargePolicy in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedSurchargePolicy = surchargePolicyMapper.toEntity(returnedSurchargePolicyDTO);
        assertSurchargePolicyUpdatableFieldsEquals(returnedSurchargePolicy, getPersistedSurchargePolicy(returnedSurchargePolicy));

        insertedSurchargePolicy = returnedSurchargePolicy;
    }

    @Test
    @Transactional
    void createSurchargePolicyWithExistingId() throws Exception {
        // Create the SurchargePolicy with an existing ID
        surchargePolicy.setId(1L);
        SurchargePolicyDTO surchargePolicyDTO = surchargePolicyMapper.toDto(surchargePolicy);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restSurchargePolicyMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(surchargePolicyDTO)))
            .andExpect(status().isBadRequest());

        // Validate the SurchargePolicy in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkHomeDeliveryEnabledIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        surchargePolicy.setHomeDeliveryEnabled(null);

        // Create the SurchargePolicy, which fails.
        SurchargePolicyDTO surchargePolicyDTO = surchargePolicyMapper.toDto(surchargePolicy);

        restSurchargePolicyMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(surchargePolicyDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkDefaultHomeDeliveryAmountIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        surchargePolicy.setDefaultHomeDeliveryAmount(null);

        // Create the SurchargePolicy, which fails.
        SurchargePolicyDTO surchargePolicyDTO = surchargePolicyMapper.toDto(surchargePolicy);

        restSurchargePolicyMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(surchargePolicyDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkCodEnabledIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        surchargePolicy.setCodEnabled(null);

        // Create the SurchargePolicy, which fails.
        SurchargePolicyDTO surchargePolicyDTO = surchargePolicyMapper.toDto(surchargePolicy);

        restSurchargePolicyMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(surchargePolicyDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkCodPercentIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        surchargePolicy.setCodPercent(null);

        // Create the SurchargePolicy, which fails.
        SurchargePolicyDTO surchargePolicyDTO = surchargePolicyMapper.toDto(surchargePolicy);

        restSurchargePolicyMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(surchargePolicyDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkCodMinFeeIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        surchargePolicy.setCodMinFee(null);

        // Create the SurchargePolicy, which fails.
        SurchargePolicyDTO surchargePolicyDTO = surchargePolicyMapper.toDto(surchargePolicy);

        restSurchargePolicyMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(surchargePolicyDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkStorageEnabledIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        surchargePolicy.setStorageEnabled(null);

        // Create the SurchargePolicy, which fails.
        SurchargePolicyDTO surchargePolicyDTO = surchargePolicyMapper.toDto(surchargePolicy);

        restSurchargePolicyMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(surchargePolicyDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkStorageFreeDaysIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        surchargePolicy.setStorageFreeDays(null);

        // Create the SurchargePolicy, which fails.
        SurchargePolicyDTO surchargePolicyDTO = surchargePolicyMapper.toDto(surchargePolicy);

        restSurchargePolicyMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(surchargePolicyDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkStorageFeePerDayIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        surchargePolicy.setStorageFeePerDay(null);

        // Create the SurchargePolicy, which fails.
        SurchargePolicyDTO surchargePolicyDTO = surchargePolicyMapper.toDto(surchargePolicy);

        restSurchargePolicyMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(surchargePolicyDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkInsuranceEnabledIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        surchargePolicy.setInsuranceEnabled(null);

        // Create the SurchargePolicy, which fails.
        SurchargePolicyDTO surchargePolicyDTO = surchargePolicyMapper.toDto(surchargePolicy);

        restSurchargePolicyMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(surchargePolicyDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkInsuranceThresholdIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        surchargePolicy.setInsuranceThreshold(null);

        // Create the SurchargePolicy, which fails.
        SurchargePolicyDTO surchargePolicyDTO = surchargePolicyMapper.toDto(surchargePolicy);

        restSurchargePolicyMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(surchargePolicyDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkInsurancePercentUnderIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        surchargePolicy.setInsurancePercentUnder(null);

        // Create the SurchargePolicy, which fails.
        SurchargePolicyDTO surchargePolicyDTO = surchargePolicyMapper.toDto(surchargePolicy);

        restSurchargePolicyMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(surchargePolicyDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkInsurancePercentOverIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        surchargePolicy.setInsurancePercentOver(null);

        // Create the SurchargePolicy, which fails.
        SurchargePolicyDTO surchargePolicyDTO = surchargePolicyMapper.toDto(surchargePolicy);

        restSurchargePolicyMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(surchargePolicyDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkRefundEnabledIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        surchargePolicy.setRefundEnabled(null);

        // Create the SurchargePolicy, which fails.
        SurchargePolicyDTO surchargePolicyDTO = surchargePolicyMapper.toDto(surchargePolicy);

        restSurchargePolicyMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(surchargePolicyDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkRefundPercentIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        surchargePolicy.setRefundPercent(null);

        // Create the SurchargePolicy, which fails.
        SurchargePolicyDTO surchargePolicyDTO = surchargePolicyMapper.toDto(surchargePolicy);

        restSurchargePolicyMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(surchargePolicyDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllSurchargePolicies() throws Exception {
        // Initialize the database
        insertedSurchargePolicy = surchargePolicyRepository.saveAndFlush(surchargePolicy);

        // Get all the surchargePolicyList
        restSurchargePolicyMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(surchargePolicy.getId().intValue())))
            .andExpect(jsonPath("$.[*].homeDeliveryEnabled").value(hasItem(DEFAULT_HOME_DELIVERY_ENABLED)))
            .andExpect(jsonPath("$.[*].defaultHomeDeliveryAmount").value(hasItem(sameNumber(DEFAULT_DEFAULT_HOME_DELIVERY_AMOUNT))))
            .andExpect(jsonPath("$.[*].codEnabled").value(hasItem(DEFAULT_COD_ENABLED)))
            .andExpect(jsonPath("$.[*].codPercent").value(hasItem(sameNumber(DEFAULT_COD_PERCENT))))
            .andExpect(jsonPath("$.[*].codMinFee").value(hasItem(sameNumber(DEFAULT_COD_MIN_FEE))))
            .andExpect(jsonPath("$.[*].storageEnabled").value(hasItem(DEFAULT_STORAGE_ENABLED)))
            .andExpect(jsonPath("$.[*].storageFreeDays").value(hasItem(DEFAULT_STORAGE_FREE_DAYS)))
            .andExpect(jsonPath("$.[*].storageFeePerDay").value(hasItem(sameNumber(DEFAULT_STORAGE_FEE_PER_DAY))))
            .andExpect(jsonPath("$.[*].insuranceEnabled").value(hasItem(DEFAULT_INSURANCE_ENABLED)))
            .andExpect(jsonPath("$.[*].insuranceThreshold").value(hasItem(sameNumber(DEFAULT_INSURANCE_THRESHOLD))))
            .andExpect(jsonPath("$.[*].insurancePercentUnder").value(hasItem(sameNumber(DEFAULT_INSURANCE_PERCENT_UNDER))))
            .andExpect(jsonPath("$.[*].insurancePercentOver").value(hasItem(sameNumber(DEFAULT_INSURANCE_PERCENT_OVER))))
            .andExpect(jsonPath("$.[*].refundEnabled").value(hasItem(DEFAULT_REFUND_ENABLED)))
            .andExpect(jsonPath("$.[*].refundPercent").value(hasItem(sameNumber(DEFAULT_REFUND_PERCENT))))
            .andExpect(jsonPath("$.[*].updatedAt").value(hasItem(DEFAULT_UPDATED_AT.toString())));
    }

    @Test
    @Transactional
    void getSurchargePolicy() throws Exception {
        // Initialize the database
        insertedSurchargePolicy = surchargePolicyRepository.saveAndFlush(surchargePolicy);

        // Get the surchargePolicy
        restSurchargePolicyMockMvc
            .perform(get(ENTITY_API_URL_ID, surchargePolicy.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(surchargePolicy.getId().intValue()))
            .andExpect(jsonPath("$.homeDeliveryEnabled").value(DEFAULT_HOME_DELIVERY_ENABLED))
            .andExpect(jsonPath("$.defaultHomeDeliveryAmount").value(sameNumber(DEFAULT_DEFAULT_HOME_DELIVERY_AMOUNT)))
            .andExpect(jsonPath("$.codEnabled").value(DEFAULT_COD_ENABLED))
            .andExpect(jsonPath("$.codPercent").value(sameNumber(DEFAULT_COD_PERCENT)))
            .andExpect(jsonPath("$.codMinFee").value(sameNumber(DEFAULT_COD_MIN_FEE)))
            .andExpect(jsonPath("$.storageEnabled").value(DEFAULT_STORAGE_ENABLED))
            .andExpect(jsonPath("$.storageFreeDays").value(DEFAULT_STORAGE_FREE_DAYS))
            .andExpect(jsonPath("$.storageFeePerDay").value(sameNumber(DEFAULT_STORAGE_FEE_PER_DAY)))
            .andExpect(jsonPath("$.insuranceEnabled").value(DEFAULT_INSURANCE_ENABLED))
            .andExpect(jsonPath("$.insuranceThreshold").value(sameNumber(DEFAULT_INSURANCE_THRESHOLD)))
            .andExpect(jsonPath("$.insurancePercentUnder").value(sameNumber(DEFAULT_INSURANCE_PERCENT_UNDER)))
            .andExpect(jsonPath("$.insurancePercentOver").value(sameNumber(DEFAULT_INSURANCE_PERCENT_OVER)))
            .andExpect(jsonPath("$.refundEnabled").value(DEFAULT_REFUND_ENABLED))
            .andExpect(jsonPath("$.refundPercent").value(sameNumber(DEFAULT_REFUND_PERCENT)))
            .andExpect(jsonPath("$.updatedAt").value(DEFAULT_UPDATED_AT.toString()));
    }

    @Test
    @Transactional
    void getNonExistingSurchargePolicy() throws Exception {
        // Get the surchargePolicy
        restSurchargePolicyMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingSurchargePolicy() throws Exception {
        // Initialize the database
        insertedSurchargePolicy = surchargePolicyRepository.saveAndFlush(surchargePolicy);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the surchargePolicy
        SurchargePolicy updatedSurchargePolicy = surchargePolicyRepository.findById(surchargePolicy.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedSurchargePolicy are not directly saved in db
        em.detach(updatedSurchargePolicy);
        updatedSurchargePolicy
            .homeDeliveryEnabled(UPDATED_HOME_DELIVERY_ENABLED)
            .defaultHomeDeliveryAmount(UPDATED_DEFAULT_HOME_DELIVERY_AMOUNT)
            .codEnabled(UPDATED_COD_ENABLED)
            .codPercent(UPDATED_COD_PERCENT)
            .codMinFee(UPDATED_COD_MIN_FEE)
            .storageEnabled(UPDATED_STORAGE_ENABLED)
            .storageFreeDays(UPDATED_STORAGE_FREE_DAYS)
            .storageFeePerDay(UPDATED_STORAGE_FEE_PER_DAY)
            .insuranceEnabled(UPDATED_INSURANCE_ENABLED)
            .insuranceThreshold(UPDATED_INSURANCE_THRESHOLD)
            .insurancePercentUnder(UPDATED_INSURANCE_PERCENT_UNDER)
            .insurancePercentOver(UPDATED_INSURANCE_PERCENT_OVER)
            .refundEnabled(UPDATED_REFUND_ENABLED)
            .refundPercent(UPDATED_REFUND_PERCENT)
            .updatedAt(UPDATED_UPDATED_AT);
        SurchargePolicyDTO surchargePolicyDTO = surchargePolicyMapper.toDto(updatedSurchargePolicy);

        restSurchargePolicyMockMvc
            .perform(
                put(ENTITY_API_URL_ID, surchargePolicyDTO.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(surchargePolicyDTO))
            )
            .andExpect(status().isOk());

        // Validate the SurchargePolicy in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedSurchargePolicyToMatchAllProperties(updatedSurchargePolicy);
    }

    @Test
    @Transactional
    void putNonExistingSurchargePolicy() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        surchargePolicy.setId(longCount.incrementAndGet());

        // Create the SurchargePolicy
        SurchargePolicyDTO surchargePolicyDTO = surchargePolicyMapper.toDto(surchargePolicy);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restSurchargePolicyMockMvc
            .perform(
                put(ENTITY_API_URL_ID, surchargePolicyDTO.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(surchargePolicyDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the SurchargePolicy in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchSurchargePolicy() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        surchargePolicy.setId(longCount.incrementAndGet());

        // Create the SurchargePolicy
        SurchargePolicyDTO surchargePolicyDTO = surchargePolicyMapper.toDto(surchargePolicy);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restSurchargePolicyMockMvc
            .perform(
                put(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(surchargePolicyDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the SurchargePolicy in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamSurchargePolicy() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        surchargePolicy.setId(longCount.incrementAndGet());

        // Create the SurchargePolicy
        SurchargePolicyDTO surchargePolicyDTO = surchargePolicyMapper.toDto(surchargePolicy);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restSurchargePolicyMockMvc
            .perform(put(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(surchargePolicyDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the SurchargePolicy in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateSurchargePolicyWithPatch() throws Exception {
        // Initialize the database
        insertedSurchargePolicy = surchargePolicyRepository.saveAndFlush(surchargePolicy);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the surchargePolicy using partial update
        SurchargePolicy partialUpdatedSurchargePolicy = new SurchargePolicy();
        partialUpdatedSurchargePolicy.setId(surchargePolicy.getId());

        partialUpdatedSurchargePolicy
            .defaultHomeDeliveryAmount(UPDATED_DEFAULT_HOME_DELIVERY_AMOUNT)
            .codEnabled(UPDATED_COD_ENABLED)
            .codPercent(UPDATED_COD_PERCENT)
            .codMinFee(UPDATED_COD_MIN_FEE)
            .storageEnabled(UPDATED_STORAGE_ENABLED)
            .refundEnabled(UPDATED_REFUND_ENABLED)
            .updatedAt(UPDATED_UPDATED_AT);

        restSurchargePolicyMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedSurchargePolicy.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedSurchargePolicy))
            )
            .andExpect(status().isOk());

        // Validate the SurchargePolicy in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertSurchargePolicyUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedSurchargePolicy, surchargePolicy),
            getPersistedSurchargePolicy(surchargePolicy)
        );
    }

    @Test
    @Transactional
    void fullUpdateSurchargePolicyWithPatch() throws Exception {
        // Initialize the database
        insertedSurchargePolicy = surchargePolicyRepository.saveAndFlush(surchargePolicy);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the surchargePolicy using partial update
        SurchargePolicy partialUpdatedSurchargePolicy = new SurchargePolicy();
        partialUpdatedSurchargePolicy.setId(surchargePolicy.getId());

        partialUpdatedSurchargePolicy
            .homeDeliveryEnabled(UPDATED_HOME_DELIVERY_ENABLED)
            .defaultHomeDeliveryAmount(UPDATED_DEFAULT_HOME_DELIVERY_AMOUNT)
            .codEnabled(UPDATED_COD_ENABLED)
            .codPercent(UPDATED_COD_PERCENT)
            .codMinFee(UPDATED_COD_MIN_FEE)
            .storageEnabled(UPDATED_STORAGE_ENABLED)
            .storageFreeDays(UPDATED_STORAGE_FREE_DAYS)
            .storageFeePerDay(UPDATED_STORAGE_FEE_PER_DAY)
            .insuranceEnabled(UPDATED_INSURANCE_ENABLED)
            .insuranceThreshold(UPDATED_INSURANCE_THRESHOLD)
            .insurancePercentUnder(UPDATED_INSURANCE_PERCENT_UNDER)
            .insurancePercentOver(UPDATED_INSURANCE_PERCENT_OVER)
            .refundEnabled(UPDATED_REFUND_ENABLED)
            .refundPercent(UPDATED_REFUND_PERCENT)
            .updatedAt(UPDATED_UPDATED_AT);

        restSurchargePolicyMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedSurchargePolicy.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedSurchargePolicy))
            )
            .andExpect(status().isOk());

        // Validate the SurchargePolicy in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertSurchargePolicyUpdatableFieldsEquals(
            partialUpdatedSurchargePolicy,
            getPersistedSurchargePolicy(partialUpdatedSurchargePolicy)
        );
    }

    @Test
    @Transactional
    void patchNonExistingSurchargePolicy() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        surchargePolicy.setId(longCount.incrementAndGet());

        // Create the SurchargePolicy
        SurchargePolicyDTO surchargePolicyDTO = surchargePolicyMapper.toDto(surchargePolicy);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restSurchargePolicyMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, surchargePolicyDTO.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(surchargePolicyDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the SurchargePolicy in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchSurchargePolicy() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        surchargePolicy.setId(longCount.incrementAndGet());

        // Create the SurchargePolicy
        SurchargePolicyDTO surchargePolicyDTO = surchargePolicyMapper.toDto(surchargePolicy);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restSurchargePolicyMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(surchargePolicyDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the SurchargePolicy in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamSurchargePolicy() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        surchargePolicy.setId(longCount.incrementAndGet());

        // Create the SurchargePolicy
        SurchargePolicyDTO surchargePolicyDTO = surchargePolicyMapper.toDto(surchargePolicy);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restSurchargePolicyMockMvc
            .perform(patch(ENTITY_API_URL).contentType("application/merge-patch+json").content(om.writeValueAsBytes(surchargePolicyDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the SurchargePolicy in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteSurchargePolicy() throws Exception {
        // Initialize the database
        insertedSurchargePolicy = surchargePolicyRepository.saveAndFlush(surchargePolicy);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the surchargePolicy
        restSurchargePolicyMockMvc
            .perform(delete(ENTITY_API_URL_ID, surchargePolicy.getId()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return surchargePolicyRepository.count();
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

    protected SurchargePolicy getPersistedSurchargePolicy(SurchargePolicy surchargePolicy) {
        return surchargePolicyRepository.findById(surchargePolicy.getId()).orElseThrow();
    }

    protected void assertPersistedSurchargePolicyToMatchAllProperties(SurchargePolicy expectedSurchargePolicy) {
        assertSurchargePolicyAllPropertiesEquals(expectedSurchargePolicy, getPersistedSurchargePolicy(expectedSurchargePolicy));
    }

    protected void assertPersistedSurchargePolicyToMatchUpdatableProperties(SurchargePolicy expectedSurchargePolicy) {
        assertSurchargePolicyAllUpdatablePropertiesEquals(expectedSurchargePolicy, getPersistedSurchargePolicy(expectedSurchargePolicy));
    }
}
