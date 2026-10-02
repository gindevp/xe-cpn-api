package com.mycompany.myapp.service.invoice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.myapp.service.realtime.ServerEventService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.StaticListableBeanFactory;

class TaxCodeLookupServiceTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String NOWHERE = "http://127.0.0.1:9";
    private static final ObjectProvider<ServerEventService> NO_EVENTS = new StaticListableBeanFactory()
        .getBeanProvider(ServerEventService.class);

    private static ObjectProvider<ServerEventService> eventsOf(ServerEventService events) {
        StaticListableBeanFactory beans = new StaticListableBeanFactory();
        beans.addBean("serverEventService", events);
        return beans.getBeanProvider(ServerEventService.class);
    }

    @Test
    void lookup_upstreamFailure_alertsAdminsOncePerCooldown() {
        ServerEventService events = mock(ServerEventService.class);
        TaxCodeLookupService service = new TaxCodeLookupService(JSON, eventsOf(events), NOWHERE, "", "", NOWHERE, NOWHERE, false) {
            @Override
            Map<String, Object> fetch(String taxCode) {
                return Map.of("ok", false, "code", "UPSTREAM_ERROR", "message", "lỗi");
            }
        };
        service.lookup("0103179782");
        service.lookup("0103179782");
        verify(events, times(1)).taxLookupError(argThat(p -> "0103179782".equals(p.get("taxCode"))));
    }

    @Test
    void lookup_notFound_doesNotAlert() {
        ServerEventService events = mock(ServerEventService.class);
        TaxCodeLookupService service = new TaxCodeLookupService(JSON, eventsOf(events), NOWHERE, "", "", NOWHERE, NOWHERE, false) {
            @Override
            Map<String, Object> fetch(String taxCode) {
                return Map.of("ok", false, "code", "NOT_FOUND");
            }
        };
        service.lookup("0103179782");
        verify(events, never()).taxLookupError(any());
    }

    @Test
    void parse_success_mapsNameAndAddress() throws Exception {
        Map<String, Object> out = TaxCodeLookupService.parse(
            "0103179782",
            JSON.readTree(
                "{\"error\":0,\"data\":{\"ten\":\"CÔNG TY TNHH X.E VIỆT NAM\",\"mst\":\"0103179782\",\"dt\":\"null\",\"dc\":\"  Số 4 đường Văn Chỉ,  TP Hà Nội \"}}"
            )
        );
        assertThat(out)
            .containsEntry("ok", true)
            .containsEntry("taxCode", "0103179782")
            .containsEntry("companyName", "CÔNG TY TNHH X.E VIỆT NAM")
            .containsEntry("address", "Số 4 đường Văn Chỉ, TP Hà Nội");
    }

    @Test
    void parse_errorOrMissingName_isNotFound() throws Exception {
        assertThat(TaxCodeLookupService.parse("0103179782", JSON.readTree("{\"error\":1,\"error_text\":\"x\"}"))).containsEntry(
            "code",
            "NOT_FOUND"
        );
        assertThat(TaxCodeLookupService.parse("0103179782", JSON.readTree("{\"error\":0,\"data\":{\"ten\":\"\"}}"))).containsEntry(
            "ok",
            false
        );
    }

    @Test
    void parse_differentTaxCode_isRejected() throws Exception {
        Map<String, Object> out = TaxCodeLookupService.parse(
            "0103179782",
            JSON.readTree("{\"error\":0,\"data\":{\"ten\":\"CÔNG TY KHÁC\",\"mst\":\"0100109106\"}}")
        );
        assertThat(out).containsEntry("ok", false).containsEntry("code", "NOT_FOUND");
    }

    @Test
    void parseVietQr_branchTaxCode_mapsNameAndAddress() throws Exception {
        Map<String, Object> out = TaxCodeLookupService.parseVietQr(
            "0101394777-008",
            JSON.readTree(
                "{\"code\":\"00\",\"desc\":\"Success\",\"data\":{\"id\":\"0101394777-008\",\"name\":\"CHI NHÁNH CÔNG TY CỔ PHẦN NGỌC HÀ TẠI NAM ĐỊNH\",\"address\":\"Số 135, đường Nguyễn Công Trứ, Phường Đông A, Ninh Bình\"}}"
            )
        );
        assertThat(out)
            .containsEntry("ok", true)
            .containsEntry("companyName", "CHI NHÁNH CÔNG TY CỔ PHẦN NGỌC HÀ TẠI NAM ĐỊNH")
            .containsEntry("address", "Số 135, đường Nguyễn Công Trứ, Phường Đông A, Ninh Bình");
    }

    @Test
    void parseVietQr_notFoundOrOtherTaxCode_isRejected() throws Exception {
        assertThat(
            TaxCodeLookupService.parseVietQr("0103179782", JSON.readTree("{\"code\":\"52\",\"desc\":\"not found\",\"data\":null}"))
        ).containsEntry("code", "NOT_FOUND");
        assertThat(
            TaxCodeLookupService.parseVietQr(
                "0103179782",
                JSON.readTree("{\"code\":\"00\",\"data\":{\"id\":\"0100109106\",\"name\":\"CÔNG TY KHÁC\"}}")
            )
        ).containsEntry("ok", false);
    }

    /** Giả lập từng nguồn trả kết quả theo {@code results}; nguồn không có trong map coi như lỗi kết nối. */
    private static TaxCodeLookupService stubSources(List<String> sources, Map<String, Map<String, Object>> results) {
        return new TaxCodeLookupService(JSON, NO_EVENTS, NOWHERE, "", "", NOWHERE, NOWHERE, false) {
            @Override
            Map<String, Object> fetchFrom(
                String source,
                String url,
                Map<String, String> headers,
                String taxCode,
                BiFunction<String, JsonNode, Map<String, Object>> parser
            ) {
                sources.add(source);
                return results.getOrDefault(source, Map.of("ok", false, "code", "UPSTREAM_ERROR"));
            }
        };
    }

    @Test
    void fetch_xinvoiceOk_skipsFallbacks() {
        List<String> sources = new ArrayList<>();
        TaxCodeLookupService service = stubSources(sources, Map.of("xinvoice", Map.of("ok", true, "companyName", "X")));
        assertThat(service.lookup("0103179782")).containsEntry("companyName", "X");
        assertThat(sources).containsExactly("xinvoice");
    }

    @Test
    void fetch_xinvoiceFails_fallsBackToVietQrThenEsgoo() {
        List<String> sources = new ArrayList<>();
        TaxCodeLookupService service = stubSources(sources, Map.of("esgoo", Map.of("ok", true, "companyName", "B")));
        assertThat(service.lookup("0103179782")).containsEntry("companyName", "B");
        assertThat(sources).containsExactly("xinvoice", "vietqr", "esgoo");
    }

    @Test
    void fetch_allMissOneNotFound_returnsNotFound() {
        List<String> sources = new ArrayList<>();
        TaxCodeLookupService service = stubSources(sources, Map.of("xinvoice", Map.of("ok", false, "code", "NOT_FOUND")));
        assertThat(service.lookup("0103179782")).containsEntry("code", "NOT_FOUND");
    }

    @Test
    void parseXinvoice_branch_mapsNameAddressAndStatus() throws Exception {
        Map<String, Object> out = TaxCodeLookupService.parseXinvoice(
            "0316794479-001",
            JSON.readTree(
                "{\"orgType\":\"Chi nhánh\",\"taxID\":\"0316794479-001\",\"name\":\"VĂN PHÒNG ĐẠI DIỆN CÔNG TY TNHH CASSO\",\"address\":\"Số 8 Lô LK1, Phường Đông Hòa, TP Hồ Chí Minh\",\"status\":\"NNT đang hoạt động\"}"
            )
        );
        assertThat(out)
            .containsEntry("ok", true)
            .containsEntry("companyName", "VĂN PHÒNG ĐẠI DIỆN CÔNG TY TNHH CASSO")
            .containsEntry("address", "Số 8 Lô LK1, Phường Đông Hòa, TP Hồ Chí Minh")
            .containsEntry("active", true);
    }

    @Test
    void parseXinvoice_otherTaxCodeOrNoName_isRejected() throws Exception {
        assertThat(
            TaxCodeLookupService.parseXinvoice("0103179782", JSON.readTree("{\"taxID\":\"0100109106\",\"name\":\"KHÁC\"}"))
        ).containsEntry("code", "NOT_FOUND");
        assertThat(
            TaxCodeLookupService.parseXinvoice("0103179782", JSON.readTree("{\"success\":false,\"message\":\"Tax not found\"}"))
        ).containsEntry("code", "NOT_FOUND");
    }

    @Test
    void isActiveStatus_detectsStoppedTaxpayers() {
        assertThat(TaxCodeLookupService.isActiveStatus("NNT đang hoạt động (đã được cấp GCN ĐKT)")).isTrue();
        assertThat(TaxCodeLookupService.isActiveStatus("NNT ngừng hoạt động và đã đóng MST")).isFalse();
        assertThat(TaxCodeLookupService.isActiveStatus("NNT tạm ngừng KD có thời hạn")).isFalse();
        assertThat(TaxCodeLookupService.isActiveStatus("NNT không hoạt động tại địa chỉ đã đăng ký")).isFalse();
        assertThat(TaxCodeLookupService.isActiveStatus("NNT ngừng HĐ nhưng chưa hoàn thành thủ tục chấm dứt hiệu lực MST")).isFalse();
    }

    @Test
    void lookup_invalidChecksum_doesNotCallUpstream() {
        TaxCodeLookupService service = new TaxCodeLookupService(JSON, NO_EVENTS, NOWHERE, "", "", NOWHERE, NOWHERE, false) {
            @Override
            Map<String, Object> fetch(String taxCode) {
                throw new AssertionError("không được gọi nguồn khi MST sai");
            }
        };
        assertThat(service.lookup("0103179783")).containsEntry("code", "INVALID");
    }

    @Test
    void lookup_cachesSuccess_only() {
        int[] calls = { 0 };
        TaxCodeLookupService service = new TaxCodeLookupService(JSON, NO_EVENTS, NOWHERE, "", "", NOWHERE, NOWHERE, false) {
            @Override
            Map<String, Object> fetch(String taxCode) {
                calls[0]++;
                return Map.of("ok", true, "taxCode", taxCode, "companyName", "A");
            }
        };
        service.lookup("0103179782");
        service.lookup("0103 179 782");
        assertThat(calls[0]).isEqualTo(1);
    }
}
