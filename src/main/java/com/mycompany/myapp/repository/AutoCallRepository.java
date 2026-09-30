package com.mycompany.myapp.repository;

import com.mycompany.myapp.domain.AutoCall;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AutoCallRepository extends JpaRepository<AutoCall, Long> {
    Optional<AutoCall> findOneByRefId(String refId);

    Optional<AutoCall> findFirstByCallId(String callId);

    long countByOrder_IdAndCallType(Long orderId, String callType);

    List<AutoCall> findByOrder_IdOrderByCreatedAtDesc(Long orderId);

    @Query("select a.refId as refId, o.orderCode as orderCode from AutoCall a join a.order o where a.refId in :refIds")
    List<RefOrderCode> findOrderCodesByRefIds(@Param("refIds") Collection<String> refIds);

    interface RefOrderCode {
        String getRefId();

        String getOrderCode();
    }
}
