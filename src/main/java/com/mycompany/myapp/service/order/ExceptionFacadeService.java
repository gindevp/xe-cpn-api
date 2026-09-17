package com.mycompany.myapp.service.order;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.myapp.domain.OrderIssue;
import com.mycompany.myapp.domain.OrderPodPhoto;
import com.mycompany.myapp.domain.OrderReturnRequest;
import com.mycompany.myapp.domain.ShipmentOrder;
import com.mycompany.myapp.domain.enumeration.ApprovalStatus;
import com.mycompany.myapp.domain.enumeration.ForwardStage;
import com.mycompany.myapp.domain.enumeration.IssueStatus;
import com.mycompany.myapp.domain.enumeration.IssueType;
import com.mycompany.myapp.domain.enumeration.OrderStatus;
import com.mycompany.myapp.domain.enumeration.ReturnStage;
import com.mycompany.myapp.domain.enumeration.RoleCode;
import com.mycompany.myapp.repository.OrderIssueRepository;
import com.mycompany.myapp.repository.OrderPodPhotoRepository;
import com.mycompany.myapp.repository.OrderReturnRequestRepository;
import com.mycompany.myapp.repository.ShipmentOrderRepository;
import com.mycompany.myapp.security.PermissionService;
import com.mycompany.myapp.security.SecurityUtils;
import com.mycompany.myapp.security.StaffAccessService;
import com.mycompany.myapp.service.day.DayClosureGuard;
import com.mycompany.myapp.service.dto.order.OrderDetailDTO;
import com.mycompany.myapp.service.dto.order.OrderTransitionRequest;
import com.mycompany.myapp.service.dto.order.ReturnCompleteRequest;
import com.mycompany.myapp.web.rest.errors.BadRequestAlertException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class ExceptionFacadeService {

    private static final String ENTITY = "order";
    private static final Pattern FROM_STAGE_SUFFIX = Pattern.compile("\\s*\\|\\s*FROM=(WH_IN|DEST_WH_IN)\\s*$");
    private static final ObjectMapper JSON = new ObjectMapper();

    private final ShipmentOrderRepository shipmentOrderRepository;
    private final OrderIssueRepository orderIssueRepository;
    private final OrderReturnRequestRepository orderReturnRequestRepository;
    private final OrderPodPhotoRepository podPhotoRepository;
    private final OrderFacadeService orderFacadeService;
    private final DayClosureGuard dayClosureGuard;
    private final StaffAccessService staffAccessService;
    private final PermissionService permissionService;

    public ExceptionFacadeService(
        ShipmentOrderRepository shipmentOrderRepository,
        OrderIssueRepository orderIssueRepository,
        OrderReturnRequestRepository orderReturnRequestRepository,
        OrderPodPhotoRepository podPhotoRepository,
        OrderFacadeService orderFacadeService,
        DayClosureGuard dayClosureGuard,
        StaffAccessService staffAccessService,
        PermissionService permissionService
    ) {
        this.shipmentOrderRepository = shipmentOrderRepository;
        this.orderIssueRepository = orderIssueRepository;
        this.orderReturnRequestRepository = orderReturnRequestRepository;
        this.podPhotoRepository = podPhotoRepository;
        this.orderFacadeService = orderFacadeService;
        this.dayClosureGuard = dayClosureGuard;
        this.staffAccessService = staffAccessService;
        this.permissionService = permissionService;
    }

    public OrderDetailDTO startReturn(String orderCode, String reason) {
        ShipmentOrder order = requireOrder(orderCode);
        dayClosureGuard.assertOrderMutable(order);

        if (!canStartReturnRole()) {
            throw new BadRequestAlertException("Only AD/DH can start return", ENTITY, "returnForbidden");
        }
        if (order.getStatus() == OrderStatus.RETURNING) {
            throw new BadRequestAlertException("Order already in return flow", ENTITY, "returnAlreadyActive");
        }
        // Chỉ sau khi đã vào kho gửi trở đi (không DRAFT). Cho phép hoàn sau giao (DELIVERED).
        if (
            order.getStatus() != OrderStatus.CONFIRMED &&
            order.getStatus() != OrderStatus.WAITING &&
            order.getStatus() != OrderStatus.IN_TRANSIT &&
            order.getStatus() != OrderStatus.AT_DEST &&
            order.getStatus() != OrderStatus.OUT_FOR_DELIVERY &&
            order.getStatus() != OrderStatus.FAILED_DELIVERY &&
            order.getStatus() != OrderStatus.DELIVERED &&
            order.getStatus() != OrderStatus.RETURNED
        ) {
            throw new BadRequestAlertException("Return not allowed from status " + order.getStatus(), ENTITY, "returnInvalidStatus");
        }

        ForwardStage current = order.getForwardStage();
        // Case A: đang nhập kho gửi → nhảy nhập kho giao cùng VP (hoàn tại chỗ).
        // Case B: kho đích / fail / đang giao → về nhập kho gửi chiều hoàn (vận chuyển về VP gốc).
        boolean caseAtOriginWh =
            current == ForwardStage.WH_IN ||
            (current == null && order.getStatus() == OrderStatus.CONFIRMED && order.getPickedUpAt() != null);
        ForwardStage nextForward = caseAtOriginWh ? ForwardStage.DEST_WH_IN : ForwardStage.WH_IN;

        OrderReturnRequest req = new OrderReturnRequest();
        req.setReason(reason == null || reason.isBlank() ? "RETURN" : reason.trim());
        req.setRequestedByUsername(actor());
        req.setRequestedAt(Instant.now());
        req.setStatus(ApprovalStatus.APPROVED);
        req.setDecidedByUsername(actor());
        req.setDecidedAt(Instant.now());
        req.setOrder(order);
        req = orderReturnRequestRepository.save(req);
        order.setReturnRequest(req);
        order.setReturnStage(ReturnStage.RETURN_PENDING);
        // COD + phí thu hộ COD = 0; cước giữ nguyên.
        order.setCodAmount(java.math.BigDecimal.ZERO);
        order.setCodFeeAmount(java.math.BigDecimal.ZERO);
        order.setForwardStage(nextForward);
        if (caseAtOriginWh) {
            // Hoàn tại chỗ: không còn trên chuyến.
            order.setCurrentTrip(null);
        }
        shipmentOrderRepository.save(order);

        if (order.getStatus() != OrderStatus.RETURNING) {
            OrderTransitionRequest tr = new OrderTransitionRequest();
            tr.setToStatus(OrderStatus.RETURNING);
            tr.setAction("RETURN_START");
            tr.setDetail(req.getReason());
            orderFacadeService.transition(order.getOrderCode(), tr);
            // transition không còn xóa forwardStage khi → RETURNING; re-apply nếu cần.
            ShipmentOrder reloaded = requireOrder(orderCode);
            if (reloaded.getForwardStage() != nextForward) {
                reloaded.setForwardStage(nextForward);
                shipmentOrderRepository.save(reloaded);
            }
        }
        return orderFacadeService.getByCode(order.getOrderCode());
    }

    private boolean canStartReturnRole() {
        if (permissionService.isSystemAdmin()) {
            return true;
        }
        return staffAccessService.current().map(p -> p.getRoleCode() == RoleCode.AD || p.getRoleCode() == RoleCode.DH).orElse(false);
    }

    public OrderDetailDTO setReturnStage(String orderCode, ReturnStage stage) {
        ShipmentOrder order = requireOrder(orderCode);
        dayClosureGuard.assertOrderMutable(order);
        if (order.getStatus() != OrderStatus.RETURNING && stage != ReturnStage.RT_DONE) {
            throw new BadRequestAlertException("returnStage requires RETURNING", ENTITY, "returnStageStatus");
        }
        order.setReturnStage(stage);
        shipmentOrderRepository.save(order);
        if (stage == ReturnStage.RT_DONE && order.getStatus() == OrderStatus.RETURNING) {
            OrderTransitionRequest tr = new OrderTransitionRequest();
            tr.setToStatus(OrderStatus.RETURNED);
            tr.setAction("RT_DONE");
            String office = staffAccessService.scopedOfficeCode().orElse("");
            tr.setDetail(office.isBlank() ? "Hoàn thành công" : "Hoàn thành công · VP=" + office);
            orderFacadeService.transition(order.getOrderCode(), tr);
        }
        return orderFacadeService.getByCode(order.getOrderCode());
    }

    public OrderDetailDTO completeReturn(String orderCode) {
        return completeReturn(orderCode, null);
    }

    public OrderDetailDTO completeReturn(String orderCode, ReturnCompleteRequest req) {
        ShipmentOrder order = requireOrder(orderCode);
        dayClosureGuard.assertOrderMutable(order);

        List<String> photos = req != null ? req.getPhotos() : null;
        if (photos == null || photos.isEmpty()) {
            throw new BadRequestAlertException("At least 1 return photo required", ENTITY, "returnPhotoRequired");
        }
        if (photos.size() > 3) {
            throw new BadRequestAlertException("Max 3 return photos", ENTITY, "returnPhotoMax");
        }

        Instant now = Instant.now();
        String actor = actor();
        if (req.getActualRecipientName() != null && !req.getActualRecipientName().isBlank()) {
            order.setReceiverActualName(req.getActualRecipientName().trim());
        }
        if (req.getActualRecipientPhone() != null && !req.getActualRecipientPhone().isBlank()) {
            order.setReceiverActualPhone(req.getActualRecipientPhone().trim());
        }

        int seq = (int) podPhotoRepository.countByOrder_Id(order.getId()) + 1;
        for (String photo : photos) {
            OrderPodPhoto row = new OrderPodPhoto();
            row.setPhotoUrl(truncateUrl(photo));
            row.setCapturedAt(now);
            row.setCapturedByUsername(actor);
            row.setSequenceNo(Math.min(seq, 3));
            row.setOrder(order);
            podPhotoRepository.save(row);
            seq++;
        }
        shipmentOrderRepository.save(order);

        return setReturnStage(orderCode, ReturnStage.RT_DONE);
    }

    private static String truncateUrl(String url) {
        if (url == null) {
            return "";
        }
        String trimmed = url.trim();
        final int max = 1_500_000;
        return trimmed.length() <= max ? trimmed : trimmed.substring(0, max);
    }

    public OrderDetailDTO openIssue(String orderCode, IssueType type, String reason) {
        return openIssue(orderCode, type, reason, List.of());
    }

    public OrderDetailDTO openIssue(String orderCode, IssueType type, String reason, List<String> photos) {
        ShipmentOrder order = requireOrder(orderCode);
        dayClosureGuard.assertOrderMutable(order);
        IssueType issueType = type != null ? type : IssueType.EXCEPTION;

        if (issueType == IssueType.LOST || issueType == IssueType.EXCEPTION || issueType == IssueType.DAMAGED) {
            if (order.getStatus() == OrderStatus.DELIVERED || order.getStatus() == OrderStatus.RETURNED) {
                throw new BadRequestAlertException("Cannot open issue on delivered/returned order", ENTITY, "issueOnSuccess");
            }
        }

        if (issueType == IssueType.EXCEPTION || issueType == IssueType.DAMAGED) {
            String note = reason == null ? "" : FROM_STAGE_SUFFIX.matcher(reason).replaceFirst("").trim();
            if (note.isEmpty()) {
                throw new BadRequestAlertException("Issue reason required", ENTITY, "issueReasonRequired");
            }
        }

        if (orderIssueRepository.existsByOrder_IdAndIssueStatus(order.getId(), IssueStatus.OPEN)) {
            // AD ghi nhận ngoại lệ/thất lạc/hư hỏng: đóng vụ việc mở rồi tạo mới.
            boolean adminIssue =
                (issueType == IssueType.LOST || issueType == IssueType.EXCEPTION || issueType == IssueType.DAMAGED) &&
                (staffAccessService.current().map(p -> p.getRoleCode() == RoleCode.AD).orElse(false) || permissionService.isSystemAdmin());
            if (!adminIssue) {
                throw new BadRequestAlertException("Order already has an open issue", ENTITY, "issueOpenExists");
            }
            for (OrderIssue open : orderIssueRepository.findByOrder_IdOrderByOpenedAtAscIdAsc(order.getId())) {
                if (open.getIssueStatus() == IssueStatus.OPEN) {
                    open.setIssueStatus(IssueStatus.RESOLVED);
                    open.setResolvedAt(Instant.now());
                    open.setResolvedByUsername(actor());
                    open.setResolutionNote("Đóng để ghi nhận " + issueType.name() + " (AD)");
                    orderIssueRepository.save(open);
                }
            }
            order.setIssue(null);
        }

        String clippedReason = reason;
        if (clippedReason != null && clippedReason.length() > 1000) {
            clippedReason = clippedReason.substring(0, 1000);
        }

        OrderIssue issue = new OrderIssue();
        issue.setIssueType(issueType);
        issue.setIssueStatus(IssueStatus.OPEN);
        issue.setReason(clippedReason);
        issue.setEvidencePhotos(encodePhotos(photos));
        issue.setOpenedAt(Instant.now());
        issue.setOpenedByUsername(actor());
        issue.setOrder(order);
        try {
            issue = orderIssueRepository.saveAndFlush(issue);
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            throw new BadRequestAlertException("Order already has an open issue", ENTITY, "issueOpenExists");
        }
        order.setIssue(issue);
        shipmentOrderRepository.save(order);
        return orderFacadeService.getByCode(order.getOrderCode());
    }

    public OrderDetailDTO resolveIssue(String orderCode, String note) {
        ShipmentOrder order = requireOrder(orderCode);
        dayClosureGuard.assertOrderMutable(order);
        OrderIssue issue = order.getIssue();
        if (issue == null) {
            throw new BadRequestAlertException("No issue on order", ENTITY, "issueMissing");
        }
        if (issue.getIssueStatus() != IssueStatus.OPEN) {
            throw new BadRequestAlertException("Current issue is not OPEN", ENTITY, "issueNotOpen");
        }
        issue.setIssueStatus(IssueStatus.RESOLVED);
        issue.setResolvedAt(Instant.now());
        issue.setResolvedByUsername(actor());
        issue.setResolutionNote(note);
        orderIssueRepository.save(issue);
        return orderFacadeService.getByCode(order.getOrderCode());
    }

    @Transactional(readOnly = true)
    public java.util.List<OrderDetailDTO.OrderIssueViewDTO> listIssues(String orderCode) {
        ShipmentOrder order = requireOrder(orderCode);
        return orderIssueRepository
            .findByOrder_IdOrderByOpenedAtAscIdAsc(order.getId())
            .stream()
            .map(ExceptionFacadeService::toIssueView)
            .toList();
    }

    @Transactional(readOnly = true)
    public java.util.List<OrderDetailDTO.OrderReturnViewDTO> listReturns(String orderCode) {
        ShipmentOrder order = requireOrder(orderCode);
        return orderReturnRequestRepository
            .findByOrder_IdOrderByRequestedAtAscIdAsc(order.getId())
            .stream()
            .map(ExceptionFacadeService::toReturnView)
            .toList();
    }

    static OrderDetailDTO.OrderIssueViewDTO toIssueView(OrderIssue issue) {
        OrderDetailDTO.OrderIssueViewDTO v = new OrderDetailDTO.OrderIssueViewDTO();
        v.setId(issue.getId());
        v.setIssueType(issue.getIssueType() != null ? issue.getIssueType().name() : null);
        v.setIssueStatus(issue.getIssueStatus() != null ? issue.getIssueStatus().name() : null);
        v.setReason(issue.getReason());
        v.setOpenedAt(issue.getOpenedAt());
        v.setOpenedByUsername(issue.getOpenedByUsername());
        v.setResolvedAt(issue.getResolvedAt());
        v.setResolvedByUsername(issue.getResolvedByUsername());
        v.setResolutionNote(issue.getResolutionNote());
        v.setPhotos(decodePhotos(issue.getEvidencePhotos()));
        return v;
    }

    private String encodePhotos(List<String> photos) {
        List<String> cleaned = normalizePhotos(photos);
        if (cleaned.isEmpty()) {
            return null;
        }
        try {
            return JSON.writeValueAsString(cleaned);
        } catch (Exception e) {
            throw new BadRequestAlertException("Invalid issue photos", ENTITY, "issuePhotosInvalid");
        }
    }

    private static List<String> decodePhotos(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        try {
            List<String> list = JSON.readValue(raw, new TypeReference<List<String>>() {});
            return list != null ? list : List.of();
        } catch (Exception e) {
            return List.of();
        }
    }

    private List<String> normalizePhotos(List<String> photos) {
        if (photos == null || photos.isEmpty()) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        for (String p : photos) {
            if (p == null) continue;
            String clipped = truncateUrl(p);
            if (!clipped.isBlank()) {
                out.add(clipped);
            }
            if (out.size() >= 3) break;
        }
        return out;
    }

    static OrderDetailDTO.OrderReturnViewDTO toReturnView(OrderReturnRequest req) {
        OrderDetailDTO.OrderReturnViewDTO v = new OrderDetailDTO.OrderReturnViewDTO();
        v.setId(req.getId());
        v.setReason(req.getReason());
        v.setStatus(req.getStatus() != null ? req.getStatus().name() : null);
        v.setRequestedAt(req.getRequestedAt());
        v.setRequestedByUsername(req.getRequestedByUsername());
        v.setDecidedAt(req.getDecidedAt());
        v.setDecidedByUsername(req.getDecidedByUsername());
        v.setDecisionNote(req.getDecisionNote());
        return v;
    }

    public OrderDetailDTO setForwardStage(String orderCode, ForwardStage stage) {
        ShipmentOrder order = requireOrder(orderCode);
        dayClosureGuard.assertOrderMutable(order);
        order.setForwardStage(stage);
        Instant now = Instant.now();
        if (stage == ForwardStage.PICKED && order.getPickingAt() == null) {
            order.setPickingAt(now);
            if (order.getPickupStaffUsername() == null) {
                order.setPickupStaffUsername(actor());
            }
        }
        if ((stage == ForwardStage.WH_IN || stage == ForwardStage.DEST_WH_IN) && order.getPickedUpAt() == null) {
            order.setPickedUpAt(now);
        }
        shipmentOrderRepository.save(order);
        return orderFacadeService.getByCode(order.getOrderCode());
    }

    private ShipmentOrder requireOrder(String code) {
        return shipmentOrderRepository
            .findOneByOrderCodeOrDraftCode(code.trim())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found: " + code));
    }

    private static String actor() {
        return SecurityUtils.getCurrentUserLogin().orElse("system");
    }
}
