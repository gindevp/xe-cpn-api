package com.mycompany.myapp.service.vehicle;

import com.mycompany.myapp.domain.Itinerary;
import com.mycompany.myapp.domain.Office;
import com.mycompany.myapp.domain.OfficeVehicleItinerary;
import com.mycompany.myapp.domain.Route;
import com.mycompany.myapp.domain.StaffProfile;
import com.mycompany.myapp.domain.Trip;
import com.mycompany.myapp.domain.VehicleEventPhoto;
import com.mycompany.myapp.domain.VehicleOfficeEvent;
import com.mycompany.myapp.domain.VehicleOfficeEvent.EventType;
import com.mycompany.myapp.domain.VehicleOfficeEvent.Source;
import com.mycompany.myapp.repository.ItineraryRepository;
import com.mycompany.myapp.repository.OfficeRepository;
import com.mycompany.myapp.repository.OfficeVehicleItineraryRepository;
import com.mycompany.myapp.repository.TripRepository;
import com.mycompany.myapp.repository.VehicleEventPhotoRepository;
import com.mycompany.myapp.repository.VehicleOfficeEventRepository;
import com.mycompany.myapp.security.ScreenKey;
import com.mycompany.myapp.security.SecurityUtils;
import com.mycompany.myapp.security.StaffAccessService;
import com.mycompany.myapp.service.OfficeItineraryPoints;
import com.mycompany.myapp.service.dto.trip.AvailableTripDTO;
import com.mycompany.myapp.service.dto.vehicle.VehicleBoardDtos;
import com.mycompany.myapp.service.partner.AvailableTripSearchService;
import com.mycompany.myapp.service.partner.VthkTripSearchClient;
import com.mycompany.myapp.web.rest.errors.BadRequestAlertException;
import java.text.Normalizer;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Danh sách xe đến/rời VP gốc của NV trong ngày + ghi nhận giờ. Không đổi trạng thái chuyến/đơn.
 * Rời VP: chuyến xuất bến hôm nay từ VP. Đến VP: chuyến có điểm đến là VP, xuất bến hôm nay hoặc hôm qua mà chưa báo đến.
 */
@Service
@Transactional
public class VehicleBoardService {

    private static final Logger LOG = LoggerFactory.getLogger(VehicleBoardService.class);
    static final String ENTITY = "vehicleEvent";
    private static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");
    static final int MAX_REPORT_DAYS = 92;
    /** Rời VP trễ hơn giờ đón từ ngần này phút là MUỘN — báo rời phải kèm lý do. */
    static final int LATE_MINUTES = 5;
    /** Phút lệch giờ đón tối đa so với giờ xuất bến (± 12 tiếng). */
    static final int MAX_OFFSET_MINUTES = 720;
    static final int MAX_PHOTO_LENGTH = 4_000_000;

    private final TripRepository tripRepository;
    private final ItineraryRepository itineraryRepository;
    private final VehicleOfficeEventRepository eventRepository;
    private final AvailableTripSearchService availableTripSearchService;
    private final VthkTripSearchClient vthkClient;
    private final StaffAccessService staffAccessService;
    private final OfficeVehicleItineraryRepository officeItineraryRepository;
    private final OfficeRepository officeRepository;
    private final VehicleEventPhotoRepository photoRepository;

    public VehicleBoardService(
        TripRepository tripRepository,
        ItineraryRepository itineraryRepository,
        VehicleOfficeEventRepository eventRepository,
        AvailableTripSearchService availableTripSearchService,
        VthkTripSearchClient vthkClient,
        StaffAccessService staffAccessService,
        OfficeVehicleItineraryRepository officeItineraryRepository,
        OfficeRepository officeRepository,
        VehicleEventPhotoRepository photoRepository
    ) {
        this.officeItineraryRepository = officeItineraryRepository;
        this.officeRepository = officeRepository;
        this.photoRepository = photoRepository;
        this.tripRepository = tripRepository;
        this.itineraryRepository = itineraryRepository;
        this.eventRepository = eventRepository;
        this.availableTripSearchService = availableTripSearchService;
        this.vthkClient = vthkClient;
        this.staffAccessService = staffAccessService;
    }

    record Candidate(
        EventType type,
        Source source,
        String tripKey,
        String tripCode,
        String externalTripId,
        String plate,
        String driver,
        String route,
        Instant departAt
    ) {}

