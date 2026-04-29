package com.redline.jj.api.notification;

import com.redline.jj.api.notification.dto.NotificationResponse;
import com.redline.jj.common.exception.BusinessException;
import com.redline.jj.common.exception.ErrorCode;
import com.redline.jj.domain.notification.RestockNotification;
import com.redline.jj.domain.notification.RestockNotificationRepository;
import com.redline.jj.domain.notification.UnreadCacheEvictEvent;
import com.redline.jj.domain.user.UserFinder;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final RestockNotificationRepository notificationRepository;
    private final UserFinder userFinder;
    private final SseEmitterRepository sseEmitterRepository;

    public SseEmitter openStream(String loginId) {
        Long userId = userFinder.getByLoginId(loginId).getId();
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);
        emitter.onTimeout(() -> sseEmitterRepository.remove(userId, emitter));
        emitter.onCompletion(() -> sseEmitterRepository.remove(userId, emitter));
        emitter.onError(e -> sseEmitterRepository.remove(userId, emitter));
        sseEmitterRepository.add(userId, emitter);
        return emitter;
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotifications(String loginId) {
        Long userId = userFinder.getByLoginId(loginId).getId();
        return notificationRepository.findByUser_IdOrderByCreatedAtDesc(userId)
            .stream()
            .map(NotificationResponse::from)
            .toList();
    }

    @Cacheable(value = "unreadCount", key = "#loginId")
    @Transactional(readOnly = true)
    public long getUnreadCount(String loginId) {
        Long userId = userFinder.getByLoginId(loginId).getId();
        return notificationRepository.countByUser_IdAndReadFalse(userId);
    }

    @CacheEvict(value = "unreadCount", key = "#loginId")
    @Transactional
    public void markAsRead(String loginId, Long notificationId) {
        Long userId = userFinder.getByLoginId(loginId).getId();
        RestockNotification notification = notificationRepository.findById(notificationId)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND));

        if (!notification.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOTIFICATION_ACCESS_DENIED);
        }

        notification.markAsRead();
    }

    @CacheEvict(value = "unreadCount", key = "#loginId")
    @Transactional
    public void markAllAsRead(String loginId) {
        Long userId = userFinder.getByLoginId(loginId).getId();
        notificationRepository.markAllAsReadByUserId(userId);
    }

    @EventListener
    @CacheEvict(value = "unreadCount", key = "#event.loginId")
    public void onUnreadCacheEvict(UnreadCacheEvictEvent event) {
    }
}
