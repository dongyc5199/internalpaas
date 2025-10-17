# Metrics Hub Security Guide

## 📋 Overview

This guide covers the security features implemented in Metrics Hub (T7: Security Hardening):

1. **TLS/mTLS** - Encrypted communication for gRPC and HTTP endpoints
2. **JWT/JWKS Authentication** - Bearer token validation with dynamic key fetching
3. **Rate Limiting** - Per-agent request throttling
4. **Log Masking** - Sensitive data protection in logs
5. **Certificate Rotation** - Automated expiry monitoring

---

## 🔐 TLS/mTLS Configuration

### Enabling TLS

TLS encrypts communication between agents and Metrics Hub, protecting data in transit.

**Configuration** (`application.yaml`):
```yaml
metrics-hub:
  security:
    tls:
      enabled: true  # Enable TLS
      mtls: false    # Disable mTLS (optional client certs)
      keystore:
        path: /path/to/keystore.p12
        password: ${KEYSTORE_PASSWORD}
        type: PKCS12
```

**Environment Variables**:
```bash
export TLS_ENABLED=true
export KEYSTORE_PATH=/etc/certs/metrics-hub.p12
export KEYSTORE_PASSWORD=SecurePassword123
```

### Enabling mTLS (Mutual TLS)

mTLS requires clients to present valid certificates, ensuring two-way authentication.

**Configuration**:
```yaml
metrics-hub:
  security:
    tls:
      enabled: true
      mtls: true  # Require client certificates
      truststore:
        path: /path/to/truststore.p12
        password: ${TRUSTSTORE_PASSWORD}
        type: PKCS12
```

**Environment Variables**:
```bash
export MTLS_ENABLED=true
export TRUSTSTORE_PATH=/etc/certs/ca-truststore.p12
export TRUSTSTORE_PASSWORD=AnotherSecurePassword
```

### Generating Certificates

#### Self-Signed Certificates (Development)

```bash
# Generate server keystore
keytool -genkeypair -alias metrics-hub \
  -keyalg RSA -keysize 2048 -validity 365 \
  -keystore /etc/certs/keystore.p12 \
  -storetype PKCS12 \
  -storepass changeit \
  -dname "CN=metrics-hub, OU=Dev, O=Example, L=City, ST=State, C=US"

# Export server certificate
keytool -exportcert -alias metrics-hub \
  -keystore /etc/certs/keystore.p12 \
  -file /etc/certs/server.crt \
  -storepass changeit

# Create truststore and import server cert
keytool -importcert -alias metrics-hub \
  -file /etc/certs/server.crt \
  -keystore /etc/certs/truststore.p12 \
  -storetype PKCS12 \
  -storepass changeit \
  -noprompt
```

#### CA-Signed Certificates (Production)

```bash
# Generate private key
openssl genrsa -out /etc/certs/server.key 2048

# Generate CSR
openssl req -new -key /etc/certs/server.key \
  -out /etc/certs/server.csr \
  -subj "/CN=metrics-hub.example.com/O=Example Inc/C=US"

# Sign with CA (send server.csr to CA, receive server.crt)

# Convert to PKCS12
openssl pkcs12 -export \
  -in /etc/certs/server.crt \
  -inkey /etc/certs/server.key \
  -out /etc/certs/keystore.p12 \
  -name metrics-hub \
  -passout pass:changeit
```

### Certificate Rotation

Metrics Hub monitors certificate expiry and logs warnings:

**Configuration**:
```yaml
metrics-hub:
  security:
    tls:
      cert-rotation:
        check-interval-hours: 24    # Check every 24 hours
        warn-days-before-expiry: 30 # Warn 30 days before expiry
```

**Rotation Process**:
1. **30 days before expiry**: Warning logs appear
2. **Replace certificate**: Update keystore file
3. **Reload SSL** (requires restart for gRPC, graceful for HTTP)
4. **Verify**: Check logs for successful reload

---

## 🔑 JWT/JWKS Authentication

### Overview

JWT authentication validates Bearer tokens using a JWKS (JSON Web Key Set) endpoint.

**Flow**:
1. Agent sends request with `Authorization: Bearer <token>`
2. Metrics Hub fetches public keys from JWKS URL
3. Validates token signature, issuer, and audience
4. Extracts agent ID from token claims
5. Applies rate limiting based on agent ID

