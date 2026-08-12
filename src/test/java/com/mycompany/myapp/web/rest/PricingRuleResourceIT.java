package com.mycompany.myapp.web.rest;

import static com.mycompany.myapp.domain.PricingRuleAsserts.*;
import static com.mycompany.myapp.web.rest.TestUtil.createUpdateProxyForBean;
import static com.mycompany.myapp.web.rest.TestUtil.sameNumber;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.myapp.IntegrationTest;
import com.mycompany.myapp.domain.PricingRule;
import com.mycompany.myapp.domain.Route;
import com.mycompany.myapp.repository.PricingRuleRepository;
import com.mycompany.myapp.service.PricingRuleService;
import com.mycompany.myapp.service.dto.PricingRuleDTO;
import com.mycompany.myapp.service.mapper.PricingRuleMapper;
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
 * Integration tests for the {@link PricingRuleResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser
class PricingRuleResourceIT {

    private static final String DEFAULT_RULE_CODE = "AAAAAAAAAA";
    private static final String UPDATED_RULE_CODE = "BBBBBBBBBB";

    private static final String DEFAULT_TIER_LABEL = "AAAAAAAAAA";
    private static final String UPDATED_TIER_LABEL = "BBBBBBBBBB";

    private static final BigDecimal DEFAULT_MIN_KG = new BigDecimal(0);
    private static final BigDecimal UPDATED_MIN_KG = new BigDecimal(1);
    private static final BigDecimal SMALLER_MIN_KG = new BigDecimal(0 - 1);

    private static final BigDecimal DEFAULT_MAX_KG = new BigDecimal(0);
    private static final BigDecimal UPDATED_MAX_KG = new BigDecimal(1);
    private static final BigDecimal SMALLER_MAX_KG = new BigDecimal(0 - 1);

    private static final BigDecimal DEFAULT_UNIT_PRICE = new BigDecimal(0);
    private static final BigDecimal UPDATED_UNIT_PRICE = new BigDecimal(1);
    private static final BigDecimal SMALLER_UNIT_PRICE = new BigDecimal(0 - 1);

    private static final BigDecimal DEFAULT_SURCHARGE_AMOUNT = new BigDecimal(0);
    private static final BigDecimal UPDATED_SURCHARGE_AMOUNT = new BigDecimal(1);
    private static final BigDecimal SMALLER_SURCHARGE_AMOUNT = new BigDecimal(0 - 1);

    private static final Integer DEFAULT_DIM_DIVISOR = 1;
    private static final Integer UPDATED_DIM_DIVISOR = 2;
    private static final Integer SMALLER_DIM_DIVISOR = 1 - 1;

    private static final BigDecimal DEFAULT_KM_MIN = new BigDecimal(0);
    private static final BigDecimal UPDATED_KM_MIN = new BigDecimal(1);
    private static final BigDecimal SMALLER_KM_MIN = new BigDecimal(0 - 1);

    private static final BigDecimal DEFAULT_KM_RATE = new BigDecimal(0);
    private static final BigDecimal UPDATED_KM_RATE = new BigDecimal(1);
    private static final BigDecimal SMALLER_KM_RATE = new BigDecimal(0 - 1);

    private static final Integer DEFAULT_STEP_GRAM = 0;
    private static final Integer UPDATED_STEP_GRAM = 1;
    private static final Integer SMALLER_STEP_GRAM = 0 - 1;

    private static final BigDecimal DEFAULT_ADD_FEE_AMOUNT = new BigDecimal(0);
    private static final BigDecimal UPDATED_ADD_FEE_AMOUNT = new BigDecimal(1);
    private static final BigDecimal SMALLER_ADD_FEE_AMOUNT = new BigDecimal(0 - 1);

    private static final Instant DEFAULT_EFFECTIVE_FROM = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_EFFECTIVE_FROM = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final Instant DEFAULT_EFFECTIVE_TO = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_EFFECTIVE_TO = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final Boolean DEFAULT_ACTIVE = false;
    private static final Boolean UPDATED_ACTIVE = true;

    private static final String ENTITY_API_URL = "/api/pricing-rules";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static Random random = new Random();
    private static AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    @Autowired
    private ObjectMapper om;

    @Autowired
    private PricingRuleRepository pricingRuleRepository;

    @Mock
    private PricingRuleRepository pricingRuleRepositoryMock;

    @Autowired
    private PricingRuleMapper pricingRuleMapper;

    @Mock
    private PricingRuleService pricingRuleServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restPricingRuleMockMvc;

    private PricingRule pricingRule;

    private PricingRule insertedPricingRule;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static PricingRule createEntity(EntityManager em) {
        PricingRule pricingRule = new PricingRule()
            .ruleCode(DEFAULT_RULE_CODE)
            .tierLabel(DEFAULT_TIER_LABEL)
            .minKg(DEFAULT_MIN_KG)
            .maxKg(DEFAULT_MAX_KG)
            .unitPrice(DEFAULT_UNIT_PRICE)
            .surchargeAmount(DEFAULT_SURCHARGE_AMOUNT)
            .dimDivisor(DEFAULT_DIM_DIVISOR)
            .kmMin(DEFAULT_KM_MIN)
            .kmRate(DEFAULT_KM_RATE)
            .stepGram(DEFAULT_STEP_GRAM)
            .addFeeAmount(DEFAULT_ADD_FEE_AMOUNT)
            .effectiveFrom(DEFAULT_EFFECTIVE_FROM)
            .effectiveTo(DEFAULT_EFFECTIVE_TO)
            .active(DEFAULT_ACTIVE);
        // Add required entity
        Route route;
        if (TestUtil.findAll(em, Route.class).isEmpty()) {
            route = RouteResourceIT.createEntity(em);
            em.persist(route);
            em.flush();
        } else {
            route = TestUtil.findAll(em, Route.class).get(0);
        }
        pricingRule.setRoute(route);
        return pricingRule;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static PricingRule createUpdatedEntity(EntityManager em) {
        PricingRule updatedPricingRule = new PricingRule()
            .ruleCode(UPDATED_RULE_CODE)
            .tierLabel(UPDATED_TIER_LABEL)
            .minKg(UPDATED_MIN_KG)
            .maxKg(UPDATED_MAX_KG)
            .unitPrice(UPDATED_UNIT_PRICE)
            .surchargeAmount(UPDATED_SURCHARGE_AMOUNT)
            .dimDivisor(UPDATED_DIM_DIVISOR)
            .kmMin(UPDATED_KM_MIN)
            .kmRate(UPDATED_KM_RATE)
            .stepGram(UPDATED_STEP_GRAM)
            .addFeeAmount(UPDATED_ADD_FEE_AMOUNT)
            .effectiveFrom(UPDATED_EFFECTIVE_FROM)
            .effectiveTo(UPDATED_EFFECTIVE_TO)
            .active(UPDATED_ACTIVE);
        // Add required entity
        Route route;
        if (TestUtil.findAll(em, Route.class).isEmpty()) {
            route = RouteResourceIT.createUpdatedEntity(em);
            em.persist(route);
            em.flush();
        } else {
            route = TestUtil.findAll(em, Route.class).get(0);
        }
        updatedPricingRule.setRoute(route);
        return updatedPricingRule;
    }

    @BeforeEach
    public void initTest() {
        pricingRule = createEntity(em);
    }

    @AfterEach
    public void cleanup() {
        if (insertedPricingRule != null) {
            pricingRuleRepository.delete(insertedPricingRule);
            insertedPricingRule = null;
        }
    }

    @Test
    @Transactional
    void createPricingRule() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the PricingRule
        PricingRuleDTO pricingRuleDTO = pricingRuleMapper.toDto(pricingRule);
        var returnedPricingRuleDTO = om.readValue(
            restPricingRuleMockMvc
                .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(pricingRuleDTO)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            PricingRuleDTO.class
        );

        // Validate the PricingRule in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedPricingRule = pricingRuleMapper.toEntity(returnedPricingRuleDTO);
        assertPricingRuleUpdatableFieldsEquals(returnedPricingRule, getPersistedPricingRule(returnedPricingRule));

        insertedPricingRule = returnedPricingRule;
    }

    @Test
    @Transactional
    void createPricingRuleWithExistingId() throws Exception {
        // Create the PricingRule with an existing ID
        pricingRule.setId(1L);
        PricingRuleDTO pricingRuleDTO = pricingRuleMapper.toDto(pricingRule);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restPricingRuleMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(pricingRuleDTO)))
            .andExpect(status().isBadRequest());

        // Validate the PricingRule in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkRuleCodeIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        pricingRule.setRuleCode(null);

        // Create the PricingRule, which fails.
        PricingRuleDTO pricingRuleDTO = pricingRuleMapper.toDto(pricingRule);

        restPricingRuleMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(pricingRuleDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkTierLabelIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        pricingRule.setTierLabel(null);

        // Create the PricingRule, which fails.
        PricingRuleDTO pricingRuleDTO = pricingRuleMapper.toDto(pricingRule);

        restPricingRuleMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(pricingRuleDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkMinKgIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        pricingRule.setMinKg(null);

        // Create the PricingRule, which fails.
        PricingRuleDTO pricingRuleDTO = pricingRuleMapper.toDto(pricingRule);

        restPricingRuleMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(pricingRuleDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkMaxKgIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        pricingRule.setMaxKg(null);

        // Create the PricingRule, which fails.
        PricingRuleDTO pricingRuleDTO = pricingRuleMapper.toDto(pricingRule);

        restPricingRuleMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(pricingRuleDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkUnitPriceIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        pricingRule.setUnitPrice(null);

        // Create the PricingRule, which fails.
        PricingRuleDTO pricingRuleDTO = pricingRuleMapper.toDto(pricingRule);

        restPricingRuleMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(pricingRuleDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkSurchargeAmountIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        pricingRule.setSurchargeAmount(null);

        // Create the PricingRule, which fails.
        PricingRuleDTO pricingRuleDTO = pricingRuleMapper.toDto(pricingRule);

        restPricingRuleMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(pricingRuleDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkEffectiveFromIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        pricingRule.setEffectiveFrom(null);

        // Create the PricingRule, which fails.
        PricingRuleDTO pricingRuleDTO = pricingRuleMapper.toDto(pricingRule);

        restPricingRuleMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(pricingRuleDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkActiveIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        pricingRule.setActive(null);

        // Create the PricingRule, which fails.
        PricingRuleDTO pricingRuleDTO = pricingRuleMapper.toDto(pricingRule);

        restPricingRuleMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(pricingRuleDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllPricingRules() throws Exception {
        // Initialize the database
        insertedPricingRule = pricingRuleRepository.saveAndFlush(pricingRule);

        // Get all the pricingRuleList
        restPricingRuleMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(pricingRule.getId().intValue())))
            .andExpect(jsonPath("$.[*].ruleCode").value(hasItem(DEFAULT_RULE_CODE)))
            .andExpect(jsonPath("$.[*].tierLabel").value(hasItem(DEFAULT_TIER_LABEL)))
            .andExpect(jsonPath("$.[*].minKg").value(hasItem(sameNumber(DEFAULT_MIN_KG))))
            .andExpect(jsonPath("$.[*].maxKg").value(hasItem(sameNumber(DEFAULT_MAX_KG))))
            .andExpect(jsonPath("$.[*].unitPrice").value(hasItem(sameNumber(DEFAULT_UNIT_PRICE))))
            .andExpect(jsonPath("$.[*].surchargeAmount").value(hasItem(sameNumber(DEFAULT_SURCHARGE_AMOUNT))))
            .andExpect(jsonPath("$.[*].dimDivisor").value(hasItem(DEFAULT_DIM_DIVISOR)))
            .andExpect(jsonPath("$.[*].kmMin").value(hasItem(sameNumber(DEFAULT_KM_MIN))))
            .andExpect(jsonPath("$.[*].kmRate").value(hasItem(sameNumber(DEFAULT_KM_RATE))))
            .andExpect(jsonPath("$.[*].stepGram").value(hasItem(DEFAULT_STEP_GRAM)))
            .andExpect(jsonPath("$.[*].addFeeAmount").value(hasItem(sameNumber(DEFAULT_ADD_FEE_AMOUNT))))
            .andExpect(jsonPath("$.[*].effectiveFrom").value(hasItem(DEFAULT_EFFECTIVE_FROM.toString())))
            .andExpect(jsonPath("$.[*].effectiveTo").value(hasItem(DEFAULT_EFFECTIVE_TO.toString())))
            .andExpect(jsonPath("$.[*].active").value(hasItem(DEFAULT_ACTIVE)));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllPricingRulesWithEagerRelationshipsIsEnabled() throws Exception {
        when(pricingRuleServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restPricingRuleMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(pricingRuleServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllPricingRulesWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(pricingRuleServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restPricingRuleMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(pricingRuleRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getPricingRule() throws Exception {
        // Initialize the database
        insertedPricingRule = pricingRuleRepository.saveAndFlush(pricingRule);

        // Get the pricingRule
        restPricingRuleMockMvc
            .perform(get(ENTITY_API_URL_ID, pricingRule.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(pricingRule.getId().intValue()))
            .andExpect(jsonPath("$.ruleCode").value(DEFAULT_RULE_CODE))
            .andExpect(jsonPath("$.tierLabel").value(DEFAULT_TIER_LABEL))
            .andExpect(jsonPath("$.minKg").value(sameNumber(DEFAULT_MIN_KG)))
            .andExpect(jsonPath("$.maxKg").value(sameNumber(DEFAULT_MAX_KG)))
            .andExpect(jsonPath("$.unitPrice").value(sameNumber(DEFAULT_UNIT_PRICE)))
            .andExpect(jsonPath("$.surchargeAmount").value(sameNumber(DEFAULT_SURCHARGE_AMOUNT)))
            .andExpect(jsonPath("$.dimDivisor").value(DEFAULT_DIM_DIVISOR))
            .andExpect(jsonPath("$.kmMin").value(sameNumber(DEFAULT_KM_MIN)))
            .andExpect(jsonPath("$.kmRate").value(sameNumber(DEFAULT_KM_RATE)))
            .andExpect(jsonPath("$.stepGram").value(DEFAULT_STEP_GRAM))
            .andExpect(jsonPath("$.addFeeAmount").value(sameNumber(DEFAULT_ADD_FEE_AMOUNT)))
            .andExpect(jsonPath("$.effectiveFrom").value(DEFAULT_EFFECTIVE_FROM.toString()))
            .andExpect(jsonPath("$.effectiveTo").value(DEFAULT_EFFECTIVE_TO.toString()))
            .andExpect(jsonPath("$.active").value(DEFAULT_ACTIVE));
    }

    @Test
    @Transactional
    void getPricingRulesByIdFiltering() throws Exception {
        // Initialize the database
        insertedPricingRule = pricingRuleRepository.saveAndFlush(pricingRule);

        Long id = pricingRule.getId();

        defaultPricingRuleFiltering("id.equals=" + id, "id.notEquals=" + id);

        defaultPricingRuleFiltering("id.greaterThanOrEqual=" + id, "id.greaterThan=" + id);

        defaultPricingRuleFiltering("id.lessThanOrEqual=" + id, "id.lessThan=" + id);
    }

    @Test
    @Transactional
    void getAllPricingRulesByRuleCodeIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedPricingRule = pricingRuleRepository.saveAndFlush(pricingRule);

        // Get all the pricingRuleList where ruleCode equals to
        defaultPricingRuleFiltering("ruleCode.equals=" + DEFAULT_RULE_CODE, "ruleCode.equals=" + UPDATED_RULE_CODE);
    }

    @Test
    @Transactional
    void getAllPricingRulesByRuleCodeIsInShouldWork() throws Exception {
        // Initialize the database
        insertedPricingRule = pricingRuleRepository.saveAndFlush(pricingRule);

        // Get all the pricingRuleList where ruleCode in
        defaultPricingRuleFiltering("ruleCode.in=" + DEFAULT_RULE_CODE + "," + UPDATED_RULE_CODE, "ruleCode.in=" + UPDATED_RULE_CODE);
    }

    @Test
    @Transactional
    void getAllPricingRulesByRuleCodeIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedPricingRule = pricingRuleRepository.saveAndFlush(pricingRule);

        // Get all the pricingRuleList where ruleCode is not null
        defaultPricingRuleFiltering("ruleCode.specified=true", "ruleCode.specified=false");
    }

    @Test
    @Transactional
    void getAllPricingRulesByRuleCodeContainsSomething() throws Exception {
        // Initialize the database
        insertedPricingRule = pricingRuleRepository.saveAndFlush(pricingRule);

        // Get all the pricingRuleList where ruleCode contains
        defaultPricingRuleFiltering("ruleCode.contains=" + DEFAULT_RULE_CODE, "ruleCode.contains=" + UPDATED_RULE_CODE);
    }

    @Test
    @Transactional
    void getAllPricingRulesByRuleCodeNotContainsSomething() throws Exception {
        // Initialize the database
        insertedPricingRule = pricingRuleRepository.saveAndFlush(pricingRule);

        // Get all the pricingRuleList where ruleCode does not contain
        defaultPricingRuleFiltering("ruleCode.doesNotContain=" + UPDATED_RULE_CODE, "ruleCode.doesNotContain=" + DEFAULT_RULE_CODE);
    }

    @Test
    @Transactional
    void getAllPricingRulesByTierLabelIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedPricingRule = pricingRuleRepository.saveAndFlush(pricingRule);

        // Get all the pricingRuleList where tierLabel equals to
        defaultPricingRuleFiltering("tierLabel.equals=" + DEFAULT_TIER_LABEL, "tierLabel.equals=" + UPDATED_TIER_LABEL);
    }

    @Test
    @Transactional
    void getAllPricingRulesByTierLabelIsInShouldWork() throws Exception {
        // Initialize the database
        insertedPricingRule = pricingRuleRepository.saveAndFlush(pricingRule);

        // Get all the pricingRuleList where tierLabel in
        defaultPricingRuleFiltering("tierLabel.in=" + DEFAULT_TIER_LABEL + "," + UPDATED_TIER_LABEL, "tierLabel.in=" + UPDATED_TIER_LABEL);
    }

    @Test
    @Transactional
    void getAllPricingRulesByTierLabelIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedPricingRule = pricingRuleRepository.saveAndFlush(pricingRule);

        // Get all the pricingRuleList where tierLabel is not null
        defaultPricingRuleFiltering("tierLabel.specified=true", "tierLabel.specified=false");
    }

    @Test
    @Transactional
    void getAllPricingRulesByTierLabelContainsSomething() throws Exception {
        // Initialize the database
        insertedPricingRule = pricingRuleRepository.saveAndFlush(pricingRule);

        // Get all the pricingRuleList where tierLabel contains
        defaultPricingRuleFiltering("tierLabel.contains=" + DEFAULT_TIER_LABEL, "tierLabel.contains=" + UPDATED_TIER_LABEL);
    }

    @Test
    @Transactional
    void getAllPricingRulesByTierLabelNotContainsSomething() throws Exception {
        // Initialize the database
        insertedPricingRule = pricingRuleRepository.saveAndFlush(pricingRule);

        // Get all the pricingRuleList where tierLabel does not contain
        defaultPricingRuleFiltering("tierLabel.doesNotContain=" + UPDATED_TIER_LABEL, "tierLabel.doesNotContain=" + DEFAULT_TIER_LABEL);
    }

    @Test
    @Transactional
    void getAllPricingRulesByMinKgIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedPricingRule = pricingRuleRepository.saveAndFlush(pricingRule);

        // Get all the pricingRuleList where minKg equals to
        defaultPricingRuleFiltering("minKg.equals=" + DEFAULT_MIN_KG, "minKg.equals=" + UPDATED_MIN_KG);
    }

    @Test
    @Transactional
    void getAllPricingRulesByMinKgIsInShouldWork() throws Exception {
        // Initialize the database
        insertedPricingRule = pricingRuleRepository.saveAndFlush(pricingRule);

        // Get all the pricingRuleList where minKg in
        defaultPricingRuleFiltering("minKg.in=" + DEFAULT_MIN_KG + "," + UPDATED_MIN_KG, "minKg.in=" + UPDATED_MIN_KG);
    }

    @Test
    @Transactional
    void getAllPricingRulesByMinKgIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedPricingRule = pricingRuleRepository.saveAndFlush(pricingRule);

        // Get all the pricingRuleList where minKg is not null
        defaultPricingRuleFiltering("minKg.specified=true", "minKg.specified=false");
    }

    @Test
    @Transactional
    void getAllPricingRulesByMinKgIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedPricingRule = pricingRuleRepository.saveAndFlush(pricingRule);

        // Get all the pricingRuleList where minKg is greater than or equal to
        defaultPricingRuleFiltering("minKg.greaterThanOrEqual=" + DEFAULT_MIN_KG, "minKg.greaterThanOrEqual=" + UPDATED_MIN_KG);
    }

    @Test
    @Transactional
    void getAllPricingRulesByMinKgIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedPricingRule = pricingRuleRepository.saveAndFlush(pricingRule);

        // Get all the pricingRuleList where minKg is less than or equal to
        defaultPricingRuleFiltering("minKg.lessThanOrEqual=" + DEFAULT_MIN_KG, "minKg.lessThanOrEqual=" + SMALLER_MIN_KG);
    }

    @Test
    @Transactional
    void getAllPricingRulesByMinKgIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedPricingRule = pricingRuleRepository.saveAndFlush(pricingRule);

        // Get all the pricingRuleList where minKg is less than
        defaultPricingRuleFiltering("minKg.lessThan=" + UPDATED_MIN_KG, "minKg.lessThan=" + DEFAULT_MIN_KG);
    }

    @Test
    @Transactional
    void getAllPricingRulesByMinKgIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedPricingRule = pricingRuleRepository.saveAndFlush(pricingRule);

        // Get all the pricingRuleList where minKg is greater than
        defaultPricingRuleFiltering("minKg.greaterThan=" + SMALLER_MIN_KG, "minKg.greaterThan=" + DEFAULT_MIN_KG);
    }

    @Test
    @Transactional
    void getAllPricingRulesByMaxKgIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedPricingRule = pricingRuleRepository.saveAndFlush(pricingRule);

        // Get all the pricingRuleList where maxKg equals to
        defaultPricingRuleFiltering("maxKg.equals=" + DEFAULT_MAX_KG, "maxKg.equals=" + UPDATED_MAX_KG);
    }

    @Test
    @Transactional
    void getAllPricingRulesByMaxKgIsInShouldWork() throws Exception {
        // Initialize the database
        insertedPricingRule = pricingRuleRepository.saveAndFlush(pricingRule);

        // Get all the pricingRuleList where maxKg in
        defaultPricingRuleFiltering("maxKg.in=" + DEFAULT_MAX_KG + "," + UPDATED_MAX_KG, "maxKg.in=" + UPDATED_MAX_KG);
    }

    @Test
    @Transactional
    void getAllPricingRulesByMaxKgIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedPricingRule = pricingRuleRepository.saveAndFlush(pricingRule);

        // Get all the pricingRuleList where maxKg is not null
        defaultPricingRuleFiltering("maxKg.specified=true", "maxKg.specified=false");
    }

    @Test
    @Transactional
    void getAllPricingRulesByMaxKgIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedPricingRule = pricingRuleRepository.saveAndFlush(pricingRule);

        // Get all the pricingRuleList where maxKg is greater than or equal to
        defaultPricingRuleFiltering("maxKg.greaterThanOrEqual=" + DEFAULT_MAX_KG, "maxKg.greaterThanOrEqual=" + UPDATED_MAX_KG);
    }

    @Test
    @Transactional
    void getAllPricingRulesByMaxKgIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedPricingRule = pricingRuleRepository.saveAndFlush(pricingRule);

        // Get all the pricingRuleList where maxKg is less than or equal to
        defaultPricingRuleFiltering("maxKg.lessThanOrEqual=" + DEFAULT_MAX_KG, "maxKg.lessThanOrEqual=" + SMALLER_MAX_KG);
    }

    @Test
    @Transactional
    void getAllPricingRulesByMaxKgIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedPricingRule = pricingRuleRepository.saveAndFlush(pricingRule);

        // Get all the pricingRuleList where maxKg is less than
        defaultPricingRuleFiltering("maxKg.lessThan=" + UPDATED_MAX_KG, "maxKg.lessThan=" + DEFAULT_MAX_KG);
    }

    @Test
    @Transactional
    void getAllPricingRulesByMaxKgIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedPricingRule = pricingRuleRepository.saveAndFlush(pricingRule);

        // Get all the pricingRuleList where maxKg is greater than
        defaultPricingRuleFiltering("maxKg.greaterThan=" + SMALLER_MAX_KG, "maxKg.greaterThan=" + DEFAULT_MAX_KG);
    }

    @Test
    @Transactional
    void getAllPricingRulesByUnitPriceIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedPricingRule = pricingRuleRepository.saveAndFlush(pricingRule);

        // Get all the pricingRuleList where unitPrice equals to
        defaultPricingRuleFiltering("unitPrice.equals=" + DEFAULT_UNIT_PRICE, "unitPrice.equals=" + UPDATED_UNIT_PRICE);
    }

    @Test
    @Transactional
    void getAllPricingRulesByUnitPriceIsInShouldWork() throws Exception {
        // Initialize the database
        insertedPricingRule = pricingRuleRepository.saveAndFlush(pricingRule);

        // Get all the pricingRuleList where unitPrice in
        defaultPricingRuleFiltering("unitPrice.in=" + DEFAULT_UNIT_PRICE + "," + UPDATED_UNIT_PRICE, "unitPrice.in=" + UPDATED_UNIT_PRICE);
    }

    @Test
    @Transactional
    void getAllPricingRulesByUnitPriceIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedPricingRule = pricingRuleRepository.saveAndFlush(pricingRule);

        // Get all the pricingRuleList where unitPrice is not null
        defaultPricingRuleFiltering("unitPrice.specified=true", "unitPrice.specified=false");
    }

    @Test
    @Transactional
    void getAllPricingRulesByUnitPriceIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedPricingRule = pricingRuleRepository.saveAndFlush(pricingRule);

        // Get all the pricingRuleList where unitPrice is greater than or equal to
        defaultPricingRuleFiltering(
            "unitPrice.greaterThanOrEqual=" + DEFAULT_UNIT_PRICE,
            "unitPrice.greaterThanOrEqual=" + UPDATED_UNIT_PRICE
        );
    }

    @Test
    @Transactional
    void getAllPricingRulesByUnitPriceIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedPricingRule = pricingRuleRepository.saveAndFlush(pricingRule);

        // Get all the pricingRuleList where unitPrice is less than or equal to
        defaultPricingRuleFiltering("unitPrice.lessThanOrEqual=" + DEFAULT_UNIT_PRICE, "unitPrice.lessThanOrEqual=" + SMALLER_UNIT_PRICE);
    }

    @Test
    @Transactional
    void getAllPricingRulesByUnitPriceIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedPricingRule = pricingRuleRepository.saveAndFlush(pricingRule);

        // Get all the pricingRuleList where unitPrice is less than
        defaultPricingRuleFiltering("unitPrice.lessThan=" + UPDATED_UNIT_PRICE, "unitPrice.lessThan=" + DEFAULT_UNIT_PRICE);
    }

    @Test
    @Transactional
    void getAllPricingRulesByUnitPriceIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedPricingRule = pricingRuleRepository.saveAndFlush(pricingRule);

        // Get all the pricingRuleList where unitPrice is greater than
        defaultPricingRuleFiltering("unitPrice.greaterThan=" + SMALLER_UNIT_PRICE, "unitPrice.greaterThan=" + DEFAULT_UNIT_PRICE);
    }

    @Test
    @Transactional
    void getAllPricingRulesBySurchargeAmountIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedPricingRule = pricingRuleRepository.saveAndFlush(pricingRule);

        // Get all the pricingRuleList where surchargeAmount equals to
        defaultPricingRuleFiltering(
            "surchargeAmount.equals=" + DEFAULT_SURCHARGE_AMOUNT,
            "surchargeAmount.equals=" + UPDATED_SURCHARGE_AMOUNT
        );
    }

    @Test
    @Transactional
    void getAllPricingRulesBySurchargeAmountIsInShouldWork() throws Exception {
        // Initialize the database
        insertedPricingRule = pricingRuleRepository.saveAndFlush(pricingRule);

        // Get all the pricingRuleList where surchargeAmount in
        defaultPricingRuleFiltering(
            "surchargeAmount.in=" + DEFAULT_SURCHARGE_AMOUNT + "," + UPDATED_SURCHARGE_AMOUNT,
            "surchargeAmount.in=" + UPDATED_SURCHARGE_AMOUNT
        );
    }

    @Test
    @Transactional
    void getAllPricingRulesBySurchargeAmountIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedPricingRule = pricingRuleRepository.saveAndFlush(pricingRule);

        // Get all the pricingRuleList where surchargeAmount is not null
        defaultPricingRuleFiltering("surchargeAmount.specified=true", "surchargeAmount.specified=false");
    }

    @Test
    @Transactional
    void getAllPricingRulesBySurchargeAmountIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedPricingRule = pricingRuleRepository.saveAndFlush(pricingRule);

        // Get all the pricingRuleList where surchargeAmount is greater than or equal to
        defaultPricingRuleFiltering(
            "surchargeAmount.greaterThanOrEqual=" + DEFAULT_SURCHARGE_AMOUNT,
            "surchargeAmount.greaterThanOrEqual=" + UPDATED_SURCHARGE_AMOUNT
        );
    }

    @Test
    @Transactional
    void getAllPricingRulesBySurchargeAmountIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedPricingRule = pricingRuleRepository.saveAndFlush(pricingRule);

        // Get all the pricingRuleList where surchargeAmount is less than or equal to
        defaultPricingRuleFiltering(
            "surchargeAmount.lessThanOrEqual=" + DEFAULT_SURCHARGE_AMOUNT,
            "surchargeAmount.lessThanOrEqual=" + SMALLER_SURCHARGE_AMOUNT
        );
    }

    @Test
    @Transactional
    void getAllPricingRulesBySurchargeAmountIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedPricingRule = pricingRuleRepository.saveAndFlush(pricingRule);

        // Get all the pricingRuleList where surchargeAmount is less than
        defaultPricingRuleFiltering(
            "surchargeAmount.lessThan=" + UPDATED_SURCHARGE_AMOUNT,
            "surchargeAmount.lessThan=" + DEFAULT_SURCHARGE_AMOUNT
        );
    }

    @Test
    @Transactional
    void getAllPricingRulesBySurchargeAmountIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedPricingRule = pricingRuleRepository.saveAndFlush(pricingRule);

        // Get all the pricingRuleList where surchargeAmount is greater than
        defaultPricingRuleFiltering(
            "surchargeAmount.greaterThan=" + SMALLER_SURCHARGE_AMOUNT,
            "surchargeAmount.greaterThan=" + DEFAULT_SURCHARGE_AMOUNT
        );
    }

    @Test
    @Transactional
    void getAllPricingRulesByDimDivisorIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedPricingRule = pricingRuleRepository.saveAndFlush(pricingRule);

        // Get all the pricingRuleList where dimDivisor equals to
        defaultPricingRuleFiltering("dimDivisor.equals=" + DEFAULT_DIM_DIVISOR, "dimDivisor.equals=" + UPDATED_DIM_DIVISOR);
    }

    @Test
    @Transactional
    void getAllPricingRulesByDimDivisorIsInShouldWork() throws Exception {
        // Initialize the database
        insertedPricingRule = pricingRuleRepository.saveAndFlush(pricingRule);

        // Get all the pricingRuleList where dimDivisor in
        defaultPricingRuleFiltering(
            "dimDivisor.in=" + DEFAULT_DIM_DIVISOR + "," + UPDATED_DIM_DIVISOR,
            "dimDivisor.in=" + UPDATED_DIM_DIVISOR
        );
    }

    @Test
    @Transactional
    void getAllPricingRulesByDimDivisorIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedPricingRule = pricingRuleRepository.saveAndFlush(pricingRule);

        // Get all the pricingRuleList where dimDivisor is not null
        defaultPricingRuleFiltering("dimDivisor.specified=true", "dimDivisor.specified=false");
    }

    @Test
    @Transactional
    void getAllPricingRulesByDimDivisorIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedPricingRule = pricingRuleRepository.saveAndFlush(pricingRule);

        // Get all the pricingRuleList where dimDivisor is greater than or equal to
        defaultPricingRuleFiltering(
            "dimDivisor.greaterThanOrEqual=" + DEFAULT_DIM_DIVISOR,
            "dimDivisor.greaterThanOrEqual=" + UPDATED_DIM_DIVISOR
        );
    }

    @Test
    @Transactional
    void getAllPricingRulesByDimDivisorIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedPricingRule = pricingRuleRepository.saveAndFlush(pricingRule);

        // Get all the pricingRuleList where dimDivisor is less than or equal to
        defaultPricingRuleFiltering(
            "dimDivisor.lessThanOrEqual=" + DEFAULT_DIM_DIVISOR,
            "dimDivisor.lessThanOrEqual=" + SMALLER_DIM_DIVISOR
        );
    }

    @Test
    @Transactional
    void getAllPricingRulesByDimDivisorIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedPricingRule = pricingRuleRepository.saveAndFlush(pricingRule);

        // Get all the pricingRuleList where dimDivisor is less than
        defaultPricingRuleFiltering("dimDivisor.lessThan=" + UPDATED_DIM_DIVISOR, "dimDivisor.lessThan=" + DEFAULT_DIM_DIVISOR);
    }

    @Test
    @Transactional
    void getAllPricingRulesByDimDivisorIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedPricingRule = pricingRuleRepository.saveAndFlush(pricingRule);

        // Get all the pricingRuleList where dimDivisor is greater than
        defaultPricingRuleFiltering("dimDivisor.greaterThan=" + SMALLER_DIM_DIVISOR, "dimDivisor.greaterThan=" + DEFAULT_DIM_DIVISOR);
    }

    @Test
    @Transactional
    void getAllPricingRulesByKmMinIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedPricingRule = pricingRuleRepository.saveAndFlush(pricingRule);

        // Get all the pricingRuleList where kmMin equals to
        defaultPricingRuleFiltering("kmMin.equals=" + DEFAULT_KM_MIN, "kmMin.equals=" + UPDATED_KM_MIN);
    }

    @Test
    @Transactional
    void getAllPricingRulesByKmMinIsInShouldWork() throws Exception {
        // Initialize the database
        insertedPricingRule = pricingRuleRepository.saveAndFlush(pricingRule);

        // Get all the pricingRuleList where kmMin in
        defaultPricingRuleFiltering("kmMin.in=" + DEFAULT_KM_MIN + "," + UPDATED_KM_MIN, "kmMin.in=" + UPDATED_KM_MIN);
    }

    @Test
    @Transactional
    void getAllPricingRulesByKmMinIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedPricingRule = pricingRuleRepository.saveAndFlush(pricingRule);

        // Get all the pricingRuleList where kmMin is not null
        defaultPricingRuleFiltering("kmMin.specified=true", "kmMin.specified=false");
    }

    @Test
    @Transactional
    void getAllPricingRulesByKmMinIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedPricingRule = pricingRuleRepository.saveAndFlush(pricingRule);

        // Get all the pricingRuleList where kmMin is greater than or equal to
        defaultPricingRuleFiltering("kmMin.greaterThanOrEqual=" + DEFAULT_KM_MIN, "kmMin.greaterThanOrEqual=" + UPDATED_KM_MIN);
    }

    @Test
    @Transactional
    void getAllPricingRulesByKmMinIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedPricingRule = pricingRuleRepository.saveAndFlush(pricingRule);

        // Get all the pricingRuleList where kmMin is less than or equal to
        defaultPricingRuleFiltering("kmMin.lessThanOrEqual=" + DEFAULT_KM_MIN, "kmMin.lessThanOrEqual=" + SMALLER_KM_MIN);
    }

    @Test
    @Transactional
    void getAllPricingRulesByKmMinIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedPricingRule = pricingRuleRepository.saveAndFlush(pricingRule);

        // Get all the pricingRuleList where kmMin is less than
        defaultPricingRuleFiltering("kmMin.lessThan=" + UPDATED_KM_MIN, "kmMin.lessThan=" + DEFAULT_KM_MIN);
    }

    @Test
    @Transactional
    void getAllPricingRulesByKmMinIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedPricingRule = pricingRuleRepository.saveAndFlush(pricingRule);

        // Get all the pricingRuleList where kmMin is greater than
        defaultPricingRuleFiltering("kmMin.greaterThan=" + SMALLER_KM_MIN, "kmMin.greaterThan=" + DEFAULT_KM_MIN);
    }

    @Test
    @Transactional
    void getAllPricingRulesByKmRateIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedPricingRule = pricingRuleRepository.saveAndFlush(pricingRule);

        // Get all the pricingRuleList where kmRate equals to
        defaultPricingRuleFiltering("kmRate.equals=" + DEFAULT_KM_RATE, "kmRate.equals=" + UPDATED_KM_RATE);
    }

    @Test
    @Transactional
    void getAllPricingRulesByKmRateIsInShouldWork() throws Exception {
        // Initialize the database
        insertedPricingRule = pricingRuleRepository.saveAndFlush(pricingRule);

        // Get all the pricingRuleList where kmRate in
        defaultPricingRuleFiltering("kmRate.in=" + DEFAULT_KM_RATE + "," + UPDATED_KM_RATE, "kmRate.in=" + UPDATED_KM_RATE);
    }

    @Test
    @Transactional
    void getAllPricingRulesByKmRateIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedPricingRule = pricingRuleRepository.saveAndFlush(pricingRule);

        // Get all the pricingRuleList where kmRate is not null
        defaultPricingRuleFiltering("kmRate.specified=true", "kmRate.specified=false");
    }

    @Test
    @Transactional
    void getAllPricingRulesByKmRateIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedPricingRule = pricingRuleRepository.saveAndFlush(pricingRule);

        // Get all the pricingRuleList where kmRate is greater than or equal to
        defaultPricingRuleFiltering("kmRate.greaterThanOrEqual=" + DEFAULT_KM_RATE, "kmRate.greaterThanOrEqual=" + UPDATED_KM_RATE);
    }

    @Test
    @Transactional
    void getAllPricingRulesByKmRateIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedPricingRule = pricingRuleRepository.saveAndFlush(pricingRule);

        // Get all the pricingRuleList where kmRate is less than or equal to
        defaultPricingRuleFiltering("kmRate.lessThanOrEqual=" + DEFAULT_KM_RATE, "kmRate.lessThanOrEqual=" + SMALLER_KM_RATE);
    }

    @Test
    @Transactional
    void getAllPricingRulesByKmRateIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedPricingRule = pricingRuleRepository.saveAndFlush(pricingRule);

        // Get all the pricingRuleList where kmRate is less than
        defaultPricingRuleFiltering("kmRate.lessThan=" + UPDATED_KM_RATE, "kmRate.lessThan=" + DEFAULT_KM_RATE);
    }

    @Test
    @Transactional
    void getAllPricingRulesByKmRateIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedPricingRule = pricingRuleRepository.saveAndFlush(pricingRule);

        // Get all the pricingRuleList where kmRate is greater than
        defaultPricingRuleFiltering("kmRate.greaterThan=" + SMALLER_KM_RATE, "kmRate.greaterThan=" + DEFAULT_KM_RATE);
    }

    @Test
    @Transactional
    void getAllPricingRulesByStepGramIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedPricingRule = pricingRuleRepository.saveAndFlush(pricingRule);

        // Get all the pricingRuleList where stepGram equals to
        defaultPricingRuleFiltering("stepGram.equals=" + DEFAULT_STEP_GRAM, "stepGram.equals=" + UPDATED_STEP_GRAM);
    }

    @Test
    @Transactional
    void getAllPricingRulesByStepGramIsInShouldWork() throws Exception {
        // Initialize the database
        insertedPricingRule = pricingRuleRepository.saveAndFlush(pricingRule);

        // Get all the pricingRuleList where stepGram in
        defaultPricingRuleFiltering("stepGram.in=" + DEFAULT_STEP_GRAM + "," + UPDATED_STEP_GRAM, "stepGram.in=" + UPDATED_STEP_GRAM);
    }

    @Test
    @Transactional
    void getAllPricingRulesByStepGramIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedPricingRule = pricingRuleRepository.saveAndFlush(pricingRule);

        // Get all the pricingRuleList where stepGram is not null
        defaultPricingRuleFiltering("stepGram.specified=true", "stepGram.specified=false");
    }

    @Test
    @Transactional
    void getAllPricingRulesByStepGramIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedPricingRule = pricingRuleRepository.saveAndFlush(pricingRule);

        // Get all the pricingRuleList where stepGram is greater than or equal to
        defaultPricingRuleFiltering("stepGram.greaterThanOrEqual=" + DEFAULT_STEP_GRAM, "stepGram.greaterThanOrEqual=" + UPDATED_STEP_GRAM);
    }

    @Test
    @Transactional
    void getAllPricingRulesByStepGramIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedPricingRule = pricingRuleRepository.saveAndFlush(pricingRule);

        // Get all the pricingRuleList where stepGram is less than or equal to
        defaultPricingRuleFiltering("stepGram.lessThanOrEqual=" + DEFAULT_STEP_GRAM, "stepGram.lessThanOrEqual=" + SMALLER_STEP_GRAM);
    }

    @Test
    @Transactional
    void getAllPricingRulesByStepGramIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedPricingRule = pricingRuleRepository.saveAndFlush(pricingRule);

        // Get all the pricingRuleList where stepGram is less than
        defaultPricingRuleFiltering("stepGram.lessThan=" + UPDATED_STEP_GRAM, "stepGram.lessThan=" + DEFAULT_STEP_GRAM);
    }

    @Test
    @Transactional
    void getAllPricingRulesByStepGramIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedPricingRule = pricingRuleRepository.saveAndFlush(pricingRule);

        // Get all the pricingRuleList where stepGram is greater than
        defaultPricingRuleFiltering("stepGram.greaterThan=" + SMALLER_STEP_GRAM, "stepGram.greaterThan=" + DEFAULT_STEP_GRAM);
    }

    @Test
    @Transactional
    void getAllPricingRulesByAddFeeAmountIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedPricingRule = pricingRuleRepository.saveAndFlush(pricingRule);

        // Get all the pricingRuleList where addFeeAmount equals to
        defaultPricingRuleFiltering("addFeeAmount.equals=" + DEFAULT_ADD_FEE_AMOUNT, "addFeeAmount.equals=" + UPDATED_ADD_FEE_AMOUNT);
    }

    @Test
    @Transactional
    void getAllPricingRulesByAddFeeAmountIsInShouldWork() throws Exception {
        // Initialize the database
        insertedPricingRule = pricingRuleRepository.saveAndFlush(pricingRule);

        // Get all the pricingRuleList where addFeeAmount in
        defaultPricingRuleFiltering(
            "addFeeAmount.in=" + DEFAULT_ADD_FEE_AMOUNT + "," + UPDATED_ADD_FEE_AMOUNT,
            "addFeeAmount.in=" + UPDATED_ADD_FEE_AMOUNT
        );
    }

    @Test
    @Transactional
    void getAllPricingRulesByAddFeeAmountIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedPricingRule = pricingRuleRepository.saveAndFlush(pricingRule);

        // Get all the pricingRuleList where addFeeAmount is not null
        defaultPricingRuleFiltering("addFeeAmount.specified=true", "addFeeAmount.specified=false");
    }

    @Test
    @Transactional
    void getAllPricingRulesByAddFeeAmountIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedPricingRule = pricingRuleRepository.saveAndFlush(pricingRule);

        // Get all the pricingRuleList where addFeeAmount is greater than or equal to
        defaultPricingRuleFiltering(
            "addFeeAmount.greaterThanOrEqual=" + DEFAULT_ADD_FEE_AMOUNT,
            "addFeeAmount.greaterThanOrEqual=" + UPDATED_ADD_FEE_AMOUNT
        );
    }

    @Test
    @Transactional
    void getAllPricingRulesByAddFeeAmountIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedPricingRule = pricingRuleRepository.saveAndFlush(pricingRule);

        // Get all the pricingRuleList where addFeeAmount is less than or equal to
        defaultPricingRuleFiltering(
            "addFeeAmount.lessThanOrEqual=" + DEFAULT_ADD_FEE_AMOUNT,
            "addFeeAmount.lessThanOrEqual=" + SMALLER_ADD_FEE_AMOUNT
        );
    }

    @Test
    @Transactional
    void getAllPricingRulesByAddFeeAmountIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedPricingRule = pricingRuleRepository.saveAndFlush(pricingRule);

        // Get all the pricingRuleList where addFeeAmount is less than
        defaultPricingRuleFiltering("addFeeAmount.lessThan=" + UPDATED_ADD_FEE_AMOUNT, "addFeeAmount.lessThan=" + DEFAULT_ADD_FEE_AMOUNT);
    }

    @Test
    @Transactional
    void getAllPricingRulesByAddFeeAmountIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedPricingRule = pricingRuleRepository.saveAndFlush(pricingRule);

        // Get all the pricingRuleList where addFeeAmount is greater than
        defaultPricingRuleFiltering(
            "addFeeAmount.greaterThan=" + SMALLER_ADD_FEE_AMOUNT,
            "addFeeAmount.greaterThan=" + DEFAULT_ADD_FEE_AMOUNT
        );
    }

    @Test
    @Transactional
    void getAllPricingRulesByEffectiveFromIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedPricingRule = pricingRuleRepository.saveAndFlush(pricingRule);

        // Get all the pricingRuleList where effectiveFrom equals to
        defaultPricingRuleFiltering("effectiveFrom.equals=" + DEFAULT_EFFECTIVE_FROM, "effectiveFrom.equals=" + UPDATED_EFFECTIVE_FROM);
    }

    @Test
    @Transactional
    void getAllPricingRulesByEffectiveFromIsInShouldWork() throws Exception {
        // Initialize the database
        insertedPricingRule = pricingRuleRepository.saveAndFlush(pricingRule);

        // Get all the pricingRuleList where effectiveFrom in
        defaultPricingRuleFiltering(
            "effectiveFrom.in=" + DEFAULT_EFFECTIVE_FROM + "," + UPDATED_EFFECTIVE_FROM,
            "effectiveFrom.in=" + UPDATED_EFFECTIVE_FROM
        );
    }

    @Test
    @Transactional
    void getAllPricingRulesByEffectiveFromIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedPricingRule = pricingRuleRepository.saveAndFlush(pricingRule);

        // Get all the pricingRuleList where effectiveFrom is not null
        defaultPricingRuleFiltering("effectiveFrom.specified=true", "effectiveFrom.specified=false");
    }

    @Test
    @Transactional
    void getAllPricingRulesByEffectiveToIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedPricingRule = pricingRuleRepository.saveAndFlush(pricingRule);

        // Get all the pricingRuleList where effectiveTo equals to
        defaultPricingRuleFiltering("effectiveTo.equals=" + DEFAULT_EFFECTIVE_TO, "effectiveTo.equals=" + UPDATED_EFFECTIVE_TO);
    }

    @Test
    @Transactional
    void getAllPricingRulesByEffectiveToIsInShouldWork() throws Exception {
        // Initialize the database
        insertedPricingRule = pricingRuleRepository.saveAndFlush(pricingRule);

        // Get all the pricingRuleList where effectiveTo in
        defaultPricingRuleFiltering(
            "effectiveTo.in=" + DEFAULT_EFFECTIVE_TO + "," + UPDATED_EFFECTIVE_TO,
            "effectiveTo.in=" + UPDATED_EFFECTIVE_TO
        );
    }

    @Test
    @Transactional
    void getAllPricingRulesByEffectiveToIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedPricingRule = pricingRuleRepository.saveAndFlush(pricingRule);

        // Get all the pricingRuleList where effectiveTo is not null
        defaultPricingRuleFiltering("effectiveTo.specified=true", "effectiveTo.specified=false");
    }

    @Test
    @Transactional
    void getAllPricingRulesByActiveIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedPricingRule = pricingRuleRepository.saveAndFlush(pricingRule);

        // Get all the pricingRuleList where active equals to
        defaultPricingRuleFiltering("active.equals=" + DEFAULT_ACTIVE, "active.equals=" + UPDATED_ACTIVE);
    }

    @Test
    @Transactional
    void getAllPricingRulesByActiveIsInShouldWork() throws Exception {
        // Initialize the database
        insertedPricingRule = pricingRuleRepository.saveAndFlush(pricingRule);

        // Get all the pricingRuleList where active in
        defaultPricingRuleFiltering("active.in=" + DEFAULT_ACTIVE + "," + UPDATED_ACTIVE, "active.in=" + UPDATED_ACTIVE);
    }

    @Test
    @Transactional
    void getAllPricingRulesByActiveIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedPricingRule = pricingRuleRepository.saveAndFlush(pricingRule);

        // Get all the pricingRuleList where active is not null
        defaultPricingRuleFiltering("active.specified=true", "active.specified=false");
    }

    @Test
    @Transactional
    void getAllPricingRulesByRouteIsEqualToSomething() throws Exception {
        Route route;
        if (TestUtil.findAll(em, Route.class).isEmpty()) {
            pricingRuleRepository.saveAndFlush(pricingRule);
            route = RouteResourceIT.createEntity(em);
        } else {
            route = TestUtil.findAll(em, Route.class).get(0);
        }
        em.persist(route);
        em.flush();
        pricingRule.setRoute(route);
        pricingRuleRepository.saveAndFlush(pricingRule);
        Long routeId = route.getId();
        // Get all the pricingRuleList where route equals to routeId
        defaultPricingRuleShouldBeFound("routeId.equals=" + routeId);

        // Get all the pricingRuleList where route equals to (routeId + 1)
        defaultPricingRuleShouldNotBeFound("routeId.equals=" + (routeId + 1));
    }

    private void defaultPricingRuleFiltering(String shouldBeFound, String shouldNotBeFound) throws Exception {
        defaultPricingRuleShouldBeFound(shouldBeFound);
        defaultPricingRuleShouldNotBeFound(shouldNotBeFound);
    }

    /**
     * Executes the search, and checks that the default entity is returned.
     */
    private void defaultPricingRuleShouldBeFound(String filter) throws Exception {
        restPricingRuleMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(pricingRule.getId().intValue())))
            .andExpect(jsonPath("$.[*].ruleCode").value(hasItem(DEFAULT_RULE_CODE)))
            .andExpect(jsonPath("$.[*].tierLabel").value(hasItem(DEFAULT_TIER_LABEL)))
            .andExpect(jsonPath("$.[*].minKg").value(hasItem(sameNumber(DEFAULT_MIN_KG))))
            .andExpect(jsonPath("$.[*].maxKg").value(hasItem(sameNumber(DEFAULT_MAX_KG))))
            .andExpect(jsonPath("$.[*].unitPrice").value(hasItem(sameNumber(DEFAULT_UNIT_PRICE))))
            .andExpect(jsonPath("$.[*].surchargeAmount").value(hasItem(sameNumber(DEFAULT_SURCHARGE_AMOUNT))))
            .andExpect(jsonPath("$.[*].dimDivisor").value(hasItem(DEFAULT_DIM_DIVISOR)))
            .andExpect(jsonPath("$.[*].kmMin").value(hasItem(sameNumber(DEFAULT_KM_MIN))))
            .andExpect(jsonPath("$.[*].kmRate").value(hasItem(sameNumber(DEFAULT_KM_RATE))))
            .andExpect(jsonPath("$.[*].stepGram").value(hasItem(DEFAULT_STEP_GRAM)))
            .andExpect(jsonPath("$.[*].addFeeAmount").value(hasItem(sameNumber(DEFAULT_ADD_FEE_AMOUNT))))
            .andExpect(jsonPath("$.[*].effectiveFrom").value(hasItem(DEFAULT_EFFECTIVE_FROM.toString())))
            .andExpect(jsonPath("$.[*].effectiveTo").value(hasItem(DEFAULT_EFFECTIVE_TO.toString())))
            .andExpect(jsonPath("$.[*].active").value(hasItem(DEFAULT_ACTIVE)));

        // Check, that the count call also returns 1
        restPricingRuleMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("1"));
    }

    /**
     * Executes the search, and checks that the default entity is not returned.
     */
    private void defaultPricingRuleShouldNotBeFound(String filter) throws Exception {
        restPricingRuleMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());

        // Check, that the count call also returns 0
        restPricingRuleMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("0"));
    }

    @Test
    @Transactional
    void getNonExistingPricingRule() throws Exception {
        // Get the pricingRule
        restPricingRuleMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingPricingRule() throws Exception {
        // Initialize the database
        insertedPricingRule = pricingRuleRepository.saveAndFlush(pricingRule);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the pricingRule
        PricingRule updatedPricingRule = pricingRuleRepository.findById(pricingRule.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedPricingRule are not directly saved in db
        em.detach(updatedPricingRule);
        updatedPricingRule
            .ruleCode(UPDATED_RULE_CODE)
            .tierLabel(UPDATED_TIER_LABEL)
            .minKg(UPDATED_MIN_KG)
            .maxKg(UPDATED_MAX_KG)
            .unitPrice(UPDATED_UNIT_PRICE)
            .surchargeAmount(UPDATED_SURCHARGE_AMOUNT)
            .dimDivisor(UPDATED_DIM_DIVISOR)
            .kmMin(UPDATED_KM_MIN)
            .kmRate(UPDATED_KM_RATE)
            .stepGram(UPDATED_STEP_GRAM)
            .addFeeAmount(UPDATED_ADD_FEE_AMOUNT)
            .effectiveFrom(UPDATED_EFFECTIVE_FROM)
            .effectiveTo(UPDATED_EFFECTIVE_TO)
            .active(UPDATED_ACTIVE);
        PricingRuleDTO pricingRuleDTO = pricingRuleMapper.toDto(updatedPricingRule);

        restPricingRuleMockMvc
            .perform(
                put(ENTITY_API_URL_ID, pricingRuleDTO.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(pricingRuleDTO))
            )
            .andExpect(status().isOk());

        // Validate the PricingRule in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedPricingRuleToMatchAllProperties(updatedPricingRule);
    }

    @Test
    @Transactional
    void putNonExistingPricingRule() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        pricingRule.setId(longCount.incrementAndGet());

        // Create the PricingRule
        PricingRuleDTO pricingRuleDTO = pricingRuleMapper.toDto(pricingRule);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restPricingRuleMockMvc
            .perform(
                put(ENTITY_API_URL_ID, pricingRuleDTO.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(pricingRuleDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the PricingRule in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchPricingRule() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        pricingRule.setId(longCount.incrementAndGet());

        // Create the PricingRule
        PricingRuleDTO pricingRuleDTO = pricingRuleMapper.toDto(pricingRule);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restPricingRuleMockMvc
            .perform(
                put(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(pricingRuleDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the PricingRule in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamPricingRule() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        pricingRule.setId(longCount.incrementAndGet());

        // Create the PricingRule
        PricingRuleDTO pricingRuleDTO = pricingRuleMapper.toDto(pricingRule);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restPricingRuleMockMvc
            .perform(put(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(pricingRuleDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the PricingRule in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdatePricingRuleWithPatch() throws Exception {
        // Initialize the database
        insertedPricingRule = pricingRuleRepository.saveAndFlush(pricingRule);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the pricingRule using partial update
        PricingRule partialUpdatedPricingRule = new PricingRule();
        partialUpdatedPricingRule.setId(pricingRule.getId());

        partialUpdatedPricingRule
            .ruleCode(UPDATED_RULE_CODE)
            .minKg(UPDATED_MIN_KG)
            .maxKg(UPDATED_MAX_KG)
            .dimDivisor(UPDATED_DIM_DIVISOR)
            .kmMin(UPDATED_KM_MIN)
            .kmRate(UPDATED_KM_RATE)
            .addFeeAmount(UPDATED_ADD_FEE_AMOUNT)
            .effectiveFrom(UPDATED_EFFECTIVE_FROM);

        restPricingRuleMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedPricingRule.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedPricingRule))
            )
            .andExpect(status().isOk());

        // Validate the PricingRule in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPricingRuleUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedPricingRule, pricingRule),
            getPersistedPricingRule(pricingRule)
        );
    }

    @Test
    @Transactional
    void fullUpdatePricingRuleWithPatch() throws Exception {
        // Initialize the database
        insertedPricingRule = pricingRuleRepository.saveAndFlush(pricingRule);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the pricingRule using partial update
        PricingRule partialUpdatedPricingRule = new PricingRule();
        partialUpdatedPricingRule.setId(pricingRule.getId());

        partialUpdatedPricingRule
            .ruleCode(UPDATED_RULE_CODE)
            .tierLabel(UPDATED_TIER_LABEL)
            .minKg(UPDATED_MIN_KG)
            .maxKg(UPDATED_MAX_KG)
            .unitPrice(UPDATED_UNIT_PRICE)
            .surchargeAmount(UPDATED_SURCHARGE_AMOUNT)
            .dimDivisor(UPDATED_DIM_DIVISOR)
            .kmMin(UPDATED_KM_MIN)
            .kmRate(UPDATED_KM_RATE)
            .stepGram(UPDATED_STEP_GRAM)
            .addFeeAmount(UPDATED_ADD_FEE_AMOUNT)
            .effectiveFrom(UPDATED_EFFECTIVE_FROM)
            .effectiveTo(UPDATED_EFFECTIVE_TO)
            .active(UPDATED_ACTIVE);

        restPricingRuleMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedPricingRule.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedPricingRule))
            )
            .andExpect(status().isOk());

        // Validate the PricingRule in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPricingRuleUpdatableFieldsEquals(partialUpdatedPricingRule, getPersistedPricingRule(partialUpdatedPricingRule));
    }

    @Test
    @Transactional
    void patchNonExistingPricingRule() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        pricingRule.setId(longCount.incrementAndGet());

        // Create the PricingRule
        PricingRuleDTO pricingRuleDTO = pricingRuleMapper.toDto(pricingRule);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restPricingRuleMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, pricingRuleDTO.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(pricingRuleDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the PricingRule in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchPricingRule() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        pricingRule.setId(longCount.incrementAndGet());

        // Create the PricingRule
        PricingRuleDTO pricingRuleDTO = pricingRuleMapper.toDto(pricingRule);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restPricingRuleMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(pricingRuleDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the PricingRule in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamPricingRule() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        pricingRule.setId(longCount.incrementAndGet());

        // Create the PricingRule
        PricingRuleDTO pricingRuleDTO = pricingRuleMapper.toDto(pricingRule);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restPricingRuleMockMvc
            .perform(patch(ENTITY_API_URL).contentType("application/merge-patch+json").content(om.writeValueAsBytes(pricingRuleDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the PricingRule in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deletePricingRule() throws Exception {
        // Initialize the database
        insertedPricingRule = pricingRuleRepository.saveAndFlush(pricingRule);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the pricingRule
        restPricingRuleMockMvc
            .perform(delete(ENTITY_API_URL_ID, pricingRule.getId()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return pricingRuleRepository.count();
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

    protected PricingRule getPersistedPricingRule(PricingRule pricingRule) {
        return pricingRuleRepository.findById(pricingRule.getId()).orElseThrow();
    }

    protected void assertPersistedPricingRuleToMatchAllProperties(PricingRule expectedPricingRule) {
        assertPricingRuleAllPropertiesEquals(expectedPricingRule, getPersistedPricingRule(expectedPricingRule));
    }

    protected void assertPersistedPricingRuleToMatchUpdatableProperties(PricingRule expectedPricingRule) {
        assertPricingRuleAllUpdatablePropertiesEquals(expectedPricingRule, getPersistedPricingRule(expectedPricingRule));
    }
}
