package com.redline.jj.batch.crawler.modeman;

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
public class ModeManDetailParser implements DetailParser {

    @Override
    public CrawledProduct parse(String url) throws BusinessException {
        try {
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0")
                    .get();

            String brandName = extractRequiredText(doc, "div.brand a");
            String modelName = extractRequiredText(doc, "div.headingArea h2");

            String optionLabel = extractOptionLabel(doc);
            Integer price = extractPrice(doc);
            boolean inStock = doc.selectFirst("div.soldOut") == null;

            return new CrawledProduct(brandName, modelName, modelName, optionLabel, price, inStock, url);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.CRAWLING_FAILED);
        }
    }

    private String extractRequiredText(Document doc, String cssSelector) {
        Element element = doc.selectFirst(cssSelector);
        if (element == null || element.text().isBlank()) {
            throw new BusinessException(ErrorCode.CRAWLING_FAILED);
        }
        return element.text().trim();
    }

    private String extractOptionLabel(Document doc) {
        Element option = doc.selectFirst("select[name^=option1] option:first-child");
        if (option == null || option.text().isBlank()) {
            return "기본";
        }
        return option.text().trim();
    }

    private Integer extractPrice(Document doc) {
        Element priceEl = doc.selectFirst("strong#span_product_price_text");
        if (priceEl == null || priceEl.text().isBlank()) {
            return null;
        }
        String digits = priceEl.text().replaceAll("[^0-9]", "");
        return digits.isEmpty() ? null : Integer.parseInt(digits);
    }
}
