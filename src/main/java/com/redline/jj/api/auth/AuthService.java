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
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    private static final String REFRESH_TOKEN_PREFIX = "refresh_token:";

    public void signup(SignupRequest request) {
        if (userRepository.findByUserId(request.getUserId()).isPresent()) {
            throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS);
        }
        User user = User.builder()
            .userId(request.getUserId())
            .userPw(passwordEncoder.encode(request.getPassword()))
            .userName(request.getUserName())
            .build();
        userRepository.save(user);
    }

    public LoginResponse login(LoginRequest request) {
        // userId 존재 여부를 외부에 노출하지 않기 위해 USER_NOT_FOUND 대신 INVALID_PASSWORD 사용
        User user = userRepository.findByUserId(request.getUserId())
            .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_PASSWORD));

        if (!passwordEncoder.matches(request.getPassword(), user.getUserPw())) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD);
        }

        String accessToken = jwtUtil.generateAccessToken(user.getUserId());
        String refreshToken = jwtUtil.generateRefreshToken(user.getUserId());

        redisTemplate.opsForValue().set(
            REFRESH_TOKEN_PREFIX + user.getUserId(),
            refreshToken,
            refreshTokenExpiration,
            TimeUnit.MILLISECONDS
        );

        return new LoginResponse(accessToken, refreshToken);
    }

    public RefreshResponse refresh(RefreshRequest request) {
        jwtUtil.validate(request.getRefreshToken());

        String userId = jwtUtil.getUserId(request.getRefreshToken());
        Object stored = redisTemplate.opsForValue().get(REFRESH_TOKEN_PREFIX + userId);

        if (stored == null || !stored.equals(request.getRefreshToken())) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID);
        }

        return new RefreshResponse(jwtUtil.generateAccessToken(userId));
    }
}
