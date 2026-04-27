package com.redline.jj.domain.model;

import com.redline.jj.config.JpaConfig;
import com.redline.jj.domain.brand.Brand;
import com.redline.jj.domain.brand.BrandRepository;
import com.redline.jj.domain.model.Model.ModelType;
import com.redline.jj.domain.site.Site;
import com.redline.jj.domain.site.Site.Platform;
import com.redline.jj.domain.site.SiteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import(JpaConfig.class)
@ActiveProfiles("test")
class ModelAliasRepositoryTest {

    @Autowired
    private ModelAliasRepository modelAliasRepository;
    @Autowired
    private ModelRepository modelRepository;
    @Autowired
    private BrandRepository brandRepository;
    @Autowired
    private SiteRepository siteRepository;

    private Model model;
    private Site site;

    @BeforeEach
    void setUp() {
        Brand brand = brandRepository.save(Brand.builder().brandName("리바이스").build());
        model = modelRepository.save(Model.builder()
            .brand(brand)
            .modelName("501")
            .modelType(ModelType.DENIM_PANTS)
            .build());
        site = siteRepository.save(Site.builder()
            .siteName("무신사")
            .platform(Platform.CAFE24)
            .build());
    }

    @Test
    void siteIdx와_siteModelName으로_조회할_수_있다() {
        ModelAlias alias = ModelAlias.builder()
            .model(model)
            .site(site)
            .siteModelName("Levi's 501 Original")
            .confidence(90)
            .build();
        modelAliasRepository.save(alias);

        Optional<ModelAlias> found = modelAliasRepository
            .findBySite_IdAndSiteModelName(site.getId(), "Levi's 501 Original");

        assertThat(found).isPresent();
        assertThat(found.get().getConfidence()).isEqualTo(90);
    }

    @Test
    void 존재하지_않는_siteModelName은_empty를_반환한다() {
        Optional<ModelAlias> found = modelAliasRepository
            .findBySite_IdAndSiteModelName(site.getId(), "없는상품");

        assertThat(found).isEmpty();
    }

    @Test
    void 동일_site에_동일_siteModelName_저장시_예외가_발생한다() {
        ModelAlias alias1 = ModelAlias.builder()
            .model(model).site(site).siteModelName("같은상품명").confidence(90).build();
        modelAliasRepository.saveAndFlush(alias1);

        ModelAlias alias2 = ModelAlias.builder()
            .model(model).site(site).siteModelName("같은상품명").confidence(85).build();

        assertThatThrownBy(() -> modelAliasRepository.saveAndFlush(alias2))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void 다른_site의_동일_siteModelName은_별도_엔티티로_저장된다() {
        Site site2 = siteRepository.save(Site.builder()
            .siteName("네이버쇼핑")
            .platform(Platform.IMWEB)
            .build());

        ModelAlias alias1 = ModelAlias.builder()
            .model(model).site(site).siteModelName("동일상품명").confidence(90).build();
        ModelAlias alias2 = ModelAlias.builder()
            .model(model).site(site2).siteModelName("동일상품명").confidence(88).build();

        modelAliasRepository.saveAndFlush(alias1);
        modelAliasRepository.saveAndFlush(alias2);

        assertThat(modelAliasRepository.count()).isEqualTo(2);
    }
}
