package com.mycompany.myapp.service.geo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.Test;

class GoogleMapsLinkServiceTest {

    @Test
    void parse_prefersPlacePinOverMapCenter() {
        String url = "https://www.google.com/maps/place/Trung+t%C3%A2m/@21.0247511,105.8359202,14z/data=!8m2!3d21.0184468!4d105.8122846";
        GoogleMapsLinkService.Pin pin = GoogleMapsLinkService.parse(url);
        assertThat(pin).isNotNull();
        assertThat(pin.lat()).isCloseTo(21.0184468, within(0.0000001));
        assertThat(pin.lng()).isCloseTo(105.8122846, within(0.0000001));
        assertThat(pin.address()).isEqualTo("Trung tâm");
    }

    @Test
    void parse_readsPlaceAddressAndIgnoresCoordinateQuery() {
        String url =
            "https://www.google.com/maps/place/21+%C4%90%E1%BA%A1i+T%E1%BB%AB,+Ho%C3%A0ng+Mai/@21.02,105.83,17z/data=!3d21.018!4d105.812";
        GoogleMapsLinkService.Pin pin = GoogleMapsLinkService.parse(url);
        assertThat(pin.address()).isEqualTo("21 Đại Từ, Hoàng Mai");
        assertThat(GoogleMapsLinkService.addressOf("https://maps.google.com/?q=21.018,105.812")).isNull();
    }
}
