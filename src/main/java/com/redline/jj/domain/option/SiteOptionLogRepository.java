package com.redline.jj.domain.option;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SiteOptionLogRepository extends JpaRepository<SiteOptionLog, Long> {
    List<SiteOptionLog> findBySiteOption_IdOrderByCreatedAtDesc(Long siteOptionId);
}
