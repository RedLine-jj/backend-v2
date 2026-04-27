package com.redline.jj.domain.notification;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RestockNotificationRepository extends JpaRepository<RestockNotification, Long> {

    long countByUser_IdxAndReadFalse(Long userIdx);

    List<RestockNotification> findByUser_IdxOrderByCreatedAtDesc(Long userIdx);
}
