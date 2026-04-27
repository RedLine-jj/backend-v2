package com.redline.jj.domain.user;

import com.redline.jj.config.JpaConfig;
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
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void userId로_사용자를_조회할_수_있다() {
        User user = User.builder()
            .userId("testUser")
            .userPw("pw123")
            .userName("홍길동")
            .build();
        userRepository.save(user);

        Optional<User> found = userRepository.findByUserId("testUser");

        assertThat(found).isPresent();
        assertThat(found.get().getUserName()).isEqualTo("홍길동");
        assertThat(found.get().getUserId()).isEqualTo("testUser");
    }

    @Test
    void 존재하지_않는_userId는_empty를_반환한다() {
        Optional<User> found = userRepository.findByUserId("notExist");

        assertThat(found).isEmpty();
    }

    @Test
    void 중복_userId_저장시_예외가_발생한다() {
        User user1 = User.builder()
            .userId("dupUser")
            .userPw("pw1")
            .userName("사용자1")
            .build();
        userRepository.saveAndFlush(user1);

        User user2 = User.builder()
            .userId("dupUser")
            .userPw("pw2")
            .userName("사용자2")
            .build();

        assertThatThrownBy(() -> userRepository.saveAndFlush(user2))
            .isInstanceOf(DataIntegrityViolationException.class);
    }
}
