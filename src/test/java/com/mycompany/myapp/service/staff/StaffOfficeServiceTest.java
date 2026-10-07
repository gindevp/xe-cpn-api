package com.mycompany.myapp.service.staff;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import com.mycompany.myapp.domain.Office;
import com.mycompany.myapp.domain.StaffOfficeAccess;
import com.mycompany.myapp.domain.StaffProfile;
import com.mycompany.myapp.repository.OfficeRepository;
import com.mycompany.myapp.repository.StaffOfficeAccessRepository;
import com.mycompany.myapp.repository.StaffProfileRepository;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StaffOfficeServiceTest {

    @Mock
    private StaffOfficeAccessRepository accessRepository;

    @Mock
    private StaffProfileRepository staffProfileRepository;

    @Mock
    private OfficeRepository officeRepository;

    private final List<StaffOfficeAccess> rows = new ArrayList<>();
    private final Map<Long, Office> offices = Map.of(
        1L,
        office(1L, "VP_A", "VP A"),
        2L,
        office(2L, "VP_B", "VP B"),
        3L,
        office(3L, "VP_C", "VP C")
    );
    private StaffProfile profile;
    private StaffOfficeService service;

    @BeforeEach
    void setUp() {
        profile = new StaffProfile();
        profile.setId(10L);
        profile.setUserLogin("nv1");
        profile.setScopeAllOffices(false);
        profile.setOffice(offices.get(1L));

        when(staffProfileRepository.findOneByUserLoginIgnoreCase("nv1")).thenReturn(Optional.of(profile));
        when(staffProfileRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(officeRepository.findById(anyLong())).thenAnswer(inv -> Optional.ofNullable(offices.get((Long) inv.getArgument(0))));
        when(officeRepository.findAllById(anyCollection())).thenAnswer(inv -> {
            Collection<Long> ids = inv.getArgument(0);
            return ids.stream().map(offices::get).filter(Objects::nonNull).toList();
        });
        when(accessRepository.findByStaffProfileId(10L)).thenAnswer(inv -> new ArrayList<>(rows));
        when(accessRepository.existsByStaffProfileIdAndOfficeId(any(), any())).thenAnswer(inv ->
            rows.stream().anyMatch(r -> r.getStaffProfileId().equals(inv.getArgument(0)) && r.getOfficeId().equals(inv.getArgument(1)))
        );
        when(accessRepository.save(any())).thenAnswer(inv -> {
            rows.add(inv.getArgument(0));
            return inv.getArgument(0);
        });
        doAnswer(inv -> rows.remove((StaffOfficeAccess) inv.getArgument(0))).when(accessRepository).delete(any());
        doAnswer(inv -> rows.removeIf(r -> r.getStaffProfileId().equals(inv.getArgument(0))))
            .when(accessRepository)
            .deleteByStaffProfileId(any());

        service = new StaffOfficeService(accessRepository, staffProfileRepository, officeRepository);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("nv1", "x"));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void allowedOffices_alwaysIncludeActiveOffice() {
        service.replaceAllowed(profile, List.of(1L, 2L));

        assertThat(rows).extracting(StaffOfficeAccess::getOfficeId).containsExactly(2L);
        assertThat(service.allowedOffices(profile)).extracting(StaffOfficeService.OfficeOption::code).containsExactly("VP_A", "VP_B");
    }

    @Test
    void switchActive_movesActiveOfficeAndKeepsWayBack() {
        service.replaceAllowed(profile, List.of(2L));

        service.switchActive(2L);

        assertThat(profile.getOffice().getCode()).isEqualTo("VP_B");
        assertThat(rows).extracting(StaffOfficeAccess::getOfficeId).containsExactly(1L);

        service.switchActive(1L);
        assertThat(profile.getOffice().getCode()).isEqualTo("VP_A");
        assertThat(rows).extracting(StaffOfficeAccess::getOfficeId).containsExactly(2L);
    }

    @Test
    void switchActive_rejectsOfficeNotAssigned() {
        service.replaceAllowed(profile, List.of(2L));

        assertThatThrownBy(() -> service.switchActive(3L)).isInstanceOf(ResponseStatusException.class);
        assertThat(profile.getOffice().getCode()).isEqualTo("VP_A");
    }

    @Test
    void replaceAllowed_nullKeepsExistingList() {
        service.replaceAllowed(profile, List.of(2L, 3L));

        service.replaceAllowed(profile, null);

        assertThat(rows).hasSize(2);
    }

    private static Office office(Long id, String code, String name) {
        Office o = new Office();
        o.setId(id);
        o.setCode(code);
        o.setName(name);
        return o;
    }
}
