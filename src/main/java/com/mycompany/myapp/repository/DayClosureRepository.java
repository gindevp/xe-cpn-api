package com.mycompany.myapp.repository;

import com.mycompany.myapp.domain.DayClosure;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the DayClosure entity.
 */
@Repository
public interface DayClosureRepository extends JpaRepository<DayClosure, Long>, JpaSpecificationExecutor<DayClosure> {
    default Optional<DayClosure> findOneWithEagerRelationships(Long id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<DayClosure> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<DayClosure> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select dayClosure from DayClosure dayClosure left join fetch dayClosure.office",
        countQuery = "select count(dayClosure) from DayClosure dayClosure"
    )
    Page<DayClosure> findAllWithToOneRelationships(Pageable pageable);

    @Query("select dayClosure from DayClosure dayClosure left join fetch dayClosure.office")
    List<DayClosure> findAllWithToOneRelationships();

    @Query("select dayClosure from DayClosure dayClosure left join fetch dayClosure.office where dayClosure.id =:id")
    Optional<DayClosure> findOneWithToOneRelationships(@Param("id") Long id);

    Optional<DayClosure> findFirstByOffice_IdAndBusinessDateOrderByIdDesc(Long officeId, java.time.LocalDate businessDate);
}
