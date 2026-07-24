# LinkCode

> 실시간 협업 코딩 면접 플랫폼 — API Gateway 기반 마이크로서비스 아키텍처

---

## 목차

- [프로젝트 개요](#프로젝트-개요)
- [시스템 아키텍처](#시스템-아키텍처)
- [주요 기능 및 기술적 구현](#주요-기능-및-기술적-구현)
- [기술 스택](#기술-스택)
- [모듈 구성](#모듈-구성)
- [API 명세](#api-명세)
- [실행 · 배포](#실행--배포)

---

## 프로젝트 개요

LinkCode는 면접관과 지원자가 **하나의 코드 에디터를 실시간으로 공유**하며 협업 코딩 면접을 진행하는 플랫폼입니다. 면접 종료 시 작성한 코드를 **AI가 자동 분석**해 리포트를 제공합니다.

| 특징 | 설명 |
|------|------|
| **API Gateway 중심 MSA** | 게이트웨이가 유일한 진입점 — RS256 JWT를 JWKS로 검증하고 `X-User-*` 헤더를 주입, 각 서비스는 헤더만 신뢰 |
| **실시간 협업** | Y.js CRDT + 이진 WebSocket으로 충돌 없는 코드 동기화 |
| **단일 오리진 배포** | Nginx가 TLS를 종단하고 프론트(정적)와 백엔드를 한 도메인에서 서빙 → 쿠키 first-party, CORS 불필요 |
| **비동기 AI 분석** | Kafka로 면접 종료 이벤트를 발행하고 별도 Python 워커(Gemini)가 분석 |

---

## 시스템 아키텍처

<img width="1295" height="465" alt="Image" src="https://github.com/user-attachments/assets/e324f0d4-2638-467d-b58a-1058308456ea" />

> 브라우저 → **Nginx**(TLS·단일 오리진) → **API Gateway**(RS256/JWKS 검증) → 각 서비스.
> 인프라(MySQL·Kafka(KRaft)·Redis)와 앱은 EC2 위 docker compose로 구동됩니다.

---

## 주요 기능 및 기술적 구현

### 1. API Gateway — 중앙 인증·인가

게이트웨이(Spring Cloud Gateway, 리액티브)가 모든 요청의 단일 진입점입니다.

- **RS256 검증**: `NimbusReactiveJwtDecoder`가 auth-service의 **JWKS**(`/oauth/.well-known/jwks.json`)로 공개키를 받아 쿠키의 JWT를 검증
- **헤더 주입**: 검증된 사용자 정보를 `X-User-*` 헤더로 넣어 하위 서비스에 전달 → 서비스는 토큰을 파싱하지 않고 헤더만 신뢰(`HeaderAuthenticationFilter`)
- **경로별 라우팅·역할 검사**: `/oauth·/login → auth`, `/ws·/ws-code·/api·/interview → interview`. 방 생성 등 보호 경로는 게이트웨이 전역 필터가 401/403 처리
- **CORS 일원화**: 게이트웨이의 `CorsWebFilter`가 CORS를 전담(각 서비스에서는 제거) → 헤더 중복 방지

### 2. OAuth2 + RS256 JWT 인증

```
① 소셜 로그인 (Google / Naver)
   → CustomOAuth2UserService: registrationId로 Provider별 파서 선택 → 사용자 정규화
   → 관리자 이메일과 일치하면 ROLE_ADMIN 자동 부여
② OAuth2LoginSuccessHandler
   → RS256 AccessToken(쿠키) 발급  ·  RefreshToken → Redis 저장 + 쿠키
   → FRONTEND_URL 로 리다이렉트 (배포 시 같은 도메인)
③ 이후 모든 요청
   → 게이트웨이가 JWKS로 RS256 검증 → X-User-* 헤더 주입 → 각 서비스로 전달
④ 재발급 /oauth/reissue  ·  로그아웃 /oauth/logout (RefreshToken → Redis 블랙리스트)
```

- **비대칭키(RS256)**: auth-service가 개인키로 서명하고 공개키를 JWKS로 공개 → 게이트웨이는 시크릿 공유 없이 검증
- **세션 없는 인증**: 서버 세션 없이 JWT 쿠키만으로 상태 전파 → 각 서비스 독립 스케일아웃
- **즉시 무효화**: Redis 블랙리스트로 로그아웃 시 토큰 즉시 무효화

### 3. Y.js CRDT 실시간 코드 동기화

면접관과 지원자가 동시에 편집해도 **충돌 없이 병합**됩니다.

- **프로토콜**: Raw Binary WebSocket (`/ws-code/{roomId}`)
- **동기화**: Y.js delta를 Redis List에 순서대로 누적(`rightPush`), 신규 입장 시 누적 delta를 순서대로 스트리밍
- **세션 관리**: `ConcurrentHashMap<Long, CopyOnWriteArraySet<WebSocketSession>>`로 방별 세션을 Thread-safe하게 관리
- **스냅샷**: 프론트가 디바운스로 현재 코드를 `/api/interview/room/{id}/snapshot`에 올려 Redis에 보관 → 면접 종료 시 수거

### 4. 면접 파이프라인 (종료 → AI 분석)

```
면접 종료 API
  ① Redis에서 최종 코드 스냅샷 수거
  ② 방 상태 ENDED (Dirty Checking)
  ③ InterviewDocument 생성·저장
  ④ Kafka 발행 (interview-ended)
        → ai-consumer(Python·Gemini)가 분석 → interview-analyzed 발행
        → interview-service가 소비해 분석 결과를 DB에 반영(Dirty Checking)
  ⑤ Redis 리소스 반환 (Y.js delta · snapshot 삭제)
```

- 채팅 이벤트 실패분은 `chat-consumer-service`가 DLT로 수거해 DB에 기록하고 **Slack**으로 알림

---

## 기술 스택

| 분류 | 기술 |
|------|------|
| Language | Java 17 |
| Framework | Spring Boot 3.5.13, **Spring Cloud Gateway 2025.0.0**, Spring Security, Spring WebSocket |
| Auth | OAuth2 Client (Google/Naver), **JWT RS256** (jjwt 0.12 + Nimbus JOSE/JWKS) |
| Messaging | Apache **Kafka (KRaft)** — Zookeeper 없음 |
| Cache | Redis (Y.js delta, 채팅 캐시, JWT 블랙리스트) |
| DB | MySQL 8.4 (linkcodeauth · linkcodeinterview · linkcodeconsumer) |
| Collaboration | Y.js (CRDT), Binary WebSocket |
| AI | Google Gemini (별도 Python 워커) |
| Build | Gradle 8.14 (멀티모듈) |
| Infra | Docker Compose, **Nginx**(TLS·단일 오리진), Let's Encrypt, **GitHub Actions CI/CD**, AWS EC2 |

---

## 모듈 구성

```
LinkCode/
├── api-gateway/            # :8080 — 단일 진입점, RS256/JWKS 검증, X-User 헤더 주입, 라우팅·CORS
├── auth-service/           # :8081 — OAuth2 소셜 로그인, RS256 JWT 발급, JWKS 공개
├── interview-service/      # :8083 — 협업 코딩(WS) + 면접 채팅(STOMP) + AI 분석 파이프라인
└── chat-consumer-service/  # port 0 — Kafka(DLT) 소비 → 실패 이벤트 DB 기록 + Slack 알림

(별도 레포)
├── crdt-front       (LinkCode-React)        # React 프론트엔드
└── ai-consumer      (AI-Analyze-Service)    # Python · Gemini 분석 워커
```

> 이전의 `common-module`·`chat-service`는 리팩터로 제거되었습니다. 공용 인증은 게이트웨이가, 면접 채팅은 interview-service가 담당합니다.

---

## API 명세

> 배포 환경에서는 모든 요청이 게이트웨이(단일 도메인)를 통해 들어갑니다. 아래는 게이트웨이 기준 경로입니다.

### 인증 (auth-service)

| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | `/oauth2/authorization/{google\|naver}` | 소셜 로그인 시작 |
| GET | `/oauth/me` | 현재 로그인 사용자 조회 |
| POST | `/oauth/reissue` | AccessToken 재발급 |
| POST | `/oauth/logout` | 로그아웃 (RefreshToken 블랙리스트) |
| GET | `/oauth/.well-known/jwks.json` | JWKS 공개키 (게이트웨이 검증용) |

### 면접 (interview-service)

| Method | Endpoint | 설명 |
|--------|----------|------|
| POST | `/interview/room/create` | 면접방 생성 (ADMIN) |
| POST | `/interview/room/join/{inviteCode}` | 초대 코드로 입장 |
| GET | `/interview/room/info/{roomId}` | 방 정보 조회 |
| GET | `/interview/room/my` | 참여 중인 면접방 목록 |
| POST | `/interview/room/{roomId}/end` | 면접 종료 (AI 분석 트리거) |
| PATCH | `/api/interview/room/{roomId}/snapshot` | 코드 스냅샷 업로드(디바운스) |
| GET | `/api/interview/reports/{documentId}` | AI 분석 리포트 조회 |
| WS | `/ws-code/{roomId}` | Y.js 코드 동기화 (Binary WebSocket) |
| WS | `/ws` | 면접 채팅 (STOMP · SockJS) |

---

## 실행 · 배포

### 로컬 개발 (docker compose)

```bash
cp .env.example .env      # 비밀값 채우기
docker compose up -d --build
```
전체 절차·검증·트러블슈팅: **[`docs/docker-run.md`](docs/docker-run.md)**

### 배포 (단일 오리진 + TLS)

Nginx 리버스 프록시(단일 오리진) + Let's Encrypt로 EC2에 배포하며, **`main` push 시 GitHub Actions가 자동 배포**합니다.
전체 배포 런북: **[`docs/tls-nginx.md`](docs/tls-nginx.md)**

```bash
# 배포 서버에서 (자동배포와 동일한 동작)
docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d --build
```

### 요구사항
- Java 17+, Docker / Docker Compose
- 비밀값은 `.env`(gitignore)에만 — `application.yml`은 `${VAR}`로만 참조
