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
    private final String brandName;
    private final String imageUrl;
    private final String optionLabel;
    private final String url;
    private final boolean status;
    private final Integer price;
    private final LocalDateTime lastCapturedAt;

    private SiteOptionResponse(Long id, Long siteId, String siteName, String siteLink,
                                Long modelId, String modelName, String brandName, String imageUrl,
                                String optionLabel, String url, boolean status,
                                Integer price, LocalDateTime lastCapturedAt) {
        this.id = id;
        this.siteId = siteId;
        this.siteName = siteName;
        this.siteLink = siteLink;
        this.modelId = modelId;
        this.modelName = modelName;
        this.brandName = brandName;
        this.imageUrl = imageUrl;
        this.optionLabel = optionLabel;
        this.url = url;
        this.status = status;
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
            siteOption.getModel().getBrand().getBrandName(),
            siteOption.getModel().getImageUrl(),
            siteOption.getOptionLabel(),
            siteOption.getUrl(),
            siteOption.isInStock(),
            siteOption.getPrice(),
            siteOption.getLastCapturedAt()
        );
    }
}
