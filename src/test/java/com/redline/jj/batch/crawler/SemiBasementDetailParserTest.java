package com.redline.jj.batch.crawler;

import com.redline.jj.batch.crawler.dto.CrawledProduct;
import com.redline.jj.batch.crawler.semibasement.SemiBasementDetailParser;
import com.redline.jj.common.exception.BusinessException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SemiBasementDetailParserTest {

    private MockWebServer server;
    private SemiBasementDetailParser parser;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        parser = new SemiBasementDetailParser(WebClient.builder(), server.url("/").toString());
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    @DisplayName("정상 JSON 응답이 오면 CrawledProduct로 변환해 반환한다")
    void parse_정상JSON응답_CrawledProduct반환() throws Exception {
        String json = """
            {
                "productCode": "P001",
                "productName": "551Z 셀비지",
                "brandName": "세미베이스먼트",
                "optionName": "32",
                "price": 398000,
                "stockCount": 5
            }
            """;
        server.enqueue(new MockResponse()
            .setBody(json)
            .addHeader("Content-Type", "application/json")
            .setResponseCode(200));

        String url = server.url("/api/v1/products/P001").toString();
        CrawledProduct result = parser.parse(url);

        assertThat(result.brandName()).isEqualTo("세미베이스먼트");
        assertThat(result.modelName()).isEqualTo("551Z 셀비지");
        assertThat(result.siteModelName()).isEqualTo("551Z 셀비지");
        assertThat(result.optionLabel()).isEqualTo("32");
        assertThat(result.inStock()).isTrue();
        assertThat(result.price()).isEqualTo(398000);
        assertThat(result.url()).isEqualTo(url);
    }

    @Test
    @DisplayName("5xx 응답이 오면 BusinessException을 던진다")
    void parse_5xx응답_BusinessException발생() {
        server.enqueue(new MockResponse().setResponseCode(500));
        String url = server.url("/api/v1/products/P001").toString();
        assertThrows(BusinessException.class, () -> parser.parse(url));
    }
}
