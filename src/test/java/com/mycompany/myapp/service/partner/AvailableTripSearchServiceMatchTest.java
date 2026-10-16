package com.mycompany.myapp.service.partner;

import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myapp.domain.Itinerary;
import org.junit.jupiter.api.Test;

class AvailableTripSearchServiceMatchTest {

    @Test
    void catalogPairAndProductFollowOfficePoint() {
        Itinerary gaTb = itinerary("GA - TB");
        assertThat(AvailableTripSearchService.catalogPair(gaTb)).containsExactly("ha-noi", "thai-binh");
        assertThat(AvailableTripSearchService.productMatches(gaTb, "Xe Ga Hà Nội đi Thái Bình hang xe XE")).isTrue();
        assertThat(AvailableTripSearchService.productMatches(gaTb, "Xe Hà Đông Hà Nội đi Thái Bình hang xe XE")).isFalse();
        assertThat(AvailableTripSearchService.productMatches(gaTb, "Xe BigC Hà Nội đi Thái Bình hang xe XE")).isFalse();

        Itinerary vtHd = itinerary("VT - HĐ");
        assertThat(AvailableTripSearchService.catalogPair(vtHd)).containsExactly("phu-tho", "ha-noi");
        assertThat(AvailableTripSearchService.productMatches(vtHd, "Xe Việt Trì đi Hà Đông Hà Nội hang xe XE")).isTrue();
        assertThat(AvailableTripSearchService.productMatches(vtHd, "Xe TX Phú Thọ đi Hà Đông Hà Nội hang xe XE")).isFalse();

        Itinerary phoco = itinerary("PHOCO - TC");
        assertThat(AvailableTripSearchService.catalogPair(phoco)).containsExactly("ha-noi", "ninh-binh");
        assertThat(AvailableTripSearchService.productMatches(phoco, "Xe Phố cổ Hà Nội đi Tam Cốc Ninh Bình hang xe XE")).isTrue();
        assertThat(
            AvailableTripSearchService.productMatches(itinerary("NB - BC"), "Xe Phố cổ Hà Nội đi Tam Cốc Ninh Bình hang xe XE")
        ).isFalse();
        assertThat(AvailableTripSearchService.productMatches(itinerary("NB - BC"), "Xe Ninh Bình đi BigC Hà Nội hang xe XE")).isTrue();
    }

    private static Itinerary itinerary(String name) {
        Itinerary it = new Itinerary();
        it.setName(name);
        it.setCode(name.replace(" - ", "-"));
        return it;
    }
}
