package com.redline.jj.batch;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BatchController.class)
@Import(BatchControllerTest.TestConfig.class)
class BatchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JobLauncher jobLauncher;

    @Autowired
    private Map<String, Job> crawlingJobMap;

    @TestConfiguration
    static class TestConfig {

        @Bean
        SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
            return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .build();
        }

        @Bean
        Clock clock() {
            return Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneId.of("Asia/Seoul"));
        }

        @Bean
        Map<String, Job> crawlingJobMap() {
            Job modeManJob = mock(Job.class);
            Job nestStoreJob = mock(Job.class);
            Job semiBasementJob = mock(Job.class);
            String suffix = BatchController.JOB_NAME_SUFFIX;
            return Map.of(
                "modeMan" + suffix, modeManJob,
                "nestStore" + suffix, nestStoreJob,
                "semiBasement" + suffix, semiBasementJob
            );
        }
    }

    // -----------------------------------------------------------------------
    // 기존 happy path
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("modeMan 사이트 크롤링 트리거 - 200 반환")
    void trigger_modeMan_200반환() throws Exception {
        given(jobLauncher.run(any(Job.class), any(JobParameters.class)))
            .willReturn(mock(JobExecution.class));

        mockMvc.perform(post("/api/batch/crawl/modeMan"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("nestStore 사이트 크롤링 트리거 - 200 반환")
    void trigger_nestStore_200반환() throws Exception {
        given(jobLauncher.run(any(Job.class), any(JobParameters.class)))
            .willReturn(mock(JobExecution.class));

        mockMvc.perform(post("/api/batch/crawl/nestStore"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("semiBasement 사이트 크롤링 트리거 - 200 반환")
    void trigger_semiBasement_200반환() throws Exception {
        given(jobLauncher.run(any(Job.class), any(JobParameters.class)))
            .willReturn(mock(JobExecution.class));

        mockMvc.perform(post("/api/batch/crawl/semiBasement"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("triggeredAt 파라미터가 고정 Clock 기준 시각(KST)으로 설정된다")
    void trigger_triggeredAt_고정Clock기준시각() throws Exception {
        given(jobLauncher.run(any(Job.class), any(JobParameters.class)))
            .willReturn(mock(JobExecution.class));

        mockMvc.perform(post("/api/batch/crawl/modeMan"))
            .andExpect(status().isOk());

        var captor = forClass(JobParameters.class);
        verify(jobLauncher).run(any(Job.class), captor.capture());

        LocalDateTime triggeredAt = captor.getValue().getLocalDateTime("triggeredAt");
        assertThat(triggeredAt).isEqualTo(LocalDateTime.of(2026, 1, 1, 9, 0, 0));
    }

    @Test
    @DisplayName("지원하지 않는 사이트 - 400 반환")
    void trigger_잘못된site_400반환() throws Exception {
        mockMvc.perform(post("/api/batch/crawl/invalidSite"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("C003"));
    }

    // -----------------------------------------------------------------------
    // 1. 알 수 없는 site 파라미터 — 추가 케이스
    // -----------------------------------------------------------------------

    @ParameterizedTest(name = "알 수 없는 사이트 [{0}] - 400 반환")
    @ValueSource(strings = {"unknown", "MODEMAN", "mode_man", "modeManJob"})
    @DisplayName("지원하지 않는 다양한 site 파라미터 - 400 반환")
    void trigger_다양한잘못된site_400반환(String invalidSite) throws Exception {
        mockMvc.perform(post("/api/batch/crawl/" + invalidSite))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("C003"));
    }

    @Test
    @DisplayName("알 수 없는 사이트 요청 시 jobLauncher.run()이 호출되지 않는다")
    void trigger_잘못된site_jobLauncher미호출() throws Exception {
        mockMvc.perform(post("/api/batch/crawl/unknownSite"))
            .andExpect(status().isBadRequest());

        verify(jobLauncher, never()).run(any(), any());
    }

    @Test
    @DisplayName("알 수 없는 사이트 400 응답에 에러 메시지가 포함된다")
    void trigger_잘못된site_에러메시지포함() throws Exception {
        mockMvc.perform(post("/api/batch/crawl/phantom"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.message").exists());
    }

    // -----------------------------------------------------------------------
    // 2. JobExecutionException 발생 시 500 반환
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("JobExecutionAlreadyRunningException 발생 시 500 반환, success=false, code=C004")
    void trigger_JobExecutionException_500반환() throws Exception {
        // JobLauncher.run()이 선언하는 checked exception 중 JobExecutionException 서브클래스 사용
        willThrow(new JobExecutionAlreadyRunningException("already running"))
            .given(jobLauncher).run(any(Job.class), any(JobParameters.class));

        mockMvc.perform(post("/api/batch/crawl/modeMan"))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("C004"));
    }

    @Test
    @DisplayName("JobExecutionAlreadyRunningException 발생 시 응답 body에 에러 메시지가 포함된다")
    void trigger_JobExecutionException_에러메시지포함() throws Exception {
        willThrow(new JobExecutionAlreadyRunningException("batch error"))
            .given(jobLauncher).run(any(Job.class), any(JobParameters.class));

        mockMvc.perform(post("/api/batch/crawl/nestStore"))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.message").exists());
    }

    // -----------------------------------------------------------------------
    // 3. JOB_NAME_SUFFIX 상수 기반 올바른 Job 선택 검증
    //    사이트명에 JOB_NAME_SUFFIX("CrawlingJob")를 붙인 키로 Job을 찾아야 한다.
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("modeMan 요청 시 modeManCrawlingJob이 jobLauncher에 전달된다")
    void trigger_modeMan_올바른Job선택() throws Exception {
        given(jobLauncher.run(any(Job.class), any(JobParameters.class)))
            .willReturn(mock(JobExecution.class));
        Job expectedJob = crawlingJobMap.get("modeMan" + BatchController.JOB_NAME_SUFFIX);

        mockMvc.perform(post("/api/batch/crawl/modeMan"))
            .andExpect(status().isOk());

        ArgumentCaptor<Job> jobCaptor = ArgumentCaptor.forClass(Job.class);
        verify(jobLauncher).run(jobCaptor.capture(), any(JobParameters.class));
        assertThat(jobCaptor.getValue()).isSameAs(expectedJob);
    }

    @Test
    @DisplayName("nestStore 요청 시 nestStoreCrawlingJob이 jobLauncher에 전달된다")
    void trigger_nestStore_올바른Job선택() throws Exception {
        given(jobLauncher.run(any(Job.class), any(JobParameters.class)))
            .willReturn(mock(JobExecution.class));
        Job expectedJob = crawlingJobMap.get("nestStore" + BatchController.JOB_NAME_SUFFIX);

        mockMvc.perform(post("/api/batch/crawl/nestStore"))
            .andExpect(status().isOk());

        ArgumentCaptor<Job> jobCaptor = ArgumentCaptor.forClass(Job.class);
        verify(jobLauncher).run(jobCaptor.capture(), any(JobParameters.class));
        assertThat(jobCaptor.getValue()).isSameAs(expectedJob);
    }

    @Test
    @DisplayName("semiBasement 요청 시 semiBasementCrawlingJob이 jobLauncher에 전달된다")
    void trigger_semiBasement_올바른Job선택() throws Exception {
        given(jobLauncher.run(any(Job.class), any(JobParameters.class)))
            .willReturn(mock(JobExecution.class));
        Job expectedJob = crawlingJobMap.get("semiBasement" + BatchController.JOB_NAME_SUFFIX);

        mockMvc.perform(post("/api/batch/crawl/semiBasement"))
            .andExpect(status().isOk());

        ArgumentCaptor<Job> jobCaptor = ArgumentCaptor.forClass(Job.class);
        verify(jobLauncher).run(jobCaptor.capture(), any(JobParameters.class));
        assertThat(jobCaptor.getValue()).isSameAs(expectedJob);
    }

    @Test
    @DisplayName("modeMan과 nestStore는 서로 다른 Job 인스턴스를 사용한다")
    void trigger_서로다른사이트_다른Job인스턴스() {
        // JOB_NAME_SUFFIX 상수가 일관되게 적용된 결과로, 사이트별 Job이 분리되어야 한다.
        String suffix = BatchController.JOB_NAME_SUFFIX;
        Job modeManJob = crawlingJobMap.get("modeMan" + suffix);
        Job nestStoreJob = crawlingJobMap.get("nestStore" + suffix);
        Job semiBasementJob = crawlingJobMap.get("semiBasement" + suffix);

        assertThat(modeManJob).isNotSameAs(nestStoreJob);
        assertThat(nestStoreJob).isNotSameAs(semiBasementJob);
        assertThat(modeManJob).isNotSameAs(semiBasementJob);
    }

    // -----------------------------------------------------------------------
    // 4. triggeredAt Job Parameter 추가 검증
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("triggeredAt 파라미터가 JobParameters에 'triggeredAt' 키로 존재한다")
    void trigger_JobParameters에_triggeredAt키존재() throws Exception {
        given(jobLauncher.run(any(Job.class), any(JobParameters.class)))
            .willReturn(mock(JobExecution.class));

        mockMvc.perform(post("/api/batch/crawl/semiBasement"))
            .andExpect(status().isOk());

        ArgumentCaptor<JobParameters> paramsCaptor = ArgumentCaptor.forClass(JobParameters.class);
        verify(jobLauncher).run(any(Job.class), paramsCaptor.capture());

        assertThat(paramsCaptor.getValue().getLocalDateTime("triggeredAt")).isNotNull();
    }

    @Test
    @DisplayName("성공 응답에 data 필드는 포함되지 않는다 (ApiResponse<Void>)")
    void trigger_성공응답_data필드없음() throws Exception {
        given(jobLauncher.run(any(Job.class), any(JobParameters.class)))
            .willReturn(mock(JobExecution.class));

        mockMvc.perform(post("/api/batch/crawl/modeMan"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").doesNotExist());
    }
}
