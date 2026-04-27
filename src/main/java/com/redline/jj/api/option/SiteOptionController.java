package com.redline.jj.api.option;

import com.redline.jj.api.option.dto.SiteOptionLogResponse;
import com.redline.jj.api.option.dto.SiteOptionResponse;
import com.redline.jj.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/site-options")
@RequiredArgsConstructor
public class SiteOptionController {

    private final SiteOptionService siteOptionService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<SiteOptionResponse>>> getSiteOptions(
            @RequestParam(required = false) Long siteId,
            @RequestParam(required = false) Long modelId,
            @RequestParam(required = false) Boolean inStock) {
        return ResponseEntity.ok(ApiResponse.ok(siteOptionService.listSiteOptions(siteId, modelId, inStock)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SiteOptionResponse>> getSiteOption(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(siteOptionService.getSiteOption(id)));
    }

    @GetMapping("/{id}/logs")
    public ResponseEntity<ApiResponse<List<SiteOptionLogResponse>>> getLogs(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(siteOptionService.getLogs(id)));
    }
}
