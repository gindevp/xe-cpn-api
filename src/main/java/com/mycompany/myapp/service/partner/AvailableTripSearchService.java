package com.mycompany.myapp.service.partner;

import com.fasterxml.jackson.databind.JsonNode;
import com.mycompany.myapp.domain.Itinerary;
import com.mycompany.myapp.domain.ShipmentOrder;
import com.mycompany.myapp.domain.Trip;
import com.mycompany.myapp.repository.ItineraryRepository;
import com.mycompany.myapp.repository.ShipmentOrderRepository;
import com.mycompany.myapp.repository.TripRepository;
import com.mycompany.myapp.service.dto.trip.AvailableTripDTO;
import com.mycompany.myapp.web.rest.errors.BadRequestAlertException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Maps CPN itinerary → CRM {@code MaHanhTrinhs}, searches trips in [now, now+1h], enriches cargo load.
 */
@Service
@Transactional(readOnly = true)
public class AvailableTripSearchService {

    private static final String ENTITY = "availableTrip";
    private static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter LOCAL_DT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    private static final DateTimeFormatter CRM_DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final VthkTripSearchClient vthkClient;
    private final ItineraryRepository itineraryRepository;
    private final TripRepository tripRepository;
    private final ShipmentOrderRepository shipmentOrderRepository;

    public AvailableTripSearchService(
        VthkTripSearchClient vthkClient,
        ItineraryRepository itineraryRepository,
        TripRepository tripRepository,
        ShipmentOrderRepository shipmentOrderRepository
    ) {
        this.vthkClient = vthkClient;
        this.itineraryRepository = itineraryRepository;
        this.tripRepository = tripRepository;
        this.shipmentOrderRepository = shipmentOrderRepository;
    }

    /**
     * @param date ignored — window is always now → now+1h (VN)
     * @param timeSlot ignored — no FE time-slot filter
     * @param lfid / ltid ignored — CRM uses MaHanhTrinhs from itinerary name
     */
    public List<AvailableTripDTO> search(String date, String itineraryCodeOrName, String lfid, String ltid, String timeSlot) {
        if (blankToNull(itineraryCodeOrName) == null) {
            throw new BadRequestAlertException("itineraryCode (or name) is required", ENTITY, "itineraryRequired");
        }
        Itinerary itinerary = resolveItinerary(itineraryCodeOrName);
        String maHanhTrinh = blankToNull(itinerary.getName());
        if (maHanhTrinh == null) {
            maHanhTrinh = blankToNull(itinerary.getCode());
        }
        if (maHanhTrinh == null) {
            throw new BadRequestAlertException("Itinerary has no name/code for MaHanhTrinhs", ENTITY, "maHanhTrinhMissing");
        }

        List<JsonNode> raw = vthkClient.searchTripsByItineraries(List.of(maHanhTrinh));
        List<AvailableTripDTO> items = new ArrayList<>();
        for (JsonNode n : raw) {
            AvailableTripDTO dto = mapTrip(n, itinerary.getCode());
            if (dto.getDepartAt() == null) {
                continue;
            }
            items.add(dto);
        }
        enrichCargo(items);
        items.sort(Comparator.comparing(AvailableTripDTO::getDepartAt, Comparator.nullsLast(Comparator.naturalOrder())));
        return items;
    }

    private void enrichCargo(List<AvailableTripDTO> items) {
        if (items.isEmpty()) {
            return;
        }
        Map<String, LoadAgg> byPlate = new HashMap<>();
        for (AvailableTripDTO item : items) {
            String plate = item.getAssignVehiclePlate();
            if (plate == null || plate.isBlank()) {
                item.setUsedKg(BigDecimal.ZERO);
                item.setUsedOrderCount(0);
                continue;
            }
            LoadAgg agg = byPlate.computeIfAbsent(plate, this::loadForPlate);
            item.setUsedKg(agg.kg);
            item.setUsedOrderCount(agg.count);
        }
    }

