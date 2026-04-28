package com.redline.jj.batch.job;

public record RestockEvent(
    Long modelId,
    String modelName,
    String brandName
) {}
