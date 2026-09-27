package com.mycompany.myapp.repository;

import com.mycompany.myapp.domain.AttendanceRecord;
import com.mycompany.myapp.service.dto.attendance.AttendanceItemDTO;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, Long> {
    /** Không tải cột ảnh. */
    @Query(
        """
        select new com.mycompany.myapp.service.dto.attendance.AttendanceItemDTO(a.id, a.checkedAt, o.code, o.name)
        from AttendanceRecord a join a.office o
        where lower(a.userLogin) = lower(:login) and a.checkedAt >= :from and a.checkedAt < :to
        order by a.checkedAt asc
        """
    )
    List<AttendanceItemDTO> findItems(@Param("login") String login, @Param("from") Instant from, @Param("to") Instant to);
}
