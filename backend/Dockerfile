# syntax=docker/dockerfile:1
# Backend (Java 25 / Spring Boot). Multi-stage, runtime non-root.

FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace
# Cache de dependências separado do código.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -q -DskipTests dependency:go-offline
COPY src/ src/
RUN ./mvnw -B -q -DskipTests package && cp target/*.jar /workspace/app.jar

FROM eclipse-temurin:25-jre
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --uid 10001 --create-home appuser
WORKDIR /app
COPY --from=build /workspace/app.jar /app/app.jar
USER 10001
EXPOSE 8080
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD curl -fsS http://localhost:8080/api/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
