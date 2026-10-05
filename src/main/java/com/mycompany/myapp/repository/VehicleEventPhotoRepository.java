package com.mycompany.myapp.repository;

import com.mycompany.myapp.domain.VehicleEventPhoto;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface VehicleEventPhotoRepository extends JpaRepository<VehicleEventPhoto, Long> {
    Optional<VehicleEventPhoto> findOneByEventId(Long eventId);

    /** Lượt báo nào trong lô có ảnh — không đọc cột ảnh. */
    @Query("select p.eventId from VehicleEventPhoto p where p.eventId in :eventIds")
    List<Long> findEventIdsIn(@Param("eventIds") Collection<Long> eventIds);
}
