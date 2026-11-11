# ==================== STAGE 1: Build ====================
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn package -DskipTests -B

# Раскладываем jar по слоям для оптимизации кэша
RUN cp target/*.jar application.jar \
    && java -Djarmode=tools -jar application.jar extract --layers --destination extracted

# ==================== STAGE 2: Runtime ====================
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# От редко меняющегося к часто меняющемуся
COPY --from=build --chown=appuser:appgroup /app/extracted/dependencies/ ./
COPY --from=build --chown=appuser:appgroup /app/extracted/spring-boot-loader/ ./
COPY --from=build --chown=appuser:appgroup /app/extracted/snapshot-dependencies/ ./
COPY --from=build --chown=appuser:appgroup /app/extracted/application/ ./

USER appuser

# Конфигурация через application.yml (переменные из окружения)
ENTRYPOINT ["java", \
  "-XX:MaxRAMPercentage=65.0", \
  "-XX:+ExitOnOutOfMemoryError", \
  "-jar", "application.jar"]
