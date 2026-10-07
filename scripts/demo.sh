#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")/.."
(cd frontend && npm ci && npm run build)
./mvnw -B package -DskipTests
exec java -jar target/springpractice-0.0.1-SNAPSHOT.jar --spring.profiles.active=demo