### Configuration

```yaml
metrics-hub:
  security:
    jwt:
      enabled: true
      jwks-url: https://auth.example.com/.well-known/jwks.json
      issuer: https://auth.example.com
      audience: metrics-hub
      rate-limit:
        enabled: true
        requests-per-minute: 120
        burst-capacity: 10
```

**Environment Variables**:
```bash
export JWT_ENABLED=true
export JWKS_URL=https://auth.example.com/.well-known/jwks.json
export JWT_ISSUER=https://auth.example.com
export JWT_AUDIENCE=metrics-hub
```

### Token Requirements

**JWT Claims**:
```json
{
  "iss": "https://auth.example.com",
  "aud": ["metrics-hub"],
  "sub": "agent-12345",
  "agent_id": "server-001",
  "exp": 1735689600,
  "iat": 1735686000
}
```

**Required Claims**:
- `iss` (issuer): Must match `jwt.issuer` config
- `aud` (audience): Must contain `jwt.audience`
- `sub` or `agent_id`: Agent identifier for rate limiting
- `exp` (expiry): Token expiration timestamp

### Testing JWT Authentication

#### Valid Token Test

```bash
# Generate token (example using JWT.io or auth service)
TOKEN="eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9..."

# Send request with Bearer token
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:4318/v1/metrics
```

#### Invalid Token Test

```bash
# Missing Authorization header
curl http://localhost:4318/v1/metrics
# Expected: 401 Unauthorized

# Invalid signature
curl -H "Authorization: Bearer invalid.token.here" \
  http://localhost:4318/v1/metrics
# Expected: 401 Unauthorized

# Wrong issuer
curl -H "Authorization: Bearer <token-with-wrong-issuer>" \
  http://localhost:4318/v1/metrics
# Expected: 401 Unauthorized
```

---

## 🚦 Rate Limiting

### Per-Agent Rate Limiting

Each agent (identified by JWT token) is limited to prevent abuse:

**Configuration**:
```yaml
metrics-hub:
  security:
    jwt:
      rate-limit:
        enabled: true
        requests-per-minute: 120  # 120 requests/min per agent
        burst-capacity: 10        # Allow bursts up to 130 req/min
```

**Behavior**:
- Agent can send up to 120 requests per minute
- Burst capacity allows temporary spikes (up to 130)
- Excess requests receive `429 Too Many Requests`

**Monitoring**:
```java
@Autowired
private TokenAuthFilter tokenAuthFilter;

// Get rate limiter stats
Map<String, Long> stats = tokenAuthFilter.getRateLimiterStats();
// Returns: {"agent-001": 45, "agent-002": 78} (available tokens)
```

---

## 🔇 Log Masking

### Sensitive Data Protection

Metrics Hub automatically masks sensitive data in logs:

**Masked Patterns**:
- Passwords: `password=***MASKED***`
- Bearer tokens: `Bearer ***MASKED***`
- API keys: `api_key=***MASKED***`
- Connection strings: `jdbc:postgresql://host:***MASKED***@db`
- JWT tokens: `***MASKED***`

**Configuration** (`logback-spring.xml`):
```xml
<appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
  <filter class="com.cmict.metricshub.security.SensitiveDataFilter"/>
  <encoder>
    <layout class="com.cmict.metricshub.security.SensitiveDataFilter$MaskingPatternLayout">
      <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
    </layout>
  </encoder>
</appender>
```

**Example**:
```
// Before masking
2025-10-17 10:00:00 DEBUG - Connecting to jdbc:postgresql://localhost:5432/db?user=admin&password=secret123
2025-10-17 10:00:01 INFO - Received request with Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...

// After masking
2025-10-17 10:00:00 DEBUG - Connecting to jdbc:postgresql://localhost:5432/db?user=admin&password=***MASKED***
2025-10-17 10:00:01 INFO - Received request with Authorization: Bearer ***MASKED***
```

---

## 🔒 Production Deployment Checklist

### Pre-Deployment

- [ ] Generate CA-signed certificates
- [ ] Configure TLS keystores and truststores
- [ ] Set up JWKS endpoint (OAuth2/OIDC provider)
- [ ] Enable mTLS for agent-to-hub communication
- [ ] Configure environment variables (passwords via secrets manager)

### Deployment

