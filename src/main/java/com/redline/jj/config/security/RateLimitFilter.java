package com.redline.jj.config.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.redline.jj.common.exception.ErrorCode;
import com.redline.jj.common.response.ApiResponse;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

public class RateLimitFilter extends OncePerRequestFilter {

    private static final int CAPACITY = 60;
    private static final int MAX_IPS = 10_000;

    private final Cache<String, Bucket> buckets = Caffeine.newBuilder()
        .maximumSize(MAX_IPS)
        .expireAfterAccess(2, TimeUnit.MINUTES)
        .build();
    private final ObjectMapper objectMapper;

    public RateLimitFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String ip = resolveClientIp(request);
        Bucket bucket = buckets.get(ip, k -> createBucket());

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
        } else {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(
                objectMapper.writeValueAsString(ApiResponse.fail(ErrorCode.RATE_LIMIT_EXCEEDED))
            );
        }
    }

    private Bucket createBucket() {
        return Bucket.builder()
            .addLimit(Bandwidth.builder()
                .capacity(CAPACITY)
                .refillGreedy(CAPACITY, Duration.ofMinutes(1))
                .build())
            .build();
    }

    private String resolveClientIp(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        if (isTrustedProxy(remoteAddr)) {
            String xff = request.getHeader("X-Forwarded-For");
            if (StringUtils.hasText(xff)) {
                return xff.split(",")[0].trim();
            }
        }
        return remoteAddr;
    }

    private boolean isTrustedProxy(String addr) {
        if (addr == null) {
            return false;
        }
        return addr.equals("127.0.0.1")
            || addr.equals("0:0:0:0:0:0:0:1")
            || addr.equals("::1")
            || addr.startsWith("10.")
            || addr.startsWith("192.168.")
            || isPrivate172(addr);
    }

    private boolean isPrivate172(String addr) {
        if (!addr.startsWith("172.")) {
            return false;
        }
        try {
            int second = Integer.parseInt(addr.split("\\.")[1]);
            return second >= 16 && second <= 31;
        } catch (Exception e) {
            return false;
        }
    }
}
