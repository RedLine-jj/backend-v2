package com.redline.jj.domain.model;

import com.redline.jj.config.JpaConfig;
import com.redline.jj.domain.brand.Brand;
import com.redline.jj.domain.brand.BrandRepository;
import com.redline.jj.domain.model.Model.ModelType;
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

    private Model model;

    @BeforeEach
    void setUp() {
        Brand brand = brandRepository.save(Brand.builder().brandName("리바이스").build());
        model = modelRepository.save(Model.builder()
            .brand(brand)
            .modelName("501")
            .modelType(ModelType.DENIM_PANTS)
            .build());
    }

    @Test
    void aliasName으로_조회할_수_있다() {
        modelAliasRepository.save(ModelAlias.builder()
            .model(model)
            .aliasName("Levi's 501 Original")
            .build());

        Optional<ModelAlias> found = modelAliasRepository.findByAliasName("Levi's 501 Original");

        assertThat(found).isPresent();
        assertThat(found.get().getAliasName()).isEqualTo("Levi's 501 Original");
    }

    @Test
    void 존재하지_않는_aliasName은_empty를_반환한다() {
        Optional<ModelAlias> found = modelAliasRepository.findByAliasName("없는상품");

        assertThat(found).isEmpty();
    }

    @Test
    void 동일_aliasName_저장시_예외가_발생한다() {
        modelAliasRepository.saveAndFlush(ModelAlias.builder()
            .model(model).aliasName("같은상품명").build());

        assertThatThrownBy(() -> modelAliasRepository.saveAndFlush(
            ModelAlias.builder().model(model).aliasName("같은상품명").build()))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void 다른_model이라도_동일_aliasName_저장시_예외가_발생한다() {
        Brand otherBrand = brandRepository.save(Brand.builder().brandName("디젤").build());
        Model otherModel = modelRepository.save(Model.builder()
            .brand(otherBrand)
            .modelName("D-staq")
            .modelType(ModelType.DENIM_PANTS)
            .build());

        modelAliasRepository.saveAndFlush(ModelAlias.builder()
            .model(model).aliasName("공유상품명").build());

        assertThatThrownBy(() -> modelAliasRepository.saveAndFlush(
            ModelAlias.builder().model(otherModel).aliasName("공유상품명").build()))
            .isInstanceOf(DataIntegrityViolationException.class);
    }
}
