---
title: "[Feature] T7 — Security Hardening (mTLS, Bearer/JWKS, Secrets)"
labels: ["feature","backend","security"]
assignees: []
date: 2025-10-13
---

## Summary
为 OTLP（gRPC/HTTP）开启 TLS/mTLS；Bearer 经 **JWKS** 验证；敏感信息 at-rest 加密与日志脱敏。

## Goals
- gRPC/HTTP TLS；生产启用 **mTLS**
- JWKS 验证 Bearer；按 Agent 做限流
- keystore/truststore 配置、轮转；日志脱敏

## Deliverables
- `TlsConfig`（gRPC Netty + Spring HTTP）
- `TokenAuthFilter` / 安全配置
- `application.yaml` 安全配置项 + 文档

## Acceptance Criteria
- `mtls_required_and_verified`、`token_rejection_invalid_signature` 通过
- 日志无明文敏感信息

## Dependencies
- T1、T4

## Branch
`feat/t7-security-hardening`

## Repo Changes
```
hub/src/main/java/com/example/hub/security/TlsConfig.java
hub/src/main/java/com/example/hub/security/TokenAuthFilter.java
hub/src/main/resources/application.yaml
docs/Security.md
```

## Test Plan
- Integration：grpcurl 带/不带客户端证书；HTTP 带/不带 Bearer  
- Automation：证书到期预警演练

## Rollback
通过开关禁用 mTLS（仅非生产）；代码回滚
