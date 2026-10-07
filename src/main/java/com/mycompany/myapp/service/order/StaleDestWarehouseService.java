package com.mycompany.myapp.service.order;

import com.mycompany.myapp.domain.ShipmentOrder;
import com.mycompany.myapp.domain.enumeration.ForwardStage;
import com.mycompany.myapp.domain.enumeration.IssueStatus;
import com.mycompany.myapp.domain.enumeration.OrderStatus;
import com.mycompany.myapp.repository.ShipmentOrderRepository;
import com.mycompany.myapp.service.autocall.AutoCallService;
import com.mycompany.myapp.service.dto.order.OrderTransitionRequest;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Đơn nằm nhập kho giao quá 2 ngày, không có sự cố đang mở → giao không thành công.
 * Không cộng số lần giao thất bại (luồng 3 lần / 48h của tài xế giữ nguyên).
 */
@Service
public class StaleDestWarehouseService {

    static final Duration STALE_AFTER = Duration.ofDays(2);
    static final int BATCH = 1000;
    static final String ACTION = "STALE_DEST";
    static final String DETAIL = "Quá 2 ngày ở nhập kho giao, khách chưa nhận";

    private static final Logger LOG = LoggerFactory.getLogger(StaleDestWarehouseService.class);

    private final ShipmentOrderRepository shipmentOrderRepository;
    private final OrderFacadeService orderFacadeService;
    private final TransactionTemplate transactionTemplate;
    private AutoCallService autoCallService;

    public StaleDestWarehouseService(
        ShipmentOrderRepository shipmentOrderRepository,
        OrderFacadeService orderFacadeService,
        PlatformTransactionManager transactionManager
    ) {
        this.shipmentOrderRepository = shipmentOrderRepository;
        this.orderFacadeService = orderFacadeService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Autowired(required = false)
    void setAutoCallService(AutoCallService autoCallService) {
        this.autoCallService = autoCallService;
    }

    @Scheduled(fixedDelay = 900_000, initialDelay = 60_000)
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void moveStaleOrders() {
        try {
            Instant cutoff = Instant.now().minus(STALE_AFTER);
            List<Long> ids = shipmentOrderRepository.findStaleDestWarehouseIds(
                OrderStatus.AT_DEST,
                ForwardStage.DEST_WH_IN,
                cutoff,
                IssueStatus.OPEN,
                PageRequest.of(0, BATCH)
            );
            int moved = 0;
            for (Long id : ids) {
                try {
                    Boolean ok = transactionTemplate.execute(s -> moveOne(id, cutoff));
                    if (Boolean.TRUE.equals(ok)) moved++;
                } catch (Exception e) {
                    LOG.info("Không chuyển đơn id {} sang giao không thành công: {}", id, e.getMessage());
                }
            }
            if (moved > 0) {
                LOG.info("Đã chuyển {} đơn quá 2 ngày ở nhập kho giao sang giao không thành công", moved);
            }
        } catch (Exception e) {
            LOG.warn("Quét đơn quá hạn nhập kho giao lỗi: {}", e.getMessage());
        }
    }

    /** @return true nếu đã chuyển */
    boolean moveOne(Long id, Instant cutoff) {
        ShipmentOrder order = shipmentOrderRepository.findById(id).orElse(null);
        if (order == null || order.getStatus() != OrderStatus.AT_DEST) {
            return false;
        }
        ForwardStage stage = order.getForwardStage();
        if (stage != null && stage != ForwardStage.DEST_WH_IN) {
            return false;
        }
        Instant ref = order.getUpdatedAt() != null ? order.getUpdatedAt() : order.getCreatedAt();
        if (ref == null || !ref.isBefore(cutoff)) {
            return false;
        }
        if (order.getIssue() != null && order.getIssue().getIssueStatus() == IssueStatus.OPEN && order.getIssue().getResolvedAt() == null) {
            return false;
        }
        OrderTransitionRequest req = new OrderTransitionRequest();
        req.setToStatus(OrderStatus.FAILED_DELIVERY);
        req.setAction(ACTION);
        req.setDetail(DETAIL);
        orderFacadeService.transition(order.getOrderCode(), req);
        if (autoCallService != null) {
            autoCallService.stopPendingCalls(order.getId(), DETAIL);
        }
        return true;
    }
}
