# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 빌드 / 실행 명령어

```bash
# 빌드 (테스트 제외)
./gradlew clean build -x test

# 로컬 서버 실행
./gradlew bootRun --args='--spring.profiles.active=local'

# 전체 테스트 실행
./gradlew test

# 단일 테스트 클래스 실행
./gradlew test --tests "com.redline.jj.SomeTest"

# 단일 테스트 메서드 실행
./gradlew test --tests "com.redline.jj.SomeTest.methodName"
```

## 로컬 인프라 전제 조건

- **MySQL** `localhost:3306` / DB명: `redline` / user: `root` / password: `root`
- **Redis** `localhost:6379`

로컬 실행 전 두 인프라가 기동 상태여야 한다.

## 환경 프로파일

| 프로파일 | 파일 | 용도 |
|---------|------|------|
| `local` (기본) | `application-local.yaml` | 로컬 개발, ddl-auto: update |
| `prod` | `application-prod.yaml` | 운영, 환경변수로 DB/Redis 주입 |

`application.yaml`에 공통 설정이 있고, 프로파일 파일이 override한다.
운영 환경변수: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD`

## 아키텍처 구조

```
com.redline.jj
├── config/          # 인프라 Config 빈 선언
├── common/
│   ├── response/    # ApiResponse<T> - 모든 API 응답 래퍼
│   └── exception/   # ErrorCode, BusinessException, GlobalExceptionHandler
├── domain/
│   └── common/      # BaseEntity (JPA Auditing: createdAt, modifiedAt)
├── api/             # (추후) 도메인별 Controller + Service
└── batch/           # (추후) Spring Batch Job/Step
```

### 핵심 설계 결정

**API 응답**: 모든 컨트롤러는 `ApiResponse<T>`로 감싸서 반환한다.
- 성공: `ApiResponse.ok(data)`, `ApiResponse.ok(message, data)`, `ApiResponse.ok()`
- 실패: `ApiResponse.fail(ErrorCode)`, `ApiResponse.fail(code, message)`

**예외 처리**: 비즈니스 오류는 `BusinessException(ErrorCode)`를 throw하면 `GlobalExceptionHandler`가 일괄 처리한다. 새 에러 코드는 `ErrorCode` enum에 추가.

**엔티티**: 모든 Entity는 `BaseEntity`를 상속해 `createdAt` / `modifiedAt` 자동 관리.

**Redis 구조**:
- `RedisConfig` - `RedisTemplate<String, Object>` (Jackson 직렬화)
- `RedisCacheConfig` - `@Cacheable` 등 어노테이션 기반 캐시, 기본 TTL 10분
- `RedisPubSubConfig` - `RedisMessageListenerContainer` (구독자는 기능 구현 시 `addMessageListener`로 등록)

**Spring Batch**: `spring.batch.job.enabled=false`로 자동 실행을 막고 있다. Job은 Scheduler 또는 REST 트리거로 직접 실행. `@EnableBatchProcessing`은 사용하지 않는다(Boot 3.x auto-config와 충돌).

## 엔드포인트

- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- Health: `http://localhost:8080/actuator/health`
