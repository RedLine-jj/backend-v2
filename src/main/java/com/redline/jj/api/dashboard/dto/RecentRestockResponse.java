package com.redline.jj.api.dashboard.dto;

import com.redline.jj.domain.option.SiteOptionLog;

import java.time.LocalDateTime;

public class RecentRestockResponse {

    private final Long modelId;
    private final String modelName;
    private final String siteName;
    private final LocalDateTime restockedAt;

    private RecentRestockResponse(Long modelId, String modelName,
                                   String siteName, LocalDateTime restockedAt) {
        this.modelId = modelId;
        this.modelName = modelName;
        this.siteName = siteName;
        this.restockedAt = restockedAt;
    }

    public static RecentRestockResponse from(SiteOptionLog log) {
        return new RecentRestockResponse(
            log.getSiteOption().getModel().getId(),
            log.getSiteOption().getModel().getModelName(),
            log.getSiteOption().getSite().getSiteName(),
            log.getCapturedAt()
        );
    }

    public Long getModelId() { return modelId; }
    public String getModelName() { return modelName; }
    public String getSiteName() { return siteName; }
    public LocalDateTime getRestockedAt() { return restockedAt; }
}
