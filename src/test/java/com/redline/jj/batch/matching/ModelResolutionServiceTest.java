package com.redline.jj.batch.matching;

import com.redline.jj.batch.crawler.dto.CrawledProduct;
import com.redline.jj.domain.brand.Brand;
import com.redline.jj.domain.brand.BrandAlias;
import com.redline.jj.domain.brand.BrandAliasRepository;
import com.redline.jj.domain.brand.BrandRepository;
import com.redline.jj.domain.model.Model;
import com.redline.jj.domain.model.ModelAlias;
import com.redline.jj.domain.model.ModelAliasRepository;
import com.redline.jj.domain.model.ModelRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class ModelResolutionServiceTest {

    @Mock
    ModelRepository modelRepository;
    @Mock
    ModelAliasRepository modelAliasRepository;
    @Mock
    BrandAliasRepository brandAliasRepository;
    @Mock
    BrandRepository brandRepository;
    @Mock
    LlmMatchClient llmMatchClient;

    private ModelResolutionService service;

    @BeforeEach
    void setUp() {
        service = new ModelResolutionService(
                modelRepository, modelAliasRepository,
                brandAliasRepository, brandRepository,
                llmMatchClient
        );
    }

    // -----------------------------------------------------------------------
    // 기존 테스트
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Exact 매치 성공 시 Alias 및 LLM 미호출")
    void resolve_Exact매치성공_AliasAndLLM미호출() {
        Model model = buildModel(1L, "모드만", "101 슬림 데님");
        when(brandAliasRepository.findByAliasName(any())).thenReturn(Optional.empty());
        when(modelRepository.findByBrand_BrandNameAndModelName("모드만", "101 슬림 데님"))
                .thenReturn(Optional.of(model));

        Model result = service.resolve(buildProduct("모드만", "101 슬림 데님", "101 슬림 데님"));

        assertThat(result).isEqualTo(model);
        verify(modelAliasRepository, never()).findByAliasName(any());
        verify(llmMatchClient, never()).match(any());
    }

    @Test
    @DisplayName("Alias 캐시 히트 시 LLM 미호출")
    void resolve_AliasHit_LLM미호출() {
        Model model = buildModel(2L, "모드만", "101 슬림");
        ModelAlias alias = ModelAlias.builder().model(model).aliasName("모드만101").build();
        when(brandAliasRepository.findByAliasName(any())).thenReturn(Optional.empty());
        when(modelRepository.findByBrand_BrandNameAndModelName(any(), any())).thenReturn(Optional.empty());
        when(modelAliasRepository.findByAliasName("모드만101")).thenReturn(Optional.of(alias));

        Model result = service.resolve(buildProduct("모드만", "101 슬림 데님", "모드만101"));

        assertThat(result.getId()).isEqualTo(2L);
        verify(llmMatchClient, never()).match(any());
    }

    @Test
    @DisplayName("LLM 매칭 성공 시 ModelAlias 저장 후 매칭된 Model 반환")
    void resolve_LLM매칭성공_Alias저장후Model반환() {
        Model matched = buildModel(3L, "모드만", "501 데님");
        when(brandAliasRepository.findByAliasName(any())).thenReturn(Optional.empty());
        when(modelRepository.findByBrand_BrandNameAndModelName("모드만", "501 데님"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(matched));
        when(modelAliasRepository.findByAliasName(any())).thenReturn(Optional.empty());
        when(llmMatchClient.match(any()))
                .thenReturn(Optional.of(new LlmMatchResult("모드만", "501 데님", 90.0)));
        when(modelAliasRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Model result = service.resolve(buildProduct("모드만", "501 데님", "모드만501"));

        assertThat(result.getId()).isEqualTo(3L);
        verify(modelAliasRepository, times(1)).save(any(ModelAlias.class));
        verify(modelRepository, never()).save(any());
    }

    @Test
    @DisplayName("LLM 매칭 실패(empty 반환) 시 신규 Model 저장")
    void resolve_LLM매칭실패_신규Model저장() {
        when(brandAliasRepository.findByAliasName(any())).thenReturn(Optional.empty());
        when(modelRepository.findByBrand_BrandNameAndModelName(any(), any())).thenReturn(Optional.empty());
        when(modelAliasRepository.findByAliasName(any())).thenReturn(Optional.empty());
        when(llmMatchClient.match(any())).thenReturn(Optional.empty());
        Brand brand = Brand.builder().brandName("모드만").build();
        when(brandRepository.findByBrandName("모드만")).thenReturn(Optional.of(brand));
        Model newModel = buildModel(99L, "모드만", "신규모델");
        when(modelRepository.save(any())).thenReturn(newModel);

        Model result = service.resolve(buildProduct("모드만", "신규모델", "신규모델사이트명"));

        verify(modelRepository, times(1)).save(any(Model.class));
        assertThat(result.getId()).isEqualTo(99L);
    }

    @Test
    @DisplayName("브랜드 별칭 정규화 후 정규화된 브랜드명으로 매칭")
    void resolve_브랜드별칭정규화_정규화된브랜드로매칭() {
        Brand brand = Brand.builder().brandName("모드만").build();
        BrandAlias alias = BrandAlias.builder().brand(brand).aliasName("ModeMAN").build();
        Model model = buildModel(5L, "모드만", "101 슬림");
        when(brandAliasRepository.findByAliasName("ModeMAN")).thenReturn(Optional.of(alias));
        when(modelRepository.findByBrand_BrandNameAndModelName("모드만", "101 슬림"))
                .thenReturn(Optional.of(model));

        Model result = service.resolve(buildProduct("ModeMAN", "101 슬림", "101 슬림"));

        assertThat(result.getId()).isEqualTo(5L);
        verify(modelRepository).findByBrand_BrandNameAndModelName("모드만", "101 슬림");
    }

    // -----------------------------------------------------------------------
    // 1. 신규 Brand 생성 경로에서 warn 로그 발생 검증
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("LLM empty + DB에 없는 브랜드일 때 '신규 Brand 자동 생성' warn 로그 발생")
    void resolve_신규Brand생성경로_warnLog발생(CapturedOutput output) {
        when(brandAliasRepository.findByAliasName(any())).thenReturn(Optional.empty());
        when(modelRepository.findByBrand_BrandNameAndModelName(any(), any())).thenReturn(Optional.empty());
        when(modelAliasRepository.findByAliasName(any())).thenReturn(Optional.empty());
        when(llmMatchClient.match(any())).thenReturn(Optional.empty());
        Brand newBrand = Brand.builder().brandName("신규브랜드").build();
        when(brandRepository.findByBrandName("신규브랜드")).thenReturn(Optional.empty());
        when(brandRepository.save(any())).thenReturn(newBrand);
        Model newModel = buildModel(100L, "신규브랜드", "신규모델");
        when(modelRepository.save(any())).thenReturn(newModel);

        service.resolve(buildProduct("신규브랜드", "신규모델", "신규모델사이트명"));

        assertThat(output.getAll()).contains("신규 Brand 자동 생성");
        assertThat(output.getAll()).contains("신규브랜드");
    }

    @Test
    @DisplayName("LLM empty 반환 시 '신규 Model 자동 생성' warn 로그 발생")
    void resolve_LLMEmpty_신규Model생성_warnLog발생(CapturedOutput output) {
        when(brandAliasRepository.findByAliasName(any())).thenReturn(Optional.empty());
        when(modelRepository.findByBrand_BrandNameAndModelName(any(), any())).thenReturn(Optional.empty());
        when(modelAliasRepository.findByAliasName(any())).thenReturn(Optional.empty());
        when(llmMatchClient.match(any())).thenReturn(Optional.empty());
        Brand brand = Brand.builder().brandName("모드만").build();
        when(brandRepository.findByBrandName("모드만")).thenReturn(Optional.of(brand));
        Model newModel = buildModel(101L, "모드만", "미지모델");
        when(modelRepository.save(any())).thenReturn(newModel);

        service.resolve(buildProduct("모드만", "미지모델", "미지모델사이트명"));

        assertThat(output.getAll()).contains("신규 Model 자동 생성");
        assertThat(output.getAll()).contains("미지모델");
    }

    @Test
    @DisplayName("LLM이 결과를 반환했지만 DB에 해당 모델이 없을 때 warn 로그 발생 (LLM 매칭 실패 DB 미매칭)")
    void resolve_LLM결과있지만DB미매칭_warnLog발생(CapturedOutput output) {
        when(brandAliasRepository.findByAliasName(any())).thenReturn(Optional.empty());
        when(modelRepository.findByBrand_BrandNameAndModelName(any(), any())).thenReturn(Optional.empty());
        when(modelAliasRepository.findByAliasName(any())).thenReturn(Optional.empty());
        when(llmMatchClient.match(any()))
                .thenReturn(Optional.of(new LlmMatchResult("모드만", "존재하지않는모델", 88.0)));
        Brand brand = Brand.builder().brandName("모드만").build();
        when(brandRepository.findByBrandName("모드만")).thenReturn(Optional.of(brand));
        Model newModel = buildModel(102L, "모드만", "존재하지않는모델");
        when(modelRepository.save(any())).thenReturn(newModel);

        service.resolve(buildProduct("모드만", "존재하지않는모델", "모드만미지모델"));

        assertThat(output.getAll()).contains("LLM 매칭 실패(DB 미매칭)");
    }

    // -----------------------------------------------------------------------
    // 2. saveAliasIfAbsent — 동일 aliasName 중복 저장 방지
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("LLM 매칭 성공 후 이미 동일 aliasName이 있으면 ModelAlias 저장 미호출")
    void resolve_LLM매칭성공_이미존재하는Alias_저장미호출() {
        Model matched = buildModel(3L, "모드만", "501 데님");
        when(brandAliasRepository.findByAliasName(any())).thenReturn(Optional.empty());
        when(modelRepository.findByBrand_BrandNameAndModelName("모드만", "501 데님"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(matched));
        ModelAlias existingAlias = ModelAlias.builder().model(matched).aliasName("모드만501").build();
        when(modelAliasRepository.findByAliasName("모드만501"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(existingAlias));
        when(llmMatchClient.match(any()))
                .thenReturn(Optional.of(new LlmMatchResult("모드만", "501 데님", 92.0)));

        Model result = service.resolve(buildProduct("모드만", "501 데님", "모드만501"));

        assertThat(result.getId()).isEqualTo(3L);
        verify(modelAliasRepository, never()).save(any(ModelAlias.class));
    }

    @Test
    @DisplayName("LLM 매칭 성공 후 alias 없으면 올바른 aliasName으로 ModelAlias 저장")
    void resolve_LLM매칭성공_Alias없음_올바른aliasName으로저장() {
        Model matched = buildModel(3L, "모드만", "501 데님");
        when(brandAliasRepository.findByAliasName(any())).thenReturn(Optional.empty());
        when(modelRepository.findByBrand_BrandNameAndModelName("모드만", "501 데님"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(matched));
        when(modelAliasRepository.findByAliasName("모드만-501")).thenReturn(Optional.empty());
        when(llmMatchClient.match(any()))
                .thenReturn(Optional.of(new LlmMatchResult("모드만", "501 데님", 92.0)));
        ArgumentCaptor<ModelAlias> aliasCaptor = ArgumentCaptor.forClass(ModelAlias.class);
        when(modelAliasRepository.save(aliasCaptor.capture())).thenAnswer(i -> i.getArgument(0));

        service.resolve(buildProduct("모드만", "501 데님", "모드만-501"));

        assertThat(aliasCaptor.getValue().getAliasName()).isEqualTo("모드만-501");
        assertThat(aliasCaptor.getValue().getModel().getId()).isEqualTo(3L);
    }

    // -----------------------------------------------------------------------
    // 3. 신규 Brand가 없을 때 brandRepository.save() 호출 검증
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("LLM empty + DB에 없는 브랜드일 때 brandRepository.save() 1회 호출")
    void resolve_신규Brand_BrandRepositorySave1회호출() {
        when(brandAliasRepository.findByAliasName(any())).thenReturn(Optional.empty());
        when(modelRepository.findByBrand_BrandNameAndModelName(any(), any())).thenReturn(Optional.empty());
        when(modelAliasRepository.findByAliasName(any())).thenReturn(Optional.empty());
        when(llmMatchClient.match(any())).thenReturn(Optional.empty());
        when(brandRepository.findByBrandName("없는브랜드")).thenReturn(Optional.empty());
        Brand savedBrand = Brand.builder().brandName("없는브랜드").build();
        when(brandRepository.save(any())).thenReturn(savedBrand);
        Model newModel = buildModel(200L, "없는브랜드", "새모델");
        when(modelRepository.save(any())).thenReturn(newModel);

        service.resolve(buildProduct("없는브랜드", "새모델", "새모델사이트"));

        ArgumentCaptor<Brand> brandCaptor = ArgumentCaptor.forClass(Brand.class);
        verify(brandRepository, times(1)).save(brandCaptor.capture());
        assertThat(brandCaptor.getValue().getBrandName()).isEqualTo("없는브랜드");
    }

    @Test
    @DisplayName("LLM empty + DB에 브랜드 존재 시 brandRepository.save() 미호출")
    void resolve_기존Brand존재_BrandRepositorySave미호출() {
        when(brandAliasRepository.findByAliasName(any())).thenReturn(Optional.empty());
        when(modelRepository.findByBrand_BrandNameAndModelName(any(), any())).thenReturn(Optional.empty());
        when(modelAliasRepository.findByAliasName(any())).thenReturn(Optional.empty());
        when(llmMatchClient.match(any())).thenReturn(Optional.empty());
        Brand existingBrand = Brand.builder().brandName("모드만").build();
        when(brandRepository.findByBrandName("모드만")).thenReturn(Optional.of(existingBrand));
        Model newModel = buildModel(201L, "모드만", "새모델");
        when(modelRepository.save(any())).thenReturn(newModel);

        service.resolve(buildProduct("모드만", "새모델", "새모델사이트"));

        verify(brandRepository, never()).save(any());
    }

    // -----------------------------------------------------------------------
    // helpers
    // -----------------------------------------------------------------------

    private Model buildModel(Long id, String brandName, String modelName) {
        Brand brand = Brand.builder().brandName(brandName).build();
        Model model = Model.builder().brand(brand).modelName(modelName).build();
        ReflectionTestUtils.setField(model, "id", id);
        return model;
    }

    private CrawledProduct buildProduct(String brandName, String modelName, String siteModelName) {
        return new CrawledProduct(brandName, modelName, siteModelName, null, null, false, null);
    }
}