    private LoadAgg loadForPlate(String plate) {
        Optional<Trip> tripOpt = tripRepository.findFirstByVehicle_PlateNumberAndStatusInOrderByIdDesc(
            plate,
            List.of(
                com.mycompany.myapp.domain.enumeration.TripStatus.CREATED,
                com.mycompany.myapp.domain.enumeration.TripStatus.LOADING,
                com.mycompany.myapp.domain.enumeration.TripStatus.DEPARTED
            )
        );
        if (tripOpt.isEmpty()) {
            return LoadAgg.EMPTY;
        }
        Trip trip = tripOpt.get();
        List<ShipmentOrder> orders = shipmentOrderRepository.findByCurrentTrip_Id(trip.getId());
        BigDecimal kg = BigDecimal.ZERO;
        for (ShipmentOrder o : orders) {
            if (o.getWeightKg() != null) {
                kg = kg.add(o.getWeightKg());
            }
        }
        return new LoadAgg(kg, orders.size());
    }

    private AvailableTripDTO mapTrip(JsonNode n, String itineraryCode) {
        AvailableTripDTO dto = new AvailableTripDTO();
        String id = text(n, "Ma");
        if (id == null || id.isBlank()) {
            id = text(n, "ChuyenDiId");
        }
        if (id == null || id.isBlank()) {
            id = n.path("Id").asText(null);
        }
        dto.setExternalTripId(id);
        dto.setItineraryCode(itineraryCode);
        dto.setRouteLabel(firstNonBlank(text(n, "TenHanhTrinh"), text(n, "TenLoTrinh"), text(n, "ProductName")));

        JsonNode thongTin = n.path("ThongTin");
        String plateRaw = firstNonBlank(text(n, "BienSoXe"), text(thongTin, "BienSoXe"));
        String plate = normalizePlate(plateRaw);
        dto.setVehiclePlate(plate);
        dto.setAssignVehiclePlate(plate);

        String driver = blankToNull(firstNonBlank(text(n, "TenLaiXe"), text(thongTin, "TenLaiXe")));
        dto.setDriverName(driver);
        dto.setAssignDriverName(driver);
        dto.setDriverPhone(blankToNull(firstNonBlank(text(n, "SoDienThoaiLienHe"), text(thongTin, "SoDienThoaiLienHe"))));
        dto.setVehicleType(blankToNull(firstNonBlank(text(n, "TenLoaiXe"), text(thongTin, "TenLoaiXe"))));

        if (n.has("SoGhe") && !n.get("SoGhe").isNull()) {
            dto.setSeatTotal(n.get("SoGhe").asInt());
        } else if (n.has("SoLuongGhe") && !n.get("SoLuongGhe").isNull()) {
            dto.setSeatTotal(n.get("SoLuongGhe").asInt());
        }
        if (n.has("SoGheTrong") && !n.get("SoGheTrong").isNull()) {
            dto.setSeatAvailable(n.get("SoGheTrong").asInt());
        } else if (n.has("SoLuongConTrong") && !n.get("SoLuongConTrong").isNull()) {
            dto.setSeatAvailable(n.get("SoLuongConTrong").asInt());
        }

        Instant depart = parseDepart(firstNonBlank(text(n, "NgayDiThuc"), text(n, "ThoiGianDi"), text(n, "NgayDi")));
        dto.setDepartAt(depart);
        dto.setEndAt(parseDepart(firstNonBlank(text(n, "ThoiGianDen"), text(n, "NgayKetThuc"))));
        if (depart != null) {
            dto.setTimeSlot(slotOf(depart));
        }
        return dto;
    }

    private Itinerary resolveItinerary(String codeOrName) {
        String raw = codeOrName.trim();
        return itineraryRepository
            .findOneByCode(raw.toUpperCase(Locale.ROOT))
            .or(() -> itineraryRepository.findOneByCode(raw))
            .or(() -> itineraryRepository.findFirstByNameIgnoreCase(raw))
            .orElseThrow(() -> new BadRequestAlertException("Unknown itinerary: " + codeOrName, ENTITY, "itineraryNotFound"));
    }

