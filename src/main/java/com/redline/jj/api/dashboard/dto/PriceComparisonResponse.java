package com.redline.jj.api.dashboard.dto;

import com.redline.jj.domain.model.Model;
import com.redline.jj.domain.option.SiteOption;
import com.redline.jj.domain.site.Site;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PriceComparisonResponse {

    private final Long modelId;
    private final String modelName;
    private final String brandName;
    private final String imageUrl;
    private final List<SiteRow> sites;

    public record SiteRow(
        Long siteId,
        String siteName,
        String siteLink,
        List<OptionEntry> options
    ) {}

    public record OptionEntry(
        Long siteOptionId,
        String optionLabel,
        boolean status,
        Integer price,
        String url,
        LocalDateTime lastCapturedAt
    ) {}

    private PriceComparisonResponse(Long modelId, String modelName, String brandName,
                                     String imageUrl, List<SiteRow> sites) {
        this.modelId = modelId;
        this.modelName = modelName;
        this.brandName = brandName;
        this.imageUrl = imageUrl;
        this.sites = sites;
    }

    public static PriceComparisonResponse from(Model model, List<SiteOption> options) {
        Map<Long, List<SiteOption>> bySite = options.stream()
            .collect(Collectors.groupingBy(
                so -> so.getSite().getId(),
                LinkedHashMap::new,
                Collectors.toList()
            ));

        List<SiteRow> siteRows = bySite.entrySet().stream()
            .map(entry -> {
                Site site = entry.getValue().get(0).getSite();
                List<OptionEntry> optionEntries = entry.getValue().stream()
                    .map(so -> new OptionEntry(
                        so.getId(),
                        so.getOptionLabel(),
                        so.isInStock(),
                        so.getPrice(),
                        so.getUrl(),
                        so.getLastCapturedAt()
                    ))
                    .toList();
                return new SiteRow(site.getId(), site.getSiteName(), site.getSiteLink(), optionEntries);
            })
            .toList();

        return new PriceComparisonResponse(
            model.getId(),
            model.getModelName(),
            model.getBrand().getBrandName(),
            model.getImageUrl(),
            siteRows
        );
    }

    public Long getModelId() { return modelId; }
    public String getModelName() { return modelName; }
    public String getBrandName() { return brandName; }
    public String getImageUrl() { return imageUrl; }
    public List<SiteRow> getSites() { return sites; }
}
