package com.mycompany.myapp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mycompany.myapp.web.rest.errors.BadRequestAlertException;
import org.junit.jupiter.api.Test;

class OfficeItineraryPointsTest {

    @Test
    void normalizesSingleAndMultiplePointsKeepingPriorityOrder() {
        assertThat(OfficeItineraryPoints.normalize("nd")).isEqualTo("ND");
        assertThat(OfficeItineraryPoints.normalize("HĐ")).isEqualTo("HD");
        assertThat(OfficeItineraryPoints.normalize(" bc , HĐ ")).isEqualTo("BC,HD");
        assertThat(OfficeItineraryPoints.normalize("BC,HD,BC")).isEqualTo("BC,HD");
        assertThat(OfficeItineraryPoints.normalize("  ")).isNull();
    }

    @Test
    void rejectsUnknownPoint() {
        assertThatThrownBy(() -> OfficeItineraryPoints.normalize("BC,XX")).isInstanceOf(BadRequestAlertException.class);
    }

    @Test
    void splitsStoredValue() {
        assertThat(OfficeItineraryPoints.split("BC,HD")).containsExactly("BC", "HD");
        assertThat(OfficeItineraryPoints.split("GA")).containsExactly("GA");
        assertThat(OfficeItineraryPoints.split(null)).isEmpty();
    }
}
