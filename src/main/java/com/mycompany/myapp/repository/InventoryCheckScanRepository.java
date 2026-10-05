package com.mycompany.myapp.repository;

import com.mycompany.myapp.domain.InventoryCheckScan;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface InventoryCheckScanRepository extends JpaRepository<InventoryCheckScan, Long> {
    List<InventoryCheckScan> findByCheckIdAndIdGreaterThanOrderByIdAsc(Long checkId, Long sinceId);

    List<InventoryCheckScan> findByCheckIdOrderByIdAsc(Long checkId);

    Optional<InventoryCheckScan> findOneByCheckIdAndOrderCodeAndPackageSeq(Long checkId, String orderCode, Integer packageSeq);

    /** [checkId, scannedByUsername, số kiện] — người tham gia từng phiên. */
    @Query(
        "select s.checkId, s.scannedByUsername, count(s) from InventoryCheckScan s where s.checkId in :checkIds " +
        "group by s.checkId, s.scannedByUsername"
    )
    List<Object[]> participants(@Param("checkIds") Collection<Long> checkIds);
}
