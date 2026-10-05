package com.mycompany.myapp.service.inventory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.myapp.domain.InventoryCheck;
import com.mycompany.myapp.domain.InventoryCheckScan;
import com.mycompany.myapp.domain.InventoryCheckStatus;
import com.mycompany.myapp.repository.InventoryCheckPhotoRepository;
import com.mycompany.myapp.repository.InventoryCheckRepository;
import com.mycompany.myapp.repository.InventoryCheckScanRepository;
import com.mycompany.myapp.repository.OfficeRepository;
import com.mycompany.myapp.repository.StaffProfileRepository;
import com.mycompany.myapp.security.StaffAccessService;
import com.mycompany.myapp.service.dto.inventory.InventoryCheckDTO;
import com.mycompany.myapp.service.inventory.InventoryCheckSessionService.CompleteRequest;
import com.mycompany.myapp.service.inventory.InventoryCheckSessionService.ExpectedOrder;
import com.mycompany.myapp.service.inventory.InventoryCheckSessionService.ScanRequest;
import com.mycompany.myapp.web.rest.errors.BadRequestAlertException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class InventoryCheckSessionServiceTest {

    @Mock
    private InventoryCheckRepository checkRepository;

    @Mock
    private InventoryCheckScanRepository scanRepository;

    @Mock
    private InventoryCheckPhotoRepository photoRepository;

    @Mock
    private OfficeRepository officeRepository;

    @Mock
    private StaffAccessService staffAccessService;

    @Mock
    private StaffProfileRepository staffProfileRepository;

    private InventoryCheckSessionService service;

    @BeforeEach
    void setUp() {
        ObjectMapper mapper = new ObjectMapper();
        InventoryCheckService checkService = new InventoryCheckService(
            checkRepository,
            photoRepository,
            officeRepository,
            staffAccessService,
            mapper,
            staffProfileRepository,
            scanRepository
        );
        service = new InventoryCheckSessionService(
            checkRepository,
            scanRepository,
            checkService,
            staffAccessService,
            staffProfileRepository,
            mapper
        );
        lenient().when(staffAccessService.scopedOfficeCode()).thenReturn(Optional.of("VP_HD"));
        lenient().when(checkRepository.saveAndFlush(any(InventoryCheck.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(checkRepository.save(any(InventoryCheck.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private static InventoryCheck open(long id, Instant openedAt) {
        InventoryCheck c = new InventoryCheck();
        c.setId(id);
        c.setOfficeCode("VP_HD");
        c.setStatus(InventoryCheckStatus.OPEN);
        c.setOpenOfficeCode("VP_HD");
        c.setOpenedAt(openedAt);
        c.setOpenedByUsername("nam");
        c.setCheckedAt(openedAt);
        c.setSessionKey("inv-shared-0001");
        return c;
    }

    private static InventoryCheckScan scan(long id, String code, int seq, String by) {
        InventoryCheckScan s = new InventoryCheckScan();
        s.setId(id);
        s.setCheckId(1L);
        s.setOrderCode(code);
        s.setPackageSeq(seq);
        s.setScannedAt(Instant.now());
        s.setScannedByUsername(by);
        return s;
    }

    @Test
    void openOrJoin_joinsExistingOpenSession() {
        when(checkRepository.findOneByOpenOfficeCode("VP_HD")).thenReturn(Optional.of(open(1L, Instant.now())));

        InventoryCheckDTO dto = service.openOrJoin("VP_OTHER");

        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getStatus()).isEqualTo(InventoryCheckStatus.OPEN);
        assertThat(dto.getSessionKey()).isEqualTo("inv-shared-0001");
        verify(checkRepository, never()).saveAndFlush(any());
    }

    @Test
    void openOrJoin_createsOpenSessionForStaffOffice() {
        when(checkRepository.findOneByOpenOfficeCode("VP_HD")).thenReturn(Optional.empty());

        InventoryCheckDTO dto = service.openOrJoin(null);

        ArgumentCaptor<InventoryCheck> cap = ArgumentCaptor.forClass(InventoryCheck.class);
        verify(checkRepository).saveAndFlush(cap.capture());
        InventoryCheck c = cap.getValue();
        assertThat(c.getStatus()).isEqualTo(InventoryCheckStatus.OPEN);
        assertThat(c.getOfficeCode()).isEqualTo("VP_HD");
        assertThat(c.getOpenOfficeCode()).isEqualTo("VP_HD");
        assertThat(c.getSessionKey()).startsWith("inv-");
        assertThat(InventoryCheckService.validSessionKeyOrNull(c.getSessionKey())).isNotNull();
        assertThat(dto.getStatus()).isEqualTo(InventoryCheckStatus.OPEN);
    }

    @Test
    void openOrJoin_yesterdaysOpenIsAbandonedAndNewOneOpened() {
        InventoryCheck stale = open(1L, Instant.now().minus(Duration.ofDays(1)));
        when(checkRepository.findOneByOpenOfficeCode("VP_HD")).thenReturn(Optional.of(stale));

        service.openOrJoin(null);

        assertThat(stale.getStatus()).isEqualTo(InventoryCheckStatus.ABANDONED);
        assertThat(stale.getOpenOfficeCode()).isNull();
        ArgumentCaptor<InventoryCheck> cap = ArgumentCaptor.forClass(InventoryCheck.class);
        verify(checkRepository, org.mockito.Mockito.times(2)).saveAndFlush(cap.capture());
        assertThat(cap.getAllValues().get(1).getStatus()).isEqualTo(InventoryCheckStatus.OPEN);
    }

    @Test
    void addScan_savesNewPackage() {
        when(checkRepository.findForShare(1L)).thenReturn(Optional.of(open(1L, Instant.now())));
        when(scanRepository.saveAndFlush(any(InventoryCheckScan.class))).thenAnswer(inv -> {
            InventoryCheckScan s = inv.getArgument(0);
            s.setId(10L);
            return s;
        });

        InventoryCheckSessionService.ScanResult r = service.addScan(1L, new ScanRequest(" hd1 ", 2, null));

        assertThat(r.duplicate()).isFalse();
        assertThat(r.scan().orderCode()).isEqualTo("HD1");
        assertThat(r.scan().packageSeq()).isEqualTo(2);
        assertThat(r.scan().id()).isEqualTo(10L);
    }

    @Test
    void addScan_packageAlreadyScannedByColleague_returnsTheirScan() {
        when(checkRepository.findForShare(1L)).thenReturn(Optional.of(open(1L, Instant.now())));
        when(scanRepository.findOneByCheckIdAndOrderCodeAndPackageSeq(1L, "HD1", 1)).thenReturn(Optional.of(scan(7L, "HD1", 1, "ngoc")));

        InventoryCheckSessionService.ScanResult r = service.addScan(1L, new ScanRequest("HD1", 1, null));

        assertThat(r.duplicate()).isTrue();
        assertThat(r.scan().scannedBy()).isEqualTo("ngoc");
        verify(scanRepository, never()).saveAndFlush(any());
    }

    @Test
    void addScan_closedSessionRejected() {
        InventoryCheck done = open(1L, Instant.now());
        done.setStatus(InventoryCheckStatus.COMPLETED);
        done.setOpenOfficeCode(null);
        when(checkRepository.findForShare(1L)).thenReturn(Optional.of(done));

        assertThatThrownBy(() -> service.addScan(1L, new ScanRequest("HD1", 1, null)))
            .isInstanceOf(ResponseStatusException.class)
            .extracting(e -> ((ResponseStatusException) e).getStatusCode())
            .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void addScan_otherOfficeDenied() {
        InventoryCheck other = open(1L, Instant.now());
        other.setOfficeCode("VP_TDN");
        when(checkRepository.findForShare(1L)).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> service.addScan(1L, new ScanRequest("HD1", 1, null)))
            .isInstanceOf(ResponseStatusException.class)
            .extracting(e -> ((ResponseStatusException) e).getStatusCode())
            .isEqualTo(HttpStatus.FORBIDDEN);
        verify(scanRepository, never()).saveAndFlush(any());
    }

    @Test
    void complete_talliesEveryonesScansAgainstLatestStock() {
        InventoryCheck c = open(1L, Instant.now());
        when(checkRepository.findForUpdate(1L)).thenReturn(Optional.of(c));
        when(scanRepository.findByCheckIdOrderByIdAsc(1L)).thenReturn(
            List.of(scan(1, "HD1", 1, "nam"), scan(2, "HD1", 2, "ngoc"), scan(3, "HD3", 1, "ngoc"))
        );

        InventoryCheckDTO dto = service.complete(
            1L,
            new CompleteRequest(List.of(new ExpectedOrder("hd1", List.of(1, 2)), new ExpectedOrder("HD2", List.of(1))))
        );

        assertThat(c.getStatus()).isEqualTo(InventoryCheckStatus.COMPLETED);
        assertThat(c.getOpenOfficeCode()).isNull();
        assertThat(c.getSystemPkgCount()).isEqualTo(3);
        assertThat(c.getCheckedPkgCount()).isEqualTo(2);
        assertThat(c.getMissingPkgCount()).isEqualTo(1);
        assertThat(c.getExtraPkgCount()).isEqualTo(1);
        assertThat(c.getSystemCount()).isEqualTo(2);
        assertThat(c.getCheckedCount()).isEqualTo(2);
        assertThat(c.getMissingCount()).isEqualTo(1);
        assertThat(dto.getMissingCodes()).containsExactly("HD2");
        assertThat(dto.getScannedCodes()).containsExactly("HD1", "HD3");
    }

    @Test
    void complete_alreadyCompletedByColleague_conflict() {
        InventoryCheck c = open(1L, Instant.now());
        c.setStatus(InventoryCheckStatus.COMPLETED);
        when(checkRepository.findForUpdate(1L)).thenReturn(Optional.of(c));

        assertThatThrownBy(() -> service.complete(1L, new CompleteRequest(List.of())))
            .isInstanceOf(ResponseStatusException.class)
            .extracting(e -> ((ResponseStatusException) e).getStatusCode())
            .isEqualTo(HttpStatus.CONFLICT);
    }

    private static InventoryCheck completed(long id, Instant checkedAt) {
        InventoryCheck c = open(id, checkedAt);
        c.setStatus(InventoryCheckStatus.COMPLETED);
        c.setOpenOfficeCode(null);
        return c;
    }

    @Test
    void reopen_latestCompletedToday() {
        InventoryCheck c = completed(1L, Instant.now());
        when(checkRepository.findForUpdate(1L)).thenReturn(Optional.of(c));
        when(
            checkRepository.findFirstByOfficeCodeIgnoreCaseAndStatusOrderByCheckedAtDesc("VP_HD", InventoryCheckStatus.COMPLETED)
        ).thenReturn(Optional.of(c));
        when(checkRepository.findOneByOpenOfficeCode("VP_HD")).thenReturn(Optional.empty());

        service.reopen(1L);

        assertThat(c.getStatus()).isEqualTo(InventoryCheckStatus.OPEN);
        assertThat(c.getOpenOfficeCode()).isEqualTo("VP_HD");
        assertThat(c.getReopenedAt()).isNotNull();
    }

    @Test
    void reopen_rejectedWhenNotLatestOrNotTodayOrOfficeBusyOrLegacy() {
        InventoryCheck older = completed(1L, Instant.now());
        when(checkRepository.findForUpdate(1L)).thenReturn(Optional.of(older));
        when(
            checkRepository.findFirstByOfficeCodeIgnoreCaseAndStatusOrderByCheckedAtDesc("VP_HD", InventoryCheckStatus.COMPLETED)
        ).thenReturn(Optional.of(completed(2L, Instant.now())));
        assertThatThrownBy(() -> service.reopen(1L)).hasFieldOrPropertyWithValue("errorKey", "reopenNotLatest");

        InventoryCheck yesterday = completed(3L, Instant.now().minus(Duration.ofDays(1)));
        when(checkRepository.findForUpdate(3L)).thenReturn(Optional.of(yesterday));
        when(
            checkRepository.findFirstByOfficeCodeIgnoreCaseAndStatusOrderByCheckedAtDesc("VP_HD", InventoryCheckStatus.COMPLETED)
        ).thenReturn(Optional.of(yesterday));
        assertThatThrownBy(() -> service.reopen(3L)).hasFieldOrPropertyWithValue("errorKey", "reopenNotToday");

        InventoryCheck today = completed(4L, Instant.now());
        when(checkRepository.findForUpdate(4L)).thenReturn(Optional.of(today));
        when(
            checkRepository.findFirstByOfficeCodeIgnoreCaseAndStatusOrderByCheckedAtDesc("VP_HD", InventoryCheckStatus.COMPLETED)
        ).thenReturn(Optional.of(today));
        when(checkRepository.findOneByOpenOfficeCode("VP_HD")).thenReturn(Optional.of(open(9L, Instant.now())));
        assertThatThrownBy(() -> service.reopen(4L)).hasFieldOrPropertyWithValue("errorKey", "officeHasOpen");

        InventoryCheck legacy = completed(5L, Instant.now());
        legacy.setOpenedAt(null);
        when(checkRepository.findForUpdate(5L)).thenReturn(Optional.of(legacy));
        assertThatThrownBy(() -> service.reopen(5L))
            .isInstanceOf(BadRequestAlertException.class)
            .hasFieldOrPropertyWithValue("errorKey", "reopenNotAllowed");
    }

    @Test
    void endOfDay_openBecomesAbandoned_reopenedRevertsToCompleted() {
        InventoryCheck fresh = open(1L, Instant.now());
        InventoryCheck reopened = completed(2L, Instant.now());
        reopened.setStatus(InventoryCheckStatus.OPEN);
        reopened.setOpenOfficeCode("VP_X");
        reopened.setReopenedAt(Instant.now());
        when(checkRepository.findByStatus(InventoryCheckStatus.OPEN)).thenReturn(List.of(fresh, reopened));

        assertThat(service.closeAllOpen()).isEqualTo(2);

        assertThat(fresh.getStatus()).isEqualTo(InventoryCheckStatus.ABANDONED);
        assertThat(fresh.getOpenOfficeCode()).isNull();
        assertThat(reopened.getStatus()).isEqualTo(InventoryCheckStatus.COMPLETED);
        assertThat(reopened.getOpenOfficeCode()).isNull();
    }

    @Test
    void expireIfStale_usesVietnamDay() {
        // 16:30Z = 23:30 VN; 17:30Z = 00:30 VN hôm sau
        InventoryCheck c = open(1L, Instant.parse("2026-10-05T16:30:00Z"));
        assertThat(service.expireIfStale(c, Instant.parse("2026-10-05T16:50:00Z"))).isFalse();
        assertThat(service.expireIfStale(c, Instant.parse("2026-10-05T17:30:00Z"))).isTrue();
        assertThat(c.getStatus()).isEqualTo(InventoryCheckStatus.ABANDONED);
    }
}
