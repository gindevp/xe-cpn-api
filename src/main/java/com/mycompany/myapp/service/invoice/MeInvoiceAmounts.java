package com.mycompany.myapp.service.invoice;

import com.mycompany.myapp.domain.Office;
import com.mycompany.myapp.domain.ShipmentOrder;
import com.mycompany.myapp.service.order.OrderMoney;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Tách gross (đã gồm VAT 10%) → net + vat cho body MISA meInvoice.
 * <p>
 * Gross = cước vận chuyển + phí thu hộ COD + phí khai báo GT.
 * Cước = cước hàng + phí lấy tận nơi + phí giao tận nơi (không gồm tiền hàng COD).
 */
public final class MeInvoiceAmounts {

    public static final BigDecimal VAT_FACTOR = new BigDecimal("1.1");
    public static final String VAT_RATE_NAME = "10%";
    public static final String ITEM_CODE = "DV-VANCHUYEN";
    public static final String UNIT_NAME = "Chuyen";

    private MeInvoiceAmounts() {}

    /**
     * Tên hàng hóa/dịch vụ trên HĐĐT:
     * {@code Dịch vụ bưu chính chuyển phát hàng hóa từ {tỉnh gửi} đến {tỉnh nhận} Bill: {mã đơn}}.
     * Tỉnh/TP lấy từ địa chỉ người gửi/nhận (đoạn cuối sau dấu phẩy); thiếu thì fallback VP.
     */
    public static String itemNameFor(ShipmentOrder order) {
        String from = placeFromAddressOrOffice(order.getPickupAddress(), order.getFromOffice());
        Office toOffice = order.getFinalToOffice() != null ? order.getFinalToOffice() : order.getToOffice();
        String to = placeFromAddressOrOffice(order.getDeliveryAddress(), toOffice);
        String code = order.getOrderCode() == null ? "" : order.getOrderCode().trim();
        if (from.isBlank()) {
            from = "không xác định";
        }
        if (to.isBlank()) {
            to = "không xác định";
        }
        String name = "Dịch vụ bưu chính chuyển phát hàng hóa từ " + from + " đến " + to + " Bill: " + code;
        return name.length() > 500 ? name.substring(0, 500) : name;
    }

    /** Ưu tiên tỉnh/TP từ địa chỉ đầy đủ; thiếu thì lấy từ địa chỉ/tên VP. */
    static String placeFromAddressOrOffice(String address, Office office) {
        String fromAddress = provinceFromAddress(address);
        if (!fromAddress.isBlank()) {
            return fromAddress;
        }
        if (office == null) {
            return "";
        }
        String fromOfficeAddress = provinceFromAddress(office.getAddress());
        if (!fromOfficeAddress.isBlank()) {
            return fromOfficeAddress;
        }
        return cleanPlaceName(office.getName());
    }

    /**
     * Địa chỉ dạng {@code số nhà, phường, [quận,] tỉnh/TP} → lấy đoạn cuối.
     */
    static String provinceFromAddress(String address) {
        if (address == null || address.isBlank()) {
            return "";
        }
        String[] parts = address.split(",");
        for (int i = parts.length - 1; i >= 0; i--) {
            String cleaned = cleanPlaceName(parts[i]);
            if (!cleaned.isBlank()) {
                return cleaned;
            }
        }
        return "";
    }

    static String cleanPlaceName(String raw) {
        if (raw == null) {
            return "";
        }
        String s = raw.trim();
        if (s.isBlank()) {
            return "";
        }
        s = s.replaceFirst("(?iu)^(Tỉnh|Thành\\s*phố|TP\\.?)\\s+", "").trim();
        return s;
    }

    public record Split(BigDecimal gross, BigDecimal net, BigDecimal vat) {}

    public record Breakdown(
        BigDecimal shipping,
        BigDecimal goodsFare,
        BigDecimal pickupFee,
        BigDecimal deliveryFee,
        BigDecimal codFee,
        BigDecimal declaredFee,
        BigDecimal gross,
        BigDecimal net,
        BigDecimal vat
    ) {}

    /** net = round(gross / 1.1), vat = gross - net (VND, scale 0). */
    public static Split splitGross(BigDecimal grossInclusiveVat) {
        BigDecimal gross = OrderMoney.nz(grossInclusiveVat).setScale(0, RoundingMode.HALF_UP);
        if (gross.compareTo(BigDecimal.ZERO) <= 0) {
            return new Split(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        }
        BigDecimal net = gross.divide(VAT_FACTOR, 0, RoundingMode.HALF_UP);
        BigDecimal vat = gross.subtract(net);
        return new Split(gross, net, vat);
    }

    public static Breakdown fromOrder(ShipmentOrder order) {
        BigDecimal pickup = OrderMoney.nz(order.getPickupFeeAmount()).setScale(0, RoundingMode.HALF_UP);
        BigDecimal delivery = OrderMoney.nz(order.getDeliveryFeeAmount()).setScale(0, RoundingMode.HALF_UP);
        BigDecimal codFee = OrderMoney.nz(order.getCodFeeAmount()).setScale(0, RoundingMode.HALF_UP);
        BigDecimal declared = OrderMoney.nz(order.getDeclaredFeeAmount()).setScale(0, RoundingMode.HALF_UP);

        BigDecimal goods;
        if (order.getGoodsFareAmount() != null) {
            goods = order.getGoodsFareAmount().setScale(0, RoundingMode.HALF_UP);
        } else {
            // Đơn cũ chưa tách thành phần: suy cước hàng từ tổng phải thu.
            BigDecimal fare = OrderMoney.nz(order.getFareAmount());
            BigDecimal discount = OrderMoney.nz(order.getDiscountAmount());
            goods = fare.subtract(pickup).subtract(delivery).subtract(codFee).subtract(declared).add(discount);
            if (goods.compareTo(BigDecimal.ZERO) < 0) {
                goods = BigDecimal.ZERO;
            }
            goods = goods.setScale(0, RoundingMode.HALF_UP);
        }

        BigDecimal shipping = goods.add(pickup).add(delivery);
        BigDecimal gross = shipping.add(codFee).add(declared);
        Split split = splitGross(gross);
        return new Breakdown(shipping, goods, pickup, delivery, codFee, declared, split.gross(), split.net(), split.vat());
    }

    public static String refIdFor(String orderCode) {
        String code = orderCode == null ? "" : orderCode.trim();
        return "XE-" + code;
    }
}
