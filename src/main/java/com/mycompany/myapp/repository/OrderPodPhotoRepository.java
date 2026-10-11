package com.mycompany.myapp.repository;

import com.mycompany.myapp.domain.OrderPodPhoto;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the OrderPodPhoto entity.
 */
@Repository
public interface OrderPodPhotoRepository extends JpaRepository<OrderPodPhoto, Long> {
    default Optional<OrderPodPhoto> findOneWithEagerRelationships(Long id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<OrderPodPhoto> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<OrderPodPhoto> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select orderPodPhoto from OrderPodPhoto orderPodPhoto left join fetch orderPodPhoto.order",
        countQuery = "select count(orderPodPhoto) from OrderPodPhoto orderPodPhoto"
    )
    Page<OrderPodPhoto> findAllWithToOneRelationships(Pageable pageable);

    @Query("select orderPodPhoto from OrderPodPhoto orderPodPhoto left join fetch orderPodPhoto.order")
    List<OrderPodPhoto> findAllWithToOneRelationships();

    @Query("select orderPodPhoto from OrderPodPhoto orderPodPhoto left join fetch orderPodPhoto.order where orderPodPhoto.id =:id")
    Optional<OrderPodPhoto> findOneWithToOneRelationships(@Param("id") Long id);

    List<OrderPodPhoto> findByOrder_IdOrderBySequenceNoAsc(Long orderId);

    long countByOrder_Id(Long orderId);

    boolean existsByOrder_Id(Long orderId);

    /** Đơn nào trong lô có ảnh POD — không đọc nội dung ảnh. */
    @Query("select distinct p.order.id from OrderPodPhoto p where p.order.id in :orderIds")
    List<Long> findOrderIdsIn(@Param("orderIds") Collection<Long> orderIds);
}
