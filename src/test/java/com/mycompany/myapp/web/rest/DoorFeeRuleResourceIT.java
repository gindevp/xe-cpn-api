package com.mycompany.myapp.web.rest;

import static com.mycompany.myapp.domain.DoorFeeRuleAsserts.*;
import static com.mycompany.myapp.web.rest.TestUtil.createUpdateProxyForBean;
import static com.mycompany.myapp.web.rest.TestUtil.sameNumber;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.myapp.IntegrationTest;
import com.mycompany.myapp.domain.DoorFeeRule;
import com.mycompany.myapp.domain.enumeration.DoorFeeKind;
import com.mycompany.myapp.repository.DoorFeeRuleRepository;
import com.mycompany.myapp.service.dto.DoorFeeRuleDTO;
import com.mycompany.myapp.service.mapper.DoorFeeRuleMapper;
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
 * Integration tests for the {@link DoorFeeRuleResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser
class DoorFeeRuleResourceIT {

    private static final DoorFeeKind DEFAULT_KIND = DoorFeeKind.PICKUP;
    private static final DoorFeeKind UPDATED_KIND = DoorFeeKind.DELIVERY;

    private static final BigDecimal DEFAULT_MIN_KG = new BigDecimal(0);
    private static final BigDecimal UPDATED_MIN_KG = new BigDecimal(1);

    private static final BigDecimal DEFAULT_MAX_KG = new BigDecimal(0);
    private static final BigDecimal UPDATED_MAX_KG = new BigDecimal(1);

    private static final BigDecimal DEFAULT_MIN_KM = new BigDecimal(0);
    private static final BigDecimal UPDATED_MIN_KM = new BigDecimal(1);

    private static final BigDecimal DEFAULT_MAX_KM = new BigDecimal(0);
    private static final BigDecimal UPDATED_MAX_KM = new BigDecimal(1);

    private static final BigDecimal DEFAULT_FEE_AMOUNT = new BigDecimal(0);
    private static final BigDecimal UPDATED_FEE_AMOUNT = new BigDecimal(1);

    private static final Boolean DEFAULT_ACTIVE = false;
    private static final Boolean UPDATED_ACTIVE = true;

    private static final String ENTITY_API_URL = "/api/door-fee-rules";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static Random random = new Random();
    private static AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    @Autowired
    private ObjectMapper om;

    @Autowired
    private DoorFeeRuleRepository doorFeeRuleRepository;

    @Autowired
    private DoorFeeRuleMapper doorFeeRuleMapper;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restDoorFeeRuleMockMvc;

    private DoorFeeRule doorFeeRule;

    private DoorFeeRule insertedDoorFeeRule;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static DoorFeeRule createEntity() {
        return new DoorFeeRule()
            .kind(DEFAULT_KIND)
            .minKg(DEFAULT_MIN_KG)
            .maxKg(DEFAULT_MAX_KG)
            .minKm(DEFAULT_MIN_KM)
            .maxKm(DEFAULT_MAX_KM)
            .feeAmount(DEFAULT_FEE_AMOUNT)
            .active(DEFAULT_ACTIVE);
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static DoorFeeRule createUpdatedEntity() {
        return new DoorFeeRule()
            .kind(UPDATED_KIND)
            .minKg(UPDATED_MIN_KG)
            .maxKg(UPDATED_MAX_KG)
            .minKm(UPDATED_MIN_KM)
            .maxKm(UPDATED_MAX_KM)
            .feeAmount(UPDATED_FEE_AMOUNT)
            .active(UPDATED_ACTIVE);
    }

    @BeforeEach
    public void initTest() {
        doorFeeRule = createEntity();
    }

    @AfterEach
    public void cleanup() {
        if (insertedDoorFeeRule != null) {
            doorFeeRuleRepository.delete(insertedDoorFeeRule);
            insertedDoorFeeRule = null;
        }
    }

    @Test
    @Transactional
    void createDoorFeeRule() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the DoorFeeRule
        DoorFeeRuleDTO doorFeeRuleDTO = doorFeeRuleMapper.toDto(doorFeeRule);
        var returnedDoorFeeRuleDTO = om.readValue(
            restDoorFeeRuleMockMvc
                .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(doorFeeRuleDTO)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            DoorFeeRuleDTO.class
        );

        // Validate the DoorFeeRule in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedDoorFeeRule = doorFeeRuleMapper.toEntity(returnedDoorFeeRuleDTO);
        assertDoorFeeRuleUpdatableFieldsEquals(returnedDoorFeeRule, getPersistedDoorFeeRule(returnedDoorFeeRule));

        insertedDoorFeeRule = returnedDoorFeeRule;
    }

    @Test
    @Transactional
    void createDoorFeeRuleWithExistingId() throws Exception {
        // Create the DoorFeeRule with an existing ID
        doorFeeRule.setId(1L);
        DoorFeeRuleDTO doorFeeRuleDTO = doorFeeRuleMapper.toDto(doorFeeRule);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restDoorFeeRuleMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(doorFeeRuleDTO)))
            .andExpect(status().isBadRequest());

        // Validate the DoorFeeRule in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkKindIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        doorFeeRule.setKind(null);

        // Create the DoorFeeRule, which fails.
        DoorFeeRuleDTO doorFeeRuleDTO = doorFeeRuleMapper.toDto(doorFeeRule);

        restDoorFeeRuleMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(doorFeeRuleDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkMinKgIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        doorFeeRule.setMinKg(null);

        // Create the DoorFeeRule, which fails.
        DoorFeeRuleDTO doorFeeRuleDTO = doorFeeRuleMapper.toDto(doorFeeRule);

        restDoorFeeRuleMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(doorFeeRuleDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkMaxKgIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        doorFeeRule.setMaxKg(null);

        // Create the DoorFeeRule, which fails.
        DoorFeeRuleDTO doorFeeRuleDTO = doorFeeRuleMapper.toDto(doorFeeRule);

        restDoorFeeRuleMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(doorFeeRuleDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkMinKmIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        doorFeeRule.setMinKm(null);

        // Create the DoorFeeRule, which fails.
        DoorFeeRuleDTO doorFeeRuleDTO = doorFeeRuleMapper.toDto(doorFeeRule);

        restDoorFeeRuleMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(doorFeeRuleDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkMaxKmIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        doorFeeRule.setMaxKm(null);

        // Create the DoorFeeRule, which fails.
        DoorFeeRuleDTO doorFeeRuleDTO = doorFeeRuleMapper.toDto(doorFeeRule);

        restDoorFeeRuleMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(doorFeeRuleDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkFeeAmountIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        doorFeeRule.setFeeAmount(null);

        // Create the DoorFeeRule, which fails.
        DoorFeeRuleDTO doorFeeRuleDTO = doorFeeRuleMapper.toDto(doorFeeRule);

        restDoorFeeRuleMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(doorFeeRuleDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkActiveIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        doorFeeRule.setActive(null);

        // Create the DoorFeeRule, which fails.
        DoorFeeRuleDTO doorFeeRuleDTO = doorFeeRuleMapper.toDto(doorFeeRule);

        restDoorFeeRuleMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(doorFeeRuleDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllDoorFeeRules() throws Exception {
        // Initialize the database
        insertedDoorFeeRule = doorFeeRuleRepository.saveAndFlush(doorFeeRule);

        // Get all the doorFeeRuleList
        restDoorFeeRuleMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(doorFeeRule.getId().intValue())))
            .andExpect(jsonPath("$.[*].kind").value(hasItem(DEFAULT_KIND.toString())))
            .andExpect(jsonPath("$.[*].minKg").value(hasItem(sameNumber(DEFAULT_MIN_KG))))
            .andExpect(jsonPath("$.[*].maxKg").value(hasItem(sameNumber(DEFAULT_MAX_KG))))
            .andExpect(jsonPath("$.[*].minKm").value(hasItem(sameNumber(DEFAULT_MIN_KM))))
            .andExpect(jsonPath("$.[*].maxKm").value(hasItem(sameNumber(DEFAULT_MAX_KM))))
            .andExpect(jsonPath("$.[*].feeAmount").value(hasItem(sameNumber(DEFAULT_FEE_AMOUNT))))
            .andExpect(jsonPath("$.[*].active").value(hasItem(DEFAULT_ACTIVE)));
    }

    @Test
    @Transactional
    void getDoorFeeRule() throws Exception {
        // Initialize the database
        insertedDoorFeeRule = doorFeeRuleRepository.saveAndFlush(doorFeeRule);

        // Get the doorFeeRule
        restDoorFeeRuleMockMvc
            .perform(get(ENTITY_API_URL_ID, doorFeeRule.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(doorFeeRule.getId().intValue()))
            .andExpect(jsonPath("$.kind").value(DEFAULT_KIND.toString()))
            .andExpect(jsonPath("$.minKg").value(sameNumber(DEFAULT_MIN_KG)))
            .andExpect(jsonPath("$.maxKg").value(sameNumber(DEFAULT_MAX_KG)))
            .andExpect(jsonPath("$.minKm").value(sameNumber(DEFAULT_MIN_KM)))
            .andExpect(jsonPath("$.maxKm").value(sameNumber(DEFAULT_MAX_KM)))
            .andExpect(jsonPath("$.feeAmount").value(sameNumber(DEFAULT_FEE_AMOUNT)))
            .andExpect(jsonPath("$.active").value(DEFAULT_ACTIVE));
    }

    @Test
    @Transactional
    void getNonExistingDoorFeeRule() throws Exception {
        // Get the doorFeeRule
        restDoorFeeRuleMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingDoorFeeRule() throws Exception {
        // Initialize the database
        insertedDoorFeeRule = doorFeeRuleRepository.saveAndFlush(doorFeeRule);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the doorFeeRule
        DoorFeeRule updatedDoorFeeRule = doorFeeRuleRepository.findById(doorFeeRule.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedDoorFeeRule are not directly saved in db
        em.detach(updatedDoorFeeRule);
        updatedDoorFeeRule
            .kind(UPDATED_KIND)
            .minKg(UPDATED_MIN_KG)
            .maxKg(UPDATED_MAX_KG)
            .minKm(UPDATED_MIN_KM)
            .maxKm(UPDATED_MAX_KM)
            .feeAmount(UPDATED_FEE_AMOUNT)
            .active(UPDATED_ACTIVE);
        DoorFeeRuleDTO doorFeeRuleDTO = doorFeeRuleMapper.toDto(updatedDoorFeeRule);

        restDoorFeeRuleMockMvc
            .perform(
                put(ENTITY_API_URL_ID, doorFeeRuleDTO.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(doorFeeRuleDTO))
            )
            .andExpect(status().isOk());

        // Validate the DoorFeeRule in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedDoorFeeRuleToMatchAllProperties(updatedDoorFeeRule);
    }

    @Test
    @Transactional
    void putNonExistingDoorFeeRule() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        doorFeeRule.setId(longCount.incrementAndGet());

        // Create the DoorFeeRule
        DoorFeeRuleDTO doorFeeRuleDTO = doorFeeRuleMapper.toDto(doorFeeRule);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restDoorFeeRuleMockMvc
            .perform(
                put(ENTITY_API_URL_ID, doorFeeRuleDTO.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(doorFeeRuleDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the DoorFeeRule in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchDoorFeeRule() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        doorFeeRule.setId(longCount.incrementAndGet());

        // Create the DoorFeeRule
        DoorFeeRuleDTO doorFeeRuleDTO = doorFeeRuleMapper.toDto(doorFeeRule);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restDoorFeeRuleMockMvc
            .perform(
                put(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(doorFeeRuleDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the DoorFeeRule in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamDoorFeeRule() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        doorFeeRule.setId(longCount.incrementAndGet());

        // Create the DoorFeeRule
        DoorFeeRuleDTO doorFeeRuleDTO = doorFeeRuleMapper.toDto(doorFeeRule);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restDoorFeeRuleMockMvc
            .perform(put(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(doorFeeRuleDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the DoorFeeRule in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateDoorFeeRuleWithPatch() throws Exception {
        // Initialize the database
        insertedDoorFeeRule = doorFeeRuleRepository.saveAndFlush(doorFeeRule);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the doorFeeRule using partial update
        DoorFeeRule partialUpdatedDoorFeeRule = new DoorFeeRule();
        partialUpdatedDoorFeeRule.setId(doorFeeRule.getId());

        partialUpdatedDoorFeeRule
            .minKg(UPDATED_MIN_KG)
            .maxKg(UPDATED_MAX_KG)
            .minKm(UPDATED_MIN_KM)
            .maxKm(UPDATED_MAX_KM)
            .feeAmount(UPDATED_FEE_AMOUNT);

        restDoorFeeRuleMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedDoorFeeRule.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedDoorFeeRule))
            )
            .andExpect(status().isOk());

        // Validate the DoorFeeRule in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertDoorFeeRuleUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedDoorFeeRule, doorFeeRule),
            getPersistedDoorFeeRule(doorFeeRule)
        );
    }

    @Test
    @Transactional
    void fullUpdateDoorFeeRuleWithPatch() throws Exception {
        // Initialize the database
        insertedDoorFeeRule = doorFeeRuleRepository.saveAndFlush(doorFeeRule);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the doorFeeRule using partial update
        DoorFeeRule partialUpdatedDoorFeeRule = new DoorFeeRule();
        partialUpdatedDoorFeeRule.setId(doorFeeRule.getId());

        partialUpdatedDoorFeeRule
            .kind(UPDATED_KIND)
            .minKg(UPDATED_MIN_KG)
            .maxKg(UPDATED_MAX_KG)
            .minKm(UPDATED_MIN_KM)
            .maxKm(UPDATED_MAX_KM)
            .feeAmount(UPDATED_FEE_AMOUNT)
            .active(UPDATED_ACTIVE);

        restDoorFeeRuleMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedDoorFeeRule.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedDoorFeeRule))
            )
            .andExpect(status().isOk());

        // Validate the DoorFeeRule in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertDoorFeeRuleUpdatableFieldsEquals(partialUpdatedDoorFeeRule, getPersistedDoorFeeRule(partialUpdatedDoorFeeRule));
    }

    @Test
    @Transactional
    void patchNonExistingDoorFeeRule() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        doorFeeRule.setId(longCount.incrementAndGet());

        // Create the DoorFeeRule
        DoorFeeRuleDTO doorFeeRuleDTO = doorFeeRuleMapper.toDto(doorFeeRule);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restDoorFeeRuleMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, doorFeeRuleDTO.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(doorFeeRuleDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the DoorFeeRule in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchDoorFeeRule() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        doorFeeRule.setId(longCount.incrementAndGet());

        // Create the DoorFeeRule
        DoorFeeRuleDTO doorFeeRuleDTO = doorFeeRuleMapper.toDto(doorFeeRule);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restDoorFeeRuleMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(doorFeeRuleDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the DoorFeeRule in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamDoorFeeRule() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        doorFeeRule.setId(longCount.incrementAndGet());

        // Create the DoorFeeRule
        DoorFeeRuleDTO doorFeeRuleDTO = doorFeeRuleMapper.toDto(doorFeeRule);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restDoorFeeRuleMockMvc
            .perform(patch(ENTITY_API_URL).contentType("application/merge-patch+json").content(om.writeValueAsBytes(doorFeeRuleDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the DoorFeeRule in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteDoorFeeRule() throws Exception {
        // Initialize the database
        insertedDoorFeeRule = doorFeeRuleRepository.saveAndFlush(doorFeeRule);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the doorFeeRule
        restDoorFeeRuleMockMvc
            .perform(delete(ENTITY_API_URL_ID, doorFeeRule.getId()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return doorFeeRuleRepository.count();
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

    protected DoorFeeRule getPersistedDoorFeeRule(DoorFeeRule doorFeeRule) {
        return doorFeeRuleRepository.findById(doorFeeRule.getId()).orElseThrow();
    }

    protected void assertPersistedDoorFeeRuleToMatchAllProperties(DoorFeeRule expectedDoorFeeRule) {
        assertDoorFeeRuleAllPropertiesEquals(expectedDoorFeeRule, getPersistedDoorFeeRule(expectedDoorFeeRule));
    }

    protected void assertPersistedDoorFeeRuleToMatchUpdatableProperties(DoorFeeRule expectedDoorFeeRule) {
        assertDoorFeeRuleAllUpdatablePropertiesEquals(expectedDoorFeeRule, getPersistedDoorFeeRule(expectedDoorFeeRule));
    }
}
