package com.redline.jj.batch;

import com.redline.jj.domain.subscription.SubscriptionRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class BatchScheduler {

    static final long DELAY_ACTIVE_MS = 20 * 60 * 1000L;
    static final long DELAY_IDLE_MS   = 60 * 60 * 1000L;

    private final TaskScheduler taskScheduler;
    private final SubscriptionRepository subscriptionRepository;
    private final Map<String, Job> crawlingJobMap;
    private final JobLauncher jobLauncher;
    private final Clock clock;

    @PostConstruct
    public void start() {
        scheduleNext(DELAY_ACTIVE_MS);
    }

    void runAndReschedule() {
        runAllJobs();
        scheduleNext(subscriptionRepository.existsAny() ? DELAY_ACTIVE_MS : DELAY_IDLE_MS);
    }

    private void runAllJobs() {
        for (String site : List.of("modeMan", "nestStore", "semiBasement")) {
            try {
                Job job = crawlingJobMap.get(site + BatchController.JOB_NAME_SUFFIX);
                if (job == null) {
                    log.warn("[BatchScheduler] Job을 찾을 수 없습니다: {}", site + BatchController.JOB_NAME_SUFFIX);
                    continue;
                }
                JobParameters params = new JobParametersBuilder()
                    .addLocalDateTime("triggeredAt", LocalDateTime.now(clock))
                    .toJobParameters();
                jobLauncher.run(job, params);
            } catch (Exception e) {
                log.error("[BatchScheduler] {} 크롤링 실패 — 격리 후 계속", site, e);
            }
        }
    }

    private void scheduleNext(long delayMs) {
        taskScheduler.schedule(this::runAndReschedule, Instant.now(clock).plusMillis(delayMs));
    }
}
