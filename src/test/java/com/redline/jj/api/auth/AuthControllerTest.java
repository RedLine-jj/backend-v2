package com.redline.jj.api.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.redline.jj.api.auth.dto.LoginRequest;
import com.redline.jj.api.auth.dto.LoginResponse;
import com.redline.jj.api.auth.dto.RefreshRequest;
import com.redline.jj.api.auth.dto.RefreshResponse;
import com.redline.jj.api.auth.dto.SignupRequest;
import com.redline.jj.common.exception.BusinessException;
import com.redline.jj.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import(AuthControllerTest.TestSecurityConfig.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @TestConfiguration
    static class TestSecurityConfig {
        @Bean
        SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
            return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .build();
        }
    }

    @Test
    void signup_성공_200() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new SignupRequest("testUser", "rawPw1234", "홍길동"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("회원가입이 완료되었습니다."));
    }

    @Test
    void signup_중복아이디_409() throws Exception {
        willThrow(new BusinessException(ErrorCode.USER_ALREADY_EXISTS))
            .given(authService).signup(any());

        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new SignupRequest("testUser", "rawPw1234", "홍길동"))))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("U001"));
    }

    @Test
    void login_성공_200() throws Exception {
        given(authService.login(any())).willReturn(new LoginResponse("access-token", "refresh-token"));

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new LoginRequest("testUser", "rawPw1234"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.accessToken").value("access-token"))
            .andExpect(jsonPath("$.data.refreshToken").value("refresh-token"));
    }

    @Test
    void login_인증실패_401() throws Exception {
        given(authService.login(any()))
            .willThrow(new BusinessException(ErrorCode.INVALID_PASSWORD));

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new LoginRequest("testUser", "wrongPw12"))))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("U003"));
    }

    @Test
    void refresh_성공_200() throws Exception {
        given(authService.refresh(any())).willReturn(new RefreshResponse("new-access-token"));

        mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new RefreshRequest("valid-refresh-token"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.accessToken").value("new-access-token"));
    }

    @Test
    void refresh_유효하지않은토큰_401() throws Exception {
        given(authService.refresh(any()))
            .willThrow(new BusinessException(ErrorCode.TOKEN_INVALID));

        mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new RefreshRequest("invalid-token"))))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("U005"));
    }
}
