package com.mycompany.myapp.repository;

import com.mycompany.myapp.domain.OfficeNetwork;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OfficeNetworkRepository extends JpaRepository<OfficeNetwork, Long> {
    List<OfficeNetwork> findByOffice_IdOrderByIdAsc(Long officeId);

    boolean existsByOffice_IdAndIpAddress(Long officeId, String ipAddress);
}
