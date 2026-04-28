package com.redline.jj.batch.crawler.semibasement.dto;

import java.util.List;

public record SemiBasementProductListResponse(
    List<SemiBasementProductItem> data,
    Integer totalCount
) {
    public record SemiBasementProductItem(
        String productCode,
        String productUrl
    ) {}
}
