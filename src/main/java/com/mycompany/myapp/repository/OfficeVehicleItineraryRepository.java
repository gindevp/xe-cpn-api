package com.mycompany.myapp.repository;

import com.mycompany.myapp.domain.OfficeVehicleItinerary;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface OfficeVehicleItineraryRepository extends JpaRepository<OfficeVehicleItinerary, Long> {
    @Query("select o.itineraryCode from OfficeVehicleItinerary o where o.officeId = :officeId")
    List<String> findCodesByOfficeId(@Param("officeId") Long officeId);

    @Modifying
    @Query("delete from OfficeVehicleItinerary o where o.officeId = :officeId")
    void deleteByOfficeId(@Param("officeId") Long officeId);
}
