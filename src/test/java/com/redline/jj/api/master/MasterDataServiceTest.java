package com.redline.jj.api.master;

import com.redline.jj.api.master.dto.BrandResponse;
import com.redline.jj.api.master.dto.ModelTypeResponse;
import com.redline.jj.api.master.dto.SiteResponse;
import com.redline.jj.domain.brand.Brand;
import com.redline.jj.domain.brand.BrandRepository;
import com.redline.jj.domain.model.Model.ModelType;
import com.redline.jj.domain.site.Site;
import com.redline.jj.domain.site.SiteRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MasterDataServiceTest {

    @Mock
    private BrandRepository brandRepository;

    @Mock
    private SiteRepository siteRepository;

    @InjectMocks
    private MasterDataService masterDataService;

    @Test
    void getBrands_첫_호출시_Repository를_1회_조회한다() {
        given(brandRepository.findAll()).willReturn(List.of(
                Brand.builder().brandName("Levi's").brandNameKo("리바이스").build()
        ));

        List<BrandResponse> result = masterDataService.getBrands();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getBrandName()).isEqualTo("Levi's");
        assertThat(result.get(0).getBrandNameKo()).isEqualTo("리바이스");
        verify(brandRepository, times(1)).findAll();
    }

    @Test
    void getBrands_캐시_미적용_시_호출마다_Repository를_조회한다() {
        // @Cacheable은 스프링 AOP 프록시 없이 동작하지 않으므로,
        // 캐시 hit 검증은 MasterDataCacheIntegrationTest에서 수행한다.
        given(brandRepository.findAll()).willReturn(List.of());

        masterDataService.getBrands();
        masterDataService.getBrands();

        verify(brandRepository, times(2)).findAll();
    }

    @Test
    void getBrands_엔티티를_BrandResponse로_변환한다() {
        Brand brand1 = Brand.builder().brandName("Diesel").brandNameKo("디젤").build();
        Brand brand2 = Brand.builder().brandName("Edwin").brandNameKo(null).build();
        given(brandRepository.findAll()).willReturn(List.of(brand1, brand2));

        List<BrandResponse> result = masterDataService.getBrands();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getBrandName()).isEqualTo("Diesel");
        assertThat(result.get(1).getBrandNameKo()).isNull();
    }

    @Test
    void getSites_첫_호출시_Repository를_1회_조회한다() {
        given(siteRepository.findAll()).willReturn(List.of(
                Site.builder().siteName("모드만").siteLink("https://modeman.co.kr").build()
        ));

        List<SiteResponse> result = masterDataService.getSites();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSiteName()).isEqualTo("모드만");
        assertThat(result.get(0).getSiteLink()).isEqualTo("https://modeman.co.kr");
        verify(siteRepository, times(1)).findAll();
    }

    @Test
    void getSites_캐시_미적용_시_호출마다_Repository를_조회한다() {
        given(siteRepository.findAll()).willReturn(List.of());

        masterDataService.getSites();
        masterDataService.getSites();

        verify(siteRepository, times(2)).findAll();
    }

    @Test
    void getModelTypes_ModelType_enum_전체를_반환한다() {
        List<ModelTypeResponse> types = masterDataService.getModelTypes();

        assertThat(types).hasSize(ModelType.values().length);
        assertThat(types).extracting(ModelTypeResponse::getCode)
                .containsExactlyInAnyOrder(
                        ModelType.DENIM_PANTS.name(),
                        ModelType.DENIM_JACKET.name()
                );
    }

    @Test
    void getModelTypes_code와_label이_올바르게_매핑된다() {
        List<ModelTypeResponse> types = masterDataService.getModelTypes();

        assertThat(types).extracting(ModelTypeResponse::getLabel)
                .containsExactlyInAnyOrder("데님 팬츠", "데님 재킷");
        assertThat(types).allMatch(t -> t.getCode().equals(t.getCode().toUpperCase()));
    }
}
