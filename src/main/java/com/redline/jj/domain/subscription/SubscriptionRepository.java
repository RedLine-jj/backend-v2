package com.redline.jj.domain.subscription;

import com.redline.jj.domain.model.Model;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    boolean existsByUser_IdAndModel_Id(Long userId, Long modelId);

    List<Subscription> findByUser_Id(Long userId);

    long countByUser_Id(Long userId);

    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM Subscription s")
    boolean existsAny();

    @Query("""
        SELECT s.model FROM Subscription s
        GROUP BY s.model
        ORDER BY COUNT(s) DESC
        LIMIT 10
        """)
    List<Model> findTop10ModelsBySubscriptionCount();
}
