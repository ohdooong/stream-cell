# syntax=docker/dockerfile:1
# Match build.gradle's Java 24 toolchain. See the production guide for support status.
FROM eclipse-temurin:24.0.2_12-jdk-noble AS build
WORKDIR /workspace
COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew
COPY src ./src
RUN --mount=type=cache,target=/root/.gradle ./gradlew --no-daemon bootJar

FROM eclipse-temurin:24.0.2_12-jre-noble AS runtime
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --gid 10001 streamcell \
    && useradd --uid 10001 --gid 10001 --no-create-home streamcell \
    && mkdir -p /app /data/uploads/flink/artifacts \
    && chown -R 10001:10001 /app /data/uploads
WORKDIR /app
COPY --from=build --chown=10001:10001 /workspace/build/libs/streamcell.jar /app/streamcell.jar
USER 10001:10001
ENV SPRING_PROFILES_ACTIVE=prod \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError -Djava.io.tmpdir=/tmp"
EXPOSE 8085
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=5 \
    CMD curl --fail --silent --max-time 4 http://127.0.0.1:8085/actuator/health/readiness > /dev/null || exit 1
ENTRYPOINT ["java", "-jar", "/app/streamcell.jar"]
