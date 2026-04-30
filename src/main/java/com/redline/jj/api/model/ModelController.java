package com.redline.jj.api.model;

import com.redline.jj.api.model.dto.ModelDetailResponse;
import com.redline.jj.api.model.dto.ModelResponse;
import com.redline.jj.common.response.ApiResponse;
import com.redline.jj.common.response.CursorPage;
import com.redline.jj.domain.model.Model.ModelType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/models")
@RequiredArgsConstructor
public class ModelController {

    private final ModelService modelService;

    @GetMapping
    public ResponseEntity<ApiResponse<CursorPage<ModelResponse>>> getModels(
            @RequestParam(required = false) List<Long> brandIds,
            @RequestParam(required = false) List<ModelType> types,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(modelService.listModels(brandIds, types, cursor, size)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ModelDetailResponse>> getModel(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(modelService.getModel(id)));
    }

    @GetMapping("/count")
    public ResponseEntity<ApiResponse<Long>> getCount() {
        return ResponseEntity.ok(ApiResponse.ok(modelService.countModels()));
    }
}
