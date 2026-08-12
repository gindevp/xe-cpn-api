package com.mycompany.myapp.repository;

import com.mycompany.myapp.domain.Driver;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Driver entity.
 */
@SuppressWarnings("unused")
@Repository
public interface DriverRepository extends JpaRepository<Driver, Long> {
    Optional<Driver> findOneByDriverCode(String driverCode);

    Optional<Driver> findFirstByFullNameIgnoreCase(String fullName);
}
