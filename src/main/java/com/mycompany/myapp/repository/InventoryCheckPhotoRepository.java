package com.mycompany.myapp.repository;

import com.mycompany.myapp.domain.InventoryCheckPhoto;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface InventoryCheckPhotoRepository extends JpaRepository<InventoryCheckPhoto, Long> {
    List<InventoryCheckPhoto> findBySessionKeyAndOrderCodeIgnoreCaseOrderByPackageSeqAscCapturedAtAsc(String sessionKey, String orderCode);

    /** [orderCode, capturedAt, capturedByUsername] từng ảnh — không tải nội dung ảnh. */
    @Query("select p.orderCode, p.capturedAt, p.capturedByUsername from InventoryCheckPhoto p where p.sessionKey = :sessionKey")
    List<Object[]> scanMetaBySession(@Param("sessionKey") String sessionKey);

    /** Id ảnh đầu tiên (nhỏ nhất) của mỗi đơn trong {@code orderCodes} (mã viết hoa). */
    @Query(
        "select min(p.id) from InventoryCheckPhoto p where p.sessionKey = :sessionKey and upper(p.orderCode) in :orderCodes group by upper(p.orderCode)"
    )
    List<Long> firstPhotoIds(@Param("sessionKey") String sessionKey, @Param("orderCodes") java.util.Collection<String> orderCodes);

    @Modifying
    @Query("delete from InventoryCheckPhoto p where p.capturedAt < :cutoff")
    int deleteCapturedBefore(@Param("cutoff") Instant cutoff);

    /** Ảnh của phiên quét không bấm Hoàn tất (không có biên bản mang session_key). */
    @Modifying
    @Query(
        "delete from InventoryCheckPhoto p where p.capturedAt < :cutoff and not exists " +
        "(select 1 from InventoryCheck c where c.sessionKey = p.sessionKey)"
    )
    int deleteAbandonedBefore(@Param("cutoff") Instant cutoff);
}
