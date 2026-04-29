package com.redline.jj.domain.notification;

import com.redline.jj.config.JpaConfig;
import com.redline.jj.domain.brand.Brand;
import com.redline.jj.domain.brand.BrandRepository;
import com.redline.jj.domain.model.Model;
import com.redline.jj.domain.model.Model.ModelType;
import com.redline.jj.domain.model.ModelRepository;
import com.redline.jj.domain.user.User;
import com.redline.jj.domain.user.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(JpaConfig.class)
@ActiveProfiles("test")
class RestockNotificationRepositoryTest {

    @Autowired
    private RestockNotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ModelRepository modelRepository;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private EntityManager em;

    private User user;
    private Model model;

    @BeforeEach
    void setUp() {
        Brand brand = brandRepository.save(Brand.builder()
            .brandName("리바이스")
            .brandNameKo("리바이스")
            .build());
        model = modelRepository.save(Model.builder()
            .brand(brand)
            .modelName("501")
            .modelType(ModelType.DENIM_PANTS)
            .build());
        user = userRepository.save(User.builder()
            .userId("testUser")
            .userPw("pw123")
            .userName("홍길동")
            .build());
    }

    // =========================================================================
    // markAllAsReadByUserId — 미읽음 → 전부 read=true
    // =========================================================================

    @Test
    @DisplayName("markAllAsReadByUserId: 미읽음 알림 2건이 모두 read=true로 변경되고 반환값은 2이다")
    void 미읽음_알림_2건_모두_read로_변경된다() {
        // given — 미읽음 알림 2건 저장
        notificationRepository.save(RestockNotification.builder()
            .user(user).model(model).build());
        notificationRepository.save(RestockNotification.builder()
            .user(user).model(model).build());
        em.flush();
        em.clear();

        // when
        int updated = notificationRepository.markAllAsReadByUserId(user.getId());

        // then
        assertThat(updated).isEqualTo(2);

        em.clear();
        List<RestockNotification> result = notificationRepository.findByUser_IdOrderByCreatedAtDesc(user.getId());
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(RestockNotification::isRead);
    }

    // =========================================================================
    // markAllAsReadByUserId — 이미 읽은 알림은 변경 없음
    // =========================================================================

    @Test
    @DisplayName("markAllAsReadByUserId: 이미 읽은 알림만 있으면 변경 없고 반환값은 0이다")
    void 이미_읽은_알림은_변경_없이_0_반환() {
        // given — read=true인 알림 저장 (markAsRead() 호출)
        RestockNotification notification = notificationRepository.save(RestockNotification.builder()
            .user(user).model(model).build());
        notification.markAsRead();
        notificationRepository.save(notification);
        em.flush();
        em.clear();

        // when
        int updated = notificationRepository.markAllAsReadByUserId(user.getId());

        // then — 이미 read=true이므로 UPDATE 대상 0건
        assertThat(updated).isEqualTo(0);
    }

    // =========================================================================
    // markAllAsReadByUserId — 다른 userId 알림은 변경 없음
    // =========================================================================

    @Test
    @DisplayName("markAllAsReadByUserId: 다른 user의 미읽음 알림은 변경하지 않는다")
    void 다른_userId_알림은_변경하지_않는다() {
        // given — 타인 알림 1건, 본인 알림 1건
        User otherUser = userRepository.save(User.builder()
            .userId("otherUser")
            .userPw("pw456")
            .userName("김철수")
            .build());

        notificationRepository.save(RestockNotification.builder()
            .user(otherUser).model(model).build());
        notificationRepository.save(RestockNotification.builder()
            .user(user).model(model).build());
        em.flush();
        em.clear();

        // when — 본인(user) userId 기준으로만 bulk update
        int updated = notificationRepository.markAllAsReadByUserId(user.getId());

        // then — 본인 알림 1건만 업데이트
        assertThat(updated).isEqualTo(1);

        em.clear();
        // 타인 알림은 여전히 read=false여야 한다
        List<RestockNotification> otherNotifications =
            notificationRepository.findByUser_IdOrderByCreatedAtDesc(otherUser.getId());
        assertThat(otherNotifications).hasSize(1);
        assertThat(otherNotifications.get(0).isRead()).isFalse();

        // 본인 알림은 read=true여야 한다
        List<RestockNotification> myNotifications =
            notificationRepository.findByUser_IdOrderByCreatedAtDesc(user.getId());
        assertThat(myNotifications).hasSize(1);
        assertThat(myNotifications.get(0).isRead()).isTrue();
    }
}
