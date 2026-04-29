package com.redline.jj.api.dashboard;

import com.redline.jj.api.dashboard.dto.PriceComparisonResponse;
import com.redline.jj.api.dashboard.dto.PriceHistoryResponse;
import com.redline.jj.api.dashboard.dto.RecentRestockResponse;
import com.redline.jj.common.exception.BusinessException;
import com.redline.jj.common.exception.ErrorCode;
import com.redline.jj.domain.model.Model;
import com.redline.jj.domain.model.ModelRepository;
import com.redline.jj.domain.notification.RestockNotification;
import com.redline.jj.domain.notification.RestockNotificationRepository;
import com.redline.jj.domain.option.SiteOption;
import com.redline.jj.domain.option.SiteOptionLog;
import com.redline.jj.domain.option.SiteOptionLogRepository;
import com.redline.jj.domain.option.SiteOptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final ModelRepository modelRepository;
    private final SiteOptionRepository siteOptionRepository;
    private final SiteOptionLogRepository siteOptionLogRepository;
    private final RestockNotificationRepository restockNotificationRepository;

    public PriceComparisonResponse getPriceComparison(Long modelId) {
        Model model = modelRepository.findById(modelId)
            .orElseThrow(() -> new BusinessException(ErrorCode.MODEL_NOT_FOUND));
        List<SiteOption> options = siteOptionRepository.search(null, modelId, null);
        return PriceComparisonResponse.from(model, options);
    }

    public PriceHistoryResponse getPriceHistory(Long modelId, int days) {
        modelRepository.findById(modelId)
            .orElseThrow(() -> new BusinessException(ErrorCode.MODEL_NOT_FOUND));
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        List<SiteOptionLog> logs = siteOptionLogRepository.findByModelIdSince(modelId, since);
        return PriceHistoryResponse.from(modelId, logs);
    }

    public List<RecentRestockResponse> getRecentRestocks() {
        List<RestockNotification> notifications = restockNotificationRepository.findTop10WithModelOrderByCreatedAtDesc();
        return notifications.stream()
            .map(RecentRestockResponse::from)
            .toList();
    }
}
