package com.mycompany.myapp.service.order;

import com.mycompany.myapp.domain.OrderEvent;
import com.mycompany.myapp.domain.ShipmentOrder;
import com.mycompany.myapp.domain.SurchargePolicy;
import com.mycompany.myapp.domain.enumeration.ForwardStage;
import com.mycompany.myapp.domain.enumeration.OrderStatus;
import com.mycompany.myapp.repository.OrderEventRepository;
import com.mycompany.myapp.repository.ShipmentOrderRepository;
import com.mycompany.myapp.repository.SurchargePolicyRepository;
import com.mycompany.myapp.security.SecurityUtils;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Phí tồn kho giao và phí hoàn — cộng vào {@code fare_amount} theo cấu hình phụ phí.
 * Tồn kho: số ngày lịch kể từ ngày nhập kho giao (ngày nhập là ngày 1) − ngày miễn phí, nhân phí/ngày. Chỉ khi đơn còn nằm kho giao.
 * Hoàn: phần trăm × (cước hàng + cước lấy + cước giao) lúc bấm hoàn; hủy hoàn thì trừ lại.
 */
@Service
@Transactional
public class SurchargeFeeService {

    private static final Logger LOG = LoggerFactory.getLogger(SurchargeFeeService.class);
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");
    static final List<String> ARRIVAL_ACTIONS = List.of(
        "SCAN_IN",
        "HUB_IN",
        "DEST_WH_IN",
        "LEG_ARRIVE_DEST",
        "FAIL_MAX",
        "FAIL_48H",
        "REDELIVER_IN"
    );

    private final ShipmentOrderRepository shipmentOrderRepository;
    private final OrderEventRepository orderEventRepository;
    private final SurchargePolicyRepository surchargePolicyRepository;

    public SurchargeFeeService(
        ShipmentOrderRepository shipmentOrderRepository,
        OrderEventRepository orderEventRepository,
        SurchargePolicyRepository surchargePolicyRepository
    ) {
        this.shipmentOrderRepository = shipmentOrderRepository;
        this.orderEventRepository = orderEventRepository;
        this.surchargePolicyRepository = surchargePolicyRepository;
    }

    /** Cập nhật mọi đơn đang nằm kho giao. Trả về số đơn đổi tiền. */
    public int refreshSittingOrders() {
        int changed = 0;
        for (Long id : shipmentOrderRepository.findIdsAccruingStorageFee()) {
            try {
                if (refreshStorageFee(id)) {
                    changed++;
                }
            } catch (RuntimeException ex) {
                LOG.warn("Storage fee skipped for order {}: {}", id, ex.getMessage());
            }
        }
        return changed;
    }

    public boolean refreshStorageFee(Long orderId) {
        if (orderId == null) {
            return false;
        }
        return shipmentOrderRepository.findById(orderId).map(this::refreshStorageFee).orElse(false);
    }

    public boolean refreshStorageFee(ShipmentOrder order) {
        if (!accruing(order)) {
            return false;
        }
        BigDecimal next = storageAmount(order, policy(), Instant.now());
        BigDecimal prev = nz(order.getStorageFeeAmount());
        if (next.compareTo(prev) == 0) {
            return false;
        }
        String detail = "Phí tồn kho " + next.toPlainString() + " (trước đó " + prev.toPlainString() + ")";
        return applyDelta(order, next, prev, true, "STORAGE_FEE", detail);
    }

    /** Cộng phí hoàn một lần. Gọi khi đơn còn số COD cũ (trước khi xóa COD). */
    public boolean applyReturnFee(ShipmentOrder order) {
        if (order == null || nz(order.getReturnFeeAmount()).signum() > 0) {
            return false;
        }
        SurchargePolicy policy = policy();
        if (policy == null || !Boolean.TRUE.equals(policy.getRefundEnabled())) {
            return false;
        }
        BigDecimal fee = returnAmount(transportBase(order), policy.getRefundPercent());
        if (fee.signum() <= 0) {
            return false;
        }
        return applyDelta(order, fee, BigDecimal.ZERO, false, "RETURN_FEE", "Phí hoàn " + fee.toPlainString());
    }

    public boolean revertReturnFee(ShipmentOrder order) {
        if (order == null) {
            return false;
        }
        BigDecimal prev = nz(order.getReturnFeeAmount());
        if (prev.signum() <= 0) {
            return false;
        }
        return applyDelta(order, BigDecimal.ZERO, prev, false, "RETURN_FEE_CANCEL", "Gỡ phí hoàn " + prev.toPlainString());
    }

