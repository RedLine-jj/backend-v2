package com.redline.jj.batch.crawler.neststore;

import com.redline.jj.batch.crawler.ListParser;
import com.redline.jj.common.exception.BusinessException;
import com.redline.jj.common.exception.ErrorCode;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Component
public class NestStoreListParser implements ListParser {

    private final String baseUrl;

    public NestStoreListParser(@Value("${crawler.neststore.base-url}") String baseUrl) {
        this.baseUrl = baseUrl;
    }

    @Override
    public List<String> parseProductUrls(int page) throws BusinessException {
        try {
            Document doc = Jsoup.connect(baseUrl + "/product/list.html?page=" + page)
                .userAgent("Mozilla/5.0")
                .timeout(10_000)
                .get();

            Elements links = doc.select("#prdList li a.name");
            if (links.isEmpty()) {
                return Collections.emptyList();
            }

            return links.stream()
                .map(a -> a.attr("abs:href"))
                .filter(href -> !href.isBlank())
                .toList();

        } catch (IOException e) {
            throw new BusinessException(ErrorCode.CRAWLING_FAILED);
        }
    }
}
