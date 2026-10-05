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
import com.mycompany.myapp.domain.InventoryCheckPhoto;
import com.mycompany.myapp.domain.StaffProfile;
import com.mycompany.myapp.repository.InventoryCheckPhotoRepository;
import com.mycompany.myapp.repository.InventoryCheckRepository;
import com.mycompany.myapp.repository.OfficeRepository;
import com.mycompany.myapp.repository.StaffProfileRepository;
import com.mycompany.myapp.security.StaffAccessService;
import com.mycompany.myapp.service.dto.inventory.CreateInventoryCheckRequest;
import com.mycompany.myapp.service.inventory.InventoryCheckService.UploadPhotoRequest;
import com.mycompany.myapp.web.rest.errors.BadRequestAlertException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class InventoryCheckServicePhotoTest {

    private static final String SESSION = "inv-1a2b3c4d5e6f";
    private static final String IMG = "data:image/jpeg;base64,AAAA";

    @Mock
    private InventoryCheckRepository checkRepository;

    @Mock
    private InventoryCheckPhotoRepository photoRepository;

    @Mock
    private OfficeRepository officeRepository;

    @Mock
    private StaffAccessService staffAccessService;

    @Mock
    private StaffProfileRepository staffProfileRepository;

    private InventoryCheckService service;

    @BeforeEach
    void setUp() {
        service = new InventoryCheckService(
            checkRepository,
            photoRepository,
            officeRepository,
            staffAccessService,
            new ObjectMapper(),
            staffProfileRepository
        );
        lenient().when(staffAccessService.scopedOfficeCode()).thenReturn(Optional.of("VP_HD"));
    }

    @Test
    void upload_savesPhotoUnderStaffOffice() {
        service.uploadPhoto(new UploadPhotoRequest(SESSION, "VP_OTHER", " hd041 ", 2, IMG, "2026-10-04T10:00:00Z"));

        ArgumentCaptor<InventoryCheckPhoto> cap = ArgumentCaptor.forClass(InventoryCheckPhoto.class);
        verify(photoRepository).save(cap.capture());
        InventoryCheckPhoto p = cap.getValue();
        assertThat(p.getSessionKey()).isEqualTo(SESSION);
        assertThat(p.getOfficeCode()).isEqualTo("VP_HD");
        assertThat(p.getOrderCode()).isEqualTo("HD041");
        assertThat(p.getPackageSeq()).isEqualTo(2);
        assertThat(p.getPhotoUrl()).isEqualTo(IMG);
        assertThat(p.getCapturedAt()).hasToString("2026-10-04T10:00:00Z");
    }

    @Test
    void upload_rejectsNonImage() {
        assertThatThrownBy(() -> service.uploadPhoto(new UploadPhotoRequest(SESSION, null, "HD1", 1, "data:text/html;base64,AA", null)))
            .isInstanceOf(BadRequestAlertException.class)
            .extracting(e -> ((BadRequestAlertException) e).getErrorKey())
            .isEqualTo("photoInvalid");
        verify(photoRepository, never()).save(any());
    }

    @Test
    void upload_rejectsTooLarge() {
        String big = "data:image/jpeg;base64," + "A".repeat(InventoryCheckService.MAX_PHOTO_LENGTH);
        assertThatThrownBy(() -> service.uploadPhoto(new UploadPhotoRequest(SESSION, null, "HD1", 1, big, null)))
            .isInstanceOf(BadRequestAlertException.class)
            .extracting(e -> ((BadRequestAlertException) e).getErrorKey())
            .isEqualTo("photoTooLarge");
    }

    @Test
    void upload_rejectsBadSessionAndSeq() {
        assertThatThrownBy(() -> service.uploadPhoto(new UploadPhotoRequest("x;drop", null, "HD1", 1, IMG, null)))
            .isInstanceOf(BadRequestAlertException.class)
            .extracting(e -> ((BadRequestAlertException) e).getErrorKey())
            .isEqualTo("sessionKeyInvalid");
        assertThatThrownBy(() -> service.uploadPhoto(new UploadPhotoRequest(SESSION, null, "HD1", 0, IMG, null)))
            .isInstanceOf(BadRequestAlertException.class)
            .extracting(e -> ((BadRequestAlertException) e).getErrorKey())
            .isEqualTo("packageSeqInvalid");
        verify(photoRepository, never()).save(any());
    }

    @Test
    void create_storesSessionKey() {
        when(officeRepository.findOneByCodeIgnoreCase("VP_HD")).thenReturn(Optional.of(new com.mycompany.myapp.domain.Office()));
        when(checkRepository.save(any(InventoryCheck.class))).thenAnswer(inv -> inv.getArgument(0));
        CreateInventoryCheckRequest req = new CreateInventoryCheckRequest();
        req.setOfficeCode("VP_HD");
        req.setSystemCodes(List.of("HD1", "HD2"));
        req.setScannedCodes(List.of("HD1"));
        req.setSessionKey(SESSION);

        service.create(req);

        ArgumentCaptor<InventoryCheck> cap = ArgumentCaptor.forClass(InventoryCheck.class);
        verify(checkRepository).save(cap.capture());
        assertThat(cap.getValue().getSessionKey()).isEqualTo(SESSION);
        assertThat(cap.getValue().getMissingCount()).isEqualTo(1);
    }

    @Test
    void create_withoutSessionKey_unchanged() {
        when(officeRepository.findOneByCodeIgnoreCase("VP_HD")).thenReturn(Optional.of(new com.mycompany.myapp.domain.Office()));
        when(checkRepository.save(any(InventoryCheck.class))).thenAnswer(inv -> inv.getArgument(0));
        CreateInventoryCheckRequest req = new CreateInventoryCheckRequest();
        req.setOfficeCode("VP_HD");

        service.create(req);

        ArgumentCaptor<InventoryCheck> cap = ArgumentCaptor.forClass(InventoryCheck.class);
        verify(checkRepository).save(cap.capture());
        assertThat(cap.getValue().getSessionKey()).isNull();
    }

    @Test
    void photos_scopedToCheckSessionAndOffice() {
        InventoryCheck check = new InventoryCheck();
        check.setOfficeCode("VP_HD");
        check.setSessionKey(SESSION);
        when(checkRepository.findById(5L)).thenReturn(Optional.of(check));
        InventoryCheckPhoto p = new InventoryCheckPhoto();
        p.setPackageSeq(1);
        p.setPhotoUrl(IMG);
        when(photoRepository.findBySessionKeyAndOrderCodeIgnoreCaseOrderByPackageSeqAscCapturedAtAsc(SESSION, "HD1")).thenReturn(
            List.of(p)
        );
        Instant t1 = Instant.parse("2026-09-29T13:43:00Z");
        Instant t2 = Instant.parse("2026-09-29T13:44:25Z");
        when(photoRepository.scanMetaBySession(SESSION)).thenReturn(
            List.<Object[]>of(new Object[] { "hd1", t2, "ngoc" }, new Object[] { "HD1", t1, "nam" }, new Object[] { "HD2", t1, "nam" })
        );
        StaffProfile ngoc = new StaffProfile();
        ngoc.setStaffCode("1805235");
        ngoc.setDisplayName("Nguyễn Thị Ngọc");
        when(staffProfileRepository.findOneByUserLoginIgnoreCase("ngoc")).thenReturn(Optional.of(ngoc));
        when(staffProfileRepository.findOneByUserLoginIgnoreCase("nam")).thenReturn(Optional.empty());

        assertThat(service.photos(5L, "HD1")).singleElement().satisfies(d -> assertThat(d.photo()).isEqualTo(IMG));
        assertThat(service.photoOrders(5L)).containsExactly(
            new InventoryCheckService.PhotoOrderCount("HD1", 2L, t2, "ngoc", "1805235", "Nguyễn Thị Ngọc"),
            new InventoryCheckService.PhotoOrderCount("HD2", 1L, t1, "nam", null, null)
        );
    }

    @Test
    void thumbnails_firstPhotoPerOrder() {
        InventoryCheck check = new InventoryCheck();
        check.setOfficeCode("VP_HD");
        check.setSessionKey(SESSION);
        when(checkRepository.findById(5L)).thenReturn(Optional.of(check));
        InventoryCheckPhoto p = new InventoryCheckPhoto();
        p.setOrderCode("hd1");
        p.setPhotoUrl(IMG);
        when(photoRepository.firstPhotoIds(SESSION, List.of("HD1", "HD2"))).thenReturn(List.of(11L));
        when(photoRepository.findAllById(List.of(11L))).thenReturn(List.of(p));

        assertThat(service.thumbnails(5L, List.of(" hd1", "HD2", "hd1", ""))).containsExactly(
            new InventoryCheckService.ThumbnailDTO("HD1", IMG)
        );
    }

    @Test
    void photos_otherOfficeDenied() {
        InventoryCheck check = new InventoryCheck();
        check.setOfficeCode("VP_TDN");
        check.setSessionKey(SESSION);
        when(checkRepository.findById(6L)).thenReturn(Optional.of(check));

        assertThatThrownBy(() -> service.photos(6L, "HD1")).isInstanceOf(ResponseStatusException.class);
        verify(photoRepository, never()).findBySessionKeyAndOrderCodeIgnoreCaseOrderByPackageSeqAscCapturedAtAsc(any(), any());
    }

    @Test
    void legacyCheckWithoutSession_hasNoPhotos() {
        InventoryCheck check = new InventoryCheck();
        check.setOfficeCode("VP_HD");
        when(checkRepository.findById(7L)).thenReturn(Optional.of(check));

        assertThat(service.photoOrders(7L)).isEmpty();
        assertThat(service.photos(7L, "HD1")).isEmpty();
    }
}
