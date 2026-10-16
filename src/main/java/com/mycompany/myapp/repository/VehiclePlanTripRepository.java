package com.mycompany.myapp.repository;

import com.mycompany.myapp.domain.VehiclePlanTrip;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VehiclePlanTripRepository extends JpaRepository<VehiclePlanTrip, Long> {
    List<VehiclePlanTrip> findByServiceDateBetween(LocalDate from, LocalDate to);

    void deleteByServiceDate(LocalDate serviceDate);
}
