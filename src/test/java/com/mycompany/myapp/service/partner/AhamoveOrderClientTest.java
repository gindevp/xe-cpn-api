package com.mycompany.myapp.service.partner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mycompany.myapp.web.rest.errors.BadRequestAlertException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AhamoveOrderClientTest {

    private final ObjectMapper om = new ObjectMapper();

    @Test
    void extractDistanceKm_fromObjectData() throws Exception {
        ObjectNode root = om.createObjectNode();
        root.put("service_id", "HAN-BIKE");
        root.putObject("data").put("distance", 2.79);
        assertThat(AhamoveOrderClient.extractDistanceKm(root)).isEqualTo(2.79);
    }

    @Test
    void extractDistanceKm_fromArrayResponse_likeDocs() throws Exception {
        ArrayNode arr = om.createArrayNode();
        ObjectNode item = arr.addObject();
        item.put("service_id", "SGN-BIKE");
        item.putObject("data").put("distance", 11.98);
        assertThat(AhamoveOrderClient.extractDistanceKm(AhamoveOrderClient.unwrapEstimateItem(arr, null))).isEqualTo(11.98);
    }

    @Test
    void unwrap_prefersItemWithDistance_whenSiblingHasError() throws Exception {
        ArrayNode arr = om.createArrayNode();
        ObjectNode bad = arr.addObject();
        bad.put("service_id", "SGN-BIKE");
        bad.putNull("data");
        ObjectNode err = bad.putObject("error");
        err.put("code", "INVALID_PICKUP_AREA");
        err.put("title", "invalid");
        ObjectNode ok = arr.addObject();
        ok.put("service_id", "HAN-BIKE");
        ok.putObject("data").put("distance", 3.5);
        assertThat(AhamoveOrderClient.extractDistanceKm(AhamoveOrderClient.unwrapEstimateItem(arr, null))).isEqualTo(3.5);
    }

    @Test
    void assertNoEstimateError_throwsOnInvalidPickupArea() throws Exception {
        ObjectNode item = om.createObjectNode();
        item.put("service_id", "SGN-BIKE");
        item.putNull("data");
        ObjectNode err = item.putObject("error");
        err.put("code", "INVALID_PICKUP_AREA");
        err.put("title", "Điểm lấy hàng không hợp lệ");
        assertThatThrownBy(() -> AhamoveOrderClient.assertNoEstimateError(item)).isInstanceOf(BadRequestAlertException.class);
    }

    @Test
    void pickPreferredServiceId_prefersBike() {
        assertThat(
            AhamoveOrderClient.pickPreferredServiceId(List.of(Map.of("id", "HAN-ECO"), Map.of("id", "HAN-BIKE"), Map.of("id", "HAN-TRUCK")))
        ).isEqualTo("HAN-BIKE");
    }

    @Test
    void extractDistanceMeters_compatMultipliesKmBy1000() throws Exception {
        ObjectNode root = om.createObjectNode();
        root.putObject("data").put("distance", 2.79);
        assertThat(AhamoveOrderClient.extractDistanceMeters(root)).isEqualTo(2790.0);
    }
}
