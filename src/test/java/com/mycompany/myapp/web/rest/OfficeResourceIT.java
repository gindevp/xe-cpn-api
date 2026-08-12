package com.mycompany.myapp.web.rest;

import static com.mycompany.myapp.domain.OfficeAsserts.*;
import static com.mycompany.myapp.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.myapp.IntegrationTest;
import com.mycompany.myapp.domain.Office;
import com.mycompany.myapp.domain.enumeration.OfficeType;
import com.mycompany.myapp.repository.OfficeRepository;
import com.mycompany.myapp.service.dto.OfficeDTO;
import com.mycompany.myapp.service.mapper.OfficeMapper;
import jakarta.persistence.EntityManager;
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
 * Integration tests for the {@link OfficeResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser
class OfficeResourceIT {

    private static final String DEFAULT_CODE = "AAAAAAAAAA";
    private static final String UPDATED_CODE = "BBBBBBBBBB";

    private static final String DEFAULT_NAME = "AAAAAAAAAA";
    private static final String UPDATED_NAME = "BBBBBBBBBB";

    private static final OfficeType DEFAULT_OFFICE_TYPE = OfficeType.HUB;
    private static final OfficeType UPDATED_OFFICE_TYPE = OfficeType.BRANCH;

    private static final Boolean DEFAULT_IS_HUB = false;
    private static final Boolean UPDATED_IS_HUB = true;

    private static final Boolean DEFAULT_ACTIVE = false;
    private static final Boolean UPDATED_ACTIVE = true;

    private static final String ENTITY_API_URL = "/api/offices";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static Random random = new Random();
    private static AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    @Autowired
    private ObjectMapper om;

    @Autowired
    private OfficeRepository officeRepository;

    @Autowired
    private OfficeMapper officeMapper;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restOfficeMockMvc;

    private Office office;

    private Office insertedOffice;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Office createEntity() {
        return new Office()
            .code(DEFAULT_CODE)
            .name(DEFAULT_NAME)
            .officeType(DEFAULT_OFFICE_TYPE)
            .isHub(DEFAULT_IS_HUB)
            .active(DEFAULT_ACTIVE);
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Office createUpdatedEntity() {
        return new Office()
            .code(UPDATED_CODE)
            .name(UPDATED_NAME)
            .officeType(UPDATED_OFFICE_TYPE)
            .isHub(UPDATED_IS_HUB)
            .active(UPDATED_ACTIVE);
    }

    @BeforeEach
    public void initTest() {
        office = createEntity();
    }

    @AfterEach
    public void cleanup() {
        if (insertedOffice != null) {
            officeRepository.delete(insertedOffice);
            insertedOffice = null;
        }
    }

    @Test
    @Transactional
    void createOffice() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the Office
        OfficeDTO officeDTO = officeMapper.toDto(office);
        var returnedOfficeDTO = om.readValue(
            restOfficeMockMvc
                .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(officeDTO)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            OfficeDTO.class
        );

        // Validate the Office in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedOffice = officeMapper.toEntity(returnedOfficeDTO);
        assertOfficeUpdatableFieldsEquals(returnedOffice, getPersistedOffice(returnedOffice));

        insertedOffice = returnedOffice;
    }

    @Test
    @Transactional
    void createOfficeWithExistingId() throws Exception {
        // Create the Office with an existing ID
        office.setId(1L);
        OfficeDTO officeDTO = officeMapper.toDto(office);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restOfficeMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(officeDTO)))
            .andExpect(status().isBadRequest());

        // Validate the Office in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkCodeIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        office.setCode(null);

        // Create the Office, which fails.
        OfficeDTO officeDTO = officeMapper.toDto(office);

        restOfficeMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(officeDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkNameIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        office.setName(null);

        // Create the Office, which fails.
        OfficeDTO officeDTO = officeMapper.toDto(office);

        restOfficeMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(officeDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkOfficeTypeIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        office.setOfficeType(null);

        // Create the Office, which fails.
        OfficeDTO officeDTO = officeMapper.toDto(office);

        restOfficeMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(officeDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkIsHubIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        office.setIsHub(null);

        // Create the Office, which fails.
        OfficeDTO officeDTO = officeMapper.toDto(office);

        restOfficeMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(officeDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkActiveIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        office.setActive(null);

        // Create the Office, which fails.
        OfficeDTO officeDTO = officeMapper.toDto(office);

        restOfficeMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(officeDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllOffices() throws Exception {
        // Initialize the database
        insertedOffice = officeRepository.saveAndFlush(office);

        // Get all the officeList
        restOfficeMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(office.getId().intValue())))
            .andExpect(jsonPath("$.[*].code").value(hasItem(DEFAULT_CODE)))
            .andExpect(jsonPath("$.[*].name").value(hasItem(DEFAULT_NAME)))
            .andExpect(jsonPath("$.[*].officeType").value(hasItem(DEFAULT_OFFICE_TYPE.toString())))
            .andExpect(jsonPath("$.[*].isHub").value(hasItem(DEFAULT_IS_HUB)))
            .andExpect(jsonPath("$.[*].active").value(hasItem(DEFAULT_ACTIVE)));
    }

    @Test
    @Transactional
    void getOffice() throws Exception {
        // Initialize the database
        insertedOffice = officeRepository.saveAndFlush(office);

        // Get the office
        restOfficeMockMvc
            .perform(get(ENTITY_API_URL_ID, office.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(office.getId().intValue()))
            .andExpect(jsonPath("$.code").value(DEFAULT_CODE))
            .andExpect(jsonPath("$.name").value(DEFAULT_NAME))
            .andExpect(jsonPath("$.officeType").value(DEFAULT_OFFICE_TYPE.toString()))
            .andExpect(jsonPath("$.isHub").value(DEFAULT_IS_HUB))
            .andExpect(jsonPath("$.active").value(DEFAULT_ACTIVE));
    }

    @Test
    @Transactional
    void getNonExistingOffice() throws Exception {
        // Get the office
        restOfficeMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingOffice() throws Exception {
        // Initialize the database
        insertedOffice = officeRepository.saveAndFlush(office);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the office
        Office updatedOffice = officeRepository.findById(office.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedOffice are not directly saved in db
        em.detach(updatedOffice);
        updatedOffice.code(UPDATED_CODE).name(UPDATED_NAME).officeType(UPDATED_OFFICE_TYPE).isHub(UPDATED_IS_HUB).active(UPDATED_ACTIVE);
        OfficeDTO officeDTO = officeMapper.toDto(updatedOffice);

        restOfficeMockMvc
            .perform(
                put(ENTITY_API_URL_ID, officeDTO.getId()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(officeDTO))
            )
            .andExpect(status().isOk());

        // Validate the Office in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedOfficeToMatchAllProperties(updatedOffice);
    }

    @Test
    @Transactional
    void putNonExistingOffice() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        office.setId(longCount.incrementAndGet());

        // Create the Office
        OfficeDTO officeDTO = officeMapper.toDto(office);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restOfficeMockMvc
            .perform(
                put(ENTITY_API_URL_ID, officeDTO.getId()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(officeDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Office in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchOffice() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        office.setId(longCount.incrementAndGet());

        // Create the Office
        OfficeDTO officeDTO = officeMapper.toDto(office);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restOfficeMockMvc
            .perform(
                put(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(officeDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Office in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamOffice() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        office.setId(longCount.incrementAndGet());

        // Create the Office
        OfficeDTO officeDTO = officeMapper.toDto(office);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restOfficeMockMvc
            .perform(put(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(officeDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Office in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateOfficeWithPatch() throws Exception {
        // Initialize the database
        insertedOffice = officeRepository.saveAndFlush(office);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the office using partial update
        Office partialUpdatedOffice = new Office();
        partialUpdatedOffice.setId(office.getId());

        restOfficeMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedOffice.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedOffice))
            )
            .andExpect(status().isOk());

        // Validate the Office in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertOfficeUpdatableFieldsEquals(createUpdateProxyForBean(partialUpdatedOffice, office), getPersistedOffice(office));
    }

    @Test
    @Transactional
    void fullUpdateOfficeWithPatch() throws Exception {
        // Initialize the database
        insertedOffice = officeRepository.saveAndFlush(office);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the office using partial update
        Office partialUpdatedOffice = new Office();
        partialUpdatedOffice.setId(office.getId());

        partialUpdatedOffice
            .code(UPDATED_CODE)
            .name(UPDATED_NAME)
            .officeType(UPDATED_OFFICE_TYPE)
            .isHub(UPDATED_IS_HUB)
            .active(UPDATED_ACTIVE);

        restOfficeMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedOffice.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedOffice))
            )
            .andExpect(status().isOk());

        // Validate the Office in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertOfficeUpdatableFieldsEquals(partialUpdatedOffice, getPersistedOffice(partialUpdatedOffice));
    }

    @Test
    @Transactional
    void patchNonExistingOffice() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        office.setId(longCount.incrementAndGet());

        // Create the Office
        OfficeDTO officeDTO = officeMapper.toDto(office);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restOfficeMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, officeDTO.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(officeDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Office in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchOffice() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        office.setId(longCount.incrementAndGet());

        // Create the Office
        OfficeDTO officeDTO = officeMapper.toDto(office);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restOfficeMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(officeDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Office in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamOffice() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        office.setId(longCount.incrementAndGet());

        // Create the Office
        OfficeDTO officeDTO = officeMapper.toDto(office);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restOfficeMockMvc
            .perform(patch(ENTITY_API_URL).contentType("application/merge-patch+json").content(om.writeValueAsBytes(officeDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Office in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteOffice() throws Exception {
        // Initialize the database
        insertedOffice = officeRepository.saveAndFlush(office);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the office
        restOfficeMockMvc
            .perform(delete(ENTITY_API_URL_ID, office.getId()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return officeRepository.count();
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

    protected Office getPersistedOffice(Office office) {
        return officeRepository.findById(office.getId()).orElseThrow();
    }

    protected void assertPersistedOfficeToMatchAllProperties(Office expectedOffice) {
        assertOfficeAllPropertiesEquals(expectedOffice, getPersistedOffice(expectedOffice));
    }

    protected void assertPersistedOfficeToMatchUpdatableProperties(Office expectedOffice) {
        assertOfficeAllUpdatablePropertiesEquals(expectedOffice, getPersistedOffice(expectedOffice));
    }
}