    @Transactional(readOnly = true)
    public VehicleBoardDtos.Board board() {
        Office office = homeOffice();
        LocalDate today = LocalDate.now(VN);
        Instant todayStart = today.atStartOfDay(VN).toInstant();
        Instant yesterdayStart = today.minusDays(1).atStartOfDay(VN).toInstant();
        Instant tomorrowStart = today.plusDays(1).atStartOfDay(VN).toInstant();

        List<Candidate> candidates = new ArrayList<>();
        for (Trip trip : tripRepository.findForOfficeBoard(office.getId(), yesterdayStart, tomorrowStart)) {
            if (departsFrom(trip, office) && !trip.getDepartAt().isBefore(todayStart)) {
                candidates.add(fromTrip(trip, EventType.DEPART));
            }
            if (arrivesAt(trip, office)) {
                candidates.add(fromTrip(trip, EventType.ARRIVE));
            }
        }

        String crmWarning = null;
        Set<String> points = officePoints(office);
        if (!points.isEmpty() && vthkClient.isEnabled()) {
            try {
                appendCrm(candidates, points, configuredCodes(office), today);
            } catch (RuntimeException e) {
                LOG.warn("Vehicle board CRM lookup failed for office {}: {}", office.getCode(), e.getMessage());
                crmWarning = "Không tải được xe Limousine từ CRM";
            }
        }

        Map<String, VehicleOfficeEvent> reported = new HashMap<>();
        Set<String> keys = new HashSet<>();
        candidates.forEach(c -> keys.add(c.tripKey()));
        if (!keys.isEmpty()) {
            for (VehicleOfficeEvent e : eventRepository.findByOffice_IdAndTripKeyIn(office.getId(), keys)) {
                reported.put(e.getEventType() + "|" + e.getTripKey(), e);
            }
        }

        PickupOffsets offsets = pickupOffsets(office);
        List<VehicleBoardDtos.Item> items = new ArrayList<>();
        for (Candidate c : candidates) {
            VehicleOfficeEvent e = reported.get(c.type() + "|" + c.tripKey());
            boolean yesterday = c.departAt() != null && c.departAt().isBefore(todayStart);
            if (yesterday && e != null) {
                continue;
            }
            items.add(toItem(c, e, pickupAt(c.departAt(), offsets.of(null, c.route()))));
        }
        items.sort(Comparator.comparing(VehicleBoardDtos.Item::plannedDepartAt, Comparator.nullsLast(Comparator.naturalOrder())));
        return new VehicleBoardDtos.Board(office.getCode(), office.getName(), items, crmWarning);
    }

    /**
     * Lộ trình VP gốc của NV báo giờ: theo cấu hình của VP; VP chưa cấu hình thì mọi lộ trình đang hoạt động có điểm đầu
     * hoặc điểm cuối là điểm của VP (theo mã "GA-YB").
     */
    @Transactional(readOnly = true)
    public List<VehicleBoardDtos.ItineraryOption> officeItineraries() {
        Office office = homeOffice();
        if (officePoints(office).isEmpty()) {
            throw new BadRequestAlertException(
                "Văn phòng " + office.getName() + " chưa cấu hình điểm lộ trình",
                ENTITY,
                "itineraryPointMissing"
            );
        }
        return reportItineraries(office);
    }

    private List<VehicleBoardDtos.ItineraryOption> reportItineraries(Office office) {
        Set<String> configured = configuredCodes(office);
        List<VehicleBoardDtos.ItineraryOption> out = new ArrayList<>();
        for (Itinerary it : pointItineraries(office)) {
            if (configured.isEmpty() || configured.contains(it.getCode())) {
                out.add(new VehicleBoardDtos.ItineraryOption(it.getCode(), firstNonBlank(it.getName(), it.getCode())));
            }
        }
        return out;
    }

    /** Lộ trình đang hoạt động có điểm đầu hoặc điểm cuối là điểm của VP, sắp theo tên. */
    private List<Itinerary> pointItineraries(Office office) {
        Set<String> points = officePoints(office);
        List<Itinerary> out = new ArrayList<>();
        if (points.isEmpty()) {
            return out;
        }
        for (Itinerary it : itineraryRepository.findFiltered(null, true)) {
            String[] ends = itineraryEnds(it.getCode());
            if (ends != null && (points.contains(ends[0]) || points.contains(ends[1]))) {
                out.add(it);
            }
        }
        out.sort(Comparator.comparing(it -> firstNonBlank(it.getName(), it.getCode()), Comparator.nullsLast(Comparator.naturalOrder())));
        return out;
    }

