package com.redline.jj.common.response;

import lombok.Getter;

import java.io.Serializable;
import java.util.List;

@Getter
public class CursorPage<T> implements Serializable {

    private final List<T> content;
    private final Long nextCursor;
    private final boolean hasNext;

    private CursorPage(List<T> content, Long nextCursor, boolean hasNext) {
        this.content = content;
        this.nextCursor = nextCursor;
        this.hasNext = hasNext;
    }

    public static <T> CursorPage<T> of(List<T> content, Long nextCursor, boolean hasNext) {
        return new CursorPage<>(content, nextCursor, hasNext);
    }
}
