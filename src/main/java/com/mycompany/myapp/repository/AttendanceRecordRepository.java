package com.mycompany.myapp.repository;

import com.mycompany.myapp.domain.AttendanceRecord;
import com.mycompany.myapp.domain.StaffProfile;
import com.mycompany.myapp.service.dto.attendance.AttendanceAdminRecordDTO;
import com.mycompany.myapp.service.dto.attendance.AttendanceItemDTO;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
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

    /** Báo cáo quản trị — không tải cột ảnh. {@code login} đã lowercase. */
    @Query(
        """
        select new com.mycompany.myapp.service.dto.attendance.AttendanceAdminRecordDTO(a.id, a.userLogin, o.code, o.name, a.checkedAt, a.clientIp)
        from AttendanceRecord a join a.office o
        where a.checkedAt >= :from and a.checkedAt < :to
          and (:officeCode is null or o.code = :officeCode)
          and (:login is null or lower(a.userLogin) = :login)
        order by a.checkedAt asc
        """
    )
    List<AttendanceAdminRecordDTO> findAdminItems(
        @Param("from") Instant from,
        @Param("to") Instant to,
        @Param("officeCode") String officeCode,
        @Param("login") String login
    );

    /** Nhân viên cần có trong bảng công (kể cả chưa chấm lượt nào). {@code login} đã lowercase. */
    @Query(
        """
        select s from StaffProfile s left join fetch s.office o
        where (s.roleCode is null or s.roleCode <> com.mycompany.myapp.domain.enumeration.RoleCode.KH)
          and (:officeCode is null or o.code = :officeCode)
          and (:login is null or lower(s.userLogin) = :login)
        order by o.code asc, s.userLogin asc
        """
    )
    List<StaffProfile> findReportStaff(@Param("officeCode") String officeCode, @Param("login") String login);

    @Query("select a.photo from AttendanceRecord a where a.id = :id")
    Optional<String> findPhotoById(@Param("id") Long id);
}
