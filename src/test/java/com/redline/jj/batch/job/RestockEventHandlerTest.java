package com.redline.jj.batch.job;

import com.redline.jj.domain.model.Model;
import com.redline.jj.domain.notification.RestockNotification;
import com.redline.jj.domain.notification.RestockNotificationRepository;
import com.redline.jj.domain.notification.UnreadCacheEvictEvent;
import com.redline.jj.domain.subscription.Subscription;
import com.redline.jj.domain.subscription.SubscriptionRepository;
import com.redline.jj.domain.user.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RestockEventHandlerTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private RestockNotificationRepository restockNotificationRepository;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private RestockEventHandler restockEventHandler;

    // =========================================================================
    // 구독자 1명
    // =========================================================================

    @Test
    @DisplayName("구독자 1명 - save 1회, publishEvent 1회, convertAndSend 1회 호출")
    void 구독자_1명_각_작업이_1회_호출된다() {
        // given
        User user = mock(User.class);
        given(user.getId()).willReturn(10L);
        given(user.getUserId()).willReturn("user10");

        Model model = mock(Model.class);

        Subscription subscription = mock(Subscription.class);
        given(subscription.getUser()).willReturn(user);
        given(subscription.getModel()).willReturn(model);

        given(subscriptionRepository.findByModel_Id(1L)).willReturn(List.of(subscription));

        RestockEvent event = new RestockEvent(1L, "501", "리바이스");

        // when
        restockEventHandler.handleRestock(event);

        // then
        verify(restockNotificationRepository, times(1)).save(any(RestockNotification.class));
        verify(eventPublisher, times(1)).publishEvent(any(UnreadCacheEvictEvent.class));
        verify(redisTemplate, times(1)).convertAndSend(eq("restock"), any());
    }

    @Test
    @DisplayName("구독자 1명 - publishEvent에 전달된 UnreadCacheEvictEvent의 userId·loginId가 구독자와 일치한다")
    void 구독자_1명_publishEvent_userId_일치() {
        // given
        User user = mock(User.class);
        given(user.getId()).willReturn(42L);
        given(user.getUserId()).willReturn("user42");

        Model model = mock(Model.class);

        Subscription subscription = mock(Subscription.class);
        given(subscription.getUser()).willReturn(user);
        given(subscription.getModel()).willReturn(model);

        given(subscriptionRepository.findByModel_Id(1L)).willReturn(List.of(subscription));

        RestockEvent event = new RestockEvent(1L, "501", "리바이스");

        // when
        restockEventHandler.handleRestock(event);

        // then
        ArgumentCaptor<UnreadCacheEvictEvent> captor = ArgumentCaptor.forClass(UnreadCacheEvictEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().userId()).isEqualTo(42L);
        assertThat(captor.getValue().loginId()).isEqualTo("user42");
    }

    // =========================================================================
    // 구독자 2명
    // =========================================================================

    @Test
    @DisplayName("구독자 2명 - save 2회, publishEvent 2회, convertAndSend 2회 호출")
    void 구독자_2명_각_작업이_2회_호출된다() {
        // given
        User user1 = mock(User.class);
        given(user1.getId()).willReturn(10L);
        given(user1.getUserId()).willReturn("user1");
        User user2 = mock(User.class);
        given(user2.getId()).willReturn(20L);
        given(user2.getUserId()).willReturn("user2");

        Model model = mock(Model.class);

        Subscription sub1 = mock(Subscription.class);
        given(sub1.getUser()).willReturn(user1);
        given(sub1.getModel()).willReturn(model);

        Subscription sub2 = mock(Subscription.class);
        given(sub2.getUser()).willReturn(user2);
        given(sub2.getModel()).willReturn(model);

        given(subscriptionRepository.findByModel_Id(5L)).willReturn(List.of(sub1, sub2));

        RestockEvent event = new RestockEvent(5L, "Slim", "리Lee");

        // when
        restockEventHandler.handleRestock(event);

        // then
        verify(restockNotificationRepository, times(2)).save(any(RestockNotification.class));
        verify(eventPublisher, times(2)).publishEvent(any(UnreadCacheEvictEvent.class));
        verify(redisTemplate, times(2)).convertAndSend(eq("restock"), any());
    }

    // =========================================================================
    // Redis 실패 — 예외 미전파
    // =========================================================================

    @Test
    @DisplayName("Redis publish 실패 시 예외가 전파되지 않고, save는 정상 호출된다")
    void redis_publish_실패시_예외_미전파() {
        // given
        User user = mock(User.class);
        given(user.getId()).willReturn(10L);
        given(user.getUserId()).willReturn("user10");

        Model model = mock(Model.class);

        Subscription subscription = mock(Subscription.class);
        given(subscription.getUser()).willReturn(user);
        given(subscription.getModel()).willReturn(model);

        given(subscriptionRepository.findByModel_Id(1L)).willReturn(List.of(subscription));

        // Redis 전송 시 RuntimeException 발생하도록 설정
        willThrow(new RuntimeException("Redis connection refused"))
            .given(redisTemplate).convertAndSend(eq("restock"), any());

        RestockEvent event = new RestockEvent(1L, "501", "리바이스");

        // when & then — 예외가 외부로 전파되지 않아야 한다
        assertThatCode(() -> restockEventHandler.handleRestock(event))
            .doesNotThrowAnyException();

        // Redis 실패와 무관하게 DB 저장은 완료되어야 한다
        verify(restockNotificationRepository, times(1)).save(any(RestockNotification.class));
    }

    // =========================================================================
    // 구독자 없음
    // =========================================================================

    @Test
    @DisplayName("구독자 없음 - save, publishEvent, convertAndSend 모두 미호출")
    void 구독자_없음_모든_작업_미호출() {
        // given
        given(subscriptionRepository.findByModel_Id(99L)).willReturn(List.of());

        RestockEvent event = new RestockEvent(99L, "NoSub", "NoName");

        // when
        restockEventHandler.handleRestock(event);

        // then
        verify(restockNotificationRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
        verify(redisTemplate, never()).convertAndSend(any(), any());
    }

    // =========================================================================
    // 헬퍼 — Spring Context 없이 mock() 사용
    // =========================================================================

    private static <T> T mock(Class<T> classToMock) {
        return org.mockito.Mockito.mock(classToMock);
    }
}
