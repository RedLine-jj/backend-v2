package com.redline.jj.batch.crawler;

import com.redline.jj.batch.crawler.modeman.ModeManListParser;
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

class ModeManListParserTest {

    private MockWebServer server;
    private ModeManListParser parser;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        parser = new ModeManListParser(buildProperties());
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    @DisplayName("청바지와 청자켓 카테고리 상품 URL을 함께 반환한다")
    void parseProductUrls_청바지청자켓_URL반환() throws InterruptedException {
        server.enqueue(new MockResponse().setBody("""
            <html><body>
                <a href="/product/denim-pants/1/category/858/display/1/" name="anchorBoxName_1">Pants</a>
                <a href="/product/denim-pants/1/category/858/display/1/" name="anchorBoxName_1">Pants duplicate</a>
            </body></html>
            """));
        server.enqueue(new MockResponse().setBody("""
            <html><body>
                <a href="/product/denim-jacket/2/category/263/display/1/" name="anchorBoxName_2">Jacket</a>
            </body></html>
            """));

        List<String> urls = parser.parseProductUrls(3);

        assertThat(urls).containsExactly(
            server.url("/product/denim-pants/1/category/858/display/1/").toString(),
            server.url("/product/denim-jacket/2/category/263/display/1/").toString()
        );
        assertThat(server.takeRequest().getPath()).isEqualTo("/product/list.html?cate_no=858&page=3");
        assertThat(server.takeRequest().getPath()).isEqualTo("/product/list.html?cate_no=263&page=3");
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
