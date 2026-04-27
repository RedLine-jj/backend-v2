package com.redline.jj.api.model.dto;

import lombok.Getter;

import java.io.Serializable;
import java.util.List;

@Getter
public class ModelPageResponse implements Serializable {

    private final List<ModelResponse> items;
    private final Long nextCursor;
    private final boolean hasNext;

    private ModelPageResponse(List<ModelResponse> items, Long nextCursor, boolean hasNext) {
        this.items = items;
        this.nextCursor = nextCursor;
        this.hasNext = hasNext;
    }

    public static ModelPageResponse of(List<ModelResponse> items, Long nextCursor, boolean hasNext) {
        return new ModelPageResponse(items, nextCursor, hasNext);
    }
}
