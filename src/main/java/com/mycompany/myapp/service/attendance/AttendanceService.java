package com.mycompany.myapp.service.attendance;

import com.mycompany.myapp.domain.AttendanceRecord;
import com.mycompany.myapp.domain.Office;
import com.mycompany.myapp.domain.OfficeNetwork;
import com.mycompany.myapp.domain.StaffProfile;
import com.mycompany.myapp.domain.enumeration.RoleCode;
import com.mycompany.myapp.repository.AttendanceRecordRepository;
import com.mycompany.myapp.repository.OfficeNetworkRepository;
import com.mycompany.myapp.repository.OfficeRepository;
import com.mycompany.myapp.security.ClientIpResolver;
import com.mycompany.myapp.security.ScreenKey;
import com.mycompany.myapp.security.SecurityUtils;
import com.mycompany.myapp.security.StaffAccessService;
import com.mycompany.myapp.service.dto.attendance.AttendanceAdminRecordDTO;
import com.mycompany.myapp.service.dto.attendance.AttendanceDtos;
import com.mycompany.myapp.service.dto.attendance.AttendanceItemDTO;
import com.mycompany.myapp.web.rest.errors.BadRequestAlertException;
import java.net.InetAddress;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Chấm công tại VP gốc của nhân viên; chỉ hợp lệ khi IP công cộng của request thuộc danh sách wifi của VP. */
@Service
@Transactional
public class AttendanceService {

    static final String ENTITY = "attendance";
    private static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");
    /** ~1.5 MB ảnh gốc sau base64 — app đã nén ảnh nhỏ hơn nhiều. */
    static final int MAX_PHOTO_CHARS = 2_000_000;
    static final int MAX_REPORT_DAYS = 92;

    private final AttendanceRecordRepository attendanceRecordRepository;
    private final OfficeNetworkRepository officeNetworkRepository;
    private final OfficeRepository officeRepository;
    private final StaffAccessService staffAccessService;

    public AttendanceService(
        AttendanceRecordRepository attendanceRecordRepository,
        OfficeNetworkRepository officeNetworkRepository,
        OfficeRepository officeRepository,
        StaffAccessService staffAccessService
    ) {
        this.attendanceRecordRepository = attendanceRecordRepository;
        this.officeNetworkRepository = officeNetworkRepository;
        this.officeRepository = officeRepository;
        this.staffAccessService = staffAccessService;
    }

    @Transactional(readOnly = true)
    public AttendanceDtos.Today today() {
        Office office = homeOffice(requireStaff());
        String login = SecurityUtils.getCurrentUserLogin().orElseThrow();
        LocalDate d = LocalDate.now(VN);
        List<AttendanceItemDTO> items = attendanceRecordRepository.findItems(
            login,
            d.atStartOfDay(VN).toInstant(),
            d.plusDays(1).atStartOfDay(VN).toInstant()
        );
        boolean wifi = !officeNetworkRepository.findByOffice_IdOrderByIdAsc(office.getId()).isEmpty();
        return new AttendanceDtos.Today(office.getCode(), office.getName(), wifi, items);
    }

    public AttendanceItemDTO checkIn(String photo, String clientIp) {
        StaffProfile profile = requireStaff();
        Office office = homeOffice(profile);
        if (photo == null || photo.isBlank()) {
            throw new BadRequestAlertException("Chưa chụp ảnh khuôn mặt", ENTITY, "photoRequired");
        }
        if (photo.length() > MAX_PHOTO_CHARS) {
            throw new BadRequestAlertException("Ảnh quá lớn", ENTITY, "photoTooLarge");
        }
        List<OfficeNetwork> networks = officeNetworkRepository.findByOffice_IdOrderByIdAsc(office.getId());
        if (networks.isEmpty()) {
            throw new BadRequestAlertException("Văn phòng chưa cấu hình wifi chấm công", ENTITY, "wifiNotConfigured");
        }
        if (!ipAllowed(clientIp, networks)) {
            throw new BadRequestAlertException(
                "Kết nối Wi-fi ở văn phòng để chấm công (IP hiện tại: " + (clientIp == null ? "không rõ" : clientIp) + ")",
                ENTITY,
                "wifiMismatch"
            );
        }
        AttendanceRecord r = new AttendanceRecord();
        r.setUserLogin(profile.getUserLogin());
        r.setOffice(office);
        r.setCheckedAt(Instant.now().truncatedTo(ChronoUnit.SECONDS));
        r.setClientIp(clientIp);
        r.setPhoto(photo.startsWith("data:") ? photo : "data:image/jpeg;base64," + photo);
        r = attendanceRecordRepository.save(r);
        return new AttendanceItemDTO(r.getId(), r.getCheckedAt(), office.getCode(), office.getName());
    }

