package com.mycompany.myapp.service.config;

import com.mycompany.myapp.domain.Office;
import com.mycompany.myapp.domain.OfficeQrScreen;
import com.mycompany.myapp.domain.ShipmentOrder;
import com.mycompany.myapp.domain.enumeration.OrderStatus;
import com.mycompany.myapp.repository.OfficeQrScreenRepository;
import com.mycompany.myapp.repository.OfficeRepository;
import com.mycompany.myapp.repository.OrderGoodsPhotoRepository;
import com.mycompany.myapp.service.storage.StoredMedia;
import com.mycompany.myapp.web.rest.errors.BadRequestAlertException;
import jakarta.persistence.EntityManager;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Màn hình QR từng văn phòng: một thiết bị phát, mã đổi theo số giây cấu hình. */
@Service
public class OfficeQrScreenService {

    /** Máy không gửi nhịp trong khoảng này thì máy khác được nhận quyền phát. */
    static final int STALE_SECONDS = 20;

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Pattern TENHANG = Pattern.compile("\\[TENHANG\\]([\\s\\S]*?)\\[/TENHANG\\]");
    private static final Pattern LOAI = Pattern.compile("\\[LOAI\\]([\\s\\S]*?)\\[/LOAI\\]");
    private static final Pattern PKGKG = Pattern.compile("\\[PKGKG\\]([\\d.,\\s]*)\\[/PKGKG\\]");
    private static final Pattern PKGDIM = Pattern.compile("\\[PKGDIM\\]([\\d.x|\\s]*)\\[/PKGDIM\\]");
    private static final List<OrderStatus> OPEN = List.of(
        OrderStatus.CONFIRMED,
        OrderStatus.WAITING,
        OrderStatus.IN_TRANSIT,
        OrderStatus.AT_DEST,
        OrderStatus.OUT_FOR_DELIVERY,
        OrderStatus.FAILED_DELIVERY
    );

    private final OfficeQrScreenRepository screenRepository;
    private final OfficeRepository officeRepository;
    private final OrderGoodsPhotoRepository goodsPhotoRepository;
    private final StoredMedia storedMedia;
    private final TrackLookupLimitService trackLookupLimitService;
    private final EntityManager em;

    public OfficeQrScreenService(
        OfficeQrScreenRepository screenRepository,
        OfficeRepository officeRepository,
        OrderGoodsPhotoRepository goodsPhotoRepository,
        StoredMedia storedMedia,
        TrackLookupLimitService trackLookupLimitService,
        EntityManager em
    ) {
        this.screenRepository = screenRepository;
        this.officeRepository = officeRepository;
        this.goodsPhotoRepository = goodsPhotoRepository;
        this.storedMedia = storedMedia;
        this.trackLookupLimitService = trackLookupLimitService;
        this.em = em;
    }

    public record ScreenLink(String officeCode, String officeName, String displayKey, boolean showing) {}

    public record Pulse(String officeCode, String officeName, int refreshSeconds, String token, Instant expiresAt) {}

    public record Piece(int seq, String weightKg, String dimensions) {}

    public record PickupOrder(
        String orderCode,
        String goodsLabel,
        String senderPhone,
        String receiverPhone,
        String fromOfficeName,
        List<Piece> packages,
        String photoUrl
    ) {}

