# syntax=docker/dockerfile:1
# Compilación multietapa: Maven con JDK compila; la imagen final solo lleva el JRE y el JAR.

FROM maven:3.9.9-eclipse-temurin-21 AS compilacion
WORKDIR /app
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn -B -q dependency:go-offline
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B -q -DskipTests package

FROM eclipse-temurin:21.0.5_11-jre-alpine
RUN addgroup -S app && adduser -S app -G app
WORKDIR /app
COPY --from=compilacion /app/target/motor-filas.jar app.jar
# Usuario sin privilegios
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
