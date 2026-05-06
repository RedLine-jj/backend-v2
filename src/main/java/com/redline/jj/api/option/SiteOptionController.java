package com.redline.jj.api.option;

import com.redline.jj.api.option.dto.SiteOptionLogResponse;
import com.redline.jj.api.option.dto.SiteOptionResponse;
import com.redline.jj.common.response.ApiResponse;
import com.redline.jj.common.response.CursorPage;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/site-options")
@RequiredArgsConstructor
public class SiteOptionController {

    private final SiteOptionService siteOptionService;

    @GetMapping
    public ResponseEntity<ApiResponse<CursorPage<SiteOptionResponse>>> getSiteOptions(
            @RequestParam(required = false) Long siteId,
            @RequestParam(required = false) Long modelId,
            @RequestParam(required = false) Boolean status,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(
            siteOptionService.listSiteOptions(siteId, modelId, status, cursor, size)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SiteOptionResponse>> getSiteOption(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(siteOptionService.getSiteOption(id)));
    }

    @GetMapping("/{id}/logs")
    public ResponseEntity<ApiResponse<CursorPage<SiteOptionLogResponse>>> getLogs(
            @PathVariable Long id,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(siteOptionService.getLogs(id, cursor, size)));
    }
}