    @Transactional
    public List<ScreenLink> listLinks() {
        List<ScreenLink> out = new ArrayList<>();
        List<Office> offices = officeRepository
            .findAll()
            .stream()
            .filter(o -> o.getCode() != null && !o.getCode().isBlank() && !Boolean.FALSE.equals(o.getActive()))
            .sorted(Comparator.comparing(Office::getName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
            .toList();
        Instant staleBefore = Instant.now().minusSeconds(STALE_SECONDS);
        for (Office office : offices) {
            String code = office.getCode().trim().toUpperCase(Locale.ROOT);
            OfficeQrScreen screen = screenRepository.findByOfficeCode(code).orElseGet(() -> create(code));
            boolean showing = screen.getSeenAt() != null && screen.getSeenAt().isAfter(staleBefore);
            out.add(new ScreenLink(code, office.getName(), screen.getDisplayKey(), showing));
        }
        return out;
    }

    @Transactional
    public ScreenLink rotate(String officeCode) {
        String code = officeCode == null ? "" : officeCode.trim().toUpperCase(Locale.ROOT);
        Office office = officeRepository
            .findOneByCodeIgnoreCase(code)
            .orElseThrow(() -> new BadRequestAlertException("Không có văn phòng", "officeQr", "notfound"));
        OfficeQrScreen screen = screenRepository.findByOfficeCode(code).orElseGet(() -> create(code));
        screen.setDisplayKey(newKey());
        screen.setDeviceId(null);
        screen.setSeenAt(null);
        screen.setTokenHash(null);
        screen.setTokenExpiresAt(null);
        screenRepository.save(screen);
        return new ScreenLink(code, office.getName(), screen.getDisplayKey(), false);
    }

    @Transactional
    public Pulse pulse(String displayKey, String deviceId, boolean holdingToken) {
        String key = displayKey == null ? "" : displayKey.trim();
        String device = deviceId == null ? "" : deviceId.trim();
        if (!device.matches("[A-Za-z0-9._-]{8,80}")) {
            throw new BadRequestAlertException("Thiết bị không hợp lệ", "officeQr", "device");
        }
        OfficeQrScreen screen = screenRepository
            .findByDisplayKey(key)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Link màn hình không còn dùng được"));
        Instant now = Instant.now();
        boolean same = device.equals(screen.getDeviceId());
        boolean free = screen.getSeenAt() == null || screen.getSeenAt().isBefore(now.minusSeconds(STALE_SECONDS));
        if (!same && !free) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Màn hình văn phòng này đang mở trên một thiết bị khác");
        }
        int refresh = trackLookupLimitService.getPolicy().getQrRefreshSeconds();
        String token = null;
        if (
            !holdingToken ||
            screen.getTokenHash() == null ||
            screen.getTokenExpiresAt() == null ||
            !screen.getTokenExpiresAt().isAfter(now.plusSeconds(1))
        ) {
            token = newToken();
            screen.setTokenHash(sha256(token));
            screen.setTokenExpiresAt(now.plusSeconds(refresh));
        }
        screen.setDeviceId(device);
        screen.setSeenAt(now);
        screenRepository.save(screen);
        String officeName = officeRepository
            .findOneByCodeIgnoreCase(screen.getOfficeCode())
            .map(Office::getName)
            .orElse(screen.getOfficeCode());
        return new Pulse(screen.getOfficeCode(), officeName, refresh, token, screen.getTokenExpiresAt());
    }

    @Transactional
    public List<PickupOrder> lookup(String token, String query, String deviceId, String clientIp) {
        trackLookupLimitService.consume(deviceId, clientIp);
        String raw = token == null ? "" : token.trim();
        if (raw.isEmpty()) {
            throw new BadRequestAlertException("Quét lại mã trên màn hình văn phòng", "officeQr", "token");
        }
        OfficeQrScreen screen = screenRepository.findByTokenHash(sha256(raw)).orElse(null);
        if (screen == null || screen.getTokenExpiresAt() == null || !screen.getTokenExpiresAt().isAfter(Instant.now())) {
            throw new BadRequestAlertException("Mã QR đã hết hạn. Quét lại mã trên màn hình văn phòng.", "officeQr", "expired");
        }
        String q = query == null ? "" : query.trim();
        if (q.isEmpty()) {
            throw new BadRequestAlertException("Nhập mã đơn hoặc số điện thoại người nhận", "officeQr", "query");
        }
        String office = screen.getOfficeCode();
        List<ShipmentOrder> orders = looksLikePhone(q) ? byPhone(office, q) : byCode(office, q);
        if (orders.isEmpty()) {
            orders = looksLikePhone(q) ? byCode(office, q) : byPhone(office, q);
        }
        List<PickupOrder> out = new ArrayList<>();
        for (ShipmentOrder o : orders) {
            if (out.size() >= 8) break;
            out.add(
                new PickupOrder(
                    o.getOrderCode(),
                    goodsLabel(o),
                    o.getSenderPhone(),
                    o.getReceiverPhone(),
                    o.getFromOffice() != null ? o.getFromOffice().getName() : null,
                    pieces(o),
                    photoUrl(o)
                )
            );
        }
        return out;
    }

    private List<ShipmentOrder> byCode(String office, String query) {
        String code = query.toUpperCase(Locale.ROOT);
        return em
            .createQuery(
                "select o from ShipmentOrder o left join fetch o.fromOffice left join o.finalToOffice ft left join o.toOffice t" +
                " where o.status in :open and upper(coalesce(ft.code, t.code)) = :office" +
                " and (upper(o.orderCode) = :code or upper(coalesce(o.draftCode, '')) = :code)" +
                " order by o.createdAt desc",
                ShipmentOrder.class
            )
            .setParameter("open", OPEN)
            .setParameter("office", office)
            .setParameter("code", code)
            .setMaxResults(8)
            .getResultList();
    }

    private List<ShipmentOrder> byPhone(String office, String query) {
        String digits = digits(query);
        if (digits.length() < 9 || digits.length() > 12) {
            return List.of();
        }
        String local = digits.startsWith("84") && digits.length() >= 11 ? "0" + digits.substring(2) : digits;
        String intl = local.startsWith("0") ? "84" + local.substring(1) : local;
        return em
            .createQuery(
                "select o from ShipmentOrder o left join fetch o.fromOffice left join o.finalToOffice ft left join o.toOffice t" +
                " where o.status in :open and upper(coalesce(ft.code, t.code)) = :office" +
                " and function('regexp_replace', o.receiverPhone, '[^0-9]', '') in :phones" +
                " order by o.createdAt desc",
                ShipmentOrder.class
            )
            .setParameter("open", OPEN)
            .setParameter("office", office)
            .setParameter("phones", List.of(local, intl, digits))
            .setMaxResults(8)
            .getResultList();
    }

    private OfficeQrScreen create(String officeCode) {
        OfficeQrScreen row = new OfficeQrScreen();
        row.setOfficeCode(officeCode);
        row.setDisplayKey(newKey());
        return screenRepository.save(row);
    }

    private static boolean looksLikePhone(String query) {
        String digits = digits(query);
        return digits.length() >= 9 && digits.length() <= 12 && digits.length() >= query.replaceAll("\\s+", "").length() - 2;
    }

    private static String digits(String raw) {
        return raw == null ? "" : raw.replaceAll("\\D", "");
    }

    static List<Piece> pieces(ShipmentOrder order) {
        String note = order.getNote() == null ? "" : order.getNote();
        String[] weights = between(note, PKGKG).split(",", -1);
        String[] dims = between(note, PKGDIM).split("\\|", -1);
        int declared = order.getQuantity() == null ? 1 : Math.max(1, order.getQuantity());
        int n = Math.max(declared, Math.max(countFilled(weights), countFilled(dims)));
        boolean anyPkg = countFilled(weights) > 0 || countFilled(dims) > 0;
        List<Piece> out = new ArrayList<>();
        if (!anyPkg) {
            String weight = order.getWeightKg() == null ? "" : trimNum(order.getWeightKg().toPlainString());
            String dim = prettyDim(order.getDimensionsText());
            if (!weight.isBlank() || !dim.isBlank()) {
                out.add(new Piece(1, weight, dim));
            }
            return out;
        }
        for (int i = 0; i < n; i++) {
            String weight = i < weights.length ? trimNum(weights[i]) : "";
            String dim = i < dims.length ? prettyDim(dims[i]) : "";
            if (weight.isBlank() && dim.isBlank()) {
                continue;
            }
            out.add(new Piece(i + 1, weight, dim));
        }
        return out;
    }

    private String photoUrl(ShipmentOrder order) {
        if (order.getId() == null) {
            return null;
        }
        return goodsPhotoRepository
            .findOneByOrderId(order.getId())
            .map(p -> storedMedia.expose(p.getPhotoUrl()))
            .filter(url -> url != null && !url.isBlank())
            .orElse(null);
    }

    private static int countFilled(String[] parts) {
        int n = 0;
        for (String part : parts) {
            if (part != null && !part.isBlank()) {
                n++;
            }
        }
        return n;
    }

    private static String trimNum(String raw) {
        if (raw == null) {
            return "";
        }
        String s = raw.trim();
        if (s.isEmpty()) {
            return "";
        }
        try {
            return new java.math.BigDecimal(s).stripTrailingZeros().toPlainString();
        } catch (NumberFormatException ex) {
            return s;
        }
    }

    private static String prettyDim(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String[] p = raw.trim().toLowerCase(Locale.ROOT).split("x");
        if (p.length != 3) {
            return raw.trim();
        }
        try {
            String d = trimNum(p[0]);
            String r = trimNum(p[1]);
            String c = trimNum(p[2]);
            if (d.isEmpty() && r.isEmpty() && c.isEmpty()) {
                return "";
            }
            return d + " × " + r + " × " + c + " cm";
        } catch (NumberFormatException ex) {
            return raw.trim();
        }
    }

    static String goodsLabel(ShipmentOrder order) {
        String note = order.getNote() == null ? "" : order.getNote();
        List<String> names = split(between(note, TENHANG));
        List<String> kinds = split(between(note, LOAI));
        List<String> labels = new ArrayList<>();
        int n = Math.max(names.size(), kinds.size());
        for (int i = 0; i < n; i++) {
            String kind = i < kinds.size() ? kinds.get(i) : "";
            String name = i < names.size() ? names.get(i) : "";
            if (!kind.isBlank() && !name.isBlank()) labels.add(kind + " (" + name + ")");
            else if (!name.isBlank()) labels.add(name);
            else if (!kind.isBlank()) labels.add(kind);
        }
        if (!labels.isEmpty()) {
            return String.join(", ", labels);
        }
        return order.getGoodsType() == null ? "Hàng hoá" : order.getGoodsType().name();
    }

    private static String between(String note, Pattern pattern) {
        Matcher m = pattern.matcher(note);
        return m.find() ? m.group(1) : "";
    }

    private static List<String> split(String raw) {
        List<String> out = new ArrayList<>();
        if (raw == null || raw.isBlank()) return out;
        for (String part : raw.split("\\|")) {
            String s = part.trim();
            if (!s.isEmpty()) out.add(s);
        }
        return out;
    }

    private static String newKey() {
        byte[] buf = new byte[18];
        RANDOM.nextBytes(buf);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
    }

    private static String newToken() {
        byte[] buf = new byte[18];
        RANDOM.nextBytes(buf);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
    }

    static String sha256(String raw) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    /** Giữ chữ ký record không bị cảnh báo unused khi map JSON bằng tay. */
    public static Map<String, Object> pulseBody(Pulse pulse) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("officeCode", pulse.officeCode());
        body.put("officeName", pulse.officeName());
        body.put("refreshSeconds", pulse.refreshSeconds());
        body.put("token", pulse.token());
        body.put("expiresAt", pulse.expiresAt());
        return body;
    }
}
