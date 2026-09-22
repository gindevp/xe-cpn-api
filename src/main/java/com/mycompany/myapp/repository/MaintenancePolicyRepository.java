package com.mycompany.myapp.repository;

import com.mycompany.myapp.domain.MaintenancePolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MaintenancePolicyRepository extends JpaRepository<MaintenancePolicy, Long> {}