    private Set<String> configuredCodes(Office office) {
        return office.getId() == null ? Set.of() : new HashSet<>(officeItineraryRepository.findCodesByOfficeId(office.getId()));
    }

    /** Mã lộ trình → phút lệch giờ đón tại VP (chỉ lộ trình đã tích và có nhập phút lệch). */
    private Map<String, Integer> offsetsByCode(Office office) {
        Map<String, Integer> out = new HashMap<>();
        if (office.getId() == null) {
            return out;
        }
        for (OfficeVehicleItinerary v : officeItineraryRepository.findByOfficeId(office.getId())) {
            if (v.getItineraryCode() != null && v.getOffsetMinutes() != null && v.getOffsetMinutes() != 0) {
                out.put(v.getItineraryCode(), v.getOffsetMinutes());
            }
        }
        return out;
    }

    private PickupOffsets pickupOffsets(Office office) {
        return new PickupOffsets(offsetsByCode(office));
    }

    /** Tra phút lệch theo mã lộ trình, hoặc theo tên tuyến khi không có mã (app cũ / chuyến hệ thống). */
    private final class PickupOffsets {

        private final Map<String, Integer> byCode;
        private Map<String, String> codeByName;

        PickupOffsets(Map<String, Integer> byCode) {
            this.byCode = byCode;
        }

        int of(String itineraryCode, String routeLabel) {
            if (byCode.isEmpty()) {
                return 0;
            }
            String code = trimToNull(itineraryCode);
            if (code == null && routeLabel != null) {
                if (codeByName == null) {
                    codeByName = new HashMap<>();
                    for (Itinerary it : itineraryRepository.findFiltered(null, true)) {
                        if (byCode.containsKey(it.getCode())) {
                            codeByName.put(nameKey(it.getCode()), it.getCode());
                            if (it.getName() != null) {
                                codeByName.put(nameKey(it.getName()), it.getCode());
                            }
                        }
                    }
                }
                code = codeByName.get(nameKey(routeLabel));
            }
            Integer v = code == null ? null : byCode.get(code);
            return v == null ? 0 : v;
        }
    }

    private static String nameKey(String s) {
        String f = foldPoint(s);
        return f == null ? "" : f.replaceAll("\\s+", "");
    }

    static Instant pickupAt(Instant departAt, int offsetMinutes) {
        return departAt == null ? null : departAt.plus(offsetMinutes, ChronoUnit.MINUTES);
    }

    /** Phút lệch giờ rời thực tế so với giờ đón (âm = sớm), làm tròn về 0 theo phút; thiếu một mốc → null. */
    static Long deviationMinutes(Instant pickupAt, Instant actualAt) {
        if (pickupAt == null || actualAt == null) {
            return null;
        }
        return java.time.Duration.between(pickupAt, actualAt).getSeconds() / 60;
    }

    static boolean departLate(Instant pickupAt, Instant actualAt) {
        Long d = deviationMinutes(pickupAt, actualAt);
        return d != null && d >= LATE_MINUTES;
    }

    /** Trang khách tạo đơn: mã VP → lộ trình VP báo giờ (chỉ VP đã cấu hình). */
    @Transactional(readOnly = true)
    public Map<String, List<String>> allOfficeItineraries() {
        Map<String, List<String>> out = new java.util.TreeMap<>();
        for (Object[] row : officeItineraryRepository.findAllOfficeCodePairs()) {
            if (row[0] != null && row[1] != null) {
                out.computeIfAbsent(row[0].toString(), k -> new ArrayList<>()).add(row[1].toString());
            }
        }
        return out;
    }

