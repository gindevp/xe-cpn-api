package com.mycompany.myapp.repository;

import com.mycompany.myapp.domain.BidvTestBill;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface BidvTestBillRepository extends JpaRepository<BidvTestBill, Long> {
    Optional<BidvTestBill> findOneByCustomerIdIgnoreCase(String customerId);

    List<BidvTestBill> findAllByOrderByIdDesc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from BidvTestBill b where b.id = :id")
    Optional<BidvTestBill> findByIdForUpdate(@Param("id") Long id);
}
