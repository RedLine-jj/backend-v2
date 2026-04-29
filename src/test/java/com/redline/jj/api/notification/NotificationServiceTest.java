package com.redline.jj.api.notification;

import com.redline.jj.common.exception.BusinessException;
import com.redline.jj.common.exception.ErrorCode;
import com.redline.jj.domain.notification.RestockNotification;
import com.redline.jj.domain.notification.RestockNotificationRepository;
import com.redline.jj.domain.user.User;
import com.redline.jj.domain.user.UserFinder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private RestockNotificationRepository notificationRepository;

    @Mock
    private UserFinder userFinder;

    @Mock
    private SseEmitterRepository sseEmitterRepository;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    @DisplayName("openStream - SSE Emitter가 등록되고 반환된다")
    void openStream_emitter_등록_반환() {
        User user = mock(User.class);
        given(user.getId()).willReturn(1L);
        given(userFinder.getByLoginId("testuser")).willReturn(user);

        SseEmitter emitter = notificationService.openStream("testuser");

        assertThat(emitter).isNotNull();
        verify(sseEmitterRepository, times(1)).add(eq(1L), any(SseEmitter.class));
    }

    @Test
    @DisplayName("markAsRead - 본인 알림이면 읽음 처리")
    void markAsRead_본인_알림_읽음처리() {
        User user = mock(User.class);
        given(user.getId()).willReturn(1L);
        given(userFinder.getByLoginId("testuser")).willReturn(user);

        RestockNotification notification = mock(RestockNotification.class);
        given(notification.getUser()).willReturn(user);
        given(notificationRepository.findById(10L)).willReturn(Optional.of(notification));

        notificationService.markAsRead("testuser", 10L);

        verify(notification, times(1)).markAsRead();
    }

    @Test
    @DisplayName("markAsRead - 타인 알림이면 N002 예외 발생")
    void markAsRead_타인_알림_N002() {
        User owner = mock(User.class);
        given(owner.getId()).willReturn(2L);

        User requester = mock(User.class);
        given(requester.getId()).willReturn(1L);
        given(userFinder.getByLoginId("testuser")).willReturn(requester);

        RestockNotification notification = mock(RestockNotification.class);
        given(notification.getUser()).willReturn(owner);
        given(notificationRepository.findById(10L)).willReturn(Optional.of(notification));

        assertThatThrownBy(() -> notificationService.markAsRead("testuser", 10L))
            .isInstanceOf(BusinessException.class)
            .extracting(e -> ((BusinessException) e).getErrorCode())
            .isEqualTo(ErrorCode.NOTIFICATION_ACCESS_DENIED);
    }

    @Test
    @DisplayName("markAsRead - 없는 알림 id이면 N001 예외 발생")
    void markAsRead_없는_알림_N001() {
        User user = mock(User.class);
        given(user.getId()).willReturn(1L);
        given(userFinder.getByLoginId("testuser")).willReturn(user);
        given(notificationRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.markAsRead("testuser", 999L))
            .isInstanceOf(BusinessException.class)
            .extracting(e -> ((BusinessException) e).getErrorCode())
            .isEqualTo(ErrorCode.NOTIFICATION_NOT_FOUND);
    }

    @Test
    @DisplayName("markAllAsRead - bulk update 쿼리 단일 호출")
    void markAllAsRead_bulk_update_단일_호출() {
        User user = mock(User.class);
        given(user.getId()).willReturn(1L);
        given(userFinder.getByLoginId("testuser")).willReturn(user);
        given(notificationRepository.markAllAsReadByUserId(1L)).willReturn(2);

        notificationService.markAllAsRead("testuser");

        verify(notificationRepository, times(1)).markAllAsReadByUserId(1L);
    }

    @Test
    @DisplayName("getNotifications - 최신순 반환")
    void getNotifications_최신순_반환() {
        User user = mock(User.class);
        given(user.getId()).willReturn(1L);
        given(userFinder.getByLoginId("testuser")).willReturn(user);
        given(notificationRepository.findByUser_IdOrderByCreatedAtDesc(1L)).willReturn(List.of());

        notificationService.getNotifications("testuser");

        verify(notificationRepository, times(1)).findByUser_IdOrderByCreatedAtDesc(1L);
    }

    @Test
    @DisplayName("openStream - 같은 userId로 두 번째 openStream 호출 시 기존 emitter가 교체된다")
    void openStream_재연결_시_기존_emitter_교체() {
        // given
        // SseEmitterRepository 실제 구현체를 spy로 생성해 add()가 실제로 emitter를 저장하도록 한다.
        // @Spy 필드를 쓰면 @InjectMocks의 동일 타입 @Mock과 충돌하므로 지역 변수로 생성한다.
        SseEmitterRepository repoSpy = spy(new SseEmitterRepository());
        NotificationService svc = new NotificationService(notificationRepository, userFinder, repoSpy);

        User user = mock(User.class);
        given(user.getId()).willReturn(1L);
        given(userFinder.getByLoginId("testuser")).willReturn(user);

        // when - 첫 번째 openStream
        SseEmitter first = svc.openStream("testuser");
        assertThat(repoSpy.get(1L)).contains(first);

        // 두 번째 openStream — 기존 emitter가 새 emitter로 교체되어야 한다
        SseEmitter second = svc.openStream("testuser");

        // then
        assertThat(second).isNotSameAs(first);
        assertThat(repoSpy.get(1L)).contains(second);
        verify(repoSpy, times(2)).add(eq(1L), any(SseEmitter.class));
    }

    @Test
    @DisplayName("openStream - remove 호출 시 repository에서 emitter가 제거된다 (콜백 결과 검증)")
    void openStream_remove_호출_시_emitter_제거() {
        // given
        // SseEmitter 콜백(onTimeout/onCompletion/onError)은 서블릿 비동기 컨텍스트 없이는
        // complete()로 트리거되지 않는다. 콜백이 최종적으로 호출하는 remove(userId) 메서드의
        // 동작 자체(emitter가 map에서 삭제됨)를 실제 구현체로 검증한다.
        SseEmitterRepository repoSpy = spy(new SseEmitterRepository());
        NotificationService svc = new NotificationService(notificationRepository, userFinder, repoSpy);

        User user = mock(User.class);
        given(user.getId()).willReturn(1L);
        given(userFinder.getByLoginId("testuser")).willReturn(user);

        svc.openStream("testuser");

        // emitter가 저장된 상태 확인
        assertThat(repoSpy.get(1L)).isPresent();

        // when — 콜백이 실행되면 결국 remove(userId)가 호출된다
        repoSpy.remove(1L);

        // then — 제거 후 get()은 비어 있어야 한다
        assertThat(repoSpy.get(1L)).isEmpty();
        verify(repoSpy, times(1)).remove(1L);
    }

    @Test
    @DisplayName("getUnreadCount - repository countByUser_IdAndReadFalse 호출 확인")
    void getUnreadCount_repository_호출_확인() {
        // given
        User user = mock(User.class);
        given(user.getId()).willReturn(1L);
        given(userFinder.getByLoginId("testuser")).willReturn(user);
        given(notificationRepository.countByUser_IdAndReadFalse(1L)).willReturn(3L);

        // when
        long count = notificationService.getUnreadCount("testuser");

        // then
        assertThat(count).isEqualTo(3L);
        verify(notificationRepository, times(1)).countByUser_IdAndReadFalse(1L);
    }
}