    /** Danh mục VP: lộ trình VP báo giờ (chọn trong các lộ trình qua điểm của VP). */
    @Transactional(readOnly = true)
    public VehicleBoardDtos.ItineraryConfig itineraryConfig(Long officeId) {
        staffAccessService.requireScreenRead(ScreenKey.MASTER);
        Office office = officeById(officeId);
        Set<String> configured = configuredCodes(office);
        Map<String, Integer> offsets = offsetsByCode(office);
        List<VehicleBoardDtos.ConfigOption> options = pointItineraries(office)
            .stream()
            .map(it ->
                new VehicleBoardDtos.ConfigOption(
                    it.getCode(),
                    firstNonBlank(it.getName(), it.getCode()),
                    configured.contains(it.getCode()),
                    offsets.get(it.getCode())
                )
            )
            .toList();
        return new VehicleBoardDtos.ItineraryConfig(office.getId(), office.getName(), options);
    }

    /** Ghi: screen Master (StaffWriteGuardFilter, prefix /api/offices). Danh sách rỗng = bỏ cấu hình, hiện mọi lộ trình qua điểm VP. */
    public VehicleBoardDtos.ItineraryConfig saveItineraryConfig(Long officeId, VehicleBoardDtos.ItineraryConfigRequest req) {
        Office office = officeById(officeId);
        Set<String> allowed = new HashSet<>();
        pointItineraries(office).forEach(it -> allowed.add(it.getCode()));
        Set<String> codes = new LinkedHashSet<>();
        if (req != null && req.itineraryCodes() != null) {
            for (String raw : req.itineraryCodes()) {
                String code = trimToNull(raw);
                if (code == null) {
                    continue;
                }
                if (!allowed.contains(code)) {
                    throw new BadRequestAlertException(
                        "Lộ trình " + code + " không đi qua điểm của văn phòng",
                        ENTITY,
                        "itineraryNotRelated"
                    );
                }
                codes.add(code);
            }
        }
        Map<String, Integer> offsets = new HashMap<>();
        if (req != null && req.offsets() != null) {
            for (VehicleBoardDtos.ItineraryOffset o : req.offsets()) {
                String code = o == null ? null : trimToNull(o.code());
                if (code == null || o.offsetMinutes() == null || !codes.contains(code)) {
                    continue;
                }
                if (Math.abs(o.offsetMinutes()) > MAX_OFFSET_MINUTES) {
                    throw new BadRequestAlertException(
                        "Phút lệch giờ đón của lộ trình " + code + " phải trong khoảng ±" + MAX_OFFSET_MINUTES,
                        ENTITY,
                        "offsetOutOfRange"
                    );
                }
                offsets.put(code, o.offsetMinutes());
            }
        }
        officeItineraryRepository.deleteByOfficeId(office.getId());
        officeItineraryRepository.flush();
        codes.forEach(c -> officeItineraryRepository.save(new OfficeVehicleItinerary(office.getId(), c, offsets.get(c))));
        return itineraryConfig(officeId);
    }

    private Office officeById(Long officeId) {
        return officeRepository
            .findById(officeId)
            .orElseThrow(() -> new BadRequestAlertException("Không tìm thấy văn phòng", ENTITY, "officeNotFound"));
    }

