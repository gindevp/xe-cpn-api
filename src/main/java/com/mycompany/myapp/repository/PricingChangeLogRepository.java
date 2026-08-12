package com.mycompany.myapp.repository;

import com.mycompany.myapp.domain.PricingChangeLog;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the PricingChangeLog entity.
 */
@Repository
public interface PricingChangeLogRepository extends JpaRepository<PricingChangeLog, Long> {
    default Optional<PricingChangeLog> findOneWithEagerRelationships(Long id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<PricingChangeLog> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<PricingChangeLog> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select pricingChangeLog from PricingChangeLog pricingChangeLog left join fetch pricingChangeLog.pricingRule",
        countQuery = "select count(pricingChangeLog) from PricingChangeLog pricingChangeLog"
    )
    Page<PricingChangeLog> findAllWithToOneRelationships(Pageable pageable);

    @Query("select pricingChangeLog from PricingChangeLog pricingChangeLog left join fetch pricingChangeLog.pricingRule")
    List<PricingChangeLog> findAllWithToOneRelationships();

    @Query(
        "select pricingChangeLog from PricingChangeLog pricingChangeLog left join fetch pricingChangeLog.pricingRule where pricingChangeLog.id =:id"
    )
    Optional<PricingChangeLog> findOneWithToOneRelationships(@Param("id") Long id);
}
