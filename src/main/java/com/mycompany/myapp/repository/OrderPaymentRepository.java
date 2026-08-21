package com.mycompany.myapp.repository;

import com.mycompany.myapp.domain.OrderPayment;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the OrderPayment entity.
 */
@Repository
public interface OrderPaymentRepository extends JpaRepository<OrderPayment, Long> {
    default Optional<OrderPayment> findOneWithEagerRelationships(Long id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<OrderPayment> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<OrderPayment> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select orderPayment from OrderPayment orderPayment left join fetch orderPayment.order",
        countQuery = "select count(orderPayment) from OrderPayment orderPayment"
    )
    Page<OrderPayment> findAllWithToOneRelationships(Pageable pageable);

    @Query("select orderPayment from OrderPayment orderPayment left join fetch orderPayment.order")
    List<OrderPayment> findAllWithToOneRelationships();

    @Query("select orderPayment from OrderPayment orderPayment left join fetch orderPayment.order where orderPayment.id =:id")
    Optional<OrderPayment> findOneWithToOneRelationships(@Param("id") Long id);

    long countByPaymentAtGreaterThanEqualAndPaymentAtLessThan(Instant from, Instant to);

    Optional<OrderPayment> findFirstByOrder_IdOrderByPaymentAtDesc(Long orderId);
}
