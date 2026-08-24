package com.mycompany.myapp.repository;

import com.mycompany.myapp.domain.RoleGroup;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RoleGroupRepository extends JpaRepository<RoleGroup, Long> {
    @Query("select g from RoleGroup g where lower(g.code) = lower(:code)")
    Optional<RoleGroup> findOneByCodeIgnoreCase(@Param("code") String code);

    @Query("select g from RoleGroup g order by g.builtin desc, g.code asc")
    List<RoleGroup> findAllOrdered();

    @Query("select count(p) from StaffProfile p where p.roleGroup.id = :groupId")
    long countStaffUsingGroup(@Param("groupId") Long groupId);
}
