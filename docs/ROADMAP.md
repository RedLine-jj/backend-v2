# ROADMAP.md — redline MVP

> 순서: 아키텍처(M1~M2) → 공통(M3~M5) → 개별 기능(M6~M9)
> 원칙: 각 서브작업 구현 완료 후 반드시 테스트 실행, 다음 작업 진행

---

## M1. 아키텍처 기반 — 의존성 · ErrorCode · Entity · Repository

### M1-1. build.gradle 의존성 추가

**구현**
- [x] `io.jsonwebtoken:jjwt-api:0.12.x`, `jjwt-impl`, `jjwt-jackson` 추가
- [x] `spring-boot-starter-security` 추가
- [x] `spring-boot-starter-webflux` 추가 (WebClient)
- [x] `org.jsoup:jsoup` 추가
- [x] `com.bucket4j:bucket4j-core` 추가
- [x] `org.testcontainers:redis`, `testcontainers` 추가 (testImplementation)
- [x] `com.squareup.okhttp3:mockwebserver` 추가 (testImplementation, Groq·imweb Mock용)

**테스트 실행**
- [x] `./gradlew clean build -x test` — 빌드 성공 확인

---

### M1-2. ErrorCode 14개 추가

**구현** (`com.redline.jj.common.exception.ErrorCode`)
- [x] U001 `USER_ALREADY_EXISTS` — 409 CONFLICT
- [x] U002 `USER_NOT_FOUND` — 404 NOT_FOUND
- [x] U003 `INVALID_PASSWORD` — 401 UNAUTHORIZED
- [x] U004 `TOKEN_EXPIRED` — 401 UNAUTHORIZED
- [x] U005 `TOKEN_INVALID` — 401 UNAUTHORIZED
- [x] U006 `RATE_LIMIT_EXCEEDED` — 429 TOO_MANY_REQUESTS
- [x] M001 `MODEL_NOT_FOUND` — 404 NOT_FOUND
- [x] M002 `SITE_OPTION_NOT_FOUND` — 404 NOT_FOUND
- [x] S001 `SUBSCRIPTION_ALREADY_EXISTS` — 409 CONFLICT
- [x] S002 `SUBSCRIPTION_NOT_FOUND` — 404 NOT_FOUND
- [x] N001 `NOTIFICATION_NOT_FOUND` — 404 NOT_FOUND
- [x] N002 `NOTIFICATION_ACCESS_DENIED` — 403 FORBIDDEN
- [x] C001 `LLM_MATCHING_FAILED` — 500 INTERNAL_SERVER_ERROR
- [x] C002 `CRAWLING_FAILED` — 500 INTERNAL_SERVER_ERROR

**단위 테스트** (`ErrorCodeTest`)
- [x] 각 ErrorCode의 httpStatus·code·message 값이 PRD 명세와 일치하는지 파라미터화 검증
- [x] `GlobalExceptionHandler`가 `BusinessException(ErrorCode.U001)` 수신 시 HTTP 409 + `ApiResponse.fail` 반환 확인 (`@WebMvcTest` + MockMvc)
- [x] 존재하지 않는 코드값으로 조회 시 NPE 미발생

**테스트 실행**
- [x] `./gradlew test --tests "com.redline.jj.common.exception.ErrorCodeTest"`

---

### M1-3. Entity 10개 + Repository 생성

**구현** (패키지: `com.redline.jj.domain`)

모든 Entity: `BaseEntity` 상속, 테이블명 `tb_` 접두어, PK `id` (AI BIGINT)

