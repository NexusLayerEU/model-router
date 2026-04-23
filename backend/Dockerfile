# ── Stage 1: Build ─────────────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app
COPY pom.xml .
# Download dependencies first (cached layer)
RUN mvn dependency:go-offline -q

COPY src ./src
RUN mvn clean package -DskipTests -q

# ── Stage 2: Runtime ────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Create non-root user
RUN addgroup -S modelrouter && adduser -S modelrouter -G modelrouter

# Data directory for H2 database
RUN mkdir -p /app/data && chown modelrouter:modelrouter /app/data

COPY --from=build /app/target/model-router-*.jar app.jar

USER modelrouter

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=15s --retries=3 \
  CMD wget -qO- http://localhost:8080/v1/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
