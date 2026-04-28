package com.redline.jj.batch.job;

import com.redline.jj.batch.dto.ResolvedItem;
import com.redline.jj.domain.brand.Brand;
import com.redline.jj.domain.model.Model;
import com.redline.jj.domain.option.SiteOption;
import com.redline.jj.domain.option.SiteOptionLog;
import com.redline.jj.domain.option.SiteOptionLogRepository;
import com.redline.jj.domain.option.SiteOptionRepository;
import com.redline.jj.domain.site.Site;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.item.Chunk;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DbSnapshotWriterTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-01-01T00:00:00Z");
    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");
    private static final LocalDateTime FIXED_NOW = LocalDateTime.ofInstant(FIXED_INSTANT, ZONE);

    @Mock
    SiteOptionRepository siteOptionRepository;

    @Mock
    SiteOptionLogRepository siteOptionLogRepository;

    @Mock
    ApplicationEventPublisher eventPublisher;

    DbSnapshotWriter writer;

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(FIXED_INSTANT, ZONE);
        writer = new DbSnapshotWriter(fixedClock, siteOptionRepository, siteOptionLogRepository, eventPublisher);
    }

    // -----------------------------------------------------------------------
    // 기존 테스트
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("false에서 true로 재고 전환 시 Log 1건 저장, RestockEvent 1회 발행")
    void write_false에서true로_재고변환_Log1건_publish1회() throws Exception {
        SiteOption existing = buildSiteOption(false, 89000);
        when(siteOptionRepository.findBySite_IdAndModel_IdAndOptionLabel(any(), any(), any()))
            .thenReturn(Optional.of(existing));

        writer.write(buildChunk(buildResolvedItem(true, 89000)));

        verify(siteOptionLogRepository, times(1)).save(any(SiteOptionLog.class));
        verify(eventPublisher, times(1)).publishEvent(any(RestockEvent.class));
    }

    @Test
    @DisplayName("true에서 true로 가격 동일 시 Log 0건, RestockEvent 0회")
    void write_true에서true로_가격동일_Log0건_publish0회() throws Exception {
        SiteOption existing = buildSiteOption(true, 89000);
        when(siteOptionRepository.findBySite_IdAndModel_IdAndOptionLabel(any(), any(), any()))
            .thenReturn(Optional.of(existing));

        writer.write(buildChunk(buildResolvedItem(true, 89000)));

        verify(siteOptionLogRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("true에서 true로 가격 변동 시 Log 1건 저장, RestockEvent 0회")
    void write_true에서true로_가격변동_Log1건_publish0회() throws Exception {
        SiteOption existing = buildSiteOption(true, 89000);
        when(siteOptionRepository.findBySite_IdAndModel_IdAndOptionLabel(any(), any(), any()))
            .thenReturn(Optional.of(existing));

        writer.write(buildChunk(buildResolvedItem(true, 95000)));

        verify(siteOptionLogRepository, times(1)).save(any(SiteOptionLog.class));
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("true에서 false로 품절 전환 시 Log 1건 저장, RestockEvent 0회")
    void write_true에서false로_품절_Log1건_publish0회() throws Exception {
        SiteOption existing = buildSiteOption(true, 89000);
        when(siteOptionRepository.findBySite_IdAndModel_IdAndOptionLabel(any(), any(), any()))
            .thenReturn(Optional.of(existing));

        writer.write(buildChunk(buildResolvedItem(false, 89000)));

        verify(siteOptionLogRepository, times(1)).save(any(SiteOptionLog.class));
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("false에서 false로 변화 없을 시 Log 0건, RestockEvent 0회")
    void write_false에서false로_변화없음_Log0건_publish0회() throws Exception {
        SiteOption existing = buildSiteOption(false, 89000);
        when(siteOptionRepository.findBySite_IdAndModel_IdAndOptionLabel(any(), any(), any()))
            .thenReturn(Optional.of(existing));

        writer.write(buildChunk(buildResolvedItem(false, 89000)));

        verify(siteOptionLogRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("신규 SiteOption이고 재고 있을 때 Log 1건 저장, lastCapturedAt이 고정 시각과 일치")
    void write_신규SiteOption_재고있음_Log1건_lastCapturedAt검증() throws Exception {
        when(siteOptionRepository.findBySite_IdAndModel_IdAndOptionLabel(any(), any(), any()))
            .thenReturn(Optional.empty());
        ArgumentCaptor<SiteOption> siteOptionCaptor = ArgumentCaptor.forClass(SiteOption.class);
        when(siteOptionRepository.save(siteOptionCaptor.capture())).thenAnswer(i -> i.getArgument(0));

        writer.write(buildChunk(buildResolvedItem(true, 89000)));

        assertThat(siteOptionCaptor.getValue().getLastCapturedAt()).isEqualTo(FIXED_NOW);
        verify(siteOptionLogRepository, times(1)).save(any(SiteOptionLog.class));
    }

    @Test
    @DisplayName("신규 SiteOption이고 재고 없을 때 Log 0건")
    void write_신규SiteOption_재고없음_Log0건() throws Exception {
        when(siteOptionRepository.findBySite_IdAndModel_IdAndOptionLabel(any(), any(), any()))
            .thenReturn(Optional.empty());
        when(siteOptionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        writer.write(buildChunk(buildResolvedItem(false, 89000)));

        verify(siteOptionLogRepository, never()).save(any());
    }

    // -----------------------------------------------------------------------
    // 1. AFTER_COMMIT 이벤트 발행 — ArgumentCaptor로 RestockEvent 값 검증
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("재고 false→true 시 publishEvent에 올바른 modelId, modelName, brandName이 전달된다")
    void write_재고전환시_RestockEvent_값검증() throws Exception {
        // given
        Brand brand = Brand.builder().brandName("테스트브랜드").build();
        Model model = Model.builder().brand(brand).modelName("테스트모델").build();
        ReflectionTestUtils.setField(model, "id", 42L);

        Site site = Site.builder().siteName("테스트사이트").build();
        SiteOption existing = SiteOption.builder()
            .site(site).model(model).optionLabel("M")
            .url("http://test.com").inStock(false).price(89000)
            .lastCapturedAt(FIXED_NOW).build();

        when(siteOptionRepository.findBySite_IdAndModel_IdAndOptionLabel(any(), any(), any()))
            .thenReturn(Optional.of(existing));

        ResolvedItem item = new ResolvedItem(model, site, "M", 89000, true, "http://test.com", "테스트모델");

        // when
        writer.write(new Chunk<>(List.of(item)));

        // then
        ArgumentCaptor<RestockEvent> eventCaptor = ArgumentCaptor.forClass(RestockEvent.class);
        verify(eventPublisher, times(1)).publishEvent(eventCaptor.capture());

        RestockEvent event = eventCaptor.getValue();
        assertThat(event.modelId()).isEqualTo(42L);
        assertThat(event.modelName()).isEqualTo("테스트모델");
        assertThat(event.brandName()).isEqualTo("테스트브랜드");
    }

    @Test
    @DisplayName("재고 true→true(가격 변경)이면 publishEvent 미호출 — 재고 이벤트와 가격 변경은 독립")
    void write_가격변경만있을때_publishEvent미호출() throws Exception {
        // given
        SiteOption existing = buildSiteOption(true, 89000);
        when(siteOptionRepository.findBySite_IdAndModel_IdAndOptionLabel(any(), any(), any()))
            .thenReturn(Optional.of(existing));

        // when - 가격만 변경 (inStock은 동일)
        writer.write(buildChunk(buildResolvedItem(true, 120000)));

        // then
        verify(eventPublisher, never()).publishEvent(any());
        verify(siteOptionLogRepository, times(1)).save(any(SiteOptionLog.class));
    }

    @Test
    @DisplayName("재고 true→false(품절)이면 publishEvent 미호출")
    void write_품절전환시_publishEvent미호출() throws Exception {
        // given
        SiteOption existing = buildSiteOption(true, 89000);
        when(siteOptionRepository.findBySite_IdAndModel_IdAndOptionLabel(any(), any(), any()))
            .thenReturn(Optional.of(existing));

        // when
        writer.write(buildChunk(buildResolvedItem(false, 89000)));

        // then
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("재고 false→false(변화 없음)이면 publishEvent 미호출")
    void write_재고변화없음_publishEvent미호출() throws Exception {
        // given
        SiteOption existing = buildSiteOption(false, 89000);
        when(siteOptionRepository.findBySite_IdAndModel_IdAndOptionLabel(any(), any(), any()))
            .thenReturn(Optional.of(existing));

        // when
        writer.write(buildChunk(buildResolvedItem(false, 89000)));

        // then
        verify(eventPublisher, never()).publishEvent(any());
    }

    // -----------------------------------------------------------------------
    // 2. 청크에 여러 아이템이 있을 때 각각 독립적으로 처리
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("청크 내 2개 아이템 중 1개만 재고 전환 시 publishEvent 정확히 1회 발행")
    void write_청크내2개중1개만재고전환_publishEvent1회() throws Exception {
        // given
        Brand brand = Brand.builder().brandName("브랜드A").build();
        Model modelA = Model.builder().brand(brand).modelName("모델A").build();
        ReflectionTestUtils.setField(modelA, "id", 10L);
        Model modelB = Model.builder().brand(brand).modelName("모델B").build();
        ReflectionTestUtils.setField(modelB, "id", 20L);
        Site site = Site.builder().siteName("테스트사이트").build();

        // 아이템A: false→true (재고 복구)
        SiteOption existingA = SiteOption.builder()
            .site(site).model(modelA).optionLabel("S")
            .url("http://a.com").inStock(false).price(80000)
            .lastCapturedAt(FIXED_NOW).build();
        // 아이템B: true→true, 가격 동일 (변화 없음)
        SiteOption existingB = SiteOption.builder()
            .site(site).model(modelB).optionLabel("M")
            .url("http://b.com").inStock(true).price(90000)
            .lastCapturedAt(FIXED_NOW).build();

        when(siteOptionRepository.findBySite_IdAndModel_IdAndOptionLabel(null, 10L, "S"))
            .thenReturn(Optional.of(existingA));
        when(siteOptionRepository.findBySite_IdAndModel_IdAndOptionLabel(null, 20L, "M"))
            .thenReturn(Optional.of(existingB));

        ResolvedItem itemA = new ResolvedItem(modelA, site, "S", 80000, true, "http://a.com", "모델A");
        ResolvedItem itemB = new ResolvedItem(modelB, site, "M", 90000, true, "http://b.com", "모델B");

        // when
        writer.write(new Chunk<>(List.of(itemA, itemB)));

        // then
        verify(eventPublisher, times(1)).publishEvent(any(RestockEvent.class));
        verify(siteOptionLogRepository, times(1)).save(any(SiteOptionLog.class));
    }

    // -----------------------------------------------------------------------
    // 3. SiteOptionLog 내용 검증
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("저장되는 SiteOptionLog에 capturedAt이 고정 시각과 일치한다")
    void write_SiteOptionLog_capturedAt_고정시각일치() throws Exception {
        // given
        SiteOption existing = buildSiteOption(true, 89000);
        when(siteOptionRepository.findBySite_IdAndModel_IdAndOptionLabel(any(), any(), any()))
            .thenReturn(Optional.of(existing));

        ArgumentCaptor<SiteOptionLog> logCaptor = ArgumentCaptor.forClass(SiteOptionLog.class);
        when(siteOptionLogRepository.save(logCaptor.capture())).thenAnswer(i -> i.getArgument(0));

        // when - 가격 변경으로 로그 발생
        writer.write(buildChunk(buildResolvedItem(true, 95000)));

        // then
        SiteOptionLog savedLog = logCaptor.getValue();
        assertThat(savedLog.getCapturedAt()).isEqualTo(FIXED_NOW);
        assertThat(savedLog.getPrice()).isEqualTo(95000);
        assertThat(savedLog.isInStock()).isTrue();
    }

    // -----------------------------------------------------------------------
    // 4. 신규 SiteOption 생성 시 RestockEvent 발행 여부
    //    신규 아이템이 재고 있음으로 들어와도 RestockEvent는 발행하지 않는다
    //    (재입고 = 기존에 false → true 전환, 신규 등록은 해당 없음)
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("신규 SiteOption 재고 있음 생성 시 RestockEvent 미발행 (재입고 아님)")
    void write_신규SiteOption_재고있음_RestockEvent미발행() throws Exception {
        // given - 기존 SiteOption 없음
        when(siteOptionRepository.findBySite_IdAndModel_IdAndOptionLabel(any(), any(), any()))
            .thenReturn(Optional.empty());
        when(siteOptionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        // when
        writer.write(buildChunk(buildResolvedItem(true, 89000)));

        // then - 신규 등록은 재입고 이벤트가 아님
        verify(eventPublisher, never()).publishEvent(any());
    }

    // -----------------------------------------------------------------------
    // helpers
    // -----------------------------------------------------------------------

    private SiteOption buildSiteOption(boolean inStock, Integer price) {
        Brand brand = Brand.builder().brandName("테스트브랜드").build();
        Model model = Model.builder().brand(brand).modelName("테스트모델").build();
        Site site = Site.builder().siteName("테스트사이트").build();
        return SiteOption.builder()
            .site(site)
            .model(model)
            .optionLabel("S")
            .url("http://test.com")
            .inStock(inStock)
            .price(price)
            .lastCapturedAt(FIXED_NOW)
            .build();
    }

    private ResolvedItem buildResolvedItem(boolean inStock, Integer price) {
        Brand brand = Brand.builder().brandName("테스트브랜드").build();
        Model model = Model.builder().brand(brand).modelName("테스트모델").build();
        Site site = Site.builder().siteName("테스트사이트").build();
        return new ResolvedItem(model, site, "S", price, inStock, "http://test.com", "테스트모델");
    }

    private Chunk<ResolvedItem> buildChunk(ResolvedItem... items) {
        return new Chunk<>(List.of(items));
    }
}
