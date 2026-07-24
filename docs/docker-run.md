# LinkCode Docker 실행 런북

백엔드 전체(게이트웨이·auth·interview·chat-consumer)와 인프라(MySQL·Kafka·Redis)를
Docker Compose로 한 번에 띄우는 방법. **IntelliJ에서 서비스별 bootRun 하던 것을 대체**한다.

> `ai-consumer`(Gemini 분석 워커)도 이 base compose에 포함된다(`../../ai-consumer` 레포 필요).
> 프론트(`crdt-front`)만 별도 — 로컬 개발은 따로 실행하고, 배포는 nginx가 같은 오리진으로 함께 서빙한다(→ `docs/tls-nginx.md`).

---

## 구성

| 컨테이너 | 포트(host:container) | 의존 | 비고 |
|---|---|---|---|
| api-gateway | 8080:8080 | auth, interview | **유일한 외부 진입점** |
| auth-service | 8081:8081 | mysql, kafka, redis | OAuth2 · RS256 JWKS |
| interview-service | 8083:8083 | mysql, kafka, redis | HTTP + WebSocket |
| chat-consumer-service | (없음, port=0) | mysql, kafka, redis | Kafka 소비 + Slack |
| ai-consumer | (없음) | kafka | Gemini 분석 워커 (interview-ended→analyzed), `GEMINI_API_KEY` 필요 |
| mysql | 3306:3306 | — | DB 3개 자동 생성 |
| kafka | 9092:9092 | — | KRaft, 내부 `kafka:19092` |
| redis | 6379:6379 | — | |

기동 순서: **mysql·kafka·redis(healthy) → auth·interview·chat-consumer → api-gateway**

---

## 사전 준비

- **Docker Desktop 실행 중** (`docker version` 에러 없어야 함)
- **`.env` 존재** — 실제 비밀값 포함, gitignore됨. 새 환경은 템플릿 복사:
  ```bash
  cp .env.example .env   # 후 *_SECRET / *_PASSWORD / *_CLIENT_* / SLACK_* 채우기
  ```
- 포트 **8080/8081/8083/3306/9092/6379** 가 비어 있어야 함
  (이전에 bootRun/다른 컨테이너로 띄운 서비스는 종료)

---

## 1. 빌드 + 기동

프로젝트 루트에서:
```bash
docker compose up -d --build
```
첫 실행은 Gradle 빌드 + 이미지 pull로 수 분 소요. `-d`는 백그라운드.

## 2. 상태 확인
```bash
docker compose ps
```
- 목표: 전부 `running`, 헬스체크 대상은 `healthy`
- **gateway까지 `healthy`면 전체 정상.** gateway는 auth가 준비된 뒤 뜨므로 가장 마지막.

실시간 로그(진단):
```bash
docker compose logs -f api-gateway     # 특정 서비스
docker compose logs -f                  # 전체
```

## 3. 인프라 검증
```bash
# MySQL: DB 3개 생성 확인
docker compose exec mysql mysql -uroot -p"$DB_PASSWORD" -e "SHOW DATABASES;"
#   → linkcodeauth / linkcodeinterview / linkcodeconsumer

# Kafka: 브로커 응답
docker compose exec kafka /opt/kafka/bin/kafka-broker-api-versions.sh \
  --bootstrap-server localhost:9092 | head -1

# Redis
docker compose exec redis redis-cli ping     # → PONG
```

## 4. 앱 / 게이트웨이 검증 (브라우저 권장)
- **JWKS(RS256 공개키)**: `http://localhost:8081/oauth/.well-known/jwks.json` → 키 셋 JSON이면 auth 정상
- **게이트웨이 + 로그인**: `http://localhost:8080/oauth2/authorization/google` 접속 →
  구글 로그인 → 콜백 성공하면 gateway↔auth 흐름 정상
- 화면 전환까지 보려면 **프론트를 따로 실행**(`http://localhost:3000`)

> PowerShell의 `curl`은 `Invoke-WebRequest` 별칭이라 헷갈리니, HTTP 확인은 브라우저나 `curl.exe` 사용.

## 5. 종료 / 리셋
```bash
docker compose down          # 컨테이너 삭제 (MySQL 데이터는 볼륨에 유지)
docker compose down -v       # 볼륨까지 삭제 → DB 초기화(init 스크립트 재실행)
```

---

## 트러블슈팅

**`dependency kafka failed to start` / kafka가 재시작 반복**
- Kafka 리스너는 `0.0.0.0`이 아니라 **빈 호스트(`://:port`)** 로 둬야 한다
  (`0.0.0.0`이면 CONTROLLER 리스너 유도 주소가 0.0.0.0이 되어 `kafka-storage format` 실패).
  현재 compose는 이미 반영됨. 로그 확인: `docker compose logs kafka`

**gateway가 한동안 `unhealthy`**
- `start_period`(40s) 동안 정상. auth가 뜨면 곧 healthy로 전환.
- 계속 unhealthy면 먼저 `docker compose logs auth-service` 확인.

**포트 충돌 (`address already in use`)**
- 기존 로컬 서비스/컨테이너를 끄거나, compose의 `ports` 좌측(host) 값을 변경.

**설정만 바꿨는데 반영이 안 됨**
- 앱 코드/설정 변경은 이미지 재빌드 필요: `docker compose up -d --build <서비스>`
- `.env` 변경은 컨테이너 재생성 필요: `docker compose up -d --force-recreate <서비스>`

**전체 초기화**
```bash
docker compose down -v
docker compose up -d --build
```

---

## 알아둘 점

- **AI 분석 동작**: `ai-consumer`(Gemini)가 base compose에 포함돼 인터뷰 분석이 돌아온다.
  `../../ai-consumer` 레포가 형제 디렉터리에 있어야 빌드되며, `.env`에 `GEMINI_API_KEY` 필요.
- **Kafka는 개발용 ephemeral**: 영속 볼륨 없이 동작. 재기동 시 토픽/오프셋 초기화.
- **비밀값**: 실제 값은 `.env`(gitignore)에만. 이미지/커밋에 노출 금지.
- **하이브리드 개발**: 특정 서비스만 IDE로 디버깅하려면, 인프라(mysql·kafka·redis)만
  compose로 띄우고 해당 앱은 IntelliJ bootRun으로 돌려도 된다.
