# ============================================================
# Metrics Hub - Multi-stage Docker Build
# ============================================================
# This Dockerfile creates a containerized version of the Metrics Hub
# service for OTLP metrics ingestion.
#
# Build:
#   docker build -t metrics-hub:0.1.0 .
#
# Run:
#   docker run -p 8080:8080 -p 4317:4317 -p 4318:4318 \
#     --name metrics-hub metrics-hub:0.1.0
#
# Exposed Ports:
#   - 8080: Main HTTP API and Actuator
#   - 4317: OTLP gRPC endpoint
#   - 4318: OTLP HTTP endpoint
# ============================================================

# ============================================================
# Stage 1: Build
# ============================================================
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /app

# Copy pom.xml and download dependencies (cached layer)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests -B

# ============================================================
# Stage 2: Runtime
# ============================================================
FROM eclipse-temurin:17-jre-alpine

LABEL maintainer="CMICT PaaS Team"
LABEL description="Metrics Hub - OTLP Metrics Ingestion Service"
LABEL version="0.1.0"

WORKDIR /app

# Create non-root user for security
RUN addgroup -S metricshub && adduser -S metricshub -G metricshub

# Copy the built artifact from build stage
COPY --from=build /app/target/*.jar app.jar

# Create logs directory
RUN mkdir -p /app/logs && chown -R metricshub:metricshub /app

# Switch to non-root user
USER metricshub

# Expose ports
EXPOSE 8080 4317 4318

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# JVM options for optimal performance
ENV JAVA_OPTS="-Xms512m -Xmx1g \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200 \
  -XX:+UseStringDeduplication \
  -Djava.security.egd=file:/dev/./urandom"

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
