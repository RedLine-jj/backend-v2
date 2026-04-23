# redline MVP PRD

**작성일**: 2026-04-23 | **상태**: Draft

---

## 1. 개요

**목적**: ModeMan·NestStore·SemiBasement 데님 편집샵의 재고·가격을 주기적으로 크롤링하고, LLM(Groq)으로 크로스사이트 동일 모델을 매칭하여, 재입고 감지 시 구독자에게 SSE로 실시간 알림을 전송한다.

**범위**

- 포함: JWT 회원인증, 크롤링 파이프라인(Cafe24 / imweb), Groq LLM 모델 매칭, 재고·가격 변동 이력, 재입고 SSE 알림, 모델 목록·상세·가격 비교·가격 추이, 구독 관리, 알림 읽음 처리
- 미포함: 소셜 로그인, 비밀번호 재설정 이메일, 데님 외 카테고리 확장, FCM·이메일 알림 채널

---

## 2. 핵심 기능

- 회원가입 시 bcrypt 암호화 저장, 로그인 시 액세스 토큰(30분)·리프레시 토큰(7일) 발급
- 브랜드·사이트·모델 타입 마스터 데이터 조회, 모델 목록(브랜드/타입 필터 + 커서 페이지네이션), 모델 상세(사이트별 옵션 포함)
- 모델 구독 등록·취소, 전체 기준 구독 TOP 10 조회
- Spring Batch로 ModeMan·NestStore(Cafe24)·SemiBasement(imweb) 크롤링, 3단계 모델 매칭(Exact → Alias 캐시 → Groq LLM, confidence >= 85%)
- `SiteOption.status` false→true 전환 감지 시 구독자 조회 후 Redis `restock` 채널 발행, `tb_restock_notification` 생성
- 인증된 사용자의 SSE 스트림 연결 시 Redis `restock` 채널 구독 → 재입고 이벤트 실시간 push
- 알림 목록·미읽음 수 조회, 단건·전체 읽음 처리, 가격 비교·30일 가격 추이 조회

---

## 3. API 명세

**공통**: Base Path `/api`, 모든 응답 `ApiResponse<T>` 래퍼, 인증 필요 경로는 `Authorization: Bearer {accessToken}` 헤더 필수

### 인증

| Method | Path | 설명 | 인증 |
|--------|------|------|------|
| POST | /api/auth/signup | 회원가입 | N |
| POST | /api/auth/login | 로그인 | N |
| POST | /api/auth/refresh | 액세스 토큰 재발급 | N |

**POST /api/auth/signup** Request: `userId(String)`, `userPw(String)`, `userName(String)`
**POST /api/auth/login** Response: `accessToken(String)`, `refreshToken(String)`
**POST /api/auth/refresh** Request: `refreshToken(String)` / Response: `accessToken(String)`

### 마스터 데이터

| Method | Path | 설명 | 인증 |
|--------|------|------|------|
| GET | /api/brands | 브랜드 목록 | N |
| GET | /api/sites | 사이트 목록 | N |
| GET | /api/models/types | 모델 타입 목록 | N |

### 모델

| Method | Path | 설명 | 인증 |
|--------|------|------|------|
| GET | /api/models | 모델 목록 | N |
| GET | /api/models/{id} | 모델 상세 (사이트별 옵션 포함) | N |
| GET | /api/models/count | 모델 수 | N |

**GET /api/models** Query: `brandIds[](Long)`, `types[](String)`, `cursor(Long)`, `size(int, default=20)`
**GET /api/models/{id}** Response: 모델 기본 정보 + `siteOptions[]` (사이트명, 옵션명, 가격, 재고 상태)

### 사이트 옵션

| Method | Path | 설명 | 인증 |
|--------|------|------|------|
| GET | /api/site-options | 옵션 목록 (siteId, modelId, status 필터) | N |
| GET | /api/site-options/{id} | 옵션 상세 | N |
| GET | /api/site-options/{id}/logs | 옵션 가격·재고 변동 이력 | N |

### 구독

