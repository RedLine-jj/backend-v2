package com.redline.jj.batch.crawler.neststore;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redline.jj.batch.crawler.DetailParser;
import com.redline.jj.batch.crawler.dto.CrawledProduct;
import com.redline.jj.common.exception.BusinessException;
import com.redline.jj.common.exception.ErrorCode;
import com.redline.jj.config.CrawlerProperties;
import com.redline.jj.domain.model.Model.ModelType;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Component
public class NestStoreDetailParser implements DetailParser {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final List<CrawlerProperties.Category> categories;

    public NestStoreDetailParser(CrawlerProperties crawlerProperties) {
        if (crawlerProperties.nestStore() == null || crawlerProperties.nestStore().categories() == null) {
            this.categories = List.of();
        } else {
            this.categories = crawlerProperties.nestStore().categories();
        }
    }

    @Override
    public CrawledProduct parse(String url) throws BusinessException {
        return parseAll(url).get(0);
    }

    @Override
    public List<CrawledProduct> parseAll(String url) throws BusinessException {
        try {
            Document doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0")
                .timeout(10_000)
                .get();

            JsonNode productJson = extractProductJson(doc);
            String brandName = extractBrandName(doc, productJson);
            String modelName = extractModelName(doc, productJson);
            String imageUrl = extractImageUrl(doc, productJson);
            ModelType modelType = resolveModelType(url, doc);

            List<CrawledProduct> products = extractOfferProducts(url, doc, productJson, brandName,
                modelName, imageUrl, modelType);
            if (!products.isEmpty()) {
                return products;
            }

            return List.of(new CrawledProduct(
                brandName,
                modelName,
                modelName,
                extractOptionLabel(doc),
                extractPagePrice(doc),
                !isSoldOutBySelectors(doc),
                url,
                imageUrl,
                modelType
            ));

        } catch (BusinessException e) {
            throw e;
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.CRAWLING_FAILED);
        }
    }

    private JsonNode extractProductJson(Document doc) {
        return doc.select("script[type=application/ld+json]")
            .stream()
            .map(Element::data)
            .map(this::readJson)
            .filter(Objects::nonNull)
            .filter(json -> "Product".equals(json.path("@type").asText()))
            .findFirst()
            .orElse(null);
    }

    private JsonNode readJson(String json) {
        try {
            return OBJECT_MAPPER.readTree(json);
        } catch (IOException e) {
            return null;
        }
    }

    private String extractBrandName(Document doc, JsonNode productJson) {
        if (productJson != null) {
            String brandName = productJson.path("brand").path("name").asText();
            if (!brandName.isBlank()) {
                return normalizeText(brandName);
            }
        }

        return extractRequiredText(doc, "#brand, div.brand a, div.xans-product-detail .brand span");
    }

    private String extractModelName(Document doc, JsonNode productJson) {
        if (productJson != null) {
            String modelName = productJson.path("name").asText();
            if (!modelName.isBlank()) {
                return normalizeText(modelName);
            }
        }

        return extractRequiredText(doc, "meta[property=og:title], div.headingArea h2, .infoArea h2");
    }

    private String extractRequiredText(Document doc, String cssSelector) {
        Element element = doc.selectFirst(cssSelector);
        if (element == null) {
            throw new BusinessException(ErrorCode.CRAWLING_FAILED);
        }

        String text = element.hasAttr("content") ? element.attr("content") : element.text();
        if (text.isBlank()) {
            throw new BusinessException(ErrorCode.CRAWLING_FAILED);
        }
        return normalizeText(text.replaceAll("\\s*-\\s*NESTSTORE\\s*$", ""));
    }

    private List<CrawledProduct> extractOfferProducts(String url, Document doc, JsonNode productJson,
                                                      String brandName, String modelName, String imageUrl,
                                                      ModelType modelType) {
        if (productJson == null) {
            return List.of();
        }

        JsonNode offers = productJson.path("offers");
        if (!offers.isArray() || offers.isEmpty()) {
            return List.of();
        }

        Integer fallbackPrice = extractPagePrice(doc);
        List<CrawledProduct> products = new ArrayList<>();
        for (JsonNode offer : offers) {
            String offerUrl = offer.path("url").asText();
            products.add(new CrawledProduct(
                brandName,
                modelName,
                modelName,
                extractOfferOptionLabel(productJson, offer),
                extractOfferPrice(offer, fallbackPrice),
                extractOfferInStock(offer),
                offerUrl.isBlank() ? url : offerUrl,
                extractOfferImageUrl(offer, imageUrl),
                modelType
            ));
        }
        return products;
    }

    private String extractOfferOptionLabel(JsonNode productJson, JsonNode offer) {
        String offerName = offer.path("name").asText();
        String productName = productJson.path("name").asText();
        String optionLabel = offerName.replace(productName, "").trim();
        return optionLabel.isBlank() ? "기본" : normalizeText(optionLabel);
    }

    private Integer extractOfferPrice(JsonNode offer, Integer fallbackPrice) {
        if (offer.hasNonNull("price")) {
            return offer.path("price").asInt();
        }
        return fallbackPrice;
    }

    private boolean extractOfferInStock(JsonNode offer) {
        String availability = offer.path("availability").asText();
        return availability.isBlank() || availability.endsWith("InStock");
    }

    private String extractOfferImageUrl(JsonNode offer, String defaultImageUrl) {
        String offerImageUrl = offer.path("image").asText();
        return offerImageUrl.isBlank() ? defaultImageUrl : offerImageUrl;
    }

    private String extractOptionLabel(Document doc) {
        Element option = doc.select("select[name^=option1] option:not([disabled])")
            .stream()
            .filter(candidate -> !candidate.attr("value").equals("*"))
            .filter(candidate -> !candidate.attr("value").equals("**"))
            .filter(candidate -> !candidate.text().contains("필수"))
            .filter(candidate -> !candidate.text().contains("---"))
            .findFirst()
            .orElse(null);
        if (option == null || option.text().isBlank()) {
            return "기본";
        }
        return normalizeText(option.text());
    }

    private Integer extractPagePrice(Document doc) {
        Element priceEl = doc.selectFirst("strong#span_product_price_text, strong.price");
        if (priceEl == null || priceEl.text().isBlank()) {
            return null;
        }
        String digits = priceEl.text().replaceAll("[^0-9]", "");
        if (digits.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(digits);
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.CRAWLING_FAILED);
        }
    }

    private String extractImageUrl(Document doc, JsonNode productJson) {
        if (productJson != null) {
            JsonNode image = productJson.path("image");
            if (image.isArray() && !image.isEmpty()) {
                return image.get(0).asText(null);
            }
            if (image.isTextual()) {
                return image.asText();
            }
        }

        Element imageEl = doc.selectFirst("meta[property=og:image], meta[name=twitter:image]");
        if (imageEl != null && !imageEl.attr("content").isBlank()) {
            return imageEl.attr("abs:content");
        }

        imageEl = doc.selectFirst(".keyImg img, .thumbnail img, img.BigImage, .detailArea img");
        if (imageEl == null) {
            return null;
        }
        String imageUrl = imageEl.attr("abs:src");
        return imageUrl.isBlank() ? null : imageUrl;
    }

    private boolean isSoldOutBySelectors(Document doc) {
        return doc.selectFirst(".btn_soldout:not(.displaynone), .sold:not(.displaynone), img[alt*=품절]") != null;
    }

    private String normalizeText(String text) {
        if (text == null) {
            return null;
        }
        String htmlText = text.replaceAll("(?i)&nbsp;", " ");
        String unescapedText = Parser.unescapeEntities(htmlText, false);
        return Jsoup.parseBodyFragment(unescapedText).text()
            .replaceAll("(?i)\\(\\s*RESTOCK\\s*\\)", " ")
            .replace('\u00A0', ' ')
            .replaceAll("\\s+", " ")
            .trim();
    }

    private ModelType resolveModelType(String url, Document doc) {
        return categories.stream()
            .filter(category -> containsCategoryNo(url, category.categoryNo()))
            .map(CrawlerProperties.Category::modelType)
            .findFirst()
            .orElseGet(() -> inferModelType(doc, url));
    }

    private boolean containsCategoryNo(String url, int categoryNo) {
        String categoryValue = String.valueOf(categoryNo);
        return containsBounded(url, "cate_no=" + categoryValue)
            || containsBounded(url, "/category/" + categoryValue);
    }

    private boolean containsBounded(String text, String target) {
        int index = text.indexOf(target);
        if (index < 0) {
            return false;
        }

        int nextIndex = index + target.length();
        return nextIndex >= text.length() || !Character.isDigit(text.charAt(nextIndex));
    }

    private ModelType inferModelType(Document doc, String url) {
        String categoryText = doc.select(".path, .breadcrumb, .xans-product-headcategory, .menuCategory").text();
        String searchableText = (categoryText + " " + doc.title() + " " + url).toLowerCase(Locale.ROOT);

        if (searchableText.contains("jacket") || searchableText.contains("재킷") || searchableText.contains("자켓")) {
            return ModelType.DENIM_JACKET;
        }
        if (searchableText.contains("denim") || searchableText.contains("jean")
            || searchableText.contains("pants") || searchableText.contains("데님")
            || searchableText.contains("팬츠") || searchableText.contains("청바지")) {
            return ModelType.DENIM_PANTS;
        }
        return null;
    }
}
