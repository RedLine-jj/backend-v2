package com.redline.jj.api.dashboard.dto;

import com.redline.jj.domain.option.SiteOptionLog;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PriceHistoryResponse {

    private final Long modelId;
    private final List<SiteOptionHistory> histories;

    public record SiteOptionHistory(
        Long siteOptionId,
        String siteName,
        String optionLabel,
        List<PricePoint> points
    ) {}

    public record PricePoint(
        LocalDateTime capturedAt,
        Integer price,
        boolean inStock
    ) {}

    private PriceHistoryResponse(Long modelId, List<SiteOptionHistory> histories) {
        this.modelId = modelId;
        this.histories = histories;
    }

    public static PriceHistoryResponse from(Long modelId, List<SiteOptionLog> logs) {
        Map<Long, List<SiteOptionLog>> bySiteOption = logs.stream()
            .collect(Collectors.groupingBy(
                sol -> sol.getSiteOption().getId(),
                LinkedHashMap::new,
                Collectors.toList()
            ));

        List<SiteOptionHistory> histories = bySiteOption.entrySet().stream()
            .map(entry -> {
                SiteOptionLog first = entry.getValue().get(0);
                List<PricePoint> points = entry.getValue().stream()
                    .map(sol -> new PricePoint(sol.getCapturedAt(), sol.getPrice(), sol.isInStock()))
                    .toList();
                return new SiteOptionHistory(
                    entry.getKey(),
                    first.getSiteOption().getSite().getSiteName(),
                    first.getOptionLabel(),
                    points
                );
            })
            .toList();

        return new PriceHistoryResponse(modelId, histories);
    }

    public Long getModelId() {
        return modelId;
    }

    public List<SiteOptionHistory> getHistories() {
        return histories;
    }
}
