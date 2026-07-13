# ── Stage 1: Build ────────────────────────────────────────────────────────────
# Uses a full JDK image to compile and package the application.
# This stage is discarded — only the jar is carried forward.
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /build

COPY mvnw pom.xml ./
COPY .mvn .mvn

# Download dependencies separately so they are cached in Docker layer.
# This layer only rebuilds when pom.xml changes — not on every source change.
RUN ./mvnw dependency:go-offline -q

COPY src src

RUN ./mvnw package -DskipTests -q

# ── Stage 2: Runtime ──────────────────────────────────────────────────────────
# Uses a minimal JRE image — no compiler, no build tools.
# Result is a significantly smaller image than using JDK at runtime.
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Non-root user for security — never run as root in production
RUN addgroup -S jobtrackr && adduser -S jobtrackr -G jobtrackr
USER jobtrackr

# Copy only the fat jar from the builder stage
COPY --from=builder /build/target/*.jar app.jar

# Actuator health endpoint — used by Render and Docker health checks
HEALTHCHECK --interval=30s --timeout=10s --retries=3 \
    CMD wget -qO- http://localhost:8080/actuator/health || exit 1

EXPOSE 8080

ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-jar", "app.jar"]