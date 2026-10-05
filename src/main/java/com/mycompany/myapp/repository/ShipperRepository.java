package com.mycompany.myapp.repository;

import com.mycompany.myapp.domain.Shipper;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ShipperRepository extends JpaRepository<Shipper, Long> {
    List<Shipper> findAllByOrderByFullNameAsc();

    List<Shipper> findByOffice_CodeOrderByFullNameAsc(String officeCode);
}
