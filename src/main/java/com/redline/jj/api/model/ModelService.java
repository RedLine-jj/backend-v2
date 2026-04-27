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
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ModelService {

    private final ModelRepository modelRepository;
    private final SiteOptionRepository siteOptionRepository;

    @Transactional(readOnly = true)
    public ModelPageResponse listModels(List<Long> brandIds, List<ModelType> types,
                                        Long cursor, int size) {
        PageRequest pageable = PageRequest.of(0, size + 1, Sort.by(Sort.Direction.DESC, "id"));
        List<Model> fetched = modelRepository.findAll(buildSpec(brandIds, types, cursor), pageable)
            .getContent();

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

    private static Specification<Model> buildSpec(List<Long> brandIds, List<ModelType> types, Long cursor) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (cursor != null) {
                predicates.add(cb.lessThan(root.get("id"), cursor));
            }
            if (brandIds != null && !brandIds.isEmpty()) {
                predicates.add(root.get("brand").get("id").in(brandIds));
            }
            if (types != null && !types.isEmpty()) {
                predicates.add(root.get("modelType").in(types));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
