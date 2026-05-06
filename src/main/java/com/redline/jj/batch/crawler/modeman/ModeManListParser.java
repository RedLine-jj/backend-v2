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
import java.util.ArrayList;
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
        List<CrawlerProperties.Category> categories = crawlerProperties.modeMan().categories();
        if (categories == null || categories.isEmpty()) {
            throw new BusinessException(ErrorCode.EMPTY_CATEGORIES);
        }
        this.categoryNos = categories.stream()
            .map(CrawlerProperties.Category::categoryNo)
            .toList();
    }

    @Override
    public List<String> parseProductUrls(int page) throws BusinessException {
        Set<String> urls = new LinkedHashSet<>();
        boolean fetchFailed = false;
        List<String> failureMessages = new ArrayList<>();

        for (Integer categoryNo : categoryNos) {
            try {
                Document doc = Jsoup.connect(baseUrl + "/product/list.html?cate_no="
                                + categoryNo + "&page=" + page)
                        .userAgent("Mozilla/5.0")
                        .timeout(10_000)
                        .get();

                urls.addAll(doc.select("a[name^=anchorBoxName_]")
                    .stream()
                    .map(a -> a.attr("abs:href"))
                    .filter(href -> !href.isBlank())
                    .toList());
            } catch (IOException e) {
                fetchFailed = true;
                failureMessages.add("categoryNo=" + categoryNo + ", message=" + e.getMessage());
                log.warn("ModeMan 카테고리 목록 크롤링 실패: categoryNo={}, page={}", categoryNo, page, e);
            }
        }

        if (urls.isEmpty() && fetchFailed) {
            String detailMessage = "ModeMan 상품 URL 수집 실패: page=" + page
                + ", failures=" + String.join("; ", failureMessages);
            throw new BusinessException(ErrorCode.CRAWLING_FAILED, detailMessage);
        }
        return List.copyOf(urls);
    }
}
