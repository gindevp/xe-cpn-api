package com.mycompany.myapp.repository;

import com.mycompany.myapp.domain.StaffProfile;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the StaffProfile entity.
 */
@Repository
public interface StaffProfileRepository extends JpaRepository<StaffProfile, Long> {
    default Optional<StaffProfile> findOneWithEagerRelationships(Long id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<StaffProfile> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<StaffProfile> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select staffProfile from StaffProfile staffProfile left join fetch staffProfile.office",
        countQuery = "select count(staffProfile) from StaffProfile staffProfile"
    )
    Page<StaffProfile> findAllWithToOneRelationships(Pageable pageable);

    @Query("select staffProfile from StaffProfile staffProfile left join fetch staffProfile.office")
    List<StaffProfile> findAllWithToOneRelationships();

    @Query("select staffProfile from StaffProfile staffProfile left join fetch staffProfile.office where staffProfile.id =:id")
    Optional<StaffProfile> findOneWithToOneRelationships(@Param("id") Long id);

    @Query(
        "select staffProfile from StaffProfile staffProfile left join fetch staffProfile.office where lower(staffProfile.userLogin) = lower(:userLogin)"
    )
    Optional<StaffProfile> findOneByUserLoginIgnoreCase(@Param("userLogin") String userLogin);
}
