package com.redline.jj.api.option;

import com.redline.jj.api.option.dto.SiteOptionLogResponse;
import com.redline.jj.api.option.dto.SiteOptionResponse;
import com.redline.jj.common.exception.BusinessException;
import com.redline.jj.common.exception.ErrorCode;
import com.redline.jj.common.response.CursorPage;
import com.redline.jj.domain.option.SiteOptionLogRepository;
import com.redline.jj.domain.option.SiteOptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SiteOptionService {

    private final SiteOptionRepository siteOptionRepository;
    private final SiteOptionLogRepository siteOptionLogRepository;

    @Transactional(readOnly = true)
    public CursorPage<SiteOptionResponse> listSiteOptions(Long siteId, Long modelId,
                                                           Boolean status, Long cursor, int size) {
        PageRequest pageable = PageRequest.of(0, size + 1);
        List<SiteOptionResponse> fetched = siteOptionRepository
            .searchWithCursor(siteId, modelId, status, cursor, pageable)
            .stream()
            .map(SiteOptionResponse::from)
            .toList();

        boolean hasNext = fetched.size() > size;
        List<SiteOptionResponse> content = fetched.stream().limit(size).toList();
        Long nextCursor = hasNext ? content.get(content.size() - 1).getId() : null;
        return CursorPage.of(content, nextCursor, hasNext);
    }

    @Transactional(readOnly = true)
    public SiteOptionResponse getSiteOption(Long id) {
        return siteOptionRepository.findById(id)
            .map(SiteOptionResponse::from)
            .orElseThrow(() -> new BusinessException(ErrorCode.SITE_OPTION_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public CursorPage<SiteOptionLogResponse> getLogs(Long id, Long cursor, int size) {
        if (!siteOptionRepository.existsById(id)) {
            throw new BusinessException(ErrorCode.SITE_OPTION_NOT_FOUND);
        }
        PageRequest pageable = PageRequest.of(0, size + 1);
        List<SiteOptionLogResponse> fetched = siteOptionLogRepository
            .findBySiteOptionWithCursor(id, cursor, pageable)
            .stream()
            .map(SiteOptionLogResponse::from)
            .toList();

        boolean hasNext = fetched.size() > size;
        List<SiteOptionLogResponse> content = fetched.stream().limit(size).toList();
        Long nextCursor = hasNext ? content.get(content.size() - 1).getId() : null;
        return CursorPage.of(content, nextCursor, hasNext);
    }
}
