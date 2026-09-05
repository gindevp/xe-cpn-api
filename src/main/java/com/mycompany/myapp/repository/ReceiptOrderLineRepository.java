package com.mycompany.myapp.repository;

import com.mycompany.myapp.domain.ReceiptOrderLine;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the ReceiptOrderLine entity.
 */
@Repository
public interface ReceiptOrderLineRepository extends JpaRepository<ReceiptOrderLine, Long> {
    default Optional<ReceiptOrderLine> findOneWithEagerRelationships(Long id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<ReceiptOrderLine> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<ReceiptOrderLine> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select receiptOrderLine from ReceiptOrderLine receiptOrderLine left join fetch receiptOrderLine.receipt left join fetch receiptOrderLine.order",
        countQuery = "select count(receiptOrderLine) from ReceiptOrderLine receiptOrderLine"
    )
    Page<ReceiptOrderLine> findAllWithToOneRelationships(Pageable pageable);

    @Query(
        "select receiptOrderLine from ReceiptOrderLine receiptOrderLine left join fetch receiptOrderLine.receipt left join fetch receiptOrderLine.order"
    )
    List<ReceiptOrderLine> findAllWithToOneRelationships();

    @Query(
        "select receiptOrderLine from ReceiptOrderLine receiptOrderLine left join fetch receiptOrderLine.receipt left join fetch receiptOrderLine.order where receiptOrderLine.id =:id"
    )
    Optional<ReceiptOrderLine> findOneWithToOneRelationships(@Param("id") Long id);

    List<ReceiptOrderLine> findByReceipt_Id(Long receiptId);

    boolean existsByOrder_Id(Long orderId);
}
