package com.mycompany.myapp.repository;

import com.mycompany.myapp.domain.OnePayCallLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OnePayCallLogRepository extends JpaRepository<OnePayCallLog, Long> {
    Page<OnePayCallLog> findAllByOrderByIdDesc(Pageable pageable);
}
