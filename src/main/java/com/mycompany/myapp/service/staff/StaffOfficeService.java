package com.mycompany.myapp.service.staff;

import com.mycompany.myapp.domain.Office;
import com.mycompany.myapp.domain.StaffOfficeAccess;
import com.mycompany.myapp.domain.StaffProfile;
import com.mycompany.myapp.repository.OfficeRepository;
import com.mycompany.myapp.repository.StaffOfficeAccessRepository;
import com.mycompany.myapp.repository.StaffProfileRepository;
import com.mycompany.myapp.security.SecurityUtils;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Nhân viên làm nhiều VP: admin gán danh sách VP được phép, nhân viên tự chuyển VP đang dùng.
 * VP đang dùng chính là {@link StaffProfile#getOffice()} nên mọi màn đang lọc theo VP của hồ sơ
 * tự bó đúng một VP; lưu ở DB nên F5 / mở lại app vẫn giữ VP đã chọn.
 */
@Service
@Transactional
public class StaffOfficeService {

    public record OfficeOption(Long id, String code, String name) {}

    private final StaffOfficeAccessRepository accessRepository;
    private final StaffProfileRepository staffProfileRepository;
    private final OfficeRepository officeRepository;

    public StaffOfficeService(
        StaffOfficeAccessRepository accessRepository,
        StaffProfileRepository staffProfileRepository,
        OfficeRepository officeRepository
    ) {
        this.accessRepository = accessRepository;
        this.staffProfileRepository = staffProfileRepository;
        this.officeRepository = officeRepository;
    }

    /** VP được phép, luôn gồm VP đang dùng. Tài khoản toàn hệ thống trả rỗng (họ chọn VP xem riêng). */
    @Transactional(readOnly = true)
    public List<OfficeOption> allowedOffices(StaffProfile profile) {
        if (profile == null || profile.getId() == null || Boolean.TRUE.equals(profile.getScopeAllOffices())) {
            return List.of();
        }
        Set<Long> ids = new LinkedHashSet<>();
        if (profile.getOffice() != null && profile.getOffice().getId() != null) {
            ids.add(profile.getOffice().getId());
        }
        accessRepository.findByStaffProfileId(profile.getId()).forEach(a -> ids.add(a.getOfficeId()));
        return toOptions(officeRepository.findAllById(ids));
    }

    /** Id VP được phép thêm (ngoài VP đang dùng) theo từng hồ sơ — cho bảng quản trị tài khoản. */
    @Transactional(readOnly = true)
    public Map<Long, List<Long>> extraOfficeIdsByProfile(Collection<Long> profileIds) {
        Map<Long, List<Long>> out = new HashMap<>();
        if (profileIds == null || profileIds.isEmpty()) {
            return out;
        }
        for (StaffOfficeAccess a : accessRepository.findByStaffProfileIdIn(new ArrayList<>(profileIds))) {
            out.computeIfAbsent(a.getStaffProfileId(), k -> new ArrayList<>()).add(a.getOfficeId());
        }
        return out;
    }

    /** Ghi đè danh sách VP được phép. VP đang dùng luôn được phép nên không cần lưu thêm. */
    public void replaceAllowed(StaffProfile profile, Collection<Long> officeIds) {
        if (profile == null || profile.getId() == null || officeIds == null) {
            return;
        }
        accessRepository.deleteByStaffProfileId(profile.getId());
        accessRepository.flush();
        if (Boolean.TRUE.equals(profile.getScopeAllOffices())) {
            return;
        }
        Long homeId = profile.getOffice() != null ? profile.getOffice().getId() : null;
        Set<Long> wanted = new LinkedHashSet<>();
        for (Long id : officeIds) {
            if (id != null && !Objects.equals(id, homeId)) {
                wanted.add(id);
            }
        }
        if (wanted.isEmpty()) {
            return;
        }
        List<Office> found = officeRepository.findAllById(wanted);
        if (found.size() != wanted.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Có văn phòng được phép không tồn tại");
        }
        for (Office o : found) {
            accessRepository.save(new StaffOfficeAccess(profile.getId(), o.getId()));
        }
    }

    /** Nhân viên tự chuyển VP đang dùng — chỉ trong danh sách admin đã gán. */
    public StaffProfile switchActive(Long officeId) {
        if (officeId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chưa chọn văn phòng");
        }
        String login = SecurityUtils.getCurrentUserLogin()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Chưa đăng nhập"));
        StaffProfile profile = staffProfileRepository
            .findOneByUserLoginIgnoreCase(login)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Tài khoản chưa có hồ sơ nhân viên"));
        if (Boolean.TRUE.equals(profile.getScopeAllOffices())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tài khoản toàn hệ thống chọn văn phòng xem ở đầu trang");
        }
        Long currentId = profile.getOffice() != null ? profile.getOffice().getId() : null;
        if (Objects.equals(currentId, officeId)) {
            return profile;
        }
        if (!accessRepository.existsByStaffProfileIdAndOfficeId(profile.getId(), officeId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn chưa được gán văn phòng này");
        }
        Office target = officeRepository
            .findById(officeId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Văn phòng không tồn tại"));
        // Giữ VP cũ trong danh sách để chuyển ngược lại được; VP mới thành VP đang dùng nên bỏ khỏi bảng phụ.
        if (currentId != null && !accessRepository.existsByStaffProfileIdAndOfficeId(profile.getId(), currentId)) {
            accessRepository.save(new StaffOfficeAccess(profile.getId(), currentId));
        }
        accessRepository
            .findByStaffProfileId(profile.getId())
            .stream()
            .filter(a -> Objects.equals(a.getOfficeId(), officeId))
            .forEach(accessRepository::delete);
        profile.setOffice(target);
        return staffProfileRepository.save(profile);
    }

    private static List<OfficeOption> toOptions(List<Office> offices) {
        Collator vi = Collator.getInstance(Locale.forLanguageTag("vi"));
        Map<Long, OfficeOption> byId = new LinkedHashMap<>();
        offices
            .stream()
            .sorted(Comparator.comparing(o -> o.getName() == null ? "" : o.getName(), vi))
            .forEach(o -> byId.put(o.getId(), new OfficeOption(o.getId(), o.getCode(), o.getName())));
        return new ArrayList<>(byId.values());
    }
}