    /** Mọi xe CRM của lộ trình xuất bến hôm nay (giờ VN), kèm giờ đã báo đến/rời tại VP gốc của NV. */
    @Transactional(readOnly = true)
    public VehicleBoardDtos.DayBoard dayTrips(String itineraryCodeOrName) {
        if (trimToNull(itineraryCodeOrName) == null) {
            throw new BadRequestAlertException("Chưa chọn lộ trình", ENTITY, "itineraryRequired");
        }
        Office office = homeOffice();
        Itinerary itinerary = availableTripSearchService.resolveItinerary(itineraryCodeOrName.trim());
        Set<String> points = officePoints(office);
        String[] ends = itineraryEnds(itinerary.getCode());
        if (!points.isEmpty() && (ends == null || !(points.contains(ends[0]) || points.contains(ends[1])))) {
            throw new BadRequestAlertException("Lộ trình không đi qua văn phòng của bạn", ENTITY, "itineraryNotRelated");
        }
        Set<String> configured = configuredCodes(office);
        if (!configured.isEmpty() && !configured.contains(itinerary.getCode())) {
            throw new BadRequestAlertException("Lộ trình không thuộc danh sách báo giờ của văn phòng", ENTITY, "itineraryNotRelated");
        }
        LocalDate today = LocalDate.now(VN);
        List<AvailableTripDTO> trips = availableTripSearchService.searchWindow(itinerary, today.atStartOfDay(), today.atTime(23, 59, 59));

        Map<String, VehicleOfficeEvent> reported = new HashMap<>();
        Set<String> keys = new HashSet<>();
        trips.forEach(t -> keys.add("C:" + cut(t.getExternalTripId(), 60)));
        if (!keys.isEmpty()) {
            for (VehicleOfficeEvent e : eventRepository.findByOffice_IdAndTripKeyIn(office.getId(), keys)) {
                reported.put(e.getEventType() + "|" + e.getTripKey(), e);
            }
        }

        int offset = pickupOffsets(office).of(itinerary.getCode(), null);
        Set<String> seen = new HashSet<>();
        List<VehicleBoardDtos.DayItem> items = new ArrayList<>();
        for (AvailableTripDTO t : trips) {
            String key = "C:" + cut(t.getExternalTripId(), 60);
            if (!seen.add(key)) {
                continue;
            }
            VehicleOfficeEvent arrive = reported.get(EventType.ARRIVE + "|" + key);
            VehicleOfficeEvent depart = reported.get(EventType.DEPART + "|" + key);
            items.add(
                new VehicleBoardDtos.DayItem(
                    cut(t.getExternalTripId(), 60),
                    firstNonBlank(t.getVehiclePlate(), t.getAssignVehiclePlate()),
                    firstNonBlank(t.getDriverName(), t.getAssignDriverName()),
                    firstNonBlank(itinerary.getName(), t.getRouteLabel()),
                    t.getDepartAt(),
                    arrive != null ? arrive.getEventAt() : null,
                    arrive != null ? arrive.getReportedBy() : null,
                    depart != null ? depart.getEventAt() : null,
                    depart != null ? depart.getReportedBy() : null,
                    pickupAt(t.getDepartAt(), offset)
                )
            );
        }
        items.sort(Comparator.comparing(VehicleBoardDtos.DayItem::plannedDepartAt, Comparator.nullsLast(Comparator.naturalOrder())));
        return new VehicleBoardDtos.DayBoard(office.getCode(), office.getName(), items);
    }

