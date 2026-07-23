# TLS 리버스 프록시 (Nginx + Let's Encrypt) 배포 런북

백엔드(게이트웨이) 앞에 Nginx를 두어 **api.example.com** 을 HTTPS로 종단하고,
Let's Encrypt(certbot)로 인증서를 자동 발급·갱신한다. 프론트(app.example.com)는
Vercel이 자체 TLS를 제공하므로 이 문서 대상이 아니다.

```
[app.example.com] Vercel(자체 TLS) ── HTTPS/WSS ─┐
                                                  ▼
브라우저 ─ HTTPS 443 ─▶ Nginx(TLS 종단) ─ HTTP 8080 ─▶ api-gateway ─▶ auth/interview
        ─ WSS 443 ──▶ (Upgrade 전달)     ─ ws ──────▶  (/ws, /ws-code)
```

> 관련 파일: `docker/nginx/conf.d/api.conf`, `docker-compose.prod.yml`,
> `docker/nginx/init-letsencrypt.sh`

---

## 0. 사전 준비
- **DNS**: `api.example.com` A레코드 → 배포 서버 공인 IP
- 서버 방화벽/보안그룹: **80·443만 외부 오픈**, `8080`(게이트웨이)은 외부 차단
- Docker + Docker Compose 설치
- ⚠️ Let's Encrypt는 **공인 도메인**이 필요하다. 로컬에는 발급 불가 → 로컬 리허설은 아래 "자체서명" 참고.

## 1. 도메인 값 치환 (2곳)
`api.example.com` 을 실제 도메인으로 바꾼다:
- `docker/nginx/conf.d/api.conf` — `server_name` 과 `ssl_certificate` 경로
- `docker/nginx/init-letsencrypt.sh` — `domain`, `email`

## 2. 최초 인증서 발급 + 기동 (서버에서 1회)
```bash
# 스테이징으로 먼저 검증 권장: init 스크립트의 staging=1 로 실행 → 성공 확인 후 staging=0 재실행
bash docker/nginx/init-letsencrypt.sh
```
스크립트가 하는 일: 더미 인증서로 nginx 기동 → certbot 웹루트 발급 → nginx reload → 갱신 루프 시작.

전체 스택 기동(앱 포함):
```bash
docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d --build
```

## 3. 앱/콘솔/프론트 설정 (HTTPS 전환의 실제 핵심)
- **서버의 `.env`**:
  ```
  OAUTH_REDIRECT_BASE=https://api.example.com
  CORS_ORIGINS=https://app.example.com
  ```
- **Google/Naver 콘솔**: 승인된 리디렉션 URI →
  `https://api.example.com/login/oauth2/code/google`, `.../naver`
- **Vercel 환경변수**:
  ```
  REACT_APP_API_BASE=https://api.example.com
  REACT_APP_WS_BASE=wss://api.example.com
  ```
- **쿠키**: app↔api 가 같은 상위도메인이라 `SameSite=Lax` 로 same-site 동작.
  auth 가 쿠키에 **`Secure`** 를 부여하는지 확인(https 필수).

## 4. 검증
```bash
curl -I https://api.example.com/oauth/.well-known/jwks.json   # 200 + 유효 인증서
docker compose -f docker-compose.yml -f docker-compose.prod.yml run --rm certbot renew --dry-run
```
- 브라우저: `https://app.example.com` 로그인 → 코드동기화(WSS `/ws-code`)·채팅(`/ws`) 동작 확인

## 5. 갱신
- `certbot` 컨테이너가 12시간마다 `renew` 시도(만료 30일 이내에만 실제 갱신).
- 갱신 후 nginx가 새 인증서를 읽도록 주기적 reload가 필요하면 cron으로:
  `docker compose ... exec nginx nginx -s reload`

---

## 트러블슈팅
- **nginx가 인증서 없다고 기동 실패**: init 스크립트를 아직 안 돌린 것. 2번 먼저 실행.
- **certbot 챌린지 실패(403/타임아웃)**: DNS A레코드/80 포트 오픈 확인. `/.well-known/acme-challenge/` 가 nginx 80 블록에서 웹루트로 서빙되는지.
- **레이트리밋**: 실패 반복 시 `staging=1` 로 테스트 후 성공하면 `staging=0`.
- **로그인 후 쿠키 미전송**: `CORS_ORIGINS`에 프론트 https 도메인 포함, 쿠키 `Secure`, 프론트 `withCredentials` 확인.
- **WebSocket 끊김**: `api.conf` 의 `Upgrade/Connection` 헤더와 `proxy_read_timeout` 확인.

## 로컬 자체서명 리허설 (도메인 없이 구조만 확인)
```bash
mkdir -p docker/nginx/certs
# api.conf 의 ssl_certificate 경로를 이 자체서명으로 임시 변경하거나, live 경로에 생성
openssl req -x509 -nodes -newkey rsa:2048 -days 365 \
  -keyout privkey.pem -out fullchain.pem -subj "/CN=localhost"
```
브라우저 경고를 무시하면 프록시/WS 경로는 로컬에서도 확인 가능하다. 실인증서는 배포 서버에서만.
