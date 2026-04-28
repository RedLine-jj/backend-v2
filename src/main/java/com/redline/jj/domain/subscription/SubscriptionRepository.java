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
        WHERE s.user.userId = :userLoginId
        """)
    List<Subscription> findByUser_UserId(@Param("userLoginId") String userLoginId);

    long countByUser_UserId(String userLoginId);

    @Query("""
        SELECT m.id        AS modelId,
               m.modelName AS modelName,
               b.brandName AS brandName
        FROM Subscription s
        JOIN s.model m
        JOIN m.brand b
        GROUP BY m.id, m.modelName, b.brandName
        ORDER BY COUNT(s) DESC
        LIMIT 10
        """)
    List<ModelSubscriptionCount> findTop10ModelsBySubscriptionCount();

    @Query("""
        SELECT s FROM Subscription s
        JOIN FETCH s.user
        WHERE s.model.id = :modelId
        """)
    List<Subscription> findByModel_Id(@Param("modelId") Long modelId);
}
