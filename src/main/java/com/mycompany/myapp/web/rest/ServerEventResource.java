package com.mycompany.myapp.web.rest;

import com.mycompany.myapp.security.ScreenKey;
import com.mycompany.myapp.security.SecurityUtils;
import com.mycompany.myapp.security.StaffAccessService;
import com.mycompany.myapp.service.realtime.ServerEventService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Luồng sự kiện realtime (SSE): event "change" = { orders: [mã đơn], trips: [mã chuyến] };
 * event "autocall-error" (chỉ người có quyền ghi màn Tích hợp) = cuộc gọi tự động bị lỗi tổng đài / gửi lỗi.
 */
@RestController
@RequestMapping("/api/events")
public class ServerEventResource {

    private final ServerEventService serverEventService;
    private final StaffAccessService staffAccessService;

    public ServerEventResource(ServerEventService serverEventService, StaffAccessService staffAccessService) {
        this.serverEventService = serverEventService;
        this.staffAccessService = staffAccessService;
    }

    @GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("X-Accel-Buffering", "no");
        String login = SecurityUtils.getCurrentUserLogin().orElse(null);
        String officeCode = staffAccessService.scopedOfficeCode().orElse(null);
        return serverEventService.subscribe(login, officeCode, canWriteIntegration());
    }

    private boolean canWriteIntegration() {
        try {
            staffAccessService.requireScreenWrite(ScreenKey.TICH_HOP);
            return true;
        } catch (ResponseStatusException e) {
            return false;
        }
    }
}
