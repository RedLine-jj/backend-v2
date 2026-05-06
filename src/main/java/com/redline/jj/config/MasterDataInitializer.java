package com.redline.jj.config;

import com.redline.jj.domain.site.Site;
import com.redline.jj.domain.site.SiteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class MasterDataInitializer implements ApplicationRunner {

    private final SiteRepository siteRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedSites();
    }

    private void seedSites() {
        List<SiteSeed> sites = List.of(
            new SiteSeed("modeMan", "https://mode-man.com"),
            new SiteSeed("nestStore", "https://neststore.co.kr"),
            new SiteSeed("semiBasement", "https://semibasement.com")
        );

        for (SiteSeed site : sites) {
            if (siteRepository.existsBySiteName(site.siteName())) {
                continue;
            }

            siteRepository.save(Site.builder()
                .siteName(site.siteName())
                .siteLink(site.siteLink())
                .build());
            log.info("사이트 마스터 데이터 생성: siteName={}, siteLink={}", site.siteName(), site.siteLink());
        }
    }

    private record SiteSeed(String siteName, String siteLink) {
    }
}
