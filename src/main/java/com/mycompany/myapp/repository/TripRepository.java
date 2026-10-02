package com.mycompany.myapp.repository;

import com.mycompany.myapp.domain.Trip;
import com.mycompany.myapp.domain.enumeration.TripStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Trip entity.
 */
@Repository
public interface TripRepository extends JpaRepository<Trip, Long>, JpaSpecificationExecutor<Trip> {
    default Optional<Trip> findOneWithEagerRelationships(Long id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<Trip> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<Trip> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select trip from Trip trip left join fetch trip.office left join fetch trip.route left join fetch trip.vehicle left join fetch trip.driver",
        countQuery = "select count(trip) from Trip trip"
    )
    Page<Trip> findAllWithToOneRelationships(Pageable pageable);

    @Query(
        "select trip from Trip trip left join fetch trip.office left join fetch trip.route left join fetch trip.vehicle left join fetch trip.driver"
    )
    List<Trip> findAllWithToOneRelationships();

    @Query(
        "select trip from Trip trip left join fetch trip.office left join fetch trip.route left join fetch trip.vehicle left join fetch trip.driver where trip.id =:id"
    )
    Optional<Trip> findOneWithToOneRelationships(@Param("id") Long id);

    @Query(
        """
        select trip from Trip trip
        left join fetch trip.office
        left join fetch trip.route
        left join fetch trip.vehicle
        left join fetch trip.driver
        where trip.tripCode = :tripCode
        """
    )
    Optional<Trip> findOneByTripCode(@Param("tripCode") String tripCode);

    @Query("select count(trip) from Trip trip where trip.tripCode like concat(:prefix, '%')")
    long countByTripCodePrefix(@Param("prefix") String prefix);

    /** Chuyến chưa huỷ trong khoảng giờ xuất bến, có liên quan tới VP (xuất phát hoặc tuyến đi/đến VP). */
    @Query(
        """
        select trip from Trip trip
        left join fetch trip.office
        left join fetch trip.route r
        left join fetch r.fromOffice
        left join fetch r.toOffice
        left join fetch trip.vehicle
        left join fetch trip.driver
        where trip.departAt >= :from and trip.departAt < :to
          and trip.status <> com.mycompany.myapp.domain.enumeration.TripStatus.CANCELLED
          and (trip.office.id = :officeId or r.fromOffice.id = :officeId or r.toOffice.id = :officeId)
        order by trip.departAt asc
        """
    )
    List<Trip> findForOfficeBoard(
        @Param("officeId") Long officeId,
        @Param("from") java.time.Instant from,
        @Param("to") java.time.Instant to
    );

    boolean existsByVehicle_Id(Long vehicleId);

    boolean existsByDriver_Id(Long driverId);

    Optional<Trip> findFirstByVehicle_PlateNumberAndStatusInOrderByIdDesc(String plateNumber, Collection<TripStatus> statuses);
}
