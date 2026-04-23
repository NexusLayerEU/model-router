#!/usr/bin/env bash
# deploy-staging.sh
# Syncs source to staging server and builds + runs Docker containers there.
# Usage: ./deploy-staging.sh
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/.env.staging"

STAGING_HOST="${STAGING_HOST:?STAGING_HOST not set}"
STAGING_USER="${STAGING_USER:?STAGING_USER not set}"
STAGING_PASS="${STAGING_PASS:?STAGING_PASS not set}"
STAGING_DIR="${STAGING_DIR:-/home/${STAGING_USER}/ModelRouter}"

SSH_OPTS="-o StrictHostKeyChecking=accept-new"
SSHPASS="sshpass -p ${STAGING_PASS}"

echo ""
echo "╔══════════════════════════════════════════╗"
echo "║   ModelRouter — Docker Deploy to Staging ║"
echo "╚══════════════════════════════════════════╝"
echo "   Target  : ${STAGING_USER}@${STAGING_HOST}:${STAGING_DIR}"
echo "   API     : http://${STAGING_HOST}:8082/v1/messages"
echo "   UI      : http://${STAGING_HOST}:3002"
echo ""

# ── 1. Package source (exclude build artifacts) ──────────────────────────────
echo "▶  Packaging source…"
tar --exclude='./.git' \
    --exclude='./backend/target' \
    --exclude='./frontend/node_modules' \
    --exclude='./frontend/dist' \
    --exclude='./backend/data' \
    --exclude='./.env.staging' \
    -czf /tmp/modelrouter-src.tar.gz -C "$SCRIPT_DIR" .

# ── 2. Upload source ──────────────────────────────────────────────────────────
echo "▶  Uploading source to staging…"
$SSHPASS ssh $SSH_OPTS ${STAGING_USER}@${STAGING_HOST} "mkdir -p ${STAGING_DIR}"
$SSHPASS scp $SSH_OPTS /tmp/modelrouter-src.tar.gz "${STAGING_USER}@${STAGING_HOST}:/tmp/"
rm -f /tmp/modelrouter-src.tar.gz

# ── 3. Extract, build, and start on staging ───────────────────────────────────
echo "▶  Building and starting containers on staging…"
$SSHPASS ssh $SSH_OPTS ${STAGING_USER}@${STAGING_HOST} "
  set -e
  tar -xzf /tmp/modelrouter-src.tar.gz -C ${STAGING_DIR} 2>/dev/null
  rm -f /tmp/modelrouter-src.tar.gz
  cd ${STAGING_DIR}
  docker compose build
  docker compose down --remove-orphans 2>/dev/null || true
  docker compose up -d
"

# ── 4. Health check ───────────────────────────────────────────────────────────
echo "▶  Waiting for backend health…"
for i in $(seq 1 12); do
  if curl -sf "http://${STAGING_HOST}:8082/v1/health" > /dev/null 2>&1; then
    echo ""
    echo "✅  Deployment successful!"
    echo "   🔀 API  : http://${STAGING_HOST}:8082/v1/messages"
    echo "   🖥  UI   : http://${STAGING_HOST}:3002"
    echo "   ❤️  Health: http://${STAGING_HOST}:8082/v1/health"
    echo ""
    exit 0
  fi
  echo "   Waiting… (${i}/12)"; sleep 5
done

echo "⚠️  Health check timed out."
echo "   ssh ${STAGING_USER}@${STAGING_HOST} 'docker logs modelrouter-backend'"
exit 1
