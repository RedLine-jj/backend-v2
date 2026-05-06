# redline

데님 편집샵의 상품 재고와 가격을 수집하고, 동일 모델을 매칭해 재입고 알림을 제공하는 Spring Boot API 서버입니다.

## 주요 기능

- JWT 기반 회원가입, 로그인, 액세스 토큰 재발급
- 브랜드, 사이트, 모델 타입 마스터 데이터 조회
- 모델 목록, 모델 상세, 사이트별 옵션, 가격/재고 변경 이력 조회
- 모델 구독 등록 및 취소
- Spring Batch 기반 편집샵 크롤링
- Exact, Alias Cache, Groq LLM 기반 모델 매칭
- 재입고 감지 시 Redis Pub/Sub 및 SSE 실시간 알림 전송
- 구독 TOP 10, 최근 재입고, 가격 비교, 가격 추이 대시보드 API 제공

## 기술 스택

- Java 17
- Spring Boot 3.5.3
- Spring Web, Spring Security, Spring Data JPA
- Spring Batch
- MySQL 8
- Redis 7
- Gradle
- SpringDoc OpenAPI
- JUnit 5, H2, Testcontainers, MockWebServer

## 프로젝트 구조

```text
src/main/java/com/redline/jj
├── api                 # REST API 컨트롤러, 서비스, DTO
├── batch               # 크롤링 배치, 파서, 모델 매칭
├── common              # 공통 응답, 예외 처리
├── config              # JPA, Redis, Security, Swagger 설정
└── domain              # Entity, Repository

src/main/resources
├── application.yaml
├── application-local.yaml
└── application-prod.yaml

docs
├── PRD.md
└── ROADMAP.md
```

## 시작하기

### 1. 사전 준비

- JDK 17
- Docker 또는 Docker Desktop
- Gradle Wrapper 사용 권장

### 2. 환경 변수 설정

`.env.example`을 참고해 `.env`를 생성합니다.

```bash
cp .env.example .env
```

로컬 실행 시 최소로 필요한 값은 다음과 같습니다.

```env
GROQ_API_KEY=your-groq-api-key
```

`application-local.yaml`에는 로컬 개발용 MySQL, Redis 접속 정보와 JWT 테스트 키가 기본 설정되어 있습니다.

### 3. 인프라 실행

```bash
docker compose -f compose.yml up -d
```

기본 포트는 다음과 같습니다.

| 서비스 | 포트 |
|--------|------|
| MySQL | 3306 |
| Redis | 6379 |

### 4. 애플리케이션 실행

```bash
./gradlew bootRun
```

기본 서버 포트는 `8081`입니다.

```text
http://localhost:8081
```

### 5. 테스트 실행

```bash
./gradlew test
```

특정 테스트만 실행하려면 다음 형식을 사용합니다.

```bash
./gradlew test --tests "com.redline.jj.api.auth.*"
```

## API 문서

애플리케이션 실행 후 Swagger UI에서 API를 확인할 수 있습니다.

```text
http://localhost:8081/swagger-ui.html
```

주요 API 그룹은 다음과 같습니다.

| 그룹 | 경로 |
|------|------|
| 인증 | `/api/auth/**` |
| 브랜드 | `/api/brands` |
| 사이트 | `/api/sites` |
| 모델 | `/api/models/**` |
| 사이트 옵션 | `/api/site-options/**` |
| 구독 | `/api/subscriptions/**` |
| 알림/SSE | `/api/notifications/**` |
| 대시보드 | `/api/dashboard/**`, `/api/restocks/recent` |
| 배치 | `/api/batch/crawl/{site}` |

## 프로필

| 프로필 | 용도 |
|--------|------|
| `local` | 로컬 개발 환경, MySQL/Redis localhost 사용 |
| `prod` | 운영 환경, 외부 환경 변수 기반 설정 |
| `test` | 테스트 환경, H2 및 테스트용 설정 사용 |

기본 활성 프로필은 `local`입니다.

## 배치 실행

크롤링 배치는 기본적으로 자동 실행되지 않습니다.

```yaml
spring:
  batch:
    job:
      enabled: false
```

수동 실행 API는 다음 형식을 사용합니다.

```http
POST /api/batch/crawl/{site}
```

`{site}` 값은 다음 중 하나입니다.

- `modeMan`
- `nestStore`
- `semiBasement`

## 참고 문서

- [PRD](docs/PRD.md)
- [ROADMAP](docs/ROADMAP.md)
