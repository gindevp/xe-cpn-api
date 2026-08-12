package com.mycompany.myapp.repository;

import com.mycompany.myapp.domain.OrderEvent;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the OrderEvent entity.
 */
@Repository
public interface OrderEventRepository extends JpaRepository<OrderEvent, Long> {
    default Optional<OrderEvent> findOneWithEagerRelationships(Long id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<OrderEvent> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<OrderEvent> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select orderEvent from OrderEvent orderEvent left join fetch orderEvent.order",
        countQuery = "select count(orderEvent) from OrderEvent orderEvent"
    )
    Page<OrderEvent> findAllWithToOneRelationships(Pageable pageable);

    @Query("select orderEvent from OrderEvent orderEvent left join fetch orderEvent.order")
    List<OrderEvent> findAllWithToOneRelationships();

    @Query("select orderEvent from OrderEvent orderEvent left join fetch orderEvent.order where orderEvent.id =:id")
    Optional<OrderEvent> findOneWithToOneRelationships(@Param("id") Long id);

    List<OrderEvent> findByOrder_IdOrderByEventAtAsc(Long orderId);
}
