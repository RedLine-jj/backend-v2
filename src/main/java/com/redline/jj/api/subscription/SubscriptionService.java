package com.redline.jj.api.subscription;

import com.redline.jj.api.subscription.dto.SubscriptionResponse;
import com.redline.jj.api.subscription.dto.SubscriptionTopResponse;
import com.redline.jj.common.exception.BusinessException;
import com.redline.jj.common.exception.ErrorCode;
import com.redline.jj.domain.model.Model;
import com.redline.jj.domain.model.ModelRepository;
import com.redline.jj.domain.subscription.Subscription;
import com.redline.jj.domain.subscription.SubscriptionRepository;
import com.redline.jj.domain.user.User;
import com.redline.jj.domain.user.UserFinder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final ModelRepository modelRepository;
    private final UserFinder userFinder;

    @Transactional
    public SubscriptionResponse subscribe(String userLoginId, Long modelId) {
        User user = userFinder.getByLoginId(userLoginId);

        if (subscriptionRepository.existsByUser_IdAndModel_Id(user.getId(), modelId)) {
            throw new BusinessException(ErrorCode.SUBSCRIPTION_ALREADY_EXISTS);
        }

        Model model = modelRepository.findById(modelId)
            .orElseThrow(() -> new BusinessException(ErrorCode.MODEL_NOT_FOUND));

        Subscription saved = subscriptionRepository.save(
            Subscription.builder()
                .user(user)
                .model(model)
                .build()
        );

        return SubscriptionResponse.from(saved);
    }

    @Transactional
    public void cancel(String userLoginId, Long subscriptionId) {
        Subscription subscription = subscriptionRepository.findByIdWithUser(subscriptionId)
            .orElseThrow(() -> new BusinessException(ErrorCode.SUBSCRIPTION_NOT_FOUND));

        if (!subscription.getUser().getUserId().equals(userLoginId)) {
            throw new BusinessException(ErrorCode.SUBSCRIPTION_ACCESS_DENIED);
        }

        subscriptionRepository.delete(subscription);
    }

    @Transactional(readOnly = true)
    public List<SubscriptionResponse> getMySubscriptions(String userLoginId) {
        return subscriptionRepository.findByUser_UserId(userLoginId)
            .stream()
            .map(SubscriptionResponse::from)
            .toList();
    }

    @Transactional(readOnly = true)
    public long getMySubscriptionCount(String userLoginId) {
        return subscriptionRepository.countByUser_UserId(userLoginId);
    }

    @Transactional(readOnly = true)
    public List<SubscriptionTopResponse> getTop10() {
        return subscriptionRepository.findTop10ModelsBySubscriptionCount()
            .stream()
            .map(SubscriptionTopResponse::from)
            .toList();
    }

}
