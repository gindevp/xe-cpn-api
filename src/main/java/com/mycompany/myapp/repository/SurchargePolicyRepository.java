package com.mycompany.myapp.repository;

import com.mycompany.myapp.domain.SurchargePolicy;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the SurchargePolicy entity.
 */
@SuppressWarnings("unused")
@Repository
public interface SurchargePolicyRepository extends JpaRepository<SurchargePolicy, Long> {}
