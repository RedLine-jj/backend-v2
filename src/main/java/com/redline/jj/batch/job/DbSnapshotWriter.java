package com.redline.jj.batch.job;

import com.redline.jj.batch.dto.ResolvedItem;
import com.redline.jj.domain.option.SiteOption;
import com.redline.jj.domain.option.SiteOptionLog;
import com.redline.jj.domain.option.SiteOptionLogRepository;
import com.redline.jj.domain.option.SiteOptionRepository;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;

@Component
public class DbSnapshotWriter implements ItemWriter<ResolvedItem> {

    private final Clock clock;
    private final SiteOptionRepository siteOptionRepository;
    private final SiteOptionLogRepository siteOptionLogRepository;
    private final ApplicationEventPublisher eventPublisher;

    public DbSnapshotWriter(
            Clock clock,
            SiteOptionRepository siteOptionRepository,
            SiteOptionLogRepository siteOptionLogRepository,
            ApplicationEventPublisher eventPublisher) {
        this.clock = clock;
        this.siteOptionRepository = siteOptionRepository;
        this.siteOptionLogRepository = siteOptionLogRepository;
        this.eventPublisher = eventPublisher;
    }

    // Step 청크 트랜잭션 위임 — 자체 @Transactional 시 AFTER_COMMIT 이벤트가 Step 커밋 전에 발행됨
    @Override
    public void write(Chunk<? extends ResolvedItem> items) {
        for (ResolvedItem item : items) {
            processItem(item);
        }
    }

    private void processItem(ResolvedItem item) {
        siteOptionRepository
            .findBySite_IdAndModel_IdAndOptionLabel(
                item.site().getId(), item.model().getId(), item.optionLabel())
            .ifPresentOrElse(
                existing -> updateExisting(existing, item),
                () -> createNew(item)
            );
    }

    private void updateExisting(SiteOption siteOption, ResolvedItem item) {
        boolean oldInStock = siteOption.isInStock();
        Integer oldPrice = siteOption.getPrice();

        boolean isRestock = siteOption.updateSnapshot(item.inStock(), item.price());
        siteOption.updateUrl(item.url());
        siteOption.updateLastCapturedAt(LocalDateTime.now(clock));

        boolean changed = (oldInStock != item.inStock())
            || !Objects.equals(oldPrice, item.price());

        if (changed) {
            siteOptionLogRepository.save(buildLog(siteOption, item));
        }

        if (isRestock) {
            eventPublisher.publishEvent(new RestockEvent(
                item.model().getId(),
                item.model().getModelName(),
                item.model().getBrand().getBrandName()
            ));
        }
    }

    private void createNew(ResolvedItem item) {
        SiteOption newOption = siteOptionRepository.save(
            SiteOption.builder()
                .site(item.site())
                .model(item.model())
                .optionLabel(item.optionLabel())
                .url(item.url())
                .inStock(item.inStock())
                .price(item.price())
                .lastCapturedAt(LocalDateTime.now(clock))
                .build()
        );

        if (item.inStock()) {
            siteOptionLogRepository.save(buildLog(newOption, item));
        }
    }

    private SiteOptionLog buildLog(SiteOption siteOption, ResolvedItem item) {
        return SiteOptionLog.builder()
            .siteOption(siteOption)
            .capturedAt(LocalDateTime.now(clock))
            .optionLabel(item.optionLabel())
            .price(item.price())
            .inStock(item.inStock())
            .build();
    }
}
