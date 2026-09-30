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

    /** Danh sách phiếu thu không đọc cột ảnh chứng từ (LONGTEXT ~50–100KB/phiếu) — chỉ trả cờ có ảnh. */
    @Query(
        value = "select new com.mycompany.myapp.repository.ReceiptListRow(r.id, r.receiptCode, r.payerName, r.payerCode, r.totalAmount," +
        " r.createdAt, r.createdByUsername, o.code, r.confirmedAt, r.confirmedByUsername," +
        " case when r.confirmProofImage is null then false else true end)" +
        " from Receipt r left join r.office o" +
        LIST_WHERE,
        countQuery = "select count(r) from Receipt r left join r.office o" + LIST_WHERE
    )
    Page<ReceiptListRow> findListRows(
        @Param("officeCode") String officeCode,
        @Param("createdBy") String createdBy,
        @Param("codeLike") String codeLike,
        @Param("payerLike") String payerLike,
        @Param("creatorLike") String creatorLike,
        @Param("createdFrom") Instant createdFrom,
        @Param("createdTo") Instant createdTo,
        Pageable pageable
    );

    @Query("select coalesce(sum(r.totalAmount), 0) from Receipt r left join r.office o" + LIST_WHERE)
    java.math.BigDecimal sumListTotal(
        @Param("officeCode") String officeCode,
        @Param("createdBy") String createdBy,
        @Param("codeLike") String codeLike,
        @Param("payerLike") String payerLike,
        @Param("creatorLike") String creatorLike,
        @Param("createdFrom") Instant createdFrom,
        @Param("createdTo") Instant createdTo
    );

    /** Tham số *Like đã là "%...%" chữ thường; createdTo loại trừ. */
    String LIST_WHERE =
        " where (:officeCode is null or o.code = :officeCode) and (:createdBy is null or r.createdByUsername = :createdBy)" +
        " and (:codeLike is null or lower(r.receiptCode) like :codeLike)" +
        " and (:payerLike is null or lower(coalesce(r.payerCode, '')) like :payerLike or lower(coalesce(r.payerName, '')) like :payerLike)" +
        " and (:creatorLike is null or lower(r.createdByUsername) like :creatorLike)" +
        " and (:createdFrom is null or r.createdAt >= :createdFrom) and (:createdTo is null or r.createdAt < :createdTo)";

    @Query("select r.confirmProofImage from Receipt r where r.receiptCode = :code")
    Optional<String> findProofImageByCode(@Param("code") String code);
}