- [x] `user/User.java` — tb_user, unique: `user_id`(String)
- [x] `user/UserRepository.java` — `findByUserId(String)`
- [x] `brand/Brand.java` — tb_brand, unique: `brand_name`
- [x] `brand/BrandRepository.java`
- [x] `brand/BrandAlias.java` — tb_brand_alias, unique: `(brand_idx, alias_name)`
- [x] `brand/BrandAliasRepository.java` — `findByAliasName(String)`
- [x] `site/Site.java` — tb_site, unique: `site_name`; enum: `platform` (CAFE24/IMWEB)
- [x] `site/SiteRepository.java`
- [x] `model/Model.java` — tb_model, unique: `(brand_idx, model_name)`; enum: `type` (DENIM_PANTS/DENIM_JACKET)
- [x] `model/ModelRepository.java` — 커서 페이지네이션 쿼리 메서드 포함
- [x] `model/ModelAlias.java` — tb_model_alias, unique: `(site_idx, site_model_name)`
- [x] `model/ModelAliasRepository.java` — `findBySiteIdxAndSiteModelName(Long, String)`
- [x] `option/SiteOption.java` — tb_site_option, unique: `(model_idx, site_idx, option_name)`; `updateSnapshot(boolean inStock, int price)` 메서드 (변경 여부 반환)
- [x] `option/SiteOptionRepository.java`
- [x] `option/SiteOptionLog.java` — tb_site_option_log (append-only)
- [x] `option/SiteOptionLogRepository.java`
- [x] `subscription/Subscription.java` — tb_subscription, unique: `(user_idx, model_idx)`
- [x] `subscription/SubscriptionRepository.java` — `existsAny()`, `findTop10ModelsBySubscriptionCount()`
- [x] `notification/RestockNotification.java` — tb_restock_notification; `isRead` boolean
- [x] `notification/RestockNotificationRepository.java` — `countByUserIdxAndIsReadFalse(Long)`

**통합 테스트** (`@DataJpaTest`, H2)
- [x] `UserRepositoryTest` — `findByUserId` 존재/미존재 케이스
- [x] `ModelAliasRepositoryTest` — `findBySiteIdxAndSiteModelName` 복합키 조회
- [x] `SubscriptionRepositoryTest`
  - `existsAny()`: 구독 있음 → true, 없음 → false
  - unique 제약: 동일 (user_idx, model_idx) insert 시 예외
- [x] `SiteOptionTest` (단위)
  - `updateSnapshot(false→true)`: 반환 true, status=true, price 갱신
  - `updateSnapshot(true→true)`: 반환 false, price 변동 없으면 그대로
  - `updateSnapshot(true→false)`: 반환 false (재입고 아님), status=false
  - `updateSnapshot(false→false)`: 반환 false
  - `updateSnapshot(가격 변경만)`: 반환 false, price 갱신

**테스트 실행**
- [x] `./gradlew test --tests "com.redline.jj.domain...*"`

---

## M2. 아키텍처 기반 — Spring Security · JWT

### M2-1. JwtUtil

**구현** (`com.redline.jj.config.security.JwtUtil`)
- [x] `generateAccessToken(String userId)` — 만료 30분
- [x] `generateRefreshToken(String userId)` — 만료 7일
- [x] `getUserId(String token)` — Claims에서 subject 추출
- [x] `isExpired(String token)` — 만료 여부
- [x] `validate(String token)` — 서명·포맷 유효성 (실패 시 `BusinessException(TOKEN_INVALID)`)

**단위 테스트** (`JwtUtilTest`)
- [x] 정상 accessToken 생성 후 `getUserId()` — 동일 userId 반환
- [x] 정상 refreshToken 생성 후 `getUserId()` — 동일 userId 반환
- [x] accessToken 만료(30분+1초 후) — `isExpired()` true
- [x] 만료 토큰 `validate()` 호출 — `TOKEN_EXPIRED` (U004) 예외
- [x] 서명 키 불일치 토큰 — `TOKEN_INVALID` (U005) 예외
- [x] 임의 문자열 토큰 — `TOKEN_INVALID` (U005) 예외
- [x] 빈 문자열 토큰 — `TOKEN_INVALID` (U005) 예외
- [x] accessToken과 refreshToken의 만료 시간 차이가 6일 이상인지 확인

**테스트 실행**
- [x] `./gradlew test --tests "com.redline.jj.config.security.JwtUtilTest"`

---

### M2-2. SecurityConfig · JwtAuthenticationFilter · CustomUserDetailsService

