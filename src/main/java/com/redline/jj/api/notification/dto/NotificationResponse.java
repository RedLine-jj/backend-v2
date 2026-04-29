package com.redline.jj.api.notification.dto;

import com.redline.jj.domain.notification.RestockNotification;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class NotificationResponse {

    private Long id;
    private Long modelId;
    private String modelName;
    private String brandName;
    private boolean read;
    private LocalDateTime createdAt;

    public static NotificationResponse from(RestockNotification notification) {
        return new NotificationResponse(
            notification.getId(),
            notification.getModel().getId(),
            notification.getModel().getModelName(),
            notification.getModel().getBrand().getBrandName(),
            notification.isRead(),
            notification.getCreatedAt()
        );
    }
}
