# ---- STAGE 1: Build the JAR (Maven + Java 17) ----
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /workspace

# Cache deps first (faster rebuilds)
COPY pom.xml ./
#COPY .mvn/ .mvn/
#COPY mvnw .        
RUN mvn -B -DskipTests dependency:go-offline || true

# Build
COPY src ./src
RUN mvn -B -DskipTests package

# ---- STAGE 2: Runtime (small JRE) ----
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Adjust the glob to your final jar name if needed
COPY --from=builder /workspace/target/*-SNAPSHOT.jar /app/app.jar

# Run as non-root
RUN useradd -r -u 1001 appuser && chown -R appuser:appuser /app
USER appuser

EXPOSE 8080
ENV JAVA_OPTS=""
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]

# line to check the test run-4
