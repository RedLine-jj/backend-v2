package com.redline.jj.api.master;

import com.redline.jj.api.master.dto.ModelTypeResponse;
import com.redline.jj.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/models")
@RequiredArgsConstructor
public class ModelTypeController {

    private final MasterDataService masterDataService;

    @GetMapping("/types")
    public ResponseEntity<ApiResponse<List<ModelTypeResponse>>> getModelTypes() {
        return ResponseEntity.ok(ApiResponse.ok(masterDataService.getModelTypes()));
    }
}