    public VehicleBoardDtos.Item report(VehicleBoardDtos.ReportRequest req) {
        if (req == null) {
            throw new BadRequestAlertException("Thiếu dữ liệu", ENTITY, "bodyRequired");
        }
        Office office = homeOffice();
        EventType type = parse(EventType.class, req.eventType(), "eventTypeInvalid");
        Source source = parse(Source.class, req.source(), "sourceInvalid");

        Candidate c;
        if (source == Source.TRIP) {
            String code = trimToNull(req.tripCode());
            if (code == null) {
                throw new BadRequestAlertException("Thiếu mã chuyến", ENTITY, "tripCodeRequired");
            }
            Trip trip = tripRepository
                .findOneByTripCode(code)
                .orElseThrow(() -> new BadRequestAlertException("Không tìm thấy chuyến " + code, ENTITY, "tripNotFound"));
            boolean related = type == EventType.DEPART ? departsFrom(trip, office) : arrivesAt(trip, office);
            if (!related) {
                throw new BadRequestAlertException(
                    type == EventType.DEPART ? "Chuyến không xuất phát từ văn phòng của bạn" : "Chuyến không đến văn phòng của bạn",
                    ENTITY,
                    "tripNotRelated"
                );
            }
            c = fromTrip(trip, type);
        } else {
            String ext = trimToNull(req.externalTripId());
            if (ext == null) {
                throw new BadRequestAlertException("Thiếu mã chuyến CRM", ENTITY, "externalTripIdRequired");
            }
            c = new Candidate(
                type,
                Source.CRM,
                "C:" + cut(ext, 60),
                null,
                cut(ext, 60),
                cut(trimToNull(req.vehiclePlate()), 30),
                cut(trimToNull(req.driverName()), 120),
                cut(trimToNull(req.routeLabel()), 120),
                req.plannedDepartAt()
            );
        }

        Instant pickup = pickupAt(c.departAt(), pickupOffsets(office).of(req.itineraryCode(), c.route()));
        var existing = eventRepository.findOneByOffice_IdAndEventTypeAndTripKey(office.getId(), type, c.tripKey());
        if (existing.isPresent()) {
            VehicleOfficeEvent old = existing.get();
            return toItem(c, old, old.getPickupAt() != null ? old.getPickupAt() : pickup);
        }
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        String reason = cut(trimToNull(req.reason()), 500);
        String photo = type == EventType.DEPART ? trimToNull(req.photo()) : null;
        if (type == EventType.DEPART) {
            eventRepository
                .findOneByOffice_IdAndEventTypeAndTripKey(office.getId(), EventType.ARRIVE, c.tripKey())
                .orElseThrow(() ->
                    new BadRequestAlertException("Chưa báo xe đến VP — báo xe đến trước rồi mới báo xe rời", ENTITY, "arriveFirst")
                );
            if (reason == null && departLate(pickup, now)) {
                throw new BadRequestAlertException(
                    "Xe rời muộn " + deviationMinutes(pickup, now) + " phút so với giờ đón — nhập lý do trước khi báo xe rời",
                    ENTITY,
                    "lateReasonRequired"
                );
            }
            if (photo == null) {
                throw new BadRequestAlertException(
                    "Cần chụp ảnh xe trước khi báo xe rời — cập nhật app nếu chưa thấy bước chụp ảnh",
                    ENTITY,
                    "photoRequired"
                );
            }
            if (!photo.startsWith("data:image/") || !photo.contains(";base64,")) {
                throw new BadRequestAlertException("Ảnh xe không hợp lệ", ENTITY, "photoInvalid");
            }
            if (photo.length() > MAX_PHOTO_LENGTH) {
                throw new BadRequestAlertException("Ảnh xe quá lớn — chụp lại", ENTITY, "photoTooLarge");
            }
        }
        VehicleOfficeEvent e = new VehicleOfficeEvent();
        e.setOffice(office);
        e.setEventType(type);
        e.setSource(c.source());
        e.setTripKey(c.tripKey());
        e.setTripCode(c.tripCode());
        e.setExternalTripId(c.externalTripId());
        e.setVehiclePlate(c.plate());
        e.setDriverName(c.driver());
        e.setRouteLabel(c.route());
        e.setPlannedDepartAt(c.departAt());
        e.setPickupAt(pickup);
        e.setEventAt(now);
        e.setReportedBy(SecurityUtils.getCurrentUserLogin().orElse(null));
        e.setReason(reason);
        try {
            e = eventRepository.saveAndFlush(e);
        } catch (DataIntegrityViolationException dup) {
            throw new BadRequestAlertException("Chuyến này vừa được báo — tải lại danh sách", ENTITY, "alreadyReported");
        }
        if (photo != null) {
            VehicleEventPhoto p = new VehicleEventPhoto();
            p.setEventId(e.getId());
            p.setPhotoUrl(photo);
            p.setCapturedAt(now);
            p.setCapturedByUsername(e.getReportedBy());
            photoRepository.save(p);
        }
        return toItem(c, e, pickup);
    }

    /** Ảnh xe của một lượt báo (screen bao-gio-xe). */
    @Transactional(readOnly = true)
    public VehicleBoardDtos.EventPhoto eventPhoto(Long eventId) {
        staffAccessService.requireScreenRead(ScreenKey.BAO_GIO_XE);
        VehicleEventPhoto p = photoRepository
            .findOneByEventId(eventId)
            .orElseThrow(() -> new BadRequestAlertException("Lượt báo này không có ảnh", ENTITY, "photoNotFound"));
        return new VehicleBoardDtos.EventPhoto(p.getEventId(), p.getPhotoUrl(), p.getCapturedAt(), p.getCapturedByUsername());
    }

