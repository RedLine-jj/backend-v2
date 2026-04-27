package com.redline.jj.api.master;

import com.redline.jj.api.master.dto.BrandResponse;
import com.redline.jj.api.master.dto.SiteResponse;
import com.redline.jj.domain.brand.BrandRepository;
import com.redline.jj.domain.model.Model.ModelType;
import com.redline.jj.domain.site.SiteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MasterDataService {

    private final BrandRepository brandRepository;
    private final SiteRepository siteRepository;

    @Cacheable("brands")
    @Transactional(readOnly = true)
    public List<BrandResponse> getBrands() {
        return brandRepository.findAll().stream()
                .map(BrandResponse::from)
                .toList();
    }

    @CacheEvict("brands")
    public void evictBrandsCache() {}

    @Cacheable("sites")
    @Transactional(readOnly = true)
    public List<SiteResponse> getSites() {
        return siteRepository.findAll().stream()
                .map(SiteResponse::from)
                .toList();
    }

    @CacheEvict("sites")
    public void evictSitesCache() {}

    public List<String> getModelTypes() {
        return Arrays.stream(ModelType.values())
                .map(Enum::name)
                .toList();
    }
}
