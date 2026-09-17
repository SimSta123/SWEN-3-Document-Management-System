# syntax=docker/dockerfile:1
# Official images: https://hub.docker.com/_/maven and https://hub.docker.com/_/eclipse-temurin
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /workspace
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw
COPY src/ src/
RUN --mount=type=cache,target=/root/.m2 ./mvnw --batch-mode --no-transfer-progress verify

FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
RUN addgroup -S paperless && adduser -S paperless -G paperless
COPY --from=build --chown=paperless:paperless /workspace/target/paperless-api-0.0.1-SNAPSHOT.jar app.jar
USER paperless
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
