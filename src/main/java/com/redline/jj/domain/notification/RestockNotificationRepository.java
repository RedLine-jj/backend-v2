package com.redline.jj.domain.notification;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RestockNotificationRepository extends JpaRepository<RestockNotification, Long> {

    long countByUser_IdAndReadFalse(Long userId);

    List<RestockNotification> findByUser_IdOrderByCreatedAtDesc(Long userId);

    @EntityGraph(attributePaths = {"model", "model.brand"})
    List<RestockNotification> findTop10ByOrderByCreatedAtDesc();

    @Modifying(clearAutomatically = true)
    @Query("UPDATE RestockNotification n SET n.read = true WHERE n.user.id = :userId AND n.read = false")
    int markAllAsReadByUserId(@Param("userId") Long userId);
}
