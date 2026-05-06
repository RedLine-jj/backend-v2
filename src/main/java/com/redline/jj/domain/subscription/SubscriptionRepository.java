package com.redline.jj.domain.subscription;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    boolean existsByUser_IdAndModel_Id(Long userId, Long modelId);

    @Query("""
        SELECT s FROM Subscription s
        JOIN FETCH s.user
        WHERE s.id = :id
        """)
    Optional<Subscription> findByIdWithUser(@Param("id") Long id);

    long countByUser_UserId(String userLoginId);

    @Query("""
        SELECT m.id        AS modelId,
               m.modelName AS modelName,
               COUNT(s)    AS count
        FROM Subscription s
        JOIN s.model m
        GROUP BY m.id, m.modelName
        ORDER BY COUNT(s) DESC
        LIMIT 10
        """)
    List<ModelSubscriptionCount> findTop10ModelsBySubscriptionCount();

    @Query("""
        SELECT s FROM Subscription s
        JOIN FETCH s.model m
        JOIN FETCH m.brand
        WHERE s.user.userId = :userLoginId
        AND (:cursor IS NULL OR s.id < :cursor)
        ORDER BY s.id DESC
        """)
    List<Subscription> findByUserWithCursor(@Param("userLoginId") String userLoginId,
                                             @Param("cursor") Long cursor,
                                             Pageable pageable);

    @Query("""
        SELECT s FROM Subscription s
        JOIN FETCH s.user
        WHERE s.model.id = :modelId
        """)
    List<Subscription> findByModel_Id(@Param("modelId") Long modelId);

    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM Subscription s")
    boolean existsAny();
}
