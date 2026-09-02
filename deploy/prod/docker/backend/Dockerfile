FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /workspace

COPY pom.xml .
RUN mvn -B -DskipTests dependency:go-offline

COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre

WORKDIR /app

RUN addgroup --system notask && adduser --system --ingroup notask notask

COPY --from=build /workspace/target/*.jar /app/app.jar

RUN mkdir -p /app/logs && chown -R notask:notask /app

USER notask

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
