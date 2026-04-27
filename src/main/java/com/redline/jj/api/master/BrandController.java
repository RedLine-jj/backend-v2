package com.redline.jj.api.master;

import com.redline.jj.api.master.dto.BrandResponse;
import com.redline.jj.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/brands")
@RequiredArgsConstructor
public class BrandController {

    private final MasterDataService masterDataService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<BrandResponse>>> getBrands() {
        return ResponseEntity.ok(ApiResponse.ok(masterDataService.getBrands()));
    }
}
