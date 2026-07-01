# LinkCode

> 실시간 채팅과 협업 코딩 인터뷰를 지원하는 마이크로서비스 플랫폼

---

## 목차

- [프로젝트 개요](#프로젝트-개요)
- [시스템 아키텍처](#시스템-아키텍처)
- [주요 기능 및 기술적 구현](#주요-기능-및-기술적-구현)
- [기술 스택](#기술-스택)
- [모듈 구성](#모듈-구성)
- [API 명세](#api-명세)
- [실행 방법](#실행-방법)

---

## 프로젝트 개요

LinkCode는 개발자 면접 환경을 지원하는 **마이크로서비스 기반 백엔드 플랫폼**입니다.  
면접관과 지원자가 동일한 코드 에디터를 실시간으로 공유하며 화상 채팅과 협업 코딩을 수행할 수 있도록 설계되었습니다.

| 특징 | 설명 |
|------|------|
| **MSA** | 5개의 독립 서비스로 구성, 각 서비스가 독립적으로 배포 가능 |
| **실시간 협업** | Y.js CRDT 기반 이진 WebSocket으로 충돌 없는 코드 동기화 |
| **보안** | Google/Kakao/Naver OAuth2 소셜 로그인 + JWT + Redis 블랙리스트 기반 인증 |

---

## 시스템 아키텍처

<img width="1057" height="605" alt="Image" src="https://github.com/user-attachments/assets/015ae3d3-bc53-43ea-98fd-b10d41370cc1" />

---

## 주요 기능 및 기술적 구현

### 1. Y.js CRDT 기반 실시간 코드 동기화

면접관과 지원자가 동시에 코드를 편집할 때 **충돌 없이 병합**되는 협업 에디터를 구현했습니다.

- **프로토콜**: Raw Binary WebSocket (`BinaryWebSocketHandler`)
- **동기화 방식**: Y.js delta를 Redis List에 순서대로 누적 (`rightPush`)
- **중간 입장 처리**: 신규 유저 접속 시 Redis에 쌓인 모든 delta를 순서대로 스트리밍
- **세션 관리**: `ConcurrentHashMap<Long, CopyOnWriteArraySet<WebSocketSession>>`으로 방별 세션 Thread-safe 관리

<img width="1006" height="600" alt="Image" src="https://github.com/user-attachments/assets/f3fcd255-b9c5-4492-854f-df599cf6be2d" />

---

### 2. OAuth2 + JWT 인증/인가

```
① 소셜 로그인 (Google / Kakao / Naver)
   → CustomOAuth2UserService.loadUser()
     → OAuth2UserInfoFactory가 registrationId로 Provider별 파서 선택
     → 이메일/이름/프로필 사진을 공통 속성 맵으로 정규화
     → Member 조회 or 신규 가입 (관리자 이메일과 일치하면 ROLE_ADMIN 자동 부여)
     → CustomOAuth2User(userId, email, name, picture, role) 생성
② OAuth2LoginSuccessHandler
   → AccessToken (30분, Cookie) 발급 — JwtTokenProvider.createAccessToken
   → RefreshToken (4일) 발급 → Redis(`refresh:{userId}`) 저장 + Cookie 설정
   → 프론트엔드로 리다이렉트
③ 모든 서비스의 JwtAuthenticationFilter (common-module)
   → 쿠키에서 JWT 추출 → 서명/만료 검증 → CustomPrincipal 생성 → SecurityContext 등록
④ 토큰 재발급 (`/api/auth/reissue`)
   → RefreshToken 유효성 검증 + Redis 저장값 비교
   → 일치 시 새 AccessToken 발급 (RefreshToken Rotation 없이 재사용)
⑤ 로그아웃 (`/api/auth/logout`)
   → RefreshToken → Redis blacklist 등록 (잔여 TTL 적용)
   → Access/Refresh Cookie 삭제
```

- **Provider 확장 구조**: `OAuth2UserInfo` 추상 클래스를 두고 `GoogleOAuth2UserInfo` 등 Provider별 구현체가 원본 attribute 맵의 형태 차이(Google은 평면 구조, Kakao/Naver는 중첩 구조)를 흡수해 동일한 `getId()/getEmail()/getName()/getPictureUrl()` 인터페이스로 노출합니다. 신규 Provider 추가 시 `OAuth2UserInfoFactory`에 분기만 추가하면 됩니다.
- **세션 없는 인증**: 로그인 성공 후 서버 세션을 만들지 않고 JWT 쿠키만으로 인증 상태를 전파하므로, 서비스 간 세션 클러스터링 없이 각 서비스가 독립적으로 스케일 아웃 가능합니다.
- Redis 블랙리스트를 활용해 **토큰 탈취 후 로그아웃 시에도 즉시 무효화**되도록 구현했습니다.


<img width="975" height="502" alt="Image" src="https://github.com/user-attachments/assets/f251c99e-3cee-4509-8e73-85a5de19310c" />

### 3. 면접 파이프라인 (면접 종료 → AI 분석)

```
면접 종료 API 호출
  ① Redis에서 최종 코드 스냅샷 수거
  ② 방 상태 ENDED로 변경 (Dirty Checking)
  ③ InterviewDocument 엔티티 생성 및 DB 저장
  ④ Kafka 이벤트 발행 (interview-ended 토픽)
     → AI 분석 Consumer가 비동기 처리
  ⑤ Redis 리소스 반환 (Y.js delta + snapshot 삭제)
```
<img width="1207" height="510" alt="Image" src="https://github.com/user-attachments/assets/7bed8d43-88dc-4a5e-be49-bcba34909317" />

---

### 4. 역할 기반 접근 제어 (RBAC)

| 역할 | 권한 |
|------|------|
| `ADMIN` | 면접 방 생성 |
| `INTERVIEWER` | 방 개설자, 면접 진행·종료 |
| `CANDIDATE` | 초대 코드로 입장, 코드 편집 |

초대 코드 입장 시 방 개설자 여부에 따라 역할이 자동 배정되고, 방 상태가 `WAITING → READY`로 전이됩니다.

---

## 기술 스택

| 분류 | 기술 |
|------|------|
| Language | Java 17 |
| Framework | Spring Boot 3.5, Spring Security, Spring WebSocket |
| ORM | Spring Data JPA / Hibernate |
| Auth | OAuth2 Client (Google/Kakao/Naver), JWT 0.12 |
| Message Queue | Apache Kafka (RetryableTopic, DLT) |
| Cache | Redis (채팅 캐시, Y.js delta, JWT 블랙리스트) |
| DB | MySQL 8 (auth, interview, consumer), H2 in-memory (chat dev) |
| Build | Gradle 8.14 (Multi-module) |
| Collaboration | Y.js (CRDT), Binary WebSocket |

---

## 모듈 구성

```
LinkCode/
├── common-module/          # 공유 라이브러리 (plain JAR)
│   ├── JwtTokenProvider    # AccessToken(30m) / RefreshToken(4d) 생성·검증
│   ├── JwtAuthenticationFilter  # 전 서비스 공통 JWT 필터
│   ├── KafkaEventPublisher # Kafka 발행 추상화
│   ├── ChatMessageDto      # Redis 직렬화용 공유 DTO
│   └── CustomPrincipal     # userId/email/userName/picture/role
│
├── auth-service/           # :8081 — 소셜 로그인 + JWT 발급
├── chat-service/           # :8082 — WebSocket 채팅
├── chat-consumer-service/  # dynamic — Kafka → Redis consumer
├── interview-service/      # :8083 — 협업 코딩 면접 + AI 분석 파이프라인, 면접방별 전용 채팅
└── build.gradle            # 전체 공통 의존성 (Lombok, Redis, Test)
```

---

## API 명세

### auth-service (:8081)

| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | `/oauth2/authorization/{provider}` | OAuth2 로그인 (google/kakao/naver) |
| POST | `/api/auth/reissue` | AccessToken 재발급 |
| POST | `/api/auth/logout` | 로그아웃 (블랙리스트 등록) |

### chat-service (:8082)

| Method | Endpoint | 설명 |
|--------|----------|------|
| POST | `/api/chat/rooms` | 채팅방 생성 |
| GET | `/api/chat/rooms/{roomId}/messages` | 메시지 조회 (Redis 캐시) |
| WS | `/ws/chat` | STOMP WebSocket 연결 |

### interview-service (:8083)

| Method | Endpoint | 설명 |
|--------|----------|------|
| POST | `/api/interview/rooms` | 면접방 생성 (ADMIN 전용) |
| POST | `/api/interview/rooms/join/{inviteCode}` | 초대 코드로 입장 |
| GET | `/api/interview/rooms/{roomId}` | 방 정보 조회 |
| GET | `/api/interview/rooms/my` | 참여 중인 면접방 목록 |
| POST | `/api/interview/rooms/{roomId}/end` | 면접 종료 (AI 분석 트리거) |
| GET | `/api/interview/rooms/{roomId}/report` | 면접 분석 보고서 조회 |
| WS | `/ws/code/{roomId}` | Y.js 코드 동기화 (Binary WebSocket) |
| WS | `/ws/interview` | 면접 채팅 (STOMP) |

---

## 실행 방법

### 사전 요구사항

- Java 17+
- Docker (MySQL, Kafka, Redis 실행용)

### 인프라 실행

```bash
# MySQL
docker run -d --name mysql -e MYSQL_ROOT_PASSWORD=password -p 3306:3306 mysql:8

# Redis
docker run -d --name redis -p 6379:6379 redis:latest

# Kafka (Zookeeper + Broker)
docker run -d --name zookeeper -p 2181:2181 confluentinc/cp-zookeeper:latest
docker run -d --name kafka -p 9092:9092 \
  -e KAFKA_ZOOKEEPER_CONNECT=zookeeper:2181 \
  -e KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://localhost:9092 \
  confluentinc/cp-kafka:latest
```

### 서비스 실행

```bash
# 전체 빌드 (테스트 제외)
./gradlew build -x test

# 개별 서비스 실행
./gradlew :auth-service:bootRun          # :8081
./gradlew :chat-service:bootRun          # :8082
./gradlew :interview-service:bootRun     # :8083
./gradlew :chat-consumer-service:bootRun # dynamic port
```

### 환경 변수

```bash
# auth-service — 관리자 이메일 (기본값 내장)
SYSTEM_ADMIN_EMAIL=your-email@example.com
```
