package com.mycompany.myapp.web.rest;

import static com.mycompany.myapp.security.SecurityUtils.AUTHORITIES_KEY;
import static com.mycompany.myapp.security.SecurityUtils.JWT_ALGORITHM;
import static com.mycompany.myapp.security.SecurityUtils.SID_CLAIM;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mycompany.myapp.security.ClientIpResolver;
import com.mycompany.myapp.service.auth.LoginControlService;
import com.mycompany.myapp.service.auth.LoginControlService.LoginDecision;
import com.mycompany.myapp.service.auth.LoginControlService.LoginRequestInfo;
import com.mycompany.myapp.service.config.SessionPolicyService;
import com.mycompany.myapp.web.rest.vm.LoginVM;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.security.Principal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.web.bind.annotation.*;

/**
 * Controller to authenticate users.
 */
@RestController
@RequestMapping("/api")
public class AuthenticateController {

    private static final Logger LOG = LoggerFactory.getLogger(AuthenticateController.class);

    private final JwtEncoder jwtEncoder;

    @Value("${jhipster.security.authentication.jwt.token-validity-in-seconds:0}")
    private long tokenValidityInSeconds;

    @Value("${jhipster.security.authentication.jwt.token-validity-in-seconds-for-remember-me:0}")
    private long tokenValidityInSecondsForRememberMe;

    private final AuthenticationManagerBuilder authenticationManagerBuilder;
    private final SessionPolicyService sessionPolicyService;
    private final LoginControlService loginControlService;

    public AuthenticateController(
        JwtEncoder jwtEncoder,
        AuthenticationManagerBuilder authenticationManagerBuilder,
        SessionPolicyService sessionPolicyService,
        LoginControlService loginControlService
    ) {
        this.loginControlService = loginControlService;
        this.jwtEncoder = jwtEncoder;
        this.authenticationManagerBuilder = authenticationManagerBuilder;
        this.sessionPolicyService = sessionPolicyService;
    }

    @PostMapping("/authenticate")
    public ResponseEntity<?> authorize(@Valid @RequestBody LoginVM loginVM, HttpServletRequest request) {
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
            loginVM.getUsername(),
            loginVM.getPassword()
        );

        Authentication authentication = authenticationManagerBuilder.getObject().authenticate(authenticationToken);
        Instant now = Instant.now();
        Instant validity = expiryOf(now, loginVM.isRememberMe());
        LoginDecision decision = loginControlService.decide(
            authentication,
            new LoginRequestInfo(
                LoginControlService.resolveChannel(loginVM.getClient(), request.getHeader(HttpHeaders.USER_AGENT)),
                ClientIpResolver.resolve(request),
                loginVM.getDeviceId(),
                loginVM.getDeviceName(),
                request.getHeader(HttpHeaders.USER_AGENT)
            ),
            validity
        );
        if (!decision.allowed()) {
            Map<String, Object> body = new HashMap<>();
            body.put("message", decision.code());
            body.put("detail", decision.message());
            body.put("title", decision.message());
            body.put("kind", decision.kind());
            body.put("value", decision.value());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
        }
        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = this.createToken(authentication, now, validity, decision.sid());
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setBearerAuth(jwt);
        return new ResponseEntity<>(new JWTToken(jwt), httpHeaders, HttpStatus.OK);
    }

    /** Đăng xuất: thu hồi phiên hiện tại (sid trong JWT). */
    @PostMapping("/logout-session")
    public ResponseEntity<Void> logoutSession(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            loginControlService.logoutSelf(jwt.getClaimAsString(SID_CLAIM));
        }
        return ResponseEntity.noContent().build();
    }

    /**
     * {@code GET /authenticate} : check if the user is authenticated, and return its login.
     *
     * @param principal the authentication principal.
     * @return the login if the user is authenticated.
     */
    @GetMapping(value = "/authenticate", produces = MediaType.TEXT_PLAIN_VALUE)
    public String isAuthenticated(Principal principal) {
        LOG.debug("REST request to check if the current user is authenticated");
        return principal == null ? null : principal.getName();
    }

    private Instant expiryOf(Instant now, boolean rememberMe) {
        Instant validity = rememberMe
            ? now.plus(this.tokenValidityInSecondsForRememberMe, ChronoUnit.SECONDS)
            : now.plus(this.tokenValidityInSeconds, ChronoUnit.SECONDS);
        return sessionPolicyService.capExpiry(now, validity);
    }

    public String createToken(Authentication authentication, Instant now, Instant validity, String sid) {
        String authorities = authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.joining(" "));

        // @formatter:off
        JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuedAt(now)
            .expiresAt(validity)
            .subject(authentication.getName())
            .claim(AUTHORITIES_KEY, authorities)
            .claim(SID_CLAIM, sid)
            .build();

        JwsHeader jwsHeader = JwsHeader.with(JWT_ALGORITHM).build();
        return this.jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, claims)).getTokenValue();
    }

    /**
     * Object to return as body in JWT Authentication.
     */
    static class JWTToken {

        private String idToken;

        JWTToken(String idToken) {
            this.idToken = idToken;
        }

        @JsonProperty("id_token")
        String getIdToken() {
            return idToken;
        }

        void setIdToken(String idToken) {
            this.idToken = idToken;
        }
    }
}
