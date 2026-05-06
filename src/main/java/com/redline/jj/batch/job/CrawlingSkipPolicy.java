package com.redline.jj.batch.job;

import com.redline.jj.common.exception.BusinessException;
import com.redline.jj.common.exception.ErrorCode;
import org.springframework.batch.core.step.skip.SkipPolicy;

import java.util.Set;

public class CrawlingSkipPolicy implements SkipPolicy {

    private static final Set<ErrorCode> SKIPPABLE = Set.of(
        ErrorCode.CRAWLING_FAILED
    );

    @Override
    public boolean shouldSkip(Throwable t, long skipCount) {
        if (t instanceof BusinessException be) {
            return SKIPPABLE.contains(be.getErrorCode());
        }
        return false;
    }
}
