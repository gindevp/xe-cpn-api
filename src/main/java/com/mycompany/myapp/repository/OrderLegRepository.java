package com.mycompany.myapp.repository;

import com.mycompany.myapp.domain.OrderLeg;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the OrderLeg entity.
 */
@Repository
public interface OrderLegRepository extends JpaRepository<OrderLeg, Long> {
    default Optional<OrderLeg> findOneWithEagerRelationships(Long id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<OrderLeg> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<OrderLeg> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select orderLeg from OrderLeg orderLeg left join fetch orderLeg.order left join fetch orderLeg.fromOffice left join fetch orderLeg.toOffice left join fetch orderLeg.trip",
        countQuery = "select count(orderLeg) from OrderLeg orderLeg"
    )
    Page<OrderLeg> findAllWithToOneRelationships(Pageable pageable);

    @Query(
        "select orderLeg from OrderLeg orderLeg left join fetch orderLeg.order left join fetch orderLeg.fromOffice left join fetch orderLeg.toOffice left join fetch orderLeg.trip"
    )
    List<OrderLeg> findAllWithToOneRelationships();

    @Query(
        "select orderLeg from OrderLeg orderLeg left join fetch orderLeg.order left join fetch orderLeg.fromOffice left join fetch orderLeg.toOffice left join fetch orderLeg.trip where orderLeg.id =:id"
    )
    Optional<OrderLeg> findOneWithToOneRelationships(@Param("id") Long id);

    List<OrderLeg> findByOrder_IdOrderByLegIndexAsc(Long orderId);
}
