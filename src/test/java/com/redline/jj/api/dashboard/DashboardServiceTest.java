package com.redline.jj.api.dashboard;

import com.redline.jj.api.dashboard.dto.PriceComparisonResponse;
import com.redline.jj.api.dashboard.dto.PriceHistoryResponse;
import com.redline.jj.api.dashboard.dto.RecentRestockResponse;
import com.redline.jj.common.exception.BusinessException;
import com.redline.jj.common.exception.ErrorCode;
import com.redline.jj.domain.brand.Brand;
import com.redline.jj.domain.model.Model;
import com.redline.jj.domain.notification.RestockNotification;
import com.redline.jj.domain.option.SiteOption;
import com.redline.jj.domain.option.SiteOptionLog;
import com.redline.jj.domain.option.SiteOptionLogRepository;
import com.redline.jj.domain.option.SiteOptionRepository;
import com.redline.jj.domain.notification.RestockNotificationRepository;
import com.redline.jj.domain.model.ModelRepository;
import com.redline.jj.domain.site.Site;
import com.redline.jj.domain.user.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private ModelRepository modelRepository;

    @Mock
    private SiteOptionRepository siteOptionRepository;

    @Mock
    private SiteOptionLogRepository siteOptionLogRepository;

    @Mock
    private RestockNotificationRepository restockNotificationRepository;

    @InjectMocks
    private DashboardService dashboardService;

    // -----------------------------------------------------------------------
    // 공통 픽스처 헬퍼
    // -----------------------------------------------------------------------

    private Brand buildBrand() {
        return Brand.builder()
            .id(1L)
            .brandName("Levis")
            .build();
    }

    private Model buildModel(Brand brand) {
        return Model.builder()
            .id(1L)
            .modelName("501")
            .brand(brand)
            .build();
    }

    private Site buildSite(Long id, String name, String link) {
        return Site.builder()
            .id(id)
            .siteName(name)
            .siteLink(link)
            .build();
    }

    private SiteOption buildSiteOption(Long id, Site site, Model model, String label, int price) {
        return SiteOption.builder()
            .id(id)
            .site(site)
            .model(model)
            .optionLabel(label)
            .url("https://example.com/" + id)
            .inStock(true)
            .price(price)
            .lastCapturedAt(LocalDateTime.now())
            .build();
    }

    private SiteOptionLog buildSiteOptionLog(Long id, SiteOption siteOption, String label, int price,
                                              LocalDateTime capturedAt) {
        return SiteOptionLog.builder()
            .id(id)
            .siteOption(siteOption)
            .capturedAt(capturedAt)
            .optionLabel(label)
            .price(price)
            .inStock(true)
            .build();
    }

    // -----------------------------------------------------------------------
    // getPriceComparison 테스트
    // -----------------------------------------------------------------------

    @Test
    void getPriceComparison_모델없으면_MODEL_NOT_FOUND() {
        // given
        given(modelRepository.findById(99L)).willReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> dashboardService.getPriceComparison(99L))
            .isInstanceOf(BusinessException.class)
            .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.MODEL_NOT_FOUND));
    }

    @Test
    void getPriceComparison_사이트별옵션그룹화_구조검증() {
        // given
        Brand brand = buildBrand();
        Model model = buildModel(brand);

        Site site1 = buildSite(1L, "modeMan", "https://mode-man.com");
        Site site2 = buildSite(2L, "nestStore", "https://nest-store.com");

        SiteOption opt1 = buildSiteOption(1L, site1, model, "30/30", 89000);
        SiteOption opt2 = buildSiteOption(2L, site1, model, "32/32", 89000);
        SiteOption opt3 = buildSiteOption(3L, site2, model, "30/30", 92000);

        given(modelRepository.findById(1L)).willReturn(Optional.of(model));
        given(siteOptionRepository.search(isNull(), eq(1L), isNull()))
            .willReturn(List.of(opt1, opt2, opt3));

        // when
        PriceComparisonResponse result = dashboardService.getPriceComparison(1L);

        // then
        assertThat(result.getSites()).hasSize(2);

        PriceComparisonResponse.SiteRow site1Row = result.getSites().stream()
            .filter(r -> r.siteId().equals(1L))
            .findFirst()
            .orElseThrow();
        assertThat(site1Row.options()).hasSize(2);

        PriceComparisonResponse.SiteRow site2Row = result.getSites().stream()
            .filter(r -> r.siteId().equals(2L))
            .findFirst()
            .orElseThrow();
        assertThat(site2Row.options()).hasSize(1);
    }

    // -----------------------------------------------------------------------
    // getPriceHistory 테스트
    // -----------------------------------------------------------------------

    @Test
    void getPriceHistory_모델없으면_MODEL_NOT_FOUND() {
        // given
        given(modelRepository.findById(99L)).willReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> dashboardService.getPriceHistory(99L, 30))
            .isInstanceOf(BusinessException.class)
            .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.MODEL_NOT_FOUND));
    }

    @Test
    void getPriceHistory_음수days_INVALID_DAYS() {
        // given - days 검증은 모델 조회 전에 발생하므로 mock 불필요

        // when / then
        assertThatThrownBy(() -> dashboardService.getPriceHistory(1L, -1))
            .isInstanceOf(BusinessException.class)
            .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_DAYS));
    }

    @Test
    void getPriceHistory_days30_로그조회_오름차순() {
        // given
        Brand brand = buildBrand();
        Model model = buildModel(brand);
        Site site = buildSite(1L, "modeMan", "https://mode-man.com");
        SiteOption siteOption = buildSiteOption(1L, site, model, "30/30", 89000);

        LocalDateTime earlier = LocalDateTime.of(2024, 1, 1, 10, 0);
        LocalDateTime later   = LocalDateTime.of(2024, 1, 2, 10, 0);

        SiteOptionLog log1 = buildSiteOptionLog(1L, siteOption, "30/30", 85000, earlier);
        SiteOptionLog log2 = buildSiteOptionLog(2L, siteOption, "30/30", 89000, later);

        given(modelRepository.findById(1L)).willReturn(Optional.of(model));
        given(siteOptionLogRepository.findByModelIdSince(eq(1L), any(LocalDateTime.class)))
            .willReturn(List.of(log1, log2));

        // when
        PriceHistoryResponse result = dashboardService.getPriceHistory(1L, 30);

        // then
        assertThat(result.getHistories()).hasSize(1);
        List<PriceHistoryResponse.PricePoint> points = result.getHistories().get(0).points();
        assertThat(points).hasSize(2);
        assertThat(points.get(0).capturedAt()).isBefore(points.get(1).capturedAt());
        verify(siteOptionLogRepository).findByModelIdSince(eq(1L), any(LocalDateTime.class));
    }

    @Test
    void getPriceHistory_days0_빈로그() {
        // given
        Brand brand = buildBrand();
        Model model = buildModel(brand);

        given(modelRepository.findById(1L)).willReturn(Optional.of(model));
        given(siteOptionLogRepository.findByModelIdSince(eq(1L), any(LocalDateTime.class)))
            .willReturn(List.of());

        // when
        PriceHistoryResponse result = dashboardService.getPriceHistory(1L, 0);

        // then
        assertThat(result.getHistories()).isEmpty();
    }

    // -----------------------------------------------------------------------
    // getRecentRestocks 테스트
    // -----------------------------------------------------------------------

    @Test
    void getRecentRestocks_최대10건반환() {
        // given
        Brand brand = buildBrand();
        Model model = buildModel(brand);
        User user = User.builder().id(1L).userId("testUser").userPw("pw").userName("테스터").build();

        RestockNotification n1 = RestockNotification.builder()
            .id(1L)
            .model(model)
            .user(user)
            .build();
        RestockNotification n2 = RestockNotification.builder()
            .id(2L)
            .model(model)
            .user(user)
            .build();

        given(restockNotificationRepository.findTop10WithModelOrderByCreatedAtDesc())
            .willReturn(List.of(n1, n2));

        // when
        List<RecentRestockResponse> result = dashboardService.getRecentRestocks();

        // then
        assertThat(result).hasSize(2);
    }

    @Test
    void getRecentRestocks_10건_순서유지() {
        // given
        Brand brand = buildBrand();
        Model model = buildModel(brand);
        User user = User.builder().id(1L).userId("testUser").userPw("pw").userName("테스터").build();

        List<RestockNotification> notifications = new ArrayList<>();
        for (long i = 1; i <= 10; i++) {
            notifications.add(RestockNotification.builder().id(i).model(model).user(user).build());
        }

        given(restockNotificationRepository.findTop10WithModelOrderByCreatedAtDesc())
            .willReturn(notifications);

        // when
        List<RecentRestockResponse> result = dashboardService.getRecentRestocks();

        // then
        assertThat(result).hasSize(10);
        assertThat(result.get(0).getNotificationId()).isEqualTo(1L);
        assertThat(result.get(9).getNotificationId()).isEqualTo(10L);
    }
}
