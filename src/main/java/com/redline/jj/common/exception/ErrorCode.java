package com.redline.jj.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "E500", "서버 내부 오류가 발생했습니다."),
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "E400", "잘못된 요청입니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "E404", "리소스를 찾을 수 없습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "E401", "인증이 필요합니다."),

    USER_ALREADY_EXISTS(HttpStatus.CONFLICT, "U001", "이미 존재하는 사용자입니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "U002", "사용자를 찾을 수 없습니다."),
    INVALID_PASSWORD(HttpStatus.UNAUTHORIZED, "U003", "아이디 또는 비밀번호가 올바르지 않습니다."),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "U004", "토큰이 만료되었습니다."),
    TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "U005", "유효하지 않은 토큰입니다."),
    RATE_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "U006", "요청 한도를 초과했습니다."),

    MODEL_NOT_FOUND(HttpStatus.NOT_FOUND, "M001", "모델을 찾을 수 없습니다."),
    SITE_OPTION_NOT_FOUND(HttpStatus.NOT_FOUND, "M002", "사이트 옵션을 찾을 수 없습니다."),
    INVALID_DAYS(HttpStatus.BAD_REQUEST, "M003", "days 파라미터는 0 이상이어야 합니다."),

    SUBSCRIPTION_ALREADY_EXISTS(HttpStatus.CONFLICT, "S001", "이미 구독 중입니다."),
    SUBSCRIPTION_NOT_FOUND(HttpStatus.NOT_FOUND, "S002", "구독을 찾을 수 없습니다."),
    SUBSCRIPTION_ACCESS_DENIED(HttpStatus.FORBIDDEN, "S003", "구독에 접근할 권한이 없습니다."),

    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "N001", "알림을 찾을 수 없습니다."),
    NOTIFICATION_ACCESS_DENIED(HttpStatus.FORBIDDEN, "N002", "알림에 접근할 권한이 없습니다."),

    LLM_MATCHING_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "C001", "LLM 매칭에 실패했습니다."),
    CRAWLING_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "C002", "크롤링 처리에 실패했습니다."),
    UNSUPPORTED_SITE(HttpStatus.BAD_REQUEST, "C003", "지원하지 않는 사이트입니다."),
    LAUNCH_FAILURE(HttpStatus.INTERNAL_SERVER_ERROR, "C004", "배치 잡 실행에 실패했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
