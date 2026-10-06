package com.mycompany.myapp.repository;

import com.mycompany.myapp.domain.Receipt;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Receipt entity.
 */
@Repository
public interface ReceiptRepository extends JpaRepository<Receipt, Long>, JpaSpecificationExecutor<Receipt> {
    default Optional<Receipt> findOneWithEagerRelationships(Long id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<Receipt> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<Receipt> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select receipt from Receipt receipt left join fetch receipt.office",
        countQuery = "select count(receipt) from Receipt receipt"
    )
    Page<Receipt> findAllWithToOneRelationships(Pageable pageable);

    @Query("select receipt from Receipt receipt left join fetch receipt.office")
    List<Receipt> findAllWithToOneRelationships();

    @Query("select receipt from Receipt receipt left join fetch receipt.office where receipt.id =:id")
    Optional<Receipt> findOneWithToOneRelationships(@Param("id") Long id);

    long countByReceiptCodeStartingWith(String prefix);

    Optional<Receipt> findOneByReceiptCode(String receiptCode);

    boolean existsByReceiptCode(String receiptCode);

    String LIST_ROW_SELECT =
        "select new com.mycompany.myapp.repository.ReceiptListRow(r.id, r.receiptCode, r.payerName, r.payerCode, r.totalAmount," +
        " r.createdAt, r.createdByUsername, o.code, r.confirmedAt, r.confirmedByUsername," +
        " case when r.confirmProofImage is null and r.transferProofImage is null then false else true end," +
        " case when r.transferProofImage is null then false else true end, r.confirmNote)" +
        " from Receipt r left join r.office o";

    /** Danh sách phiếu thu không đọc cột ảnh chứng từ (LONGTEXT ~50–100KB/phiếu) — chỉ trả cờ có ảnh. */
    @Query(value = LIST_ROW_SELECT + LIST_WHERE, countQuery = "select count(r) from Receipt r left join r.office o" + LIST_WHERE)
    Page<ReceiptListRow> findListRows(
        @Param("officeCode") String officeCode,
        @Param("createdBy") String createdBy,
        @Param("codeLike") String codeLike,
        @Param("payerLike") String payerLike,
        @Param("creatorLike") String creatorLike,
        @Param("createdFrom") Instant createdFrom,
        @Param("createdTo") Instant createdTo,
        @Param("status") String status,
        @Param("paidFrom") Instant paidFrom,
        @Param("paidTo") Instant paidTo,
        Pageable pageable
    );

    @Query("select r.id from Receipt r left join r.office o" + LIST_WHERE)
    List<Long> findListIds(
        @Param("officeCode") String officeCode,
        @Param("createdBy") String createdBy,
        @Param("codeLike") String codeLike,
        @Param("payerLike") String payerLike,
        @Param("creatorLike") String creatorLike,
        @Param("createdFrom") Instant createdFrom,
        @Param("createdTo") Instant createdTo,
        @Param("status") String status,
        @Param("paidFrom") Instant paidFrom,
        @Param("paidTo") Instant paidTo
    );

    @Query("select coalesce(sum(r.totalAmount), 0) from Receipt r left join r.office o" + LIST_WHERE)
    java.math.BigDecimal sumListTotal(
        @Param("officeCode") String officeCode,
        @Param("createdBy") String createdBy,
        @Param("codeLike") String codeLike,
        @Param("payerLike") String payerLike,
        @Param("creatorLike") String creatorLike,
        @Param("createdFrom") Instant createdFrom,
        @Param("createdTo") Instant createdTo,
        @Param("status") String status,
        @Param("paidFrom") Instant paidFrom,
        @Param("paidTo") Instant paidTo
    );

    /** Tiền khách trả của đơn {@code l.order} (bỏ dòng RECEIPT* nộp quỹ) — khớp OrderPaymentRepository#latestCustomerPaymentAtByOrderIds. */
    String CUSTOMER_PAY_SUB =
        "select 1 from OrderPayment p where p.order = l.order and p.paymentAt is not null" +
        " and upper(trim(coalesce(p.note, ''))) not like 'RECEIPT%'";

