package com.mycompany.myapp.service.attendance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mycompany.myapp.domain.AttendanceRecord;
import com.mycompany.myapp.domain.Office;
import com.mycompany.myapp.domain.OfficeNetwork;
import com.mycompany.myapp.domain.StaffProfile;
import com.mycompany.myapp.domain.enumeration.RoleCode;
import com.mycompany.myapp.repository.AttendanceRecordRepository;
import com.mycompany.myapp.repository.OfficeNetworkRepository;
import com.mycompany.myapp.repository.OfficeRepository;
import com.mycompany.myapp.security.StaffAccessService;
import com.mycompany.myapp.service.dto.attendance.AttendanceItemDTO;
import com.mycompany.myapp.web.rest.errors.BadRequestAlertException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class AttendanceServiceTest {

    @Mock
    private AttendanceRecordRepository attendanceRecordRepository;

    @Mock
    private OfficeNetworkRepository officeNetworkRepository;

    @Mock
    private OfficeRepository officeRepository;

    @Mock
    private StaffAccessService staffAccessService;

    private AttendanceService service;
    private Office office;
    private StaffProfile profile;

    @BeforeEach
    void setUp() {
        service = new AttendanceService(attendanceRecordRepository, officeNetworkRepository, officeRepository, staffAccessService);
        office = new Office();
        office.setId(7L);
        office.setCode("GA");
        office.setName("Ga Hà Nội");
        profile = new StaffProfile();
        profile.setUserLogin("nv01");
        profile.setOffice(office);
        profile.setActive(true);
        profile.setRoleCode(RoleCode.Q);
    }

    private static OfficeNetwork net(String ip) {
        OfficeNetwork n = new OfficeNetwork();
        n.setIpAddress(ip);
        return n;
    }

    @Test
    void checkInOnOfficeWifiSaves() {
        when(staffAccessService.current()).thenReturn(Optional.of(profile));
        when(officeNetworkRepository.findByOffice_IdOrderByIdAsc(7L)).thenReturn(List.of(net("113.160.1.2")));
        when(attendanceRecordRepository.save(any(AttendanceRecord.class))).thenAnswer(inv -> {
            AttendanceRecord r = inv.getArgument(0);
            r.setId(1L);
            return r;
        });

        AttendanceItemDTO item = service.checkIn("abc", "113.160.1.2");

        assertThat(item.officeCode()).isEqualTo("GA");
        assertThat(item.checkedAt()).isNotNull();
    }

    @Test
    void checkInFromOtherIpRejected() {
        when(staffAccessService.current()).thenReturn(Optional.of(profile));
        when(officeNetworkRepository.findByOffice_IdOrderByIdAsc(7L)).thenReturn(List.of(net("113.160.1.2")));

        assertThatThrownBy(() -> service.checkIn("abc", "27.72.9.9"))
            .isInstanceOf(BadRequestAlertException.class)
            .hasMessageContaining("Kết nối Wi-fi");
        verify(attendanceRecordRepository, never()).save(any());
    }

    @Test
    void checkInWithoutConfiguredWifiRejected() {
        when(staffAccessService.current()).thenReturn(Optional.of(profile));
        when(officeNetworkRepository.findByOffice_IdOrderByIdAsc(7L)).thenReturn(List.of());

        assertThatThrownBy(() -> service.checkIn("abc", "113.160.1.2"))
            .isInstanceOf(BadRequestAlertException.class)
            .hasMessageContaining("chưa cấu hình wifi");
    }

    @Test
    void checkInWithoutPhotoRejected() {
        when(staffAccessService.current()).thenReturn(Optional.of(profile));

        assertThatThrownBy(() -> service.checkIn(" ", "113.160.1.2")).isInstanceOf(BadRequestAlertException.class);
    }

    @Test
    void inactiveOrCustomerCannotCheckIn() {
        profile.setActive(false);
        when(staffAccessService.current()).thenReturn(Optional.of(profile));
        assertThatThrownBy(() -> service.checkIn("abc", "113.160.1.2")).isInstanceOf(ResponseStatusException.class);

        profile.setActive(true);
        profile.setRoleCode(RoleCode.KH);
        assertThatThrownBy(() -> service.checkIn("abc", "113.160.1.2")).isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void ipMatchIgnoresMappedIpv6Prefix() {
        assertThat(AttendanceService.ipAllowed("::ffff:113.160.1.2", List.of(net("113.160.1.2")))).isTrue();
        assertThat(AttendanceService.ipAllowed(null, List.of(net("113.160.1.2")))).isFalse();
    }

    @Test
    void validateIpAcceptsV4AndV6() {
        assertThat(AttendanceService.validateIp(" 113.160.1.2 ")).isEqualTo("113.160.1.2");
        assertThat(AttendanceService.validateIp("2001:DB8::1")).isEqualTo("2001:db8::1");
        assertThatThrownBy(() -> AttendanceService.validateIp("300.1.1.1")).isInstanceOf(BadRequestAlertException.class);
        assertThatThrownBy(() -> AttendanceService.validateIp("abc")).isInstanceOf(BadRequestAlertException.class);
    }
}
