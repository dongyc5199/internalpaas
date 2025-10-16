# Metrics Hub - OTLP Receiver Service

**Version**: 0.1.0
**Task**: T1 - OTLP gRPC/HTTP Receiver (TLS-ready)
**Status**: ✅ Development Complete

---

## 📋 Overview

Metrics Hub is a high-performance OTLP (OpenTelemetry Protocol) metrics ingestion service that receives metrics data via both gRPC and HTTP protocols, queues them for processing, and provides comprehensive monitoring capabilities.

### Key Features

- ✅ **Dual Protocol Support**: gRPC (port 4317) and HTTP (port 4318)
- ✅ **Bounded Queue**: Configurable in-memory queue with backpressure handling
- ✅ **Health Monitoring**: Built-in health checks and Prometheus metrics
- ✅ **Production Ready**: Docker support, graceful shutdown, comprehensive logging
- 🔒 **TLS Ready**: Infrastructure prepared for TLS/mTLS (T7)

---

## 🚀 Quick Start

### Prerequisites

- Java 17+
- Maven 3.6+
- Docker (optional)

### Run Locally

```bash
# Build
mvn clean package

# Run
java -jar target/metrics-hub-0.1.0-SNAPSHOT.jar

# Or with Maven
mvn spring-boot:run
```

### Run with Docker

```bash
# Build image
docker build -t metrics-hub:0.1.0 .

# Run container
docker run -p 8080:8080 -p 4317:4317 -p 4318:4318 \
  --name metrics-hub \
  metrics-hub:0.1.0
```

---

## 🔌 Endpoints

### OTLP Ingestion

| Protocol | Port | Endpoint | Description |
|----------|------|----------|-------------|
| **gRPC** | 4317 | `MetricsService.Export` | OTLP gRPC metrics export |
| **HTTP** | 4318 | `POST /v1/metrics` | OTLP HTTP metrics export |

### Management

| Endpoint | Description |
|----------|-------------|
| `GET /actuator/health` | Service health check |
| `GET /actuator/metrics` | Micrometer metrics |
| `GET /actuator/prometheus` | Prometheus metrics endpoint |
| `GET /v1/metrics/health` | HTTP ingestion health |
| `GET /v1/metrics/stats` | HTTP ingestion statistics |

---

## 📊 Testing

### Send Test Metrics via HTTP

```bash
# Create mock OTLP payload (simplified example)
echo -n "mock_otlp_data" > payload.bin

# Send via HTTP
curl -X POST http://localhost:4318/v1/metrics \
  -H "Content-Type: application/x-protobuf" \
  --data-binary @payload.bin

# Expected response:
# {"status":"accepted","queueDepth":1}
```

### Send Test Metrics via gRPC

```bash
# Using grpcurl (install: https://github.com/fullstorydev/grpcurl)
grpcurl -plaintext \
  -d '{"resource_metrics":[]}' \
  localhost:4317 \
  opentelemetry.proto.collector.metrics.v1.MetricsService/Export
```

### Check Health

```bash
# Overall health
curl http://localhost:8080/actuator/health

# Expected response:
# {
#   "status": "UP",
#   "components": {
#     "ingestQueue": {
#       "status": "HEALTHY",
#       "details": {
#         "queueDepth": 0,
#         "queueCapacity": 10000,
#         "queueUtilization": "0.00%"
#       }
#     },
#     "grpcServer": {
#       "status": "RUNNING",
#       "details": {
#         "port": 4317,
#         "protocol": "grpc"
#       }
#     }
#   }
# }
```

### View Metrics

```bash
# Micrometer metrics
curl http://localhost:8080/actuator/metrics

# Specific metric
curl http://localhost:8080/actuator/metrics/ingest.queue.depth

# Prometheus format
curl http://localhost:8080/actuator/prometheus
```

---

## ⚙️ Configuration

Key configuration options in `application.yaml`:

```yaml
metrics-hub:
  ingest:
    queue:
      capacity: 10000              # Queue capacity
      offer-timeout-ms: 100        # Enqueue timeout

    http:
      max-payload-size: 4194304    # 4MB max payload

    grpc:
      port: 4317
      max-inbound-message-size: 4194304
```

### Profiles

- `dev`: Development (smaller queue, debug logging)
- `prod`: Production (larger queue, optimized logging)
- `test`: Testing (minimal queue, error logging)

---

## 📈 Monitoring Metrics

### Queue Metrics

| Metric | Type | Description |
|--------|------|-------------|
| `ingest.queue.depth` | Gauge | Current queue depth |
| `ingest.queue.capacity` | Gauge | Queue capacity |
| `ingest.queue.enqueue.success` | Counter | Successful enqueues |
| `ingest.queue.enqueue.rejected` | Counter | Rejected enqueues (full) |
| `ingest.queue.dequeue` | Counter | Dequeue operations |

