package com.redline.jj.batch.crawler;

import com.redline.jj.batch.crawler.dto.CrawledProduct;
import com.redline.jj.batch.crawler.modeman.ModeManDetailParser;
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

class ModeManDetailParserTest {

    private MockWebServer server;
    private ModeManDetailParser parser;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        parser = new ModeManDetailParser(buildProperties());
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    @DisplayName("정상 HTML 파싱 시 CrawledProduct를 반환한다")
    void parse_정상HTML_CrawledProduct반환() throws IOException {
        String html = "<html><body>"
                + "<div class=\"brand\"><a>모드만</a></div>"
                + "<div class=\"headingArea\"><h2>101 슬림 데님</h2></div>"
                + "<strong id=\"span_product_price_text\">89,000원</strong>"
                + "<select name=\"option1\"><option>S</option><option>M</option></select>"
                + "</body></html>";

        server.enqueue(new MockResponse().setBody(html).setResponseCode(200));
        String url = server.url("/product/detail.html?no=1").toString();

        CrawledProduct result = parser.parse(url);

        assertThat(result.brandName()).isEqualTo("모드만");
        assertThat(result.modelName()).isEqualTo("101 슬림 데님");
        assertThat(result.siteModelName()).isEqualTo("101 슬림 데님");
        assertThat(result.price()).isEqualTo(89000);
        assertThat(result.inStock()).isTrue();
        assertThat(result.optionLabel()).isEqualTo("S");
        assertThat(result.url()).isEqualTo(url);
    }

    @Test
    @DisplayName("ModeMan 현재 상세 HTML의 JSON-LD 상품 정보를 파싱한다")
    void parse_JSONLD상품정보_CrawledProduct반환() {
        String html = """
            <html><body>
                <script type="application/ld+json">
                {
                    "@context": "https://schema.org",
                    "@type": "Product",
                    "name": "00-L105-81 60s Selvedge Denim Jeans Zipper Fly One Wash",
                    "image": [
                        "https://mode-man.com/web/product/big/sample.jpg"
                    ],
                    "brand": {
                        "@type": "Brand",
                        "name": "LEVI&#039;S"
                    },
                    "offers": [
                        {
                            "name": "00-L105-81 60s Selvedge Denim Jeans Zipper Fly One Wash 2",
                            "price": 378000,
                            "priceCurrency": "KRW",
                            "availability": "InStock"
                        }
                    ]
                }
                </script>
                <strong id="span_product_price_text">w378,000</strong>
                <select name="option1">
                    <option value="*" selected>- [필수] 옵션을 선택해 주세요 -</option>
                    <option value="**" disabled>-------------------</option>
                    <optgroup label="Size">
                        <option value="P0000UYU000C">2</option>
                    </optgroup>
                </select>
            </body></html>
            """;

        server.enqueue(new MockResponse().setBody(html).setResponseCode(200));
        String url = server.url("/product/sample/14164/category/858/display/1/").toString();

        CrawledProduct result = parser.parse(url);

        assertThat(result.brandName()).isEqualTo("LEVI'S");
        assertThat(result.modelName()).isEqualTo("00-L105-81 60s Selvedge Denim Jeans Zipper Fly One Wash");
        assertThat(result.siteModelName()).isEqualTo("00-L105-81 60s Selvedge Denim Jeans Zipper Fly One Wash");
        assertThat(result.price()).isEqualTo(378000);
        assertThat(result.inStock()).isTrue();
        assertThat(result.optionLabel()).isEqualTo("2");
        assertThat(result.imageUrl()).isEqualTo("https://mode-man.com/web/product/big/sample.jpg");
        assertThat(result.modelType()).isEqualTo(ModelType.DENIM_PANTS);
        assertThat(result.url()).isEqualTo(url);
    }

