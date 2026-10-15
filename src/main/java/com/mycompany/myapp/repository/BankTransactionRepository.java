package com.mycompany.myapp.repository;

import com.mycompany.myapp.domain.BankTransaction;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface BankTransactionRepository extends JpaRepository<BankTransaction, Long> {
    Optional<BankTransaction> findOneByTransId(String transId);

    Optional<BankTransaction> findFirstByTargetTypeAndTargetIdOrderByIdDesc(String targetType, Long targetId);

    List<BankTransaction> findByTargetTypeAndTargetIdIn(String targetType, Collection<Long> targetIds);

    @Modifying
    @Query("delete from BankTransaction t where t.targetType = :targetType")
    int deleteByTargetType(@Param("targetType") String targetType);

    @Modifying
    @Query("delete from BankTransaction t where t.targetType = :targetType and t.targetId = :targetId")
    int deleteByTarget(@Param("targetType") String targetType, @Param("targetId") Long targetId);
}
