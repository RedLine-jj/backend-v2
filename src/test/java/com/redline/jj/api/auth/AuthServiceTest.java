package com.redline.jj.api.auth;

import com.redline.jj.api.auth.dto.LoginRequest;
import com.redline.jj.api.auth.dto.LoginResponse;
import com.redline.jj.api.auth.dto.RefreshRequest;
import com.redline.jj.api.auth.dto.RefreshResponse;
import com.redline.jj.api.auth.dto.SignupRequest;
import com.redline.jj.common.exception.BusinessException;
import com.redline.jj.common.exception.ErrorCode;
import com.redline.jj.config.security.JwtUtil;
import com.redline.jj.domain.user.User;
import com.redline.jj.domain.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "refreshTokenExpiration", 604800000L);
    }

    @Test
    void signup_성공() {
        given(userRepository.findByUserId("testUser")).willReturn(Optional.empty());
        given(passwordEncoder.encode("rawPw1234")).willReturn("encodedPw");

        authService.signup(new SignupRequest("testUser", "rawPw1234", "홍길동"));

        verify(userRepository).save(argThat(u ->
            u.getUserId().equals("testUser") &&
            u.getUserPw().equals("encodedPw") &&
            u.getUserName().equals("홍길동")
        ));
    }

    @Test
    void signup_중복아이디_예외() {
        User existing = User.builder().userId("testUser").userPw("pw").userName("기존").build();
        given(userRepository.findByUserId("testUser")).willReturn(Optional.of(existing));

        assertThatThrownBy(() -> authService.signup(new SignupRequest("testUser", "rawPw1234", "홍길동")))
            .isInstanceOf(BusinessException.class)
            .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.USER_ALREADY_EXISTS));

        verify(userRepository, never()).save(any());
    }

    @Test
    void login_성공() {
        User user = User.builder().userId("testUser").userPw("encodedPw").userName("홍길동").build();
        given(userRepository.findByUserId("testUser")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("rawPw1234", "encodedPw")).willReturn(true);
        given(jwtUtil.generateAccessToken("testUser")).willReturn("access-token");
        given(jwtUtil.generateRefreshToken("testUser")).willReturn("refresh-token");
        given(redisTemplate.opsForValue()).willReturn(valueOperations);

        LoginResponse result = authService.login(new LoginRequest("testUser", "rawPw1234"));

        assertThat(result.getAccessToken()).isEqualTo("access-token");
        assertThat(result.getRefreshToken()).isEqualTo("refresh-token");
        verify(valueOperations).set(
            eq("refresh_token:testUser"),
            eq("refresh-token"),
            eq(604800000L),
            eq(TimeUnit.MILLISECONDS)
        );
    }

    @Test
    void login_아이디없음_예외() {
        given(userRepository.findByUserId("unknown")).willReturn(Optional.empty());

        // userId 존재 여부 노출 차단: USER_NOT_FOUND가 아닌 INVALID_PASSWORD
        assertThatThrownBy(() -> authService.login(new LoginRequest("unknown", "rawPw1234")))
            .isInstanceOf(BusinessException.class)
            .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_PASSWORD));
    }

    @Test
    void login_비밀번호틀림_예외() {
        User user = User.builder().userId("testUser").userPw("encodedPw").userName("홍길동").build();
        given(userRepository.findByUserId("testUser")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("wrongPw", "encodedPw")).willReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("testUser", "wrongPw")))
            .isInstanceOf(BusinessException.class)
            .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_PASSWORD));
    }

    @Test
    void refresh_성공() {
        given(jwtUtil.getUserId("valid-refresh")).willReturn("testUser");
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get("refresh_token:testUser")).willReturn("valid-refresh");
        given(jwtUtil.generateAccessToken("testUser")).willReturn("new-access-token");

        RefreshResponse result = authService.refresh(new RefreshRequest("valid-refresh"));

        assertThat(result.getAccessToken()).isEqualTo("new-access-token");
    }

    @Test
    void refresh_토큰불일치_예외() {
        given(jwtUtil.getUserId("valid-refresh")).willReturn("testUser");
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get("refresh_token:testUser")).willReturn("different-token");

        assertThatThrownBy(() -> authService.refresh(new RefreshRequest("valid-refresh")))
            .isInstanceOf(BusinessException.class)
            .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.TOKEN_INVALID));
    }

    @Test
    void refresh_Redis에_토큰없음_예외() {
        given(jwtUtil.getUserId("valid-refresh")).willReturn("testUser");
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get("refresh_token:testUser")).willReturn(null);

        assertThatThrownBy(() -> authService.refresh(new RefreshRequest("valid-refresh")))
            .isInstanceOf(BusinessException.class)
            .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.TOKEN_INVALID));
    }
}
