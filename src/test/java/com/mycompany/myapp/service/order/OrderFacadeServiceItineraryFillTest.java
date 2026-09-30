package com.mycompany.myapp.service.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.mycompany.myapp.domain.Branch;
import com.mycompany.myapp.domain.Itinerary;
import com.mycompany.myapp.domain.Office;
import com.mycompany.myapp.domain.ShipmentOrder;
import com.mycompany.myapp.repository.ItineraryRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OrderFacadeServiceItineraryFillTest {

    private ItineraryRepository itineraryRepository;
    private OrderFacadeService service;

    @BeforeEach
    void setUp() {
        itineraryRepository = mock(ItineraryRepository.class);
        service = new OrderFacadeService(null, null, null, null, null, null, null, null, null, null, null, null);
        service.setItineraryRepository(itineraryRepository);
    }

    private static Office office(String point) {
        Office o = new Office();
        o.setItineraryPoint(point);
        return o;
    }

    private static Itinerary itinerary(String name, String branchName, boolean active) {
        Branch b = new Branch();
        b.setName(branchName);
        Itinerary it = new Itinerary();
        it.setName(name);
        it.setBranch(b);
        it.setActive(active);
        return it;
    }

    private static ShipmentOrder order(String fromPt, String toPt) {
        ShipmentOrder o = new ShipmentOrder();
        o.setFromOffice(office(fromPt));
        o.setToOffice(office(toPt));
        o.setFinalToOffice(office(toPt));
        return o;
    }

    @Test
    void fillsFromOfficeItineraryPoints() {
        when(itineraryRepository.findOneByCode("PT-HD")).thenReturn(Optional.of(itinerary("PT - HĐ", "Phú Thọ", true)));
        ShipmentOrder o = order("PT", "HD");

        service.fillItineraryIfMissing(o);

        assertThat(o.getRouteLabel()).isEqualTo("Phú Thọ");
        assertThat(o.getItineraryLabel()).isEqualTo("PT - HĐ");
    }

    @Test
    void keepsLabelsSentByClient() {
        ShipmentOrder o = order("PT", "HD");
        o.setRouteLabel("Tuyến FE");
        o.setItineraryLabel("Lộ trình FE");

        service.fillItineraryIfMissing(o);

        assertThat(o.getRouteLabel()).isEqualTo("Tuyến FE");
        assertThat(o.getItineraryLabel()).isEqualTo("Lộ trình FE");
    }

    @Test
    void inactiveOrMissingItineraryLeavesBlank() {
        when(itineraryRepository.findOneByCode("PT-BC")).thenReturn(Optional.of(itinerary("PT - BC", "Phú Thọ", false)));
        when(itineraryRepository.findOneByCode("PT-XX")).thenReturn(Optional.empty());
        ShipmentOrder inactive = order("PT", "BC");
        ShipmentOrder missing = order("PT", "XX");

        service.fillItineraryIfMissing(inactive);
        service.fillItineraryIfMissing(missing);

        assertThat(inactive.getItineraryLabel()).isNull();
        assertThat(missing.getRouteLabel()).isNull();
    }
}
