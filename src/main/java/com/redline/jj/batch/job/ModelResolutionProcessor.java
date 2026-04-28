package com.redline.jj.batch.job;

import com.redline.jj.batch.crawler.DetailParser;
import com.redline.jj.batch.crawler.dto.CrawledProduct;
import com.redline.jj.batch.dto.ResolvedItem;
import com.redline.jj.batch.matching.ModelResolutionService;
import com.redline.jj.common.exception.BusinessException;
import com.redline.jj.common.exception.ErrorCode;
import com.redline.jj.domain.model.Model;
import com.redline.jj.domain.site.Site;
import com.redline.jj.domain.site.SiteRepository;
import org.springframework.batch.item.ItemProcessor;

public class ModelResolutionProcessor implements ItemProcessor<String, ResolvedItem> {

    private final DetailParser detailParser;
    private final ModelResolutionService modelResolutionService;
    private final SiteRepository siteRepository;
    private final String siteName;

    private Site cachedSite;

    public ModelResolutionProcessor(DetailParser detailParser,
                                    ModelResolutionService modelResolutionService,
                                    SiteRepository siteRepository,
                                    String siteName) {
        this.detailParser = detailParser;
        this.modelResolutionService = modelResolutionService;
        this.siteRepository = siteRepository;
        this.siteName = siteName;
    }

    @Override
    public ResolvedItem process(String url) {
        CrawledProduct product = detailParser.parse(url);
        Site site = resolveSite();
        Model model = modelResolutionService.resolve(product);
        return new ResolvedItem(
            model, site, product.optionLabel(), product.price(),
            product.inStock(), url, product.siteModelName()
        );
    }

    private Site resolveSite() {
        if (cachedSite == null) {
            cachedSite = siteRepository.findBySiteName(siteName)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        }
        return cachedSite;
    }
}
