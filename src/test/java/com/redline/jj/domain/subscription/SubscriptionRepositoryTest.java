package com.redline.jj.domain.subscription;

import com.redline.jj.config.JpaConfig;
import com.redline.jj.domain.brand.Brand;
import com.redline.jj.domain.brand.BrandRepository;
import com.redline.jj.domain.model.Model;
import com.redline.jj.domain.model.Model.ModelType;
import com.redline.jj.domain.model.ModelRepository;
import com.redline.jj.domain.user.User;
import com.redline.jj.domain.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

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

    private User user;
    private Model model;

    @BeforeEach
    void setUp() {
        user = userRepository.save(User.builder()
            .userId("testUser")
            .userPw("pw123")
            .userName("홍길동")
            .build());
        Brand brand = brandRepository.save(Brand.builder().brandName("리바이스").build());
        model = modelRepository.save(Model.builder()
            .brand(brand)
            .modelName("501")
            .modelType(ModelType.DENIM_PANTS)
            .build());
    }

    @Test
    void userIdx와_modelIdx로_구독_존재_여부를_확인한다() {
        subscriptionRepository.save(Subscription.builder().user(user).model(model).build());

        assertThat(subscriptionRepository.existsByUser_IdxAndModel_Idx(user.getIdx(), model.getIdx())).isTrue();
        assertThat(subscriptionRepository.existsByUser_IdxAndModel_Idx(user.getIdx(), 999L)).isFalse();
    }

    @Test
    void 구독이_하나라도_있으면_existsAny가_true다() {
        subscriptionRepository.save(Subscription.builder().user(user).model(model).build());

        assertThat(subscriptionRepository.existsAny()).isTrue();
    }

    @Test
    void 구독이_없으면_existsAny가_false다() {
        assertThat(subscriptionRepository.existsAny()).isFalse();
    }

    @Test
    void 동일_user_model_중복_구독시_예외가_발생한다() {
        subscriptionRepository.saveAndFlush(Subscription.builder().user(user).model(model).build());

        assertThatThrownBy(() ->
            subscriptionRepository.saveAndFlush(Subscription.builder().user(user).model(model).build())
        ).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void 구독_수_기준_상위_10개_모델을_반환한다() {
        Brand brand = brandRepository.findAll().get(0);
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

        List<Model> top10 = subscriptionRepository.findTop10ModelsBySubscriptionCount();

        assertThat(top10).hasSize(10);
    }
}
