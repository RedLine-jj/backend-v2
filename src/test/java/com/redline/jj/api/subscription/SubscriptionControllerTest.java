package com.redline.jj.api.subscription;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.redline.jj.api.subscription.dto.SubscriptionResponse;
import com.redline.jj.common.exception.BusinessException;
import com.redline.jj.common.exception.ErrorCode;
import com.redline.jj.common.response.ApiResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SubscriptionController.class)
@Import(SubscriptionControllerTest.TestSecurityConfig.class)
class SubscriptionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private SubscriptionService subscriptionService;

    @TestConfiguration
    static class TestSecurityConfig {

        @Bean
        SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
            ObjectMapper mapper = new ObjectMapper();
            return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                    .requestMatchers(HttpMethod.GET, "/api/subscriptions/top").permitAll()
                    .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex.authenticationEntryPoint(
                    (request, response, e) -> {
                        response.setStatus(HttpStatus.UNAUTHORIZED.value());
                        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                        response.setCharacterEncoding("UTF-8");
                        response.getWriter().write(
                            mapper.writeValueAsString(ApiResponse.fail(ErrorCode.UNAUTHORIZED))
                        );
                    }
                ))
                .build();
        }
    }

    @Test
    @DisplayName("subscribe - 비인증 요청은 401을 반환한다")
    void subscribe_비인증_401() throws Exception {
        mockMvc.perform(post("/api/subscriptions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"modelId\":1}"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("E401"));
    }

    @Test
    @DisplayName("cancel - 비인증 요청은 401을 반환한다")
    void cancel_비인증_401() throws Exception {
        mockMvc.perform(delete("/api/subscriptions/1"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("E401"));
    }

    @Test
    @DisplayName("getTop10 - 비인증 요청도 200을 반환한다")
    void getTop10_비인증_200() throws Exception {
        given(subscriptionService.getTop10()).willReturn(List.of());

        mockMvc.perform(get("/api/subscriptions/top"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("subscribe - 인증된 사용자가 구독하면 200과 구독 정보를 반환한다")
    void subscribe_인증_성공_200() throws Exception {
        SubscriptionResponse response = new SubscriptionResponse(
            1L, "testuser", 10L, "Model X", "BrandA", LocalDateTime.of(2026, 4, 27, 0, 0)
        );
        given(subscriptionService.subscribe(eq("testuser"), eq(10L))).willReturn(response);

        mockMvc.perform(post("/api/subscriptions")
                .with(user("testuser").roles("USER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"modelId\":10}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").value(1))
            .andExpect(jsonPath("$.data.userLoginId").value("testuser"))
            .andExpect(jsonPath("$.data.modelId").value(10));
    }

    @Test
    @DisplayName("cancel - 인증된 사용자가 구독 취소하면 200을 반환한다")
    void cancel_인증_성공_200() throws Exception {
        willDoNothing().given(subscriptionService).cancel(eq("testuser"), eq(1L));

        mockMvc.perform(delete("/api/subscriptions/1")
                .with(user("testuser").roles("USER")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("getMySubscriptions - 인증된 사용자가 목록을 조회하면 200과 구독 리스트를 반환한다")
    void getMySubscriptions_인증_성공_200() throws Exception {
        SubscriptionResponse response = new SubscriptionResponse(
            2L, "testuser", 20L, "Model Y", "BrandB", LocalDateTime.of(2026, 4, 27, 0, 0)
        );
        given(subscriptionService.getMySubscriptions("testuser")).willReturn(List.of(response));

        mockMvc.perform(get("/api/subscriptions")
                .with(user("testuser").roles("USER")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data[0].id").value(2))
            .andExpect(jsonPath("$.data[0].modelId").value(20));
    }

    @Test
    @DisplayName("getMySubscriptionCount - 인증된 사용자가 구독 건수를 조회하면 200과 카운트를 반환한다")
    void getMySubscriptionCount_인증_성공_200() throws Exception {
        given(subscriptionService.getMySubscriptionCount("testuser")).willReturn(3L);

        mockMvc.perform(get("/api/subscriptions/count")
                .with(user("testuser").roles("USER")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").value(3));
    }

    @Test
    @DisplayName("cancel - 타인 구독 취소 시도 시 서비스가 S003(FORBIDDEN)을 던지면 HTTP 403을 반환한다")
    void cancel_타인_구독_403() throws Exception {
        // given — 서비스 계층에서 S003(SUBSCRIPTION_ACCESS_DENIED, 403)을 던진다
        // 컨트롤러가 GlobalExceptionHandler에 위임해 HTTP 상태코드를 올바르게 내려주는지 검증한다
        willThrow(new BusinessException(ErrorCode.SUBSCRIPTION_ACCESS_DENIED))
            .given(subscriptionService).cancel(eq("testuser"), eq(1L));

        // when & then
        mockMvc.perform(delete("/api/subscriptions/1")
                .with(user("testuser").roles("USER")))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("S003"));
    }

    @Test
    @DisplayName("subscribe - modelId가 null인 요청은 @Valid 검증에 의해 HTTP 400을 반환한다")
    void subscribe_modelId_null_400() throws Exception {
        // given — modelId 필드가 없는(null) 요청 바디를 전송한다
        // SubscriptionRequest의 @NotNull이 동작해 GlobalExceptionHandler가 400으로 응답해야 한다

        // when & then
        mockMvc.perform(post("/api/subscriptions")
                .with(user("testuser").roles("USER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"modelId\":null}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("E400"));
    }
}
