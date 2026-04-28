package com.redline.jj.batch.job;

import com.redline.jj.batch.crawler.modeman.ModeManDetailParser;
import com.redline.jj.batch.crawler.modeman.ModeManListParser;
import com.redline.jj.batch.crawler.neststore.NestStoreDetailParser;
import com.redline.jj.batch.crawler.neststore.NestStoreListParser;
import com.redline.jj.batch.crawler.semibasement.SemiBasementDetailParser;
import com.redline.jj.batch.crawler.semibasement.SemiBasementListParser;
import com.redline.jj.batch.dto.ResolvedItem;
import com.redline.jj.batch.matching.ModelResolutionService;
import com.redline.jj.common.exception.BusinessException;
import com.redline.jj.domain.site.SiteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@RequiredArgsConstructor
public class CrawlingJobConfig {

    private final DbSnapshotWriter dbSnapshotWriter;
    private final ModelResolutionService modelResolutionService;
    private final SiteRepository siteRepository;

    // =========== ModeMan ===========

    // 새 사이트 추가 시: {siteName}CrawlingJob 형식으로 빈 이름 설정 — BatchController에서 site + JOB_NAME_SUFFIX 로 조회
    @Bean
    public Job modeManCrawlingJob(JobRepository jobRepository,
                                  @Qualifier("modeManCrawlingStep") Step step) {
        return new JobBuilder("modeManCrawlingJob", jobRepository)
            .start(step)
            .build();
    }

    @Bean("modeManCrawlingStep")
    public Step modeManCrawlingStep(JobRepository jobRepository,
                                    PlatformTransactionManager transactionManager,
                                    @Qualifier("modeManCrawlReader") ItemReader<String> reader,
                                    @Qualifier("modeManProcessor") ItemProcessor<String, ResolvedItem> processor) {
        return new StepBuilder("modeManCrawlingStep", jobRepository)
            .<String, ResolvedItem>chunk(50, transactionManager)
            .reader(reader)
            .processor(processor)
            .writer(dbSnapshotWriter)
            .faultTolerant()
            .skip(BusinessException.class)
            .skipLimit(100)
            .build();
    }

    @Bean("modeManCrawlReader")
    @StepScope
    public CrawlItemReader modeManCrawlReader(ModeManListParser listParser) {
        return new CrawlItemReader(listParser);
    }

    @Bean("modeManProcessor")
    @StepScope
    public ModelResolutionProcessor modeManProcessor(ModeManDetailParser detailParser) {
        return new ModelResolutionProcessor(detailParser, modelResolutionService, siteRepository, "modeMan");
    }

    // =========== NestStore ===========

    // 새 사이트 추가 시: {siteName}CrawlingJob 형식으로 빈 이름 설정 — BatchController에서 site + JOB_NAME_SUFFIX 로 조회
    @Bean
    public Job nestStoreCrawlingJob(JobRepository jobRepository,
                                    @Qualifier("nestStoreCrawlingStep") Step step) {
        return new JobBuilder("nestStoreCrawlingJob", jobRepository)
            .start(step)
            .build();
    }

    @Bean("nestStoreCrawlingStep")
    public Step nestStoreCrawlingStep(JobRepository jobRepository,
                                      PlatformTransactionManager transactionManager,
                                      @Qualifier("nestStoreCrawlReader") ItemReader<String> reader,
                                      @Qualifier("nestStoreProcessor") ItemProcessor<String, ResolvedItem> processor) {
        return new StepBuilder("nestStoreCrawlingStep", jobRepository)
            .<String, ResolvedItem>chunk(50, transactionManager)
            .reader(reader)
            .processor(processor)
            .writer(dbSnapshotWriter)
            .faultTolerant()
            .skip(BusinessException.class)
            .skipLimit(100)
            .build();
    }

    @Bean("nestStoreCrawlReader")
    @StepScope
    public CrawlItemReader nestStoreCrawlReader(NestStoreListParser listParser) {
        return new CrawlItemReader(listParser);
    }

    @Bean("nestStoreProcessor")
    @StepScope
    public ModelResolutionProcessor nestStoreProcessor(NestStoreDetailParser detailParser) {
        return new ModelResolutionProcessor(detailParser, modelResolutionService, siteRepository, "nestStore");
    }

    // =========== SemiBasement ===========

    // 새 사이트 추가 시: {siteName}CrawlingJob 형식으로 빈 이름 설정 — BatchController에서 site + JOB_NAME_SUFFIX 로 조회
    @Bean
    public Job semiBasementCrawlingJob(JobRepository jobRepository,
                                       @Qualifier("semiBasementCrawlingStep") Step step) {
        return new JobBuilder("semiBasementCrawlingJob", jobRepository)
            .start(step)
            .build();
    }

    @Bean("semiBasementCrawlingStep")
    public Step semiBasementCrawlingStep(JobRepository jobRepository,
                                         PlatformTransactionManager transactionManager,
                                         @Qualifier("semiBasementCrawlReader") ItemReader<String> reader,
                                         @Qualifier("semiBasementProcessor") ItemProcessor<String, ResolvedItem> processor) {
        return new StepBuilder("semiBasementCrawlingStep", jobRepository)
            .<String, ResolvedItem>chunk(50, transactionManager)
            .reader(reader)
            .processor(processor)
            .writer(dbSnapshotWriter)
            .faultTolerant()
            .skip(BusinessException.class)
            .skipLimit(100)
            .build();
    }

    @Bean("semiBasementCrawlReader")
    @StepScope
    public CrawlItemReader semiBasementCrawlReader(SemiBasementListParser listParser) {
        return new CrawlItemReader(listParser);
    }

    @Bean("semiBasementProcessor")
    @StepScope
    public ModelResolutionProcessor semiBasementProcessor(SemiBasementDetailParser detailParser) {
        return new ModelResolutionProcessor(detailParser, modelResolutionService, siteRepository, "semiBasement");
    }
}
