package com.mycompany.myapp.service.vehicle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mycompany.myapp.domain.Itinerary;
import com.mycompany.myapp.domain.Office;
import com.mycompany.myapp.domain.OfficeVehicleItinerary;
import com.mycompany.myapp.domain.Route;
import com.mycompany.myapp.domain.StaffProfile;
import com.mycompany.myapp.domain.Trip;
import com.mycompany.myapp.domain.Vehicle;
import com.mycompany.myapp.domain.VehicleEventPhoto;
import com.mycompany.myapp.domain.VehicleOfficeEvent;
import com.mycompany.myapp.domain.VehicleOfficeEvent.EventType;
import com.mycompany.myapp.repository.ItineraryRepository;
import com.mycompany.myapp.repository.OfficeRepository;
import com.mycompany.myapp.repository.OfficeVehicleItineraryRepository;
import com.mycompany.myapp.repository.TripRepository;
import com.mycompany.myapp.repository.VehicleEventPhotoRepository;
import com.mycompany.myapp.repository.VehicleOfficeEventRepository;
import com.mycompany.myapp.security.StaffAccessService;
import com.mycompany.myapp.service.dto.trip.AvailableTripDTO;
import com.mycompany.myapp.service.dto.vehicle.VehicleBoardDtos;
import com.mycompany.myapp.service.partner.AvailableTripSearchService;
import com.mycompany.myapp.service.partner.VthkTripSearchClient;
import com.mycompany.myapp.web.rest.errors.BadRequestAlertException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VehicleBoardServiceTest {

    private static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");

    @Mock
    private TripRepository tripRepository;

    @Mock
    private ItineraryRepository itineraryRepository;

    @Mock
    private VehicleOfficeEventRepository eventRepository;

    @Mock
    private AvailableTripSearchService availableTripSearchService;

    @Mock
    private VthkTripSearchClient vthkClient;

    @Mock
    private StaffAccessService staffAccessService;

    @Mock
    private OfficeVehicleItineraryRepository officeItineraryRepository;

    @Mock
    private OfficeRepository officeRepository;

    @Mock
    private VehicleEventPhotoRepository photoRepository;

    private VehicleBoardService service;
    private Office ga;
    private Office yb;

    @BeforeEach
    void setUp() {
        service = new VehicleBoardService(
            tripRepository,
            itineraryRepository,
            eventRepository,
            availableTripSearchService,
            vthkClient,
            staffAccessService,
            officeItineraryRepository,
            officeRepository,
            photoRepository
        );
        ga = office(1L, "GA");
        yb = office(2L, "YB");
        lenient().when(vthkClient.isEnabled()).thenReturn(false);
    }

    private static Office office(Long id, String code) {
        Office o = new Office();
        o.setId(id);
        o.setCode(code);
        o.setName(code);
        return o;
    }

    private void loginAt(Office o) {
        StaffProfile p = new StaffProfile();
        p.setUserLogin("nv01");
        p.setOffice(o);
        when(staffAccessService.current()).thenReturn(Optional.of(p));
    }

    private static Trip trip(String code, Office from, Office to, Instant departAt) {
        Route r = new Route();
        r.setFromOffice(from);
        r.setToOffice(to);
        r.setName(from.getCode() + " - " + to.getCode());
        Vehicle v = new Vehicle();
        v.setPlateNumber("29E-25636");
        Trip t = new Trip();
        t.setTripCode(code);
        t.setOffice(from);
        t.setRoute(r);
        t.setVehicle(v);
        t.setDepartAt(departAt);
        return t;
    }

    private static Instant todayAt(int hour) {
        return LocalDate.now(VN).atTime(hour, 0).atZone(VN).toInstant();
    }

    @Test
    void departingTripShowsDepartForOriginAndArriveForDestination() {
        Trip t = trip("T1", ga, yb, todayAt(8));
        when(tripRepository.findForOfficeBoard(eq(1L), any(), any())).thenReturn(List.of(t));
        loginAt(ga);
        VehicleBoardDtos.Board atGa = service.board();
        assertThat(atGa.items()).extracting(VehicleBoardDtos.Item::eventType).containsExactly("DEPART");

        when(tripRepository.findForOfficeBoard(eq(2L), any(), any())).thenReturn(List.of(t));
        loginAt(yb);
        VehicleBoardDtos.Board atYb = service.board();
        assertThat(atYb.items()).extracting(VehicleBoardDtos.Item::eventType).containsExactly("ARRIVE");
    }

    @Test
    void yesterdayTripOnlyShownAsArrivalUntilReported() {
        Trip t = trip("T0", ga, yb, todayAt(8).minusSeconds(86_400));
        when(tripRepository.findForOfficeBoard(eq(2L), any(), any())).thenReturn(List.of(t));
        loginAt(yb);
        assertThat(service.board().items()).hasSize(1);

        VehicleOfficeEvent e = new VehicleOfficeEvent();
        e.setEventType(EventType.ARRIVE);
        e.setTripKey("T:T0");
        e.setEventAt(Instant.now());
        when(eventRepository.findByOffice_IdAndTripKeyIn(eq(2L), any())).thenReturn(List.of(e));
        assertThat(service.board().items()).isEmpty();

        when(tripRepository.findForOfficeBoard(eq(1L), any(), any())).thenReturn(List.of(t));
        loginAt(ga);
        assertThat(service.board().items()).isEmpty();
    }

    @Test
    void reportedTodayKeepsCardWithTime() {
        Trip t = trip("T1", ga, yb, todayAt(8));
        when(tripRepository.findForOfficeBoard(eq(1L), any(), any())).thenReturn(List.of(t));
        VehicleOfficeEvent e = new VehicleOfficeEvent();
        e.setEventType(EventType.DEPART);
        e.setTripKey("T:T1");
        e.setEventAt(Instant.now());
        e.setReportedBy("nv01");
        when(eventRepository.findByOffice_IdAndTripKeyIn(eq(1L), any())).thenReturn(List.of(e));
        loginAt(ga);

        VehicleBoardDtos.Item item = service.board().items().get(0);
        assertThat(item.reportedAt()).isNotNull();
        assertThat(item.reportedBy()).isEqualTo("nv01");
    }

    @Test
    void reportTwiceReturnsExistingWithoutNewRow() {
        Trip t = trip("T1", ga, yb, todayAt(8));
        when(tripRepository.findOneByTripCode("T1")).thenReturn(Optional.of(t));
        VehicleOfficeEvent existing = new VehicleOfficeEvent();
        existing.setEventAt(Instant.parse("2026-09-27T01:15:00Z"));
        when(eventRepository.findOneByOffice_IdAndEventTypeAndTripKey(1L, EventType.DEPART, "T:T1")).thenReturn(Optional.of(existing));
        loginAt(ga);

        VehicleBoardDtos.Item item = service.report(
            new VehicleBoardDtos.ReportRequest("DEPART", "TRIP", "T1", null, null, null, null, null)
        );

        assertThat(item.reportedAt()).isEqualTo(existing.getEventAt());
        verify(eventRepository, never()).saveAndFlush(any());
    }

    @Test
    void reportRejectsTripNotRelatedToOffice() {
        Trip t = trip("T1", ga, yb, todayAt(8));
        when(tripRepository.findOneByTripCode("T1")).thenReturn(Optional.of(t));
        loginAt(ga);

        assertThatThrownBy(() -> service.report(new VehicleBoardDtos.ReportRequest("ARRIVE", "TRIP", "T1", null, null, null, null, null))
        ).isInstanceOf(BadRequestAlertException.class);
    }

    @Test
    void reportCrmTripSavesSnapshot() {
        when(eventRepository.findOneByOffice_IdAndEventTypeAndTripKey(anyLong(), any(), any())).thenReturn(Optional.empty());
        when(eventRepository.saveAndFlush(any(VehicleOfficeEvent.class))).thenAnswer(inv -> inv.getArgument(0));
        loginAt(ga);

        VehicleBoardDtos.Item item = service.report(
            new VehicleBoardDtos.ReportRequest("ARRIVE", "CRM", null, "CH123", "30H-83330", "Lê Anh Tuấn", "GA - YB", todayAt(9))
        );

        assertThat(item.source()).isEqualTo("CRM");
        assertThat(item.vehiclePlate()).isEqualTo("30H-83330");
        assertThat(item.reportedAt()).isNotNull();
    }

    @Test
    void departRequiresArriveFirst() {
        when(eventRepository.findOneByOffice_IdAndEventTypeAndTripKey(anyLong(), any(), any())).thenReturn(Optional.empty());
        loginAt(ga);

        assertThatThrownBy(() ->
            service.report(new VehicleBoardDtos.ReportRequest("DEPART", "CRM", null, "CH123", "30H-83330", null, null, todayAt(9)))
        )
            .isInstanceOf(BadRequestAlertException.class)
            .hasMessageContaining("Chưa báo xe đến");
        verify(eventRepository, never()).saveAndFlush(any());
    }

    @Test
    void departAfterArriveSaves() {
        VehicleOfficeEvent arrived = new VehicleOfficeEvent();
        arrived.setEventAt(Instant.now());
        when(eventRepository.findOneByOffice_IdAndEventTypeAndTripKey(1L, EventType.DEPART, "C:CH123")).thenReturn(Optional.empty());
        when(eventRepository.findOneByOffice_IdAndEventTypeAndTripKey(1L, EventType.ARRIVE, "C:CH123")).thenReturn(Optional.of(arrived));
        when(eventRepository.saveAndFlush(any(VehicleOfficeEvent.class))).thenAnswer(inv -> inv.getArgument(0));
        loginAt(ga);

        VehicleBoardDtos.Item item = service.report(depart(Instant.now(), null, null, null, PHOTO));
        assertThat(item.eventType()).isEqualTo("DEPART");
        assertThat(item.reportedAt()).isNotNull();
    }

    private static final String PHOTO = "data:image/jpeg;base64,AAAA";

    private static VehicleBoardDtos.ReportRequest depart(
        Instant planned,
        String reason,
        String routeLabel,
        String itineraryCode,
        String photo
    ) {
        return new VehicleBoardDtos.ReportRequest(
            "DEPART",
            "CRM",
            null,
            "CH123",
            "30H-83330",
            null,
            routeLabel,
            planned,
            reason,
            itineraryCode,
            photo
        );
    }

    private void arrivedAtGa() {
        VehicleOfficeEvent arrived = new VehicleOfficeEvent();
        arrived.setEventAt(Instant.now().minusSeconds(60 * 60));
        when(eventRepository.findOneByOffice_IdAndEventTypeAndTripKey(1L, EventType.DEPART, "C:CH123")).thenReturn(Optional.empty());
        when(eventRepository.findOneByOffice_IdAndEventTypeAndTripKey(1L, EventType.ARRIVE, "C:CH123")).thenReturn(Optional.of(arrived));
        loginAt(ga);
    }

    @Test
    void departLateVsPickupRequiresReason() {
        arrivedAtGa();

        assertThatThrownBy(() ->
            service.report(
                new VehicleBoardDtos.ReportRequest(
                    "DEPART",
                    "CRM",
                    null,
                    "CH123",
                    "30H-83330",
                    null,
                    null,
                    Instant.now().minusSeconds(6 * 60),
                    "  "
                )
            )
        )
            .isInstanceOf(BadRequestAlertException.class)
            .extracting(ex -> ((BadRequestAlertException) ex).getErrorKey())
            .isEqualTo("lateReasonRequired");
        verify(eventRepository, never()).saveAndFlush(any());
    }

    @Test
    void longDwellButOnTimeVsPickupNeedsNoReason() {
        arrivedAtGa();
        when(eventRepository.saveAndFlush(any(VehicleOfficeEvent.class))).thenAnswer(inv -> inv.getArgument(0));

        VehicleBoardDtos.Item item = service.report(depart(Instant.now().plusSeconds(600), null, null, null, PHOTO));
        assertThat(item.reportedAt()).isNotNull();
    }

    @Test
    void departWithoutPhotoIsRejected() {
        arrivedAtGa();

        assertThatThrownBy(() -> service.report(depart(Instant.now(), null, null, null, "  ")))
            .isInstanceOf(BadRequestAlertException.class)
            .extracting(ex -> ((BadRequestAlertException) ex).getErrorKey())
            .isEqualTo("photoRequired");
        verify(eventRepository, never()).saveAndFlush(any());
    }

    @Test
    void departWithNonImagePhotoIsRejected() {
        arrivedAtGa();

        assertThatThrownBy(() -> service.report(depart(Instant.now(), null, null, null, "hello")))
            .isInstanceOf(BadRequestAlertException.class)
            .extracting(ex -> ((BadRequestAlertException) ex).getErrorKey())
            .isEqualTo("photoInvalid");
    }

    @Test
    void departSavesPhotoLinkedToEvent() {
        arrivedAtGa();
        when(eventRepository.saveAndFlush(any(VehicleOfficeEvent.class))).thenAnswer(inv -> {
            VehicleOfficeEvent e = inv.getArgument(0);
            e.setId(77L);
            return e;
        });
        ArgumentCaptor<VehicleEventPhoto> photo = ArgumentCaptor.forClass(VehicleEventPhoto.class);

        service.report(depart(Instant.now(), null, null, null, PHOTO));

        verify(photoRepository).save(photo.capture());
        assertThat(photo.getValue().getEventId()).isEqualTo(77L);
        assertThat(photo.getValue().getPhotoUrl()).isEqualTo(PHOTO);
        assertThat(photo.getValue().getCapturedAt()).isNotNull();
    }

    @Test
    void arriveDoesNotNeedOrStorePhoto() {
        when(eventRepository.findOneByOffice_IdAndEventTypeAndTripKey(1L, EventType.ARRIVE, "C:CH123")).thenReturn(Optional.empty());
        when(eventRepository.saveAndFlush(any(VehicleOfficeEvent.class))).thenAnswer(inv -> inv.getArgument(0));
        loginAt(ga);

        service.report(
            new VehicleBoardDtos.ReportRequest("ARRIVE", "CRM", null, "CH123", "30H-83330", null, null, Instant.now(), null, null, PHOTO)
        );
        verify(photoRepository, never()).save(any());
    }

    @Test
    void departLateWithReasonSavesReasonAndPickup() {
        arrivedAtGa();
        ArgumentCaptor<VehicleOfficeEvent> saved = ArgumentCaptor.forClass(VehicleOfficeEvent.class);
        when(eventRepository.saveAndFlush(saved.capture())).thenAnswer(inv -> inv.getArgument(0));
        Instant planned = Instant.now().minusSeconds(20 * 60);

        service.report(depart(planned, " Chờ hàng ", null, null, PHOTO));
        assertThat(saved.getValue().getReason()).isEqualTo("Chờ hàng");
        assertThat(saved.getValue().getPickupAt()).isEqualTo(planned);
    }

    @Test
    void pickupOffsetOfItineraryShiftsLateCheck() {
        arrivedAtGa();
        OfficeVehicleItinerary cfg = new OfficeVehicleItinerary(1L, "GA-YB", 30);
        when(officeItineraryRepository.findByOfficeId(1L)).thenReturn(List.of(cfg));
        ArgumentCaptor<VehicleOfficeEvent> saved = ArgumentCaptor.forClass(VehicleOfficeEvent.class);
        when(eventRepository.saveAndFlush(saved.capture())).thenAnswer(inv -> inv.getArgument(0));
        Instant planned = Instant.now().minusSeconds(20 * 60);

        service.report(depart(planned, null, null, "GA-YB", PHOTO));
        assertThat(saved.getValue().getPickupAt()).isEqualTo(planned.plusSeconds(30 * 60));
        assertThat(saved.getValue().getReason()).isNull();
    }

    @Test
    void pickupOffsetResolvedByRouteLabelWhenNoCode() {
        arrivedAtGa();
        when(officeItineraryRepository.findByOfficeId(1L)).thenReturn(List.of(new OfficeVehicleItinerary(1L, "GA-YB", -120)));
        when(itineraryRepository.findFiltered(null, true)).thenReturn(List.of(itinerary("GA-YB", "GA - YB")));
        ArgumentCaptor<VehicleOfficeEvent> saved = ArgumentCaptor.forClass(VehicleOfficeEvent.class);
        when(eventRepository.saveAndFlush(saved.capture())).thenAnswer(inv -> inv.getArgument(0));
        Instant planned = Instant.now().plusSeconds(150 * 60);

        service.report(depart(planned, "Đợi khách", "GA - YB", null, PHOTO));
        assertThat(saved.getValue().getPickupAt()).isEqualTo(planned.minusSeconds(120 * 60));
    }

    @Test
    void deviationMinutesAndLateThreshold() {
        Instant p = Instant.parse("2026-10-03T01:00:00Z");
        assertThat(VehicleBoardService.deviationMinutes(p, p.plusSeconds(4 * 60 + 59))).isEqualTo(4);
        assertThat(VehicleBoardService.departLate(p, p.plusSeconds(4 * 60 + 59))).isFalse();
        assertThat(VehicleBoardService.departLate(p, p.plusSeconds(5 * 60))).isTrue();
        assertThat(VehicleBoardService.deviationMinutes(p, p.minusSeconds(3 * 60))).isEqualTo(-3);
        assertThat(VehicleBoardService.departLate(p, null)).isFalse();
    }

    @Test
    void dayTripsListsAllCrmTripsWithReportedTimes() {
        Itinerary it = new Itinerary();
        it.setCode("GA-YB");
        it.setName("GA - YB");
        when(availableTripSearchService.resolveItinerary("GA-YB")).thenReturn(it);
        AvailableTripDTO a = new AvailableTripDTO();
        a.setExternalTripId("CH2");
        a.setVehiclePlate("30H-2");
        a.setDepartAt(todayAt(14));
        AvailableTripDTO b = new AvailableTripDTO();
        b.setExternalTripId("CH1");
        b.setVehiclePlate("30H-1");
        b.setDepartAt(todayAt(7));
        when(availableTripSearchService.searchWindow(eq(it), any(), any())).thenReturn(List.of(a, b));
        VehicleOfficeEvent arrived = new VehicleOfficeEvent();
        arrived.setEventType(EventType.ARRIVE);
        arrived.setTripKey("C:CH1");
        arrived.setEventAt(Instant.now());
        arrived.setReportedBy("nv01");
        when(eventRepository.findByOffice_IdAndTripKeyIn(eq(1L), any())).thenReturn(List.of(arrived));
        loginAt(ga);

        VehicleBoardDtos.DayBoard board = service.dayTrips("GA-YB");

        assertThat(board.items()).extracting(VehicleBoardDtos.DayItem::externalTripId).containsExactly("CH1", "CH2");
        assertThat(board.items().get(0).arrivedAt()).isNotNull();
        assertThat(board.items().get(0).departedAt()).isNull();
        assertThat(board.items().get(1).arrivedAt()).isNull();
    }

    private static Itinerary itinerary(String code, String name) {
        Itinerary it = new Itinerary();
        it.setCode(code);
        it.setName(name);
        return it;
    }

    @Test
    void officeItinerariesOnlyStartOrEndAtOfficePoint() {
        yb.setItineraryPoint("YB");
        when(itineraryRepository.findFiltered(null, true)).thenReturn(
            List.of(itinerary("GA-YB", "GA - YB"), itinerary("YB-HD", "YB - HĐ"), itinerary("GA-TB", "GA - TB"))
        );
        loginAt(yb);

        assertThat(service.officeItineraries())
            .extracting(VehicleBoardDtos.ItineraryOption::code)
            .containsExactlyInAnyOrder("GA-YB", "YB-HD");
    }

    @Test
    void officeItinerariesCoverEveryPointOfMultiPointOffice() {
        yb.setItineraryPoint("BC,HD");
        when(itineraryRepository.findFiltered(null, true)).thenReturn(
            List.of(itinerary("BC-ND", "BC - NĐ"), itinerary("VT-HD", "VT - HĐ"), itinerary("GA-TB", "GA - TB"))
        );
        loginAt(yb);

        assertThat(service.officeItineraries())
            .extracting(VehicleBoardDtos.ItineraryOption::code)
            .containsExactlyInAnyOrder("BC-ND", "VT-HD");
    }

    @Test
    void dayTripsRejectsItineraryNotThroughOffice() {
        yb.setItineraryPoint("YB");
        when(availableTripSearchService.resolveItinerary("GA-TB")).thenReturn(itinerary("GA-TB", "GA - TB"));
        loginAt(yb);

        assertThatThrownBy(() -> service.dayTrips("GA-TB")).isInstanceOf(BadRequestAlertException.class);
    }

    @Test
    void officeItinerariesFollowOfficeConfig() {
        ga.setItineraryPoint("GA");
        when(itineraryRepository.findFiltered(null, true)).thenReturn(
            List.of(itinerary("GA-NB", "GA - NB"), itinerary("GA-YB", "GA - YB"), itinerary("NB-GA", "NB - GA"))
        );
        when(officeItineraryRepository.findCodesByOfficeId(1L)).thenReturn(List.of("GA-NB"));
        loginAt(ga);

        assertThat(service.officeItineraries()).extracting(VehicleBoardDtos.ItineraryOption::code).containsExactly("GA-NB");
    }

    @Test
    void dayTripsRejectsItineraryOutsideOfficeConfig() {
        ga.setItineraryPoint("GA");
        when(availableTripSearchService.resolveItinerary("GA-YB")).thenReturn(itinerary("GA-YB", "GA - YB"));
        when(officeItineraryRepository.findCodesByOfficeId(1L)).thenReturn(List.of("GA-NB"));
        loginAt(ga);

        assertThatThrownBy(() -> service.dayTrips("GA-YB")).isInstanceOf(BadRequestAlertException.class);
    }

    @Test
    void saveConfigRejectsItineraryNotThroughOfficePoint() {
        ga.setItineraryPoint("GA");
        when(officeRepository.findById(1L)).thenReturn(Optional.of(ga));
        when(itineraryRepository.findFiltered(null, true)).thenReturn(
            List.of(itinerary("GA-NB", "GA - NB"), itinerary("YB-HD", "YB - HĐ"))
        );

        assertThatThrownBy(() -> service.saveItineraryConfig(1L, new VehicleBoardDtos.ItineraryConfigRequest(List.of("YB-HD")))
        ).isInstanceOf(BadRequestAlertException.class);
        verify(officeItineraryRepository, never()).deleteByOfficeId(any());
    }

    @Test
    void saveConfigReplacesSelection() {
        ga.setItineraryPoint("GA");
        when(officeRepository.findById(1L)).thenReturn(Optional.of(ga));
        when(itineraryRepository.findFiltered(null, true)).thenReturn(
            List.of(itinerary("GA-NB", "GA - NB"), itinerary("GA-YB", "GA - YB"))
        );
        when(officeItineraryRepository.findCodesByOfficeId(1L)).thenReturn(List.of("GA-NB"));

        VehicleBoardDtos.ItineraryConfig cfg = service.saveItineraryConfig(
            1L,
            new VehicleBoardDtos.ItineraryConfigRequest(List.of("GA-NB", " "))
        );

        verify(officeItineraryRepository).deleteByOfficeId(1L);
        verify(officeItineraryRepository).save(any());
        assertThat(cfg.options()).extracting(VehicleBoardDtos.ConfigOption::selected).containsExactly(true, false);
    }

    @Test
    void saveConfigStoresOffsetOnlyForSelectedItinerary() {
        ga.setItineraryPoint("GA");
        when(officeRepository.findById(1L)).thenReturn(Optional.of(ga));
        when(itineraryRepository.findFiltered(null, true)).thenReturn(
            List.of(itinerary("GA-NB", "GA - NB"), itinerary("GA-YB", "GA - YB"))
        );
        ArgumentCaptor<OfficeVehicleItinerary> saved = ArgumentCaptor.forClass(OfficeVehicleItinerary.class);

        service.saveItineraryConfig(
            1L,
            new VehicleBoardDtos.ItineraryConfigRequest(
                List.of("GA-NB"),
                List.of(new VehicleBoardDtos.ItineraryOffset("GA-NB", -120), new VehicleBoardDtos.ItineraryOffset("GA-YB", 30))
            )
        );

        verify(officeItineraryRepository).save(saved.capture());
        assertThat(saved.getValue().getItineraryCode()).isEqualTo("GA-NB");
        assertThat(saved.getValue().getOffsetMinutes()).isEqualTo(-120);
    }

    @Test
    void saveConfigRejectsOffsetOutOfRange() {
        ga.setItineraryPoint("GA");
        when(officeRepository.findById(1L)).thenReturn(Optional.of(ga));
        when(itineraryRepository.findFiltered(null, true)).thenReturn(List.of(itinerary("GA-NB", "GA - NB")));

        assertThatThrownBy(() ->
            service.saveItineraryConfig(
                1L,
                new VehicleBoardDtos.ItineraryConfigRequest(List.of("GA-NB"), List.of(new VehicleBoardDtos.ItineraryOffset("GA-NB", 1000)))
            )
        ).isInstanceOf(BadRequestAlertException.class);
        verify(officeItineraryRepository, never()).deleteByOfficeId(any());
    }

    @Test
    void allOfficeItinerariesGroupsByOfficeCode() {
        when(officeItineraryRepository.findAllOfficeCodePairs()).thenReturn(
            List.of(new Object[] { "VP_PV", "GA-NB" }, new Object[] { "VP_PV", "GA-TB" }, new Object[] { "VP_NB", "GA-NB" })
        );
        assertThat(service.allOfficeItineraries())
            .containsEntry("VP_PV", List.of("GA-NB", "GA-TB"))
            .containsEntry("VP_NB", List.of("GA-NB"));
    }

    @Test
    void itineraryCodeEnds() {
        assertThat(VehicleBoardService.itineraryEnds("GA-YB")).containsExactly("GA", "YB");
        assertThat(VehicleBoardService.itineraryEnds("HĐ-TB")).containsExactly("HD", "TB");
        assertThat(VehicleBoardService.itineraryEnds("GA")).isNull();
        assertThat(VehicleBoardService.foldPoint("hđ")).isEqualTo("HD");
    }

    @Test
    void crmIgnoredWhenDisabled() {
        when(tripRepository.findForOfficeBoard(anyLong(), any(), any())).thenReturn(List.of());
        ga.setItineraryPoint("GA");
        loginAt(ga);
        assertThat(service.board().crmWarning()).isNull();
        verify(itineraryRepository, never()).findFiltered(any(), anyBoolean());
    }
}
