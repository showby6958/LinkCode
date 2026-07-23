#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────
#  Let's Encrypt 최초 인증서 발급 (배포 서버에서 1회 실행)
#
#  닭-달걀 문제 해결: nginx 443 블록은 인증서가 있어야 뜬다. 그래서
#    (1) 더미 자체서명 인증서를 만들어 nginx 를 먼저 띄우고
#    (2) certbot 웹루트로 실인증서를 발급받아 교체한 뒤
#    (3) nginx 를 reload 한다.
#
#  사용:  bash docker/nginx/init-letsencrypt.sh
#  (프로젝트 루트에서 실행. docker compose 가 필요.)
# ─────────────────────────────────────────────────────────────
set -euo pipefail

# ── 배포 전 값 확인/교체 ──────────────────────────────
domain="api.example.com"          # ⚠️ 실제 백엔드 도메인 (api.conf 와 동일해야 함)
email="you@example.com"           # ⚠️ 만료 알림 받을 이메일
staging=0                         # 1로 두면 LE 스테이징(테스트, 레이트리밋 회피). 성공 확인 후 0으로.
# ─────────────────────────────────────────────────────

compose="docker compose -f docker-compose.yml -f docker-compose.prod.yml"
cert_path="/etc/letsencrypt/live/$domain"

echo "### (1) 더미 인증서 생성 → nginx 기동용 ..."
$compose run --rm --entrypoint "\
  sh -c 'mkdir -p $cert_path && \
  openssl req -x509 -nodes -newkey rsa:2048 -days 1 \
    -keyout $cert_path/privkey.pem \
    -out $cert_path/fullchain.pem \
    -subj \"/CN=$domain\"'" certbot

echo "### nginx 기동 ..."
$compose up -d nginx

echo "### (2) 더미 인증서 제거 후 실인증서 요청 ..."
$compose run --rm --entrypoint "\
  rm -rf /etc/letsencrypt/live/$domain \
         /etc/letsencrypt/archive/$domain \
         /etc/letsencrypt/renewal/$domain.conf" certbot

staging_arg=""
if [ "$staging" != "0" ]; then staging_arg="--staging"; fi

$compose run --rm --entrypoint "\
  certbot certonly --webroot -w /var/www/certbot $staging_arg \
    -d $domain \
    --email $email --agree-tos --no-eff-email --force-renewal" certbot

echo "### (3) nginx reload (실인증서 적용) ..."
$compose exec nginx nginx -s reload

echo "### 완료. certbot 자동 갱신 루프 시작 ..."
$compose up -d certbot

echo "✅ https://$domain 준비 완료. 확인: curl -I https://$domain/oauth/.well-known/jwks.json"
