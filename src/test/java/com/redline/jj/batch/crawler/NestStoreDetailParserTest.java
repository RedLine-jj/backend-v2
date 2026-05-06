package com.redline.jj.batch.crawler;

import com.redline.jj.batch.crawler.dto.CrawledProduct;
import com.redline.jj.batch.crawler.neststore.NestStoreDetailParser;
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

class NestStoreDetailParserTest {

    private MockWebServer server;
    private NestStoreDetailParser parser;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        parser = new NestStoreDetailParser(buildProperties());
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    @DisplayName("URL 카테고리 설정으로 모델 타입을 반환한다")
    void parse_URL카테고리_모델타입반환() {
        server.enqueue(new MockResponse().setBody("""
            <html>
                <head>
                    <meta property="og:image" content="/image/product.jpg">
                </head>
                <body>
                    <div class="xans-product-detail">
                        <div class="brand"><span>LEVI'S</span></div>
                    </div>
                    <div class="headingArea"><h2>TYPE I DENIM JACKET</h2></div>
                    <select class="option_select"><option value="">선택</option><option value="M">M</option></select>
                    <strong class="price">129,000원</strong>
                </body>
            </html>
            """));

        CrawledProduct result = parser.parse(server.url("/product/detail.html?cate_no=12&product_no=1").toString());

        assertThat(result.modelType()).isEqualTo(ModelType.DENIM_JACKET);
        assertThat(result.imageUrl()).isEqualTo(server.url("/image/product.jpg").toString());
    }

    @Test
    @DisplayName("JSON-LD offers가 있으면 옵션별 CrawledProduct로 펼쳐 반환한다")
    void parseAll_JSONLDOffers_옵션별반환() {
        server.enqueue(new MockResponse().setBody("""
            <html>
                <head>
                    <script type="application/ld+json">
                    {
                        "@context": "https://schema.org",
                        "@type": "Product",
                        "name": "LOT DSB-1001XX DENIM PANTS",
                        "image": ["https://neststore.co.kr/web/product/big/product.jpg"],
                        "brand": {"@type": "Brand", "name": "WAREHOUSE"},
                        "offers": [
                            {
                                "name": "LOT DSB-1001XX DENIM PANTS 32",
                                "price": 529000,
                                "availability": "OutOfStock",
                                "url": "https://neststore.co.kr/product/detail.html?product_no=6858&item_code=P0000KDU000A",
                                "image": "https://neststore.co.kr/web/product/big/product.jpg"
                            },
                            {
                                "name": "LOT DSB-1001XX DENIM PANTS 33",
                                "price": 529000,
                                "availability": "InStock",
                                "url": "https://neststore.co.kr/product/detail.html?product_no=6858&item_code=P0000KDU000B",
                                "image": "https://neststore.co.kr/web/product/big/product.jpg"
                            }
                        ]
                    }
                    </script>
                </head>
                <body>
                    <div id="brand">WAREHOUSE</div>
                    <strong id="span_product_price_text">529,000원</strong>
                </body>
            </html>
            """));

        List<CrawledProduct> results = parser.parseAll(
            server.url("/product/lot-dsb/6858/category/34/display/1/").toString()
        );

        assertThat(results).extracting(CrawledProduct::brandName).containsOnly("WAREHOUSE");
        assertThat(results).extracting(CrawledProduct::modelName).containsOnly("LOT DSB-1001XX DENIM PANTS");
        assertThat(results).extracting(CrawledProduct::optionLabel).containsExactly("32", "33");
        assertThat(results).extracting(CrawledProduct::price).containsOnly(529000);
        assertThat(results).extracting(CrawledProduct::inStock).containsExactly(false, true);
        assertThat(results).extracting(CrawledProduct::modelType).containsOnly(ModelType.DENIM_PANTS);
    }

    @Test
    @DisplayName("상품명에 HTML 태그가 포함되어도 태그를 제거한다")
    void parseAll_상품명HTML태그_제거() {
        server.enqueue(new MockResponse().setBody("""
            <html>
                <head>
                    <script type="application/ld+json">
                    {
                        "@context": "https://schema.org",
                        "@type": "Product",
                        "name": "<b><font color=\\"red\\">(RESTOCK) </b></font><br> 0107SSW SUPER SMOOTH WIDE \\"MONROE\\" FOR WOMEN",
                        "image": ["https://neststore.co.kr/web/product/big/product.jpg"],
                        "brand": {"@type": "Brand", "name": "FULLCOUNT"},
                        "offers": [
                            {
                                "name": "<b><font color=\\"red\\">(RESTOCK) </b></font><br> 0107SSW SUPER SMOOTH WIDE \\"MONROE\\" FOR WOMEN 29",
                                "price": 368000,
                                "availability": "OutOfStock",
                                "url": "https://neststore.co.kr/product/detail.html?product_no=6366&item_code=P0000JKW000E"
                            }
                        ]
                    }
                    </script>
                </head>
                <body>
                    <div id="brand">FULLCOUNT</div>
                    <strong id="span_product_price_text">368,000원</strong>
                </body>
            </html>
            """));

        List<CrawledProduct> results = parser.parseAll(
            server.url("/product/restock-0107ssw/6366/category/34/display/1/").toString()
        );

        assertThat(results).hasSize(1);
        assertThat(results.get(0).modelName())
            .isEqualTo("0107SSW SUPER SMOOTH WIDE \"MONROE\" FOR WOMEN");
        assertThat(results.get(0).optionLabel()).isEqualTo("29");
        assertThat(results.get(0).inStock()).isFalse();
    }

    private CrawlerProperties buildProperties() {
        return new CrawlerProperties(
            new CrawlerProperties.ModeMan(null, List.of()),
            new CrawlerProperties.NestStore(
                server.url("/").toString(),
                List.of(
                    new CrawlerProperties.Category(12, ModelType.DENIM_JACKET),
                    new CrawlerProperties.Category(34, ModelType.DENIM_PANTS)
                )
            ),
            new CrawlerProperties.SemiBasement(null, List.of())
        );
    }
}
