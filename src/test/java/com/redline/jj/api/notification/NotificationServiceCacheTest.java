package com.redline.jj.api.notification;

import com.redline.jj.batch.job.RestockEvent;
import com.redline.jj.domain.notification.RestockNotificationRepository;
import com.redline.jj.domain.notification.UnreadCacheEvictEvent;
import com.redline.jj.domain.user.User;
import com.redline.jj.domain.user.UserFinder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Objects;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

// 로컬 인프라 전제 조건(CLAUDE.md): Redis localhost:6379이 기동 상태여야 한다.
@SpringBootTest
@ActiveProfiles("test")
class NotificationServiceCacheTest {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @MockBean
    private RestockNotificationRepository notificationRepository;

    @MockBean
    private UserFinder userFinder;

    @MockBean
    private SseEmitterRepository sseEmitterRepository;

    private User mockUser;

    @BeforeEach
    void setUp() {
        mockUser = mock(User.class);
        given(mockUser.getId()).willReturn(1L);
        given(userFinder.getByLoginId("testuser")).willReturn(mockUser);
        given(notificationRepository.countByUser_IdAndReadFalse(1L)).willReturn(3L);
        Objects.requireNonNull(cacheManager.getCache("unreadCount")).clear();
    }

    @AfterEach
    void tearDown() {
        Objects.requireNonNull(cacheManager.getCache("unreadCount")).clear();
    }

    @Test
    @DisplayName("UnreadCacheEvictEvent 발행 후 unreadCount 캐시 evict 확인")
    void 이벤트_발행_후_캐시_evict() {
        // 1. 첫 호출: cache miss → Repository 조회
        notificationService.getUnreadCount("testuser");
        verify(notificationRepository, times(1)).countByUser_IdAndReadFalse(1L);

        // 2. UnreadCacheEvictEvent 발행 → @CacheEvict(allEntries=true) 작동
        applicationEventPublisher.publishEvent(new UnreadCacheEvictEvent(1L));

        // 3. 재호출: cache miss (evict됨) → Repository 재조회
        notificationService.getUnreadCount("testuser");
        verify(notificationRepository, times(2)).countByUser_IdAndReadFalse(1L);
    }

    @Test
    @DisplayName("배치 트랜잭션 롤백 시 AFTER_COMMIT 리스너 미호출로 캐시 evict 미발생")
    void 트랜잭션_롤백_시_캐시_evict_미발생() {
        // 1. 캐시 워밍업
        notificationService.getUnreadCount("testuser");
        verify(notificationRepository, times(1)).countByUser_IdAndReadFalse(1L);

        // 2. 트랜잭션 내 RestockEvent 발행 후 강제 롤백
        //    RestockEventHandler.handleRestock()는 @TransactionalEventListener(AFTER_COMMIT)이므로
        //    롤백 시 호출되지 않고, 따라서 UnreadCacheEvictEvent도 발행되지 않는다
        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
        try {
            txTemplate.execute(status -> {
                applicationEventPublisher.publishEvent(new RestockEvent(1L, "Test Model", "Test Brand"));
                throw new RuntimeException("강제 롤백");
            });
        } catch (RuntimeException ignored) {}

        // 3. cache evict 미발생 → Repository 재호출 없음 (여전히 1회)
        notificationService.getUnreadCount("testuser");
        verify(notificationRepository, times(1)).countByUser_IdAndReadFalse(1L);
    }
}