**구현**
- [x] `SecurityConfig.java` — CSRF 비활성화, Stateless 세션, permitAll 경로 설정
  - permitAll: `POST /api/auth/**`, `GET /api/brands`, `GET /api/sites`, `GET /api/models/**`, `GET /api/site-options/**`, `GET /api/subscriptions/top`, `GET /api/dashboard/**`, `GET /api/restocks/recent`, `POST /api/batch/crawl/**`, `GET /swagger-ui/**`, `GET /v3/api-docs/**`
- [x] `JwtAuthenticationFilter.java` — `OncePerRequestFilter`
  - `Authorization: Bearer {token}` 파싱 → `JwtUtil.validate()` → `SecurityContextHolder` 주입
  - 토큰 없음·형식 오류·만료: SecurityContext 미주입 (다음 필터로 통과)
- [x] `CustomUserDetailsService.java` — `UserRepository.findByUserId()`로 조회, 미존재 시 `USER_NOT_FOUND` (U002)

**단위 테스트** (`JwtAuthenticationFilterTest`, `@WebMvcTest` 슬라이스)
- [x] 유효한 Bearer 토큰 → `SecurityContextHolder`에 userId 주입, 인증 필요 경로 200
- [x] Bearer 토큰 없음 → SecurityContext 비어있음, 공개 경로 200, 보호 경로 401
- [x] 만료 토큰 → SecurityContext 미주입, 보호 경로 401
- [x] 위조(서명 불일치) 토큰 → SecurityContext 미주입, 보호 경로 401

**테스트 실행**
- [x] `./gradlew test --tests "com.redline.jj.config.security.*"`

---

## M3. 공통 — 인증 API

### M3-1. AuthController · AuthService

**구현**
- [x] `com.redline.jj.api.auth.AuthController` — `POST /api/auth/signup`, `POST /api/auth/login`, `POST /api/auth/refresh`
- [x] `com.redline.jj.api.auth.AuthService`
- [x] `SignupRequest`, `LoginRequest`, `LoginResponse`, `RefreshRequest`, `RefreshResponse` DTO (Bean Validation 포함)

**단위 테스트** (`AuthServiceTest`, Mockito)
- [x] `signup` — 정상: bcrypt 암호화 후 저장, 원문 비밀번호 DB 미저장 확인 (`passwordEncoder.encode()` 호출 verify)
- [x] `signup` — 이미 존재하는 userId → `USER_ALREADY_EXISTS` (U001)
- [x] `login` — 정상: accessToken·refreshToken 비어있지 않게 반환
- [x] `login` — 없는 userId → `INVALID_PASSWORD` (U003) (userId 존재 여부 노출 방지)
- [x] `login` — 잘못된 비밀번호 → `INVALID_PASSWORD` (U003)
- [x] `login` — `passwordEncoder.matches()` 호출 횟수 1회 verify
- [x] `refresh` — 정상 refreshToken → 새 accessToken 반환
- [x] `refresh` — 만료된 refreshToken → `TOKEN_EXPIRED` (U004)
- [x] `refresh` — 위조 토큰 → `TOKEN_INVALID` (U005)
- [x] `refresh` — accessToken을 refreshToken 자리에 → `TOKEN_INVALID` (U005)

**컨트롤러 테스트** (`AuthControllerTest`, `@WebMvcTest`)
- [x] `POST /api/auth/signup` — 정상 요청 201 또는 200
- [x] `POST /api/auth/signup` — 필수 필드 누락 → 400
- [x] `POST /api/auth/login` — 정상 요청 → accessToken·refreshToken 포함 응답
- [x] `POST /api/auth/refresh` — 정상 요청 → accessToken 포함 응답

**테스트 실행**
- [x] `./gradlew test --tests "com.redline.jj.api.auth.*"`

---

## M4. 공통 — 마스터 데이터 API

### M4-1. BrandController · SiteController · ModelTypeController

**구현**
- [x] `GET /api/brands` — `@Cacheable("brands")`, `List<BrandResponse>` 반환
- [x] `GET /api/sites` — `@Cacheable("sites")`, `List<SiteResponse>` 반환
- [x] `GET /api/models/types` — ModelType enum 목록 반환 (캐시 불필요)

