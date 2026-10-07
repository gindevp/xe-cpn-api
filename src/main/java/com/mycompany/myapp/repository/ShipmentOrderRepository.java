package com.mycompany.myapp.repository;

import com.mycompany.myapp.domain.ShipmentOrder;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the ShipmentOrder entity.
 */
@Repository
public interface ShipmentOrderRepository extends JpaRepository<ShipmentOrder, Long>, JpaSpecificationExecutor<ShipmentOrder> {
    default Optional<ShipmentOrder> findOneWithEagerRelationships(Long id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<ShipmentOrder> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<ShipmentOrder> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select shipmentOrder from ShipmentOrder shipmentOrder left join fetch shipmentOrder.senderCustomer left join fetch shipmentOrder.fromOffice left join fetch shipmentOrder.toOffice left join fetch shipmentOrder.hubOffice left join fetch shipmentOrder.finalToOffice left join fetch shipmentOrder.currentTrip",
        countQuery = "select count(shipmentOrder) from ShipmentOrder shipmentOrder"
    )
    Page<ShipmentOrder> findAllWithToOneRelationships(Pageable pageable);

    @Query(
        "select shipmentOrder from ShipmentOrder shipmentOrder left join fetch shipmentOrder.senderCustomer left join fetch shipmentOrder.fromOffice left join fetch shipmentOrder.toOffice left join fetch shipmentOrder.hubOffice left join fetch shipmentOrder.finalToOffice left join fetch shipmentOrder.currentTrip"
    )
    List<ShipmentOrder> findAllWithToOneRelationships();

    @Query(
        "select shipmentOrder from ShipmentOrder shipmentOrder left join fetch shipmentOrder.senderCustomer left join fetch shipmentOrder.fromOffice left join fetch shipmentOrder.toOffice left join fetch shipmentOrder.hubOffice left join fetch shipmentOrder.finalToOffice left join fetch shipmentOrder.currentTrip where shipmentOrder.id =:id"
    )
    Optional<ShipmentOrder> findOneWithToOneRelationships(@Param("id") Long id);

    Optional<ShipmentOrder> findOneByOrderCode(String orderCode);

    Optional<ShipmentOrder> findOneByDraftCode(String draftCode);

    Optional<ShipmentOrder> findFirstByPartnerOrderId(String partnerOrderId);

    /** [shipperId, số đơn] — đơn đang giao (OUT_FOR_DELIVERY) mỗi shipper nội bộ đang giữ. */
    @Query(
        "select o.shipper.id, count(o) from ShipmentOrder o where o.shipper.id in :ids " +
        "and o.status = com.mycompany.myapp.domain.enumeration.OrderStatus.OUT_FOR_DELIVERY group by o.shipper.id"
    )
    List<Object[]> countOutForDeliveryByShipper(@Param("ids") List<Long> ids);

    @Query(
        """
        select shipmentOrder from ShipmentOrder shipmentOrder
        left join fetch shipmentOrder.fromOffice
        left join fetch shipmentOrder.toOffice
        left join fetch shipmentOrder.hubOffice
        left join fetch shipmentOrder.finalToOffice
        left join fetch shipmentOrder.currentTrip
        where shipmentOrder.orderCode = :code or shipmentOrder.draftCode = :code
        """
    )
    Optional<ShipmentOrder> findOneByOrderCodeOrDraftCode(@Param("code") String code);

    @Query(
        "select o from ShipmentOrder o left join fetch o.fromOffice left join fetch o.toOffice left join fetch o.finalToOffice" +
        " where o.orderCode in :codes"
    )
    List<ShipmentOrder> findWithOfficesByOrderCodeIn(@Param("codes") java.util.Collection<String> codes);

    /**
     * Mã đơn cùng VP + ngày, để lấy số thứ tự kế tiếp.
     * Không dùng max() SQL vì so chuỗi sẽ sai khi có hậu tố 4 chữ số ("999" > "1000") — số lớn nhất tính trong Java.
     */
    @Query("select shipmentOrder.orderCode from ShipmentOrder shipmentOrder where shipmentOrder.orderCode like concat(:prefix, '%')")
    List<String> findOrderCodesByPrefix(@Param("prefix") String prefix);

    boolean existsByDraftCode(String draftCode);

    boolean existsByOrderCode(String orderCode);

    /** Trùng đuôi mã (để 4 ký tự cuối tìm đơn ít đụng). */
    boolean existsByOrderCodeEndingWithIgnoreCase(String suffix);

    List<ShipmentOrder> findByCurrentTrip_Id(Long tripId);

