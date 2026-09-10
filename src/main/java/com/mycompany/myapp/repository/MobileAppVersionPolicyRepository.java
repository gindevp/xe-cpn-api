package com.mycompany.myapp.repository;

import com.mycompany.myapp.domain.MobileAppVersionPolicy;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the MobileAppVersionPolicy entity.
 */
@SuppressWarnings("unused")
@Repository
public interface MobileAppVersionPolicyRepository extends JpaRepository<MobileAppVersionPolicy, Long> {}
