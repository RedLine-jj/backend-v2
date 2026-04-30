package com.redline.jj.api.subscription;

import com.redline.jj.api.subscription.dto.SubscriptionRequest;
import com.redline.jj.api.subscription.dto.SubscriptionResponse;
import com.redline.jj.api.subscription.dto.SubscriptionTopResponse;
import com.redline.jj.common.response.ApiResponse;
import com.redline.jj.common.response.CursorPage;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @GetMapping
    public ResponseEntity<ApiResponse<CursorPage<SubscriptionResponse>>> getMySubscriptions(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(
            subscriptionService.getMySubscriptions(userDetails.getUsername(), cursor, size)));
    }

    @GetMapping("/count")
    public ResponseEntity<ApiResponse<Long>> getMySubscriptionCount(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.ok(
            subscriptionService.getMySubscriptionCount(userDetails.getUsername())));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SubscriptionResponse>> subscribe(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody SubscriptionRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
            subscriptionService.subscribe(userDetails.getUsername(), request.getModelId())));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> cancel(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        subscriptionService.cancel(userDetails.getUsername(), id);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @GetMapping("/top")
    public ResponseEntity<ApiResponse<List<SubscriptionTopResponse>>> getTop10() {
        return ResponseEntity.ok(ApiResponse.ok(subscriptionService.getTop10()));
    }
}
