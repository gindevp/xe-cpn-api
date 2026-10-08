package com.mycompany.myapp.service.geo;

import com.mycompany.myapp.web.rest.errors.BadRequestAlertException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

/**
 * Link rút gọn {@code maps.app.goo.gl} không có tọa độ. Mở redirect rồi lấy GPS điểm đến
 * ({@code !3dlat!4dlng}), không lấy tâm khung bản đồ ({@code @lat,lng}).
 */
@Service
public class GoogleMapsLinkService {

    private static final String ENTITY = "geo";
    private static final Pattern PLACE = Pattern.compile("!3d(-?\\d+(?:\\.\\d+)?)!4d(-?\\d+(?:\\.\\d+)?)");
    private static final Pattern VIEW = Pattern.compile("@(-?\\d+(?:\\.\\d+)?),\\s*(-?\\d+(?:\\.\\d+)?)");
    private static final int MAX_HOPS = 5;

    private final HttpClient http = HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.NEVER)
        .connectTimeout(Duration.ofSeconds(8))
        .build();

    public Pin resolve(String raw) {
        String text = raw == null ? "" : raw.trim();
        if (text.isEmpty()) {
            throw new BadRequestAlertException("Thiếu link Google Maps", ENTITY, "mapsLinkEmpty");
        }
        URI uri;
        try {
            uri = URI.create(text);
        } catch (Exception e) {
            throw new BadRequestAlertException("Link Google Maps không hợp lệ", ENTITY, "mapsLinkInvalid");
        }
        if (!allowed(uri)) {
            throw new BadRequestAlertException("Chỉ nhận link Google Maps", ENTITY, "mapsLinkHost");
        }
        Pin direct = parse(text);
        if (direct != null && !isShort(uri)) {
            return direct;
        }
        String current = text;
        for (int hop = 0; hop < MAX_HOPS; hop++) {
            URI next = follow(URI.create(current));
            if (next == null) {
                break;
            }
            if (!allowed(next)) {
                throw new BadRequestAlertException("Link Google Maps chuyển hướng ra ngoài", ENTITY, "mapsLinkRedirect");
            }
            Pin pin = parse(next.toString());
            if (pin != null) {
                return pin;
            }
            current = next.toString();
            if (!isShort(next)) {
                break;
            }
        }
        throw new BadRequestAlertException("Không thấy GPS trong link Google Maps", ENTITY, "mapsLinkNoGps");
    }

    /** Điểm trên link. Ưu tiên tọa độ địa điểm, rồi mới tới tâm bản đồ. */
    public static Pin parse(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        Matcher place = PLACE.matcher(text);
        Pin last = null;
        while (place.find()) {
            last = pin(place.group(1), place.group(2));
        }
        if (last != null) {
            return last;
        }
        Matcher view = VIEW.matcher(text);
        if (view.find()) {
            return pin(view.group(1), view.group(2));
        }
        return null;
    }

    private URI follow(URI uri) {
        try {
            HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(12))
                .header("User-Agent", "Mozilla/5.0")
                .header("Accept", "text/html")
                .GET()
                .build();
            HttpResponse<Void> response = http.send(request, HttpResponse.BodyHandlers.discarding());
            if (response.statusCode() < 300 || response.statusCode() >= 400) {
                return null;
            }
            return response.headers().firstValue("location").map(uri::resolve).orElse(null);
        } catch (Exception e) {
            throw new BadRequestAlertException("Không mở được link Google Maps", ENTITY, "mapsLinkFetch");
        }
    }

    private static boolean isShort(URI uri) {
        String host = host(uri);
        return "maps.app.goo.gl".equals(host) || "goo.gl".equals(host);
    }

    private static boolean allowed(URI uri) {
        String host = host(uri);
        if (host.isEmpty()) {
            return false;
        }
        return (
            "maps.app.goo.gl".equals(host) ||
            "goo.gl".equals(host) ||
            "google.com".equals(host) ||
            host.endsWith(".google.com") ||
            "google.com.vn".equals(host) ||
            host.endsWith(".google.com.vn")
        );
    }

    private static String host(URI uri) {
        String host = uri.getHost();
        return host == null ? "" : host.toLowerCase();
    }

    private static Pin pin(String latRaw, String lngRaw) {
        try {
            double lat = Double.parseDouble(latRaw);
            double lng = Double.parseDouble(lngRaw);
            if (Math.abs(lat) > 90 || Math.abs(lng) > 180) {
                return null;
            }
            return new Pin(lat, lng);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public record Pin(double lat, double lng) {}
}
