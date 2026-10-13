# Multi-stage build for Product Similar API
FROM maven:3.9.5-eclipse-temurin-17 AS builder

# Set working directory
WORKDIR /build

# Copy pom.xml and download dependencies
COPY pom.xml .
RUN mvn -q dependency:resolve

# Copy source code
COPY src ./src

# Build the application
RUN mvn -q clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jre

# Install curl for health checks
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*

# Create app user
RUN groupadd -r appuser && useradd -r -g appuser -s /bin/sh -m appuser

# Set working directory
WORKDIR /app

# Copy JAR from builder
COPY --from=builder /build/target/product-similar-app-1.0.0.jar application.jar

# Change ownership
RUN chown -R appuser:appuser /app

# Switch to app user
USER appuser

# Expose port
EXPOSE 5001

# Run the application
ENTRYPOINT ["java", "-jar", "application.jar"]
