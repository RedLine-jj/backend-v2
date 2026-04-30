package com.redline.jj.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.redline.jj.config.security.RateLimitFilter;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RateLimitFilterTest {

    @Mock
    private FilterChain filterChain;

    private RateLimitFilter filter;

    @BeforeEach
    void setUp() {
        filter = new RateLimitFilter(new ObjectMapper());
    }

    private MockHttpServletRequest request(String ip) {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRemoteAddr(ip);
        return req;
    }

    @Test
    void 동일IP_60회요청_모두통과() throws Exception {
        // given
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when
        for (int i = 0; i < 60; i++) {
            filter.doFilter(request("1.2.3.4"), response, filterChain);
        }

        // then
        verify(filterChain, times(60)).doFilter(any(), any());
        assertThat(response.getStatus()).isNotEqualTo(429);
    }

    @Test
    void 동일IP_61번째요청_429반환() throws Exception {
        // given - 60회 먼저 소진
        for (int i = 0; i < 60; i++) {
            filter.doFilter(request("1.2.3.4"), new MockHttpServletResponse(), filterChain);
        }

        // when - 61번째 요청
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request("1.2.3.4"), response, filterChain);

        // then
        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getContentAsString()).contains("U006");
        // 61번째는 filterChain을 통과하지 않으므로 총 60번만 호출
        verify(filterChain, times(60)).doFilter(any(), any());
    }

    @Test
    void 다른IP_독립버킷_첫IP소진후두번째IP통과() throws Exception {
        // given - IP A 60회 소진
        for (int i = 0; i < 60; i++) {
            filter.doFilter(request("1.1.1.1"), new MockHttpServletResponse(), filterChain);
        }

        // when - IP B 첫 번째 요청 (독립 버킷이므로 통과해야 함)
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request("2.2.2.2"), response, filterChain);

        // then
        assertThat(response.getStatus()).isNotEqualTo(429);
    }

    @Test
    void XForwardedFor_헤더있으면_첫번째IP사용() throws Exception {
        // given - X-Forwarded-For 헤더의 첫 번째 IP 기준으로 60회 소진
        for (int i = 0; i < 60; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest();
            req.setRemoteAddr("10.0.0.1"); // 프록시 IP
            req.addHeader("X-Forwarded-For", "1.2.3.4");
            filter.doFilter(req, new MockHttpServletResponse(), filterChain);
        }

        // when - 61번째 요청 (동일 X-Forwarded-For)
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRemoteAddr("10.0.0.1");
        req.addHeader("X-Forwarded-For", "1.2.3.4");
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(req, response, filterChain);

        // then - 1.2.3.4 기준 버킷이 소진되었으므로 429
        assertThat(response.getStatus()).isEqualTo(429);
    }

    @Test
    void XForwardedFor_여러IP_콤마구분_첫번째IP사용() throws Exception {
        // given - 여러 IP가 콤마로 구분된 헤더에서 첫 번째 IP(1.2.3.4) 기준 60회 소진
        for (int i = 0; i < 60; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest();
            req.addHeader("X-Forwarded-For", "1.2.3.4, 5.6.7.8, 9.10.11.12");
            filter.doFilter(req, new MockHttpServletResponse(), filterChain);
        }

        // when - 61번째 요청
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("X-Forwarded-For", "1.2.3.4, 5.6.7.8, 9.10.11.12");
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(req, response, filterChain);

        // then - 1.2.3.4 기준 버킷 소진 → 429
        assertThat(response.getStatus()).isEqualTo(429);
    }
}
