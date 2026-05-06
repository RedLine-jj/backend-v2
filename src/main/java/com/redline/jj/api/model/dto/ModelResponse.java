package com.redline.jj.api.model.dto;

import com.redline.jj.domain.model.Model;
import lombok.Getter;

import java.io.Serializable;

@Getter
public class ModelResponse implements Serializable {

    private final Long id;
    private final Long brandId;
    private final String brandName;
    private final String brandNameKo;
    private final String modelName;
    private final String imageUrl;
    private final String type;
    private final Integer lowestPrice;

    private ModelResponse(Long id, Long brandId, String brandName, String brandNameKo,
                          String modelName, String imageUrl, String type, Integer lowestPrice) {
        this.id = id;
        this.brandId = brandId;
        this.brandName = brandName;
        this.brandNameKo = brandNameKo;
        this.modelName = modelName;
        this.imageUrl = imageUrl;
        this.type = type;
        this.lowestPrice = lowestPrice;
    }

    public static ModelResponse from(Model model, Integer lowestPrice) {
        return new ModelResponse(
            model.getId(),
            model.getBrand().getId(),
            model.getBrand().getBrandName(),
            model.getBrand().getBrandNameKo(),
            model.getModelName(),
            model.getImageUrl(),
            model.getModelType() != null ? model.getModelType().name() : null,
            lowestPrice
        );
    }
}
