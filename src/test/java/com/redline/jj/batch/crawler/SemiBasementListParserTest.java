package com.redline.jj.batch.crawler;

import com.redline.jj.batch.crawler.semibasement.SemiBasementListParser;
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

class SemiBasementListParserTest {

    private MockWebServer server;
    private SemiBasementListParser parser;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        parser = new SemiBasementListParser(buildProperties());
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
                <a href="/89/?idx=4340">Pants image</a>
                <a href="/89/?idx=4340">Pants text</a>
            </body></html>
            """));
        server.enqueue(new MockResponse().setBody("""
            <html><body>
                <a href="/93/?idx=1234">Jacket</a>
            </body></html>
            """));

        List<String> urls = parser.parseProductUrls(1);

        assertThat(urls).containsExactly(
            server.url("/89/?idx=4340").toString(),
            server.url("/93/?idx=1234").toString()
        );
        assertThat(server.takeRequest().getPath()).isEqualTo("/89");
        assertThat(server.takeRequest().getPath()).isEqualTo("/93");
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
