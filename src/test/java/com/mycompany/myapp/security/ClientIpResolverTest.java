package com.mycompany.myapp.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class ClientIpResolverTest {

    @Test
    void publicRemoteAddrWins() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRemoteAddr("113.160.1.2");
        req.addHeader("X-Forwarded-For", "1.1.1.1");
        assertThat(ClientIpResolver.resolve(req)).isEqualTo("113.160.1.2");
    }

    @Test
    void internalRemoteFallsBackToRightmostPublicForwardedIp() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRemoteAddr("10.0.0.5");
        req.addHeader("X-Forwarded-For", "8.8.8.8, 113.160.1.2, 100.64.0.3");
        assertThat(ClientIpResolver.resolve(req)).isEqualTo("113.160.1.2");
    }

    @Test
    void noForwardedHeaderKeepsRemote() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRemoteAddr("127.0.0.1");
        assertThat(ClientIpResolver.resolve(req)).isEqualTo("127.0.0.1");
    }

    @Test
    void internalRanges() {
        assertThat(ClientIpResolver.isInternal("172.20.1.1")).isTrue();
        assertThat(ClientIpResolver.isInternal("172.32.1.1")).isFalse();
        assertThat(ClientIpResolver.isInternal("192.168.1.10")).isTrue();
        assertThat(ClientIpResolver.isInternal("113.160.1.2")).isFalse();
    }
}
