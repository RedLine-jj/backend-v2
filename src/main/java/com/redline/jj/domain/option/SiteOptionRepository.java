package com.redline.jj.domain.option;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SiteOptionRepository extends JpaRepository<SiteOption, Long> {

    List<SiteOption> findByModel_IdOrderByIdAsc(Long modelId);

    @Query("SELECT so FROM SiteOption so " +
           "JOIN FETCH so.site " +
           "JOIN FETCH so.model " +
           "WHERE (:siteId IS NULL OR so.site.id = :siteId) " +
           "AND (:modelId IS NULL OR so.model.id = :modelId) " +
           "AND (:inStock IS NULL OR so.inStock = :inStock) " +
           "ORDER BY so.id DESC")
    List<SiteOption> search(@Param("siteId") Long siteId,
                            @Param("modelId") Long modelId,
                            @Param("inStock") Boolean inStock);

    @Query("SELECT so FROM SiteOption so " +
           "JOIN FETCH so.site " +
           "JOIN FETCH so.model m " +
           "JOIN FETCH m.brand " +
           "WHERE (:siteId IS NULL OR so.site.id = :siteId) " +
           "AND (:modelId IS NULL OR so.model.id = :modelId) " +
           "AND (:status IS NULL OR so.inStock = :status) " +
           "AND (:cursor IS NULL OR so.id < :cursor) " +
           "ORDER BY so.id DESC")
    List<SiteOption> searchWithCursor(@Param("siteId") Long siteId,
                                      @Param("modelId") Long modelId,
                                      @Param("status") Boolean status,
                                      @Param("cursor") Long cursor,
                                      Pageable pageable);

    Optional<SiteOption> findBySite_IdAndModel_IdAndOptionLabel(Long siteId, Long modelId, String optionLabel);

    @Query("SELECT so.model.id, MIN(so.price) FROM SiteOption so " +
           "WHERE so.model.id IN :ids AND so.inStock = true " +
           "GROUP BY so.model.id")
    List<Object[]> findLowestPricesByModelIds(@Param("ids") List<Long> ids);
}
