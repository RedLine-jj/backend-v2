package com.redline.jj.batch;

import com.redline.jj.common.exception.ErrorCode;
import com.redline.jj.common.response.ApiResponse;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.JobExecutionException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/batch/crawl")
public class BatchController {

    // CrawlingJobConfig에서 Job 빈 이름은 반드시 {siteName}CrawlingJob 형식을 따라야 한다.
    // 예) modeManCrawlingJob, nestStoreCrawlingJob
    public static final String JOB_NAME_SUFFIX = "CrawlingJob";

    private final Map<String, Job> crawlingJobMap;
    private final JobLauncher jobLauncher;
    private final Clock clock;

    public BatchController(Map<String, Job> crawlingJobMap, JobLauncher jobLauncher, Clock clock) {
        this.crawlingJobMap = crawlingJobMap;
        this.jobLauncher = jobLauncher;
        this.clock = clock;
    }

    @PostMapping("/{site}")
    public ResponseEntity<ApiResponse<Void>> trigger(@PathVariable String site) {
        Job job = crawlingJobMap.get(site + JOB_NAME_SUFFIX);

        if (job == null) {
            return ResponseEntity
                .badRequest()
                .body(ApiResponse.fail("E400", "지원하지 않는 사이트입니다: " + site));
        }

        try {
            JobParameters params = new JobParametersBuilder()
                .addLocalDateTime("triggeredAt", LocalDateTime.now(clock))
                .toJobParameters();
            jobLauncher.run(job, params);
            return ResponseEntity.ok(ApiResponse.ok());
        } catch (JobExecutionException e) {
            return ResponseEntity
                .internalServerError()
                .body(ApiResponse.fail(ErrorCode.CRAWLING_FAILED));
        }
    }
}
