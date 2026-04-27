package com.redline.jj.domain.subscription;

import com.redline.jj.config.JpaConfig;
import com.redline.jj.domain.brand.Brand;
import com.redline.jj.domain.brand.BrandRepository;
import com.redline.jj.domain.model.Model;
import com.redline.jj.domain.model.Model.ModelType;
import com.redline.jj.domain.model.ModelRepository;
import com.redline.jj.domain.user.User;
import com.redline.jj.domain.user.UserRepository;
import jakarta.persistence.EntityManager;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


@DataJpaTest
@Import(JpaConfig.class)
@ActiveProfiles("test")
class SubscriptionRepositoryTest {

    @Autowired
    private SubscriptionRepository subscriptionRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ModelRepository modelRepository;
    @Autowired
    private BrandRepository brandRepository;
    @Autowired
    private EntityManager em;

    private User user;
    private Brand brand;
    private Model model;

    @BeforeEach
    void setUp() {
        user = userRepository.save(User.builder()
            .userId("testUser")
            .userPw("pw123")
            .userName("홍길동")
            .build());
        brand = brandRepository.save(Brand.builder().brandName("리바이스").build());
        model = modelRepository.save(Model.builder()
            .brand(brand)
            .modelName("501")
            .modelType(ModelType.DENIM_PANTS)
            .build());
    }

    // =========================================================================
    // existsByUser_IdAndModel_Id
    // =========================================================================

    @Test
    @DisplayName("existsByUser_IdAndModel_Id: 구독이 존재하면 true, 없으면 false 반환")
    void userIdx와_modelIdx로_구독_존재_여부를_확인한다() {
        subscriptionRepository.save(Subscription.builder().user(user).model(model).build());

        assertThat(subscriptionRepository.existsByUser_IdAndModel_Id(user.getId(), model.getId())).isTrue();
        assertThat(subscriptionRepository.existsByUser_IdAndModel_Id(user.getId(), 999L)).isFalse();
    }

    @Test
    @DisplayName("existsByUser_IdAndModel_Id: 다른 user의 구독은 false 반환")
    void 다른_유저의_구독은_존재하지_않는다() {
        User anotherUser = userRepository.save(User.builder()
            .userId("anotherUser")
            .userPw("pw")
            .userName("김철수")
            .build());
        subscriptionRepository.save(Subscription.builder().user(anotherUser).model(model).build());

        // user는 구독하지 않았으므로 false여야 한다
        assertThat(subscriptionRepository.existsByUser_IdAndModel_Id(user.getId(), model.getId())).isFalse();
    }

    // =========================================================================
    // unique constraint
    // =========================================================================

    @Test
    @DisplayName("uk_subscription: 동일 user + model 중복 구독 시 DataIntegrityViolationException 발생")
    void 동일_user_model_중복_구독시_예외가_발생한다() {
        subscriptionRepository.saveAndFlush(Subscription.builder().user(user).model(model).build());

        assertThatThrownBy(() ->
            subscriptionRepository.saveAndFlush(Subscription.builder().user(user).model(model).build())
        ).isInstanceOf(DataIntegrityViolationException.class);
    }

    // =========================================================================
    // findByIdWithUser
    // =========================================================================

    @Test
    @DisplayName("findByIdWithUser: 결과에 user가 JOIN FETCH되어 LazyInitializationException 없이 로딩된다")
    void findByIdWithUser_user가_즉시로딩된다() {
        Subscription saved = subscriptionRepository.saveAndFlush(
            Subscription.builder().user(user).model(model).build()
        );
        // 1차 캐시를 제거해 실제 DB 조회를 강제한다
        em.clear();

        Optional<Subscription> result = subscriptionRepository.findByIdWithUser(saved.getId());

        assertThat(result).isPresent();
        // 트랜잭션 내부이므로 프록시가 아닌 실제 데이터에 접근 가능하다
        assertThat(result.get().getUser().getUserId()).isEqualTo("testUser");
        assertThat(result.get().getUser().getUserName()).isEqualTo("홍길동");
    }

    @Test
    @DisplayName("findByIdWithUser: 존재하지 않는 id는 Optional.empty() 반환")
    void findByIdWithUser_없는id는_empty반환() {
        Optional<Subscription> result = subscriptionRepository.findByIdWithUser(999L);

        assertThat(result).isEmpty();
    }

    // =========================================================================
    // findByUser_UserId — N+1 검증 (model, brand JOIN FETCH)
    // =========================================================================

    @Test
    @DisplayName("findByUser_UserId: model과 brand가 JOIN FETCH되어 단일 SQL 쿼리로 로딩된다")
    void findByUser_UserId_model과_brand가_즉시로딩된다() {
        subscriptionRepository.saveAndFlush(Subscription.builder().user(user).model(model).build());
        em.clear();

        Statistics stats = em.getEntityManagerFactory()
            .unwrap(SessionFactory.class)
            .getStatistics();
        stats.setStatisticsEnabled(true);
        stats.clear();

        List<Subscription> results = subscriptionRepository.findByUser_UserId("testUser");

        assertThat(results).hasSize(1);
        Subscription sub = results.get(0);
        assertThat(sub.getModel().getModelName()).isEqualTo("501");
        assertThat(sub.getModel().getBrand().getBrandName()).isEqualTo("리바이스");
        assertThat(sub.getUser().getUserId()).isEqualTo("testUser");
        // JOIN FETCH가 올바르게 적용됐다면 SELECT 1회만 발생해야 한다
        assertThat(stats.getPrepareStatementCount()).isEqualTo(1);

        stats.setStatisticsEnabled(false);
    }

