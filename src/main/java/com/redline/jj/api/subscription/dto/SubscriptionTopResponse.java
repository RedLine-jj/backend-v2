package com.redline.jj.api.subscription.dto;

import com.redline.jj.domain.subscription.ModelSubscriptionCount;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SubscriptionTopResponse {

    private Long modelId;
    private String modelName;
    private Long count;

    public static SubscriptionTopResponse from(ModelSubscriptionCount projection) {
        return new SubscriptionTopResponse(
            projection.getModelId(),
            projection.getModelName(),
            projection.getCount()
        );
    }
}
