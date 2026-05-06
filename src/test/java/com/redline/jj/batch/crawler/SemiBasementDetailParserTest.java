package com.redline.jj.batch.crawler;

import com.redline.jj.batch.crawler.dto.CrawledProduct;
import com.redline.jj.batch.crawler.semibasement.SemiBasementDetailParser;
import com.redline.jj.common.exception.BusinessException;
import com.redline.jj.common.exception.ErrorCode;
import com.redline.jj.config.CrawlerProperties;
import com.redline.jj.domain.model.Model.ModelType;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SemiBasementDetailParserTest {

    private MockWebServer server;
    private SemiBasementDetailParser parser;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        parser = new SemiBasementDetailParser(buildProperties());
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    @DisplayName("정상 HTML 응답이 오면 CrawledProduct로 변환해 반환한다")
    void parse_정상HTML응답_CrawledProduct반환() {
        String html = """
            <html>
                <head>
                    <meta property="og:title" content="Lot.1604 Waist Overall Dirt Denim : Semi Basement General Store">
                    <meta property="product:brand" content="Trophy Clothing">
                    <meta property="og:image" content="/images/product.jpg">
                </head>
                <body>
                    <div class="price">434,000원</div>
                    <ul>
                        <li>Size：30, 32, 34</li>
                    </ul>
                </body>
            </html>
            """;
        server.enqueue(new MockResponse()
            .setBody(html)
            .addHeader("Content-Type", "text/html")
            .setResponseCode(200));

        String url = server.url("/89/?idx=4340").toString();
        CrawledProduct result = parser.parse(url);

        assertThat(result.brandName()).isEqualTo("TROPHY CLOTHING");
        assertThat(result.modelName()).isEqualTo("Lot.1604 Waist Overall Dirt Denim");
        assertThat(result.siteModelName()).isEqualTo("Lot.1604 Waist Overall Dirt Denim");
        assertThat(result.optionLabel()).isEqualTo("30");
        assertThat(result.inStock()).isTrue();
        assertThat(result.price()).isEqualTo(434000);
        assertThat(result.url()).isEqualTo(url);
        assertThat(result.imageUrl()).isEqualTo(server.url("/images/product.jpg").toString());
        assertThat(result.modelType()).isEqualTo(ModelType.DENIM_PANTS);
    }

    @Test
    @DisplayName("Size 항목이 있으면 옵션별 CrawledProduct로 펼쳐 반환한다")
    void parseAll_Size항목_옵션별반환() {
        String html = """
            <html>
                <head>
                    <meta property="og:title" content="[ JP94313 ] 55 Denim 313XX : Semi Basement General Store">
                    <meta property="product:brand" content="Jelado">
                    <meta property="og:image" content="/images/product.jpg">
                </head>
                <body>
                    <div class="price">398,000원</div>
                    <ul>
                        <li>Size：28, 29, 30, 31, 32, 33, 34, 36, 38</li>
                        <li>Color：Indigo</li>
                    </ul>
                </body>
            </html>
            """;
        server.enqueue(new MockResponse()
            .setBody(html)
            .addHeader("Content-Type", "text/html")
            .setResponseCode(200));

        List<CrawledProduct> results = parser.parseAll(server.url("/89/?idx=388").toString());

        assertThat(results).extracting(CrawledProduct::optionLabel)
            .containsExactly("28", "29", "30", "31", "32", "33", "34", "36", "38");
        assertThat(results).extracting(CrawledProduct::price)
            .containsOnly(398000);
        assertThat(results).extracting(CrawledProduct::modelType)
            .containsOnly(ModelType.DENIM_PANTS);
    }

    @Test
    @DisplayName("JSON-LD brand가 있으면 Semi Basement 기본값 대신 해당 브랜드를 사용한다")
    void parseAll_JSONLDBrand_브랜드반환() {
        String html = """
            <html>
                <head>
                    <meta property="og:title" content="[ JP94407S ] S407XX 1942 WPB L-181 War Model / S-M사이즈 : Semi Basement General Store">
                    <script type="application/ld+json">
                    {
                        "@context": "https://schema.org",
                        "@type": "Product",
                        "name": "[ JP94407S ] S407XX 1942 WPB L-181 War Model / S-M사이즈",
                        "brand": {"@type": "Brand", "name": "Jelado"}
                    }
                    </script>
                </head>
                <body>
                    <div class="price">728,000원</div>
                    <ul>
                        <li>Size：36(S), 38(M)</li>
                    </ul>
                </body>
            </html>
            """;
        server.enqueue(new MockResponse()
            .setBody(html)
            .addHeader("Content-Type", "text/html")
            .setResponseCode(200));

        List<CrawledProduct> results = parser.parseAll(server.url("/93/?idx=5310").toString());

        assertThat(results).extracting(CrawledProduct::brandName)
            .containsOnly("JELADO");
        assertThat(results).extracting(CrawledProduct::modelType)
            .containsOnly(ModelType.DENIM_JACKET);
    }

    @Test
    @DisplayName("슬래시가 포함된 Size는 하나의 옵션으로 유지한다")
    void parseAll_슬래시Size_하나의옵션으로유지() {
        String html = """
            <html>
                <head>
                    <meta property="og:title" content="Denim Jacket : Semi Basement General Store">
                    <meta property="product:brand" content="Semi Basement">
                </head>
                <body>
                    <div class="price">298,000원</div>
                    <ul>
                        <li>Size：36/S, 38/M, 40/L</li>
                    </ul>
                </body>
            </html>
            """;
        server.enqueue(new MockResponse()
            .setBody(html)
            .addHeader("Content-Type", "text/html")
            .setResponseCode(200));

        List<CrawledProduct> results = parser.parseAll(server.url("/89/?idx=500").toString());

        assertThat(results).extracting(CrawledProduct::optionLabel)
            .containsExactly("36/S", "38/M", "40/L");
    }

    @Test
    @DisplayName("5xx 응답이 오면 BusinessException을 던진다")
    void parse_5xx응답_BusinessException발생() {
        server.enqueue(new MockResponse().setResponseCode(500));
        String url = server.url("/api/v1/products/P001").toString();
        assertThatThrownBy(() -> parser.parse(url))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.CRAWLING_FAILED));
    }

    private CrawlerProperties buildProperties() {
        return new CrawlerProperties(
            new CrawlerProperties.ModeMan(null, List.of()),
            new CrawlerProperties.NestStore(null, List.of()),
            new CrawlerProperties.SemiBasement(
                server.url("/").toString(),
                List.of(
                    new CrawlerProperties.Category(89, ModelType.DENIM_PANTS),
                    new CrawlerProperties.Category(93, ModelType.DENIM_JACKET)
                )
            )
        );
    }
}
