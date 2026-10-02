package com.mycompany.myapp.service.invoice;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TaxCodeLookupServiceTest {

    private static final ObjectMapper JSON = new ObjectMapper();

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
    void lookup_invalidChecksum_doesNotCallUpstream() {
        TaxCodeLookupService service = new TaxCodeLookupService(JSON, "http://127.0.0.1:9", false) {
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
        TaxCodeLookupService service = new TaxCodeLookupService(JSON, "http://127.0.0.1:9", false) {
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
