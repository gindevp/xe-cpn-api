package com.mycompany.myapp.repository;

import com.mycompany.myapp.domain.UserSession;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, Long> {
    Optional<UserSession> findOneBySid(String sid);

    List<UserSession> findByUserLoginIgnoreCaseAndChannelAndRevokedAtIsNull(String userLogin, String channel);

    List<UserSession> findByUserLoginIgnoreCaseAndRevokedAtIsNull(String userLogin);

    List<UserSession> findByRevokedAtIsNullAndExpiresAtAfterOrderByLastSeenAtDesc(Instant now);
}