| Method | Path | 설명 | 인증 |
|--------|------|------|------|
| GET | /api/subscriptions | 내 구독 목록 | Y |
| GET | /api/subscriptions/count | 내 구독 수 | Y |
| POST | /api/subscriptions | 구독 등록 | Y |
| DELETE | /api/subscriptions/{id} | 구독 취소 | Y |
| GET | /api/subscriptions/top | 전체 기준 구독 TOP 10 | N |

**POST /api/subscriptions** Request: `modelId(Long)`

### 알림 / SSE

| Method | Path | 설명 | 인증 |
|--------|------|------|------|
| GET | /api/notifications/stream | SSE 스트림 연결 | Y |
| GET | /api/notifications | 내 알림 목록 | Y |
| GET | /api/notifications/unread-count | 미읽음 수 | Y |
| PATCH | /api/notifications/{id}/read | 단건 읽음 처리 | Y |
| PATCH | /api/notifications/read-all | 전체 읽음 처리 | Y |

**GET /api/notifications/stream** Response: `text/event-stream`, 이벤트 페이로드 `{ modelId, modelName, brandName, optionName }`

### 대시보드

| Method | Path | 설명 | 인증 |
|--------|------|------|------|
| GET | /api/dashboard/price-comparison | 모델별 사이트×옵션 가격 비교 | N |
| GET | /api/dashboard/price-history | 모델별 최근 30일 가격 추이 | N |
| GET | /api/restocks/recent | 최근 재입고 10건 | N |

**GET /api/dashboard/price-comparison** Query: `modelId(Long)`
**GET /api/dashboard/price-history** Query: `modelId(Long)`, `days(int, default=30)`

### 배치 트리거

| Method | Path | 설명 | 인증 |
|--------|------|------|------|
| POST | /api/batch/crawl/{site} | 배치 수동 실행 | N |

`{site}`: `modeMan`, `nestStore`, `semiBasement`

### 주요 에러 코드

| 코드명 | HTTP | 메시지 |
|--------|------|--------|
| USER_ALREADY_EXISTS | 409 | 이미 사용 중인 아이디입니다 |
| USER_NOT_FOUND | 404 | 사용자를 찾을 수 없습니다 |
| INVALID_PASSWORD | 401 | 아이디 또는 비밀번호가 올바르지 않습니다 |
| TOKEN_EXPIRED | 401 | 만료된 토큰입니다 |
| TOKEN_INVALID | 401 | 유효하지 않은 토큰입니다 |
| RATE_LIMIT_EXCEEDED | 429 | 요청 한도를 초과했습니다 |
| MODEL_NOT_FOUND | 404 | 모델을 찾을 수 없습니다 |
| SITE_OPTION_NOT_FOUND | 404 | 사이트 옵션을 찾을 수 없습니다 |
| SUBSCRIPTION_ALREADY_EXISTS | 409 | 이미 구독 중인 모델입니다 |
| SUBSCRIPTION_NOT_FOUND | 404 | 구독 정보를 찾을 수 없습니다 |
| NOTIFICATION_NOT_FOUND | 404 | 알림을 찾을 수 없습니다 |
| NOTIFICATION_ACCESS_DENIED | 403 | 알림에 접근할 권한이 없습니다 |
| LLM_MATCHING_FAILED | 500 | LLM 모델 매칭에 실패했습니다 |
| CRAWLING_FAILED | 500 | 크롤링에 실패했습니다 |

---

## 4. 데이터 모델

모든 Entity는 `BaseEntity` 상속 (`createdAt`, `modifiedAt` 자동 관리).

### 엔티티 목록 및 관계

