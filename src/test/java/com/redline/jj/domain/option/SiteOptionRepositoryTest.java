package com.redline.jj.domain.option;

import com.redline.jj.config.JpaConfig;
import com.redline.jj.domain.brand.Brand;
import com.redline.jj.domain.brand.BrandRepository;
import com.redline.jj.domain.model.Model;
import com.redline.jj.domain.model.Model.ModelType;
import com.redline.jj.domain.model.ModelRepository;
import com.redline.jj.domain.site.Site;
import com.redline.jj.domain.site.SiteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import(JpaConfig.class)
@ActiveProfiles("test")
class SiteOptionRepositoryTest {

    @Autowired
    private SiteOptionRepository siteOptionRepository;
    @Autowired
    private SiteRepository siteRepository;
    @Autowired
    private ModelRepository modelRepository;
    @Autowired
    private BrandRepository brandRepository;

    private Site site;
    private Model model;

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
            .siteLink("https://musinsa.com")
            .build());
    }

    @Test
    void 모든_필수_필드를_채우면_정상_저장된다() {
        LocalDateTime capturedAt = LocalDateTime.now();
        SiteOption option = SiteOption.builder()
            .site(site)
            .model(model)
            .optionLabel("S")
            .url("https://musinsa.com/products/501-S")
            .price(99000)
            .lastCapturedAt(capturedAt)
            .build();

        SiteOption saved = siteOptionRepository.saveAndFlush(option);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getSite().getId()).isEqualTo(site.getId());
        assertThat(saved.getModel().getId()).isEqualTo(model.getId());
        assertThat(saved.getOptionLabel()).isEqualTo("S");
        assertThat(saved.getUrl()).isEqualTo("https://musinsa.com/products/501-S");
        assertThat(saved.getPrice()).isEqualTo(99000);
        assertThat(saved.isInStock()).isFalse();
        assertThat(saved.getLastCapturedAt()).isEqualTo(capturedAt);
    }

    @Test
    void url_누락시_DataIntegrityViolationException이_발생한다() {
        SiteOption option = SiteOption.builder()
            .site(site)
            .model(model)
            .optionLabel("M")
            .price(99000)
            .lastCapturedAt(LocalDateTime.now())
            .build();

        assertThatThrownBy(() -> siteOptionRepository.saveAndFlush(option))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void lastCapturedAt_누락시_DataIntegrityViolationException이_발생한다() {
        SiteOption option = SiteOption.builder()
            .site(site)
            .model(model)
            .optionLabel("L")
            .url("https://musinsa.com/products/501-L")
            .price(99000)
            .build();

        assertThatThrownBy(() -> siteOptionRepository.saveAndFlush(option))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void optionLabel_누락시_DataIntegrityViolationException이_발생한다() {
        SiteOption option = SiteOption.builder()
            .site(site)
            .model(model)
            .url("https://musinsa.com/products/501-XL")
            .price(99000)
            .lastCapturedAt(LocalDateTime.now())
            .build();

        assertThatThrownBy(() -> siteOptionRepository.saveAndFlush(option))
            .isInstanceOf(DataIntegrityViolationException.class);
    }
}
