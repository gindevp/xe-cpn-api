package com.mycompany.myapp.service.day;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.mycompany.myapp.domain.DayClosure;
import com.mycompany.myapp.domain.Office;
import com.mycompany.myapp.domain.ShipmentOrder;
import com.mycompany.myapp.domain.enumeration.DayClosureStatus;
import com.mycompany.myapp.repository.DayClosureRepository;
import com.mycompany.myapp.repository.OfficeRepository;
import com.mycompany.myapp.web.rest.errors.BadRequestAlertException;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DayClosureGuardTest {

    @Mock
    private DayClosureRepository dayClosureRepository;

    @Mock
    private OfficeRepository officeRepository;

    private DayClosureGuard guard;

    private Office office;
    private LocalDate day;

    @BeforeEach
    void setUp() {
        guard = new DayClosureGuard(dayClosureRepository, officeRepository);
        office = new Office();
        office.setId(5L);
        office.setCode("GP");
        day = LocalDate.of(2026, 8, 12);
    }

    @Test
    void assertOfficeOpen_whenClosed_throwsDayClosed() {
        DayClosure closure = new DayClosure();
        closure.setStatus(DayClosureStatus.CLOSED);
        when(dayClosureRepository.findFirstByOffice_IdAndBusinessDateOrderByIdDesc(5L, day)).thenReturn(Optional.of(closure));

        assertThatThrownBy(() -> guard.assertOfficeOpen(office, day))
            .isInstanceOf(BadRequestAlertException.class)
            .hasMessageContaining("CLOSED")
            .extracting(ex -> ((BadRequestAlertException) ex).getErrorKey())
            .isEqualTo("dayClosed");
    }

    @Test
    void assertOfficeOpen_whenReopened_allows() {
        DayClosure closure = new DayClosure();
        closure.setStatus(DayClosureStatus.REOPENED);
        when(dayClosureRepository.findFirstByOffice_IdAndBusinessDateOrderByIdDesc(5L, day)).thenReturn(Optional.of(closure));

        assertThatCode(() -> guard.assertOfficeOpen(office, day)).doesNotThrowAnyException();
    }

    @Test
    void assertOfficeOpen_whenNoClosure_allows() {
        when(dayClosureRepository.findFirstByOffice_IdAndBusinessDateOrderByIdDesc(5L, day)).thenReturn(Optional.empty());

        assertThatCode(() -> guard.assertOfficeOpen(office, day)).doesNotThrowAnyException();
    }

    @Test
    void assertOrderMutable_usesFromOffice() {
        ShipmentOrder order = new ShipmentOrder();
        order.setFromOffice(office);
        DayClosure closure = new DayClosure();
        closure.setStatus(DayClosureStatus.CLOSED);
        when(dayClosureRepository.findFirstByOffice_IdAndBusinessDateOrderByIdDesc(5L, guard.today())).thenReturn(Optional.of(closure));

        assertThatThrownBy(() -> guard.assertOrderMutable(order)).isInstanceOf(BadRequestAlertException.class);
    }

    @Test
    void assertCollectionMutable_blocksWhenToOfficeClosed_evenIfFromOpen() {
        Office dest = new Office();
        dest.setId(9L);
        dest.setCode("NB");
        ShipmentOrder order = new ShipmentOrder();
        order.setFromOffice(office);
        order.setToOffice(dest);

        when(dayClosureRepository.findFirstByOffice_IdAndBusinessDateOrderByIdDesc(5L, guard.today())).thenReturn(Optional.empty());
        DayClosure destClosed = new DayClosure();
        destClosed.setStatus(DayClosureStatus.CLOSED);
        when(dayClosureRepository.findFirstByOffice_IdAndBusinessDateOrderByIdDesc(9L, guard.today())).thenReturn(Optional.of(destClosed));

        assertThatCode(() -> guard.assertOrderMutable(order)).doesNotThrowAnyException();
        assertThatThrownBy(() -> guard.assertCollectionMutable(order))
            .isInstanceOf(BadRequestAlertException.class)
            .extracting(ex -> ((BadRequestAlertException) ex).getErrorKey())
            .isEqualTo("dayClosed");
    }

    @Test
    void assertCollectionMutable_blocksWhenFromOfficeClosed() {
        Office dest = new Office();
        dest.setId(9L);
        dest.setCode("NB");
        ShipmentOrder order = new ShipmentOrder();
        order.setFromOffice(office);
        order.setToOffice(dest);

        DayClosure fromClosed = new DayClosure();
        fromClosed.setStatus(DayClosureStatus.CLOSED);
        when(dayClosureRepository.findFirstByOffice_IdAndBusinessDateOrderByIdDesc(5L, guard.today())).thenReturn(Optional.of(fromClosed));

        assertThatThrownBy(() -> guard.assertCollectionMutable(order))
            .isInstanceOf(BadRequestAlertException.class)
            .extracting(ex -> ((BadRequestAlertException) ex).getErrorKey())
            .isEqualTo("dayClosed");
    }
}
