package com.mycompany.myapp.repository;

import com.mycompany.myapp.domain.StaffOfficeAccess;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface StaffOfficeAccessRepository extends JpaRepository<StaffOfficeAccess, Long> {
    List<StaffOfficeAccess> findByStaffProfileId(Long staffProfileId);

    List<StaffOfficeAccess> findByStaffProfileIdIn(List<Long> staffProfileIds);

    boolean existsByStaffProfileIdAndOfficeId(Long staffProfileId, Long officeId);

    @Modifying
    @Query("delete from StaffOfficeAccess a where a.staffProfileId = :staffProfileId")
    void deleteByStaffProfileId(@Param("staffProfileId") Long staffProfileId);
}
