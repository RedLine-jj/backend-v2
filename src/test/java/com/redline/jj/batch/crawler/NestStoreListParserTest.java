package com.redline.jj.batch.crawler;

import com.redline.jj.batch.crawler.neststore.NestStoreListParser;
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

class NestStoreListParserTest {

    private MockWebServer server;
    private NestStoreListParser parser;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        parser = new NestStoreListParser(buildProperties());
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
                <ul id="prdList">
                    <li><a class="name" href="/product/pants/1/category/34/">Pants</a></li>
                </ul>
            </body></html>
            """));
        server.enqueue(new MockResponse().setBody("""
            <html><body>
                <a href="/product/jacket/2/category/230/" name="anchorBoxName_2">Jacket</a>
            </body></html>
            """));

        List<String> urls = parser.parseProductUrls(2);

        assertThat(urls).containsExactly(
            server.url("/product/pants/1/category/34/").toString(),
            server.url("/product/jacket/2/category/230/").toString()
        );
        assertThat(server.takeRequest().getPath()).isEqualTo("/product/list.html?cate_no=34&page=2");
        assertThat(server.takeRequest().getPath()).isEqualTo("/product/list.html?cate_no=230&page=2");
    }

    private CrawlerProperties buildProperties() {
        return new CrawlerProperties(
            new CrawlerProperties.ModeMan(null, List.of()),
            new CrawlerProperties.NestStore(
                server.url("/").toString(),
                List.of(
                    new CrawlerProperties.Category(34, ModelType.DENIM_PANTS),
                    new CrawlerProperties.Category(230, ModelType.DENIM_JACKET)
                )
            ),
            new CrawlerProperties.SemiBasement(null, List.of())
        );
    }
}
