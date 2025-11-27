# ---- Build stage ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy pom.xml and download dependencies (better cache)
COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline

# Copy source code and build the app
COPY src ./src
RUN mvn -q -DskipTests package

# ---- Run stage ----
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Copy the built jar from the build stage
COPY --from=build /app/target/teamwork.project-0.0.1-SNAPSHOT.jar app.jar

# Expose the port (matches server.port in your Spring Boot app)
EXPOSE 8080

# Start the Spring Boot application
ENTRYPOINT ["java", "-jar", "app.jar"]
