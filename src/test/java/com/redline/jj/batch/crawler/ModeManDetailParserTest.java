package com.redline.jj.batch.crawler;

import com.redline.jj.batch.crawler.dto.CrawledProduct;
import com.redline.jj.batch.crawler.modeman.ModeManDetailParser;
import com.redline.jj.common.exception.BusinessException;
import com.redline.jj.common.exception.ErrorCode;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class ModeManDetailParserTest {

    private MockWebServer server;
    private ModeManDetailParser parser;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        parser = new ModeManDetailParser();
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
}
