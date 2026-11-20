FROM gradle:9.1.0-jdk21 AS build
WORKDIR /workspace
COPY build.gradle settings.gradle ./
COPY src/main ./src/main
RUN gradle --no-daemon bootJar

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN groupadd --gid 10001 app && useradd --uid 10001 --gid app --no-create-home app
COPY --from=build --chown=app:app /workspace/build/libs/*.jar /app/app.jar
USER app
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70 -Duser.timezone=America/Bogota"
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
