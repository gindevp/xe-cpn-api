package com.mycompany.myapp.repository;

import com.mycompany.myapp.domain.TripOrderAssignment;
import com.mycompany.myapp.domain.enumeration.AssignmentStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the TripOrderAssignment entity.
 */
@Repository
public interface TripOrderAssignmentRepository extends JpaRepository<TripOrderAssignment, Long> {
    default Optional<TripOrderAssignment> findOneWithEagerRelationships(Long id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<TripOrderAssignment> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<TripOrderAssignment> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select tripOrderAssignment from TripOrderAssignment tripOrderAssignment left join fetch tripOrderAssignment.trip left join fetch tripOrderAssignment.order",
        countQuery = "select count(tripOrderAssignment) from TripOrderAssignment tripOrderAssignment"
    )
    Page<TripOrderAssignment> findAllWithToOneRelationships(Pageable pageable);

    @Query(
        "select tripOrderAssignment from TripOrderAssignment tripOrderAssignment left join fetch tripOrderAssignment.trip left join fetch tripOrderAssignment.order"
    )
    List<TripOrderAssignment> findAllWithToOneRelationships();

    @Query(
        "select tripOrderAssignment from TripOrderAssignment tripOrderAssignment left join fetch tripOrderAssignment.trip left join fetch tripOrderAssignment.order where tripOrderAssignment.id =:id"
    )
    Optional<TripOrderAssignment> findOneWithToOneRelationships(@Param("id") Long id);

    @Query(
        """
        select a from TripOrderAssignment a
        left join fetch a.order
        where a.trip.id = :tripId and a.assignmentStatus <> :removed
        """
    )
    List<TripOrderAssignment> findActiveByTripId(@Param("tripId") Long tripId, @Param("removed") AssignmentStatus removed);

    default List<TripOrderAssignment> findActiveByTripId(Long tripId) {
        return findActiveByTripId(tripId, AssignmentStatus.REMOVED);
    }

    Optional<TripOrderAssignment> findFirstByTrip_IdAndOrder_IdAndAssignmentStatusNot(Long tripId, Long orderId, AssignmentStatus excluded);

    long countByTrip_IdAndAssignmentStatus(Long tripId, AssignmentStatus status);

    long countByTrip_IdAndScannedAtIsNotNullAndAssignmentStatusNot(Long tripId, AssignmentStatus excluded);
}
