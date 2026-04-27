package com.redline.jj.api.master;

import com.redline.jj.api.master.dto.BrandResponse;
import com.redline.jj.api.master.dto.SiteResponse;
import com.redline.jj.domain.brand.Brand;
import com.redline.jj.domain.brand.BrandRepository;
import com.redline.jj.domain.site.Site;
import com.redline.jj.domain.site.SiteRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

// 로컬 인프라 전제 조건(CLAUDE.md): Redis localhost:6379이 기동 상태여야 한다.
@SpringBootTest
@ActiveProfiles("test")
class MasterDataCacheIntegrationTest {

    @Autowired
    private MasterDataService masterDataService;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private SiteRepository siteRepository;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void setUp() {
        brandRepository.save(Brand.builder().brandName("Levi's").brandNameKo("리바이스").build());
        siteRepository.save(Site.builder().siteName("모드만").siteLink("https://modeman.co.kr").build());
    }

    @AfterEach
    void tearDown() {
        cacheManager.getCache("brands").clear();
        cacheManager.getCache("sites").clear();
        brandRepository.deleteAll();
        siteRepository.deleteAll();
    }

    @Test
    void getBrands_첫_조회_후_Redis에_brands_키가_존재한다() {
        masterDataService.getBrands();

        Set<String> keys = redisTemplate.keys("brands*");
        assertThat(keys).isNotEmpty();
    }

    @Test
    void getBrands_두번째_호출시_캐시가_hit된다() {
        List<BrandResponse> first = masterDataService.getBrands();
        brandRepository.save(Brand.builder().brandName("Diesel").brandNameKo("디젤").build());
        List<BrandResponse> second = masterDataService.getBrands();

        // DB에 새 데이터를 추가했지만 캐시 hit으로 이전 결과가 반환된다
        assertThat(second.size()).isEqualTo(first.size());
    }

    @Test
    void brands_캐시_TTL이_10분으로_설정된다() {
        masterDataService.getBrands();

        Set<String> keys = redisTemplate.keys("brands*");
        assertThat(keys).isNotEmpty();
        Long expireSeconds = redisTemplate.getExpire(keys.iterator().next());
        Duration ttl = Duration.ofSeconds(expireSeconds);

        assertThat(ttl)
                .isGreaterThan(Duration.ofMinutes(9))
                .isLessThanOrEqualTo(Duration.ofMinutes(10));
    }

    @Test
    void evictBrandsCache_호출_후_Redis에서_brands_키가_삭제된다() {
        masterDataService.getBrands();
        assertThat(redisTemplate.keys("brands*")).isNotEmpty();

        masterDataService.evictBrandsCache();

        assertThat(redisTemplate.keys("brands*")).isEmpty();
    }

    @Test
    void getSites_첫_조회_후_Redis에_sites_키가_존재한다() {
        masterDataService.getSites();

        Set<String> keys = redisTemplate.keys("sites*");
        assertThat(keys).isNotEmpty();
    }

    @Test
    void getSites_두번째_호출시_캐시가_hit된다() {
        List<SiteResponse> first = masterDataService.getSites();
        siteRepository.save(Site.builder().siteName("네스트").siteLink("https://nest.co.kr").build());
        List<SiteResponse> second = masterDataService.getSites();

        assertThat(second.size()).isEqualTo(first.size());
    }

    @Test
    void evictSitesCache_호출_후_Redis에서_sites_키가_삭제된다() {
        masterDataService.getSites();
        assertThat(redisTemplate.keys("sites*")).isNotEmpty();

        masterDataService.evictSitesCache();

        assertThat(redisTemplate.keys("sites*")).isEmpty();
    }
}
