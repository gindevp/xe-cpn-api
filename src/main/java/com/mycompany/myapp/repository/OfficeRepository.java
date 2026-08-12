package com.mycompany.myapp.repository;

import com.mycompany.myapp.domain.Office;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Office entity.
 */
@SuppressWarnings("unused")
@Repository
public interface OfficeRepository extends JpaRepository<Office, Long> {
    Optional<Office> findOneByCode(String code);
}
