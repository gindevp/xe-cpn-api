package com.mycompany.myapp.service.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myapp.domain.IntegrationConfig;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class ConfigFacadeServiceMisaAutoIssueTest {

    private static final Instant T1 = Instant.parse("2026-10-02T10:00:00Z");
    private static final Instant T2 = Instant.parse("2026-10-03T10:00:00Z");

    private static IntegrationConfig incoming(Boolean enabled) {
        IntegrationConfig c = new IntegrationConfig();
        c.setMisaAutoIssueEnabled(enabled);
        return c;
    }

    @Test
    void turnOn_recordsSince_turnOnAgainKeepsIt() {
        IntegrationConfig current = new IntegrationConfig();
        current.setMisaAutoIssueEnabled(false);

        ConfigFacadeService.mergeMisaAutoIssue(current, incoming(true), T1);
        assertThat(current.getMisaAutoIssueEnabled()).isTrue();
        assertThat(current.getMisaAutoIssueSince()).isEqualTo(T1);

        ConfigFacadeService.mergeMisaAutoIssue(current, incoming(true), T2);
        assertThat(current.getMisaAutoIssueSince()).isEqualTo(T1);
    }

    @Test
    void offThenOn_movesSinceForward_soOffPeriodIsNotBackfilled() {
        IntegrationConfig current = new IntegrationConfig();
        current.setMisaAutoIssueEnabled(true);
        current.setMisaAutoIssueSince(T1);

        ConfigFacadeService.mergeMisaAutoIssue(current, incoming(false), T1);
        assertThat(current.getMisaAutoIssueEnabled()).isFalse();

        ConfigFacadeService.mergeMisaAutoIssue(current, incoming(true), T2);
        assertThat(current.getMisaAutoIssueSince()).isEqualTo(T2);
    }

    @Test
    void nullMeansUnchanged() {
        IntegrationConfig current = new IntegrationConfig();
        current.setMisaAutoIssueEnabled(true);
        current.setMisaAutoIssueSince(T1);
        ConfigFacadeService.mergeMisaAutoIssue(current, incoming(null), T2);
        assertThat(current.getMisaAutoIssueEnabled()).isTrue();
        assertThat(current.getMisaAutoIssueSince()).isEqualTo(T1);
    }
}
