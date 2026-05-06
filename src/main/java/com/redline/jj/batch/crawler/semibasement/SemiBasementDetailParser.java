package com.redline.jj.batch.crawler.semibasement;

import com.redline.jj.batch.crawler.DetailParser;
import com.redline.jj.batch.crawler.dto.CrawledProduct;
import com.redline.jj.batch.crawler.semibasement.dto.SemiBasementProductResponse;
import com.redline.jj.common.exception.BusinessException;
import com.redline.jj.common.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Component
public class SemiBasementDetailParser implements DetailParser {

    private final WebClient webClient;
    private final String baseUrl;

    public SemiBasementDetailParser(
        WebClient.Builder webClientBuilder,
        @Value("${crawler.semibasement.base-url}") String baseUrl
    ) {
        this.baseUrl = baseUrl;
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
    }

    @Override
    public CrawledProduct parse(String url) throws BusinessException {
        if (!url.startsWith(baseUrl)) {
            throw new BusinessException(ErrorCode.CRAWLING_FAILED);
        }
        String relativePath = url.substring(baseUrl.length());

        SemiBasementProductResponse response;
        try {
            response = webClient.get()
                .uri(uriBuilder -> uriBuilder.path(relativePath).build())
                .retrieve()
                .onStatus(HttpStatusCode::isError, clientResponse ->
                    Mono.error(new BusinessException(ErrorCode.CRAWLING_FAILED)))
                .bodyToMono(SemiBasementProductResponse.class)
                .timeout(Duration.ofSeconds(10))
                .block();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.CRAWLING_FAILED);
        }

        if (response == null) {
            throw new BusinessException(ErrorCode.CRAWLING_FAILED);
        }

        return new CrawledProduct(
            response.brandName(),
            response.productName(),
            response.productName(),
            response.optionName() != null ? response.optionName() : "기본",
            response.price(),
            response.stockCount() != null && response.stockCount() > 0,
            url,
            null,
            null
        );
    }
}