### HTTP Metrics

| Metric | Type | Description |
|--------|------|-------------|
| `otlp.http.requests` | Counter | Total HTTP requests |
| `otlp.http.success` | Counter | Successful requests |
| `otlp.http.rejected` | Counter | Rejected (queue full) |
| `otlp.http.oversized` | Counter | Oversized payloads |
| `otlp.http.duration` | Timer | Request duration |

### gRPC Metrics

| Metric | Type | Description |
|--------|------|-------------|
| `otlp.grpc.requests` | Counter | Total gRPC requests |
| `otlp.grpc.success` | Counter | Successful requests |
| `otlp.grpc.rejected` | Counter | Rejected (queue full) |
| `otlp.grpc.duration` | Timer | Request duration |

---

## 🧪 Running Tests

```bash
# Run all tests
mvn test

# Run specific test
mvn test -Dtest=OtlpHttpEndpointTest

# Run with coverage
mvn test jacoco:report
```

---

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    Metrics Hub                          │
│                                                         │
│  ┌──────────────┐              ┌──────────────┐        │
│  │ HTTP:4318    │              │ gRPC:4317    │        │
│  │              │              │              │        │
│  │ OtlpHttp     │              │ OtlpGrpc     │        │
│  │ Controller   │              │ Server       │        │
│  └──────┬───────┘              └──────┬───────┘        │
│         │                             │                │
│         └──────────┬──────────────────┘                │
│                    │                                   │
│              ┌─────▼─────┐                             │
│              │           │                             │
│              │ Ingest    │  Bounded Queue (10k)        │
│              │ Queue     │  Backpressure Support       │
│              │           │  Metrics Collection         │
│              └─────┬─────┘                             │
│                    │                                   │
│              ┌─────▼─────┐                             │
│              │ Processing│  (T2: Normalizer)           │
│              │ Pipeline  │  (Future Tasks)             │
│              └───────────┘                             │
│                                                         │
│  ┌──────────────────────────────────────────────────┐  │
│  │ Actuator: /actuator/health, /metrics, /prometheus│ │
│  └──────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────┘
```

---

## 📁 Project Structure

```
hub/
├── src/
│   ├── main/
│   │   ├── java/com/cmict/metricshub/
│   │   │   ├── HubApplication.java          # Main application
│   │   │   ├── config/                      # Configuration classes
│   │   │   │   ├── TomcatConfig.java        # Multi-port config
│   │   │   │   ├── IngestQueueHealthIndicator.java
│   │   │   │   └── GrpcServerHealthIndicator.java
│   │   │   └── ingest/                      # Ingestion components
│   │   │       ├── IngestQueue.java         # Bounded queue
│   │   │       ├── MetricsPayload.java      # Payload wrapper
│   │   │       ├── OtlpHttpController.java  # HTTP endpoint
│   │   │       ├── OtlpGrpcServer.java      # gRPC server
│   │   │       └── OtlpMetricsServiceImpl.java # gRPC service
│   │   ├── proto/                           # Protobuf definitions
│   │   │   └── opentelemetry/proto/collector/metrics/v1/
│   │   │       └── metrics_service.proto
│   │   └── resources/
│   │       └── application.yaml             # Configuration
│   └── test/
│       └── java/com/cmict/metricshub/
│           ├── OtlpHttpEndpointTest.java    # Integration test
│           └── IngestQueueTest.java         # Unit test
├── Dockerfile                               # Container image
├── .dockerignore
├── pom.xml                                  # Maven build
└── README.md                                # This file
```

---

## 🔄 Next Steps (Roadmap)

### T2: Normalizer (Next)
- Parse OTLP protobuf payloads
- Extract metrics and metadata
- Normalize to internal format

### T3: Dual-write Storage
- Redis writer (hot data, 5min TTL)
- TimescaleDB writer (historical data)

### T7: Security Hardening
- TLS/mTLS support
- Certificate management
- Authentication/Authorization

---

## 🐛 Troubleshooting

### Port Already in Use

```bash
# Check what's using port 4317
netstat -tuln | grep 4317

# Kill the process
lsof -ti:4317 | xargs kill -9
```

### gRPC Not Working

Check that protobuf compilation succeeded:

```bash
mvn clean compile
ls target/generated-sources/protobuf/
```

### Queue Full Errors

Increase queue capacity in `application.yaml`:

```yaml
metrics-hub:
  ingest:
    queue:
      capacity: 50000  # Increase from default 10000
```

---

## 📄 License

Internal CMICT PaaS Project

---

## 👥 Team

**CMICT PaaS Team**
**Task Owner**: T1 Development Team
**Last Updated**: 2025-10-15
