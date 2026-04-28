package com.redline.jj.batch.crawler.semibasement;

import com.redline.jj.batch.crawler.ListParser;
import com.redline.jj.batch.crawler.semibasement.dto.SemiBasementProductListResponse;
import com.redline.jj.common.exception.BusinessException;
import com.redline.jj.common.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

@Component
public class SemiBasementListParser implements ListParser {

    private final WebClient webClient;

    public SemiBasementListParser(
        WebClient.Builder webClientBuilder,
        @Value("${crawler.semibasement.base-url}") String baseUrl
    ) {
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
    }

    @Override
    public List<String> parseProductUrls(int page) throws BusinessException {
        SemiBasementProductListResponse response;
        try {
            response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/api/v1/products")
                    .queryParam("page", page)
                    .queryParam("size", 50)
                    .build())
                .retrieve()
                .onStatus(HttpStatusCode::isError, clientResponse ->
                    Mono.error(new BusinessException(ErrorCode.CRAWLING_FAILED)))
                .bodyToMono(SemiBasementProductListResponse.class)
                .timeout(Duration.ofSeconds(10))
                .block();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.CRAWLING_FAILED);
        }

        if (response == null || response.data() == null || response.data().isEmpty()) {
            return Collections.emptyList();
        }

        return response.data().stream()
            .map(SemiBasementProductListResponse.SemiBasementProductItem::productUrl)
            .filter(u -> u != null && !u.trim().isBlank())
            .toList();
    }
}
