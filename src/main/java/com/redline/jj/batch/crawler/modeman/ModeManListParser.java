package com.redline.jj.batch.crawler.modeman;

import com.redline.jj.batch.crawler.ListParser;
import com.redline.jj.common.exception.BusinessException;
import com.redline.jj.common.exception.ErrorCode;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
public class ModeManListParser implements ListParser {

    private static final List<Integer> CRAWL_CATEGORY_NOS = List.of(858, 263);

    private final String baseUrl;

    public ModeManListParser(@Value("${crawler.modeman.base-url}") String baseUrl) {
        this.baseUrl = baseUrl.replaceAll("/+$", "");
    }

    @Override
    public List<String> parseProductUrls(int page) throws BusinessException {
        try {
            Set<String> urls = new LinkedHashSet<>();
            for (Integer categoryNo : CRAWL_CATEGORY_NOS) {
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
            }

            return List.copyOf(urls);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.CRAWLING_FAILED);
        }
    }
}
