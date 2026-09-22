package com.mycompany.myapp.service.partner;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Map;
import org.junit.jupiter.api.Test;

class OsmNominatimServiceTest {

    private final ObjectMapper om = new ObjectMapper();

    @Test
    void encodePlaceId() {
        assertThat(OsmNominatimService.encodePlaceId("way", "123")).isEqualTo("way:123");
        assertThat(OsmNominatimService.encodePlaceId(null, "1")).isNull();
    }

    @Test
    void toOsmIdsParam() {
        assertThat(OsmNominatimService.toOsmIdsParam("way:987")).isEqualTo("W987");
        assertThat(OsmNominatimService.toOsmIdsParam("node:1")).isEqualTo("N1");
        assertThat(OsmNominatimService.toOsmIdsParam("relation:9")).isEqualTo("R9");
        assertThat(OsmNominatimService.toOsmIdsParam("bad")).isNull();
    }

    @Test
    void mapPhotonFeature_readsLngLatOrder() throws Exception {
        ObjectNode f = om.createObjectNode();
        ArrayNode coords = f.putObject("geometry").putArray("coordinates");
        coords.add(105.8525);
        coords.add(21.0289);
        ObjectNode props = f.putObject("properties");
        props.put("osm_type", "W");
        props.put("osm_id", 99);
        props.put("name", "Phường Trung Hoà");
        props.put("city", "Hà Nội");
        props.put("country", "Vietnam");

        Map<String, String> row = OsmNominatimService.mapPhotonFeature(f);
        assertThat(row).isNotNull();
        assertThat(row.get("lat")).isEqualTo("21.0289");
        assertThat(row.get("lng")).isEqualTo("105.8525");
        assertThat(row.get("placeId")).isEqualTo("way:99");
        assertThat(row.get("description")).contains("Trung Hoà");
    }
}
