# -- Build stage -------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /app

# Copy pom.xml first so Docker can cache dependency resolution.
COPY pom.xml .
RUN mvn dependency:go-offline -q

# Copy the source and build the runnable jar.
COPY src ./src
RUN mvn package -q -DskipTests

# -- Runtime stage -----------------------------------------------------------
FROM eclipse-temurin:17-jre

WORKDIR /app

COPY --from=builder /app/target/ai-chat-assistant-1.0.jar app.jar

# Keep stdin open for interactive CLI chat.
ENTRYPOINT ["java", "-jar", "app.jar"]
