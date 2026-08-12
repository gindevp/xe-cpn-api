package com.mycompany.myapp.web.rest;

import static com.mycompany.myapp.domain.ProductPriceRuleAsserts.*;
import static com.mycompany.myapp.web.rest.TestUtil.createUpdateProxyForBean;
import static com.mycompany.myapp.web.rest.TestUtil.sameNumber;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.myapp.IntegrationTest;
import com.mycompany.myapp.domain.ProductPriceRule;
import com.mycompany.myapp.repository.ProductPriceRuleRepository;
import com.mycompany.myapp.service.dto.ProductPriceRuleDTO;
import com.mycompany.myapp.service.mapper.ProductPriceRuleMapper;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
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
 * Integration tests for the {@link ProductPriceRuleResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser
class ProductPriceRuleResourceIT {

    private static final String DEFAULT_GROUP_NAME = "AAAAAAAAAA";
    private static final String UPDATED_GROUP_NAME = "BBBBBBBBBB";

    private static final String DEFAULT_PRODUCT_NAME = "AAAAAAAAAA";
    private static final String UPDATED_PRODUCT_NAME = "BBBBBBBBBB";

    private static final BigDecimal DEFAULT_CURRENT_PRICE = new BigDecimal(0);
    private static final BigDecimal UPDATED_CURRENT_PRICE = new BigDecimal(1);

    private static final BigDecimal DEFAULT_APPLIED_PRICE = new BigDecimal(0);
    private static final BigDecimal UPDATED_APPLIED_PRICE = new BigDecimal(1);

    private static final String DEFAULT_NOTE = "AAAAAAAAAA";
    private static final String UPDATED_NOTE = "BBBBBBBBBB";

    private static final Boolean DEFAULT_ACTIVE = false;
    private static final Boolean UPDATED_ACTIVE = true;

    private static final String ENTITY_API_URL = "/api/product-price-rules";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static Random random = new Random();
    private static AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    @Autowired
    private ObjectMapper om;

    @Autowired
    private ProductPriceRuleRepository productPriceRuleRepository;

    @Autowired
    private ProductPriceRuleMapper productPriceRuleMapper;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restProductPriceRuleMockMvc;

    private ProductPriceRule productPriceRule;

    private ProductPriceRule insertedProductPriceRule;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static ProductPriceRule createEntity() {
        return new ProductPriceRule()
            .groupName(DEFAULT_GROUP_NAME)
            .productName(DEFAULT_PRODUCT_NAME)
            .currentPrice(DEFAULT_CURRENT_PRICE)
            .appliedPrice(DEFAULT_APPLIED_PRICE)
            .note(DEFAULT_NOTE)
            .active(DEFAULT_ACTIVE);
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static ProductPriceRule createUpdatedEntity() {
        return new ProductPriceRule()
            .groupName(UPDATED_GROUP_NAME)
            .productName(UPDATED_PRODUCT_NAME)
            .currentPrice(UPDATED_CURRENT_PRICE)
            .appliedPrice(UPDATED_APPLIED_PRICE)
            .note(UPDATED_NOTE)
            .active(UPDATED_ACTIVE);
    }

    @BeforeEach
    public void initTest() {
        productPriceRule = createEntity();
    }

    @AfterEach
    public void cleanup() {
        if (insertedProductPriceRule != null) {
            productPriceRuleRepository.delete(insertedProductPriceRule);
            insertedProductPriceRule = null;
        }
    }

    @Test
    @Transactional
    void createProductPriceRule() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the ProductPriceRule
        ProductPriceRuleDTO productPriceRuleDTO = productPriceRuleMapper.toDto(productPriceRule);
        var returnedProductPriceRuleDTO = om.readValue(
            restProductPriceRuleMockMvc
                .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(productPriceRuleDTO)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            ProductPriceRuleDTO.class
        );

        // Validate the ProductPriceRule in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedProductPriceRule = productPriceRuleMapper.toEntity(returnedProductPriceRuleDTO);
        assertProductPriceRuleUpdatableFieldsEquals(returnedProductPriceRule, getPersistedProductPriceRule(returnedProductPriceRule));

        insertedProductPriceRule = returnedProductPriceRule;
    }

    @Test
    @Transactional
    void createProductPriceRuleWithExistingId() throws Exception {
        // Create the ProductPriceRule with an existing ID
        productPriceRule.setId(1L);
        ProductPriceRuleDTO productPriceRuleDTO = productPriceRuleMapper.toDto(productPriceRule);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restProductPriceRuleMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(productPriceRuleDTO)))
            .andExpect(status().isBadRequest());

        // Validate the ProductPriceRule in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkGroupNameIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        productPriceRule.setGroupName(null);

        // Create the ProductPriceRule, which fails.
        ProductPriceRuleDTO productPriceRuleDTO = productPriceRuleMapper.toDto(productPriceRule);

        restProductPriceRuleMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(productPriceRuleDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkProductNameIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        productPriceRule.setProductName(null);

        // Create the ProductPriceRule, which fails.
        ProductPriceRuleDTO productPriceRuleDTO = productPriceRuleMapper.toDto(productPriceRule);

        restProductPriceRuleMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(productPriceRuleDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkAppliedPriceIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        productPriceRule.setAppliedPrice(null);

        // Create the ProductPriceRule, which fails.
        ProductPriceRuleDTO productPriceRuleDTO = productPriceRuleMapper.toDto(productPriceRule);

        restProductPriceRuleMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(productPriceRuleDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkActiveIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        productPriceRule.setActive(null);

        // Create the ProductPriceRule, which fails.
        ProductPriceRuleDTO productPriceRuleDTO = productPriceRuleMapper.toDto(productPriceRule);

        restProductPriceRuleMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(productPriceRuleDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllProductPriceRules() throws Exception {
        // Initialize the database
        insertedProductPriceRule = productPriceRuleRepository.saveAndFlush(productPriceRule);

        // Get all the productPriceRuleList
        restProductPriceRuleMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(productPriceRule.getId().intValue())))
            .andExpect(jsonPath("$.[*].groupName").value(hasItem(DEFAULT_GROUP_NAME)))
            .andExpect(jsonPath("$.[*].productName").value(hasItem(DEFAULT_PRODUCT_NAME)))
            .andExpect(jsonPath("$.[*].currentPrice").value(hasItem(sameNumber(DEFAULT_CURRENT_PRICE))))
            .andExpect(jsonPath("$.[*].appliedPrice").value(hasItem(sameNumber(DEFAULT_APPLIED_PRICE))))
            .andExpect(jsonPath("$.[*].note").value(hasItem(DEFAULT_NOTE)))
            .andExpect(jsonPath("$.[*].active").value(hasItem(DEFAULT_ACTIVE)));
    }

    @Test
    @Transactional
    void getProductPriceRule() throws Exception {
        // Initialize the database
        insertedProductPriceRule = productPriceRuleRepository.saveAndFlush(productPriceRule);

        // Get the productPriceRule
        restProductPriceRuleMockMvc
            .perform(get(ENTITY_API_URL_ID, productPriceRule.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(productPriceRule.getId().intValue()))
            .andExpect(jsonPath("$.groupName").value(DEFAULT_GROUP_NAME))
            .andExpect(jsonPath("$.productName").value(DEFAULT_PRODUCT_NAME))
            .andExpect(jsonPath("$.currentPrice").value(sameNumber(DEFAULT_CURRENT_PRICE)))
            .andExpect(jsonPath("$.appliedPrice").value(sameNumber(DEFAULT_APPLIED_PRICE)))
            .andExpect(jsonPath("$.note").value(DEFAULT_NOTE))
            .andExpect(jsonPath("$.active").value(DEFAULT_ACTIVE));
    }

    @Test
    @Transactional
    void getNonExistingProductPriceRule() throws Exception {
        // Get the productPriceRule
        restProductPriceRuleMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingProductPriceRule() throws Exception {
        // Initialize the database
        insertedProductPriceRule = productPriceRuleRepository.saveAndFlush(productPriceRule);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the productPriceRule
        ProductPriceRule updatedProductPriceRule = productPriceRuleRepository.findById(productPriceRule.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedProductPriceRule are not directly saved in db
        em.detach(updatedProductPriceRule);
        updatedProductPriceRule
            .groupName(UPDATED_GROUP_NAME)
            .productName(UPDATED_PRODUCT_NAME)
            .currentPrice(UPDATED_CURRENT_PRICE)
            .appliedPrice(UPDATED_APPLIED_PRICE)
            .note(UPDATED_NOTE)
            .active(UPDATED_ACTIVE);
        ProductPriceRuleDTO productPriceRuleDTO = productPriceRuleMapper.toDto(updatedProductPriceRule);

        restProductPriceRuleMockMvc
            .perform(
                put(ENTITY_API_URL_ID, productPriceRuleDTO.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(productPriceRuleDTO))
            )
            .andExpect(status().isOk());

        // Validate the ProductPriceRule in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedProductPriceRuleToMatchAllProperties(updatedProductPriceRule);
    }

    @Test
    @Transactional
    void putNonExistingProductPriceRule() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        productPriceRule.setId(longCount.incrementAndGet());

        // Create the ProductPriceRule
        ProductPriceRuleDTO productPriceRuleDTO = productPriceRuleMapper.toDto(productPriceRule);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restProductPriceRuleMockMvc
            .perform(
                put(ENTITY_API_URL_ID, productPriceRuleDTO.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(productPriceRuleDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the ProductPriceRule in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchProductPriceRule() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        productPriceRule.setId(longCount.incrementAndGet());

        // Create the ProductPriceRule
        ProductPriceRuleDTO productPriceRuleDTO = productPriceRuleMapper.toDto(productPriceRule);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restProductPriceRuleMockMvc
            .perform(
                put(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(productPriceRuleDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the ProductPriceRule in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamProductPriceRule() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        productPriceRule.setId(longCount.incrementAndGet());

        // Create the ProductPriceRule
        ProductPriceRuleDTO productPriceRuleDTO = productPriceRuleMapper.toDto(productPriceRule);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restProductPriceRuleMockMvc
            .perform(put(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(productPriceRuleDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the ProductPriceRule in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateProductPriceRuleWithPatch() throws Exception {
        // Initialize the database
        insertedProductPriceRule = productPriceRuleRepository.saveAndFlush(productPriceRule);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the productPriceRule using partial update
        ProductPriceRule partialUpdatedProductPriceRule = new ProductPriceRule();
        partialUpdatedProductPriceRule.setId(productPriceRule.getId());

        partialUpdatedProductPriceRule.groupName(UPDATED_GROUP_NAME).appliedPrice(UPDATED_APPLIED_PRICE);

        restProductPriceRuleMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedProductPriceRule.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedProductPriceRule))
            )
            .andExpect(status().isOk());

        // Validate the ProductPriceRule in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertProductPriceRuleUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedProductPriceRule, productPriceRule),
            getPersistedProductPriceRule(productPriceRule)
        );
    }

    @Test
    @Transactional
    void fullUpdateProductPriceRuleWithPatch() throws Exception {
        // Initialize the database
        insertedProductPriceRule = productPriceRuleRepository.saveAndFlush(productPriceRule);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the productPriceRule using partial update
        ProductPriceRule partialUpdatedProductPriceRule = new ProductPriceRule();
        partialUpdatedProductPriceRule.setId(productPriceRule.getId());

        partialUpdatedProductPriceRule
            .groupName(UPDATED_GROUP_NAME)
            .productName(UPDATED_PRODUCT_NAME)
            .currentPrice(UPDATED_CURRENT_PRICE)
            .appliedPrice(UPDATED_APPLIED_PRICE)
            .note(UPDATED_NOTE)
            .active(UPDATED_ACTIVE);

        restProductPriceRuleMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedProductPriceRule.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedProductPriceRule))
            )
            .andExpect(status().isOk());

        // Validate the ProductPriceRule in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertProductPriceRuleUpdatableFieldsEquals(
            partialUpdatedProductPriceRule,
            getPersistedProductPriceRule(partialUpdatedProductPriceRule)
        );
    }

    @Test
    @Transactional
    void patchNonExistingProductPriceRule() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        productPriceRule.setId(longCount.incrementAndGet());

        // Create the ProductPriceRule
        ProductPriceRuleDTO productPriceRuleDTO = productPriceRuleMapper.toDto(productPriceRule);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restProductPriceRuleMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, productPriceRuleDTO.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(productPriceRuleDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the ProductPriceRule in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchProductPriceRule() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        productPriceRule.setId(longCount.incrementAndGet());

        // Create the ProductPriceRule
        ProductPriceRuleDTO productPriceRuleDTO = productPriceRuleMapper.toDto(productPriceRule);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restProductPriceRuleMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(productPriceRuleDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the ProductPriceRule in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamProductPriceRule() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        productPriceRule.setId(longCount.incrementAndGet());

        // Create the ProductPriceRule
        ProductPriceRuleDTO productPriceRuleDTO = productPriceRuleMapper.toDto(productPriceRule);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restProductPriceRuleMockMvc
            .perform(patch(ENTITY_API_URL).contentType("application/merge-patch+json").content(om.writeValueAsBytes(productPriceRuleDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the ProductPriceRule in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteProductPriceRule() throws Exception {
        // Initialize the database
        insertedProductPriceRule = productPriceRuleRepository.saveAndFlush(productPriceRule);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the productPriceRule
        restProductPriceRuleMockMvc
            .perform(delete(ENTITY_API_URL_ID, productPriceRule.getId()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return productPriceRuleRepository.count();
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

    protected ProductPriceRule getPersistedProductPriceRule(ProductPriceRule productPriceRule) {
        return productPriceRuleRepository.findById(productPriceRule.getId()).orElseThrow();
    }

    protected void assertPersistedProductPriceRuleToMatchAllProperties(ProductPriceRule expectedProductPriceRule) {
        assertProductPriceRuleAllPropertiesEquals(expectedProductPriceRule, getPersistedProductPriceRule(expectedProductPriceRule));
    }

    protected void assertPersistedProductPriceRuleToMatchUpdatableProperties(ProductPriceRule expectedProductPriceRule) {
        assertProductPriceRuleAllUpdatablePropertiesEquals(
            expectedProductPriceRule,
            getPersistedProductPriceRule(expectedProductPriceRule)
        );
    }
}
