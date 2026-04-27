package com.redline.jj.api.master.dto;

import com.redline.jj.domain.brand.Brand;
import lombok.Getter;

import java.io.Serializable;

@Getter
public class BrandResponse implements Serializable {

    private final Long id;
    private final String brandName;
    private final String brandNameKo;

    private BrandResponse(Long id, String brandName, String brandNameKo) {
        this.id = id;
        this.brandName = brandName;
        this.brandNameKo = brandNameKo;
    }

    public static BrandResponse from(Brand brand) {
        return new BrandResponse(brand.getId(), brand.getBrandName(), brand.getBrandNameKo());
    }
}
