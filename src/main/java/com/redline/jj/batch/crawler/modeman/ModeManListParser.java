package com.redline.jj.batch.crawler.modeman;

import com.redline.jj.batch.crawler.ListParser;
import com.redline.jj.common.exception.BusinessException;
import com.redline.jj.common.exception.ErrorCode;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Component
public class ModeManListParser implements ListParser {

    private final String baseUrl;

    public ModeManListParser(@Value("${crawler.modeman.base-url}") String baseUrl) {
        this.baseUrl = baseUrl;
    }

    @Override
    public List<String> parseProductUrls(int page) throws BusinessException {
        try {
            Document doc = Jsoup.connect(baseUrl + "/product/list.html?page=" + page)
                    .userAgent("Mozilla/5.0")
                    .get();

            List<String> urls = doc.select("ul.prdList li a.name")
                    .stream()
                    .map(a -> a.attr("abs:href"))
                    .filter(href -> !href.isBlank())
                    .toList();

            return urls.isEmpty() ? Collections.emptyList() : urls;
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.CRAWLING_FAILED);
        }
    }
}
