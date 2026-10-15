package com.mycompany.myapp.web.rest;

import com.mycompany.myapp.security.ClientIpResolver;
import com.mycompany.myapp.service.payment.onepay.OnePayAdminService;
import com.mycompany.myapp.service.payment.onepay.OnePayCallLogService;
import com.mycompany.myapp.service.payment.onepay.OnePayIpnService;
import com.mycompany.myapp.service.payment.onepay.OnePayIpnService.IpnRequest;
import com.mycompany.myapp.service.payment.onepay.OnePayIpnService.Outcome;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** OnePay gọi vào (không đăng nhập): xác thực bằng chữ ký OWS trong {@link OnePayIpnService}. */
@RestController
public class OnePayIpnResource {

    private static final Logger LOG = LoggerFactory.getLogger(OnePayIpnResource.class);

    private final OnePayIpnService ipnService;
    private final OnePayCallLogService callLogService;

    public OnePayIpnResource(OnePayIpnService ipnService, OnePayCallLogService callLogService) {
        this.ipnService = ipnService;
        this.callLogService = callLogService;
    }

    @RequestMapping(
        value = OnePayAdminService.IPN_PATH,
        method = { RequestMethod.PUT, RequestMethod.POST },
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<Map<String, Object>> ipn(
        @RequestParam(value = "id", required = false) String id,
        @RequestBody(required = false) byte[] rawBody,
        HttpServletRequest http
    ) {
        long started = System.currentTimeMillis();
        byte[] body = rawBody == null ? new byte[0] : rawBody;
        Map<String, String> headers = new HashMap<>();
        for (String name : Collections.list(http.getHeaderNames())) {
            headers.put(name.toLowerCase(Locale.ROOT), http.getHeader(name));
        }
        Outcome outcome;
        try {
            outcome = ipnService.handle(new IpnRequest(http.getMethod(), requestUrl(http), http.getQueryString(), id, headers, body));
        } catch (RuntimeException e) {
            LOG.warn("OnePay IPN {} failed: {}", id, e.getMessage());
            outcome = new Outcome(500, false, "SYSTEM_ERROR", "Lỗi hệ thống: " + e.getMessage(), null, id, null);
        }
        callLogService.record(
            new OnePayCallLogService.Entry(
                "IN",
                "IPN",
                outcome.httpStatus(),
                outcome.result(),
                outcome.message(),
                outcome.reference(),
                outcome.transId(),
                outcome.amount(),
                ClientIpResolver.resolve(http),
                System.currentTimeMillis() - started,
                http.getMethod() +
                " " +
                http.getRequestURI() +
                (http.getQueryString() == null ? "" : "?" + http.getQueryString()) +
                "\n" +
                new String(body, StandardCharsets.UTF_8),
                outcome.body().toString()
            )
        );
        return ResponseEntity.status(outcome.httpStatus()).contentType(MediaType.APPLICATION_JSON).body(outcome.body());
    }

    /** URL công khai (không query) như OnePay nhìn thấy — sau proxy lấy theo X-Forwarded-*. */
    private static String requestUrl(HttpServletRequest http) {
        String proto = first(http.getHeader("X-Forwarded-Proto"));
        String host = first(http.getHeader("X-Forwarded-Host"));
        if (host == null) {
            host = http.getHeader("Host");
        }
        if (proto == null) {
            proto = http.getScheme();
        }
        return host == null ? http.getRequestURL().toString() : proto + "://" + host + http.getRequestURI();
    }

    private static String first(String v) {
        if (v == null || v.isBlank()) {
            return null;
        }
        int comma = v.indexOf(',');
        return (comma < 0 ? v : v.substring(0, comma)).trim();
    }
}
