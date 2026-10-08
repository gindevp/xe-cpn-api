package com.mycompany.myapp.repository;

import com.mycompany.myapp.domain.OfficeQrScreen;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OfficeQrScreenRepository extends JpaRepository<OfficeQrScreen, Long> {
    Optional<OfficeQrScreen> findByDisplayKey(String displayKey);

    Optional<OfficeQrScreen> findByOfficeCode(String officeCode);

    Optional<OfficeQrScreen> findByTokenHash(String tokenHash);
}
