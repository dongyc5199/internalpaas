#!/bin/bash

# ============================================================
# Metrics Hub - Quick Start Script
# ============================================================
# This script provides convenient commands to:
# - Build the project
# - Run tests
# - Start the service
# - Build Docker image
# ============================================================

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Function to check prerequisites
check_prerequisites() {
    print_info "Checking prerequisites..."

    if ! command -v java &> /dev/null; then
        print_error "Java is not installed. Please install Java 17 or higher."
        exit 1
    fi

    JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
    if [ "$JAVA_VERSION" -lt 17 ]; then
        print_error "Java version must be 17 or higher. Current version: $JAVA_VERSION"
        exit 1
    fi

    if ! command -v mvn &> /dev/null; then
        print_error "Maven is not installed. Please install Maven 3.6 or higher."
        exit 1
    fi

    print_success "Prerequisites check passed"
}

# Function to build the project
build() {
    print_info "Building Metrics Hub..."
    mvn clean compile
    print_success "Build completed"
}

# Function to run tests
test() {
    print_info "Running tests..."
    mvn test
    print_success "Tests completed"
}

# Function to package the application
package() {
    print_info "Packaging application..."
    mvn clean package -DskipTests
    print_success "Package created: target/metrics-hub-*.jar"
}

# Function to run the application
run() {
    print_info "Starting Metrics Hub..."
    print_info "Ports:"
    print_info "  - 8080: Main HTTP API and Actuator"
    print_info "  - 4317: OTLP gRPC endpoint"
    print_info "  - 4318: OTLP HTTP endpoint"
    print_info ""

    if [ ! -f target/metrics-hub-*.jar ]; then
        print_warning "JAR file not found. Building..."
        package
    fi

    java -jar target/metrics-hub-*.jar

}

# Function to run in development mode
dev() {
    print_info "Starting Metrics Hub in development mode..."
    mvn spring-boot:run -Dspring-boot.run.profiles=dev
}

# Function to build Docker image
docker_build() {
    print_info "Building Docker image..."

    if ! command -v docker &> /dev/null; then
        print_error "Docker is not installed."
        exit 1
    fi

    docker build -t metrics-hub:latest .
    print_success "Docker image built: metrics-hub:latest"
}

# Function to run Docker container
docker_run() {
    print_info "Starting Metrics Hub in Docker..."

    if ! docker images | grep -q "metrics-hub"; then
        print_warning "Docker image not found. Building..."
        docker_build
    fi

    docker run -d \
        --name metrics-hub \
        -p 8080:8080 \
        -p 4317:4317 \
        -p 4318:4318 \
        metrics-hub:latest

    print_success "Metrics Hub started in Docker"
    print_info "View logs: docker logs -f metrics-hub"
    print_info "Stop container: docker stop metrics-hub"
    print_info "Remove container: docker rm metrics-hub"
}

# Function to check health
health_check() {
    print_info "Checking Metrics Hub health..."

    if ! curl -f http://localhost:8080/actuator/health &> /dev/null; then
        print_error "Health check failed. Is the service running?"
        exit 1
    fi

    print_success "Service is healthy"
    echo ""
    curl -s http://localhost:8080/actuator/health | jq . 2>/dev/null || \
        curl -s http://localhost:8080/actuator/health
}

# Function to show help
show_help() {
    cat << EOF
Metrics Hub - Quick Start Script

Usage: $0 [COMMAND]

Commands:
    check       Check prerequisites (Java, Maven)
    build       Build the project
    test        Run tests
    package     Package the application (creates JAR)
    run         Run the application (build if needed)
    dev         Run in development mode (with hot reload)
    docker-build    Build Docker image
    docker-run      Run in Docker container
    health      Check service health
    help        Show this help message

Examples:
    $0 dev              # Start in dev mode
    $0 package && $0 run    # Package and run
    $0 docker-build && $0 docker-run    # Build and run in Docker
    $0 health           # Check if service is running

EOF
}

# Main script
case "$1" in
    check)
        check_prerequisites
        ;;
    build)
        check_prerequisites
        build
        ;;
    test)
        check_prerequisites
        test
        ;;
    package)
        check_prerequisites
        package
        ;;
    run)
        check_prerequisites
        run
        ;;
    dev)
        check_prerequisites
        dev
        ;;
    docker-build)
        docker_build
        ;;
    docker-run)
        docker_run
        ;;
    health)
        health_check
        ;;
    help|--help|-h)
        show_help
        ;;
    *)
        print_error "Unknown command: $1"
        echo ""
        show_help
        exit 1
        ;;
esac
