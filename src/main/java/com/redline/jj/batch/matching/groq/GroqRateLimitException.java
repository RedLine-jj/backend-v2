package com.redline.jj.batch.matching.groq;

class GroqRateLimitException extends RuntimeException {

    final long retryAfterSeconds; // -1 이면 Retry-After 헤더 없음

    GroqRateLimitException(long retryAfterSeconds) {
        super(null, null, true, false);
        this.retryAfterSeconds = retryAfterSeconds;
    }
}