**단위 테스트** (`MasterDataServiceTest`, Mockito)
- [x] `getBrands()` 첫 호출: `BrandRepository.findAll()` 1회 호출
- [x] `getBrands()` 두 번째 호출: `BrandRepository.findAll()` 미호출 (캐시 hit)
- [x] `getSites()` 동일 패턴

**통합 테스트** (`@SpringBootTest` + 로컬 Redis)
- [x] 첫 조회 후 Redis에 `brands` 키 존재 확인
- [x] 캐시 TTL 10분 설정 확인
- [x] `@CacheEvict("brands")` 호출 후 캐시 키 삭제 확인

**테스트 실행**
- [x] `./gradlew test --tests "com.redline.jj.api.master.*"`

---

## M5. 공통 — 모델 · 사이트옵션 조회 API

### M5-1. ModelController · ModelService

**구현**
- [x] `GET /api/models` — `brandIds[]`, `types[]` 필터 + 커서 페이지네이션 (`cursor`, `size=20`)
- [x] `GET /api/models/{id}` — 모델 기본 정보 + `siteOptions[]` (사이트명·옵션명·가격·재고 상태); 미존재 시 M001
- [x] `GET /api/models/count` — 전체 모델 수

**단위 테스트** (`ModelServiceTest`, Mockito)
- [x] `getModels(null, null, null, 20)` — 필터 없이 전체 조회
- [x] `getModels([brandId], null, null, 20)` — 브랜드 필터 적용
- [x] `getModels(null, [DENIM_PANTS], null, 20)` — 타입 필터 적용
- [x] `getModels(null, null, cursor=5, 20)` — cursor 이후 20개 반환
- [x] `getModels(null, null, cursor=마지막, 20)` — 빈 리스트 반환 (다음 페이지 없음)
- [x] `getModel(존재하는 id)` — siteOptions 포함 반환
- [x] `getModel(없는 id)` — `MODEL_NOT_FOUND` (M001)

**컨트롤러 테스트** (`@WebMvcTest`)
- [x] `GET /api/models?brandIds=1,2&types=DENIM_PANTS&cursor=10&size=5` — 파라미터 바인딩 정상
- [x] `GET /api/models/999` — 404 M001

**테스트 실행**
- [x] `./gradlew test --tests "com.redline.jj.api.model.*"`

---

### M5-2. SiteOptionController · SiteOptionService

**구현**
- [x] `GET /api/site-options` — `siteId`, `modelId`, `inStock` 필터
- [x] `GET /api/site-options/{id}` — 옵션 상세; 미존재 시 M002
- [x] `GET /api/site-options/{id}/logs` — 가격·재고 변동 이력 목록

**단위 테스트** (`SiteOptionServiceTest`, Mockito)
- [x] 각 필터 단독·조합 적용 시 Repository 호출 파라미터 verify
- [x] `getSiteOption(없는 id)` — `SITE_OPTION_NOT_FOUND` (M002)
- [x] `getLogs(id)` — SiteOptionLog 최신순 반환

**테스트 실행**
- [x] `./gradlew test --tests "com.redline.jj.api.option.*"`
- [x] `./gradlew test` — M1~M5 전체 그린 확인

---

## M6. 개별 기능 — 구독 API

### M6-1. SubscriptionController · SubscriptionService

**구현**
- [x] `GET /api/subscriptions` — 내 구독 목록 (인증 필요)
- [x] `GET /api/subscriptions/count` — 내 구독 수 (인증 필요)
- [x] `POST /api/subscriptions` — 구독 등록, Request: `modelId(Long)` (인증 필요)
- [x] `DELETE /api/subscriptions/{id}` — 구독 취소 (인증 필요)
- [x] `GET /api/subscriptions/top` — 전체 기준 구독 TOP 10 (인증 불필요)

