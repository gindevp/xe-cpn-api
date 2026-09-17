package com.mycompany.myapp.repository;

import com.mycompany.myapp.domain.InventoryCheck;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InventoryCheckRepository extends JpaRepository<InventoryCheck, Long> {
    List<InventoryCheck> findByOfficeCodeIgnoreCaseOrderByCheckedAtDesc(String officeCode);

    List<InventoryCheck> findAllByOrderByCheckedAtDesc();
}