    /** @return [lfid, ltid] — kept for tests / legacy mapping helpers */
    static String[] toLocationPair(Itinerary itinerary) {
        String from = toSlug(itinerary.getDeparturePoint());
        String to = toSlug(itinerary.getDestinationPoint());
        if (from == null) {
            from = toSlugFromDirection(itinerary.getRouteDirection(), true);
        }
        if (to == null) {
            to = toSlugFromDirection(itinerary.getRouteDirection(), false);
        }
        if (from == null || to == null) {
            throw new BadRequestAlertException(
                "Cannot map itinerary " + itinerary.getCode() + " to VTHK lfid/ltid",
                ENTITY,
                "locationMapMissing"
            );
        }
        return new String[] { from, to };
    }

    static String toSlug(String place) {
        if (place == null || place.isBlank()) {
            return null;
        }
        String p = place.trim().toLowerCase(Locale.ROOT);
        p = p.replace('đ', 'd').replace('Đ', 'd');
        String ascii = java.text.Normalizer.normalize(p, java.text.Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        ascii = ascii.replaceAll("[^a-z0-9\\s-]", " ").replaceAll("\\s+", " ").trim();
        if (
            ascii.contains("ha noi") ||
            ascii.contains("ga ha noi") ||
            ascii.contains("ha dong") ||
            ascii.contains("big c") ||
            ascii.contains("bigc") ||
            ascii.contains("noi bai") ||
            ascii.contains("pho co")
        ) {
            return "ha-noi";
        }
        if (ascii.contains("ninh binh") || ascii.contains("tam coc")) {
            return "ninh-binh";
        }
        if (ascii.contains("thai binh")) {
            return "thai-binh";
        }
        if (ascii.contains("nam dinh")) {
            return "nam-dinh";
        }
        if (ascii.contains("phu tho")) {
            return "phu-tho";
        }
        if (ascii.contains("viet tri")) {
            return "viet-tri";
        }
        if (ascii.contains("yen bai")) {
            return "yen-bai";
        }
        return null;
    }

    private static String toSlugFromDirection(String direction, boolean fromSide) {
        if (direction == null || direction.isBlank()) {
            return null;
        }
        String[] parts = direction.split("[-–—]");
        if (parts.length < 2) {
            return null;
        }
        return toSlug(fromSide ? parts[0].trim() : parts[parts.length - 1].trim());
    }

    static String normalizePlate(String raw) {
        if (raw == null) {
            return null;
        }
        String t = raw.trim();
        if (t.isEmpty() || t.matches("-+") || "N/A".equalsIgnoreCase(t) || "null".equalsIgnoreCase(t)) {
            return null;
        }
        return t;
    }

    static String syntheticPlate(String externalTripId) {
        String id = externalTripId == null ? "0" : externalTripId.replaceAll("[^0-9A-Za-z]", "");
        String plate = "CH" + id;
        return plate.length() <= 20 ? plate : plate.substring(0, 20);
    }

    static String slotOf(Instant departAt) {
        int hour = departAt.atZone(VN).getHour();
        int start = (hour / 2) * 2;
        int end = start + 2;
        return String.format("%02d:00-%02d:00", start, end == 24 ? 24 : end).replace("24:00", "24:00");
    }

    private static Instant parseDepart(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String s = raw.trim();
        try {
            if (s.endsWith("Z") || s.contains("+")) {
                return Instant.parse(s);
            }
            if (s.length() >= 19 && s.charAt(10) == ' ') {
                LocalDateTime ldt = LocalDateTime.parse(s.substring(0, 19), CRM_DT);
                return ldt.atZone(VN).toInstant();
            }
            LocalDateTime ldt = LocalDateTime.parse(s.length() >= 19 ? s.substring(0, 19) : s, LOCAL_DT);
            return ldt.atZone(VN).toInstant();
        } catch (Exception e) {
            return null;
        }
    }

    private static String text(JsonNode n, String field) {
        if (n == null || n.isMissingNode() || n.isNull()) {
            return null;
        }
        JsonNode v = n.get(field);
        if (v == null || v.isNull()) {
            return null;
        }
        String s = v.asText(null);
        return s == null || s.isBlank() ? null : s;
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private static String firstNonBlank(String... vals) {
        if (vals == null) {
            return null;
        }
        for (String v : vals) {
            if (v != null && !v.isBlank()) {
                return v;
            }
        }
        return null;
    }

    private record LoadAgg(BigDecimal kg, int count) {
        static final LoadAgg EMPTY = new LoadAgg(BigDecimal.ZERO, 0);
    }
}
