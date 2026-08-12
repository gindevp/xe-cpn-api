package com.mycompany.myapp.repository;

import com.mycompany.myapp.domain.ProductPriceRule;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the ProductPriceRule entity.
 */
@SuppressWarnings("unused")
@Repository
public interface ProductPriceRuleRepository extends JpaRepository<ProductPriceRule, Long> {}
