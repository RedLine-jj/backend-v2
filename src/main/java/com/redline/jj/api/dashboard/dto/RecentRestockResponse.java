package com.redline.jj.api.dashboard.dto;

import com.redline.jj.domain.notification.RestockNotification;

import java.time.LocalDateTime;

public class RecentRestockResponse {

    private final Long notificationId;
    private final Long modelId;
    private final String modelName;
    private final String brandName;
    private final LocalDateTime notifiedAt;

    private RecentRestockResponse(Long notificationId, Long modelId, String modelName,
                                   String brandName, LocalDateTime notifiedAt) {
        this.notificationId = notificationId;
        this.modelId = modelId;
        this.modelName = modelName;
        this.brandName = brandName;
        this.notifiedAt = notifiedAt;
    }

    public static RecentRestockResponse from(RestockNotification n) {
        return new RecentRestockResponse(
            n.getId(),
            n.getModel().getId(),
            n.getModel().getModelName(),
            n.getModel().getBrand().getBrandName(),
            n.getCreatedAt()
        );
    }

    public Long getNotificationId() {
        return notificationId;
    }

    public Long getModelId() {
        return modelId;
    }

    public String getModelName() {
        return modelName;
    }

    public String getBrandName() {
        return brandName;
    }

    public LocalDateTime getNotifiedAt() {
        return notifiedAt;
    }
}
