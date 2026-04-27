package com.redline.jj.api.subscription.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SubscriptionRequest {

    @NotNull
    private Long modelId;
}