| 엔티티 | 테이블 | 역할 | 주요 관계 |
|--------|--------|------|-----------|
| User | tb_user | 회원 (아이디/비밀번호/이름) | Subscription, RestockNotification |
| Brand | tb_brand | 브랜드 마스터 | Model, BrandAlias |
| BrandAlias | tb_brand_alias | 브랜드 이름 별칭 (사이트별 표기 차이 대응) | Brand |
| Site | tb_site | 크롤링 대상 사이트 (플랫폼: CAFE24 / IMWEB) | SiteOption, ModelAlias |
| Model | tb_model | 크로스사이트 통합 모델 (타입: DENIM_PANTS / DENIM_JACKET) | Brand, SiteOption, Subscription |
| ModelAlias | tb_model_alias | 사이트별 상품명 → 통합 Model 매핑 캐시 (LLM 매칭 결과) | Model, Site |
| SiteOption | tb_site_option | 사이트별 사이즈 옵션 (재고 상태 · 가격) | Model, Site, SiteOptionLog |
| SiteOptionLog | tb_site_option_log | 가격·재고 변동 이력 (append-only) | SiteOption |
| Subscription | tb_subscription | 사용자-모델 구독 관계 (user+model 복합 유니크) | User, Model |
| RestockNotification | tb_restock_notification | 재입고 감지 시 생성되는 알림 (읽음 여부 포함) | User, Model |

### 핵심 비즈니스 규칙

- `SiteOption`의 재고 상태(false → true) 전환이 감지될 때 해당 모델 구독자 전원에게 `RestockNotification`이 생성된다.
- `ModelAlias`는 Groq LLM 매칭 confidence >= 85% 결과만 저장하며, 이후 동일 상품명 재크롤링 시 LLM 호출을 건너뛴다.
- `SiteOptionLog`는 배치 실행마다 append되며 삭제하지 않는다 (가격 추이 조회 원본 데이터).

---

## 5. 예외 처리

`ErrorCode` enum에 신규 추가 (기존 `INTERNAL_ERROR`, `INVALID_INPUT`, `NOT_FOUND`, `UNAUTHORIZED` 유지):

| 코드명 | HTTP 상태 | 코드값 | 메시지 |
|--------|-----------|--------|--------|
| USER_ALREADY_EXISTS | 409 CONFLICT | U001 | 이미 사용 중인 아이디입니다 |
| USER_NOT_FOUND | 404 NOT_FOUND | U002 | 사용자를 찾을 수 없습니다 |
| INVALID_PASSWORD | 401 UNAUTHORIZED | U003 | 아이디 또는 비밀번호가 올바르지 않습니다 |
| TOKEN_EXPIRED | 401 UNAUTHORIZED | U004 | 만료된 토큰입니다 |
| TOKEN_INVALID | 401 UNAUTHORIZED | U005 | 유효하지 않은 토큰입니다 |
| RATE_LIMIT_EXCEEDED | 429 TOO_MANY_REQUESTS | U006 | 요청 한도를 초과했습니다 |
| MODEL_NOT_FOUND | 404 NOT_FOUND | M001 | 모델을 찾을 수 없습니다 |
| SITE_OPTION_NOT_FOUND | 404 NOT_FOUND | M002 | 사이트 옵션을 찾을 수 없습니다 |
| SUBSCRIPTION_ALREADY_EXISTS | 409 CONFLICT | S001 | 이미 구독 중인 모델입니다 |
| SUBSCRIPTION_NOT_FOUND | 404 NOT_FOUND | S002 | 구독 정보를 찾을 수 없습니다 |
| NOTIFICATION_NOT_FOUND | 404 NOT_FOUND | N001 | 알림을 찾을 수 없습니다 |
| NOTIFICATION_ACCESS_DENIED | 403 FORBIDDEN | N002 | 알림에 접근할 권한이 없습니다 |
| LLM_MATCHING_FAILED | 500 INTERNAL_SERVER_ERROR | C001 | LLM 모델 매칭에 실패했습니다 |
| CRAWLING_FAILED | 500 INTERNAL_SERVER_ERROR | C002 | 크롤링에 실패했습니다 |

---

## 6. Redis 활용 전략

### 캐시 (@Cacheable 기반, 기본 TTL 10분)

| Redis 키 | Value | TTL | Evict 조건 |
|----------|-------|-----|------------|
| `brands` | `List<BrandResponse>` JSON | 10분 | 브랜드 데이터 변경 시 |
| `sites` | `List<SiteResponse>` JSON | 10분 | 사이트 데이터 변경 시 |
| `unread:{userId}` | Integer (미읽음 수) | 10분 | 읽음 처리·새 알림 생성 시 |

