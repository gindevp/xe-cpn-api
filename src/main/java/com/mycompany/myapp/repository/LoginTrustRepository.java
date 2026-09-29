package com.mycompany.myapp.repository;

import com.mycompany.myapp.domain.LoginTrust;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LoginTrustRepository extends JpaRepository<LoginTrust, Long> {
    Optional<LoginTrust> findOneByUserLoginIgnoreCaseAndKindAndTrustValue(String userLogin, String kind, String trustValue);

    List<LoginTrust> findByStatusOrderByRequestedAtDesc(String status);

    List<LoginTrust> findByStatusInOrderByRequestedAtDesc(List<String> statuses);
}
