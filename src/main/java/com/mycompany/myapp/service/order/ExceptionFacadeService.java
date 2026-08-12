package com.mycompany.myapp.service.order;

import com.mycompany.myapp.domain.OrderIssue;
import com.mycompany.myapp.domain.OrderReturnRequest;
import com.mycompany.myapp.domain.ShipmentOrder;
import com.mycompany.myapp.domain.enumeration.ApprovalStatus;
import com.mycompany.myapp.domain.enumeration.ForwardStage;
import com.mycompany.myapp.domain.enumeration.IssueStatus;
import com.mycompany.myapp.domain.enumeration.IssueType;
import com.mycompany.myapp.domain.enumeration.OrderStatus;
import com.mycompany.myapp.domain.enumeration.ReturnStage;
import com.mycompany.myapp.repository.OrderIssueRepository;
import com.mycompany.myapp.repository.OrderReturnRequestRepository;
import com.mycompany.myapp.repository.ShipmentOrderRepository;
import com.mycompany.myapp.security.SecurityUtils;
import com.mycompany.myapp.service.day.DayClosureGuard;
import com.mycompany.myapp.service.dto.order.OrderDetailDTO;
import com.mycompany.myapp.service.dto.order.OrderTransitionRequest;
import com.mycompany.myapp.web.rest.errors.BadRequestAlertException;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class ExceptionFacadeService {

    private static final String ENTITY = "order";

    private final ShipmentOrderRepository shipmentOrderRepository;
    private final OrderIssueRepository orderIssueRepository;
    private final OrderReturnRequestRepository orderReturnRequestRepository;
    private final OrderFacadeService orderFacadeService;
    private final DayClosureGuard dayClosureGuard;

    public ExceptionFacadeService(
        ShipmentOrderRepository shipmentOrderRepository,
        OrderIssueRepository orderIssueRepository,
        OrderReturnRequestRepository orderReturnRequestRepository,
        OrderFacadeService orderFacadeService,
        DayClosureGuard dayClosureGuard
    ) {
        this.shipmentOrderRepository = shipmentOrderRepository;
        this.orderIssueRepository = orderIssueRepository;
        this.orderReturnRequestRepository = orderReturnRequestRepository;
        this.orderFacadeService = orderFacadeService;
        this.dayClosureGuard = dayClosureGuard;
    }

    public OrderDetailDTO startReturn(String orderCode, String reason) {
        ShipmentOrder order = requireOrder(orderCode);
        dayClosureGuard.assertOrderMutable(order);
        if (
            order.getStatus() != OrderStatus.AT_DEST &&
            order.getStatus() != OrderStatus.DELIVERED &&
            order.getStatus() != OrderStatus.FAILED_DELIVERY &&
            order.getStatus() != OrderStatus.RETURNED
        ) {
            throw new BadRequestAlertException(
                "Return only from AT_DEST/DELIVERED/FAILED_DELIVERY/RETURNED",
                ENTITY,
                "returnInvalidStatus"
            );
        }
        OrderReturnRequest req = new OrderReturnRequest();
        req.setReason(reason == null || reason.isBlank() ? "RETURN" : reason.trim());
        req.setRequestedByUsername(actor());
        req.setRequestedAt(Instant.now());
        req.setStatus(ApprovalStatus.APPROVED);
        req.setDecidedByUsername(actor());
        req.setDecidedAt(Instant.now());
        req.setOrder(order); // history link (TASK-008)
        req = orderReturnRequestRepository.save(req);
        order.setReturnRequest(req); // current pointer
        order.setReturnStage(ReturnStage.RETURN_PENDING);
        shipmentOrderRepository.save(order);

        if (order.getStatus() != OrderStatus.RETURNING) {
            OrderTransitionRequest tr = new OrderTransitionRequest();
            tr.setToStatus(OrderStatus.RETURNING);
            tr.setAction("RETURN_START");
            tr.setDetail(req.getReason());
            orderFacadeService.transition(order.getOrderCode(), tr);
        }
        return orderFacadeService.getByCode(order.getOrderCode());
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
            tr.setDetail("Return completed");
            orderFacadeService.transition(order.getOrderCode(), tr);
        }
        return orderFacadeService.getByCode(order.getOrderCode());
    }

    public OrderDetailDTO completeReturn(String orderCode) {
        return setReturnStage(orderCode, ReturnStage.RT_DONE);
    }

    public OrderDetailDTO openIssue(String orderCode, IssueType type, String reason) {
        ShipmentOrder order = requireOrder(orderCode);
        dayClosureGuard.assertOrderMutable(order);
        if (orderIssueRepository.existsByOrder_IdAndIssueStatus(order.getId(), IssueStatus.OPEN)) {
            throw new BadRequestAlertException("Order already has an open issue", ENTITY, "issueOpenExists");
        }
        OrderIssue issue = new OrderIssue();
        issue.setIssueType(type != null ? type : IssueType.EXCEPTION);
        issue.setIssueStatus(IssueStatus.OPEN);
        issue.setReason(reason);
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
        return v;
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
