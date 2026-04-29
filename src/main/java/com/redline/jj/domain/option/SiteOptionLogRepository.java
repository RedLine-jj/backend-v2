package com.redline.jj.domain.option;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface SiteOptionLogRepository extends JpaRepository<SiteOptionLog, Long> {

    List<SiteOptionLog> findBySiteOption_IdOrderByCreatedAtDesc(Long siteOptionId);

    @Query("""
        SELECT sol FROM SiteOptionLog sol
        JOIN FETCH sol.siteOption so
        JOIN FETCH so.site
        WHERE so.model.id = :modelId AND sol.capturedAt >= :since
        ORDER BY sol.capturedAt ASC
        """)
    List<SiteOptionLog> findByModelIdSince(
        @Param("modelId") Long modelId,
        @Param("since") LocalDateTime since
    );
}
