package com.redline.jj.batch.crawler.neststore;

import com.redline.jj.batch.crawler.DetailParser;
import com.redline.jj.batch.crawler.dto.CrawledProduct;
import com.redline.jj.common.exception.BusinessException;
import com.redline.jj.common.exception.ErrorCode;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class NestStoreDetailParser implements DetailParser {

    @Override
    public CrawledProduct parse(String url) throws BusinessException {
        try {
            Document doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0")
                .timeout(10_000)
                .get();

            Element brandEl = doc.selectFirst("div.xans-product-detail .brand span");
            if (brandEl == null) {
                throw new BusinessException(ErrorCode.CRAWLING_FAILED);
            }
            String brandName = brandEl.text();
            if (brandName.isBlank()) {
                throw new BusinessException(ErrorCode.CRAWLING_FAILED);
            }

            Element modelEl = doc.selectFirst("div.headingArea h2");
            if (modelEl == null) {
                throw new BusinessException(ErrorCode.CRAWLING_FAILED);
            }
            String modelName = modelEl.text();
            if (modelName.isBlank()) {
                throw new BusinessException(ErrorCode.CRAWLING_FAILED);
            }

            String siteModelName = modelName;

            Element firstOption = doc.selectFirst("select.option_select option:not([value=''])");
            String optionLabel = firstOption != null ? firstOption.text() : "기본";

            Element priceEl = doc.selectFirst("strong.price");
            Integer price = null;
            if (priceEl != null) {
                String digits = priceEl.text().replaceAll("[^0-9]", "");
                if (!digits.isBlank()) {
                    try {
                        price = Integer.parseInt(digits);
                    } catch (NumberFormatException e) {
                        throw new BusinessException(ErrorCode.CRAWLING_FAILED);
                    }
                }
            }

            boolean inStock = doc.selectFirst(".btn_soldout") == null;
            String imageUrl = extractImageUrl(doc);

            return new CrawledProduct(
                brandName,
                modelName,
                siteModelName,
                optionLabel,
                price,
                inStock,
                url,
                imageUrl,
                null // TODO: NestStore 카테고리-타입 매핑이 정리되면 ModelResolutionService 입력으로 전달한다.
            );

        } catch (BusinessException e) {
            throw e;
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.CRAWLING_FAILED);
        }
    }

    private String extractImageUrl(Document doc) {
        Element imageEl = doc.selectFirst("meta[property=og:image], meta[name=twitter:image]");
        if (imageEl != null && !imageEl.attr("content").isBlank()) {
            return imageEl.attr("abs:content");
        }

        imageEl = doc.selectFirst(".keyImg img, .thumbnail img, img.BigImage");
        if (imageEl == null) {
            return null;
        }
        String imageUrl = imageEl.attr("abs:src");
        return imageUrl.isBlank() ? null : imageUrl;
    }
}
