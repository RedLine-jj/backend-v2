package com.redline.jj.batch.crawler.semibasement.dto;

public record SemiBasementProductResponse(
    String productCode,
    String productName,
    String brandName,
    String optionName,
    Integer price,
    Integer stockCount
) {}
