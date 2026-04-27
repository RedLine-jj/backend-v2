package com.redline.jj.domain.subscription;

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

    @Query("""
        SELECT s FROM Subscription s
        JOIN FETCH s.user
        JOIN FETCH s.model m
        JOIN FETCH m.brand
        WHERE s.user.id = :userId
        """)
    List<Subscription> findByUser_Id(@Param("userId") Long userId);

    long countByUser_Id(Long userId);

    @Query("""
        SELECT s.model.id      AS modelId,
               s.model.modelName AS modelName,
               s.model.brand.brandName AS brandName
        FROM Subscription s
        JOIN s.model m
        JOIN m.brand b
        GROUP BY s.model.id, s.model.modelName, s.model.brand.brandName
        ORDER BY COUNT(s) DESC
        LIMIT 10
        """)
    List<ModelSubscriptionCount> findTop10ModelsBySubscriptionCount();
}
