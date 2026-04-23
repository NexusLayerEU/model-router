#!/usr/bin/env bash
# start-backend.sh — Start the ModelRouter Spring Boot backend
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR/backend"

# Use Java 21 if not already the active JVM
JAVA21_HOME=/Library/Java/JavaVirtualMachines/zulu-21.jdk/Contents/Home
if [ -d "$JAVA21_HOME" ]; then
  export JAVA_HOME="$JAVA21_HOME"
fi

echo "🔀 Starting ModelRouter backend on http://localhost:8080"
echo "   Using Java: $(java -version 2>&1 | head -1)"
mvn spring-boot:run
