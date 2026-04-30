package com.redline.jj.api.dashboard;

import com.redline.jj.api.dashboard.dto.PriceComparisonResponse;
import com.redline.jj.api.dashboard.dto.PriceHistoryResponse;
import com.redline.jj.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/price-comparison")
    public ResponseEntity<ApiResponse<PriceComparisonResponse>> getPriceComparison(
            @RequestParam Long modelId) {
        return ResponseEntity.ok(ApiResponse.ok(dashboardService.getPriceComparison(modelId)));
    }

    @GetMapping("/price-history")
    public ResponseEntity<ApiResponse<PriceHistoryResponse>> getPriceHistory(
            @RequestParam Long modelId,
            @RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(ApiResponse.ok(dashboardService.getPriceHistory(modelId, days)));
    }
}
