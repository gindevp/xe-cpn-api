package com.mycompany.myapp.web.rest;

import static com.mycompany.myapp.domain.StaffProfileAsserts.*;
import static com.mycompany.myapp.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.myapp.IntegrationTest;
import com.mycompany.myapp.domain.Office;
import com.mycompany.myapp.domain.StaffProfile;
import com.mycompany.myapp.domain.enumeration.RoleCode;
import com.mycompany.myapp.repository.StaffProfileRepository;
import com.mycompany.myapp.service.StaffProfileService;
import com.mycompany.myapp.service.dto.StaffProfileDTO;
import com.mycompany.myapp.service.mapper.StaffProfileMapper;
import jakarta.persistence.EntityManager;
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
 * Integration tests for the {@link StaffProfileResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser
class StaffProfileResourceIT {

    private static final String DEFAULT_STAFF_CODE = "AAAAAAAAAA";
    private static final String UPDATED_STAFF_CODE = "BBBBBBBBBB";

    private static final String DEFAULT_USER_LOGIN = "AAAAAAAAAA";
    private static final String UPDATED_USER_LOGIN = "BBBBBBBBBB";

    private static final String DEFAULT_DISPLAY_NAME = "AAAAAAAAAA";
    private static final String UPDATED_DISPLAY_NAME = "BBBBBBBBBB";

    private static final RoleCode DEFAULT_ROLE_CODE = RoleCode.KH;
    private static final RoleCode UPDATED_ROLE_CODE = RoleCode.Q;

    private static final Boolean DEFAULT_SCOPE_ALL_OFFICES = false;
    private static final Boolean UPDATED_SCOPE_ALL_OFFICES = true;

    private static final Boolean DEFAULT_ACTIVE = false;
    private static final Boolean UPDATED_ACTIVE = true;

    private static final String ENTITY_API_URL = "/api/staff-profiles";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static Random random = new Random();
    private static AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    @Autowired
    private ObjectMapper om;

    @Autowired
    private StaffProfileRepository staffProfileRepository;

    @Mock
    private StaffProfileRepository staffProfileRepositoryMock;

    @Autowired
    private StaffProfileMapper staffProfileMapper;

    @Mock
    private StaffProfileService staffProfileServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restStaffProfileMockMvc;

    private StaffProfile staffProfile;

    private StaffProfile insertedStaffProfile;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static StaffProfile createEntity(EntityManager em) {
        StaffProfile staffProfile = new StaffProfile()
            .staffCode(DEFAULT_STAFF_CODE)
            .userLogin(DEFAULT_USER_LOGIN)
            .displayName(DEFAULT_DISPLAY_NAME)
            .roleCode(DEFAULT_ROLE_CODE)
            .scopeAllOffices(DEFAULT_SCOPE_ALL_OFFICES)
            .active(DEFAULT_ACTIVE);
        // Add required entity
        Office office;
        if (TestUtil.findAll(em, Office.class).isEmpty()) {
            office = OfficeResourceIT.createEntity();
            em.persist(office);
            em.flush();
        } else {
            office = TestUtil.findAll(em, Office.class).get(0);
        }
        staffProfile.setOffice(office);
        return staffProfile;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static StaffProfile createUpdatedEntity(EntityManager em) {
        StaffProfile updatedStaffProfile = new StaffProfile()
            .staffCode(UPDATED_STAFF_CODE)
            .userLogin(UPDATED_USER_LOGIN)
            .displayName(UPDATED_DISPLAY_NAME)
            .roleCode(UPDATED_ROLE_CODE)
            .scopeAllOffices(UPDATED_SCOPE_ALL_OFFICES)
            .active(UPDATED_ACTIVE);
        // Add required entity
        Office office;
        if (TestUtil.findAll(em, Office.class).isEmpty()) {
            office = OfficeResourceIT.createUpdatedEntity();
            em.persist(office);
            em.flush();
        } else {
            office = TestUtil.findAll(em, Office.class).get(0);
        }
        updatedStaffProfile.setOffice(office);
        return updatedStaffProfile;
    }

    @BeforeEach
    public void initTest() {
        staffProfile = createEntity(em);
    }

    @AfterEach
    public void cleanup() {
        if (insertedStaffProfile != null) {
            staffProfileRepository.delete(insertedStaffProfile);
            insertedStaffProfile = null;
        }
    }

    @Test
    @Transactional
    void createStaffProfile() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the StaffProfile
        StaffProfileDTO staffProfileDTO = staffProfileMapper.toDto(staffProfile);
        var returnedStaffProfileDTO = om.readValue(
            restStaffProfileMockMvc
                .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(staffProfileDTO)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            StaffProfileDTO.class
        );

        // Validate the StaffProfile in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedStaffProfile = staffProfileMapper.toEntity(returnedStaffProfileDTO);
        assertStaffProfileUpdatableFieldsEquals(returnedStaffProfile, getPersistedStaffProfile(returnedStaffProfile));

        insertedStaffProfile = returnedStaffProfile;
    }

    @Test
    @Transactional
    void createStaffProfileWithExistingId() throws Exception {
        // Create the StaffProfile with an existing ID
        staffProfile.setId(1L);
        StaffProfileDTO staffProfileDTO = staffProfileMapper.toDto(staffProfile);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restStaffProfileMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(staffProfileDTO)))
            .andExpect(status().isBadRequest());

        // Validate the StaffProfile in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkUserLoginIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        staffProfile.setUserLogin(null);

        // Create the StaffProfile, which fails.
        StaffProfileDTO staffProfileDTO = staffProfileMapper.toDto(staffProfile);

        restStaffProfileMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(staffProfileDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkRoleCodeIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        staffProfile.setRoleCode(null);

        // Create the StaffProfile, which fails.
        StaffProfileDTO staffProfileDTO = staffProfileMapper.toDto(staffProfile);

        restStaffProfileMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(staffProfileDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkScopeAllOfficesIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        staffProfile.setScopeAllOffices(null);

        // Create the StaffProfile, which fails.
        StaffProfileDTO staffProfileDTO = staffProfileMapper.toDto(staffProfile);

        restStaffProfileMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(staffProfileDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkActiveIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        staffProfile.setActive(null);

        // Create the StaffProfile, which fails.
        StaffProfileDTO staffProfileDTO = staffProfileMapper.toDto(staffProfile);

        restStaffProfileMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(staffProfileDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllStaffProfiles() throws Exception {
        // Initialize the database
        insertedStaffProfile = staffProfileRepository.saveAndFlush(staffProfile);

        // Get all the staffProfileList
        restStaffProfileMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(staffProfile.getId().intValue())))
            .andExpect(jsonPath("$.[*].staffCode").value(hasItem(DEFAULT_STAFF_CODE)))
            .andExpect(jsonPath("$.[*].userLogin").value(hasItem(DEFAULT_USER_LOGIN)))
            .andExpect(jsonPath("$.[*].displayName").value(hasItem(DEFAULT_DISPLAY_NAME)))
            .andExpect(jsonPath("$.[*].roleCode").value(hasItem(DEFAULT_ROLE_CODE.toString())))
            .andExpect(jsonPath("$.[*].scopeAllOffices").value(hasItem(DEFAULT_SCOPE_ALL_OFFICES)))
            .andExpect(jsonPath("$.[*].active").value(hasItem(DEFAULT_ACTIVE)));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllStaffProfilesWithEagerRelationshipsIsEnabled() throws Exception {
        when(staffProfileServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restStaffProfileMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(staffProfileServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllStaffProfilesWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(staffProfileServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restStaffProfileMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(staffProfileRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getStaffProfile() throws Exception {
        // Initialize the database
        insertedStaffProfile = staffProfileRepository.saveAndFlush(staffProfile);

        // Get the staffProfile
        restStaffProfileMockMvc
            .perform(get(ENTITY_API_URL_ID, staffProfile.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(staffProfile.getId().intValue()))
            .andExpect(jsonPath("$.staffCode").value(DEFAULT_STAFF_CODE))
            .andExpect(jsonPath("$.userLogin").value(DEFAULT_USER_LOGIN))
            .andExpect(jsonPath("$.displayName").value(DEFAULT_DISPLAY_NAME))
            .andExpect(jsonPath("$.roleCode").value(DEFAULT_ROLE_CODE.toString()))
            .andExpect(jsonPath("$.scopeAllOffices").value(DEFAULT_SCOPE_ALL_OFFICES))
            .andExpect(jsonPath("$.active").value(DEFAULT_ACTIVE));
    }

    @Test
    @Transactional
    void getNonExistingStaffProfile() throws Exception {
        // Get the staffProfile
        restStaffProfileMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingStaffProfile() throws Exception {
        // Initialize the database
        insertedStaffProfile = staffProfileRepository.saveAndFlush(staffProfile);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the staffProfile
        StaffProfile updatedStaffProfile = staffProfileRepository.findById(staffProfile.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedStaffProfile are not directly saved in db
        em.detach(updatedStaffProfile);
        updatedStaffProfile
            .staffCode(UPDATED_STAFF_CODE)
            .userLogin(UPDATED_USER_LOGIN)
            .displayName(UPDATED_DISPLAY_NAME)
            .roleCode(UPDATED_ROLE_CODE)
            .scopeAllOffices(UPDATED_SCOPE_ALL_OFFICES)
            .active(UPDATED_ACTIVE);
        StaffProfileDTO staffProfileDTO = staffProfileMapper.toDto(updatedStaffProfile);

        restStaffProfileMockMvc
            .perform(
                put(ENTITY_API_URL_ID, staffProfileDTO.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(staffProfileDTO))
            )
            .andExpect(status().isOk());

        // Validate the StaffProfile in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedStaffProfileToMatchAllProperties(updatedStaffProfile);
    }

    @Test
    @Transactional
    void putNonExistingStaffProfile() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        staffProfile.setId(longCount.incrementAndGet());

        // Create the StaffProfile
        StaffProfileDTO staffProfileDTO = staffProfileMapper.toDto(staffProfile);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restStaffProfileMockMvc
            .perform(
                put(ENTITY_API_URL_ID, staffProfileDTO.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(staffProfileDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the StaffProfile in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchStaffProfile() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        staffProfile.setId(longCount.incrementAndGet());

        // Create the StaffProfile
        StaffProfileDTO staffProfileDTO = staffProfileMapper.toDto(staffProfile);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restStaffProfileMockMvc
            .perform(
                put(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(staffProfileDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the StaffProfile in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamStaffProfile() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        staffProfile.setId(longCount.incrementAndGet());

        // Create the StaffProfile
        StaffProfileDTO staffProfileDTO = staffProfileMapper.toDto(staffProfile);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restStaffProfileMockMvc
            .perform(put(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(staffProfileDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the StaffProfile in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateStaffProfileWithPatch() throws Exception {
        // Initialize the database
        insertedStaffProfile = staffProfileRepository.saveAndFlush(staffProfile);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the staffProfile using partial update
        StaffProfile partialUpdatedStaffProfile = new StaffProfile();
        partialUpdatedStaffProfile.setId(staffProfile.getId());

        partialUpdatedStaffProfile.staffCode(UPDATED_STAFF_CODE).displayName(UPDATED_DISPLAY_NAME).active(UPDATED_ACTIVE);

        restStaffProfileMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedStaffProfile.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedStaffProfile))
            )
            .andExpect(status().isOk());

        // Validate the StaffProfile in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertStaffProfileUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedStaffProfile, staffProfile),
            getPersistedStaffProfile(staffProfile)
        );
    }

    @Test
    @Transactional
    void fullUpdateStaffProfileWithPatch() throws Exception {
        // Initialize the database
        insertedStaffProfile = staffProfileRepository.saveAndFlush(staffProfile);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the staffProfile using partial update
        StaffProfile partialUpdatedStaffProfile = new StaffProfile();
        partialUpdatedStaffProfile.setId(staffProfile.getId());

        partialUpdatedStaffProfile
            .staffCode(UPDATED_STAFF_CODE)
            .userLogin(UPDATED_USER_LOGIN)
            .displayName(UPDATED_DISPLAY_NAME)
            .roleCode(UPDATED_ROLE_CODE)
            .scopeAllOffices(UPDATED_SCOPE_ALL_OFFICES)
            .active(UPDATED_ACTIVE);

        restStaffProfileMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedStaffProfile.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedStaffProfile))
            )
            .andExpect(status().isOk());

        // Validate the StaffProfile in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertStaffProfileUpdatableFieldsEquals(partialUpdatedStaffProfile, getPersistedStaffProfile(partialUpdatedStaffProfile));
    }

    @Test
    @Transactional
    void patchNonExistingStaffProfile() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        staffProfile.setId(longCount.incrementAndGet());

        // Create the StaffProfile
        StaffProfileDTO staffProfileDTO = staffProfileMapper.toDto(staffProfile);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restStaffProfileMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, staffProfileDTO.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(staffProfileDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the StaffProfile in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchStaffProfile() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        staffProfile.setId(longCount.incrementAndGet());

        // Create the StaffProfile
        StaffProfileDTO staffProfileDTO = staffProfileMapper.toDto(staffProfile);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restStaffProfileMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(staffProfileDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the StaffProfile in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamStaffProfile() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        staffProfile.setId(longCount.incrementAndGet());

        // Create the StaffProfile
        StaffProfileDTO staffProfileDTO = staffProfileMapper.toDto(staffProfile);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restStaffProfileMockMvc
            .perform(patch(ENTITY_API_URL).contentType("application/merge-patch+json").content(om.writeValueAsBytes(staffProfileDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the StaffProfile in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteStaffProfile() throws Exception {
        // Initialize the database
        insertedStaffProfile = staffProfileRepository.saveAndFlush(staffProfile);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the staffProfile
        restStaffProfileMockMvc
            .perform(delete(ENTITY_API_URL_ID, staffProfile.getId()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return staffProfileRepository.count();
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

    protected StaffProfile getPersistedStaffProfile(StaffProfile staffProfile) {
        return staffProfileRepository.findById(staffProfile.getId()).orElseThrow();
    }

    protected void assertPersistedStaffProfileToMatchAllProperties(StaffProfile expectedStaffProfile) {
        assertStaffProfileAllPropertiesEquals(expectedStaffProfile, getPersistedStaffProfile(expectedStaffProfile));
    }

    protected void assertPersistedStaffProfileToMatchUpdatableProperties(StaffProfile expectedStaffProfile) {
        assertStaffProfileAllUpdatablePropertiesEquals(expectedStaffProfile, getPersistedStaffProfile(expectedStaffProfile));
    }
}
