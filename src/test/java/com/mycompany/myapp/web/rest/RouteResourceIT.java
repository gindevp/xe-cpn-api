package com.mycompany.myapp.web.rest;

import static com.mycompany.myapp.domain.RouteAsserts.*;
import static com.mycompany.myapp.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.myapp.IntegrationTest;
import com.mycompany.myapp.domain.Office;
import com.mycompany.myapp.domain.Route;
import com.mycompany.myapp.repository.RouteRepository;
import com.mycompany.myapp.service.RouteService;
import com.mycompany.myapp.service.dto.RouteDTO;
import com.mycompany.myapp.service.mapper.RouteMapper;
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
 * Integration tests for the {@link RouteResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser
class RouteResourceIT {

    private static final String DEFAULT_CODE = "AAAAAAAAAA";
    private static final String UPDATED_CODE = "BBBBBBBBBB";

    private static final String DEFAULT_NAME = "AAAAAAAAAA";
    private static final String UPDATED_NAME = "BBBBBBBBBB";

    private static final Boolean DEFAULT_ACTIVE = false;
    private static final Boolean UPDATED_ACTIVE = true;

    private static final String ENTITY_API_URL = "/api/routes";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static Random random = new Random();
    private static AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    @Autowired
    private ObjectMapper om;

    @Autowired
    private RouteRepository routeRepository;

    @Mock
    private RouteRepository routeRepositoryMock;

    @Autowired
    private RouteMapper routeMapper;

    @Mock
    private RouteService routeServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restRouteMockMvc;

    private Route route;

    private Route insertedRoute;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Route createEntity(EntityManager em) {
        Route route = new Route().code(DEFAULT_CODE).name(DEFAULT_NAME).active(DEFAULT_ACTIVE);
        // Add required entity
        Office office;
        if (TestUtil.findAll(em, Office.class).isEmpty()) {
            office = OfficeResourceIT.createEntity();
            em.persist(office);
            em.flush();
        } else {
            office = TestUtil.findAll(em, Office.class).get(0);
        }
        route.setFromOffice(office);
        // Add required entity
        route.setToOffice(office);
        return route;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Route createUpdatedEntity(EntityManager em) {
        Route updatedRoute = new Route().code(UPDATED_CODE).name(UPDATED_NAME).active(UPDATED_ACTIVE);
        // Add required entity
        Office office;
        if (TestUtil.findAll(em, Office.class).isEmpty()) {
            office = OfficeResourceIT.createUpdatedEntity();
            em.persist(office);
            em.flush();
        } else {
            office = TestUtil.findAll(em, Office.class).get(0);
        }
        updatedRoute.setFromOffice(office);
        // Add required entity
        updatedRoute.setToOffice(office);
        return updatedRoute;
    }

    @BeforeEach
    public void initTest() {
        route = createEntity(em);
    }

    @AfterEach
    public void cleanup() {
        if (insertedRoute != null) {
            routeRepository.delete(insertedRoute);
            insertedRoute = null;
        }
    }

    @Test
    @Transactional
    void createRoute() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the Route
        RouteDTO routeDTO = routeMapper.toDto(route);
        var returnedRouteDTO = om.readValue(
            restRouteMockMvc
                .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(routeDTO)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            RouteDTO.class
        );

        // Validate the Route in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedRoute = routeMapper.toEntity(returnedRouteDTO);
        assertRouteUpdatableFieldsEquals(returnedRoute, getPersistedRoute(returnedRoute));

        insertedRoute = returnedRoute;
    }

    @Test
    @Transactional
    void createRouteWithExistingId() throws Exception {
        // Create the Route with an existing ID
        route.setId(1L);
        RouteDTO routeDTO = routeMapper.toDto(route);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restRouteMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(routeDTO)))
            .andExpect(status().isBadRequest());

        // Validate the Route in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkCodeIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        route.setCode(null);

        // Create the Route, which fails.
        RouteDTO routeDTO = routeMapper.toDto(route);

        restRouteMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(routeDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkNameIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        route.setName(null);

        // Create the Route, which fails.
        RouteDTO routeDTO = routeMapper.toDto(route);

        restRouteMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(routeDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkActiveIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        route.setActive(null);

        // Create the Route, which fails.
        RouteDTO routeDTO = routeMapper.toDto(route);

        restRouteMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(routeDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllRoutes() throws Exception {
        // Initialize the database
        insertedRoute = routeRepository.saveAndFlush(route);

        // Get all the routeList
        restRouteMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(route.getId().intValue())))
            .andExpect(jsonPath("$.[*].code").value(hasItem(DEFAULT_CODE)))
            .andExpect(jsonPath("$.[*].name").value(hasItem(DEFAULT_NAME)))
            .andExpect(jsonPath("$.[*].active").value(hasItem(DEFAULT_ACTIVE)));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllRoutesWithEagerRelationshipsIsEnabled() throws Exception {
        when(routeServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restRouteMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(routeServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllRoutesWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(routeServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restRouteMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(routeRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getRoute() throws Exception {
        // Initialize the database
        insertedRoute = routeRepository.saveAndFlush(route);

        // Get the route
        restRouteMockMvc
            .perform(get(ENTITY_API_URL_ID, route.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(route.getId().intValue()))
            .andExpect(jsonPath("$.code").value(DEFAULT_CODE))
            .andExpect(jsonPath("$.name").value(DEFAULT_NAME))
            .andExpect(jsonPath("$.active").value(DEFAULT_ACTIVE));
    }

    @Test
    @Transactional
    void getNonExistingRoute() throws Exception {
        // Get the route
        restRouteMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingRoute() throws Exception {
        // Initialize the database
        insertedRoute = routeRepository.saveAndFlush(route);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the route
        Route updatedRoute = routeRepository.findById(route.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedRoute are not directly saved in db
        em.detach(updatedRoute);
        updatedRoute.code(UPDATED_CODE).name(UPDATED_NAME).active(UPDATED_ACTIVE);
        RouteDTO routeDTO = routeMapper.toDto(updatedRoute);

        restRouteMockMvc
            .perform(
                put(ENTITY_API_URL_ID, routeDTO.getId()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(routeDTO))
            )
            .andExpect(status().isOk());

        // Validate the Route in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedRouteToMatchAllProperties(updatedRoute);
    }

    @Test
    @Transactional
    void putNonExistingRoute() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        route.setId(longCount.incrementAndGet());

        // Create the Route
        RouteDTO routeDTO = routeMapper.toDto(route);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restRouteMockMvc
            .perform(
                put(ENTITY_API_URL_ID, routeDTO.getId()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(routeDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Route in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchRoute() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        route.setId(longCount.incrementAndGet());

        // Create the Route
        RouteDTO routeDTO = routeMapper.toDto(route);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restRouteMockMvc
            .perform(
                put(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(routeDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Route in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamRoute() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        route.setId(longCount.incrementAndGet());

        // Create the Route
        RouteDTO routeDTO = routeMapper.toDto(route);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restRouteMockMvc
            .perform(put(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(routeDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Route in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateRouteWithPatch() throws Exception {
        // Initialize the database
        insertedRoute = routeRepository.saveAndFlush(route);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the route using partial update
        Route partialUpdatedRoute = new Route();
        partialUpdatedRoute.setId(route.getId());

        partialUpdatedRoute.code(UPDATED_CODE).active(UPDATED_ACTIVE);

        restRouteMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedRoute.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedRoute))
            )
            .andExpect(status().isOk());

        // Validate the Route in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertRouteUpdatableFieldsEquals(createUpdateProxyForBean(partialUpdatedRoute, route), getPersistedRoute(route));
    }

    @Test
    @Transactional
    void fullUpdateRouteWithPatch() throws Exception {
        // Initialize the database
        insertedRoute = routeRepository.saveAndFlush(route);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the route using partial update
        Route partialUpdatedRoute = new Route();
        partialUpdatedRoute.setId(route.getId());

        partialUpdatedRoute.code(UPDATED_CODE).name(UPDATED_NAME).active(UPDATED_ACTIVE);

        restRouteMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedRoute.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedRoute))
            )
            .andExpect(status().isOk());

        // Validate the Route in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertRouteUpdatableFieldsEquals(partialUpdatedRoute, getPersistedRoute(partialUpdatedRoute));
    }

    @Test
    @Transactional
    void patchNonExistingRoute() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        route.setId(longCount.incrementAndGet());

        // Create the Route
        RouteDTO routeDTO = routeMapper.toDto(route);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restRouteMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, routeDTO.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(routeDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Route in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchRoute() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        route.setId(longCount.incrementAndGet());

        // Create the Route
        RouteDTO routeDTO = routeMapper.toDto(route);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restRouteMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(routeDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Route in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamRoute() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        route.setId(longCount.incrementAndGet());

        // Create the Route
        RouteDTO routeDTO = routeMapper.toDto(route);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restRouteMockMvc
            .perform(patch(ENTITY_API_URL).contentType("application/merge-patch+json").content(om.writeValueAsBytes(routeDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Route in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteRoute() throws Exception {
        // Initialize the database
        insertedRoute = routeRepository.saveAndFlush(route);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the route
        restRouteMockMvc
            .perform(delete(ENTITY_API_URL_ID, route.getId()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return routeRepository.count();
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

    protected Route getPersistedRoute(Route route) {
        return routeRepository.findById(route.getId()).orElseThrow();
    }

    protected void assertPersistedRouteToMatchAllProperties(Route expectedRoute) {
        assertRouteAllPropertiesEquals(expectedRoute, getPersistedRoute(expectedRoute));
    }

    protected void assertPersistedRouteToMatchUpdatableProperties(Route expectedRoute) {
        assertRouteAllUpdatablePropertiesEquals(expectedRoute, getPersistedRoute(expectedRoute));
    }
}