    @Test
    @DisplayName("ModeMan JSON-LD offers를 옵션별 상품으로 모두 파싱한다")
    void parseAll_JSONLDOffers_옵션별CrawledProduct반환() {
        String html = """
            <html><body>
                <script type="application/ld+json">
                {
                    "@context": "https://schema.org",
                    "@type": "Product",
                    "name": "00-L105-81 60s Selvedge Denim Jeans Zipper Fly One Wash",
                    "image": "https://mode-man.com/web/product/big/default.jpg",
                    "brand": {
                        "@type": "Brand",
                        "name": "LEVI&#039;S"
                    },
                    "offers": [
                        {
                            "name": "00-L105-81 60s Selvedge Denim Jeans Zipper Fly One Wash 2",
                            "price": 378000,
                            "availability": "InStock",
                            "url": "https://mode-man.com/product/detail.html?product_no=14164&item_code=A",
                            "image": "https://mode-man.com/web/product/big/option-a.jpg"
                        },
                        {
                            "name": "00-L105-81 60s Selvedge Denim Jeans Zipper Fly One Wash 3",
                            "availability": "OutOfStock",
                            "url": "https://mode-man.com/product/detail.html?product_no=14164&item_code=B"
                        },
                        {
                            "name": "00-L105-81 60s Selvedge Denim Jeans Zipper Fly One Wash 4",
                            "price": 378000,
                            "availability": "InStock",
                            "url": "https://mode-man.com/product/detail.html?product_no=14164&item_code=C"
                        }
                    ]
                }
                </script>
                <strong id="span_product_price_text">w378,000</strong>
            </body></html>
            """;

        server.enqueue(new MockResponse().setBody(html).setResponseCode(200));
        String url = server.url("/product/sample/14164/category/263/display/1/").toString();

        List<CrawledProduct> results = parser.parseAll(url);

        assertThat(results).hasSize(3);
        assertThat(results).extracting(CrawledProduct::brandName)
            .containsOnly("LEVI'S");
        assertThat(results).extracting(CrawledProduct::optionLabel)
            .containsExactly("2", "3", "4");
        assertThat(results).extracting(CrawledProduct::inStock)
            .containsExactly(true, false, true);
        assertThat(results).extracting(CrawledProduct::price)
            .containsExactly(378000, 378000, 378000);
        assertThat(results).extracting(CrawledProduct::imageUrl)
            .containsExactly(
                "https://mode-man.com/web/product/big/option-a.jpg",
                "https://mode-man.com/web/product/big/default.jpg",
                "https://mode-man.com/web/product/big/default.jpg"
            );
        assertThat(results).extracting(CrawledProduct::modelType)
            .containsOnly(ModelType.DENIM_JACKET);
        assertThat(results).extracting(CrawledProduct::url)
            .containsExactly(
                "https://mode-man.com/product/detail.html?product_no=14164&item_code=A",
                "https://mode-man.com/product/detail.html?product_no=14164&item_code=B",
                "https://mode-man.com/product/detail.html?product_no=14164&item_code=C"
            );
    }

    @Test
    @DisplayName("soldOut div가 있는 HTML 파싱 시 inStock이 false이다")
    void parse_품절HTML_inStockFalse() throws IOException {
        String html = "<html><body>"
                + "<div class=\"brand\"><a>모드만</a></div>"
                + "<div class=\"headingArea\"><h2>101 슬림 데님</h2></div>"
                + "<strong id=\"span_product_price_text\">89,000원</strong>"
                + "<select name=\"option1\"><option>S</option></select>"
                + "<div class=\"soldOut\">품절</div>"
                + "</body></html>";

        server.enqueue(new MockResponse().setBody(html).setResponseCode(200));
        String url = server.url("/product/detail.html?no=2").toString();

        CrawledProduct result = parser.parse(url);

        assertThat(result.inStock()).isFalse();
    }

    @Test
    @DisplayName("brandName 요소가 없는 HTML 파싱 시 BusinessException이 발생한다")
    void parse_필수요소누락HTML_BusinessException발생() {
        String html = "<html><body>"
                + "<div class=\"headingArea\"><h2>101 슬림 데님</h2></div>"
                + "<strong id=\"span_product_price_text\">89,000원</strong>"
                + "</body></html>";

        server.enqueue(new MockResponse().setBody(html).setResponseCode(200));
        String url = server.url("/product/detail.html?no=3").toString();

        assertThatThrownBy(() -> parser.parse(url))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.CRAWLING_FAILED));
    }

    private CrawlerProperties buildProperties() {
        return new CrawlerProperties(
            new CrawlerProperties.ModeMan(
                server.url("/").toString(),
                List.of(
                    new CrawlerProperties.Category(858, ModelType.DENIM_PANTS),
                    new CrawlerProperties.Category(263, ModelType.DENIM_JACKET)
                )
            )
        );
    }
}
