package com.redline.jj.domain.subscription;

/**
 * findTop10ModelsBySubscriptionCount 전용 Projection 인터페이스.
 * JPQL에서 SELECT new 없이 Spring Data Projection으로 컬럼을 직접 매핑한다.
 */
public interface ModelSubscriptionCount {

    Long getModelId();

    String getModelName();

    String getBrandName();
}
