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

    boolean existsByOrder_IdAndActionAndActorUsername(Long orderId, String action, String actorUsername);

    /** Hàng: [orderId, actorUsername] của sự kiện tạo đơn ("customer" = khách tự tạo). */
    @Query("select e.order.id, e.actorUsername from OrderEvent e where e.order.id in :orderIds and upper(e.action) = 'CREATE'")
    List<Object[]> creatorsByOrderIds(@Param("orderIds") java.util.Collection<Long> orderIds);

    /** Hàng: [orderId, max(eventAt)] của các sự kiện thuộc {@code actions}. */
    @Query(
        "select e.order.id, max(e.eventAt) from OrderEvent e where e.order.id in :orderIds and upper(e.action) in :actions group by e.order.id"
    )
    List<Object[]> latestEventAtByOrderIds(
        @Param("orderIds") java.util.Collection<Long> orderIds,
        @Param("actions") java.util.Collection<String> actions
    );

    /** Hàng: [orderId, upper(action), max(eventAt)] — tách theo từng action. */
    @Query(
        "select e.order.id, upper(e.action), max(e.eventAt) from OrderEvent e where e.order.id in :orderIds and upper(e.action) in :actions group by e.order.id, upper(e.action)"
    )
    List<Object[]> latestEventAtByOrderIdsAndAction(
        @Param("orderIds") java.util.Collection<Long> orderIds,
        @Param("actions") java.util.Collection<String> actions
    );

    /** Đơn nhận trả/COD/chia tỉ lệ đã giao trong [from, to], chưa có trạng thái HĐ, không công nợ — chờ tự xuất HĐ. */
    @Query(
        """
        select distinct e.order.id from OrderEvent e
        where upper(e.action) in :actions and e.eventAt >= :from and e.eventAt <= :to
        and e.order.status = com.mycompany.myapp.domain.enumeration.OrderStatus.DELIVERED
        and e.order.paymentTerm is not null
        and e.order.paymentTerm <> com.mycompany.myapp.domain.enumeration.PaymentTerm.GUI_TRA
        and (e.order.invoiceStatus is null or e.order.invoiceStatus = 'SKIPPED')
        and (e.order.onCredit is null or e.order.onCredit = false)
        and coalesce(e.order.fareAmount, 0) <= coalesce(e.order.paidAmount, 0)
        """
    )
    List<Long> findAutoInvoiceDeliveredIds(
        @Param("actions") java.util.Collection<String> actions,
        @Param("from") java.time.Instant from,
        @Param("to") java.time.Instant to,
        Pageable pageable
    );

    /** Hàng [orderId, lúc giao gần nhất] của đơn không phải gửi trả, giao thành công trong [from, to). */
    @Query(
        """
        select e.order.id, max(e.eventAt) from OrderEvent e
        where upper(e.action) in :actions
        and e.order.status = com.mycompany.myapp.domain.enumeration.OrderStatus.DELIVERED
        and e.order.paymentTerm is not null
        and e.order.paymentTerm <> com.mycompany.myapp.domain.enumeration.PaymentTerm.GUI_TRA
        group by e.order.id
        having max(e.eventAt) >= :from and max(e.eventAt) < :to
        """
    )
    List<Object[]> findInvoiceDeliveredBetween(
        @Param("actions") java.util.Collection<String> actions,
        @Param("from") java.time.Instant from,
        @Param("to") java.time.Instant to
    );
}
