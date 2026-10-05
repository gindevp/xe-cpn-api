package com.mycompany.myapp.service.partner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.myapp.web.rest.errors.BadRequestAlertException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class AhamoveDispatchServiceTest {

    private final ObjectMapper om = new ObjectMapper();

    @Test
    void stripStopSuffix_removesStopIndexOnly() {
        assertThat(AhamoveDispatchService.stripStopSuffix("24AB7XQ9-1")).isEqualTo("24AB7XQ9");
        assertThat(AhamoveDispatchService.stripStopSuffix("24AB7XQ9")).isEqualTo("24AB7XQ9");
        assertThat(AhamoveDispatchService.stripStopSuffix(" 24AB7XQ9 ")).isEqualTo("24AB7XQ9");
        assertThat(AhamoveDispatchService.stripStopSuffix("AB-CD")).isEqualTo("AB-CD");
        assertThat(AhamoveDispatchService.stripStopSuffix(null)).isNull();
    }

    @Test
    void parseWebhook_completedWithPod() throws Exception {
        String json =
            """
            {"_id":"24AB7XQ9","status":"COMPLETED","sub_status":"","supplier_name":"Nguyen Van A","supplier_id":"84901234567",
             "shared_link":"https://cloudjetsandbox.ahamove.com/share-order/24AB7XQ9","total_pay":32000,
             "path":[{"address":"VP","status":"COMPLETED"},
                     {"address":"Nha khach","status":"COMPLETED","pod_info":[{"image_url":"https://img.ahamove.com/pod1.jpg"},"https://img.ahamove.com/pod2.jpg"]}]}
            """;
        AhamoveDispatchService.WebhookUpdate u = AhamoveDispatchService.parseWebhook(om.readTree(json));
        assertThat(u.orderId()).isEqualTo("24AB7XQ9");
        assertThat(u.status()).isEqualTo("COMPLETED");
        assertThat(u.dropStatus()).isEqualTo("COMPLETED");
        assertThat(u.driverName()).isEqualTo("Nguyen Van A");
        assertThat(u.driverPhone()).isEqualTo("84901234567");
        assertThat(u.totalPay()).isEqualByComparingTo(new BigDecimal("32000"));
        assertThat(u.podUrls()).containsExactly("https://img.ahamove.com/pod1.jpg", "https://img.ahamove.com/pod2.jpg");
    }

    @Test
    void parseWebhook_failedStop_noPod() throws Exception {
        String json =
            """
            {"_id":"24AB7XQ9-1","status":"IN PROCESS",
             "path":[{"status":"COMPLETED"},{"status":"FAILED","fail_comment":"Khách không nghe máy","pod_info":""}]}
            """;
        AhamoveDispatchService.WebhookUpdate u = AhamoveDispatchService.parseWebhook(om.readTree(json));
        assertThat(u.orderId()).isEqualTo("24AB7XQ9");
        assertThat(u.dropStatus()).isEqualTo("FAILED");
        assertThat(u.failReason()).isEqualTo("Khách không nghe máy");
        assertThat(u.podUrls()).isEmpty();
    }

    @Test
    void parseWebhook_rejectsNonObject() throws Exception {
        assertThat(AhamoveDispatchService.parseWebhook(om.readTree("[]"))).isNull();
        assertThat(AhamoveDispatchService.parseWebhook(null)).isNull();
    }

    @Test
    void parseCreated_readsIdLinkAndFee() throws Exception {
        String json =
            """
            {"order_id":"24AB7XQ9","status":"ASSIGNING","shared_link":"https://ahamove.com/s/24AB7XQ9",
             "order":{"_id":"24AB7XQ9","service_id":"HAN-BIKE","total_pay":28500.4}}
            """;
        AhamoveOrderClient.CreatedOrder c = AhamoveOrderClient.parseCreated(om.readTree(json));
        assertThat(c.orderId()).isEqualTo("24AB7XQ9");
        assertThat(c.status()).isEqualTo("ASSIGNING");
        assertThat(c.sharedLink()).isEqualTo("https://ahamove.com/s/24AB7XQ9");
        assertThat(c.totalPay()).isEqualByComparingTo(new BigDecimal("28500"));
        assertThat(c.serviceId()).isEqualTo("HAN-BIKE");
    }

    @Test
    void parseCreated_missingId_throws() throws Exception {
        assertThatThrownBy(() -> AhamoveOrderClient.parseCreated(om.readTree("{\"status\":\"IDLE\"}"))).isInstanceOf(
            BadRequestAlertException.class
        );
    }

    @Test
    void extractTotalPrice_fromEstimateData() throws Exception {
        assertThat(
            AhamoveOrderClient.extractTotalPrice(om.readTree("{\"data\":{\"distance\":3.1,\"total_price\":25000}}"))
        ).isEqualByComparingTo(new BigDecimal("25000"));
        assertThat(AhamoveOrderClient.extractTotalPrice(om.readTree("{\"data\":{\"distance\":3.1}}"))).isNull();
    }
}