    /** Đơn gửi trả chờ tự xuất HĐ: đã nhập kho gửi trong [from, to], chưa có trạng thái HĐ, không công nợ, không ngoại lệ mở. */
    @Query(
        """
        select o.id from ShipmentOrder o
        left join o.issue iss
        where (o.paymentTerm is null or o.paymentTerm = com.mycompany.myapp.domain.enumeration.PaymentTerm.GUI_TRA)
        and (iss is null or iss.issueStatus <> com.mycompany.myapp.domain.enumeration.IssueStatus.OPEN)
        and o.pickedUpAt >= :from and o.pickedUpAt <= :to
        and o.status in (com.mycompany.myapp.domain.enumeration.OrderStatus.DELIVERED, com.mycompany.myapp.domain.enumeration.OrderStatus.RETURNED)
        and o.status not in :excluded
        and (o.invoiceStatus is null or o.invoiceStatus = 'SKIPPED')
        and (o.onCredit is null or o.onCredit = false)
        and coalesce(o.fareAmount, 0) <= coalesce(o.paidAmount, 0)
        order by o.pickedUpAt asc
        """
    )
    List<Long> findAutoInvoiceWarehouseInIds(
        @Param("from") java.time.Instant from,
        @Param("to") java.time.Instant to,
        @Param("excluded") java.util.Collection<com.mycompany.myapp.domain.enumeration.OrderStatus> excluded,
        Pageable pageable
    );

    /** Đơn gửi trả nhập kho gửi trong [from, to) — màn Quản lý hoá đơn. */
    @Query(
        """
        select o from ShipmentOrder o
        left join fetch o.fromOffice
        left join fetch o.toOffice
        left join fetch o.finalToOffice
        where (o.paymentTerm is null or o.paymentTerm = com.mycompany.myapp.domain.enumeration.PaymentTerm.GUI_TRA)
        and o.pickedUpAt >= :from and o.pickedUpAt < :to
        and o.status not in :excluded
        """
    )
    List<ShipmentOrder> findInvoiceWarehouseInBetween(
        @Param("from") java.time.Instant from,
        @Param("to") java.time.Instant to,
        @Param("excluded") java.util.Collection<com.mycompany.myapp.domain.enumeration.OrderStatus> excluded
    );

    @Query(
        """
        select o from ShipmentOrder o
        left join fetch o.fromOffice
        left join fetch o.toOffice
        left join fetch o.finalToOffice
        where o.id in :ids
        """
    )
    List<ShipmentOrder> findAllWithOfficesByIdIn(@Param("ids") java.util.Collection<Long> ids);

    /**
     * Đơn có thông tin HĐ công ty mà SĐT (chỉ chữ số, gồm dạng 0… và 84…) là người gửi hoặc người nhận.
     * So khớp sau khi bỏ dấu cách / chấm / gạch, nên {@code 0901 234 567} vẫn khớp {@code 0901234567}.
     */
    @Query(
        """
        select o from ShipmentOrder o
        where o.invoiceTaxCode is not null and o.invoiceCompanyName is not null
        and (
            function('regexp_replace', o.senderPhone, '[^0-9]', '') in :phones
            or function('regexp_replace', o.receiverPhone, '[^0-9]', '') in :phones
        )
        order by o.id desc
        """
    )
    List<ShipmentOrder> findInvoiceProfilesByPhone(@Param("phones") java.util.Collection<String> phones, Pageable pageable);

    @Query(
        """
        select o.senderName from ShipmentOrder o
        where o.senderPhone = :phone and o.senderName is not null and o.senderName <> ''
        order by o.id desc
        """
    )
    List<String> findLatestSenderNames(@Param("phone") String phone, Pageable pageable);

    /**
     * Đơn nhập kho giao (AT_DEST) quá mốc, chưa có sự cố đang mở.
     * {@code forwardStage} null = đơn cũ chưa ghi stage, màn hình vẫn xếp vào nhập kho giao.
     */
    @Query(
        """
        select o.id from ShipmentOrder o
        left join o.issue i
        where o.status = :status
          and (o.forwardStage is null or o.forwardStage = :stage)
          and coalesce(o.updatedAt, o.createdAt) < :cutoff
          and (i is null or i.issueStatus <> :open or i.resolvedAt is not null)
        order by o.id
        """
    )
    List<Long> findStaleDestWarehouseIds(
        @Param("status") com.mycompany.myapp.domain.enumeration.OrderStatus status,
        @Param("stage") com.mycompany.myapp.domain.enumeration.ForwardStage stage,
        @Param("cutoff") Instant cutoff,
        @Param("open") com.mycompany.myapp.domain.enumeration.IssueStatus open,
        Pageable pageable
    );
}
