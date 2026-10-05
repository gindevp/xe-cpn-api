package com.mycompany.myapp.service;

import com.mycompany.myapp.domain.Office;
import com.mycompany.myapp.domain.Shipper;
import com.mycompany.myapp.repository.OfficeRepository;
import com.mycompany.myapp.repository.ShipmentOrderRepository;
import com.mycompany.myapp.repository.ShipperRepository;
import com.mycompany.myapp.service.dto.ShipperDTO;
import com.mycompany.myapp.web.rest.errors.BadRequestAlertException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ShipperService {

    private static final String ENTITY = "shipper";

    private final ShipperRepository shipperRepository;
    private final OfficeRepository officeRepository;
    private final ShipmentOrderRepository orderRepository;

    public ShipperService(ShipperRepository shipperRepository, OfficeRepository officeRepository, ShipmentOrderRepository orderRepository) {
        this.shipperRepository = shipperRepository;
        this.officeRepository = officeRepository;
        this.orderRepository = orderRepository;
    }

    @Transactional(readOnly = true)
    public List<ShipperDTO> list(String officeCode, boolean includeInactive) {
        List<Shipper> rows = officeCode == null || officeCode.isBlank()
            ? shipperRepository.findAllByOrderByFullNameAsc()
            : shipperRepository.findByOffice_CodeOrderByFullNameAsc(officeCode.trim());
        if (!includeInactive) {
            rows = rows.stream().filter(s -> Boolean.TRUE.equals(s.getActive())).toList();
        }
        Map<Long, Long> busy = new HashMap<>();
        if (!rows.isEmpty()) {
            for (Object[] r : orderRepository.countOutForDeliveryByShipper(rows.stream().map(Shipper::getId).toList())) {
                busy.put((Long) r[0], ((Number) r[1]).longValue());
            }
        }
        return rows.stream().map(s -> toDto(s, busy.getOrDefault(s.getId(), 0L))).toList();
    }

    public ShipperDTO create(ShipperDTO in) {
        Shipper s = new Shipper();
        apply(s, in);
        if (s.getActive() == null) {
            s.setActive(true);
        }
        return toDto(shipperRepository.save(s), 0L);
    }

    public ShipperDTO update(Long id, ShipperDTO in) {
        Shipper s = shipperRepository
            .findById(id)
            .orElseThrow(() -> new BadRequestAlertException("Không tìm thấy shipper", ENTITY, "idnotfound"));
        apply(s, in);
        return toDto(shipperRepository.save(s), 0L);
    }

    /** Ngừng hoạt động (giữ bản ghi vì đơn cũ còn tham chiếu). */
    public void deactivate(Long id) {
        Shipper s = shipperRepository
            .findById(id)
            .orElseThrow(() -> new BadRequestAlertException("Không tìm thấy shipper", ENTITY, "idnotfound"));
        s.setActive(false);
        shipperRepository.save(s);
    }

    private void apply(Shipper s, ShipperDTO in) {
        String name = in.getFullName() == null ? "" : in.getFullName().trim();
        if (name.isEmpty()) {
            throw new BadRequestAlertException("Nhập tên shipper", ENTITY, "nameRequired");
        }
        s.setFullName(name.length() > 100 ? name.substring(0, 100) : name);
        String phone = in.getPhone() == null ? null : in.getPhone().trim();
        s.setPhone(phone == null || phone.isEmpty() ? null : phone);
        String note = in.getNote() == null ? null : in.getNote().trim();
        s.setNote(note == null || note.isEmpty() ? null : note.length() > 255 ? note.substring(0, 255) : note);
        if (in.getOfficeCode() != null && !in.getOfficeCode().isBlank()) {
            Office office = officeRepository
                .findOneByCodeIgnoreCase(in.getOfficeCode())
                .orElseThrow(() -> new BadRequestAlertException("Không tìm thấy VP " + in.getOfficeCode(), ENTITY, "officeNotFound"));
            s.setOffice(office);
        } else if (s.getOffice() == null) {
            throw new BadRequestAlertException("Chọn VP cho shipper", ENTITY, "officeRequired");
        }
        if (in.getActive() != null) {
            s.setActive(in.getActive());
        }
    }

    public static ShipperDTO toDto(Shipper s, long busyCount) {
        ShipperDTO d = new ShipperDTO();
        d.setId(s.getId());
        d.setFullName(s.getFullName());
        d.setPhone(s.getPhone());
        if (s.getOffice() != null) {
            d.setOfficeCode(s.getOffice().getCode());
            d.setOfficeName(s.getOffice().getName());
        }
        d.setActive(s.getActive());
        d.setNote(s.getNote());
        d.setBusyCount(busyCount);
        return d;
    }
}
