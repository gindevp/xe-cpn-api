package com.mycompany.myapp.repository;

import com.mycompany.myapp.domain.PartnerFeeExpense;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PartnerFeeExpenseRepository extends JpaRepository<PartnerFeeExpense, Long> {
    boolean existsByPartnerOrderId(String partnerOrderId);

    /** Phí chưa trừ vào phiếu thu nào, kèm đơn + VP. */
    @Query(
        "select e from PartnerFeeExpense e join fetch e.order o left join fetch o.fromOffice left join fetch o.toOffice " +
        "left join fetch o.finalToOffice where e.receipt is null order by e.incurredAt"
    )
    List<PartnerFeeExpense> findOpenWithOrder();

    @Query("select e from PartnerFeeExpense e where e.receipt is null and e.order.id = :orderId and lower(e.payerUsername) = lower(:payer)")
    List<PartnerFeeExpense> findOpenByOrderAndPayer(@Param("orderId") Long orderId, @Param("payer") String payer);

    @Query("select e from PartnerFeeExpense e join fetch e.order where e.receipt is null and lower(e.payerUsername) = lower(:payer)")
    List<PartnerFeeExpense> findOpenByPayer(@Param("payer") String payer);

    List<PartnerFeeExpense> findByReceipt_Id(Long receiptId);

    List<PartnerFeeExpense> findByOrder_Id(Long orderId);
}
