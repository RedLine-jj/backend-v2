package com.redline.jj.api.option;

import com.redline.jj.api.option.dto.SiteOptionLogResponse;
import com.redline.jj.api.option.dto.SiteOptionResponse;
import com.redline.jj.common.exception.BusinessException;
import com.redline.jj.common.exception.ErrorCode;
import com.redline.jj.common.response.CursorPage;
import com.redline.jj.domain.brand.Brand;
import com.redline.jj.domain.model.Model;
import com.redline.jj.domain.model.Model.ModelType;
import com.redline.jj.domain.option.SiteOption;
import com.redline.jj.domain.option.SiteOptionLog;
import com.redline.jj.domain.option.SiteOptionLogRepository;
import com.redline.jj.domain.option.SiteOptionRepository;
import com.redline.jj.domain.site.Site;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SiteOptionServiceTest {

    @Mock
    private SiteOptionRepository siteOptionRepository;

    @Mock
    private SiteOptionLogRepository siteOptionLogRepository;

    @InjectMocks
    private SiteOptionService siteOptionService;

    private SiteOption buildSiteOption(Long id) {
        Brand brand = Brand.builder().brandName("Levi's").brandNameKo("리바이스").build();
        Model model = Model.builder().id(1L).brand(brand).modelName("501").modelType(ModelType.DENIM_PANTS).build();
        Site site = Site.builder().id(1L).siteName("모드만").siteLink("https://modeman.co.kr").build();
        return SiteOption.builder()
            .id(id)
            .site(site)
            .model(model)
            .optionLabel("30인치")
            .url("https://modeman.co.kr/501")
            .inStock(true)
            .price(89000)
            .lastCapturedAt(LocalDateTime.now())
            .build();
    }

    @Test
    void listSiteOptions_siteId만_필터링() {
        given(siteOptionRepository.searchWithCursor(eq(1L), isNull(), isNull(), isNull(), any(Pageable.class)))
            .willReturn(List.of(buildSiteOption(1L)));

        CursorPage<SiteOptionResponse> result = siteOptionService.listSiteOptions(1L, null, null, null, 20);

        assertThat(result.getContent()).hasSize(1);
        verify(siteOptionRepository).searchWithCursor(eq(1L), isNull(), isNull(), isNull(), any(Pageable.class));
    }

    @Test
    void listSiteOptions_modelId만_필터링() {
        given(siteOptionRepository.searchWithCursor(isNull(), eq(2L), isNull(), isNull(), any(Pageable.class)))
            .willReturn(List.of(buildSiteOption(1L)));

        CursorPage<SiteOptionResponse> result = siteOptionService.listSiteOptions(null, 2L, null, null, 20);

        assertThat(result.getContent()).hasSize(1);
        verify(siteOptionRepository).searchWithCursor(isNull(), eq(2L), isNull(), isNull(), any(Pageable.class));
    }

    @Test
    void listSiteOptions_status만_필터링() {
        given(siteOptionRepository.searchWithCursor(isNull(), isNull(), eq(true), isNull(), any(Pageable.class)))
            .willReturn(List.of(buildSiteOption(1L)));

        CursorPage<SiteOptionResponse> result = siteOptionService.listSiteOptions(null, null, true, null, 20);

        assertThat(result.getContent()).hasSize(1);
        verify(siteOptionRepository).searchWithCursor(isNull(), isNull(), eq(true), isNull(), any(Pageable.class));
    }

    @Test
    void listSiteOptions_전체필터_조합() {
        given(siteOptionRepository.searchWithCursor(eq(1L), eq(2L), eq(true), isNull(), any(Pageable.class)))
            .willReturn(List.of(buildSiteOption(1L)));

        CursorPage<SiteOptionResponse> result = siteOptionService.listSiteOptions(1L, 2L, true, null, 20);

        assertThat(result.getContent()).hasSize(1);
        verify(siteOptionRepository).searchWithCursor(eq(1L), eq(2L), eq(true), isNull(), any(Pageable.class));
    }

    @Test
    void getSiteOption_존재하면_응답반환() {
        given(siteOptionRepository.findById(1L)).willReturn(Optional.of(buildSiteOption(1L)));

        SiteOptionResponse result = siteOptionService.getSiteOption(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getOptionLabel()).isEqualTo("30인치");
        assertThat(result.isStatus()).isTrue();
    }

    @Test
    void getSiteOption_없는id_SITE_OPTION_NOT_FOUND_예외() {
        given(siteOptionRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> siteOptionService.getSiteOption(999L))
            .isInstanceOf(BusinessException.class)
            .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.SITE_OPTION_NOT_FOUND));
    }

    @Test
    void getLogs_없는id_SITE_OPTION_NOT_FOUND_예외() {
        given(siteOptionRepository.existsById(999L)).willReturn(false);

        assertThatThrownBy(() -> siteOptionService.getLogs(999L, null, 20))
            .isInstanceOf(BusinessException.class)
            .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.SITE_OPTION_NOT_FOUND));
    }

    @Test
    void getLogs_존재하고_로그없으면_빈페이지반환() {
        given(siteOptionRepository.existsById(1L)).willReturn(true);
        given(siteOptionLogRepository.findBySiteOptionWithCursor(eq(1L), isNull(), any(Pageable.class)))
            .willReturn(List.of());

        CursorPage<SiteOptionLogResponse> result = siteOptionService.getLogs(1L, null, 20);

        assertThat(result.getContent()).isEmpty();
        verify(siteOptionRepository).existsById(1L);
    }

    @Test
    void getLogs_로그있으면_응답반환() {
        SiteOption siteOption = buildSiteOption(1L);
        SiteOptionLog log = SiteOptionLog.builder()
            .id(10L)
            .siteOption(siteOption)
            .capturedAt(LocalDateTime.now())
            .optionLabel("30인치")
            .price(89000)
            .inStock(true)
            .build();

        given(siteOptionRepository.existsById(1L)).willReturn(true);
        given(siteOptionLogRepository.findBySiteOptionWithCursor(eq(1L), isNull(), any(Pageable.class)))
            .willReturn(List.of(log));

        CursorPage<SiteOptionLogResponse> result = siteOptionService.getLogs(1L, null, 20);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getId()).isEqualTo(10L);
        assertThat(result.getContent().get(0).getPrice()).isEqualTo(89000);
    }
}
