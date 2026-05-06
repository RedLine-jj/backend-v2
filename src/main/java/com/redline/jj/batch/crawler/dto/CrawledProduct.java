package com.redline.jj.batch.crawler.dto;

import com.redline.jj.domain.model.Model.ModelType;

public record CrawledProduct(
    String brandName,
    String modelName,
    String siteModelName,
    String optionLabel,
    Integer price,
    boolean inStock,
    String url,
    String imageUrl,
    ModelType modelType
) {
}