    /**
     * Theo dõi quản trị: các lượt báo trong [from, to] (giờ VN), kèm lượt báo ở VP khác của cùng chuyến trong ±2 ngày
     * để ghép rời → đến. VP bị giới hạn theo phạm vi nếu không phải admin.
     */
    @Transactional(readOnly = true)
    public VehicleBoardDtos.Report reportList(LocalDate from, LocalDate to, String officeCode) {
        staffAccessService.requireScreenRead(ScreenKey.BAO_GIO_XE);
        if (from == null || to == null || to.isBefore(from)) {
            throw new BadRequestAlertException("Khoảng ngày không hợp lệ", ENTITY, "rangeInvalid");
        }
        if (ChronoUnit.DAYS.between(from, to) > MAX_REPORT_DAYS) {
            throw new BadRequestAlertException("Chỉ xem tối đa " + MAX_REPORT_DAYS + " ngày mỗi lần", ENTITY, "rangeTooLong");
        }
        String office = trimToNull(officeCode);
        if (!staffAccessService.isSystemAdmin()) {
            office = staffAccessService.scopedOfficeCode().orElse(office);
        }
        List<VehicleOfficeEvent> events = eventRepository.findForReport(
            from.atStartOfDay(VN).toInstant(),
            to.plusDays(1).atStartOfDay(VN).toInstant(),
            from.minusDays(2).atStartOfDay(VN).toInstant(),
            to.plusDays(3).atStartOfDay(VN).toInstant(),
            office
        );
        Set<String> logins = new HashSet<>();
        for (VehicleOfficeEvent e : events) {
            if (e.getReportedBy() != null) {
                logins.add(e.getReportedBy().toLowerCase(Locale.ROOT));
            }
        }
        Map<String, String> names = new HashMap<>();
        if (!logins.isEmpty()) {
            for (Object[] row : eventRepository.findStaffNames(logins)) {
                if (row[0] != null && row[1] != null) {
                    names.put(row[0].toString().toLowerCase(Locale.ROOT), row[1].toString());
                }
            }
        }
        List<Long> departIds = events
            .stream()
            .filter(e -> e.getEventType() == EventType.DEPART && e.getId() != null)
            .map(VehicleOfficeEvent::getId)
            .toList();
        Set<Long> withPhoto = departIds.isEmpty() ? Set.of() : new HashSet<>(photoRepository.findEventIdsIn(departIds));
        List<VehicleBoardDtos.ReportItem> items = events
            .stream()
            .map(e ->
                new VehicleBoardDtos.ReportItem(
                    e.getId(),
                    e.getOffice().getCode(),
                    e.getOffice().getName(),
                    e.getEventType().name(),
                    e.getSource().name(),
                    e.getTripKey(),
                    e.getTripCode(),
                    e.getExternalTripId(),
                    e.getVehiclePlate(),
                    e.getDriverName(),
                    e.getRouteLabel(),
                    e.getPlannedDepartAt(),
                    e.getEventAt(),
                    e.getReportedBy(),
                    e.getReportedBy() == null ? null : names.get(e.getReportedBy().toLowerCase(Locale.ROOT)),
                    e.getReason(),
                    e.getPickupAt() != null ? e.getPickupAt() : e.getPlannedDepartAt(),
                    withPhoto.contains(e.getId())
                )
            )
            .toList();
        List<VehicleBoardDtos.ItineraryOption> itineraries = office == null
            ? List.of()
            : officeRepository.findOneByCode(office).map(this::reportItineraries).orElse(List.of());
        return new VehicleBoardDtos.Report(items, itineraries);
    }

    private void appendCrm(List<Candidate> candidates, Set<String> points, Set<String> configured, LocalDate today) {
        Set<String> tripPlates = new HashSet<>();
        for (Candidate c : candidates) {
            if (c.plate() != null) {
                tripPlates.add(c.type() + "|" + plateKey(c.plate()));
            }
        }
        Set<String> seen = new HashSet<>();
        for (Itinerary it : itineraryRepository.findFiltered(null, true)) {
            String[] ends = itineraryEnds(it.getCode());
            if (ends == null || (!configured.isEmpty() && !configured.contains(it.getCode()))) {
                continue;
            }
            EventType type;
            LocalDate fromDay;
            if (points.contains(ends[0])) {
                type = EventType.DEPART;
                fromDay = today;
            } else if (points.contains(ends[1])) {
                type = EventType.ARRIVE;
                fromDay = today.minusDays(1);
            } else {
                continue;
            }
            List<AvailableTripDTO> trips = availableTripSearchService.searchWindow(it, fromDay.atStartOfDay(), today.atTime(23, 59, 59));
            for (AvailableTripDTO t : trips) {
                if (t.getVehiclePlate() != null && tripPlates.contains(type + "|" + plateKey(t.getVehiclePlate()))) {
                    continue;
                }
                String key = "C:" + cut(t.getExternalTripId(), 60);
                if (!seen.add(type + "|" + key)) {
                    continue;
                }
                candidates.add(
                    new Candidate(
                        type,
                        Source.CRM,
                        key,
                        null,
                        cut(t.getExternalTripId(), 60),
                        t.getVehiclePlate(),
                        firstNonBlank(t.getDriverName(), t.getAssignDriverName()),
                        firstNonBlank(it.getName(), t.getRouteLabel()),
                        t.getDepartAt()
                    )
                );
            }
        }
    }