**단위 테스트** (`SubscriptionServiceTest`, Mockito)
- [x] `subscribe(userId, modelId)` — 정상: `Subscription` 1건 저장, 저장 결과 반환
- [x] `subscribe(userId, modelId)` — 이미 구독 중 → `SUBSCRIPTION_ALREADY_EXISTS` (S001)
- [x] `subscribe(userId, 없는 modelId)` — `MODEL_NOT_FOUND` (M001)
- [x] `cancel(userId, subscriptionId)` — 본인 구독 취소 정상
- [x] `cancel(userId, 타인의 subscriptionId)` — `SUBSCRIPTION_ACCESS_DENIED` (S003) (403 반환, S002→S003으로 변경)
- [x] `cancel(userId, 없는 subscriptionId)` — `SUBSCRIPTION_NOT_FOUND` (S002)
- [x] `getTop10()` — 구독 수 기준 내림차순 TOP 10 반환, 11번째 미포함 확인
- [ ] `existsAny()` — 구독 1건 이상: true / 0건: false (M9 BatchScheduler 구현 시 함께 추가)

**컨트롤러 테스트** (`@WebMvcTest`)
- [x] `POST /api/subscriptions` 비인증 → 401
- [x] `DELETE /api/subscriptions/{id}` 비인증 → 401
- [x] `GET /api/subscriptions/top` 비인증 → 200 (공개 API)

**테스트 실행**
- [x] `./gradlew test --tests "com.redline.jj.api.subscription.*"`

---

## M7. 개별 기능 — 크롤링 파이프라인

### M7-1. 파서 인터페이스 · 구현체

**구현** (`com.redline.jj.batch.crawler`)
- [x] `ListParser` 인터페이스 — `List<String> parseProductUrls(int page)` 상품 URL 목록 반환
- [x] `DetailParser` 인터페이스 — `CrawledProduct parse(String url)` 상품 상세 파싱, `CrawledProduct` DTO 정의
- [x] `ModeManListParser` / `ModeManDetailParser` — Jsoup HTML 파싱 (Cafe24)
- [x] `NestStoreListParser` / `NestStoreDetailParser` — Jsoup HTML 파싱 (Cafe24)
- [x] `SemiBasementListParser` / `SemiBasementDetailParser` — WebClient OMS API 호출 (imweb)

**단위 테스트** (MockWebServer / WireMock 활용)
- [x] `ModeManDetailParserTest` — 정상 HTML 응답 → `CrawledProduct` 필드(상품명·브랜드명·옵션·가격·재고) 파싱 확인
- [x] `ModeManDetailParserTest` — 상품 품절 HTML → `inStock=false` 파싱
- [x] `SemiBasementDetailParserTest` — 정상 OMS JSON 응답 → `CrawledProduct` 파싱
- [x] 파싱 중 HTML 구조 변경(누락 요소) → BusinessException 발생 확인 (C002 연계)

**테스트 실행**
- [x] `./gradlew test --tests "com.redline.jj.batch.crawler.*"`

---

### M7-2. ModelResolutionService (3단계 매칭)

**구현** (`com.redline.jj.batch.matching.ModelResolutionService`)
- [x] 1단계: `ModelRepository`에서 brandName + modelName Exact match
- [x] 2단계: `ModelAliasRepository`에서 `(siteIdx, siteModelName)` 조회
- [x] 3단계: Groq LLM WebClient 호출 — confidence >= 85%: `ModelAlias` 저장·Model 반환 / < 85%: 신규 Model 저장, ModelAlias 미저장
- [x] `BrandAliasRepository`로 사이트 표기 브랜드명 정규화

**단위 테스트** (`ModelResolutionServiceTest` + `GroqLlmClientTest`, Mockito + MockWebServer)
- [x] **Exact match 성공**: `ModelRepository` 1회 조회, `ModelAliasRepository` 미호출, LLM 미호출
- [x] **Alias 캐시 hit**: `ModelAliasRepository` 조회 후 바로 반환, LLM 미호출 (verify 0회)
- [x] **Exact miss + Alias miss → LLM 호출**: LLM 매칭 성공 시 Alias 저장 후 Model 반환
- [x] **LLM confidence >= 85%**: `ModelAlias` 저장 1회, 이미 존재 시 저장 미호출
- [x] **LLM confidence < 85%**: 신규 Model 저장, `ModelAlias` 미저장
- [x] **LLM 타임아웃**: `LLM_MATCHING_FAILED` (C001) 예외 (`GroqLlmClientTest`)
- [x] **LLM 5xx 응답**: `LLM_MATCHING_FAILED` (C001) 예외
- [x] **LLM JSON 파싱 실패**: `LLM_MATCHING_FAILED` (C001) 예외
- [x] **브랜드 별칭 정규화**: `BrandAlias`에 등록된 표기 → 정규 브랜드명으로 변환 후 매칭 진행
- [x] **동일 (site, siteModelName) ModelAlias 중복 저장 방지**: 이미 존재 시 저장 스킵

