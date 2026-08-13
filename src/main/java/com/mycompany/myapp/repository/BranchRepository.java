package com.mycompany.myapp.repository;

import com.mycompany.myapp.domain.Branch;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Branch entity.
 */
@Repository
public interface BranchRepository extends JpaRepository<Branch, Long> {
    Optional<Branch> findOneByCode(String code);

    List<Branch> findAllByActiveTrueOrderByNameAsc();
}
