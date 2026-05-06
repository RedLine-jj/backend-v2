package com.redline.jj.config;

import com.redline.jj.domain.site.Site;
import com.redline.jj.domain.site.SiteRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
public class MasterDataInitializer implements ApplicationRunner {

    private final SiteRepository siteRepository;
    private final String modeManBaseUrl;
    private final String nestStoreBaseUrl;
    private final String semiBasementBaseUrl;

    public MasterDataInitializer(
        SiteRepository siteRepository,
        @Value("${crawler.modeman.base-url}") String modeManBaseUrl,
        @Value("${crawler.neststore.base-url}") String nestStoreBaseUrl,
        @Value("${crawler.semibasement.base-url}") String semiBasementBaseUrl
    ) {
        this.siteRepository = siteRepository;
        this.modeManBaseUrl = modeManBaseUrl;
        this.nestStoreBaseUrl = nestStoreBaseUrl;
        this.semiBasementBaseUrl = semiBasementBaseUrl;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedSites();
    }

    private void seedSites() {
        List<SiteSeed> sites = List.of(
            new SiteSeed("modeMan", modeManBaseUrl),
            new SiteSeed("nestStore", nestStoreBaseUrl),
            new SiteSeed("semiBasement", semiBasementBaseUrl)
        );

        for (SiteSeed site : sites) {
            if (siteRepository.existsBySiteName(site.siteName())) {
                continue;
            }

            siteRepository.save(Site.builder()
                .siteName(site.siteName())
                .siteLink(normalizeUrl(site.siteLink()))
                .build());
            log.info("사이트 마스터 데이터 생성: siteName={}, siteLink={}", site.siteName(), site.siteLink());
        }
    }

    private String normalizeUrl(String url) {
        return url.replaceAll("/+$", "");
    }

    private record SiteSeed(String siteName, String siteLink) {
    }
}
