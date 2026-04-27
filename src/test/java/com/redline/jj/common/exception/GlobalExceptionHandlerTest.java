package com.redline.jj.common.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.stream.Stream;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    static Stream<Arguments> businessExceptionCases() {
        return Stream.of(
                Arguments.of("USER_ALREADY_EXISTS", 409, "U001"),
                Arguments.of("MODEL_NOT_FOUND", 404, "M001"),
                Arguments.of("RATE_LIMIT_EXCEEDED", 429, "U006"),
                Arguments.of("LLM_MATCHING_FAILED", 500, "C001")
        );
    }

    @MethodSource("businessExceptionCases")
    @ParameterizedTest
    void BusinessException은_적절한_HttpStatus와_ApiResponse를_반환한다(String errorCodeName, int expectedStatus, String expectedCode) throws Exception {
        mockMvc.perform(get("/test/throw/" + errorCodeName))
                .andExpect(status().is(expectedStatus))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(expectedCode));
    }

    @Test
    void USER_ALREADY_EXISTS_예외는_409와_메시지를_반환한다() throws Exception {
        mockMvc.perform(get("/test/throw/USER_ALREADY_EXISTS"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("U001"))
                .andExpect(jsonPath("$.message").value("이미 존재하는 사용자입니다."));
    }

    @Test
    void 미처리_예외는_500_INTERNAL_ERROR로_응답한다() throws Exception {
        mockMvc.perform(get("/test/throw-runtime"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("E500"));
    }

    @RestController
    static class TestController {

        @GetMapping("/test/throw/{code}")
        void throwBusiness(@PathVariable String code) {
            throw new BusinessException(ErrorCode.valueOf(code));
        }

        @GetMapping("/test/throw-runtime")
        void throwRuntime() {
            throw new RuntimeException("boom");
        }
    }
}
