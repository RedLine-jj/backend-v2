package com.redline.jj.batch.job;

import com.redline.jj.domain.notification.RestockNotification;
import com.redline.jj.domain.notification.RestockNotificationRepository;
import com.redline.jj.domain.subscription.Subscription;
import com.redline.jj.domain.subscription.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class RestockEventHandler {

    private final SubscriptionRepository subscriptionRepository;
    private final RestockNotificationRepository restockNotificationRepository;
    private final RedisTemplate<String, Object> redisTemplate;

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

            redisTemplate.convertAndSend("restock", Map.of(
                "userId", subscription.getUser().getId(),
                "modelId", event.modelId(),
                "modelName", event.modelName(),
                "brandName", event.brandName()
            ));
        }
    }
}
