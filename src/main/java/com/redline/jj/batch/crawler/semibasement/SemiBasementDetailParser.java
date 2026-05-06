package com.redline.jj.batch.crawler.semibasement;

import com.redline.jj.batch.crawler.DetailParser;
import com.redline.jj.batch.crawler.dto.CrawledProduct;
import com.redline.jj.common.exception.BusinessException;
import com.redline.jj.common.exception.ErrorCode;
import com.redline.jj.config.CrawlerProperties;
import com.redline.jj.domain.model.Model.ModelType;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class SemiBasementDetailParser implements DetailParser {

    private static final Pattern PRICE_PATTERN = Pattern.compile("([0-9,]+)원");
    private static final Pattern SIZE_PATTERN = Pattern.compile(
        "Size\\s*[：:]\\s*(.*?)(?=\\s+(?:Color|Material)\\s*[：:]|$)"
    );

    private final String baseUrl;
    private final List<CrawlerProperties.Category> categories;

    public SemiBasementDetailParser(CrawlerProperties crawlerProperties) {
        this.baseUrl = crawlerProperties.semiBasement().baseUrl().replaceAll("/+$", "");
        List<CrawlerProperties.Category> configuredCategories = crawlerProperties.semiBasement().categories();
        this.categories = configuredCategories == null ? List.of() : configuredCategories;
    }

    @Override
    public CrawledProduct parse(String url) throws BusinessException {
        return parseAll(url).get(0);
    }

    @Override
    public List<CrawledProduct> parseAll(String url) throws BusinessException {
        if (!url.startsWith(baseUrl)) {
            throw new BusinessException(ErrorCode.CRAWLING_FAILED);
        }

        try {
            Document doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0")
                .timeout(10_000)
                .get();

            String modelName = extractModelName(doc);
            String brandName = extractBrandName(doc).orElse("Semi Basement");
            Integer price = extractPrice(doc);
            boolean inStock = !doc.text().toUpperCase().contains("SOLDOUT")
                && !doc.text().contains("품절");
            String imageUrl = extractImageUrl(doc);
            ModelType modelType = resolveModelType(url);

            return extractOptionLabels(doc).stream()
                .map(optionLabel -> new CrawledProduct(
                    brandName,
                    modelName,
                    modelName,
                    optionLabel,
                    price,
                    inStock,
                    url,
                    imageUrl,
                    modelType
                ))
                .toList();
        } catch (BusinessException e) {
            throw e;
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.CRAWLING_FAILED);
        }
    }

    private List<String> extractOptionLabels(Document doc) {
        Optional<String> sizeText = doc.getAllElements().stream()
            .map(Element::ownText)
            .filter(text -> text.contains("Size"))
            .map(this::extractSizeText)
            .flatMap(Optional::stream)
            .findFirst()
            .or(() -> extractSizeText(doc.text()));
        if (sizeText.isEmpty()) {
            return List.of("기본");
        }

        List<String> optionLabels = Pattern.compile("[,，]")
            .splitAsStream(sizeText.get())
            .map(String::trim)
            .filter(size -> !size.isBlank())
            .distinct()
            .toList();
        return optionLabels.isEmpty() ? List.of("기본") : optionLabels;
    }

    private Optional<String> extractSizeText(String text) {
        Matcher matcher = SIZE_PATTERN.matcher(text);
        if (!matcher.find()) {
            return Optional.empty();
        }
        return Optional.of(matcher.group(1).trim());
    }

    private String extractModelName(Document doc) {
        return extractMetaContent(doc, "meta[property=og:title]")
            .map(title -> title.replaceAll("\\s*:\\s*Semi Basement General Store.*$", "").trim())
            .filter(title -> !title.isBlank())
            .or(() -> selectText(doc, ".prod_goods_form h1, .shop_view h1, .view_tit, h1"))
            .orElseThrow(() -> new BusinessException(ErrorCode.CRAWLING_FAILED));
    }

    private Optional<String> extractBrandName(Document doc) {
        return extractMetaContent(doc, "meta[property=product:brand], meta[name=brand]")
            .or(() -> selectText(doc, ".brand, .prod_brand, .shop_brand"));
    }

    private Optional<String> selectText(Document doc, String selector) {
        Element element = doc.selectFirst(selector);
        if (element == null || element.text().isBlank()) {
            return Optional.empty();
        }
        return Optional.of(element.text().trim());
    }

    private Optional<String> extractMetaContent(Document doc, String selector) {
        Element element = doc.selectFirst(selector);
        if (element == null || element.attr("content").isBlank()) {
            return Optional.empty();
        }
        return Optional.of(element.attr("content").trim());
    }

    private Integer extractPrice(Document doc) {
        Optional<String> metaPrice = extractMetaContent(doc,
            "meta[property=product:price:amount], meta[property=og:price:amount]");
        if (metaPrice.isPresent()) {
            return parsePrice(metaPrice.get());
        }

        Matcher matcher = PRICE_PATTERN.matcher(doc.text());
        if (!matcher.find()) {
            return null;
        }
        return parsePrice(matcher.group(1));
    }

    private Integer parsePrice(String text) {
        String digits = text.replaceAll("[^0-9]", "");
        if (digits.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(digits);
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.CRAWLING_FAILED);
        }
    }

    private String extractImageUrl(Document doc) {
        Element metaImage = doc.selectFirst("meta[property=og:image], meta[name=twitter:image]");
        if (metaImage != null && !metaImage.attr("content").isBlank()) {
            String imageUrl = metaImage.attr("abs:content");
            return imageUrl.isBlank() ? metaImage.attr("content") : imageUrl;
        }

        Element imageEl = doc.selectFirst(".shop_view img, .prod_goods_form img, img");
        if (imageEl == null) {
            return null;
        }
        String imageUrl = imageEl.attr("abs:src");
        return imageUrl.isBlank() ? null : imageUrl;
    }

    private ModelType resolveModelType(String url) {
        return categories.stream()
            .filter(category -> containsCategoryNo(url, category.categoryNo()))
            .map(CrawlerProperties.Category::modelType)
            .findFirst()
            .orElse(null);
    }

    private boolean containsCategoryNo(String url, int categoryNo) {
        String target = "/" + categoryNo;
        int index = url.indexOf(target);
        if (index < 0) {
            return false;
        }

        int nextIndex = index + target.length();
        return nextIndex >= url.length() || !Character.isDigit(url.charAt(nextIndex));
    }
}
