package com.mycompany.myapp.repository;

import com.mycompany.myapp.domain.Vehicle;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Vehicle entity.
 */
@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
    Optional<Vehicle> findOneByPlateNumber(String plateNumber);

    @Query("select v from Vehicle v left join fetch v.office left join fetch v.defaultDriver")
    List<Vehicle> findAllWithRefs();

    @Query("select v from Vehicle v left join fetch v.office left join fetch v.defaultDriver where v.id = :id")
    Optional<Vehicle> findOneWithRefs(@Param("id") Long id);
}
