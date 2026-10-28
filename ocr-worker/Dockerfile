# Same Java version as the backend.
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /workspace
COPY pom.xml ./
COPY src/ src/
RUN --mount=type=cache,target=/root/.m2 mvn --batch-mode --no-transfer-progress verify

FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
RUN addgroup -S paperless && adduser -S paperless -G paperless
COPY --from=build --chown=paperless:paperless /workspace/target/ocr-worker-0.0.1-SNAPSHOT.jar app.jar
USER paperless
ENTRYPOINT ["java", "-jar", "app.jar"]
