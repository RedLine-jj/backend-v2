package com.redline.jj.api.master.dto;

import com.redline.jj.domain.model.Model.ModelType;
import lombok.Getter;

@Getter
public class ModelTypeResponse {

    private final String code;
    private final String label;

    private ModelTypeResponse(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public static ModelTypeResponse from(ModelType modelType) {
        return new ModelTypeResponse(modelType.name(), modelType.getLabel());
    }
}
