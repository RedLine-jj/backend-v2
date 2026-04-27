package com.redline.jj.domain.option;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SiteOptionRepository extends JpaRepository<SiteOption, Long> {

    List<SiteOption> findByModel_IdOrderByIdAsc(Long modelId);

    @Query("SELECT so FROM SiteOption so " +
           "WHERE (:siteId IS NULL OR so.site.id = :siteId) " +
           "AND (:modelId IS NULL OR so.model.id = :modelId) " +
           "AND (:inStock IS NULL OR so.inStock = :inStock) " +
           "ORDER BY so.id DESC")
    List<SiteOption> search(@Param("siteId") Long siteId,
                            @Param("modelId") Long modelId,
                            @Param("inStock") Boolean inStock);
}
