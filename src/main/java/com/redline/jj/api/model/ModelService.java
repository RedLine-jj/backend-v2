package com.redline.jj.api.model;

import com.redline.jj.api.model.dto.ModelDetailResponse;
import com.redline.jj.api.model.dto.ModelPageResponse;
import com.redline.jj.api.model.dto.ModelResponse;
import com.redline.jj.common.exception.BusinessException;
import com.redline.jj.common.exception.ErrorCode;
import com.redline.jj.domain.model.Model;
import com.redline.jj.domain.model.Model.ModelType;
import com.redline.jj.domain.model.ModelRepository;
import com.redline.jj.domain.option.SiteOption;
import com.redline.jj.domain.option.SiteOptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ModelService {

    private final ModelRepository modelRepository;
    private final SiteOptionRepository siteOptionRepository;

    @Transactional(readOnly = true)
    public ModelPageResponse listModels(List<Long> brandIds, List<ModelType> types,
                                        Long cursor, int size) {
        PageRequest pageable = PageRequest.of(0, size + 1);
        boolean hasBrandFilter = brandIds != null && !brandIds.isEmpty();
        boolean hasTypeFilter = types != null && !types.isEmpty();

        List<Model> fetched;
        if (!hasBrandFilter && !hasTypeFilter && cursor == null) {
            fetched = modelRepository.findAllByOrderByIdDesc(pageable);
        } else if (!hasBrandFilter && !hasTypeFilter) {
            fetched = modelRepository.findByIdLessThanOrderByIdDesc(cursor, pageable);
        } else if (hasBrandFilter && !hasTypeFilter && cursor == null) {
            fetched = modelRepository.findByBrand_IdInOrderByIdDesc(brandIds, pageable);
        } else if (hasBrandFilter && !hasTypeFilter) {
            fetched = modelRepository.findByIdLessThanAndBrand_IdInOrderByIdDesc(cursor, brandIds, pageable);
        } else if (!hasBrandFilter && cursor == null) {
            fetched = modelRepository.findByModelTypeInOrderByIdDesc(types, pageable);
        } else if (!hasBrandFilter) {
            fetched = modelRepository.findByIdLessThanAndModelTypeInOrderByIdDesc(cursor, types, pageable);
        } else if (cursor == null) {
            fetched = modelRepository.findByBrand_IdInAndModelTypeInOrderByIdDesc(brandIds, types, pageable);
        } else {
            fetched = modelRepository.findByIdLessThanAndBrand_IdInAndModelTypeInOrderByIdDesc(
                cursor, brandIds, types, pageable);
        }

        boolean hasNext = fetched.size() > size;
        List<ModelResponse> items = fetched.stream()
            .limit(size)
            .map(ModelResponse::from)
            .toList();
        Long nextCursor = hasNext ? items.get(items.size() - 1).getId() : null;

        return ModelPageResponse.of(items, nextCursor, hasNext);
    }

    @Transactional(readOnly = true)
    public ModelDetailResponse getModel(Long id) {
        Model model = modelRepository.findById(id)
            .orElseThrow(() -> new BusinessException(ErrorCode.MODEL_NOT_FOUND));
        List<SiteOption> options = siteOptionRepository.findByModel_IdOrderByIdAsc(id);
        return ModelDetailResponse.from(model, options);
    }

    @Transactional(readOnly = true)
    public long countModels() {
        return modelRepository.count();
    }
}
