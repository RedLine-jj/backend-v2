package com.redline.jj.api.restock;

import com.redline.jj.api.dashboard.DashboardService;
import com.redline.jj.api.dashboard.dto.RecentRestockResponse;
import com.redline.jj.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/restocks")
@RequiredArgsConstructor
public class RestockController {

    private final DashboardService dashboardService;

    @GetMapping("/recent")
    public ResponseEntity<ApiResponse<List<RecentRestockResponse>>> getRecentRestocks() {
        return ResponseEntity.ok(ApiResponse.ok(dashboardService.getRecentRestocks()));
    }
}