**테스트 실행**
- [x] `./gradlew test --tests "com.redline.jj.batch.matching.*"`

---

### M7-3. Spring Batch Job 3개 · DbSnapshotWriter

**구현** (`com.redline.jj.batch`)
- [x] `CrawlItemReader` — `ListParser` 구현체로 상품 URL 페이지 순회, chunk=50
- [x] `ModelResolutionProcessor` — `CrawledProduct` → `ResolvedItem` (매칭된 Model + 옵션 정보)
- [x] `DbSnapshotWriter` — `SiteOption` upsert, `SiteOptionLog` append, 재입고 시 `RestockEvent` publish (`SnapshotProcessor` 기능 통합)
- [x] `modeManCrawlingJob`, `nestStoreCrawlingJob`, `semiBasementCrawlingJob` 빈 등록 (`CrawlingJobConfig`)
- [x] `BatchController` — `POST /api/batch/crawl/{site}` 수동 트리거

**단위 테스트** (`DbSnapshotWriterTest` + `RestockEventHandlerTest`, Mockito)
- [x] `false → true` 전환: `SiteOptionLog` append 1건, `RestockEvent` publish 1회
- [x] `true → true` 유지 (가격 동일): publish 미발행, Log 미append
- [x] `true → true` (가격 변동): publish 미발행, Log append 1건
- [x] `true → false` 전환(품절): publish 미발행, Log append 1건
- [x] `false → false` 유지: publish 미발행, Log 미append
- [x] `RestockEvent` 페이로드: `modelId`, `modelName`, `brandName` 값 검증
- [x] 구독자 N명 재입고: `RestockNotification` N건 생성 (1명·2명 케이스로 검증)
- [x] 구독자 없음: save·publishEvent·convertAndSend 모두 미호출
- [x] **트랜잭션 롤백 시**: AFTER_COMMIT 리스너 미호출로 캐시 evict 없음 (`NotificationServiceCacheTest`에서 검증)

**배치 통합 테스트** (`@SpringBatchTest`)
- [ ] `modeManCrawlingJob` 실행 → `BatchStatus.COMPLETED`
- [ ] 파싱 실패 아이템: skip 처리, `CRAWLING_FAILED` 로그, 나머지 아이템 계속 처리

**컨트롤러 테스트** (`BatchControllerTest`, `@WebMvcTest`)
- [x] `POST /api/batch/crawl/modeMan` — 200
- [x] `POST /api/batch/crawl/invalidSite` — 400

**테스트 실행**
- [x] `./gradlew test --tests "com.redline.jj.batch.*"`

---

## M8. 개별 기능 — 재입고 SSE · 알림 API

### M8-1. SseEmitterRepository · RestockSubscriber

**구현** (`com.redline.jj.api.notification`)
- [x] `SseEmitterRepository` — `ConcurrentHashMap<Long, SseEmitter>` 기반
  - `add(Long userId, SseEmitter emitter)`: 기존 emitter 존재 시 `complete()` 후 교체
  - `remove(Long userId)`
  - `get(Long userId)`: Optional 반환
- [x] `RestockSubscriber` (`MessageListener`) — Redis `restock` 채널 수신, 페이로드 역직렬화 → 해당 userId emitter.send()
- [x] `RedisPubSubConfig`에 `RestockSubscriber` 등록 (`addMessageListener`)

