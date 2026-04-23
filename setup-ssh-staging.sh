#!/usr/bin/env bash
# setup-ssh-staging.sh
# Run this ONCE to install your SSH public key on the staging server.
# After this, deploy-staging.sh works without a password.
#
# Usage: ./setup-ssh-staging.sh
#
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/.env.staging"

STAGING_HOST="${STAGING_HOST:?}"
STAGING_USER="${STAGING_USER:?}"

echo "🔑  Setting up SSH key authentication for ${STAGING_USER}@${STAGING_HOST}"
echo ""

# Generate a key if none exists
if [ ! -f ~/.ssh/id_ed25519 ]; then
  echo "▶  Generating SSH key (~/.ssh/id_ed25519)…"
  ssh-keygen -t ed25519 -C "modelrouter-deploy-$(hostname)" -f ~/.ssh/id_ed25519 -N ""
else
  echo "✓  SSH key already exists: ~/.ssh/id_ed25519"
fi

echo ""
echo "▶  Copying public key to ${STAGING_USER}@${STAGING_HOST}…"
echo "   (You will be prompted for the password ONE last time)"
echo ""

ssh-copy-id -i ~/.ssh/id_ed25519.pub "${STAGING_USER}@${STAGING_HOST}"

echo ""
echo "✅  SSH key installed. You can now run:"
echo "   ./deploy-staging.sh   (no password required)"
