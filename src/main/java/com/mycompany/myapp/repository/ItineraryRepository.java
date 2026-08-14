package com.mycompany.myapp.repository;

import com.mycompany.myapp.domain.Itinerary;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Itinerary entity.
 */
@Repository
public interface ItineraryRepository extends JpaRepository<Itinerary, Long> {
    Optional<Itinerary> findOneByCode(String code);

    Optional<Itinerary> findFirstByNameIgnoreCase(String name);

    @Query(
        "select i from Itinerary i left join fetch i.branch where (:branchId is null or i.branch.id = :branchId) and (:activeOnly = false or i.active = true) order by i.displayOrder asc, i.priority desc, i.name asc"
    )
    List<Itinerary> findFiltered(@Param("branchId") Long branchId, @Param("activeOnly") boolean activeOnly);

    @Query("select i from Itinerary i left join fetch i.branch where i.id = :id")
    Optional<Itinerary> findOneWithBranch(@Param("id") Long id);
}