**단위 테스트** (`SseEmitterRepositoryTest`)
- [x] `add(userId, emitter)` — get으로 동일 emitter 반환
- [x] `add(userId, newEmitter)` (동일 userId 재연결) — 기존 emitter `complete()` 호출 verify, newEmitter로 교체
- [x] `remove(userId)` — 이후 `get()` → empty
- [x] `remove(없는 userId)` — 예외 미발생

**단위 테스트** (`RestockSubscriberTest`, Mockito)
- [x] 정상 메시지 수신 → 해당 userId `emitter.send()` 1회 호출
- [x] 메시지의 userId에 해당 emitter 없음 → no-op, 예외 미발생
- [x] `emitter.send()` IOException 발생 → `SseEmitterRepository.remove()` 호출
- [x] 메시지 JSON 역직렬화 실패 → 예외 없이 로그만 (서비스 중단 방지)
- [x] 동시에 다수 메시지 수신 (멀티스레드) — ConcurrentHashMap 동시성 안전 확인

**테스트 실행**
- [x] `./gradlew test --tests "com.redline.jj.api.notification.SseEmitterRepositoryTest"`
- [x] `./gradlew test --tests "com.redline.jj.api.notification.RestockSubscriberTest"`

---

### M8-2. NotificationController · NotificationService (SSE 스트림 포함)

**구현**
- [x] `GET /api/notifications/stream` — 인증된 userId로 `SseEmitter` 생성, `SseEmitterRepository` 등록; timeout·complete·error 콜백 시 제거
- [x] `GET /api/notifications` — 내 알림 목록 최신순 (인증 필요)
- [x] `GET /api/notifications/unread-count` — 미읽음 수, `@Cacheable("unreadCount")` 적용 (인증 필요)
- [x] `PATCH /api/notifications/{id}/read` — 단건 읽음 처리 (인증 필요)
- [x] `PATCH /api/notifications/read-all` — 전체 읽음 처리 (인증 필요)
- [x] 읽음 처리·새 Notification 생성 시 `@CacheEvict("unreadCount")` (`@EventListener` + `UnreadCacheEvictEvent` 방식으로 batch→api 역방향 의존 제거)

**단위 테스트** (`NotificationServiceTest`, Mockito)
- [x] `getNotifications(userId)` — 최신순 정렬 확인
- [x] `getUnreadCount(userId)` — 첫 호출: `Repository` 조회 (캐시 hit 재호출은 Spring Cache 추상화 책임이므로 단위테스트 제외)
- [x] `markAsRead(userId, 본인 notificationId)` — `isRead=true` 저장 + 캐시 evict
- [x] `markAsRead(userId, 타인 notificationId)` — `NOTIFICATION_ACCESS_DENIED` (N002)
- [x] `markAsRead(userId, 없는 notificationId)` — `NOTIFICATION_NOT_FOUND` (N001)
- [x] `markAllAsRead(userId)` — 본인 미읽음 전체 읽음 처리 + `@Modifying` bulk update (N+1 제거)
- [x] 새 `RestockNotification` 생성 이벤트 후 `unreadCount` 캐시 evict 확인 (`NotificationServiceCacheTest` @SpringBootTest)
- [x] 트랜잭션 롤백 시 캐시 evict 미발생 (AFTER_COMMIT 보장 — RestockEvent 롤백 → handleRestock 미호출 → evict 없음)

**컨트롤러 테스트** (`@WebMvcTest`)
- [x] `GET /api/notifications/stream` — `Content-Type: text/event-stream` 헤더 확인
- [x] `GET /api/notifications/stream` 비인증 → 401
- [x] `PATCH /api/notifications/{id}/read` 비인증 → 401

**테스트 실행**
- [x] `./gradlew test --tests "com.redline.jj.api.notification.*"`
- [x] `./gradlew test` — M1~M8 전체 그린 확인 (265개 전체 통과)

---

## M9. 개별 기능 — 대시보드 · 배치 스케줄러 · Rate Limit

### M9-1. DashboardController · DashboardService

