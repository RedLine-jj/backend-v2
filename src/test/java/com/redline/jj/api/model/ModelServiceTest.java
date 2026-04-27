package com.redline.jj.api.model;

import com.redline.jj.api.model.dto.ModelDetailResponse;
import com.redline.jj.api.model.dto.ModelPageResponse;
import com.redline.jj.common.exception.BusinessException;
import com.redline.jj.common.exception.ErrorCode;
import com.redline.jj.domain.brand.Brand;
import com.redline.jj.domain.model.Model;
import com.redline.jj.domain.model.Model.ModelType;
import com.redline.jj.domain.model.ModelRepository;
import com.redline.jj.domain.option.SiteOptionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
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

    @Test
    void listModels_필터없음_cursor없음_전체조회() {
        given(modelRepository.findAllByOrderByIdDesc(any(PageRequest.class)))
            .willReturn(List.of(buildModel(2L, "M2"), buildModel(1L, "M1")));

        ModelPageResponse result = modelService.listModels(null, null, null, 20);

        assertThat(result.getItems()).hasSize(2);
        assertThat(result.isHasNext()).isFalse();
        assertThat(result.getNextCursor()).isNull();
        verify(modelRepository).findAllByOrderByIdDesc(any(PageRequest.class));
        verify(modelRepository, never()).findByIdLessThanOrderByIdDesc(any(), any());
    }

    @Test
    void listModels_브랜드필터_cursor없음() {
        given(modelRepository.findByBrand_IdInOrderByIdDesc(anyList(), any(PageRequest.class)))
            .willReturn(List.of(buildModel(1L, "M1")));

        ModelPageResponse result = modelService.listModels(List.of(1L), null, null, 20);

        assertThat(result.getItems()).hasSize(1);
        verify(modelRepository).findByBrand_IdInOrderByIdDesc(eq(List.of(1L)), any(PageRequest.class));
        verify(modelRepository, never()).findAllByOrderByIdDesc(any());
    }

    @Test
    void listModels_타입필터_cursor없음() {
        given(modelRepository.findByModelTypeInOrderByIdDesc(anyList(), any(PageRequest.class)))
            .willReturn(List.of(buildModel(1L, "M1")));

        ModelPageResponse result = modelService.listModels(null, List.of(ModelType.DENIM_PANTS), null, 20);

        assertThat(result.getItems()).hasSize(1);
        verify(modelRepository).findByModelTypeInOrderByIdDesc(eq(List.of(ModelType.DENIM_PANTS)), any(PageRequest.class));
    }

    @Test
    void listModels_cursor있음_필터없음_cursor이후반환() {
        given(modelRepository.findByIdLessThanOrderByIdDesc(eq(5L), any(PageRequest.class)))
            .willReturn(List.of(buildModel(4L, "M4"), buildModel(3L, "M3")));

        ModelPageResponse result = modelService.listModels(null, null, 5L, 20);

        assertThat(result.getItems()).hasSize(2);
        verify(modelRepository).findByIdLessThanOrderByIdDesc(eq(5L), any(PageRequest.class));
    }

    @Test
    void listModels_cursor가_마지막이면_빈리스트() {
        given(modelRepository.findByIdLessThanOrderByIdDesc(any(), any(PageRequest.class)))
            .willReturn(List.of());

        ModelPageResponse result = modelService.listModels(null, null, 1L, 20);

        assertThat(result.getItems()).isEmpty();
        assertThat(result.isHasNext()).isFalse();
        assertThat(result.getNextCursor()).isNull();
    }

    @Test
    void listModels_결과가_size초과이면_hasNext_true() {
        List<Model> models = new ArrayList<>();
        for (long i = 5; i >= 1; i--) {
            models.add(buildModel(i, "M" + i));
        }
        given(modelRepository.findAllByOrderByIdDesc(any(PageRequest.class))).willReturn(models);

        ModelPageResponse result = modelService.listModels(null, null, null, 4);

        assertThat(result.isHasNext()).isTrue();
        assertThat(result.getItems()).hasSize(4);
        assertThat(result.getNextCursor()).isEqualTo(2L);
    }

    @Test
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
    void getModel_없는id_MODEL_NOT_FOUND_예외() {
        given(modelRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> modelService.getModel(999L))
            .isInstanceOf(BusinessException.class)
            .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.MODEL_NOT_FOUND));

        verify(siteOptionRepository, never()).findByModel_IdOrderByIdAsc(any());
    }
}
