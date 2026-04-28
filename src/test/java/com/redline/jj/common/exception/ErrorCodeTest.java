package com.redline.jj.common.exception;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.http.HttpStatus;

import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorCodeTest {

    @Test
    void ErrorCode_총_개수는_18개다() {
        assertThat(ErrorCode.values()).hasSize(21);
    }

    @EnumSource(ErrorCode.class)
    @ParameterizedTest
    void 모든_ErrorCode는_null_필드가_없다(ErrorCode errorCode) {
        assertThat(errorCode.getStatus()).isNotNull();
        assertThat(errorCode.getCode()).isNotNull();
        assertThat(errorCode.getMessage()).isNotNull();
    }

    @CsvSource({
        "USER_ALREADY_EXISTS,   409, U001",
        "USER_NOT_FOUND,        404, U002",
        "INVALID_PASSWORD,      401, U003",
        "TOKEN_EXPIRED,         401, U004",
        "TOKEN_INVALID,         401, U005",
        "RATE_LIMIT_EXCEEDED,   429, U006",
        "MODEL_NOT_FOUND,       404, M001",
        "SITE_OPTION_NOT_FOUND, 404, M002",
        "SUBSCRIPTION_ALREADY_EXISTS, 409, S001",
        "SUBSCRIPTION_NOT_FOUND,      404, S002",
        "NOTIFICATION_NOT_FOUND,      404, N001",
        "NOTIFICATION_ACCESS_DENIED,  403, N002",
        "LLM_MATCHING_FAILED,   500, C001",
        "CRAWLING_FAILED,       500, C002"
    })
    @ParameterizedTest
    void 신규_ErrorCode의_HttpStatus와_code가_명세와_일치한다(String name, int expectedStatus, String expectedCode) {
        ErrorCode errorCode = ErrorCode.valueOf(name.trim());
        assertThat(errorCode.getStatus().value()).isEqualTo(expectedStatus);
        assertThat(errorCode.getCode()).isEqualTo(expectedCode.trim());
    }

    @Test
    void 신규_ErrorCode의_code는_도메인_prefix_패턴을_따른다() {
        Pattern pattern = Pattern.compile("^[UMSCN]\\d{3}$");
        for (ErrorCode errorCode : ErrorCode.values()) {
            String code = errorCode.getCode();
            if (code.startsWith("E")) {
                continue;
            }
            assertThat(code).matches(pattern);
        }
    }

    @Test
    void INVALID_PASSWORD는_401_UNAUTHORIZED다() {
        assertThat(ErrorCode.INVALID_PASSWORD.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void NOTIFICATION_ACCESS_DENIED는_403_FORBIDDEN다() {
        assertThat(ErrorCode.NOTIFICATION_ACCESS_DENIED.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void RATE_LIMIT_EXCEEDED는_429_TOO_MANY_REQUESTS다() {
        assertThat(ErrorCode.RATE_LIMIT_EXCEEDED.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }
}
