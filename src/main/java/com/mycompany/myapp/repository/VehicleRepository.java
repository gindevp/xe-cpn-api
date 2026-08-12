package com.mycompany.myapp.repository;

import com.mycompany.myapp.domain.Vehicle;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Vehicle entity.
 */
@SuppressWarnings("unused")
@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
    Optional<Vehicle> findOneByPlateNumber(String plateNumber);
}
