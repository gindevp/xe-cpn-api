package com.mycompany.myapp.service.order;

import com.mycompany.myapp.domain.ShipmentOrder;
import com.mycompany.myapp.domain.enumeration.ForwardStage;
import com.mycompany.myapp.domain.enumeration.OrderStatus;

/**
 * Nhãn trạng thái tra cứu khách = tên tab vận hành mà đơn đang nằm
 * (Chờ bàn giao / Nhập kho-Luân chuyển), không dùng nhãn OrderStatus thuần.
 */
public final class CustomerTrackStatus {

    private CustomerTrackStatus() {}

    public static String labelOf(ShipmentOrder o) {
        if (o == null || o.getStatus() == null) {
            return "";
        }
        OrderStatus st = o.getStatus();
        if (st == OrderStatus.DELIVERED) {
            return "Đã giao";
        }
        if (st == OrderStatus.CANCELLED) {
            return "Đã hủy";
        }
        if (st == OrderStatus.RETURNED) {
            return "Đã hoàn";
        }
        if (st == OrderStatus.RETURNING) {
            return "Đang hoàn";
        }

        // Chờ bàn giao (chưa vào kho gửi)
        String pending = pendingHandoverTab(o);
        if (pending != null) {
            return pending;
        }

        ForwardStage stage = resolveStage(o);
        if (stage != null) {
            return pipelineTabLabel(stage);
        }

        return switch (st) {
            case DRAFT -> "Nháp";
            case CONFIRMED -> "Đã chốt";
            case WAITING -> "Đợi trung chuyển giao";
            case IN_TRANSIT -> "Hàng trên xe";
            case AT_DEST -> "Nhập kho giao";
            case OUT_FOR_DELIVERY -> "Đang giao hàng";
            case FAILED_DELIVERY -> "Giao hàng không thành công";
            default -> st.name();
        };
    }

    /** null nếu không thuộc màn Chờ bàn giao. */
    static String pendingHandoverTab(ShipmentOrder o) {
        OrderStatus st = o.getStatus();
        if (
            st == OrderStatus.CANCELLED ||
            st == OrderStatus.DELIVERED ||
            st == OrderStatus.RETURNED ||
            st == OrderStatus.IN_TRANSIT ||
            st == OrderStatus.AT_DEST
        ) {
            return null;
        }
        if (o.getPickedUpAt() != null) {
            return null;
        }
        if (o.getCurrentTrip() != null) {
            return null;
        }
        boolean homePickup = Boolean.TRUE.equals(o.getHomePickup());
        boolean qrDropOff = Boolean.TRUE.equals(o.getQrDropOff());
        // Legacy: đơn nháp cũ chưa migrate — vẫn hiện Chờ nhận hàng.
        boolean legacyDraftDropOff = st == OrderStatus.DRAFT && !homePickup;
        if (!homePickup && !qrDropOff && !legacyDraftDropOff) {
            return null;
        }
        if (qrDropOff || legacyDraftDropOff) {
            return "Chờ nhận hàng";
        }
        // homePickup
        boolean picking = o.getPickingAt() != null || (o.getPickupStaffUsername() != null && !o.getPickupStaffUsername().isBlank());
        return picking ? "Đang lấy hàng" : "Chờ lấy hàng";
    }

    static ForwardStage resolveStage(ShipmentOrder o) {
        OrderStatus st = o.getStatus();
        if (
            st == OrderStatus.DELIVERED ||
            st == OrderStatus.CANCELLED ||
            st == OrderStatus.RETURNED ||
            st == OrderStatus.RETURNING ||
            st == OrderStatus.DRAFT
        ) {
            return null;
        }
        if (o.getForwardStage() != null) {
            return o.getForwardStage();
        }
        return switch (st) {
            case FAILED_DELIVERY -> ForwardStage.FAILED;
            case OUT_FOR_DELIVERY -> ForwardStage.DELIVERING;
            case AT_DEST -> ForwardStage.DEST_WH_IN;
            case IN_TRANSIT -> ForwardStage.TRANSFERRING;
            case WAITING -> ForwardStage.TRANSFER_PENDING;
            case CONFIRMED -> {
                if (o.getPickedUpAt() != null) {
                    yield ForwardStage.WH_IN;
                }
                if (Boolean.TRUE.equals(o.getHomePickup()) && o.getPickingAt() != null) {
                    yield ForwardStage.PICKED;
                }
                if (Boolean.TRUE.equals(o.getHomePickup()) || Boolean.TRUE.equals(o.getQrDropOff())) {
                    yield null;
                }
                yield ForwardStage.WH_IN;
            }
            default -> null;
        };
    }

    static String pipelineTabLabel(ForwardStage stage) {
        return switch (stage) {
            case PICKED -> "Lấy hàng thành công";
            case WH_IN -> "Nhập kho gửi";
            case TRANSFER_PENDING -> "Đợi trung chuyển giao";
            case TRANSFERRING -> "Hàng trên xe";
            case DEST_WH_IN -> "Nhập kho giao";
            case DELIVERING -> "Đang giao hàng";
            case FAILED -> "Giao hàng không thành công";
            case REDELIVER_WAIT -> "Chờ giao lại";
        };
    }
}
