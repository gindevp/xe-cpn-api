package com.mycompany.myapp.repository;

import com.mycompany.myapp.domain.VehicleOfficeEvent;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface VehicleOfficeEventRepository extends JpaRepository<VehicleOfficeEvent, Long> {
    List<VehicleOfficeEvent> findByOffice_IdAndTripKeyIn(Long officeId, Collection<String> tripKeys);

    Optional<VehicleOfficeEvent> findOneByOffice_IdAndEventTypeAndTripKey(
        Long officeId,
        VehicleOfficeEvent.EventType eventType,
        String tripKey
    );

    /**
     * Mọi lượt báo (trong [outerFrom, outerTo)) của các chuyến có ít nhất một lượt báo trong [from, to) — và tại
     * {@code officeCode} nếu có — để ghép được cả đầu rời lẫn đầu đến của chuyến dù khác ngày / khác VP.
     */
    @Query(
        """
        select e from VehicleOfficeEvent e join fetch e.office o
        where e.eventAt >= :outerFrom and e.eventAt < :outerTo
          and e.tripKey in (
            select e2.tripKey from VehicleOfficeEvent e2
            where e2.eventAt >= :from and e2.eventAt < :to
              and (:officeCode is null or e2.office.code = :officeCode)
          )
        order by e.eventAt asc
        """
    )
    List<VehicleOfficeEvent> findForReport(
        @Param("from") Instant from,
        @Param("to") Instant to,
        @Param("outerFrom") Instant outerFrom,
        @Param("outerTo") Instant outerTo,
        @Param("officeCode") String officeCode
    );

    /** [login, displayName]; {@code logins} đã lowercase. */
    @Query("select s.userLogin, s.displayName from StaffProfile s where lower(s.userLogin) in :logins")
    List<Object[]> findStaffNames(@Param("logins") Collection<String> logins);
}
