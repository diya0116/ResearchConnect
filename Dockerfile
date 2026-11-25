FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /workspace

COPY pom.xml ./
COPY .mvn/ .mvn/
RUN mvn -B -ntp dependency:go-offline

COPY src/ src/
RUN mvn -B -ntp package

FROM eclipse-temurin:21-jre

WORKDIR /app
COPY --from=build --chown=10001:0 /workspace/target/research-connect-0.0.1-SNAPSHOT.jar /app/app.jar

USER 10001
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
