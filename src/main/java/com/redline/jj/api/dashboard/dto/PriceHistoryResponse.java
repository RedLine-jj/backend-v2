package com.redline.jj.api.dashboard.dto;

import com.redline.jj.domain.option.SiteOptionLog;

import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PriceHistoryResponse {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final Long modelId;
    private final List<SitePriceHistory> sites;

    public record SitePriceHistory(
        String siteName,
        Integer currentPrice,
        Integer priceChange,
        Integer minPrice,
        Integer maxPrice,
        List<DailyPrice> history
    ) {}

    public record DailyPrice(
        String date,
        Integer price
    ) {}

    private PriceHistoryResponse(Long modelId, List<SitePriceHistory> sites) {
        this.modelId = modelId;
        this.sites = sites;
    }

    public static PriceHistoryResponse from(Long modelId, List<SiteOptionLog> logs) {
        Map<String, List<SiteOptionLog>> bySite = logs.stream()
            .filter(log -> log.getPrice() != null)
            .collect(Collectors.groupingBy(
                log -> log.getSiteOption().getSite().getSiteName(),
                LinkedHashMap::new,
                Collectors.toList()
            ));

        List<SitePriceHistory> siteHistories = bySite.entrySet().stream()
            .map(entry -> {
                String siteName = entry.getKey();
                List<SiteOptionLog> siteLogs = entry.getValue();

                Map<String, Integer> dailyMin = siteLogs.stream()
                    .collect(Collectors.groupingBy(
                        log -> log.getCapturedAt().format(DATE_FMT),
                        Collectors.collectingAndThen(
                            Collectors.minBy(Comparator.comparingInt(SiteOptionLog::getPrice)),
                            opt -> opt.map(SiteOptionLog::getPrice).orElse(null)
                        )
                    ));

                List<DailyPrice> history = dailyMin.entrySet().stream()
                    .filter(e -> e.getValue() != null)
                    .sorted(Map.Entry.comparingByKey())
                    .map(e -> new DailyPrice(e.getKey(), e.getValue()))
                    .toList();

                Integer currentPrice = history.isEmpty()
                    ? null : history.get(history.size() - 1).price();

                Integer priceChange = null;
                if (history.size() >= 2) {
                    priceChange = history.get(history.size() - 1).price()
                        - history.get(history.size() - 2).price();
                }

                int min = siteLogs.stream().mapToInt(SiteOptionLog::getPrice).min().orElse(0);
                int max = siteLogs.stream().mapToInt(SiteOptionLog::getPrice).max().orElse(0);
                Integer minPrice = siteLogs.isEmpty() ? null : min;
                Integer maxPrice = siteLogs.isEmpty() ? null : max;

                return new SitePriceHistory(siteName, currentPrice, priceChange,
                    minPrice, maxPrice, history);
            })
            .toList();

        return new PriceHistoryResponse(modelId, siteHistories);
    }

    public Long getModelId() { return modelId; }
    public List<SitePriceHistory> getSites() { return sites; }
}
