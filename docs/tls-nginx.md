# 단일 오리진 배포 런북 (Nginx + Let's Encrypt)

프론트(React 정적)와 백엔드(게이트웨이)를 **하나의 도메인(같은 오리진)** 에서 Nginx로 서빙한다.
Nginx가 TLS를 종단하고, Let's Encrypt(certbot)로 인증서를 자동 발급·갱신한다.

**같은 오리진의 이점**: 브라우저 입장에서 프론트·API가 동일 출처 → **쿠키가 first-party**라
로그인 세션이 그대로 붙고, **CORS도 불필요**하다. (별도 프론트 호스팅·Vercel 불필요)

```
브라우저 ─ HTTPS 443 ─▶ Nginx (linkcoder.duckdns.org, TLS 종단)
                         ├─ /api /oauth /oauth2 /login /interview/room /ws /ws-code → api-gateway:8080 ─▶ auth/interview
                         └─ 그 외 전부                                              → frontend:80 (React SPA)
```

> 관련 파일: `docker/nginx/conf.d/api.conf`, `docker-compose.prod.yml`, `docker/nginx/init-letsencrypt.sh`

---

## 0. 사전 준비
- **도메인**: DuckDNS 등 무료 가능. 예 `linkcoder.duckdns.org` → 서버 **Elastic IP**로 A레코드(또는 DuckDNS IP 설정)
- 서버 보안그룹: **80·443만 외부 오픈** (8080/3306/9092/6379 차단)
- Docker + Docker Compose 설치
- ⚠️ Let's Encrypt는 **공인 도메인**이 필요 → 로컬에선 발급 불가(구조 리허설은 맨 아래 자체서명 참고)

## 1. 세 레포를 형제 디렉터리로 배치
compose가 `../../ai-consumer` 와 `../../crdt-front` 를 빌드하므로 위치가 정확해야 한다:
```bash
mkdir -p ~/apps && cd ~/apps
git clone https://github.com/showby6958/LinkCode.git
cd LinkCode && git checkout feat/nginx-tls && cd ..
git clone https://github.com/showby6958/AI-Analyze-Service.git ~/ai-consumer
git clone https://github.com/showby6958/LinkCode-React.git    ~/crdt-front
# 확인 (~/apps/LinkCode 기준 ../../ 는 ~/)
ls ~/apps/LinkCode/../../ai-consumer/Dockerfile ~/apps/LinkCode/../../crdt-front/Dockerfile
```

## 2. 도메인 치환 체크리스트 (도메인 바꿀 때 이 4곳 전부)
`linkcoder.duckdns.org` 를 실제 도메인으로:
- `docker/nginx/conf.d/api.conf` — `server_name`(2곳) + `ssl_certificate` 경로(2줄)
- `docker/nginx/init-letsencrypt.sh` — `domain`, `email`
- `docker-compose.prod.yml` — `frontend` 의 `REACT_APP_API_BASE`(https), `REACT_APP_WS_BASE`(wss)
- `.env` — `OAUTH_REDIRECT_BASE=https://<도메인>` (아래 3번)

## 3. `.env` (서버)
로컬 `.env` 복사 후 https 로만 수정:
```
OAUTH_REDIRECT_BASE=https://linkcoder.duckdns.org
CORS_ORIGINS=https://linkcoder.duckdns.org       # 같은 오리진이라 사실상 미사용이지만 맞춰둠
```
나머지(DB/JWT/OAuth/Slack/GEMINI, 인프라 호스트)는 로컬과 동일. 단 비밀값은 공개 서버용으로 강하게.

## 4. OAuth 콘솔
- Google/Naver 승인 리디렉션 URI → `https://linkcoder.duckdns.org/login/oauth2/code/{google,naver}`
- (프론트가 같은 도메인이라 Vercel 환경변수 설정은 불필요 — 프론트는 compose가 빌드 시 주입)

## 5. 최초 인증서 발급 (서버에서 1회)
```bash
cd ~/apps/LinkCode
# 스크립트 상단 staging=1 로 테스트 → 성공 후 staging=0 재실행
bash docker/nginx/init-letsencrypt.sh
```
동작: 더미 인증서로 nginx 기동 → certbot 웹루트 발급 → nginx reload → 갱신 루프 시작.

## 6. 전체 기동
```bash
docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d --build
docker compose -f docker-compose.yml -f docker-compose.prod.yml ps
```
> 첫 빌드는 오래 걸린다(백엔드 4개 Gradle + 프론트 npm 빌드). 프론트 빌드에 메모리를 쓰니 스왑 권장.

## 7. 검증
```bash
curl -I https://linkcoder.duckdns.org/                                   # 프론트 200 + 유효 인증서
curl -I https://linkcoder.duckdns.org/oauth/.well-known/jwks.json        # 백엔드 라우팅 200
```
- 브라우저: `https://linkcoder.duckdns.org` 접속 → 로그인 → 방 생성 → 코드동기화(WSS)·리포트까지 e2e

## 8. 갱신 / 업데이트
```bash
DC="docker compose -f docker-compose.yml -f docker-compose.prod.yml"
$DC run --rm certbot renew --dry-run     # 갱신 리허설
git pull && $DC up -d --build            # 코드 업데이트 배포 (프론트 변경도 재빌드로 반영)
docker system prune -af                  # 디스크 정리
```

---

## 라우팅 규칙 요약 (`api.conf`)
| 경로 | 대상 | 비고 |
|---|---|---|
| `/api/ /oauth/ /oauth2/ /login/ /interview/room/` | api-gateway | REST/OAuth |
| `/ws /ws-code` | api-gateway | WebSocket(Upgrade) |
| 그 외 전부 | frontend | React SPA (컨테이너 nginx가 try_files 폴백) |

> 백엔드 interview API는 `/interview/room/` 하위뿐이라, 프론트 라우트
> (`/interview/create`·`/interview/report`·`/interview/{id}`)와 충돌하지 않는다.

## 트러블슈팅
- **nginx 인증서 없다고 기동 실패**: init 스크립트 먼저 실행(5번).
- **certbot 챌린지 실패**: DNS 전파/80 오픈/`/.well-known/acme-challenge/` 확인.
- **프론트는 뜨는데 API 404**: `api.conf` location 프리픽스, 게이트웨이 healthy 확인.
- **로그인 후 세션 안 붙음**: 같은 오리진이면 대개 정상. `OAUTH_REDIRECT_BASE` https 여부, auth 쿠키 `Secure` 확인.
- **WebSocket 끊김**: `/ws`·`/ws-code` location 의 Upgrade 헤더·`proxy_read_timeout` 확인.
- **프론트 주소가 옛 도메인**: `REACT_APP_*` 는 빌드 시 구워지므로 도메인 변경 후 `--build` 재빌드 필요.

## 로컬 자체서명 리허설 (도메인 없이 구조 확인)
```bash
# live 경로에 자체서명 인증서를 넣고 nginx 만 띄워 프록시/SPA 라우팅 확인
openssl req -x509 -nodes -newkey rsa:2048 -days 365 \
  -keyout privkey.pem -out fullchain.pem -subj "/CN=localhost"
```
브라우저 경고를 무시하면 라우팅은 로컬에서도 검증 가능. 실인증서는 배포 서버에서만.