    /**
     * Số ngày lịch (giờ VN, ngày nhập kho là ngày 1) − ngày miễn phí, rồi nhân phí/ngày. Chưa tới kho thì 0.
     * Theo ngày lịch để phí chỉ đổi qua nửa đêm — số hiện trên màn giao / phiếu thu không nhảy giữa ngày.
     */
    static BigDecimal storageAmount(Instant arrivedAt, Instant now, Integer freeDays, BigDecimal feePerDay) {
        if (arrivedAt == null || now == null || !now.isAfter(arrivedAt) || feePerDay == null || feePerDay.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        long days = ChronoUnit.DAYS.between(LocalDate.ofInstant(arrivedAt, VN), LocalDate.ofInstant(now, VN)) + 1;
        int free = freeDays == null ? 0 : Math.max(0, freeDays);
        int charged = (int) Math.max(0, days - free);
        if (charged == 0) {
            return BigDecimal.ZERO;
        }
        return feePerDay.multiply(BigDecimal.valueOf(charged)).setScale(0, RoundingMode.HALF_UP);
    }

    static BigDecimal returnAmount(BigDecimal transport, BigDecimal percent) {
        if (transport == null || transport.signum() <= 0 || percent == null || percent.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        return transport.multiply(percent).divide(HUNDRED, 0, RoundingMode.HALF_UP);
    }

    /** Cước vận chuyển: cước hàng + lấy tận nơi + giao tận nơi. Không gồm COD, khai giá, tồn kho, phí hoàn. */
    static BigDecimal transportBase(ShipmentOrder order) {
        if (order == null) {
            return BigDecimal.ZERO;
        }
        if (order.getGoodsFareAmount() != null) {
            return nz(order.getGoodsFareAmount())
                .add(nz(order.getPickupFeeAmount()))
                .add(nz(order.getDeliveryFeeAmount()))
                .max(BigDecimal.ZERO);
        }
        return nz(order.getFareAmount())
            .subtract(nz(order.getPickupFeeAmount()))
            .subtract(nz(order.getDeliveryFeeAmount()))
            .subtract(nz(order.getCodFeeAmount()))
            .subtract(nz(order.getDeclaredFeeAmount()))
            .subtract(nz(order.getStorageFeeAmount()))
            .subtract(nz(order.getReturnFeeAmount()))
            .add(nz(order.getDiscountAmount()))
            .max(BigDecimal.ZERO)
            .add(nz(order.getPickupFeeAmount()))
            .add(nz(order.getDeliveryFeeAmount()));
    }

    static Instant arrivalAt(List<OrderEvent> events) {
        if (events == null) {
            return null;
        }
        Instant best = null;
        for (OrderEvent event : events) {
            if (event.getAction() == null || event.getEventAt() == null || !ARRIVAL_ACTIONS.contains(event.getAction())) {
                continue;
            }
            if (best == null || event.getEventAt().isBefore(best)) {
                best = event.getEventAt();
            }
        }
        return best;
    }

    static boolean accruing(ShipmentOrder order) {
        if (order == null || order.getStatus() == null) {
            return false;
        }
        OrderStatus status = order.getStatus();
        if (
            status == OrderStatus.RETURNING ||
            status == OrderStatus.DELIVERED ||
            status == OrderStatus.CANCELLED ||
            status == OrderStatus.RETURNED
        ) {
            return false;
        }
        ForwardStage stage = order.getForwardStage();
        if (stage == ForwardStage.DEST_WH_IN || stage == ForwardStage.FAILED || stage == ForwardStage.REDELIVER_WAIT) {
            return true;
        }
        return status == OrderStatus.AT_DEST || status == OrderStatus.FAILED_DELIVERY;
    }

    private BigDecimal storageAmount(ShipmentOrder order, SurchargePolicy policy, Instant now) {
        if (policy == null || !Boolean.TRUE.equals(policy.getStorageEnabled())) {
            return BigDecimal.ZERO;
        }
        List<OrderEvent> events = order.getId() == null ? List.of() : orderEventRepository.findByOrder_IdOrderByEventAtAsc(order.getId());
        return storageAmount(arrivalAt(events), now, policy.getStorageFreeDays(), policy.getStorageFeePerDay());
    }

    private boolean applyDelta(ShipmentOrder order, BigDecimal next, BigDecimal prev, boolean storage, String action, String detail) {
        next = nz(next).max(BigDecimal.ZERO);
        prev = nz(prev);
        BigDecimal delta = next.subtract(prev);
        if (delta.signum() == 0) {
            return false;
        }
        BigDecimal fare = nz(order.getFareAmount());
        BigDecimal paid = nz(order.getPaidAmount());
        if (fare.add(delta).compareTo(paid) < 0) {
            delta = paid.subtract(fare);
            if (delta.signum() == 0) {
                return false;
            }
            next = prev.add(delta).max(BigDecimal.ZERO);
        }
        if (storage) {
            order.setStorageFeeAmount(next);
        } else {
            order.setReturnFeeAmount(next);
        }
        order.setFareAmount(fare.add(delta).max(BigDecimal.ZERO));
        shipmentOrderRepository.save(order);
        OrderEvent event = new OrderEvent();
        event.setEventAt(Instant.now());
        event.setAction(action);
        event.setDetail(detail.length() > OrderEvent.DETAIL_MAX ? detail.substring(0, OrderEvent.DETAIL_MAX) : detail);
        event.setActorUsername(SecurityUtils.getCurrentUserLogin().orElse("system"));
        event.setOrder(order);
        orderEventRepository.save(event);
        return true;
    }

    private SurchargePolicy policy() {
        return surchargePolicyRepository.findAll().stream().findFirst().orElse(null);
    }

    private static BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
