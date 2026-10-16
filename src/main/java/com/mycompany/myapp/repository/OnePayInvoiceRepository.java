package com.mycompany.myapp.repository;

import com.mycompany.myapp.domain.OnePayInvoice;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface OnePayInvoiceRepository extends JpaRepository<OnePayInvoice, Long> {
    List<OnePayInvoice> findAllByOrderByIdDesc();

    Optional<OnePayInvoice> findOneByUserReference(String userReference);

    Optional<OnePayInvoice> findOneByOnepayInvoiceId(String onepayInvoiceId);

    Optional<OnePayInvoice> findOneByAccountNumber(String accountNumber);

    Optional<OnePayInvoice> findFirstByOrderCodeOrderByIdDesc(String orderCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from OnePayInvoice i where i.id = :id")
    Optional<OnePayInvoice> findByIdForUpdate(@Param("id") Long id);
}
