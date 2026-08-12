package com.mycompany.myapp.service.day;

import com.mycompany.myapp.domain.Office;
import com.mycompany.myapp.domain.ShipmentOrder;
import com.mycompany.myapp.domain.enumeration.DayClosureStatus;
import com.mycompany.myapp.repository.DayClosureRepository;
import com.mycompany.myapp.repository.OfficeRepository;
import com.mycompany.myapp.web.rest.errors.BadRequestAlertException;
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Shared domain guard: when an office day is CLOSED, selected mutations are rejected.
 * Business date uses Asia/Ho_Chi_Minh (same as existing payment/receipt gates).
 *
 * <p>Two scopes (M2):
 * <ul>
 *   <li>{@link #assertOrderMutable} — origin ops (PATCH, transition, legs, …): {@code fromOffice} only.
 *       Dest closed must not freeze warehouse work at origin.
 *   <li>{@link #assertCollectionMutable} — money/POD/receipt lines: {@code fromOffice} OR {@code toOffice}.
 *       Aligns with FE báo-cáo-thu closing the session office that may be dest.
 * </ul>
 */
@Service
@Transactional(readOnly = true)
public class DayClosureGuard {

    public static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final String ENTITY = "dayClosure";

    private final DayClosureRepository dayClosureRepository;
    private final OfficeRepository officeRepository;

    public DayClosureGuard(DayClosureRepository dayClosureRepository, OfficeRepository officeRepository) {
        this.dayClosureRepository = dayClosureRepository;
        this.officeRepository = officeRepository;
    }

    public LocalDate today() {
        return LocalDate.now(VN);
    }

    public boolean isOfficeClosed(Office office, LocalDate businessDate) {
        if (office == null || office.getId() == null || businessDate == null) {
            return false;
        }
        return dayClosureRepository
            .findFirstByOffice_IdAndBusinessDateOrderByIdDesc(office.getId(), businessDate)
            .filter(c -> c.getStatus() == DayClosureStatus.CLOSED)
            .isPresent();
    }

    public boolean isOfficeClosedToday(Office office) {
        return isOfficeClosed(office, today());
    }

    public void assertOfficeOpen(Office office) {
        assertOfficeOpen(office, today());
    }

    public void assertOfficeOpen(Office office, LocalDate businessDate) {
        if (office == null) {
            return;
        }
        if (isOfficeClosed(office, businessDate)) {
            throw closed(office.getCode());
        }
    }

    public void assertOfficeOpenByCode(String officeCode) {
        if (officeCode == null || officeCode.isBlank()) {
            return;
        }
        Office office = officeRepository.findOneByCode(officeCode.trim().toUpperCase()).orElse(null);
        assertOfficeOpen(office);
    }

    /**
     * Origin ops: gated by {@code fromOffice} + today.
     * Must not weaken C1/C2/H2 for origin closed.
     */
    public void assertOrderMutable(ShipmentOrder order) {
        if (order == null) {
            return;
        }
        assertOfficeOpen(order.getFromOffice());
    }

    /**
     * Collection / POD / receipt-on-order: blocked if fromOffice OR toOffice is CLOSED today.
     */
    public void assertCollectionMutable(ShipmentOrder order) {
        if (order == null) {
            return;
        }
        assertOfficeOpen(order.getFromOffice());
        assertOfficeOpen(order.getToOffice());
    }

    private static BadRequestAlertException closed(String officeCode) {
        String code = officeCode == null ? "?" : officeCode;
        return new BadRequestAlertException("Day is CLOSED for office " + code, ENTITY, "dayClosed");
    }
}
