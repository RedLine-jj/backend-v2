package com.redline.jj.api.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.redline.jj.api.notification.dto.NotificationResponse;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(NotificationController.class)
@Import(NotificationControllerTest.TestSecurityConfig.class)
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationService notificationService;

    @TestConfiguration
    static class TestSecurityConfig {

        @Bean
        SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
            ObjectMapper mapper = new ObjectMapper();
            return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
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
    @DisplayName("GET /api/notifications/stream 인증됨 - text/event-stream 반환")
    void stream_인증_text_event_stream() throws Exception {
        SseEmitter emitter = new SseEmitter();
        given(notificationService.openStream("testuser")).willAnswer(inv -> {
            emitter.complete();
            return emitter;
        });

        MvcResult result = mockMvc.perform(get("/api/notifications/stream")
                .with(user("testuser").roles("USER"))
                .accept(MediaType.TEXT_EVENT_STREAM_VALUE))
            .andExpect(request().asyncStarted())
            .andReturn();

        mockMvc.perform(asyncDispatch(result))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM_VALUE));
    }

    @Test
    @DisplayName("GET /api/notifications/stream 비인증 - 401 반환")
    void stream_비인증_401() throws Exception {
        mockMvc.perform(get("/api/notifications/stream"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("E401"));
    }

    @Test
    @DisplayName("GET /api/notifications 비인증 - 401 반환")
    void getNotifications_비인증_401() throws Exception {
        mockMvc.perform(get("/api/notifications"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("E401"));
    }

    @Test
    @DisplayName("PATCH /api/notifications/{id}/read 비인증 - 401 반환")
    void markAsRead_비인증_401() throws Exception {
        mockMvc.perform(patch("/api/notifications/1/read"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("E401"));
    }

    @Test
    @DisplayName("PATCH /api/notifications/read-all 비인증 - 401 반환")
    void markAllAsRead_비인증_401() throws Exception {
        mockMvc.perform(patch("/api/notifications/read-all"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("E401"));
    }

    // =========================================================================
    // 인증 성공 케이스
    // =========================================================================

    @Test
    @DisplayName("GET /api/notifications 인증 성공 - 200, data 배열 반환")
    void getNotifications_인증_성공_200() throws Exception {
        NotificationResponse item = new NotificationResponse(
            1L, 10L, "501", "리바이스", false, LocalDateTime.of(2026, 4, 1, 12, 0)
        );
        given(notificationService.getNotifications("testuser")).willReturn(List.of(item));

        mockMvc.perform(get("/api/notifications")
                .with(user("testuser").roles("USER")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").isArray())
            .andExpect(jsonPath("$.data[0].id").value(1))
            .andExpect(jsonPath("$.data[0].modelName").value("501"));
    }

    @Test
    @DisplayName("GET /api/notifications/unread-count 인증 성공 - 200, data=3 반환")
    void getUnreadCount_인증_성공_200() throws Exception {
        given(notificationService.getUnreadCount("testuser")).willReturn(3L);

        mockMvc.perform(get("/api/notifications/unread-count")
                .with(user("testuser").roles("USER")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").value(3));
    }

    @Test
    @DisplayName("PATCH /api/notifications/1/read 인증 성공 - 200, success=true 반환")
    void markAsRead_인증_성공_200() throws Exception {
        willDoNothing().given(notificationService).markAsRead("testuser", 1L);

        mockMvc.perform(patch("/api/notifications/1/read")
                .with(user("testuser").roles("USER")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("PATCH /api/notifications/read-all 인증 성공 - 200, success=true 반환")
    void markAllAsRead_인증_성공_200() throws Exception {
        willDoNothing().given(notificationService).markAllAsRead("testuser");

        mockMvc.perform(patch("/api/notifications/read-all")
                .with(user("testuser").roles("USER")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    // =========================================================================
    // 서비스 예외 → HTTP 상태코드 변환 케이스
    // =========================================================================

    @Test
    @DisplayName("PATCH /api/notifications/1/read - 서비스가 N001(NOT_FOUND) 던지면 404, $.code=N001 반환")
    void markAsRead_N001_404() throws Exception {
        willThrow(new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND))
            .given(notificationService).markAsRead("testuser", 1L);

        mockMvc.perform(patch("/api/notifications/1/read")
                .with(user("testuser").roles("USER")))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("N001"));
    }

    @Test
    @DisplayName("PATCH /api/notifications/1/read - 서비스가 N002(FORBIDDEN) 던지면 403, $.code=N002 반환")
    void markAsRead_N002_403() throws Exception {
        willThrow(new BusinessException(ErrorCode.NOTIFICATION_ACCESS_DENIED))
            .given(notificationService).markAsRead("testuser", 1L);

        mockMvc.perform(patch("/api/notifications/1/read")
                .with(user("testuser").roles("USER")))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("N002"));
    }
}