    /** Sự kiện coi như khách đã trả khi đơn chưa có dòng tiền — phải khớp FinanceFacadeService.CUSTOMER_PAID_EVENT_ACTIONS. */
    String CUSTOMER_PAID_EVENT_SUB =
        "select 1 from OrderEvent e where e.order = l.order and e.eventAt is not null" +
        " and upper(e.action) in ('POD', 'POD_QUAY', 'POD_HOME', 'DELIVERED', 'WAREHOUSE_RECEIVE', 'WH_IN')";

    /** Phiếu có ít nhất 1 đơn có mốc khách trả; không có thì ngày thu tiền = ngày lập phiếu. */
    String HAS_PAID_LINE =
        "exists (select 1 from ReceiptOrderLine l where l.receipt = r and (exists (" +
        CUSTOMER_PAY_SUB +
        ") or exists (" +
        CUSTOMER_PAID_EVENT_SUB +
        ")))";

    String PAID_LINE_ON_OR_AFTER_FROM =
        "exists (select 1 from ReceiptOrderLine l where l.receipt = r and (exists (" +
        CUSTOMER_PAY_SUB +
        " and p.paymentAt >= :paidFrom) or (not exists (" +
        CUSTOMER_PAY_SUB +
        ") and exists (" +
        CUSTOMER_PAID_EVENT_SUB +
        " and e.eventAt >= :paidFrom))))";

    String PAID_LINE_ON_OR_AFTER_TO =
        "exists (select 1 from ReceiptOrderLine l where l.receipt = r and (exists (" +
        CUSTOMER_PAY_SUB +
        " and p.paymentAt >= :paidTo) or (not exists (" +
        CUSTOMER_PAY_SUB +
        ") and exists (" +
        CUSTOMER_PAID_EVENT_SUB +
        " and e.eventAt >= :paidTo))))";

    /**
     * Tham số *Like đã là "%...%" chữ thường; createdTo/paidTo loại trừ; status CONFIRMED (đã thu) / PENDING (chưa thu).
     * paidFrom/paidTo lọc theo ngày thu tiền = mốc khách trả muộn nhất trong các đơn của phiếu (như ReceiptDTO.customerPaidAt).
     */
    String LIST_WHERE =
        " where (:officeCode is null or o.code = :officeCode) and (:createdBy is null or r.createdByUsername = :createdBy)" +
        " and (:codeLike is null or lower(r.receiptCode) like :codeLike)" +
        " and (:payerLike is null or lower(coalesce(r.payerCode, '')) like :payerLike or lower(coalesce(r.payerName, '')) like :payerLike)" +
        " and (:creatorLike is null or lower(r.createdByUsername) like :creatorLike)" +
        " and (:createdFrom is null or r.createdAt >= :createdFrom) and (:createdTo is null or r.createdAt < :createdTo)" +
        " and (:paidFrom is null or " +
        PAID_LINE_ON_OR_AFTER_FROM +
        " or (not " +
        HAS_PAID_LINE +
        " and r.createdAt >= :paidFrom))" +
        " and (:paidTo is null or (" +
        HAS_PAID_LINE +
        " and not " +
        PAID_LINE_ON_OR_AFTER_TO +
        ") or (not " +
        HAS_PAID_LINE +
        " and r.createdAt < :paidTo))" +
        " and (:status is null or (:status = 'CONFIRMED' and r.confirmedAt is not null) or (:status = 'PENDING' and r.confirmedAt is null))";

    /** Phiếu NV tự nộp (payerCode = login), mới nhất trước. */
    @Query(LIST_ROW_SELECT + " where lower(r.payerCode) = lower(:payer) and r.createdAt >= :from order by r.id desc")
    List<ReceiptListRow> findPayerRowsSince(@Param("payer") String payer, @Param("from") Instant from);
}