    @Test
    @DisplayName("findByUser_UserId: 해당 user의 구독만 반환하고 다른 user 구독은 포함하지 않는다")
    void findByUser_UserId_다른_유저의_구독은_반환하지_않는다() {
        User anotherUser = userRepository.save(User.builder()
            .userId("anotherUser")
            .userPw("pw")
            .userName("김철수")
            .build());
        Model anotherModel = modelRepository.save(Model.builder()
            .brand(brand)
            .modelName("502")
            .modelType(ModelType.DENIM_PANTS)
            .build());

        subscriptionRepository.saveAndFlush(Subscription.builder().user(user).model(model).build());
        subscriptionRepository.saveAndFlush(Subscription.builder().user(anotherUser).model(anotherModel).build());
        em.clear();

        List<Subscription> results = subscriptionRepository.findByUser_UserId("testUser");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getUser().getUserId()).isEqualTo("testUser");
    }

    @Test
    @DisplayName("findByUser_UserId: 구독이 없는 user는 빈 리스트를 반환한다")
    void findByUser_UserId_구독없는_유저는_빈리스트반환() {
        List<Subscription> results = subscriptionRepository.findByUser_UserId("testUser");

        assertThat(results).isEmpty();
    }

    // =========================================================================
    // findTop10ModelsBySubscriptionCount — Projection 컬럼 매핑 + 정렬
    // =========================================================================

    @Test
    @DisplayName("findTop10ModelsBySubscriptionCount: 10개 초과 데이터에서 상위 10개만 반환한다")
    void 구독_수_기준_상위_10개_모델을_반환한다() {
        // 모델 12개 생성 후 구독 수 다르게 설정
        for (int i = 1; i <= 12; i++) {
            Model m = modelRepository.save(Model.builder()
                .brand(brand)
                .modelName("모델" + i)
                .modelType(ModelType.DENIM_PANTS)
                .build());
            // 마지막 2개(i=11,12)는 구독 없음
            if (i <= 10) {
                User u = userRepository.save(User.builder()
                    .userId("user" + i)
                    .userPw("pw")
                    .userName("사용자" + i)
                    .build());
                subscriptionRepository.save(Subscription.builder().user(u).model(m).build());
            }
        }

        List<ModelSubscriptionCount> top10 = subscriptionRepository.findTop10ModelsBySubscriptionCount();

        assertThat(top10).hasSize(10);
    }

    @Test
    @DisplayName("findTop10ModelsBySubscriptionCount: Projection의 modelId, modelName, brandName이 올바르게 매핑된다")
    void findTop10_Projection_컬럼_매핑이_올바르다() {
        subscriptionRepository.saveAndFlush(Subscription.builder().user(user).model(model).build());

        List<ModelSubscriptionCount> result = subscriptionRepository.findTop10ModelsBySubscriptionCount();

        assertThat(result).hasSize(1);
        ModelSubscriptionCount projection = result.get(0);
        assertThat(projection.getModelId()).isEqualTo(model.getId());
        assertThat(projection.getModelName()).isEqualTo("501");
        assertThat(projection.getBrandName()).isEqualTo("리바이스");
    }

    @Test
    @DisplayName("findTop10ModelsBySubscriptionCount: 구독 수 내림차순으로 정렬된다")
    void findTop10_구독수_내림차순_정렬() {
        // 모델A: 구독 3건, 모델B: 구독 1건 — A가 먼저 나와야 한다
        Model modelA = modelRepository.save(Model.builder()
            .brand(brand)
            .modelName("모델A")
            .modelType(ModelType.DENIM_PANTS)
            .build());
        Model modelB = modelRepository.save(Model.builder()
            .brand(brand)
            .modelName("모델B")
            .modelType(ModelType.DENIM_PANTS)
            .build());

        for (int i = 1; i <= 3; i++) {
            User u = userRepository.save(User.builder()
                .userId("userA" + i)
                .userPw("pw")
                .userName("사용자A" + i)
                .build());
            subscriptionRepository.save(Subscription.builder().user(u).model(modelA).build());
        }
        User singleUser = userRepository.save(User.builder()
            .userId("userB1")
            .userPw("pw")
            .userName("사용자B1")
            .build());
        subscriptionRepository.save(Subscription.builder().user(singleUser).model(modelB).build());

        List<ModelSubscriptionCount> result = subscriptionRepository.findTop10ModelsBySubscriptionCount();

        // 구독이 많은 modelA가 첫 번째여야 한다
        assertThat(result.get(0).getModelName()).isEqualTo("모델A");
        assertThat(result.get(1).getModelName()).isEqualTo("모델B");
    }

    @Test
    @DisplayName("findTop10ModelsBySubscriptionCount: 구독이 하나도 없으면 빈 리스트를 반환한다")
    void findTop10_구독없으면_빈리스트반환() {
        List<ModelSubscriptionCount> result = subscriptionRepository.findTop10ModelsBySubscriptionCount();

        assertThat(result).isEmpty();
    }
}
