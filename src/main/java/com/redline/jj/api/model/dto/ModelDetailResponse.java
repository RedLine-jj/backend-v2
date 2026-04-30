package com.redline.jj.api.model.dto;

import com.redline.jj.api.option.dto.SiteOptionResponse;
import com.redline.jj.domain.model.Model;
import com.redline.jj.domain.option.SiteOption;
import lombok.Getter;

import java.io.Serializable;
import java.util.List;
import java.util.OptionalInt;

@Getter
public class ModelDetailResponse implements Serializable {

    private final Long id;
    private final Long brandId;
    private final String brandName;
    private final String brandNameKo;
    private final String modelName;
    private final String imageUrl;
    private final String type;
    private final Integer lowestPrice;
    private final List<SiteOptionResponse> siteOptions;

    private ModelDetailResponse(Long id, Long brandId, String brandName, String brandNameKo,
                                 String modelName, String imageUrl, String type,
                                 Integer lowestPrice, List<SiteOptionResponse> siteOptions) {
        this.id = id;
        this.brandId = brandId;
        this.brandName = brandName;
        this.brandNameKo = brandNameKo;
        this.modelName = modelName;
        this.imageUrl = imageUrl;
        this.type = type;
        this.lowestPrice = lowestPrice;
        this.siteOptions = siteOptions;
    }

    public static ModelDetailResponse from(Model model, List<SiteOption> options) {
        OptionalInt min = options.stream()
            .filter(so -> so.isInStock() && so.getPrice() != null)
            .mapToInt(SiteOption::getPrice)
            .min();
        Integer lowestPrice = min.isPresent() ? min.getAsInt() : null;

        return new ModelDetailResponse(
            model.getId(),
            model.getBrand().getId(),
            model.getBrand().getBrandName(),
            model.getBrand().getBrandNameKo(),
            model.getModelName(),
            model.getImageUrl(),
            model.getModelType() != null ? model.getModelType().name() : null,
            lowestPrice,
            options.stream().map(SiteOptionResponse::from).toList()
        );
    }
}
