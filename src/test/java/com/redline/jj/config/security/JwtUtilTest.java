package com.redline.jj.config.security;

import com.redline.jj.common.exception.BusinessException;
import com.redline.jj.common.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtUtilTest {

    private static final String SECRET = "dGVzdC1zZWNyZXQta2V5LWZvci10ZXN0aW5nLW9ubHktMjU2LWJpdHM=";
    private static final String OTHER_SECRET = "b3RoZXItc2VjcmV0LWtleS1mb3ItdGVzdGluZy1vbmx5LTI1Ni1iaXRz";

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(SECRET, 1800000L, 604800000L);
    }

    @Test
    void accessToken_생성후_getUserId_동일_userId_반환() {
        String token = jwtUtil.generateAccessToken("user1");

        assertThat(jwtUtil.getUserId(token)).isEqualTo("user1");
    }

    @Test
    void refreshToken_생성후_getUserId_동일_userId_반환() {
        String token = jwtUtil.generateRefreshToken("user1");

        assertThat(jwtUtil.getUserId(token)).isEqualTo("user1");
    }

    @Test
    void 만료된_accessToken_isExpired_true_반환() {
        JwtUtil shortLivedUtil = new JwtUtil(SECRET, -1L, -1L);
        String token = shortLivedUtil.generateAccessToken("user1");

        assertThat(jwtUtil.isExpired(token)).isTrue();
    }

    @Test
    void 만료된_토큰_validate_TOKEN_EXPIRED_예외() {
        JwtUtil shortLivedUtil = new JwtUtil(SECRET, -1L, -1L);
        String expiredToken = shortLivedUtil.generateAccessToken("user1");

        assertThatThrownBy(() -> jwtUtil.validate(expiredToken))
            .isInstanceOf(BusinessException.class)
            .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.TOKEN_EXPIRED));
    }

    @Test
    void 다른_서명키_토큰_validate_TOKEN_INVALID_예외() {
        JwtUtil otherUtil = new JwtUtil(OTHER_SECRET, 1800000L, 604800000L);
        String foreignToken = otherUtil.generateAccessToken("user1");

        assertThatThrownBy(() -> jwtUtil.validate(foreignToken))
            .isInstanceOf(BusinessException.class)
            .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.TOKEN_INVALID));
    }

    @Test
    void 임의문자열_validate_TOKEN_INVALID_예외() {
        assertThatThrownBy(() -> jwtUtil.validate("not.a.valid.token"))
            .isInstanceOf(BusinessException.class)
            .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.TOKEN_INVALID));
    }

    @Test
    void 빈문자열_validate_TOKEN_INVALID_예외() {
        assertThatThrownBy(() -> jwtUtil.validate(""))
            .isInstanceOf(BusinessException.class)
            .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.TOKEN_INVALID));
    }

    @Test
    void accessToken과_refreshToken_만료시간_차이_6일_이상() {
        String accessToken = jwtUtil.generateAccessToken("user1");
        String refreshToken = jwtUtil.generateRefreshToken("user1");

        Date accessExpiration = jwtUtil.parseClaims(accessToken).getExpiration();
        Date refreshExpiration = jwtUtil.parseClaims(refreshToken).getExpiration();

        long diffMs = refreshExpiration.getTime() - accessExpiration.getTime();
        long sixDaysMs = 6L * 24 * 60 * 60 * 1000;

        assertThat(diffMs).isGreaterThanOrEqualTo(sixDaysMs);
    }
}
