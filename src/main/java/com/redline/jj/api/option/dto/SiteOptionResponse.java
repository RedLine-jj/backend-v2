package com.redline.jj.api.option.dto;

import com.redline.jj.domain.option.SiteOption;
import lombok.Getter;

import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
public class SiteOptionResponse implements Serializable {

    private final Long id;
    private final Long siteId;
    private final String siteName;
    private final String siteLink;
    private final Long modelId;
    private final String modelName;
    private final String optionLabel;
    private final String url;
    private final boolean inStock;
    private final Integer price;
    private final LocalDateTime lastCapturedAt;

    private SiteOptionResponse(Long id, Long siteId, String siteName, String siteLink,
                                Long modelId, String modelName, String optionLabel, String url,
                                boolean inStock, Integer price, LocalDateTime lastCapturedAt) {
        this.id = id;
        this.siteId = siteId;
        this.siteName = siteName;
        this.siteLink = siteLink;
        this.modelId = modelId;
        this.modelName = modelName;
        this.optionLabel = optionLabel;
        this.url = url;
        this.inStock = inStock;
        this.price = price;
        this.lastCapturedAt = lastCapturedAt;
    }

    public static SiteOptionResponse from(SiteOption siteOption) {
        return new SiteOptionResponse(
            siteOption.getId(),
            siteOption.getSite().getId(),
            siteOption.getSite().getSiteName(),
            siteOption.getSite().getSiteLink(),
            siteOption.getModel().getId(),
            siteOption.getModel().getModelName(),
            siteOption.getOptionLabel(),
            siteOption.getUrl(),
            siteOption.isInStock(),
            siteOption.getPrice(),
            siteOption.getLastCapturedAt()
        );
    }
}
