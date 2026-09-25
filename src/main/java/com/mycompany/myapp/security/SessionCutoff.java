package com.mycompany.myapp.security;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;

/** Phiên đăng nhập kết thúc vào giờ cấu hình theo giờ Việt Nam. */
public final class SessionCutoff {

    public static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    public static final String DEFAULT_TIME = "21:00";

    private SessionCutoff() {}

    public static LocalTime parseTime(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Giờ đăng xuất không được để trống");
        }
        try {
            LocalTime time = LocalTime.parse(raw.trim());
            if (time.getSecond() != 0 || time.getNano() != 0) {
                throw new IllegalArgumentException("Giờ đăng xuất chỉ nhận giờ và phút");
            }
            return time;
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Giờ đăng xuất phải dạng HH:mm, ví dụ 21:00");
        }
    }

    /**
     * Mốc hết phiên của một token: giờ cấu hình cùng ngày nếu phát hành trước giờ đó,
     * nếu không thì giờ đó của ngày hôm sau.
     */
    public static Instant nextEnd(Instant issuedAt, LocalTime logoutTime) {
        ZonedDateTime issued = issuedAt.atZone(ZONE);
        ZonedDateTime sameDay = ZonedDateTime.of(issued.toLocalDate(), logoutTime, ZONE);
        if (issued.isBefore(sameDay)) {
            return sameDay.toInstant();
        }
        LocalDate next = issued.toLocalDate().plusDays(1);
        return ZonedDateTime.of(next, logoutTime, ZONE).toInstant();
    }
}
