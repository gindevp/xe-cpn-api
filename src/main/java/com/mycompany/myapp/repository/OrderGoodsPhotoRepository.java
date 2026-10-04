package com.mycompany.myapp.repository;

import com.mycompany.myapp.domain.OrderGoodsPhoto;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderGoodsPhotoRepository extends JpaRepository<OrderGoodsPhoto, Long> {
    Optional<OrderGoodsPhoto> findOneByOrderId(Long orderId);

    boolean existsByOrderId(Long orderId);

    /** Đơn nào trong lô có ảnh — không đọc cột ảnh. */
    @Query("select p.orderId from OrderGoodsPhoto p where p.orderId in :orderIds")
    List<Long> findOrderIdsIn(@Param("orderIds") Collection<Long> orderIds);
}
