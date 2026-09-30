package com.mycompany.myapp.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DemoStaffSeedTest {

    @Test
    void nonDevSeedsOnlyAdmin() {
        assertThat(DemoStaffSeed.seedsFor(false)).extracting(s -> s.login()).containsExactly("admin");
    }

    @Test
    void devKeepsFullDemoList() {
        assertThat(DemoStaffSeed.seedsFor(true)).extracting(s -> s.login()).contains("admin", "quay.hn", "kt.hn", "dh", "bl");
    }
}
