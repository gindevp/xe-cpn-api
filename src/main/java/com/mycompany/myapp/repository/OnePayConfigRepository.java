package com.mycompany.myapp.repository;

import com.mycompany.myapp.domain.OnePayConfig;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OnePayConfigRepository extends JpaRepository<OnePayConfig, Long> {
    Optional<OnePayConfig> findFirstByOrderByIdAsc();
}