    private Office homeOffice() {
        StaffProfile p = staffAccessService
            .current()
            .orElseThrow(() -> new BadRequestAlertException("Tài khoản chưa có hồ sơ nhân viên", ENTITY, "staffMissing"));
        if (p.getOffice() == null) {
            throw new BadRequestAlertException("Tài khoản chưa gắn văn phòng", ENTITY, "officeMissing");
        }
        return p.getOffice();
    }

    static boolean departsFrom(Trip trip, Office office) {
        Route r = trip.getRoute();
        return (
            (trip.getOffice() != null && Objects.equals(trip.getOffice().getId(), office.getId())) ||
            (r != null && r.getFromOffice() != null && Objects.equals(r.getFromOffice().getId(), office.getId()))
        );
    }

    static boolean arrivesAt(Trip trip, Office office) {
        Route r = trip.getRoute();
        return r != null && r.getToOffice() != null && Objects.equals(r.getToOffice().getId(), office.getId());
    }

    private static Candidate fromTrip(Trip trip, EventType type) {
        Route r = trip.getRoute();
        String route = firstNonBlank(trip.getItineraryLabel(), r != null ? r.getName() : null, r != null ? r.getCode() : null);
        return new Candidate(
            type,
            Source.TRIP,
            "T:" + trip.getTripCode(),
            trip.getTripCode(),
            null,
            trip.getVehicle() != null ? trip.getVehicle().getPlateNumber() : null,
            trip.getDriver() != null ? trip.getDriver().getFullName() : null,
            route,
            trip.getDepartAt()
        );
    }

    private static VehicleBoardDtos.Item toItem(Candidate c, VehicleOfficeEvent e, Instant pickupAt) {
        return new VehicleBoardDtos.Item(
            c.type() + "|" + c.tripKey(),
            c.type().name(),
            c.source().name(),
            c.tripCode(),
            c.externalTripId(),
            c.plate(),
            c.driver(),
            c.route(),
            c.departAt(),
            e != null ? e.getEventAt() : null,
            e != null ? e.getReportedBy() : null,
            pickupAt
        );
    }

    /** "GA-YB" → ["GA","YB"]; "HĐ-TB" → ["HD","TB"]. */
    static String[] itineraryEnds(String code) {
        if (code == null) {
            return null;
        }
        String[] parts = code.split("-");
        if (parts.length != 2) {
            return null;
        }
        String from = foldPoint(parts[0]);
        String to = foldPoint(parts[1]);
        return from == null || to == null ? null : new String[] { from, to };
    }

    /** Các điểm lộ trình VP kiêm (đã fold), rỗng nếu chưa cấu hình. */
    static Set<String> officePoints(Office office) {
        Set<String> out = new LinkedHashSet<>();
        for (String p : OfficeItineraryPoints.pointsOf(office)) {
            String f = foldPoint(p);
            if (f != null) {
                out.add(f);
            }
        }
        return out;
    }

    static String foldPoint(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String s = raw.trim().replace('Đ', 'D').replace('đ', 'd');
        s = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toUpperCase(Locale.ROOT);
        return s.isEmpty() ? null : s;
    }

    private static String plateKey(String plate) {
        return plate.replaceAll("[^0-9A-Za-z]", "").toUpperCase(Locale.ROOT);
    }

    private static <E extends Enum<E>> E parse(Class<E> type, String raw, String errorKey) {
        try {
            return Enum.valueOf(type, raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new BadRequestAlertException("Giá trị không hợp lệ: " + raw, ENTITY, errorKey);
        }
    }

    private static String trimToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private static String cut(String s, int max) {
        return s == null || s.length() <= max ? s : s.substring(0, max);
    }

    private static String firstNonBlank(String... vals) {
        for (String v : vals) {
            if (v != null && !v.isBlank()) {
                return v.trim();
            }
        }
        return null;
    }
}
