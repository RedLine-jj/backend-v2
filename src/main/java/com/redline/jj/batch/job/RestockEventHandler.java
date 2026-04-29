package com.redline.jj.batch.job;

import com.redline.jj.domain.notification.RestockNotification;
import com.redline.jj.domain.notification.RestockNotificationRepository;
import com.redline.jj.domain.notification.UnreadCacheEvictEvent;
import com.redline.jj.domain.subscription.Subscription;
import com.redline.jj.domain.subscription.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class RestockEventHandler {

    private final SubscriptionRepository subscriptionRepository;
    private final RestockNotificationRepository restockNotificationRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ApplicationEventPublisher eventPublisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleRestock(RestockEvent event) {
        List<Subscription> subscriptions = subscriptionRepository.findByModel_Id(event.modelId());

        for (Subscription subscription : subscriptions) {
            restockNotificationRepository.save(
                RestockNotification.builder()
                    .user(subscription.getUser())
                    .model(subscription.getModel())
                    .build()
            );

            Long userId = subscription.getUser().getId();
            String loginId = subscription.getUser().getUserId();
            eventPublisher.publishEvent(new UnreadCacheEvictEvent(userId, loginId));

            try {
                redisTemplate.convertAndSend("restock", Map.of(
                    "userId", userId,
                    "modelId", event.modelId(),
                    "modelName", event.modelName(),
                    "brandName", event.brandName()
                ));
            } catch (Exception e) {
                log.warn("Redis 재입고 알림 전송 실패 — userId={}, modelId={}",
                    userId, event.modelId(), e);
            }
        }
    }
}
