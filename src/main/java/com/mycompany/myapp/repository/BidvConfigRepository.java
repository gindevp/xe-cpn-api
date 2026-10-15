package com.mycompany.myapp.repository;

import com.mycompany.myapp.domain.BidvConfig;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BidvConfigRepository extends JpaRepository<BidvConfig, Long> {
    Optional<BidvConfig> findFirstByOrderByIdAsc();
}
