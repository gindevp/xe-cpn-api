package com.mycompany.myapp.repository;

import com.mycompany.myapp.domain.TrackLookupCounter;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TrackLookupCounterRepository extends JpaRepository<TrackLookupCounter, Long> {
    Optional<TrackLookupCounter> findByDeviceKeyAndDayVn(String deviceKey, LocalDate dayVn);
}
