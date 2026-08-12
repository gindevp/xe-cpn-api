package com.mycompany.myapp.repository;

import com.mycompany.myapp.domain.DoorFeeRule;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the DoorFeeRule entity.
 */
@SuppressWarnings("unused")
@Repository
public interface DoorFeeRuleRepository extends JpaRepository<DoorFeeRule, Long> {}
