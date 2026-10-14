package com.mycompany.myapp.service.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.mycompany.myapp.domain.Office;
import com.mycompany.myapp.domain.TransitHubPolicy;
import com.mycompany.myapp.repository.OfficeRepository;
import com.mycompany.myapp.repository.TransitHubPolicyRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TransitHubPolicyServiceTest {

    @Mock
    private TransitHubPolicyRepository repository;

    @Mock
    private OfficeRepository officeRepository;

    private TransitHubPolicyService service;
    private final Office nd = office(1L, "VP_ND", "VP Nam Định", "Nam Định", "ND");
    private final Office tb = office(2L, "VP_TB", "VP Thái Bình", "Thái Bình", "TB");
    private final Office pt = office(3L, "VP_PT", "VP Phú Thọ", "Phú Thọ", "PT");
    private final Office hn72 = office(4L, "VP_BC", "VP Bến xe", "72 Trần Nhân Tông, Hà Nội", "BC");
    private final Office hn31 = office(5L, "VP_HD", "VP Hà Đông", "31A Nguyễn Quốc Trị, Hà Nội", "HD");

    @BeforeEach
    void setUp() {
        service = new TransitHubPolicyService(repository, officeRepository);
        TransitHubPolicy row = new TransitHubPolicy();
        row.setGroupPoints("NB,ND,TB");
        when(repository.findAll()).thenReturn(List.of(row));
        when(officeRepository.findAll()).thenReturn(List.of(nd, tb, pt, hn72, hn31));
    }

    @Test
    void groupProvinces_useGroupHubByAddress() {
        assertThat(service.resolveHub(nd, tb)).contains(hn72);
    }

    @Test
    void otherProvincePairs_useDefaultHubByAddress() {
        assertThat(service.resolveHub(nd, pt)).contains(hn31);
        assertThat(service.resolveHub(pt, tb)).contains(hn31);
    }

    @Test
    void configuredCode_winsOverAddressHint() {
        TransitHubPolicy row = new TransitHubPolicy();
        row.setGroupPoints("NB,ND,TB");
        row.setGroupHubOfficeCode("VP_HD");
        when(repository.findAll()).thenReturn(List.of(row));
        when(officeRepository.findOneByCodeIgnoreCase("VP_HD")).thenReturn(Optional.of(hn31));

        assertThat(service.resolveHub(nd, tb)).contains(hn31);
    }

    @Test
    void hanoiEndpoint_isNotTransit() {
        assertThat(service.resolveHub(nd, hn72)).isEmpty();
        assertThat(service.resolveHub(hn31, tb)).isEmpty();
        assertThat(service.resolveHub(nd, nd)).isEmpty();
    }

    private static Office office(long id, String code, String name, String address, String point) {
        Office o = new Office();
        o.setId(id);
        o.setCode(code);
        o.setName(name);
        o.setAddress(address);
        o.setItineraryPoint(point);
        return o;
    }
}
