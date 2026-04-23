#!/usr/bin/env bash
# start-frontend.sh — Start the ModelRouter React UI
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR/frontend"

if [ ! -d node_modules ]; then
  echo "📦 Installing dependencies..."
  npm install
fi

echo "🎨 Starting ModelRouter UI on http://localhost:5173"
npm run dev
