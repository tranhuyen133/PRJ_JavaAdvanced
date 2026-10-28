package com.gymflow.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

class AuthRateLimitFilterTest {

    private final AtomicLong clock = new AtomicLong(1_000_000);
    private final AuthRateLimitFilter filter = new AuthRateLimitFilter(new ObjectMapper(), true, clock::get);

    private MockHttpServletResponse call(String method, String uri, String ip) throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest(method, uri);
        req.setRequestURI(uri);
        req.addHeader("X-Real-IP", ip);
        MockHttpServletResponse res = new MockHttpServletResponse();
        filter.doFilter(req, res, new MockFilterChain());
        return res;
    }

    @Test
    void loginBlockedAfterTenAttemptsPerMinute() throws Exception {
        for (int i = 0; i < 10; i++) {
            assertThat(call("POST", "/api/auth/login", "1.1.1.1").getStatus()).isEqualTo(200);
        }
        MockHttpServletResponse blocked = call("POST", "/api/auth/login", "1.1.1.1");
        assertThat(blocked.getStatus()).isEqualTo(429);
        assertThat(blocked.getHeader("Retry-After")).isEqualTo("60");
        assertThat(blocked.getContentAsString()).contains("\"status\":429");
    }

    @Test
    void limitIsPerIpAndResetsAfterWindow() throws Exception {
        for (int i = 0; i < 11; i++) call("POST", "/api/auth/login", "2.2.2.2");
        assertThat(call("POST", "/api/auth/login", "3.3.3.3").getStatus()).isEqualTo(200);
        clock.addAndGet(60_000);
        assertThat(call("POST", "/api/auth/login", "2.2.2.2").getStatus()).isEqualTo(200);
    }

    @Test
    void registerHasStricterLimitAndSeparateBucket() throws Exception {
        for (int i = 0; i < 5; i++) {
            assertThat(call("POST", "/api/auth/register", "4.4.4.4").getStatus()).isEqualTo(200);
        }
        assertThat(call("POST", "/api/auth/register", "4.4.4.4").getStatus()).isEqualTo(429);
        assertThat(call("POST", "/api/auth/login", "4.4.4.4").getStatus()).isEqualTo(200);
    }

    @Test
    void otherEndpointsAndMethodsAreNotLimited() throws Exception {
        for (int i = 0; i < 30; i++) {
            assertThat(call("GET", "/api/auth/options", "5.5.5.5").getStatus()).isEqualTo(200);
            assertThat(call("GET", "/api/members", "5.5.5.5").getStatus()).isEqualTo(200);
        }
    }

    @Test
    void disabledFilterNeverBlocks() throws Exception {
        AuthRateLimitFilter off = new AuthRateLimitFilter(new ObjectMapper(), false, clock::get);
        for (int i = 0; i < 20; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/auth/login");
            MockHttpServletResponse res = new MockHttpServletResponse();
            off.doFilter(req, res, new MockFilterChain());
            assertThat(res.getStatus()).isEqualTo(200);
        }
    }
}
