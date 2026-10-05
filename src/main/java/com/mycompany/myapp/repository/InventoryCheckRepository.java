package com.mycompany.myapp.repository;

import com.mycompany.myapp.domain.InventoryCheck;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface InventoryCheckRepository extends JpaRepository<InventoryCheck, Long> {
    List<InventoryCheck> findByOfficeCodeIgnoreCaseOrderByCheckedAtDesc(String officeCode);

    List<InventoryCheck> findAllByOrderByCheckedAtDesc();

    List<InventoryCheck> findByOfficeCodeIgnoreCaseAndStatusNotOrderByCheckedAtDesc(String officeCode, String status);

    List<InventoryCheck> findByStatusNotOrderByCheckedAtDesc(String status);

    Optional<InventoryCheck> findOneByOpenOfficeCode(String openOfficeCode);

    List<InventoryCheck> findByStatus(String status);

    Optional<InventoryCheck> findFirstByOfficeCodeIgnoreCaseAndStatusOrderByCheckedAtDesc(String officeCode, String status);

    /** Quét kiện giữ khoá chia sẻ, Hoàn tất giữ khoá ghi — kiện không lọt sau lúc chốt biên bản. */
    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("select c from InventoryCheck c where c.id = :id")
    Optional<InventoryCheck> findForShare(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from InventoryCheck c where c.id = :id")
    Optional<InventoryCheck> findForUpdate(@Param("id") Long id);
}
