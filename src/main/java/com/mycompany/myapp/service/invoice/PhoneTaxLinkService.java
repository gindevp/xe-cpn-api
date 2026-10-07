package com.mycompany.myapp.service.invoice;

import com.mycompany.myapp.domain.PhoneTaxLink;
import com.mycompany.myapp.domain.ShipmentOrder;
import com.mycompany.myapp.repository.PhoneTaxLinkRepository;
import com.mycompany.myapp.repository.ShipmentOrderRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * MST gắn với SĐT, tối đa {@link #MAX_PER_PHONE}. CRM thêm/sửa/xóa vào bảng này.
 * Xuất hóa đơn doanh nghiệp thì gắn MST vào SĐT người trả cước (theo hình thức thanh toán)
 * và nếu đã đủ 5 thì bỏ MST cũ nhất.
 * Lần đầu bảng trống thì lấy từ hóa đơn đã xuất.
 */
@Service
public class PhoneTaxLinkService {

    private static final Logger LOG = LoggerFactory.getLogger(PhoneTaxLinkService.class);
    public static final int MAX_PER_PHONE = 5;
    private static final int DIRECTORY_PHONES = 200;
    private static final int BACKFILL_SCAN = 2000;
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private final PhoneTaxLinkRepository linkRepository;
    private final ShipmentOrderRepository shipmentOrderRepository;
    private volatile boolean backfilled;
    private volatile boolean payerAudited;

    public PhoneTaxLinkService(PhoneTaxLinkRepository linkRepository, ShipmentOrderRepository shipmentOrderRepository) {
        this.linkRepository = linkRepository;
        this.shipmentOrderRepository = shipmentOrderRepository;
    }

    public record BuyerDirectoryEntry(String phone, String name, List<Map<String, String>> profiles) {}

    @Transactional(readOnly = true)
    public Optional<Map<String, String>> profile(String phone) {
        return profiles(phone).stream().findFirst();
    }

    @Transactional
    public List<Map<String, String>> profiles(String phone) {
        backfillOnce();
        auditPayerLinksOnce();
        String canonical = canonicalPhone(phone);
        if (canonical == null) {
            return List.of();
        }
        return linkRepository.findByPhoneOrderByUpdatedAtDescIdDesc(canonical).stream().map(PhoneTaxLinkService::toMap).toList();
    }

    @Transactional
    public List<BuyerDirectoryEntry> directory(String query) {
        backfillOnce();
        auditPayerLinksOnce();
        String canonical = canonicalPhone(query);
        if (canonical != null) {
            List<PhoneTaxLink> rows = linkRepository.findByPhoneOrderByUpdatedAtDescIdDesc(canonical);
            if (rows.isEmpty()) {
                return List.of();
            }
            return List.of(entryOf(rows));
        }
        if (query != null && !query.isBlank()) {
            return List.of();
        }
        Map<String, List<PhoneTaxLink>> grouped = new LinkedHashMap<>();
        for (PhoneTaxLink row : linkRepository.findAllByOrderByUpdatedAtDesc(PageRequest.of(0, DIRECTORY_PHONES * MAX_PER_PHONE))) {
            List<PhoneTaxLink> bucket = grouped.get(row.getPhone());
            if (bucket == null) {
                if (grouped.size() >= DIRECTORY_PHONES) {
                    continue;
                }
                bucket = new ArrayList<>();
                grouped.put(row.getPhone(), bucket);
            }
            bucket.add(row);
        }
        List<BuyerDirectoryEntry> out = new ArrayList<>();
        for (List<PhoneTaxLink> rows : grouped.values()) {
            out.add(entryOf(rows));
        }
        return out;
    }

    @Transactional
    public Map<String, String> create(String phone, String taxCode, String companyName, String address, String email, String contactName) {
        backfillOnce();
        auditPayerLinksOnce();
        return toMap(saveLink(phone, contactName, taxCode, companyName, address, email, null, false));
    }

    @Transactional
    public Map<String, String> update(Long id, String taxCode, String companyName, String address, String email, String contactName) {
        PhoneTaxLink row = linkRepository
            .findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không thấy MST này"));
        String tax = requireTax(taxCode);
        String company = requireCompany(companyName);
        linkRepository
            .findByPhoneAndTaxCode(row.getPhone(), tax)
            .filter(other -> !other.getId().equals(id))
            .ifPresent(other -> {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "SĐT này đã gắn MST " + tax);
            });
        row.setTaxCode(tax);
        row.setCompanyName(company);
        row.setAddress(blankToNull(address, 500));
        row.setEmail(optionalEmail(email, false));
        if (contactName != null && !contactName.isBlank()) {
            row.setContactName(clip(contactName.trim(), 100));
        }
        row.setUpdatedAt(Instant.now());
        return toMap(linkRepository.save(row));
    }

    @Transactional
    public void delete(Long id) {
        if (!linkRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không thấy MST này");
        }
        linkRepository.deleteById(id);
    }

    /** Gắn MST của hóa đơn doanh nghiệp vừa xuất vào SĐT người trả cước. Lỗi ở đây không được hoàn tác hóa đơn. */
    @Transactional
    public void rememberIssued(ShipmentOrder order) {
        try {
            backfillOnce();
            auditPayerLinksOnce();
            linkIssued(order);
        } catch (RuntimeException e) {
            LOG.warn("Không lưu được MST theo SĐT của đơn {}: {}", order == null ? "" : order.getOrderCode(), e.toString());
        }
    }

    private void linkIssued(ShipmentOrder order) {
        if (!issuedCompany(order)) {
            return;
        }
        saveLink(
            InvoicePolicy.payerPhone(order),
            InvoicePolicy.payerName(order),
            VietnamTaxCode.normalize(order.getInvoiceTaxCode()),
            order.getInvoiceCompanyName(),
            order.getInvoiceCompanyAddress(),
            order.getInvoiceEmail(),
            order.getOrderCode(),
            true
        );
    }

    /**
     * Gỡ MST đang gắn nhầm vào SĐT không phải người trả của đơn nguồn.
     * Gắn tay (không có mã đơn) giữ nguyên. Chạy một lần mỗi phiên.
     */
    private void auditPayerLinksOnce() {
        if (payerAudited) {
            return;
        }
        synchronized (this) {
            if (payerAudited) {
                return;
            }
            try {
                List<PhoneTaxLink> rows = linkRepository.findByFromOrderCodeIsNotNull();
                if (!rows.isEmpty()) {
                    java.util.Map<String, ShipmentOrder> byCode = ordersByCode(rows);
                    for (PhoneTaxLink row : rows) {
                        ShipmentOrder order = byCode.get(row.getFromOrderCode());
                        if (order == null || !issuedCompany(order)) {
                            continue;
                        }
                        String payer = canonicalPhone(InvoicePolicy.payerPhone(order));
                        if (payer != null && payer.equals(row.getPhone())) {
                            continue;
                        }
                        linkRepository.delete(row);
                        if (payer != null && linkRepository.findByPhoneAndTaxCode(payer, row.getTaxCode()).isEmpty()) {
                            linkIssued(order);
                        }
                    }
                }
                payerAudited = true;
            } catch (RuntimeException e) {
                payerAudited = true;
                LOG.warn("Không rà được MST theo người trả cước: {}", e.toString());
            }
        }
    }

    private java.util.Map<String, ShipmentOrder> ordersByCode(List<PhoneTaxLink> rows) {
        java.util.Set<String> codes = new java.util.LinkedHashSet<>();
        for (PhoneTaxLink row : rows) {
            if (row.getFromOrderCode() != null && !row.getFromOrderCode().isBlank()) {
                codes.add(row.getFromOrderCode());
            }
        }
        java.util.Map<String, ShipmentOrder> byCode = new java.util.HashMap<>();
        List<String> list = new ArrayList<>(codes);
        for (int i = 0; i < list.size(); i += 500) {
            List<String> slice = list.subList(i, Math.min(i + 500, list.size()));
            for (ShipmentOrder order : shipmentOrderRepository.findWithOfficesByOrderCodeIn(slice)) {
                if (order.getOrderCode() != null) {
                    byCode.put(order.getOrderCode(), order);
                }
            }
        }
        return byCode;
    }

    private static boolean issuedCompany(ShipmentOrder order) {
        return (
            order != null &&
            VietnamTaxCode.isValid(order.getInvoiceTaxCode()) &&
            order.getInvoiceCompanyName() != null &&
            !order.getInvoiceCompanyName().isBlank()
        );
    }

    private PhoneTaxLink saveLink(
        String rawPhone,
        String contactName,
        String taxCode,
        String companyName,
        String address,
        String email,
        String fromOrderCode,
        boolean evictOldest
    ) {
        String phone = canonicalPhone(rawPhone);
        if (phone == null) {
            if (evictOldest) {
                return null;
            }
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số điện thoại không hợp lệ");
        }
        String tax = requireTax(taxCode);
        String company = requireCompany(companyName);
        Optional<PhoneTaxLink> existing = linkRepository.findByPhoneAndTaxCode(phone, tax);
        if (existing.isPresent()) {
            PhoneTaxLink row = existing.get();
            row.setCompanyName(company);
            row.setAddress(blankToNull(address, 500));
            String mail = optionalEmail(email, evictOldest);
            if (mail != null) {
                row.setEmail(mail);
            }
            if (contactName != null && !contactName.isBlank()) {
                row.setContactName(clip(contactName.trim(), 100));
            }
            if (fromOrderCode != null && !fromOrderCode.isBlank()) {
                row.setFromOrderCode(clip(fromOrderCode.trim(), 40));
            }
            row.setUpdatedAt(Instant.now());
            return linkRepository.save(row);
        }
        if (linkRepository.countByPhone(phone) >= MAX_PER_PHONE) {
            if (!evictOldest) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mỗi SĐT tối đa 5 MST. Xóa bớt rồi thêm lại.");
            }
            List<PhoneTaxLink> oldest = linkRepository.findByPhoneOrderByUpdatedAtAscIdAsc(phone);
            if (!oldest.isEmpty()) {
                linkRepository.delete(oldest.get(0));
            }
        }
        PhoneTaxLink row = new PhoneTaxLink();
        row.setPhone(phone);
        row.setTaxCode(tax);
        row.setCompanyName(company);
        row.setAddress(blankToNull(address, 500));
        row.setEmail(optionalEmail(email, evictOldest));
        if (contactName != null && !contactName.isBlank()) {
            row.setContactName(clip(contactName.trim(), 100));
        }
        if (fromOrderCode != null && !fromOrderCode.isBlank()) {
            row.setFromOrderCode(clip(fromOrderCode.trim(), 40));
        }
        row.setUpdatedAt(Instant.now());
        return linkRepository.save(row);
    }

    private void backfillOnce() {
        if (backfilled) {
            return;
        }
        synchronized (this) {
            if (backfilled) {
                return;
            }
            try {
                if (linkRepository.count() > 0) {
                    backfilled = true;
                    return;
                }
                List<ShipmentOrder> orders = new ArrayList<>(
                    shipmentOrderRepository.findIssuedCompanyInvoices(PageRequest.of(0, BACKFILL_SCAN))
                );
                java.util.Collections.reverse(orders);
                for (ShipmentOrder order : orders) {
                    linkIssued(order);
                }
                backfilled = true;
            } catch (RuntimeException e) {
                backfilled = true;
                LOG.warn("Không nạp được MST từ hóa đơn đã xuất: {}", e.toString());
            }
        }
    }

    private static BuyerDirectoryEntry entryOf(List<PhoneTaxLink> rows) {
        String name = null;
        List<Map<String, String>> profiles = new ArrayList<>();
        for (PhoneTaxLink row : rows) {
            if (name == null && row.getContactName() != null && !row.getContactName().isBlank()) {
                name = row.getContactName();
            }
            profiles.add(toMap(row));
        }
        return new BuyerDirectoryEntry(rows.get(0).getPhone(), name, profiles);
    }

    static Map<String, String> toMap(PhoneTaxLink row) {
        Map<String, String> m = new LinkedHashMap<>();
        if (row.getId() != null) {
            m.put("id", String.valueOf(row.getId()));
        }
        m.put("phone", row.getPhone());
        m.put("taxCode", row.getTaxCode());
        m.put("companyName", row.getCompanyName());
        m.put("address", row.getAddress());
        m.put("email", row.getEmail());
        m.put("fromOrderCode", row.getFromOrderCode());
        if (row.getUpdatedAt() != null) {
            m.put("issuedAt", row.getUpdatedAt().toString());
            m.put("updatedAt", row.getUpdatedAt().toString());
        }
        return m;
    }

    static String canonicalPhone(String raw) {
        List<String> keys = lookupKeys(raw);
        return keys.isEmpty() ? null : keys.get(0);
    }

    private static List<String> lookupKeys(String raw) {
        String d = raw == null ? "" : raw.replaceAll("\\D", "");
        if (d.startsWith("84") && d.length() == 11) {
            d = "0" + d.substring(2);
        }
        if (d.length() < 9) {
            return List.of();
        }
        if (d.startsWith("0") && d.length() >= 10) {
            return List.of(d);
        }
        return List.of(d);
    }

    private static String requireTax(String raw) {
        if (!VietnamTaxCode.isValid(raw)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "MST không hợp lệ");
        }
        return VietnamTaxCode.normalize(raw);
    }

    private static String requireCompany(String raw) {
        String name = raw == null ? "" : raw.trim();
        if (name.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chưa có tên công ty. Tra MST trước khi lưu.");
        }
        return clip(name, 255);
    }

    private static String optionalEmail(String raw, boolean lenient) {
        String mail = blankToNull(raw, 100);
        if (mail != null && !EMAIL.matcher(mail).matches()) {
            if (lenient) {
                return null;
            }
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email không hợp lệ");
        }
        return mail;
    }

    private static String blankToNull(String raw, int max) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return clip(raw.trim(), max);
    }

    private static String clip(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max);
    }
}