- `SiteOption` 캐시 무효화는 `@TransactionalEventListener(phase = AFTER_COMMIT)`로 처리 — 트랜잭션 롤백 시 캐시 오염 방지

### Pub/Sub

- 채널: `restock`
- 발행 페이로드: `{ "userId": Long, "modelId": Long, "modelName": String, "brandName": String }`
- 발행자: `DbSnapshotWriter` — `SiteOption.updateSnapshot()` 반환값이 true(재입고)일 때
- 구독자: `RestockSubscriber`(MessageListener) — `SseEmitterRepository`에서 해당 사용자 emitter 조회 후 `send()`

---

## 7. Spring Batch 구조

### Job 구성 (사이트별 3개)

`modeManCrawlingJob`, `nestStoreCrawlingJob`, `semiBasementCrawlingJob`

각 Job 구조:

```
{site}CrawlingJob
  └─ crawlingStep (chunk=50)
       ├─ CrawlItemReader    : ListParser 구현체 → 상품 URL 목록 페이지 순회
       ├─ CrawlItemProcessor : ModelResolutionProcessor (3단계 매칭) → SnapshotProcessor (변경 감지)
       └─ DbSnapshotWriter   : SiteOption upsert + 재입고 이벤트 publish
```

### 파서 인터페이스 (플랫폼 통일)

- `ListParser`: 사이트에서 상품 URL 목록 수집
- `DetailParser`: 상품명·브랜드명·사이즈 옵션·가격·재고 파싱
- Cafe24 구현체: HTML 파싱(Jsoup), imweb 구현체: OMS API 호출(WebClient)

### 모델 매칭 3단계 (ModelResolutionService)

1. Exact match: `tb_model`에서 브랜드명 + 상품명 정확히 일치
2. Alias 캐시: `tb_model_alias`에서 `(site_idx, site_model_name)` 조회
3. Groq LLM: 위 두 단계 미매칭 시에만 호출. confidence < 85% 이면 신규 모델로 저장, `tb_model_alias` 미기록

### 배치 스케줄러

- `BatchScheduler` (`@Scheduled`): `SubscriptionRepository.existsAny()` 결과에 따라 20분(구독자 있음) / 1시간(구독자 없음) 주기로 모든 사이트 Job 실행
- 수동 트리거: `POST /api/batch/crawl/{site}`

---

## 8. 의존성 추가 (build.gradle)

현재 뼈대에서 추가 필요한 의존성:

- `io.jsonwebtoken:jjwt-api`, `jjwt-impl`, `jjwt-jackson` (0.12.x)
- `org.springframework.boot:spring-boot-starter-security`
- `org.springframework.boot:spring-boot-starter-webflux` (WebClient — Groq·imweb 호출)
- `org.jsoup:jsoup` (Cafe24 HTML 파싱)
- `com.github.vladimir-bukhtoyarov:bucket4j-core` (Rate Limit, IP당 60 req/min)

---

## 9. 구현 순서 (Phase)

**Phase 1 — 기반**
1. build.gradle 의존성 추가
2. `ErrorCode` 14개 추가
3. Entity 10개 + Repository 전체 생성
4. `SecurityConfig` + JWT 유틸·필터
5. `AuthController` / `AuthService`

**Phase 2 — 조회 API**
6. 마스터 데이터 API (Brand, Site, ModelType)
7. `ModelController` / `ModelService` (목록·상세·카운트·타입)
8. `SiteOptionController` / `SiteOptionService` (목록·상세·이력)

**Phase 3 — 구독·알림**
9. `SubscriptionController` / `SubscriptionService`
10. 크롤링 파이프라인 (ListParser·DetailParser → Job 3개)
11. 재입고 감지 (`DbSnapshotWriter` + `RestockSubscriber` + SSE)
12. `NotificationController` / `NotificationService`

**Phase 4 — 부가 기능**
13. `DashboardController` / `DashboardService` (가격 비교·추이)
14. `BatchScheduler` (동적 주기 조정)
15. Rate Limit(Bucket4j) 적용
