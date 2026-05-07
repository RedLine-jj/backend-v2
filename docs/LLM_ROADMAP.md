# LLM 429 대응 및 중복 매칭 안정화 로드맵

**PRD**: [LLM_PRD.md](./LLM_PRD.md)
**작성일**: 2026-05-07
**브랜치**: feature/llm-rate-limit

---

## 현황 요약

PRD의 핵심 구현 항목은 이미 코드에 반영된 상태이다. 이 로드맵은 구현 완료 항목을 검증하고, 미비한 테스트를 보강하는 것을 목표로 한다.

| 항목 | 상태 |
|------|------|
| `ErrorCode.LLM_RATE_LIMITED` (C006, 429) | 완료 |
| `GroqLlmClient` retry/backoff (`Retry-After` 포함) | 완료 |
| `CrawlingSkipPolicy` — `LLM_RATE_LIMITED` skip 등록 | 완료 |
| `ModelResolutionService` — `llmResultCache` (`ConcurrentHashMap`) | 완료 |
| `ModelResolutionService` — 429 시 fallback 저장 차단 | 완료 |
| 테스트 보강 | **진행 중** |

---

## Phase 1. 아키텍처 / 공통 변경 검증

> 이미 구현된 공통 인프라 변경이 의도대로 동작하는지 확인한다.

### 1-1. ErrorCode 검증

- [ ] `ErrorCode.LLM_RATE_LIMITED` 코드값 `C006`, HTTP 상태 `429` 확인
- [ ] `GlobalExceptionHandler`에서 `LLM_RATE_LIMITED` 예외가 `ApiResponse.fail`로 올바르게 변환되는지 확인

**테스트 실행**

```bash
./gradlew test --tests "com.redline.jj.common.exception.ErrorCodeTest"
```

---

### 1-2. CrawlingSkipPolicy 검증

- [ ] `LLM_RATE_LIMITED` 포함 여부 코드 리뷰
- [ ] `LLM_MATCHING_FAILED`는 skip 대상이 아님을 재확인

**테스트 실행**

```bash
./gradlew test --tests "com.redline.jj.batch.job.CrawlingSkipPolicyTest"
```

---

## Phase 2. Groq 클라이언트 retry/backoff 테스트

> `GroqLlmClient`의 429 재시도 로직을 단위 테스트로 검증한다.

### 2-1. 재시도 성공 케이스

- [ ] 429 1회 발생 후 재시도 성공 시 정상 `LlmMatchResult` 반환 확인

### 2-2. 재시도 전체 실패 케이스

- [ ] 429가 `MAX_RATE_LIMIT_ATTEMPTS`(4회)만큼 지속되면 `BusinessException(LLM_RATE_LIMITED)` 발생 확인

### 2-3. Backoff 대기 케이스

- [ ] `Retry-After` 헤더가 있을 때 해당 값으로 대기 적용 확인
- [ ] `Retry-After` 헤더가 없을 때 `2s → 5s → 10s` backoff 순서 적용 확인

### 2-4. 기존 테스트 유지

- [ ] 설명문 포함 JSON 응답 파싱 기존 테스트 통과 확인

**테스트 실행**

```bash
./gradlew test --tests "com.redline.jj.batch.matching.groq.GroqLlmClientTest"
```

---

## Phase 3. ModelResolutionService 캐시 및 fallback 차단 테스트

> 캐시 동작과 429 시 fallback 저장 차단 로직을 단위 테스트로 검증한다.

### 3-1. LLM 결과 캐시 동작

- [ ] 동일 `normalizedBrand + siteModelName`으로 두 번 호출 시 `llmMatchClient.match` 1회만 호출됨 확인
- [ ] confidence 미달 결과(`Optional.empty()`)도 캐시되어 두 번째 호출에서 LLM 미호출 확인
- [ ] 429 예외 발생 시 캐시에 저장되지 않아 다음 호출에서 LLM 재호출됨 확인

### 3-2. 429 fallback 저장 차단

- [ ] `LLM_RATE_LIMITED` 발생 시 `modelRepository.save`, `modelAliasRepository.save`, `brandRepository.save` 미호출 확인

### 3-3. 기존 흐름 유지

- [ ] LLM 반환 canonical 모델명이 DB에 없을 때 LLM 결과명으로 신규 모델 저장하는 기존 흐름 통과 확인

**테스트 실행**

```bash
./gradlew test --tests "com.redline.jj.batch.matching.ModelResolutionServiceTest"
```

---

## Phase 4. 전체 테스트 통합 검증

> 모든 테스트를 일괄 실행하여 기존 테스트 회귀 없음을 확인한다.

- [ ] 전체 테스트 통과 확인
- [ ] `LLM_MATCHING_FAILED` 기존 동작 회귀 없음 확인
- [ ] `CRAWLING_FAILED` 기존 동작 회귀 없음 확인

**테스트 실행**

```bash
./gradlew test
```

---

## Phase 5. 빌드 및 마무리

- [ ] 테스트 제외 빌드 성공 확인

```bash
./gradlew clean build -x test
```

- [ ] PR 생성 및 코드 리뷰 요청 (`feature/llm-rate-limit` → `main`)

---

## 미포함 (추후 작업)

| 항목 | 이유 |
|------|------|
| rate limit 값 `application.yaml` property화 | 코드 기본값으로 먼저 운영 후 필요 시 분리 |
| Redis 기반 분산 캐시 | 현재 단일 JVM 환경에서는 메모리 캐시로 충분 |
| 5xx 오류 및 기타 LLM 파싱 실패 처리 개선 | 별도 이슈로 관리 |
