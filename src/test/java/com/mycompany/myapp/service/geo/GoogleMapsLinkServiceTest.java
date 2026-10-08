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
    }
}
