package com.redline.jj.batch.crawler.modeman;

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
import java.util.Objects;

@Component
public class ModeManDetailParser implements DetailParser {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final List<CrawlerProperties.Category> categories;

    public ModeManDetailParser(CrawlerProperties crawlerProperties) {
        this.categories = crawlerProperties.modeMan().categories();
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
            String imageUrl = extractImageUrl(productJson);
            ModelType modelType = extractModelType(url);

            List<CrawledProduct> products = extractOfferProducts(url, doc, productJson, brandName, modelName, imageUrl, modelType);
            if (!products.isEmpty()) {
                return products;
            }

            String optionLabel = extractOptionLabel(doc, productJson);
            Integer price = extractPrice(doc, productJson);
            boolean inStock = extractInStock(doc, productJson);

            return List.of(new CrawledProduct(brandName, modelName, modelName, optionLabel, price, inStock, url, imageUrl, modelType));
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

        return extractRequiredText(doc, "div.brand a");
    }

    private String extractModelName(Document doc, JsonNode productJson) {
        if (productJson != null) {
            String modelName = productJson.path("name").asText();
            if (!modelName.isBlank()) {
                return normalizeText(modelName);
            }
        }

        return extractRequiredText(doc, "div.headingArea h2");
    }

    private String extractRequiredText(Document doc, String cssSelector) {
        Element element = doc.selectFirst(cssSelector);
        if (element == null || element.text().isBlank()) {
            throw new BusinessException(ErrorCode.CRAWLING_FAILED);
        }
        return normalizeText(element.text());
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
            String optionLabel = extractOfferOptionLabel(productJson, offer);
            Integer price = extractOfferPrice(offer, fallbackPrice);
            boolean inStock = extractOfferInStock(offer, doc);
            String offerUrl = offer.path("url").asText();

            products.add(new CrawledProduct(
                brandName,
                modelName,
                modelName,
                optionLabel,
                price,
                inStock,
                offerUrl.isBlank() ? url : offerUrl,
                extractOfferImageUrl(offer, imageUrl),
                modelType
            ));
        }
        return products;
    }

    private ModelType extractModelType(String url) {
        return categories.stream()
            .filter(category -> containsCategoryNo(url, category.categoryNo()))
            .map(CrawlerProperties.Category::modelType)
            .findFirst()
            .orElse(null);
    }

    private boolean containsCategoryNo(String url, int categoryNo) {
        return url.contains("/category/" + categoryNo + "/") || containsCategoryNoQueryParam(url, categoryNo);
    }

    private boolean containsCategoryNoQueryParam(String url, int categoryNo) {
        String token = "cate_no=" + categoryNo;
        int index = url.indexOf(token);
        while (index >= 0) {
            int nextIndex = index + token.length();
            if (nextIndex >= url.length() || !Character.isDigit(url.charAt(nextIndex))) {
                return true;
            }
            index = url.indexOf(token, index + 1);
        }
        return false;
    }

    private String extractImageUrl(JsonNode productJson) {
        if (productJson == null) {
            return null;
        }

        JsonNode image = productJson.path("image");
        if (image.isArray() && !image.isEmpty()) {
            return image.get(0).asText(null);
        }
        if (image.isTextual()) {
            return image.asText();
        }
        return null;
    }

    private String extractOfferImageUrl(JsonNode offer, String defaultImageUrl) {
        String offerImageUrl = offer.path("image").asText();
        return offerImageUrl.isBlank() ? defaultImageUrl : offerImageUrl;
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

    private boolean extractOfferInStock(JsonNode offer, Document doc) {
        String availability = offer.path("availability").asText();
        if (availability.isBlank()) {
            return !isSoldOutBySelectors(doc);
        }
        return availability.endsWith("InStock");
    }

    private String extractOptionLabel(Document doc, JsonNode productJson) {
        JsonNode firstOffer = extractFirstOffer(productJson);
        if (firstOffer != null) {
            String offerName = firstOffer.path("name").asText();
            String productName = productJson.path("name").asText();
            String optionLabel = offerName.replace(productName, "").trim();
            if (!optionLabel.isBlank()) {
                return normalizeText(optionLabel);
            }
        }

        Element option = doc.selectFirst("select[name^=option1] optgroup option:not([disabled])");
        if (option == null) {
            option = doc.select("select[name^=option1] option:not([disabled])")
                    .stream()
                    .filter(candidate -> !candidate.attr("value").equals("*"))
                    .filter(candidate -> !candidate.attr("value").equals("**"))
                    .filter(candidate -> !candidate.text().contains("필수"))
                    .filter(candidate -> !candidate.text().contains("---"))
                    .findFirst()
                    .orElse(null);
        }
        if (option == null || option.text().isBlank()) {
            return "기본";
        }
        return normalizeText(option.text());
    }

    private Integer extractPrice(Document doc, JsonNode productJson) {
        JsonNode firstOffer = extractFirstOffer(productJson);
        if (firstOffer != null && firstOffer.hasNonNull("price")) {
            return firstOffer.path("price").asInt();
        }

        return extractPagePrice(doc);
    }

    private Integer extractPagePrice(Document doc) {
        Element priceEl = doc.selectFirst("strong#span_product_price_text");
        if (priceEl == null || priceEl.text().isBlank()) {
            return null;
        }
        String digits = priceEl.text().replaceAll("[^0-9]", "");
        if (digits.isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(digits);
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.CRAWLING_FAILED);
        }
    }

    private String normalizeText(String text) {
        if (text == null) {
            return null;
        }
        String htmlText = text.replaceAll("(?i)&nbsp;", " ");
        return Parser.unescapeEntities(htmlText, false)
            .replace('\u00A0', ' ')
            .replaceAll("\\s+", " ")
            .trim();
    }

    private boolean extractInStock(Document doc, JsonNode productJson) {
        JsonNode firstOffer = extractFirstOffer(productJson);
        if (firstOffer != null) {
            String availability = firstOffer.path("availability").asText();
            if (!availability.isBlank()) {
                return availability.endsWith("InStock");
            }
        }

        return !isSoldOutBySelectors(doc);
    }

    private boolean isSoldOutBySelectors(Document doc) {
        return doc.selectFirst("div.soldOut, .btn_soldout:not(.displaynone), .sold:not(.displaynone)") != null;
    }

    private JsonNode extractFirstOffer(JsonNode productJson) {
        if (productJson == null) {
            return null;
        }

        JsonNode offers = productJson.path("offers");
        if (offers.isArray() && !offers.isEmpty()) {
            return offers.get(0);
        }
        if (offers.isObject()) {
            return offers;
        }
        return null;
    }
}
