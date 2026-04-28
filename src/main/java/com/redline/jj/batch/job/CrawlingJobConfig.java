package com.redline.jj.batch.job;

import com.redline.jj.batch.crawler.DetailParser;
import com.redline.jj.batch.crawler.modeman.ModeManDetailParser;
import com.redline.jj.batch.crawler.modeman.ModeManListParser;
import com.redline.jj.batch.crawler.neststore.NestStoreDetailParser;
import com.redline.jj.batch.crawler.neststore.NestStoreListParser;
import com.redline.jj.batch.crawler.semibasement.SemiBasementDetailParser;
import com.redline.jj.batch.crawler.semibasement.SemiBasementListParser;
import com.redline.jj.batch.dto.ResolvedItem;
import com.redline.jj.batch.matching.ModelResolutionService;
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
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    // 새 사이트 추가 시: SiteDescriptor.of("{siteName}") 로 생성하고 아래 4개 @Bean 메서드를 추가한다.
    // BatchController 는 {siteName}CrawlingJob 형식으로 Job 빈을 조회한다.
    record SiteDescriptor(String siteName, String readerBeanName, String processorBeanName,
                          String stepBeanName, String jobBeanName) {
        static SiteDescriptor of(String siteName) {
            return new SiteDescriptor(
                siteName,
                siteName + "CrawlReader",
                siteName + "Processor",
                siteName + "CrawlingStep",
                siteName + "CrawlingJob"
            );
        }
    }

    // ===== ModeMan =====

    @Bean("modeManCrawlingJob")
    public Job modeManCrawlingJob(@Qualifier("modeManCrawlingStep") Step step) {
        return buildJob(SiteDescriptor.of("modeMan"), step);
    }

    @Bean("modeManCrawlingStep")
    public Step modeManCrawlingStep(@Qualifier("modeManCrawlReader") ItemReader<String> reader,
                                    @Qualifier("modeManProcessor") ItemProcessor<String, ResolvedItem> processor) {
        return buildStep(SiteDescriptor.of("modeMan"), reader, processor);
    }

    @Bean("modeManCrawlReader")
    @StepScope
    public CrawlItemReader modeManCrawlReader(ModeManListParser listParser) {
        return new CrawlItemReader(listParser);
    }

    @Bean("modeManProcessor")
    @StepScope
    public ModelResolutionProcessor modeManProcessor(ModeManDetailParser detailParser) {
        return buildProcessor(detailParser, "modeMan");
    }

    // ===== NestStore =====

    @Bean("nestStoreCrawlingJob")
    public Job nestStoreCrawlingJob(@Qualifier("nestStoreCrawlingStep") Step step) {
        return buildJob(SiteDescriptor.of("nestStore"), step);
    }

    @Bean("nestStoreCrawlingStep")
    public Step nestStoreCrawlingStep(@Qualifier("nestStoreCrawlReader") ItemReader<String> reader,
                                      @Qualifier("nestStoreProcessor") ItemProcessor<String, ResolvedItem> processor) {
        return buildStep(SiteDescriptor.of("nestStore"), reader, processor);
    }

    @Bean("nestStoreCrawlReader")
    @StepScope
    public CrawlItemReader nestStoreCrawlReader(NestStoreListParser listParser) {
        return new CrawlItemReader(listParser);
    }

    @Bean("nestStoreProcessor")
    @StepScope
    public ModelResolutionProcessor nestStoreProcessor(NestStoreDetailParser detailParser) {
        return buildProcessor(detailParser, "nestStore");
    }

    // ===== SemiBasement =====

    @Bean("semiBasementCrawlingJob")
    public Job semiBasementCrawlingJob(@Qualifier("semiBasementCrawlingStep") Step step) {
        return buildJob(SiteDescriptor.of("semiBasement"), step);
    }

    @Bean("semiBasementCrawlingStep")
    public Step semiBasementCrawlingStep(@Qualifier("semiBasementCrawlReader") ItemReader<String> reader,
                                         @Qualifier("semiBasementProcessor") ItemProcessor<String, ResolvedItem> processor) {
        return buildStep(SiteDescriptor.of("semiBasement"), reader, processor);
    }

    @Bean("semiBasementCrawlReader")
    @StepScope
    public CrawlItemReader semiBasementCrawlReader(SemiBasementListParser listParser) {
        return new CrawlItemReader(listParser);
    }

    @Bean("semiBasementProcessor")
    @StepScope
    public ModelResolutionProcessor semiBasementProcessor(SemiBasementDetailParser detailParser) {
        return buildProcessor(detailParser, "semiBasement");
    }

    // ===== 팩토리 헬퍼 =====

    private Job buildJob(SiteDescriptor site, Step step) {
        return new JobBuilder(site.jobBeanName(), jobRepository)
            .start(step)
            .build();
    }

    private Step buildStep(SiteDescriptor site, ItemReader<String> reader,
                           ItemProcessor<String, ResolvedItem> processor) {
        return new StepBuilder(site.stepBeanName(), jobRepository)
            .<String, ResolvedItem>chunk(50, transactionManager)
            .reader(reader)
            .processor(processor)
            .writer(dbSnapshotWriter)
            .faultTolerant()
            .skipPolicy(new CrawlingSkipPolicy())
            .build();
    }

    private ModelResolutionProcessor buildProcessor(DetailParser detailParser, String siteName) {
        return new ModelResolutionProcessor(detailParser, modelResolutionService, siteRepository, siteName);
    }
}
