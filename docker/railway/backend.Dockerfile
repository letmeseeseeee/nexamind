FROM maven:3.9.9-eclipse-temurin-17-alpine AS builder

WORKDIR /build
COPY NexaMind/pom.xml ./pom.xml
RUN mvn -B dependency:go-offline

COPY NexaMind/src ./src
RUN mvn -B clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine

RUN apk add --no-cache curl postgresql-client \
    && addgroup -S nexamind \
    && adduser -S -G nexamind nexamind

WORKDIR /app

COPY --from=builder /build/target/*.jar /app/app.jar
COPY docs/sql/01_init.sql /app/db/01_init.sql
COPY docker/railway/backend-entrypoint.sh /app/backend-entrypoint.sh

RUN chmod +x /app/backend-entrypoint.sh \
    && chown -R nexamind:nexamind /app

USER nexamind

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=70 -XX:+UseG1GC"

EXPOSE 8088

HEALTHCHECK --interval=30s --timeout=10s --start-period=120s --retries=5 \
    CMD curl -fsS "http://localhost:${PORT:-8088}/api/health" || exit 1

ENTRYPOINT ["/app/backend-entrypoint.sh"]
