package com.mycompany.myapp.service.payment.onepay;

import com.mycompany.myapp.domain.ShipmentOrder;
import com.mycompany.myapp.repository.ShipmentOrderRepository;
import com.mycompany.myapp.service.order.OrderMoney;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Khi OnePay báo đủ tiền đơn app: cộng vào số đã thu và báo mobile_service. */
@Service
public class OnePayOrderPaidHandler {

    private static final Logger LOG = LoggerFactory.getLogger(OnePayOrderPaidHandler.class);

    private final ShipmentOrderRepository orders;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();

    @Value("${cpn.mobile-callback-url:}")
    private String callbackUrl;

    @Value("${cpn.mobile-track-key:}")
    private String callbackKey;

    public OnePayOrderPaidHandler(ShipmentOrderRepository orders) {
        this.orders = orders;
    }

    @Transactional
    public void paid(String orderCode, BigDecimal amount) {
        if (orderCode == null || orderCode.isBlank() || amount == null || amount.signum() <= 0) {
            return;
        }
        ShipmentOrder order = orders.findOneByOrderCodeOrDraftCode(orderCode.trim()).orElse(null);
        if (order == null) {
            LOG.warn("IPN OnePay khớp đơn {} nhưng không thấy vận đơn", orderCode);
            return;
        }
        BigDecimal next = OrderMoney.nz(order.getPaidAmount()).add(amount);
        order.setPaidAmount(next);
        orders.save(order);
        notifyMobile(order.getOrderCode(), next);
    }

    private void notifyMobile(String orderCode, BigDecimal paidAmount) {
        if (callbackUrl == null || callbackUrl.isBlank()) {
            LOG.info("Chưa cấu hình CPN_MOBILE_CALLBACK_URL, bỏ qua báo đã thu đơn {}", orderCode);
            return;
        }
        try {
            String body = "{\"orderCode\":\"" + orderCode + "\",\"paidAmount\":" + paidAmount.toPlainString() + "}";
            HttpRequest.Builder req = HttpRequest.newBuilder()
                .uri(URI.create(callbackUrl.trim()))
                .timeout(Duration.ofSeconds(12))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body));
            if (callbackKey != null && !callbackKey.isBlank()) {
                req.header("X-Mobile-Track-Key", callbackKey);
            }
            HttpResponse<String> res = http.send(req.build(), HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() < 200 || res.statusCode() >= 300) {
                LOG.warn("Mobile không nhận callback đơn {} HTTP {}", orderCode, res.statusCode());
            }
        } catch (Exception ex) {
            LOG.warn("Không gọi được mobile callback đơn {}: {}", orderCode, ex.getMessage());
        }
    }
}