**구현**
- [ ] `GET /api/dashboard/price-comparison?modelId=` — 모델별 사이트×옵션 가격 비교 (인증 불필요)
- [ ] `GET /api/dashboard/price-history?modelId=&days=30` — 최근 N일 가격 추이 (`SiteOptionLog` 기반, 인증 불필요)
- [ ] `GET /api/restocks/recent` — 최근 재입고 10건 (`RestockNotification` 기준, 인증 불필요)

**단위 테스트** (`DashboardServiceTest`, Mockito)
- [ ] `getPriceComparison(modelId)` — 사이트별 옵션 그룹화 결과 구조 확인
- [ ] `getPriceHistory(modelId, 30)` — 30일 이내 `SiteOptionLog` 조회, 날짜 오름차순
- [ ] `getPriceHistory(modelId, days=0)` — 빈 리스트 또는 예외 처리
- [ ] `getRecentRestocks()` — 최신 10건, 11번째 미포함

**테스트 실행**
- [ ] `./gradlew test --tests "com.redline.jj.api.dashboard.*"`

---

### M9-2. BatchScheduler (동적 주기)

**구현** (`com.redline.jj.batch.BatchScheduler`)
- [ ] `@Scheduled` 기반, 실행 시점에 `SubscriptionRepository.existsAny()` 조회
- [ ] `existsAny()=true` → 다음 실행 20분 후 스케줄
- [ ] `existsAny()=false` → 다음 실행 1시간 후 스케줄
- [ ] 스케줄 실행 시 `modeManCrawlingJob`, `nestStoreCrawlingJob`, `semiBasementCrawlingJob` 순차 실행

**단위 테스트** (`BatchSchedulerTest`, Mockito)
- [ ] `existsAny()=true` → 3개 Job 모두 실행 (JobLauncher.run() 3회 verify)
- [ ] `existsAny()=false` → 3개 Job 모두 실행 (실행 자체는 동일, 주기는 로그/설정으로 확인)
- [ ] Job 실행 중 예외 발생 → 나머지 Job 계속 실행 (예외 격리)

**테스트 실행**
- [ ] `./gradlew test --tests "com.redline.jj.batch.BatchSchedulerTest"`

---

### M9-3. Rate Limit (Bucket4j)

**구현** (`com.redline.jj.config.RateLimitFilter`, `OncePerRequestFilter`)
- [ ] IP당 60 req/min 버킷 생성 (`ConcurrentHashMap<String, Bucket>`)
- [ ] `X-Forwarded-For` 헤더 존재 시 원 IP 추출, 없으면 `RemoteAddr` 사용
- [ ] 버킷 소진 → 429 + `ApiResponse.fail(ErrorCode.RATE_LIMIT_EXCEEDED)`
- [ ] 모든 경로에 적용 (인증·비인증 무관)

**단위 테스트** (`RateLimitFilterTest`, MockMvc)
- [ ] 동일 IP 60회 요청 → 모두 200 (또는 원래 응답)
- [ ] 동일 IP 61번째 요청 → 429 `RATE_LIMIT_EXCEEDED` (U006)
- [ ] 다른 IP → 독립 버킷, 첫 번째 IP 소진 후 다른 IP 정상 통과
- [ ] `X-Forwarded-For` 헤더 있을 때 원 IP 기준 버킷 사용
- [ ] 버킷 리필 (1분 경과 시뮬레이션) → 재허용

**테스트 실행**
- [ ] `./gradlew test --tests "com.redline.jj.config.RateLimitFilterTest"`

---

## 최종 검증

- [x] `./gradlew clean build` — 빌드 성공
- [x] `./gradlew test` — 전체 테스트 그린 (265개)
- [ ] `./gradlew bootRun --args='--spring.profiles.active=local'` — 서버 기동
- [ ] `http://localhost:8080/actuator/health` — `{"status":"UP"}`
- [ ] `http://localhost:8080/swagger-ui/index.html` — 전체 API 문서화 확인
- [ ] SSE E2E: `curl -N -H "Authorization: Bearer {token}" http://localhost:8080/api/notifications/stream` 연결 → `POST /api/batch/crawl/modeMan` 수동 트리거 → SSE 이벤트 수신 확인
- [ ] Rate Limit: 동일 IP 61회 요청 시 429 포함 확인
