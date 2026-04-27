package com.redline.jj.api.option.dto;

import com.redline.jj.domain.option.SiteOptionLog;
import lombok.Getter;

import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
public class SiteOptionLogResponse implements Serializable {

    private final Long id;
    private final LocalDateTime capturedAt;
    private final String optionLabel;
    private final Integer price;
    private final boolean inStock;

    private SiteOptionLogResponse(Long id, LocalDateTime capturedAt, String optionLabel,
                                   Integer price, boolean inStock) {
        this.id = id;
        this.capturedAt = capturedAt;
        this.optionLabel = optionLabel;
        this.price = price;
        this.inStock = inStock;
    }

    public static SiteOptionLogResponse from(SiteOptionLog log) {
        return new SiteOptionLogResponse(
            log.getId(),
            log.getCapturedAt(),
            log.getOptionLabel(),
            log.getPrice(),
            log.isInStock()
        );
    }
}
