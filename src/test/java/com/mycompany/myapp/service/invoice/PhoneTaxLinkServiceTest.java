package com.mycompany.myapp.service.invoice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import com.mycompany.myapp.domain.PhoneTaxLink;
import com.mycompany.myapp.domain.ShipmentOrder;
import com.mycompany.myapp.domain.enumeration.PaymentTerm;
import com.mycompany.myapp.repository.PhoneTaxLinkRepository;
import com.mycompany.myapp.repository.ShipmentOrderRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PhoneTaxLinkServiceTest {

    private static final String[] TAXES = {
        "0101243150",
        "0312345673",
        "0100233488",
        "0103179782",
        "0106688883",
        "0107777776",
        "0123456787",
    };

    @Mock
    private PhoneTaxLinkRepository links;

    @Mock
    private ShipmentOrderRepository orders;

    private final List<PhoneTaxLink> store = new ArrayList<>();
    private PhoneTaxLinkService service;

    @BeforeEach
    void setUp() {
        AtomicLong ids = new AtomicLong(1);
        when(links.count()).thenReturn(0L);
        when(orders.findIssuedCompanyInvoices(any())).thenReturn(List.of());
        when(links.save(any())).thenAnswer(inv -> {
            PhoneTaxLink row = inv.getArgument(0);
            if (row.getId() == null) {
                row.setId(ids.getAndIncrement());
                store.add(row);
            } else if (!store.contains(row)) {
                store.add(row);
            }
            return row;
        });
        when(links.findById(any())).thenAnswer(inv -> store.stream().filter(r -> inv.getArgument(0).equals(r.getId())).findFirst());
        when(links.findByPhoneAndTaxCode(any(), any())).thenAnswer(inv ->
            store.stream().filter(r -> r.getPhone().equals(inv.getArgument(0)) && r.getTaxCode().equals(inv.getArgument(1))).findFirst()
        );
        when(links.countByPhone(any())).thenAnswer(inv -> store.stream().filter(r -> r.getPhone().equals(inv.getArgument(0))).count());
        when(links.findByPhoneOrderByUpdatedAtAscIdAsc(any())).thenAnswer(inv ->
            store
                .stream()
                .filter(r -> r.getPhone().equals(inv.getArgument(0)))
                .sorted(Comparator.comparing(PhoneTaxLink::getUpdatedAt).thenComparing(PhoneTaxLink::getId))
                .toList()
        );
        when(links.findByPhoneOrderByUpdatedAtDescIdDesc(any())).thenAnswer(inv ->
            store
                .stream()
                .filter(r -> r.getPhone().equals(inv.getArgument(0)))
                .sorted(Comparator.comparing(PhoneTaxLink::getUpdatedAt).thenComparing(PhoneTaxLink::getId).reversed())
                .toList()
        );
        doAnswer(inv -> {
            store.remove(inv.getArgument(0));
            return null;
        })
            .when(links)
            .delete(any());
        when(links.findByFromOrderCodeIsNotNull()).thenAnswer(inv ->
            store.stream().filter(r -> r.getFromOrderCode() != null && !r.getFromOrderCode().isBlank()).toList()
        );
        when(orders.findWithOfficesByOrderCodeIn(any())).thenReturn(List.of());
        service = new PhoneTaxLinkService(links, orders);
    }

    @Test
    void create_canonicalPhone_rejectsSixth() {
        for (int i = 0; i < 5; i++) {
            service.create("0901.234.567", TAXES[i], "Cty " + i, "Ha Noi", null, "An");
        }
        assertThat(store).extracting(PhoneTaxLink::getPhone).containsOnly("0901234567");
        assertThatThrownBy(() -> service.create("84901234567", TAXES[5], "Cty 6", "Ha Noi", null, "An"))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("tối đa 5");
        assertThat(store).hasSize(5);
    }

    @Test
    void rememberIssued_linksOnlyPayer_andDropsOldest() {
        ShipmentOrder first = issued("O1", "0901234567", "0988000111", TAXES[0], "Moi", PaymentTerm.GUI_TRA);
        service.rememberIssued(first);
        assertThat(store).extracting(PhoneTaxLink::getPhone).containsExactly("0901234567");

        ShipmentOrder receiverPays = issued("R1", "0901234567", "0988000111", TAXES[1], "Nhan", PaymentTerm.NHAN_TRA);
        service.rememberIssued(receiverPays);
        assertThat(store).filteredOn(r -> r.getTaxCode().equals(TAXES[1])).extracting(PhoneTaxLink::getPhone).containsExactly("0988000111");

        service.rememberIssued(issued("C1", "0911000000", "0922000000", TAXES[2], "Cod", PaymentTerm.COD));
        assertThat(store)
            .filteredOn(r -> "C1".equals(r.getFromOrderCode()))
            .extracting(PhoneTaxLink::getPhone)
            .containsExactly("0922000000");

        for (int i = 2; i <= 6; i++) {
            service.rememberIssued(issued("O" + (i + 1), "0901234567", "091100000" + i, TAXES[i], "Cty " + i, PaymentTerm.GUI_TRA));
        }

        List<String> senderTaxes = store
            .stream()
            .filter(r -> r.getPhone().equals("0901234567"))
            .sorted(Comparator.comparing(PhoneTaxLink::getUpdatedAt).reversed())
            .map(PhoneTaxLink::getTaxCode)
            .toList();
        assertThat(senderTaxes).hasSize(5).doesNotContain(TAXES[0]).first().isEqualTo(TAXES[6]);
    }

    @Test
    void audit_dropsTaxLinkedToNonPayer() {
        ShipmentOrder senderPays = issued("O1", "0901234567", "0988000111", TAXES[0], "Gui", PaymentTerm.GUI_TRA);
        store.add(link("0901234567", TAXES[0], "O1"));
        store.add(link("0988000111", TAXES[0], "O1"));
        when(orders.findWithOfficesByOrderCodeIn(any())).thenReturn(List.of(senderPays));

        assertThat(service.profiles("0901234567")).extracting(m -> m.get("taxCode")).containsExactly(TAXES[0]);
        assertThat(store).extracting(PhoneTaxLink::getPhone).containsExactly("0901234567");
    }

    @Test
    void update_bumpsRecency_andProfilesNewestFirst() {
        service.create("0901234567", TAXES[0], "Cu", "A", null, "An");
        service.create("0901234567", TAXES[1], "Moi", "B", "a@b.c", "An");
        Long olderId = store.stream().filter(r -> r.getTaxCode().equals(TAXES[0])).findFirst().orElseThrow().getId();

        service.update(olderId, TAXES[2], "Sua", "C", null, "Binh");

        assertThat(service.profiles("0901 234 567")).extracting(m -> m.get("taxCode")).containsExactly(TAXES[2], TAXES[1]);
        assertThat(service.profiles("09")).isEmpty();
    }

    private static ShipmentOrder issued(String code, String sender, String receiver, String tax, String company, PaymentTerm term) {
        ShipmentOrder o = new ShipmentOrder();
        o.setOrderCode(code);
        o.setPaymentTerm(term);
        o.setSenderPhone(sender);
        o.setReceiverPhone(receiver);
        o.setSenderName("AN");
        o.setReceiverName("BINH");
        o.setInvoiceTaxCode(tax);
        o.setInvoiceCompanyName(company);
        o.setInvoiceCompanyAddress("Dia chi");
        o.setInvoiceStatus(MeInvoiceIssueService.STATUS_ISSUED);
        o.setInvoiceIssuedAt(Instant.parse("2026-10-02T02:00:00Z"));
        return o;
    }

    private static PhoneTaxLink link(String phone, String tax, String orderCode) {
        PhoneTaxLink row = new PhoneTaxLink();
        row.setId(phone.equals("0901234567") ? 1L : 2L);
        row.setPhone(phone);
        row.setTaxCode(tax);
        row.setCompanyName("Cty");
        row.setFromOrderCode(orderCode);
        row.setUpdatedAt(Instant.parse("2026-10-02T02:00:00Z"));
        return row;
    }
}
