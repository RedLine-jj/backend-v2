package com.redline.jj.batch.crawler.dto;

public record CrawledProduct(
    String brandName,
    String modelName,
    String siteModelName,
    String optionLabel,
    Integer price,
    boolean inStock,
    String url
) {
}
