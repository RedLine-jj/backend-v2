package com.redline.jj.config;

import com.redline.jj.domain.model.Model.ModelType;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "crawler")
public record CrawlerProperties(
    ModeMan modeMan
) {

    public record ModeMan(
        String baseUrl,
        List<Category> categories
    ) {
    }

    public record Category(
        int categoryNo,
        ModelType modelType
    ) {
    }
}
