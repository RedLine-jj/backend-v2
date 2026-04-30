package com.redline.jj.batch;

import com.redline.jj.domain.subscription.SubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.scheduling.TaskScheduler;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BatchSchedulerTest {

    @Mock
    private TaskScheduler taskScheduler;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private JobLauncher jobLauncher;

    @Mock
    private Job modeManCrawlingJob;

    @Mock
    private Job nestStoreCrawlingJob;

    @Mock
    private Job semiBasementCrawlingJob;

    private final Clock fixedClock = Clock.fixed(
        Instant.parse("2024-01-01T00:00:00Z"),
        ZoneOffset.UTC
    );

    private BatchScheduler scheduler;

    @BeforeEach
    void setUp() {
        Map<String, Job> jobMap = Map.of(
            "modeMan" + BatchController.JOB_NAME_SUFFIX, modeManCrawlingJob,
            "nestStore" + BatchController.JOB_NAME_SUFFIX, nestStoreCrawlingJob,
            "semiBasement" + BatchController.JOB_NAME_SUFFIX, semiBasementCrawlingJob
        );
        // new 로 직접 생성 → @PostConstruct(start()) 호출 없음
        scheduler = new BatchScheduler(taskScheduler, subscriptionRepository, jobMap, jobLauncher, fixedClock);
    }

    // -----------------------------------------------------------------------
    // start() 테스트
    // -----------------------------------------------------------------------

    @Test
    void start_구독있으면_ACTIVE딜레이로_초기스케줄() {
        // given
        given(subscriptionRepository.existsAny()).willReturn(true);

        // when
        scheduler.start();

        // then
        ArgumentCaptor<Instant> instantCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(taskScheduler).schedule(any(Runnable.class), instantCaptor.capture());
        assertThat(instantCaptor.getValue())
            .isEqualTo(fixedClock.instant().plusMillis(BatchScheduler.DELAY_ACTIVE_MS));
    }

    @Test
    void start_구독없으면_IDLE딜레이로_초기스케줄() {
        // given
        given(subscriptionRepository.existsAny()).willReturn(false);

        // when
        scheduler.start();

        // then
        ArgumentCaptor<Instant> instantCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(taskScheduler).schedule(any(Runnable.class), instantCaptor.capture());
        assertThat(instantCaptor.getValue())
            .isEqualTo(fixedClock.instant().plusMillis(BatchScheduler.DELAY_IDLE_MS));
    }

    // -----------------------------------------------------------------------
    // runAndReschedule() 테스트
    // -----------------------------------------------------------------------

    @Test
    void runAndReschedule_existsAny_true_3개Job실행후_ACTIVE딜레이로재스케줄() throws Exception {
        // given
        given(subscriptionRepository.existsAny()).willReturn(true);

        // when
        scheduler.runAndReschedule();

        // then - 3개 Job 실행 검증
        verify(jobLauncher, times(3)).run(any(Job.class), any(JobParameters.class));

        // then - ACTIVE 딜레이(20분)로 다음 스케줄 등록 검증
        ArgumentCaptor<Instant> instantCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(taskScheduler).schedule(any(Runnable.class), instantCaptor.capture());
        assertThat(instantCaptor.getValue())
            .isEqualTo(fixedClock.instant().plusMillis(BatchScheduler.DELAY_ACTIVE_MS));
    }

    @Test
    void runAndReschedule_existsAny_false_3개Job실행후_IDLE딜레이로재스케줄() throws Exception {
        // given
        given(subscriptionRepository.existsAny()).willReturn(false);

        // when
        scheduler.runAndReschedule();

        // then - 3개 Job 실행 검증
        verify(jobLauncher, times(3)).run(any(Job.class), any(JobParameters.class));

        // then - IDLE 딜레이(60분)로 다음 스케줄 등록 검증
        ArgumentCaptor<Instant> instantCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(taskScheduler).schedule(any(Runnable.class), instantCaptor.capture());
        assertThat(instantCaptor.getValue())
            .isEqualTo(fixedClock.instant().plusMillis(BatchScheduler.DELAY_IDLE_MS));
    }

    @Test
    void runAllJobs_첫번째사이트_예외발생시_나머지사이트계속실행() throws Exception {
        // given - modeMan Job 실행 시 예외 발생
        given(subscriptionRepository.existsAny()).willReturn(true);
        willThrow(new JobExecutionAlreadyRunningException("test error"))
            .given(jobLauncher).run(eq(modeManCrawlingJob), any(JobParameters.class));

        // when - 예외가 외부로 전파되지 않아야 한다
        scheduler.runAndReschedule();

        // then - modeMan 포함 3회 시도 검증 (modeMan은 실패했지만 나머지는 실행)
        verify(jobLauncher, times(3)).run(any(Job.class), any(JobParameters.class));
        verify(jobLauncher).run(eq(nestStoreCrawlingJob), any(JobParameters.class));
        verify(jobLauncher).run(eq(semiBasementCrawlingJob), any(JobParameters.class));

        // then - 예외 발생 후에도 finally 블록에서 다음 스케줄이 등록되어야 한다 (existsAny=true → ACTIVE 딜레이)
        ArgumentCaptor<Instant> captor = ArgumentCaptor.forClass(Instant.class);
        verify(taskScheduler).schedule(any(Runnable.class), captor.capture());
        assertThat(captor.getValue())
            .isEqualTo(fixedClock.instant().plusMillis(BatchScheduler.DELAY_ACTIVE_MS));
    }
}
