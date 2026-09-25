package com.mycompany.myapp.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;

class SessionCutoffTest {

    @Test
    void loginBeforeCutoffEndsSameDay() {
        Instant issued = Instant.parse("2026-09-25T01:00:00Z"); // 08:00 VN
        Instant end = SessionCutoff.nextEnd(issued, LocalTime.of(21, 0));
        assertThat(end).isEqualTo(Instant.parse("2026-09-25T14:00:00Z")); // 21:00 VN
    }

    @Test
    void loginAfterCutoffEndsNextDay() {
        Instant issued = Instant.parse("2026-09-25T15:00:00Z"); // 22:00 VN
        Instant end = SessionCutoff.nextEnd(issued, LocalTime.of(21, 0));
        assertThat(end).isEqualTo(Instant.parse("2026-09-26T14:00:00Z"));
    }

    @Test
    void rejectsBadTime() {
        assertThatThrownBy(() -> SessionCutoff.parseTime("9pm")).isInstanceOf(IllegalArgumentException.class);
    }
}
