package com.redline.jj.api.option;

import com.redline.jj.api.option.dto.SiteOptionLogResponse;
import com.redline.jj.api.option.dto.SiteOptionResponse;
import com.redline.jj.common.exception.BusinessException;
import com.redline.jj.common.exception.ErrorCode;
import com.redline.jj.domain.option.SiteOptionLogRepository;
import com.redline.jj.domain.option.SiteOptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SiteOptionService {

    private final SiteOptionRepository siteOptionRepository;
    private final SiteOptionLogRepository siteOptionLogRepository;

    @Transactional(readOnly = true)
    public List<SiteOptionResponse> listSiteOptions(Long siteId, Long modelId, Boolean inStock) {
        return siteOptionRepository.search(siteId, modelId, inStock)
            .stream()
            .map(SiteOptionResponse::from)
            .toList();
    }

    @Transactional(readOnly = true)
    public SiteOptionResponse getSiteOption(Long id) {
        return siteOptionRepository.findById(id)
            .map(SiteOptionResponse::from)
            .orElseThrow(() -> new BusinessException(ErrorCode.SITE_OPTION_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public List<SiteOptionLogResponse> getLogs(Long id) {
        List<SiteOptionLogResponse> logs = siteOptionLogRepository
            .findBySiteOption_IdOrderByCreatedAtDesc(id)
            .stream()
            .map(SiteOptionLogResponse::from)
            .toList();
        if (logs.isEmpty() && !siteOptionRepository.existsById(id)) {
            throw new BusinessException(ErrorCode.SITE_OPTION_NOT_FOUND);
        }
        return logs;
    }
}
