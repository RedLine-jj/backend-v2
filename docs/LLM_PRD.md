# LLM 429 대응 및 중복 매칭 안정화 PRD

**작성일**: 2026-05-07

---

## 1. 개요

**목적**: Groq 429 rate limit 발생 시 잘못된 모델 데이터가 DB에 저장되는 것을 막고, 동일 상품에 대한 중복 LLM 호출을 제거하여 배치 크롤링의 데이터 정합성과 안정성을 높인다.

**범위**

- 포함: 429 전용 예외 코드 추가, Groq 클라이언트 내 retry/backoff 로직, 배치 skip 정책 등록, 앱 실행 중 메모리 캐시 적용
- 미포함: Redis 기반 분산 캐시, 다른 LLM 파싱 실패(C001) 또는 5xx 오류의 동작 변경, rate limit 설정값의 외부 프로퍼티화 (이후 별도 작업)

---

## 2. 핵심 기능

**Groq 429 구분 처리**

`GroqLlmClient`에서 HTTP 429 응답을 내부적으로 일반 오류와 구분하여 처리한다. `ErrorCode.LLM_RATE_LIMITED`(C006, 429)를 신규 추가하고, 해당 예외 발생 시 `ModelResolutionService`에서 신규 `Brand`/`Model`/`ModelAlias` 저장 fallback으로 진입하지 않는다.

**Retry/Backoff**

429 발생 시 최대 3회까지 재시도한다. `Retry-After` 헤더가 있으면 해당 값을 대기 시간으로 사용하고, 없으면 `2s → 5s → 10s` 순서로 대기 후 재시도한다. 3회(`MAX_RATE_LIMIT_ATTEMPTS=4`, 총 시도 4회) 재시도 후에도 실패하면 `BusinessException(LLM_RATE_LIMITED)`를 던진다.

**Batch Skip**

`CrawlingSkipPolicy`의 skippable 셋에 `LLM_RATE_LIMITED`를 추가한다. 해당 예외가 최종적으로 전파되면 해당 URL item 전체가 DB 저장 없이 skip된다. 현재 구조상 `process(String url)` 단위 skip이므로, 한 URL의 옵션 묶음 처리 중 rate limit이 발생하면 해당 URL item 전체가 skip된다.

**LLM Result Cache**

`ModelResolutionService`에 앱 실행 중 유지되는 `ConcurrentHashMap` 기반 메모리 캐시를 적용한다.

- 캐시 키: `normalizedBrand + "|" + product.siteModelName()`
- 캐시 대상: 성공 결과(`Optional<LlmMatchResult>`)와 confidence 미달 결과(`Optional.empty()`) 모두 캐시
- 캐시 제외: API 오류 및 429 예외 결과는 캐시하지 않음
- 캐시 hit 시 Groq 호출 없이 기존 결과로 매칭 로직을 진행

---

## 3. API 명세

이 기능은 배치 내부 처리 변경이므로 신규 외부 API 없음. 기존 배치 수동 트리거 엔드포인트는 동작 그대로 유지된다.

| Method | Path | 설명 |
|--------|------|------|
| POST | /api/batch/crawl/{site} | 배치 수동 실행 (변경 없음) |

**주요 에러 코드 (배치 내부)**

| 코드명 | HTTP 상태 | 코드값 | 메시지 | skip 여부 |
|--------|-----------|--------|--------|-----------|
| LLM_RATE_LIMITED | 429 | C006 | LLM 요청 한도를 초과했습니다. | O |
| LLM_MATCHING_FAILED | 500 | C001 | LLM 매칭에 실패했습니다. | X (기존 동작 유지) |

---

## 4. 데이터 모델

신규 테이블 없음. 기존 엔티티 변경 없음.

`LLM_RATE_LIMITED` 발생 시 `tb_model`, `tb_model_alias`, `tb_brand`에 신규 row가 생성되지 않는다는 것이 이번 변경의 핵심 데이터 정합성 보장 지점이다.

`ModelResolutionService`의 `llmResultCache` 필드는 스프링 빈 생명주기(= 앱 실행 단위)와 함께하며, 배치 Job 재실행 시에도 동일 JVM 인스턴스이면 캐시가 유지된다.

---

## 5. 예외 처리

| 코드명 | skip 여부 | 근거 |
|--------|-----------|------|
| LLM_RATE_LIMITED | O | 잘못된 fallback 저장 방지, 재시도 가능한 일시적 오류 |
| CRAWLING_FAILED | O | 기존 동작 유지 |
| LLM_MATCHING_FAILED | X | 기존 동작 유지 (원시 문자열 fallback 저장으로 처리) |

---

## 6. 테스트 기준

**GroqLlmClientTest**

- 429 1회 후 재시도 성공 시 정상 `LlmMatchResult` 반환 확인
- 429가 `MAX_RATE_LIMIT_ATTEMPTS`(4회 시도 = 3회 재시도)만큼 지속되면 `BusinessException(LLM_RATE_LIMITED)` 발생 확인
- `Retry-After` 헤더 값이 있을 때 해당 값으로 대기, 없을 때 backoff 순서(`2s → 5s → 10s`) 적용 확인
- 기존 설명문 포함 JSON 응답 파싱 테스트 유지

**ModelResolutionServiceTest**

- 동일 `normalizedBrand + siteModelName`으로 두 번 `resolve` 호출 시 `llmMatchClient.match` 1회만 호출됨을 확인
- `LLM_RATE_LIMITED` 발생 시 `modelRepository.save`, `modelAliasRepository.save`, `brandRepository.save` 미호출 확인
- LLM 반환 canonical 모델명이 DB에 없을 때 LLM 결과명으로 신규 모델 저장하는 기존 흐름 유지 확인

**CrawlingSkipPolicyTest**

- `LLM_RATE_LIMITED`를 담은 `BusinessException` 전달 시 `shouldSkip` true 반환 확인
- `LLM_MATCHING_FAILED`를 담은 `BusinessException` 전달 시 `shouldSkip` false 반환 확인
- `CRAWLING_FAILED` 기존 동작 유지 확인

---

## 7. 전제 조건 및 제약

- 429는 잘못된 모델 생성을 막기 위해 fallback 신규 저장하지 않는다.
- 다른 LLM 파싱 실패나 5xx 오류는 현재 동작을 우선 유지한다.
- rate limit 값(재시도 횟수, backoff 시간)은 코드 기본값으로 시작하고, 필요하면 이후 `application.yaml` property로 추출한다.
- 캐시는 JVM 인스턴스 단위로 동작하며, 서버 재시작 시 초기화된다. 분산 캐시(Redis)는 이번 범위 밖이다.
