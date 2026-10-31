package com.footballai.social.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/** RNF/RF1: o IP do rate limit não pode ser escolhido pelo cliente via X-Forwarded-For. */
class RateLimitFilterTest {
    private static MockHttpServletRequest req(String remote, String xff) {
        MockHttpServletRequest r = new MockHttpServletRequest();
        r.setRemoteAddr(remote);
        if (xff != null) r.addHeader("X-Forwarded-For", xff);
        return r;
    }

    @Test
    void forwardedForFromAnUntrustedConnectionIsIgnored() {
        // cliente direto tentando trocar de IP (e de cota) a cada requisição
        assertThat(RateLimitFilter.resolveClientIp(req("203.0.113.9", "1.2.3.4"), Set.of())).isEqualTo("203.0.113.9");
        assertThat(RateLimitFilter.resolveClientIp(req("203.0.113.9", "1.2.3.4"), Set.of("10.0.0.2"))).isEqualTo("203.0.113.9");
    }

    @Test
    void behindTrustedProxiesTheClientIsTheRightmostUntrustedHop() {
        Set<String> proxies = Set.of("10.0.0.2", "10.0.0.3");
        // o cliente pode forjar o começo da lista; vale o salto adicionado pelo primeiro proxy confiável
        assertThat(RateLimitFilter.resolveClientIp(req("10.0.0.2", "6.6.6.6, 198.51.100.7"), proxies)).isEqualTo("198.51.100.7");
        assertThat(RateLimitFilter.resolveClientIp(req("10.0.0.2", "198.51.100.7, 10.0.0.3"), proxies)).isEqualTo("198.51.100.7");
        assertThat(RateLimitFilter.resolveClientIp(req("10.0.0.2", null), proxies)).isEqualTo("10.0.0.2");
        assertThat(RateLimitFilter.resolveClientIp(req("10.0.0.2", " , "), proxies)).isEqualTo("10.0.0.2");
    }

    @Test
    void controllersReuseTheResolvedIp() {
        MockHttpServletRequest r = req("203.0.113.9", "1.2.3.4");
        assertThat(RateLimitFilter.clientIp(r)).isEqualTo("203.0.113.9");
        r.setAttribute(RateLimitFilter.CLIENT_IP_ATTR, "198.51.100.7");
        assertThat(RateLimitFilter.clientIp(r)).isEqualTo("198.51.100.7");
    }
}