- [ ] Enable TLS: `TLS_ENABLED=true`
- [ ] Enable mTLS: `MTLS_ENABLED=true`
- [ ] Enable JWT: `JWT_ENABLED=true`
- [ ] Set JWKS URL: `JWKS_URL=https://auth.prod.example.com/...`
- [ ] Configure rate limits (adjust for expected load)
- [ ] Verify log masking is active

### Post-Deployment

- [ ] Test TLS connection: `openssl s_client -connect hub:4317`
- [ ] Verify mTLS: Test with/without client certificate
- [ ] Validate JWT: Test with valid/invalid tokens
- [ ] Monitor logs for security events
- [ ] Set up certificate expiry alerts (30 days)

---

## 🛡️ Security Best Practices

### 1. Secrets Management

**DO NOT**:
- ❌ Store passwords in application.yaml
- ❌ Commit keystores to git
- ❌ Use default passwords in production

**DO**:
- ✅ Use environment variables: `${KEYSTORE_PASSWORD}`
- ✅ Use secrets managers (AWS Secrets Manager, HashiCorp Vault)
- ✅ Rotate secrets regularly (90-day cycle)

### 2. Certificate Management

- Use CA-signed certificates in production
- Set up automated certificate renewal (Let's Encrypt/ACME)
- Monitor certificate expiry (Metrics Hub logs warnings)
- Plan certificate rotation (requires application restart)

### 3. Access Control

- Enable mTLS for all production environments
- Issue unique client certificates per agent/server
- Revoke compromised certificates immediately
- Maintain certificate revocation list (CRL)

### 4. Monitoring

**Key Metrics**:
- Authentication failures (401 errors)
- Rate limit violations (429 errors)
- Certificate expiry warnings
- SSL handshake failures

**Alerting**:
- Certificate expires in < 30 days
- > 10 authentication failures per minute
- > 5% rate limit violations
- Any SSL handshake errors

---

## 🧪 Testing Guide

### Unit Tests

```bash
# Run security-related tests
cd hub
../mvnw.cmd test -Dtest=*SecurityTest
```

### Integration Tests

#### Test TLS Connection

```bash
# With TLS (should succeed)
grpcurl -d '{"metrics":[...]}' \
  -cacert /etc/certs/ca.crt \
  metrics-hub.example.com:4317 \
  opentelemetry.proto.collector.metrics.v1.MetricsService/Export

# Without TLS (should fail)
grpcurl -plaintext \
  metrics-hub.example.com:4317 \
  opentelemetry.proto.collector.metrics.v1.MetricsService/Export
```

#### Test mTLS

```bash
# With client certificate (should succeed)
grpcurl -d '{"metrics":[...]}' \
  -cacert /etc/certs/ca.crt \
  -cert /etc/certs/client.crt \
  -key /etc/certs/client.key \
  metrics-hub.example.com:4317 \
  opentelemetry.proto.collector.metrics.v1.MetricsService/Export

# Without client certificate (should fail)
grpcurl -d '{"metrics":[...]}' \
  -cacert /etc/certs/ca.crt \
  metrics-hub.example.com:4317 \
  opentelemetry.proto.collector.metrics.v1.MetricsService/Export
```

#### Test JWT Authentication

```bash
# Generate test token
TOKEN=$(curl -X POST https://auth.example.com/token \
  -d "grant_type=client_credentials" \
  -d "client_id=test-agent" \
  -d "client_secret=secret" | jq -r '.access_token')

# Valid token (should succeed)
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:4318/v1/metrics \
  -d '{"resourceMetrics":[...]}'

# Invalid token (should fail with 401)
curl -H "Authorization: Bearer invalid.token" \
  http://localhost:4318/v1/metrics
```

---

## 📚 References

- [OWASP Transport Layer Protection Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Transport_Layer_Protection_Cheat_Sheet.html)
- [JWT Best Practices (RFC 8725)](https://datatracker.ietf.org/doc/html/rfc8725)
- [NIST SP 800-52 Rev. 2 (TLS Guidelines)](https://nvlpubs.nist.gov/nistpubs/SpecialPublications/NIST.SP.800-52r2.pdf)
- [gRPC Authentication Guide](https://grpc.io/docs/guides/auth/)

---

**Document Version**: 1.0.0  
**Last Updated**: 2025-10-17  
**Task**: T7 - Security Hardening
