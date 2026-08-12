package com.mycompany.myapp.service.trip;

import com.mycompany.myapp.repository.TripRepository;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Component;

/** Format aligned with FE {@code genTripCode}: {@code T{office}{yyMMdd}-{seq}}. */
@Component
public class TripCodeGenerator {

    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyMMdd");

    private final TripRepository tripRepository;

    public TripCodeGenerator(TripRepository tripRepository) {
        this.tripRepository = tripRepository;
    }

    public String nextTripCode(String officeCode) {
        String office = officeCode == null || officeCode.isBlank() ? "XX" : officeCode.trim().toUpperCase();
        String stamp = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")).format(STAMP);
        String prefix = "T" + office + stamp;
        long seq = tripRepository.countByTripCodePrefix(prefix) + 1;
        return prefix + "-" + String.format("%02d", seq);
    }
}