    /** Bảng công: NV (lọc VP / cá nhân / toàn hệ thống) + các lượt chấm trong [from, to] theo giờ VN. */
    @Transactional(readOnly = true)
    public AttendanceDtos.Report report(LocalDate from, LocalDate to, String officeCode, String login) {
        staffAccessService.requireScreenRead(ScreenKey.CHAM_CONG);
        if (from == null || to == null || to.isBefore(from)) {
            throw new BadRequestAlertException("Khoảng ngày không hợp lệ", ENTITY, "rangeInvalid");
        }
        if (ChronoUnit.DAYS.between(from, to) > MAX_REPORT_DAYS) {
            throw new BadRequestAlertException("Chỉ xem tối đa " + MAX_REPORT_DAYS + " ngày mỗi lần", ENTITY, "rangeTooLong");
        }
        String office = blankToNull(officeCode);
        if (!staffAccessService.isSystemAdmin()) {
            office = staffAccessService.scopedOfficeCode().orElse(office);
        }
        String user = blankToNull(login);
        user = user == null ? null : user.toLowerCase(Locale.ROOT);
        List<AttendanceDtos.ReportStaff> staff = attendanceRecordRepository
            .findReportStaff(office, user)
            .stream()
            .map(s ->
                new AttendanceDtos.ReportStaff(
                    s.getUserLogin(),
                    s.getStaffCode(),
                    s.getDisplayName(),
                    s.getOffice() == null ? null : s.getOffice().getCode(),
                    s.getOffice() == null ? null : s.getOffice().getName(),
                    !Boolean.FALSE.equals(s.getActive())
                )
            )
            .toList();
        List<AttendanceAdminRecordDTO> records = attendanceRecordRepository.findAdminItems(
            from.atStartOfDay(VN).toInstant(),
            to.plusDays(1).atStartOfDay(VN).toInstant(),
            office,
            user
        );
        return new AttendanceDtos.Report(staff, records);
    }

    @Transactional(readOnly = true)
    public AttendanceDtos.Photo photo(Long id) {
        staffAccessService.requireScreenRead(ScreenKey.CHAM_CONG);
        String photo = attendanceRecordRepository
            .findPhotoById(id)
            .orElseThrow(() -> new BadRequestAlertException("Không tìm thấy lượt chấm công", ENTITY, "recordNotFound"));
        return new AttendanceDtos.Photo(id, photo);
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    @Transactional(readOnly = true)
    public List<AttendanceDtos.OfficeNetworkItem> listNetworks(Long officeId) {
        requireOffice(officeId);
        return officeNetworkRepository.findByOffice_IdOrderByIdAsc(officeId).stream().map(AttendanceService::toItem).toList();
    }

    public AttendanceDtos.OfficeNetworkItem addNetwork(Long officeId, AttendanceDtos.OfficeNetworkRequest body) {
        Office office = requireOffice(officeId);
        String ip = validateIp(body == null ? null : body.ipAddress());
        if (officeNetworkRepository.existsByOffice_IdAndIpAddress(officeId, ip)) {
            throw new BadRequestAlertException("IP " + ip + " đã có trong danh sách", ENTITY, "ipDuplicate");
        }
        OfficeNetwork n = new OfficeNetwork();
        n.setOffice(office);
        n.setIpAddress(ip);
        String label = body.label() == null ? null : body.label().trim();
        n.setLabel(label == null || label.isEmpty() ? null : label.length() > 100 ? label.substring(0, 100) : label);
        n.setCreatedBy(SecurityUtils.getCurrentUserLogin().orElse(null));
        n.setCreatedAt(Instant.now());
        return toItem(officeNetworkRepository.save(n));
    }

    public void deleteNetwork(Long officeId, Long networkId) {
        OfficeNetwork n = officeNetworkRepository
            .findById(networkId)
            .filter(x -> x.getOffice().getId().equals(officeId))
            .orElseThrow(() -> new BadRequestAlertException("Không tìm thấy IP", ENTITY, "networkNotFound"));
        officeNetworkRepository.delete(n);
    }

    static boolean ipAllowed(String clientIp, List<OfficeNetwork> networks) {
        String ip = ClientIpResolver.normalize(clientIp);
        if (ip == null) {
            return false;
        }
        return networks.stream().anyMatch(n -> ip.equals(ClientIpResolver.normalize(n.getIpAddress())));
    }

    static String validateIp(String raw) {
        String ip = ClientIpResolver.normalize(raw);
        boolean ok =
            ip != null &&
            (ip.matches("^(25[0-5]|2[0-4]\\d|1?\\d?\\d)(\\.(25[0-5]|2[0-4]\\d|1?\\d?\\d)){3}$") ||
                (ip.contains(":") && ip.matches("^[0-9a-f:]+$")));
        if (ok && ip.contains(":")) {
            try {
                InetAddress.getByName(ip);
            } catch (Exception e) {
                ok = false;
            }
        }
        if (!ok) {
            throw new BadRequestAlertException("IP không hợp lệ", ENTITY, "ipInvalid");
        }
        if (ClientIpResolver.isInternal(ip)) {
            throw new BadRequestAlertException(
                "IP " +
                ip +
                " là IP nội bộ (IP wifi / bộ định tuyến) — nhập IP công cộng của văn phòng, bấm \"Lấy IP máy này\" khi đang dùng mạng văn phòng",
                ENTITY,
                "ipPrivate"
            );
        }
        return ip;
    }

    private StaffProfile requireStaff() {
        StaffProfile p = staffAccessService
            .current()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Tài khoản chưa có hồ sơ nhân viên"));
        if (Boolean.FALSE.equals(p.getActive()) || p.getRoleCode() == RoleCode.KH) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tài khoản không được chấm công");
        }
        return p;
    }

    private static Office homeOffice(StaffProfile p) {
        if (p.getOffice() == null) {
            throw new BadRequestAlertException("Tài khoản chưa gắn văn phòng", ENTITY, "officeMissing");
        }
        return p.getOffice();
    }

    private Office requireOffice(Long officeId) {
        return officeRepository
            .findById(officeId)
            .orElseThrow(() -> new BadRequestAlertException("Không tìm thấy văn phòng", ENTITY, "officeNotFound"));
    }

    private static AttendanceDtos.OfficeNetworkItem toItem(OfficeNetwork n) {
        return new AttendanceDtos.OfficeNetworkItem(n.getId(), n.getIpAddress(), n.getLabel(), n.getCreatedBy(), n.getCreatedAt());
    }
}
