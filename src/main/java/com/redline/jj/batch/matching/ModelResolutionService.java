package com.redline.jj.batch.matching;

import com.redline.jj.batch.crawler.dto.CrawledProduct;
import com.redline.jj.domain.brand.Brand;
import com.redline.jj.domain.brand.BrandAliasRepository;
import com.redline.jj.domain.brand.BrandRepository;
import com.redline.jj.domain.model.Model;
import com.redline.jj.domain.model.ModelAlias;
import com.redline.jj.domain.model.ModelAliasRepository;
import com.redline.jj.domain.model.ModelRepository;
import com.redline.jj.common.exception.BusinessException;
import com.redline.jj.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ModelResolutionService {

    private final ModelRepository modelRepository;
    private final ModelAliasRepository modelAliasRepository;
    private final BrandAliasRepository brandAliasRepository;
    private final BrandRepository brandRepository;
    private final LlmMatchClient llmMatchClient;

    @Transactional
    public Model resolve(CrawledProduct product) {
        String normalizedBrand = brandAliasRepository.findByAliasName(product.brandName())
                .map(alias -> alias.getBrand().getBrandName())
                .orElse(product.brandName());

        Optional<Model> exactMatch = modelRepository.findByBrand_BrandNameAndModelName(
                normalizedBrand, product.modelName());
        if (exactMatch.isPresent()) {
            Model model = exactMatch.get();
            model.updateImageUrlIfAbsent(product.imageUrl());
            model.updateModelTypeIfAbsent(product.modelType());
            return model;
        }

        Optional<ModelAlias> aliasMatch = modelAliasRepository.findByAliasName(product.siteModelName());
        if (aliasMatch.isPresent()) {
            Model model = aliasMatch.get().getModel();
            model.updateImageUrlIfAbsent(product.imageUrl());
            model.updateModelTypeIfAbsent(product.modelType());
            return model;
        }

        return resolveByLlm(product, normalizedBrand);
    }

    private Model resolveByLlm(CrawledProduct product, String normalizedBrand) {
        Optional<LlmMatchResult> llmResult = matchWithFallback(product, normalizedBrand);

        if (llmResult.isEmpty()) {
            log.warn("LLM 매칭 실패(신뢰도 미달), 원시 문자열로 신규 Brand/Model 생성: brand={}, model={}",
                    normalizedBrand, product.modelName());
        } else {
            LlmMatchResult result = llmResult.get();
            Optional<Model> matched = modelRepository.findByBrand_BrandNameAndModelName(
                    result.brandName(), result.modelName());

            if (matched.isPresent()) {
                Model model = matched.get();
                model.updateImageUrlIfAbsent(product.imageUrl());
                model.updateModelTypeIfAbsent(product.modelType());
                saveAliasIfAbsent(product.siteModelName(), model);
                return model;
            }

            log.warn("LLM 매칭 실패(DB 미매칭), 원시 문자열로 신규 Brand/Model 생성: brand={}, model={}, llmBrand={}, llmModel={}, confidence={}",
                    normalizedBrand, product.modelName(), result.brandName(), result.modelName(), result.confidence());
        }

        Brand brand = findOrCreateBrand(normalizedBrand);
        Model saved = modelRepository.save(Model.builder()
                .brand(brand)
                .modelName(product.modelName())
                .imageUrl(product.imageUrl())
                .modelType(product.modelType())
                .build());
        saveAliasIfAbsent(product.siteModelName(), saved);
        log.warn("신규 Model 자동 생성: brand={}, model={}", brand.getBrandName(), saved.getModelName());
        return saved;
    }

    private Optional<LlmMatchResult> matchWithFallback(CrawledProduct product, String normalizedBrand) {
        try {
            return llmMatchClient.match(product);
        } catch (BusinessException e) {
            if (e.getErrorCode() != ErrorCode.LLM_MATCHING_FAILED) {
                throw e;
            }

            log.warn("LLM 매칭 실패(API/응답 오류), 원시 문자열로 신규 Brand/Model 생성: brand={}, model={}",
                    normalizedBrand, product.modelName(), e);
            return Optional.empty();
        }
    }

    private Brand findOrCreateBrand(String brandName) {
        return brandRepository.findByBrandName(brandName)
                .orElseGet(() -> {
                    log.warn("신규 Brand 자동 생성: brand={}", brandName);
                    return brandRepository.save(Brand.builder().brandName(brandName).build());
                });
    }

    private void saveAliasIfAbsent(String siteModelName, Model model) {
        if (modelAliasRepository.findByAliasName(siteModelName).isEmpty()) {
            modelAliasRepository.save(ModelAlias.builder()
                    .model(model)
                    .aliasName(siteModelName)
                    .build());
        }
    }
}
