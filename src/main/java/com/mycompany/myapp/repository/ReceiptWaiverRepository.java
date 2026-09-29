package com.mycompany.myapp.repository;

import com.mycompany.myapp.domain.ReceiptWaiver;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ReceiptWaiverRepository extends JpaRepository<ReceiptWaiver, Long> {
    /** [orderId, portion, sum(amount)]. */
    @Query(
        "select w.order.id, w.portion, coalesce(sum(w.amount), 0) from ReceiptWaiver w where w.order.id in :orderIds group by w.order.id, w.portion"
    )
    List<Object[]> sumByOrderIds(@Param("orderIds") Collection<Long> orderIds);
}
