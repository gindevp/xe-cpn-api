package com.mycompany.myapp.service.invoice;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ProvinceMergerTest {

    @Test
    void exact_oldAndNewNames() {
        assertThat(ProvinceMerger.exact("Tỉnh Nam Định")).isEqualTo("Ninh Bình");
        assertThat(ProvinceMerger.exact("Hà Nam")).isEqualTo("Ninh Bình");
        assertThat(ProvinceMerger.exact("Ninh Bình")).isEqualTo("Ninh Bình");
        assertThat(ProvinceMerger.exact("Thái Bình")).isEqualTo("Hưng Yên");
        assertThat(ProvinceMerger.exact("Yên Bái")).isEqualTo("Lào Cai");
        assertThat(ProvinceMerger.exact("Vĩnh Phúc")).isEqualTo("Phú Thọ");
        assertThat(ProvinceMerger.exact("Hòa Bình")).isEqualTo("Phú Thọ");
        assertThat(ProvinceMerger.exact("Bắc Giang")).isEqualTo("Bắc Ninh");
        assertThat(ProvinceMerger.exact("Hải Dương")).isEqualTo("Hải Phòng");
        assertThat(ProvinceMerger.exact("TP. Hồ Chí Minh")).isEqualTo("Hồ Chí Minh");
        assertThat(ProvinceMerger.exact("Bình Dương")).isEqualTo("Hồ Chí Minh");
        assertThat(ProvinceMerger.exact("Thừa Thiên Huế")).isEqualTo("Huế");
        assertThat(ProvinceMerger.exact("Thành phố Hà Nội")).isEqualTo("Hà Nội");
        assertThat(ProvinceMerger.exact("Hoàn Kiếm")).isEmpty();
    }

    @Test
    void find_lastSegmentWins() {
        assertThat(ProvinceMerger.find("Số 104, Thành phố Nam Định, Tỉnh Nam Định")).isEqualTo("Ninh Bình");
        assertThat(ProvinceMerger.find("Phố Huế, Hai Bà Trưng, Hà Nội")).isEqualTo("Hà Nội");
        assertThat(ProvinceMerger.find("Bà Rịa - Vũng Tàu")).isEqualTo("Hồ Chí Minh");
    }

    @Test
    void find_containsWhenNoSegmentMatches() {
        assertThat(ProvinceMerger.find("Vincom - TX.Phú Thọ")).isEqualTo("Phú Thọ");
        assertThat(ProvinceMerger.find("18 Vũ Trọng Khánh - HN")).isEqualTo("Hà Nội");
        assertThat(ProvinceMerger.find("VP Hưng Yên")).isEqualTo("Hưng Yên");
        assertThat(ProvinceMerger.find("1 Giải Phóng")).isEmpty();
        assertThat(ProvinceMerger.find(null)).isEmpty();
    }
}
