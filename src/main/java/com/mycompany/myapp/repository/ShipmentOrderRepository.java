package com.mycompany.myapp.repository;

import com.mycompany.myapp.domain.ShipmentOrder;
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

    @Query("select max(shipmentOrder.orderCode) from ShipmentOrder shipmentOrder where shipmentOrder.orderCode like concat(:prefix, '%')")
    Optional<String> findMaxOrderCodeByPrefix(@Param("prefix") String prefix);

    boolean existsByDraftCode(String draftCode);

    boolean existsByOrderCode(String orderCode);

    List<ShipmentOrder> findByCurrentTrip_Id(Long tripId);
}
