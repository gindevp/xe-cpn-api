package com.mycompany.myapp.repository;

import com.mycompany.myapp.domain.OrderDeliveryAttempt;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the OrderDeliveryAttempt entity.
 */
@Repository
public interface OrderDeliveryAttemptRepository extends JpaRepository<OrderDeliveryAttempt, Long> {
    default Optional<OrderDeliveryAttempt> findOneWithEagerRelationships(Long id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<OrderDeliveryAttempt> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<OrderDeliveryAttempt> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select orderDeliveryAttempt from OrderDeliveryAttempt orderDeliveryAttempt left join fetch orderDeliveryAttempt.order",
        countQuery = "select count(orderDeliveryAttempt) from OrderDeliveryAttempt orderDeliveryAttempt"
    )
    Page<OrderDeliveryAttempt> findAllWithToOneRelationships(Pageable pageable);

    @Query("select orderDeliveryAttempt from OrderDeliveryAttempt orderDeliveryAttempt left join fetch orderDeliveryAttempt.order")
    List<OrderDeliveryAttempt> findAllWithToOneRelationships();

    @Query(
        "select orderDeliveryAttempt from OrderDeliveryAttempt orderDeliveryAttempt left join fetch orderDeliveryAttempt.order where orderDeliveryAttempt.id =:id"
    )
    Optional<OrderDeliveryAttempt> findOneWithToOneRelationships(@Param("id") Long id);

    long countByOrder_Id(Long orderId);

    List<OrderDeliveryAttempt> findByOrder_IdOrderByAttemptNoAsc(Long orderId);

    List<OrderDeliveryAttempt> findByOrder_IdOrderByAttemptAtAsc(Long orderId);
}
