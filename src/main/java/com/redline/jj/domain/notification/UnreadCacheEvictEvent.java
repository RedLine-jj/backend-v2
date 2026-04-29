package com.redline.jj.domain.notification;

public record UnreadCacheEvictEvent(Long userId, String loginId) {
}
