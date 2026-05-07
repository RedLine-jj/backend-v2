package com.redline.jj.batch.job;

import com.redline.jj.common.exception.BusinessException;
import com.redline.jj.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CrawlingSkipPolicyTest {

    private final CrawlingSkipPolicy skipPolicy = new CrawlingSkipPolicy();

    @Test
    void LLM_RATE_LIMITED_예외는_skip된다() {
        assertThat(skipPolicy.shouldSkip(new BusinessException(ErrorCode.LLM_RATE_LIMITED), 0)).isTrue();
    }

    @Test
    void CRAWLING_FAILED_예외는_skip된다() {
        assertThat(skipPolicy.shouldSkip(new BusinessException(ErrorCode.CRAWLING_FAILED), 0)).isTrue();
    }

    @Test
    void LLM_MATCHING_FAILED_예외는_skip되지_않는다() {
        assertThat(skipPolicy.shouldSkip(new BusinessException(ErrorCode.LLM_MATCHING_FAILED), 0)).isFalse();
    }

    @Test
    void 일반_RuntimeException은_skip되지_않는다() {
        assertThat(skipPolicy.shouldSkip(new RuntimeException("일반 예외"), 0)).isFalse();
    }
}
