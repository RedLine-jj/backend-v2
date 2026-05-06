package com.redline.jj.batch.matching.groq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.redline.jj.batch.crawler.dto.CrawledProduct;
import com.redline.jj.batch.matching.LlmMatchResult;
import com.redline.jj.common.exception.BusinessException;
import com.redline.jj.common.exception.ErrorCode;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class GroqLlmClientTest {

    private MockWebServer groqServer;
    private GroqLlmClient groqLlmClient;

    @BeforeEach
    void setUp() throws IOException {
        groqServer = new MockWebServer();
        groqServer.start();
        groqLlmClient = new GroqLlmClient(
                WebClient.builder(),
                groqServer.url("/").toString(),
                "test-key",
                new ObjectMapper(),
                "llama3-8b-8192",
                85.0,
                "테스트 프롬프트 — 브랜드: {brandHint}, 상품명: {siteModelName}"
        );
    }

    @AfterEach
    void tearDown() throws IOException {
        groqServer.shutdown();
    }

    // -----------------------------------------------------------------------
    // 기존 happy path
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("confidence 85 이상이면 LlmMatchResult 반환")
    void match_Confidence85이상_LlmMatchResult반환() {
        groqServer.enqueue(new MockResponse()
                .setBody(buildGroqResponse("{\"brandName\":\"모드만\",\"modelName\":\"501 데님\",\"confidence\":92.0}"))
                .addHeader("Content-Type", "application/json")
                .setResponseCode(200));

        Optional<LlmMatchResult> result = groqLlmClient.match(buildProduct("모드만", "501 데님", "모드만501"));

        assertThat(result).isPresent();
        assertThat(result.get().brandName()).isEqualTo("모드만");
        assertThat(result.get().modelName()).isEqualTo("501 데님");
        assertThat(result.get().confidence()).isGreaterThanOrEqualTo(85.0);
    }

    @Test
    @DisplayName("confidence 85 미만이면 Optional.empty 반환")
    void match_Confidence85미만_OptionalEmpty반환() {
        groqServer.enqueue(new MockResponse()
                .setBody(buildGroqResponse("{\"brandName\":\"모드만\",\"modelName\":\"기존모델\",\"confidence\":60.0}"))
                .addHeader("Content-Type", "application/json")
                .setResponseCode(200));

        Optional<LlmMatchResult> result = groqLlmClient.match(buildProduct("모드만", "신규모델", "신규사이트명"));

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("LLM 5xx 응답 시 LLM_MATCHING_FAILED 예외 발생")
    void match_LLM5xx응답_LLM_MATCHING_FAILED예외() {
        groqServer.enqueue(new MockResponse().setResponseCode(500));

        assertThatThrownBy(() -> groqLlmClient.match(buildProduct("모드만", "테스트", "테스트")))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.LLM_MATCHING_FAILED));
    }

    @Test
    @DisplayName("LLM 응답 JSON 파싱 실패 시 LLM_MATCHING_FAILED 예외 발생")
    void match_JSON파싱실패_LLM_MATCHING_FAILED예외() {
        groqServer.enqueue(new MockResponse()
                .setBody(buildGroqResponse("유효하지않은JSON{{{"))
                .addHeader("Content-Type", "application/json")
                .setResponseCode(200));

        assertThatThrownBy(() -> groqLlmClient.match(buildProduct("모드만", "테스트", "테스트")))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.LLM_MATCHING_FAILED));
    }

    // -----------------------------------------------------------------------
    // 1. @JsonIgnoreProperties 동작 검증
    //    응답 JSON에 unknown 필드(usage, extra)가 포함돼도 정상 파싱되어야 한다.
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("unknown 필드(usage, extra) 포함 응답도 @JsonIgnoreProperties 덕분에 정상 파싱")
    void match_UnknownField포함응답_정상파싱() {
        String contentWithUnknown =
                "{\"brandName\":\"모드만\",\"modelName\":\"501 데님\",\"confidence\":90.0," +
                "\"usage\":{\"prompt_tokens\":100,\"completion_tokens\":50}," +
                "\"extra\":\"some_extra_field\"}";

        groqServer.enqueue(new MockResponse()
                .setBody(buildGroqResponse(contentWithUnknown))
                .addHeader("Content-Type", "application/json")
                .setResponseCode(200));

        Optional<LlmMatchResult> result = groqLlmClient.match(buildProduct("모드만", "501 데님", "모드만501"));

        assertThat(result).isPresent();
        assertThat(result.get().brandName()).isEqualTo("모드만");
        assertThat(result.get().modelName()).isEqualTo("501 데님");
        assertThat(result.get().confidence()).isEqualTo(90.0);
    }

    @Test
    @DisplayName("응답 최상위에 unknown 필드(id, created, system_fingerprint)가 포함돼도 GroqApiResponse 정상 파싱")
    void match_응답최상위UnknownField_정상파싱() {
        // GroqApiResponse 자체도 unknown 필드를 처리할 수 있어야 한다.
        // choices 배열만 사용하고 나머지 필드는 무시되어야 함.
        String fullResponse = "{\"id\":\"chatcmpl-xyz\",\"object\":\"chat.completion\"," +
                "\"created\":1700000000,\"model\":\"llama3-8b-8192\"," +
                "\"system_fingerprint\":\"fp_test\"," +
                "\"choices\":[{\"message\":{\"content\":" +
                "\"{\\\"brandName\\\":\\\"모드만\\\",\\\"modelName\\\":\\\"501 데님\\\",\\\"confidence\\\":90.0}\"}" +
                "}]," +
                "\"usage\":{\"prompt_tokens\":100,\"completion_tokens\":50,\"total_tokens\":150}}";

        groqServer.enqueue(new MockResponse()
                .setBody(fullResponse)
                .addHeader("Content-Type", "application/json")
                .setResponseCode(200));

        Optional<LlmMatchResult> result = groqLlmClient.match(buildProduct("모드만", "501 데님", "모드만501"));

        assertThat(result).isPresent();
        assertThat(result.get().confidence()).isEqualTo(90.0);
    }

    // -----------------------------------------------------------------------
    // 2. confidence 경계값 검증
    //    정확히 85.0: Optional.of(), 84.9: Optional.empty()
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("confidence 정확히 85.0이면 Optional.of 반환 (경계값 포함)")
    void match_Confidence정확히85_OptionalOf반환() {
        groqServer.enqueue(new MockResponse()
                .setBody(buildGroqResponse("{\"brandName\":\"모드만\",\"modelName\":\"501\",\"confidence\":85.0}"))
                .addHeader("Content-Type", "application/json")
                .setResponseCode(200));

        Optional<LlmMatchResult> result = groqLlmClient.match(buildProduct("모드만", "501", "모드만501"));

        assertThat(result).isPresent();
        assertThat(result.get().confidence()).isEqualTo(85.0);
    }

    @Test
    @DisplayName("confidence 84.9이면 Optional.empty 반환 (경계값 미달)")
    void match_Confidence84점9_OptionalEmpty반환() {
        groqServer.enqueue(new MockResponse()
                .setBody(buildGroqResponse("{\"brandName\":\"모드만\",\"modelName\":\"501\",\"confidence\":84.9}"))
                .addHeader("Content-Type", "application/json")
                .setResponseCode(200));

        Optional<LlmMatchResult> result = groqLlmClient.match(buildProduct("모드만", "501", "모드만501"));

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("confidence 0.0이면 Optional.empty 반환")
    void match_Confidence0_OptionalEmpty반환() {
        groqServer.enqueue(new MockResponse()
                .setBody(buildGroqResponse("{\"brandName\":\"모드만\",\"modelName\":\"501\",\"confidence\":0.0}"))
                .addHeader("Content-Type", "application/json")
                .setResponseCode(200));

        Optional<LlmMatchResult> result = groqLlmClient.match(buildProduct("모드만", "501", "모드만501"));

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("confidence 100.0이면 Optional.of 반환")
    void match_Confidence100_OptionalOf반환() {
        groqServer.enqueue(new MockResponse()
                .setBody(buildGroqResponse("{\"brandName\":\"모드만\",\"modelName\":\"501\",\"confidence\":100.0}"))
                .addHeader("Content-Type", "application/json")
                .setResponseCode(200));

        Optional<LlmMatchResult> result = groqLlmClient.match(buildProduct("모드만", "501", "모드만501"));

        assertThat(result).isPresent();
        assertThat(result.get().confidence()).isEqualTo(100.0);
    }

    // -----------------------------------------------------------------------
    // 3. 빈 choices 배열 응답 → BusinessException
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("빈 choices 배열 응답 시 LLM_MATCHING_FAILED 예외 발생")
    void match_빈choices배열_LLM_MATCHING_FAILED예외() {
        String emptyChoicesResponse = "{\"choices\":[]}";

        groqServer.enqueue(new MockResponse()
                .setBody(emptyChoicesResponse)
                .addHeader("Content-Type", "application/json")
                .setResponseCode(200));

        assertThatThrownBy(() -> groqLlmClient.match(buildProduct("모드만", "501", "모드만501")))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.LLM_MATCHING_FAILED));
    }

    @Test
    @DisplayName("choices 필드 null 응답 시 LLM_MATCHING_FAILED 예외 발생")
    void match_choicesNull_LLM_MATCHING_FAILED예외() {
        String nullChoicesResponse = "{\"choices\":null}";

        groqServer.enqueue(new MockResponse()
                .setBody(nullChoicesResponse)
                .addHeader("Content-Type", "application/json")
                .setResponseCode(200));

        assertThatThrownBy(() -> groqLlmClient.match(buildProduct("모드만", "501", "모드만501")))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.LLM_MATCHING_FAILED));
    }

    // -----------------------------------------------------------------------
    // 4. 프롬프트 플레이스홀더 치환 검증
    //    MockWebServer가 받은 요청 body에 실제 값이 들어가 있는지 확인
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("요청 body의 프롬프트에 brandHint, siteModelName이 치환되어야 한다")
    void match_프롬프트플레이스홀더_치환검증() throws InterruptedException {
        groqServer.enqueue(new MockResponse()
                .setBody(buildGroqResponse("{\"brandName\":\"모드만\",\"modelName\":\"501\",\"confidence\":92.0}"))
                .addHeader("Content-Type", "application/json")
                .setResponseCode(200));

        groqLlmClient.match(buildProduct("모드만", "501", "모드만501"));

        RecordedRequest request = groqServer.takeRequest();
        String requestBody = request.getBody().readUtf8();

        // {brandHint}는 brandName(=모드만)으로, {siteModelName}은 modelName(=501)으로 치환
        assertThat(requestBody).contains("모드만");
        assertThat(requestBody).contains("501");
        // 플레이스홀더가 남아있으면 안 됨
        assertThat(requestBody).doesNotContain("{brandHint}");
        assertThat(requestBody).doesNotContain("{siteModelName}");
    }

    @Test
    @DisplayName("요청 헤더에 Authorization: Bearer test-key 가 포함되어야 한다")
    void match_요청헤더Authorization_포함검증() throws InterruptedException {
        groqServer.enqueue(new MockResponse()
                .setBody(buildGroqResponse("{\"brandName\":\"모드만\",\"modelName\":\"501\",\"confidence\":92.0}"))
                .addHeader("Content-Type", "application/json")
                .setResponseCode(200));

        groqLlmClient.match(buildProduct("모드만", "501", "모드만501"));

        RecordedRequest request = groqServer.takeRequest();
        assertThat(request.getHeader("Authorization")).isEqualTo("Bearer test-key");
    }

    @Test
    @DisplayName("요청 body에 설정된 모델명이 포함되어야 한다")
    void match_요청body에_모델명포함() throws InterruptedException {
        groqServer.enqueue(new MockResponse()
                .setBody(buildGroqResponse("{\"brandName\":\"모드만\",\"modelName\":\"501\",\"confidence\":92.0}"))
                .addHeader("Content-Type", "application/json")
                .setResponseCode(200));

        groqLlmClient.match(buildProduct("모드만", "501", "모드만501"));

        RecordedRequest request = groqServer.takeRequest();
        String requestBody = request.getBody().readUtf8();

        assertThat(requestBody).contains("llama3-8b-8192");
    }

    // -----------------------------------------------------------------------
    // 5. 4xx 응답
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("LLM 401 응답 시 LLM_MATCHING_FAILED 예외 발생")
    void match_LLM401응답_LLM_MATCHING_FAILED예외() {
        groqServer.enqueue(new MockResponse().setResponseCode(401));

        assertThatThrownBy(() -> groqLlmClient.match(buildProduct("모드만", "테스트", "테스트")))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.LLM_MATCHING_FAILED));
    }

    @Test
    @DisplayName("LLM 429 Too Many Requests 응답 시 LLM_MATCHING_FAILED 예외 발생")
    void match_LLM429응답_LLM_MATCHING_FAILED예외() {
        groqServer.enqueue(new MockResponse().setResponseCode(429));

        assertThatThrownBy(() -> groqLlmClient.match(buildProduct("모드만", "테스트", "테스트")))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.LLM_MATCHING_FAILED));
    }

    // -----------------------------------------------------------------------
    // helpers
    // -----------------------------------------------------------------------

    private CrawledProduct buildProduct(String brandName, String modelName, String siteModelName) {
        return new CrawledProduct(brandName, modelName, siteModelName, null, null, false, null, null, null);
    }

    private String buildGroqResponse(String content) {
        String escaped = content.replace("\\", "\\\\").replace("\"", "\\\"");
        return "{\"choices\":[{\"message\":{\"content\":\"" + escaped + "\"}}]}";
    }
}
