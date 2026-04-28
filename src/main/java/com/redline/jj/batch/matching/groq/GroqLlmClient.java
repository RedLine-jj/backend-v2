package com.redline.jj.batch.matching.groq;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redline.jj.batch.crawler.dto.CrawledProduct;
import com.redline.jj.batch.matching.LlmMatchClient;
import com.redline.jj.batch.matching.LlmMatchResult;
import com.redline.jj.common.exception.BusinessException;
import com.redline.jj.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
public class GroqLlmClient implements LlmMatchClient {

    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String model;
    private final double confidenceThreshold;
    private final String promptTemplate;

    public GroqLlmClient(
            WebClient.Builder webClientBuilder,
            @Value("${groq.base-url:https://api.groq.com/openai/v1/chat/completions}") String baseUrl,
            @Value("${groq.api-key}") String apiKey,
            ObjectMapper objectMapper,
            @Value("${groq.model:llama3-8b-8192}") String model,
            @Value("${groq.confidence-threshold:85.0}") double confidenceThreshold,
            @Value("${groq.prompt}") String promptTemplate
    ) {
        this.webClient = webClientBuilder
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
        this.objectMapper = objectMapper;
        this.model = model;
        this.confidenceThreshold = confidenceThreshold;
        this.promptTemplate = promptTemplate;
    }

    @Override
    public Optional<LlmMatchResult> match(CrawledProduct product) {
        LlmMatchResult llmResult = callGroqApi(product.brandName(), product.modelName());

        if (llmResult.confidence() < confidenceThreshold) {
            log.debug("LLM confidence {:.1f} < {} — no match for product: {}",
                    llmResult.confidence(), confidenceThreshold, product.modelName());
            return Optional.empty();
        }

        return Optional.of(llmResult);
    }

    private LlmMatchResult callGroqApi(String brandName, String modelName) {
        String prompt = promptTemplate
                .replace("{brandHint}", brandName)
                .replace("{siteModelName}", modelName);

        GroqApiRequest request = new GroqApiRequest(
                model,
                List.of(new GroqMessage("user", prompt))
        );

        try {
            GroqApiResponse apiResponse = webClient.post()
                    .bodyValue(request)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError,
                            r -> Mono.error(new BusinessException(ErrorCode.LLM_MATCHING_FAILED)))
                    .bodyToMono(GroqApiResponse.class)
                    .timeout(TIMEOUT)
                    .block();

            if (apiResponse == null || apiResponse.choices() == null || apiResponse.choices().isEmpty()) {
                throw new BusinessException(ErrorCode.LLM_MATCHING_FAILED);
            }

            String content = apiResponse.choices().get(0).message().content();
            return objectMapper.readValue(content, LlmMatchResult.class);

        } catch (JsonProcessingException e) {
            log.warn("Groq 응답 JSON 파싱 실패 — brandName={}, modelName={}", brandName, modelName, e);
            throw new BusinessException(ErrorCode.LLM_MATCHING_FAILED);
        } catch (WebClientRequestException e) {
            log.warn("Groq API 요청 실패 — brandName={}, modelName={}", brandName, modelName, e);
            throw new BusinessException(ErrorCode.LLM_MATCHING_FAILED);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Groq API 호출 중 예외 발생 — brandName={}, modelName={}", brandName, modelName, e);
            throw new BusinessException(ErrorCode.LLM_MATCHING_FAILED);
        }
    }
}
