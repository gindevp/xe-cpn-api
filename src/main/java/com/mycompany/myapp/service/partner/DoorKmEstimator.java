package com.mycompany.myapp.service.partner;

import com.mycompany.myapp.domain.Office;
import com.mycompany.myapp.web.rest.errors.BadRequestAlertException;
import java.math.BigDecimal;
import java.util.Map;
import org.springframework.stereotype.Service;

/** KM từ VP tới địa chỉ lấy/giao tận nơi theo ước tính Ahamove (không tạo đơn) — dùng để tra bảng phí tận nơi. */
@Service
public class DoorKmEstimator {

    private static final String ENTITY = "order";

    private final AhamoveOrderClient ahamoveOrderClient;

    public DoorKmEstimator(AhamoveOrderClient ahamoveOrderClient) {
        this.ahamoveOrderClient = ahamoveOrderClient;
    }

    /**
     * @param label "giao" / "lấy" — chỉ để báo lỗi.
     * @param pinLat/pinLng null → Ahamove tự dò theo {@code address}.
     */
    public BigDecimal km(Office office, Double pinLat, Double pinLng, String address, String label) {
        if (office == null || office.getLatitude() == null || office.getLongitude() == null) {
            throw new BadRequestAlertException(
                "VP " +
                (office != null && office.getCode() != null ? office.getCode() + " " : "") +
                "chưa có toạ độ GPS (Danh mục VP) — không tính được km " +
                label +
                " tận nơi",
                ENTITY,
                "doorKmOfficeGps"
            );
        }
        boolean pinned = pinLat != null && pinLng != null;
        String addr = address == null ? "" : address.trim();
        if (!pinned && addr.isEmpty()) {
            throw new BadRequestAlertException("Nhập địa chỉ " + label + " hàng để tính km tận nơi", ENTITY, "doorKmAddressRequired");
        }
        Object km;
        try {
            Map<String, Object> r = ahamoveOrderClient.estimatePickupDistance(
                office.getLatitude().doubleValue(),
                office.getLongitude().doubleValue(),
                office.getAddress(),
                pinned ? pinLat : null,
                pinned ? pinLng : null,
                addr.isEmpty() ? null : addr
            );
            km = r.get("distanceKm");
        } catch (RuntimeException e) {
            throw new BadRequestAlertException(
                "Không lấy được km " + label + " tận nơi từ Ahamove — kiểm tra lại địa chỉ (" + e.getMessage() + ")",
                ENTITY,
                "doorKmEstimate"
            );
        }
        if (km == null) {
            throw new BadRequestAlertException(
                "Ahamove không trả km " + label + " tận nơi — kiểm tra lại địa chỉ",
                ENTITY,
                "doorKmEstimate"
            );
        }
        BigDecimal v = km instanceof BigDecimal b ? b : new BigDecimal(km.toString());
        return v.max(BigDecimal.ZERO);
    }
}
