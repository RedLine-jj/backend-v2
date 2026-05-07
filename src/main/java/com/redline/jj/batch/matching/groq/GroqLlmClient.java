package com.redline.jj.batch.matching.groq;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redline.jj.batch.crawler.dto.CrawledProduct;
import com.redline.jj.batch.matching.LlmMatchClient;
import com.redline.jj.batch.matching.LlmMatchResult;
import com.redline.jj.common.exception.BusinessException;
import com.redline.jj.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.function.LongConsumer;

@Slf4j
@Component
public class GroqLlmClient implements LlmMatchClient {

    private static final Duration TIMEOUT = Duration.ofSeconds(10);
    private static final int MAX_RATE_LIMIT_RETRIES = 3;
    private static final long MAX_RETRY_AFTER_SECONDS = 60L;
    static final long[] DEFAULT_BACKOFF_SECONDS = {2L, 5L, 10L};

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String model;
    private final double confidenceThreshold;
    private final String promptTemplate;
    private final long[] backoffSeconds;
    private final LongConsumer sleeper;

    @Autowired
    public GroqLlmClient(
            WebClient.Builder webClientBuilder,
            @Value("${groq.base-url:https://api.groq.com/openai/v1/chat/completions}") String baseUrl,
            @Value("${groq.api-key}") String apiKey,
            ObjectMapper objectMapper,
            @Value("${groq.model:llama-3.1-8b-instant}") String model,
            @Value("${groq.confidence-threshold:85.0}") double confidenceThreshold,
            @Value("${groq.prompt}") String promptTemplate
    ) {
        this(webClientBuilder, baseUrl, apiKey, objectMapper, model,
                confidenceThreshold, promptTemplate, DEFAULT_BACKOFF_SECONDS,
                seconds -> {
                    try { Thread.sleep(seconds * 1_000); }
                    catch (InterruptedException ex) { Thread.currentThread().interrupt(); }
                });
    }

    GroqLlmClient(
            WebClient.Builder webClientBuilder,
            String baseUrl,
            String apiKey,
            ObjectMapper objectMapper,
            String model,
            double confidenceThreshold,
            String promptTemplate,
            long[] backoffSeconds,
            LongConsumer sleeper
    ) {
        this.webClient = webClientBuilder
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
        this.objectMapper = objectMapper;
        this.model = model;
        this.confidenceThreshold = confidenceThreshold;
        this.promptTemplate = promptTemplate;
        this.backoffSeconds = backoffSeconds;
        this.sleeper = sleeper;
    }

    @Override
    public Optional<LlmMatchResult> match(CrawledProduct product) {
        LlmMatchResult llmResult = callGroqApiWithRetry(product.brandName(), product.siteModelName());

        if (llmResult.confidence() < confidenceThreshold) {
            log.debug("LLM confidence {:.1f} < {} — no match for product: {}",
                    llmResult.confidence(), confidenceThreshold, product.modelName());
            return Optional.empty();
        }

        return Optional.of(llmResult);
    }

    private LlmMatchResult callGroqApiWithRetry(String brandName, String modelName) {
        for (int attempt = 0; attempt <= MAX_RATE_LIMIT_RETRIES; attempt++) {
            try {
                return executeGroqRequest(brandName, modelName);
            } catch (GroqRateLimitException e) {
                if (attempt == MAX_RATE_LIMIT_RETRIES) {
                    log.warn("Groq 429 최대 재시도 초과 — brandName={}, modelName={}", brandName, modelName);
                    throw new BusinessException(ErrorCode.LLM_RATE_LIMITED);
                }
                long waitSeconds = (e.retryAfterSeconds >= 0)
                        ? Math.min(e.retryAfterSeconds, MAX_RETRY_AFTER_SECONDS)
                        : backoffSeconds[attempt];
                log.warn("Groq 429 rate limit — {}회 재시도 ({}s 대기)", attempt + 1, waitSeconds);
                sleepSeconds(waitSeconds);
            }
        }
        // 루프 내 attempt == MAX_RATE_LIMIT_RETRIES 분기에서 항상 throw하므로 도달 불가
        throw new AssertionError("unreachable");
    }

    private LlmMatchResult executeGroqRequest(String brandName, String modelName) {
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
                    .onStatus(status -> status.value() == 429, clientResponse -> {
                        String retryAfterHeader = clientResponse.headers().asHttpHeaders().getFirst("Retry-After");
                        long seconds = -1;
                        if (retryAfterHeader != null) {
                            try { seconds = Long.parseLong(retryAfterHeader.trim()); }
                            catch (NumberFormatException ignored) {}
                        }
                        return Mono.error(new GroqRateLimitException(seconds));
                    })
                    .onStatus(HttpStatusCode::isError,
                            r -> Mono.error(new BusinessException(ErrorCode.LLM_MATCHING_FAILED)))
                    .bodyToMono(GroqApiResponse.class)
                    .timeout(TIMEOUT)
                    .block();

            if (apiResponse == null || apiResponse.choices() == null || apiResponse.choices().isEmpty()) {
                throw new BusinessException(ErrorCode.LLM_MATCHING_FAILED);
            }

            String content = apiResponse.choices().get(0).message().content();
            return objectMapper.readValue(extractJsonObject(content), LlmMatchResult.class);

        } catch (GroqRateLimitException e) {
            throw e;
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

    private void sleepSeconds(long seconds) {
        sleeper.accept(seconds);
    }

    private String extractJsonObject(String content) {
        if (content == null || content.isBlank()) {
            throw new BusinessException(ErrorCode.LLM_MATCHING_FAILED);
        }

        int startIndex = content.indexOf('{');
        if (startIndex < 0) {
            throw new BusinessException(ErrorCode.LLM_MATCHING_FAILED);
        }

        int depth = 0;
        boolean inString = false;
        boolean escaped = false;
        for (int index = startIndex; index < content.length(); index++) {
            char current = content.charAt(index);
            if (escaped) {
                escaped = false;
                continue;
            }
            if (current == '\\') {
                escaped = inString;
                continue;
            }
            if (current == '"') {
                inString = !inString;
                continue;
            }
            if (inString) {
                continue;
            }
            if (current == '{') {
                depth++;
            } else if (current == '}') {
                depth--;
                if (depth == 0) {
                    return content.substring(startIndex, index + 1);
                }
            }
        }

        throw new BusinessException(ErrorCode.LLM_MATCHING_FAILED);
    }
}
