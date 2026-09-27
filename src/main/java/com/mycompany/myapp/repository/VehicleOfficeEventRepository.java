package com.mycompany.myapp.repository;

import com.mycompany.myapp.domain.VehicleOfficeEvent;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VehicleOfficeEventRepository extends JpaRepository<VehicleOfficeEvent, Long> {
    List<VehicleOfficeEvent> findByOffice_IdAndTripKeyIn(Long officeId, Collection<String> tripKeys);

    Optional<VehicleOfficeEvent> findOneByOffice_IdAndEventTypeAndTripKey(
        Long officeId,
        VehicleOfficeEvent.EventType eventType,
        String tripKey
    );
}
