# ── Stage 1: Build ────────────────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-21-alpine AS builder
WORKDIR /app

# Cache de dependencias (se invalida solo si cambia pom.xml)
COPY pom.xml .
RUN mvn dependency:go-offline -q

COPY src ./src
RUN mvn package -DskipTests -q

# ── Stage 2: Run ──────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Usuario no-root
RUN addgroup -S orderly && adduser -S orderly -G orderly

COPY --from=builder /app/target/*.jar app.jar
RUN mkdir -p uploads && chown -R orderly:orderly /app

USER orderly

EXPOSE 8080
ENTRYPOINT ["java", \
  "-Xmx512m", "-Xms256m", \
  "-XX:+UseContainerSupport", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
