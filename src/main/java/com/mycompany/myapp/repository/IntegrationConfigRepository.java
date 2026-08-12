package com.mycompany.myapp.repository;

import com.mycompany.myapp.domain.IntegrationConfig;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the IntegrationConfig entity.
 */
@SuppressWarnings("unused")
@Repository
public interface IntegrationConfigRepository extends JpaRepository<IntegrationConfig, Long> {}
