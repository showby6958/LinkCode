#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────
#  서버 배포 스크립트 (GitHub Actions 또는 수동 실행)
#  실행 위치와 무관하게 동작하도록 절대경로(~) 사용.
#
#  전제 레이아웃 (형제 디렉터리):
#    ~/apps/LinkCode   (이 레포, docker-compose 위치)
#    ~/ai-consumer     (AI-Analyze-Service)
#    ~/crdt-front      (LinkCode-React)
#
#  하는 일: 3개 레포 최신화 → 이미지 재빌드 → 기동 → nginx 재해석
# ─────────────────────────────────────────────────────────────
set -euo pipefail

LINKCODE_DIR="$HOME/apps/LinkCode"
AI_DIR="$HOME/ai-consumer"
FRONT_DIR="$HOME/crdt-front"
DC="docker compose -f docker-compose.yml -f docker-compose.prod.yml"

echo "==> [1/4] 레포 최신화"
git -C "$LINKCODE_DIR" pull --ff-only
git -C "$AI_DIR"       pull --ff-only
git -C "$FRONT_DIR"    pull --ff-only

cd "$LINKCODE_DIR"

echo "==> [2/4] 이미지 빌드 + 기동 (변경분만 캐시 재사용)"
$DC up -d --build

echo "==> [3/4] nginx 재시작 (업스트림 IP 재해석 → 502 방지)"
# P4(resolver) 적용 후에는 이 줄을 지워도 된다.
$DC restart nginx

echo "==> [4/4] 상태"
$DC ps

echo "==> 배포 완료"
