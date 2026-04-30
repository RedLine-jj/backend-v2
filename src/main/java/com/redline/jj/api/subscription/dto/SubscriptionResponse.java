package com.redline.jj.api.subscription.dto;

import com.redline.jj.domain.subscription.Subscription;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class SubscriptionResponse {

    private Long id;
    private Long modelId;
    private String modelName;
    private String brandName;
    private String imageUrl;
    private LocalDateTime createdAt;

    public static SubscriptionResponse from(Subscription subscription) {
        return new SubscriptionResponse(
            subscription.getId(),
            subscription.getModel().getId(),
            subscription.getModel().getModelName(),
            subscription.getModel().getBrand().getBrandName(),
            subscription.getModel().getImageUrl(),
            subscription.getCreatedAt()
        );
    }
}
