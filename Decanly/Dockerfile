# syntax=docker/dockerfile:1
FROM maven:3.9.16-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml mvnw ./
COPY .mvn .mvn
COPY src src
# El secreto opcional permite usar un proxy Maven en entornos restringidos.
RUN --mount=type=secret,id=maven_settings --mount=type=secret,id=java_cacerts \
    if [ -f /run/secrets/java_cacerts ]; then export MAVEN_OPTS="-Djavax.net.ssl.trustStore=/run/secrets/java_cacerts"; fi; \
    if [ -f /run/secrets/maven_settings ]; then \
      mvn -B -ntp -s /run/secrets/maven_settings verify; \
    else mvn -B -ntp verify; fi

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
RUN groupadd --gid 10001 decanly && useradd --uid 10001 --gid decanly --no-create-home decanly
COPY --from=build --chown=decanly:decanly /src/target/Decanly-0.0.1-SNAPSHOT.war /app/app.war
USER 10001:10001
ENV PORT=8080 SPRING_PROFILES_ACTIVE=prod
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.war"]
