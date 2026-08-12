package com.mycompany.myapp.repository;

import com.mycompany.myapp.domain.PricingRule;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the PricingRule entity.
 */
@Repository
public interface PricingRuleRepository extends JpaRepository<PricingRule, Long>, JpaSpecificationExecutor<PricingRule> {
    default Optional<PricingRule> findOneWithEagerRelationships(Long id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<PricingRule> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<PricingRule> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select pricingRule from PricingRule pricingRule left join fetch pricingRule.route",
        countQuery = "select count(pricingRule) from PricingRule pricingRule"
    )
    Page<PricingRule> findAllWithToOneRelationships(Pageable pageable);

    @Query("select pricingRule from PricingRule pricingRule left join fetch pricingRule.route")
    List<PricingRule> findAllWithToOneRelationships();

    @Query("select pricingRule from PricingRule pricingRule left join fetch pricingRule.route where pricingRule.id =:id")
    Optional<PricingRule> findOneWithToOneRelationships(@Param("id") Long id);
}
