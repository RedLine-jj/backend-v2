package com.redline.jj.api.subscription;

import com.redline.jj.api.subscription.dto.SubscriptionResponse;
import com.redline.jj.api.subscription.dto.SubscriptionTopResponse;
import com.redline.jj.common.exception.BusinessException;
import com.redline.jj.common.exception.ErrorCode;
import com.redline.jj.domain.model.Model;
import com.redline.jj.domain.model.ModelRepository;
import com.redline.jj.domain.subscription.ModelSubscriptionCount;
import com.redline.jj.domain.subscription.Subscription;
import com.redline.jj.domain.subscription.SubscriptionRepository;
import com.redline.jj.domain.user.User;
import com.redline.jj.domain.user.UserFinder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private ModelRepository modelRepository;

    @Mock
    private UserFinder userFinder;

    @InjectMocks
    private SubscriptionService subscriptionService;

    // --- 공통 Mock 객체 ---
    @Mock
    private User user;

    @Mock
    private User otherUser;

    @Mock
    private Model model;

    @Mock
    private Subscription subscription;

    // =========================================================================
    // subscribe
    // =========================================================================

    @Test
    @DisplayName("subscribe: 정상 구독 저장 후 응답 반환")
    void subscribe_정상_구독저장후응답반환() {
        // given — 순서: user 조회 → 중복 체크 → model 조회 → 저장
        given(userFinder.getByLoginId("user1")).willReturn(user);
        given(user.getId()).willReturn(1L);
        given(subscriptionRepository.existsByUser_IdAndModel_Id(1L, 1L)).willReturn(false);
        given(modelRepository.findById(1L)).willReturn(Optional.of(model));
        given(subscriptionRepository.save(any(Subscription.class))).willReturn(subscription);

        // SubscriptionResponse.from() 에 필요한 최소 stub
        given(subscription.getUser()).willReturn(user);
        given(subscription.getModel()).willReturn(model);
        given(user.getUserId()).willReturn("user1");
        given(model.getId()).willReturn(1L);
        given(model.getModelName()).willReturn("모델명");
        com.redline.jj.domain.brand.Brand brandMock = mock(com.redline.jj.domain.brand.Brand.class);
        given(model.getBrand()).willReturn(brandMock);
        given(brandMock.getBrandName()).willReturn("브랜드명");

        // when
        SubscriptionResponse result = subscriptionService.subscribe("user1", 1L);

        // then
        verify(subscriptionRepository).save(any(Subscription.class));
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("subscribe: 이미 구독 중이면 S001 예외")
    void subscribe_이미구독중_S001예외() {
        // given — 순서 변경: user 조회 → 중복 체크(true) → 예외 발생, model 조회 불필요
        given(userFinder.getByLoginId("user1")).willReturn(user);
        given(user.getId()).willReturn(1L);
        given(subscriptionRepository.existsByUser_IdAndModel_Id(1L, 1L)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> subscriptionService.subscribe("user1", 1L))
            .isInstanceOf(BusinessException.class)
            .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.SUBSCRIPTION_ALREADY_EXISTS));
    }

    @Test
    @DisplayName("subscribe: 중복 구독 시 model 조회가 전혀 호출되지 않는다")
    void subscribe_중복시_model조회_never호출() {
        // given — user 조회 후 중복 체크에서 바로 예외, model 조회까지 도달하면 안 된다
        given(userFinder.getByLoginId("user1")).willReturn(user);
        given(user.getId()).willReturn(1L);
        given(subscriptionRepository.existsByUser_IdAndModel_Id(1L, 1L)).willReturn(true);

        // when
        assertThatThrownBy(() -> subscriptionService.subscribe("user1", 1L))
            .isInstanceOf(BusinessException.class);

        // then — 중복 체크 실패 이후 model 조회는 절대 호출되어선 안 된다
        verify(modelRepository, never()).findById(any());
    }

    @Test
    @DisplayName("subscribe: 존재하지 않는 모델이면 M001 예외")
    void subscribe_없는모델_M001예외() {
        // given — 순서 변경: user 조회 → 중복 체크(false) → model 조회(없음) → 예외
        given(userFinder.getByLoginId("user1")).willReturn(user);
        given(user.getId()).willReturn(1L);
        given(subscriptionRepository.existsByUser_IdAndModel_Id(1L, 999L)).willReturn(false);
        given(modelRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> subscriptionService.subscribe("user1", 999L))
            .isInstanceOf(BusinessException.class)
            .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.MODEL_NOT_FOUND));
    }

    // =========================================================================
    // cancel
    // =========================================================================

    @Test
    @DisplayName("cancel: 정상 취소 시 로드된 엔티티로 delete 호출")
    void cancel_정상_삭제호출() {
        // given
        given(subscriptionRepository.findByIdWithUser(1L)).willReturn(Optional.of(subscription));
        given(subscription.getUser()).willReturn(user);
        given(user.getUserId()).willReturn("user1");

        // when
        subscriptionService.cancel("user1", 1L);

        // then
        verify(subscriptionRepository).delete(subscription);
    }

    @Test
    @DisplayName("cancel: 타인 구독 취소 시도 시 S003(SUBSCRIPTION_ACCESS_DENIED, 403) 예외")
    void cancel_타인구독_S003예외() {
        // given — 구독은 존재하지만 소유자가 다른 사람이다
        given(subscriptionRepository.findByIdWithUser(1L)).willReturn(Optional.of(subscription));
        given(subscription.getUser()).willReturn(otherUser);
        given(otherUser.getUserId()).willReturn("other");

        // when & then — S002(NOT_FOUND)가 아니라 반드시 S003(ACCESS_DENIED)이어야 한다
        assertThatThrownBy(() -> subscriptionService.cancel("user1", 1L))
            .isInstanceOf(BusinessException.class)
            .satisfies(e -> {
                BusinessException be = (BusinessException) e;
                assertThat(be.getErrorCode()).isEqualTo(ErrorCode.SUBSCRIPTION_ACCESS_DENIED);
                assertThat(be.getErrorCode().getCode()).isEqualTo("S003");
                assertThat(be.getErrorCode().getStatus().value()).isEqualTo(403);
            });
    }

    @Test
    @DisplayName("cancel: 존재하지 않는 구독 취소 시도 시 S002(SUBSCRIPTION_NOT_FOUND, 404) 예외")
    void cancel_없는구독_S002예외() {
        // given — 구독 자체가 없다 (S003과 명확히 다른 케이스)
        given(subscriptionRepository.findByIdWithUser(999L)).willReturn(Optional.empty());

        // when & then — S003(ACCESS_DENIED)이 아니라 반드시 S002(NOT_FOUND)이어야 한다
        assertThatThrownBy(() -> subscriptionService.cancel("user1", 999L))
            .isInstanceOf(BusinessException.class)
            .satisfies(e -> {
                BusinessException be = (BusinessException) e;
                assertThat(be.getErrorCode()).isEqualTo(ErrorCode.SUBSCRIPTION_NOT_FOUND);
                assertThat(be.getErrorCode().getCode()).isEqualTo("S002");
                assertThat(be.getErrorCode().getStatus().value()).isEqualTo(404);
            });
    }

    // =========================================================================
    // getTop10
    // =========================================================================

    @Test
    @DisplayName("getTop10: Projection 결과가 SubscriptionTopResponse로 1:1 변환된다")
    void getTop10_Projection_DTO_변환() {
        // given — Projection 인터페이스 Mock 1개로 변환 로직 집중 검증
        ModelSubscriptionCount projection = mock(ModelSubscriptionCount.class);
        given(projection.getModelId()).willReturn(42L);
        given(projection.getModelName()).willReturn("501 Original");
        given(projection.getBrandName()).willReturn("Levi's");

        given(subscriptionRepository.findTop10ModelsBySubscriptionCount()).willReturn(List.of(projection));

        // when
        List<SubscriptionTopResponse> result = subscriptionService.getTop10();

        // then — Projection의 각 getter가 DTO 필드에 누락 없이 매핑됐는지 검증한다
        assertThat(result).hasSize(1);
        assertThat(result.get(0))
            .satisfies(dto -> {
                assertThat(dto.getModelId()).isEqualTo(42L);
                assertThat(dto.getModelName()).isEqualTo("501 Original");
                assertThat(dto.getBrandName()).isEqualTo("Levi's");
            });
    }

    @Test
    @DisplayName("getTop10: 구독 TOP10 모델 목록 10개 반환")
    void getTop10_구독TOP10반환() {
        // given — Projection 인터페이스 Mock 10개
        List<ModelSubscriptionCount> top10 = IntStream.rangeClosed(1, 10)
            .mapToObj(i -> {
                ModelSubscriptionCount projection = mock(ModelSubscriptionCount.class);
                given(projection.getModelId()).willReturn((long) i);
                given(projection.getModelName()).willReturn("모델" + i);
                given(projection.getBrandName()).willReturn("브랜드" + i);
                return projection;
            })
            .toList();

        given(subscriptionRepository.findTop10ModelsBySubscriptionCount()).willReturn(top10);

        // when
        List<SubscriptionTopResponse> result = subscriptionService.getTop10();

        // then
        assertThat(result).hasSize(10);
        assertThat(result.get(0).getModelId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("getTop10: 구독이 없으면 빈 리스트를 반환한다")
    void getTop10_구독없으면_빈리스트반환() {
        given(subscriptionRepository.findTop10ModelsBySubscriptionCount()).willReturn(List.of());

        List<SubscriptionTopResponse> result = subscriptionService.getTop10();

        assertThat(result).isEmpty();
    }
}
