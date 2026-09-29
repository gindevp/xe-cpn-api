package com.mycompany.myapp.repository;

import com.mycompany.myapp.domain.AuditLog;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the AuditLog entity.
 */
@SuppressWarnings("unused")
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long>, JpaSpecificationExecutor<AuditLog> {
    List<AuditLog> findByEntityTypeInAndActedAtGreaterThanEqualAndActedAtLessThanOrderByActedAtDesc(
        Collection<String> entityTypes,
        Instant from,
        Instant to
    );
}
