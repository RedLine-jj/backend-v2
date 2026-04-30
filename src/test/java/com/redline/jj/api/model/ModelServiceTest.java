package com.redline.jj.api.model;

import com.redline.jj.api.model.dto.ModelDetailResponse;
import com.redline.jj.common.exception.BusinessException;
import com.redline.jj.common.exception.ErrorCode;
import com.redline.jj.common.response.CursorPage;
import com.redline.jj.domain.brand.Brand;
import com.redline.jj.domain.model.Model;
import com.redline.jj.domain.model.Model.ModelType;
import com.redline.jj.domain.model.ModelRepository;
import com.redline.jj.domain.option.SiteOptionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ModelServiceTest {

    @Mock
    private ModelRepository modelRepository;

    @Mock
    private SiteOptionRepository siteOptionRepository;

    @InjectMocks
    private ModelService modelService;

    private Model buildModel(Long id, String name) {
        Brand brand = Brand.builder().brandName("Levi's").brandNameKo("리바이스").build();
        return Model.builder()
            .id(id)
            .brand(brand)
            .modelName(name)
            .modelType(ModelType.DENIM_PANTS)
            .build();
    }

    @SuppressWarnings("unchecked")
    private void givenModels(List<Model> models) {
        given(modelRepository.findAll(any(Specification.class), any(PageRequest.class)))
            .willReturn(new PageImpl<>(models));
    }

    @Test
    @DisplayName("결과가 없으면 빈 CursorPage를 반환한다")
    void listModels_결과없으면_빈페이지반환() {
        givenModels(List.of());

        CursorPage<?> result = modelService.listModels(null, null, null, 20);

        assertThat(result.getContent()).isEmpty();
        assertThat(result.isHasNext()).isFalse();
        assertThat(result.getNextCursor()).isNull();
    }

    @Test
    @DisplayName("결과가 size 이하이면 hasNext가 false이다")
    void listModels_size이하이면_hasNext_false() {
        List<Model> models = List.of(buildModel(2L, "M2"), buildModel(1L, "M1"));
        givenModels(models);
        given(siteOptionRepository.findLowestPricesByModelIds(List.of(2L, 1L))).willReturn(List.of());

        CursorPage<?> result = modelService.listModels(null, null, null, 20);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.isHasNext()).isFalse();
        assertThat(result.getNextCursor()).isNull();
    }

    @Test
    @DisplayName("결과가 size 초과이면 hasNext가 true이고 nextCursor가 마지막 아이템 id이다")
    void listModels_결과가_size초과이면_hasNext_true_nextCursor설정() {
        List<Model> models = new ArrayList<>();
        for (long i = 5; i >= 1; i--) {
            models.add(buildModel(i, "M" + i));
        }
        givenModels(models);
        given(siteOptionRepository.findLowestPricesByModelIds(List.of(5L, 4L, 3L, 2L)))
            .willReturn(List.of());

        CursorPage<?> result = modelService.listModels(null, null, null, 4);

        assertThat(result.isHasNext()).isTrue();
        assertThat(result.getContent()).hasSize(4);
        assertThat(result.getNextCursor()).isEqualTo(2L);
    }

    @Test
    @DisplayName("브랜드 필터 전달 시 findAll이 호출된다")
    void listModels_브랜드필터_전달시_findAll_호출() {
        givenModels(List.of(buildModel(1L, "M1")));
        given(siteOptionRepository.findLowestPricesByModelIds(List.of(1L))).willReturn(List.of());

        CursorPage<?> result = modelService.listModels(List.of(1L), null, null, 20);

        assertThat(result.getContent()).hasSize(1);
        verify(modelRepository).findAll(any(Specification.class), any(PageRequest.class));
    }

    @Test
    @DisplayName("타입 필터 전달 시 findAll이 호출된다")
    void listModels_타입필터_전달시_findAll_호출() {
        givenModels(List.of(buildModel(1L, "M1")));
        given(siteOptionRepository.findLowestPricesByModelIds(List.of(1L))).willReturn(List.of());

        CursorPage<?> result = modelService.listModels(null, List.of(ModelType.DENIM_PANTS), null, 20);

        assertThat(result.getContent()).hasSize(1);
        verify(modelRepository).findAll(any(Specification.class), any(PageRequest.class));
    }

    @Test
    @DisplayName("cursor 전달 시 size+1 pageable이 사용된다")
    void listModels_cursor_전달시_size플러스1_pageable_사용() {
        givenModels(List.of());

        modelService.listModels(null, null, 5L, 10);

        verify(modelRepository).findAll(
            any(Specification.class),
            org.mockito.ArgumentMatchers.eq(PageRequest.of(0, 11, Sort.by(Sort.Direction.DESC, "id")))
        );
    }

    @Test
    @DisplayName("모델이 존재하면 siteOptions를 포함하여 반환한다")
    void getModel_존재하면_siteOptions포함반환() {
        Model model = buildModel(1L, "501");
        given(modelRepository.findById(1L)).willReturn(Optional.of(model));
        given(siteOptionRepository.findByModel_IdOrderByIdAsc(1L)).willReturn(List.of());

        ModelDetailResponse result = modelService.getModel(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getModelName()).isEqualTo("501");
        assertThat(result.getSiteOptions()).isEmpty();
        verify(siteOptionRepository).findByModel_IdOrderByIdAsc(1L);
    }

    @Test
    @DisplayName("존재하지 않는 id로 조회 시 MODEL_NOT_FOUND 예외가 발생한다")
    void getModel_없는id_MODEL_NOT_FOUND_예외() {
        given(modelRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> modelService.getModel(999L))
            .isInstanceOf(BusinessException.class)
            .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.MODEL_NOT_FOUND));

        verify(siteOptionRepository, never()).findByModel_IdOrderByIdAsc(any());
    }
}
