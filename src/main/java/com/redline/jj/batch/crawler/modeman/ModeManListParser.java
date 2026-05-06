package com.redline.jj.batch.crawler.modeman;

import com.redline.jj.batch.crawler.ListParser;
import com.redline.jj.common.exception.BusinessException;
import com.redline.jj.common.exception.ErrorCode;
import com.redline.jj.config.CrawlerProperties;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
public class ModeManListParser implements ListParser {

    private final String baseUrl;
    private final List<Integer> categoryNos;

    public ModeManListParser(CrawlerProperties crawlerProperties) {
        this.baseUrl = crawlerProperties.modeMan().baseUrl().replaceAll("/+$", "");
        this.categoryNos = crawlerProperties.modeMan().categories().stream()
            .map(CrawlerProperties.Category::categoryNo)
            .toList();
    }

    @Override
    public List<String> parseProductUrls(int page) throws BusinessException {
        Set<String> urls = new LinkedHashSet<>();
        boolean fetchSucceeded = false;
        boolean fetchFailed = false;

        for (Integer categoryNo : categoryNos) {
            try {
                Document doc = Jsoup.connect(baseUrl + "/product/list.html?cate_no="
                                + categoryNo + "&page=" + page)
                        .userAgent("Mozilla/5.0")
                        .timeout(10_000)
                        .get();
                fetchSucceeded = true;

                urls.addAll(doc.select("a[name^=anchorBoxName_]")
                    .stream()
                    .map(a -> a.attr("abs:href"))
                    .filter(href -> !href.isBlank())
                    .toList());
            } catch (IOException e) {
                fetchFailed = true;
                log.warn("ModeMan 카테고리 목록 크롤링 실패: categoryNo={}, page={}", categoryNo, page, e);
            }
        }

        if (urls.isEmpty() && !fetchSucceeded && fetchFailed) {
            throw new BusinessException(ErrorCode.CRAWLING_FAILED);
        }
        return List.copyOf(urls);
    }
}
