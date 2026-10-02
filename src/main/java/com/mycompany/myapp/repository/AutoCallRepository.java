package com.mycompany.myapp.repository;

import com.mycompany.myapp.domain.AutoCall;
import com.mycompany.myapp.domain.enumeration.OrderStatus;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AutoCallRepository extends JpaRepository<AutoCall, Long> {
    Optional<AutoCall> findOneByRefId(String refId);

    Optional<AutoCall> findFirstByCallId(String callId);

    long countByOrder_IdAndCallType(Long orderId, String callType);

    List<AutoCall> findByOrder_IdOrderByCreatedAtDesc(Long orderId);

    @Query("select a.id from AutoCall a where a.nextRetryAt is not null and a.nextRetryAt <= :now order by a.nextRetryAt")
    List<Long> findDueRetryIds(@Param("now") Instant now);

    /** Cuộc gọi chưa gửi đang chờ đến khung giờ gọi của các đơn ở {@code statuses} → bỏ qua. */
    @Modifying
    @Query(
        "update AutoCall a set a.status = 'SKIPPED', a.errorCode = :code, a.errorMessage = :message, a.nextRetryAt = null " +
        "where a.nextRetryAt is not null and a.status = 'PENDING' and a.callId is null and a.order.id in " +
        "(select o.id from ShipmentOrder o where o.status in :statuses)"
    )
    int skipScheduledForOrderStatuses(
        @Param("statuses") Collection<OrderStatus> statuses,
        @Param("code") String code,
        @Param("message") String message
    );

    @Modifying
    @Query(
        "update AutoCall a set a.status = 'SKIPPED', a.errorCode = :code, a.errorMessage = :message, a.nextRetryAt = null " +
        "where a.nextRetryAt is not null and a.status = 'PENDING' and a.callId is null and a.order.id = :orderId"
    )
    int skipScheduledForOrder(@Param("orderId") Long orderId, @Param("code") String code, @Param("message") String message);

    /** Đơn đã giao / huỷ / hoàn → bỏ mọi lịch gọi lại. */
    @Modifying
    @Query(
        "update AutoCall a set a.nextRetryAt = null where a.nextRetryAt is not null and a.order.id in " +
        "(select o.id from ShipmentOrder o where o.status in :statuses)"
    )
    int clearRetriesForOrderStatuses(@Param("statuses") Collection<OrderStatus> statuses);

    @Modifying
    @Query("update AutoCall a set a.nextRetryAt = null where a.nextRetryAt is not null and a.order.id = :orderId")
    int clearRetriesForOrder(@Param("orderId") Long orderId);

    boolean existsByOrder_IdAndCreatedAtAfter(Long orderId, Instant createdAt);

    @Query("select a.refId as refId, o.orderCode as orderCode from AutoCall a join a.order o where a.refId in :refIds")
    List<RefOrderCode> findOrderCodesByRefIds(@Param("refIds") Collection<String> refIds);

    interface RefOrderCode {
        String getRefId();

        String getOrderCode();
    }
}
