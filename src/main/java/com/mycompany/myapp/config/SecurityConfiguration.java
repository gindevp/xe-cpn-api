package com.mycompany.myapp.config;

import static org.springframework.security.config.Customizer.withDefaults;

import com.mycompany.myapp.security.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.MvcRequestMatcher;
import org.springframework.web.servlet.handler.HandlerMappingIntrospector;
import tech.jhipster.config.JHipsterProperties;

@Configuration
@EnableMethodSecurity(securedEnabled = true)
public class SecurityConfiguration {

    private final JHipsterProperties jHipsterProperties;

    public SecurityConfiguration(JHipsterProperties jHipsterProperties) {
        this.jHipsterProperties = jHipsterProperties;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, MvcRequestMatcher.Builder mvc) throws Exception {
        http
            .cors(withDefaults())
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(authz ->
                // prettier-ignore
                authz
                    .requestMatchers(mvc.pattern("/swagger-ui/**")).permitAll()
                    .requestMatchers(mvc.pattern(HttpMethod.POST, "/api/authenticate")).permitAll()
                    .requestMatchers(mvc.pattern(HttpMethod.GET, "/api/authenticate")).permitAll()
                    .requestMatchers(mvc.pattern("/api/register")).permitAll()
                    .requestMatchers(mvc.pattern("/api/activate")).permitAll()
                    .requestMatchers(mvc.pattern("/api/account/reset-password/init")).permitAll()
                    .requestMatchers(mvc.pattern("/api/account/reset-password/finish")).permitAll()
                    .requestMatchers(mvc.pattern(HttpMethod.POST, "/api/orders/drafts")).permitAll()
                    .requestMatchers(mvc.pattern(HttpMethod.POST, "/api/orders/track")).permitAll()
                    .requestMatchers(mvc.pattern("/api/admin/**")).hasAuthority(AuthoritiesConstants.ADMIN)
                    // Generated domain CRUD must not bypass facade money/status/day-close guards (TASK-007)
                    .requestMatchers(mvc.pattern(HttpMethod.POST, "/api/shipment-orders")).hasAuthority(AuthoritiesConstants.ADMIN)
                    .requestMatchers(mvc.pattern(HttpMethod.PUT, "/api/shipment-orders/**")).hasAuthority(AuthoritiesConstants.ADMIN)
                    .requestMatchers(mvc.pattern(HttpMethod.PATCH, "/api/shipment-orders/**")).hasAuthority(AuthoritiesConstants.ADMIN)
                    .requestMatchers(mvc.pattern(HttpMethod.DELETE, "/api/shipment-orders/**")).hasAuthority(AuthoritiesConstants.ADMIN)
                    .requestMatchers(mvc.pattern(HttpMethod.POST, "/api/order-payments")).hasAuthority(AuthoritiesConstants.ADMIN)
                    .requestMatchers(mvc.pattern(HttpMethod.PUT, "/api/order-payments/**")).hasAuthority(AuthoritiesConstants.ADMIN)
                    .requestMatchers(mvc.pattern(HttpMethod.PATCH, "/api/order-payments/**")).hasAuthority(AuthoritiesConstants.ADMIN)
                    .requestMatchers(mvc.pattern(HttpMethod.DELETE, "/api/order-payments/**")).hasAuthority(AuthoritiesConstants.ADMIN)
                    .requestMatchers(mvc.pattern(HttpMethod.POST, "/api/order-issues")).hasAuthority(AuthoritiesConstants.ADMIN)
                    .requestMatchers(mvc.pattern(HttpMethod.PUT, "/api/order-issues/**")).hasAuthority(AuthoritiesConstants.ADMIN)
                    .requestMatchers(mvc.pattern(HttpMethod.PATCH, "/api/order-issues/**")).hasAuthority(AuthoritiesConstants.ADMIN)
                    .requestMatchers(mvc.pattern(HttpMethod.DELETE, "/api/order-issues/**")).hasAuthority(AuthoritiesConstants.ADMIN)
                    .requestMatchers(mvc.pattern(HttpMethod.POST, "/api/order-legs")).hasAuthority(AuthoritiesConstants.ADMIN)
                    .requestMatchers(mvc.pattern(HttpMethod.PUT, "/api/order-legs/**")).hasAuthority(AuthoritiesConstants.ADMIN)
                    .requestMatchers(mvc.pattern(HttpMethod.PATCH, "/api/order-legs/**")).hasAuthority(AuthoritiesConstants.ADMIN)
                    .requestMatchers(mvc.pattern(HttpMethod.DELETE, "/api/order-legs/**")).hasAuthority(AuthoritiesConstants.ADMIN)
                    .requestMatchers(mvc.pattern("/api/**")).authenticated()
                    .requestMatchers(mvc.pattern("/v3/api-docs/**")).hasAuthority(AuthoritiesConstants.ADMIN)
                    .requestMatchers(mvc.pattern("/management/health")).permitAll()
                    .requestMatchers(mvc.pattern("/management/health/**")).permitAll()
                    .requestMatchers(mvc.pattern("/management/info")).permitAll()
                    .requestMatchers(mvc.pattern("/management/prometheus")).permitAll()
                    .requestMatchers(mvc.pattern("/management/**")).hasAuthority(AuthoritiesConstants.ADMIN)
            )
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(exceptions ->
                exceptions
                    .authenticationEntryPoint(new BearerTokenAuthenticationEntryPoint())
                    .accessDeniedHandler(new BearerTokenAccessDeniedHandler())
            )
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(withDefaults()));
        return http.build();
    }

    @Bean
    MvcRequestMatcher.Builder mvc(HandlerMappingIntrospector introspector) {
        return new MvcRequestMatcher.Builder(introspector);
    }
}
