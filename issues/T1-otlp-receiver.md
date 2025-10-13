---
title: "[Feature] T1 — OTLP gRPC/HTTP Receiver (TLS-ready)"
labels: ["feature","backend","ingestion"]
assignees: []
date: 2025-10-13
---

## Summary
实现 OTLP 指标接收：**gRPC:4317** 与 **HTTP:4318**。收到 payload 后快速返回并入队（内存队列），暴露健康检查。TLS/mTLS 钩子预留（T7 完成加固）。

## Goals
- 接收 OTLP（gRPC/HTTP），成功入队。
- 端口/并发/最大包大小可配；基础可观测（QPS/队列深度）。
- Actuator 健康检查。

## Deliverables
- 运行中的 Spring Boot 服务：
  - `OtlpGrpcServer` / `OtlpHttpController`
  - `IngestQueue`（有界队列）
- `Dockerfile`（暴露 8080/4317/4318）
- CI 构建与基础测试

## Acceptance Criteria
- 集成用例 **otlp_end_to_end_ingest**：发送最小 OTLP → 202/OK（HTTP）或 OK（gRPC）
- 观察到请求计数/队列深度指标

## Dependencies
- 无

## Branch
`feat/t1-otlp-receiver`

## Repo Changes
```
hub/pom.xml (或 build.gradle)
hub/src/main/java/com/example/hub/HubApplication.java
hub/src/main/java/com/example/hub/ingest/OtlpGrpcServer.java
hub/src/main/java/com/example/hub/ingest/OtlpHttpController.java
hub/src/main/java/com/example/hub/ingest/IngestQueue.java
hub/src/main/resources/application.yaml
hub/Dockerfile
.github/workflows/ci.yml
```

## Implementation Plan
1) 搭建 Spring Boot 与 OTLP gRPC stub  
2) 增加 `/v1/metrics` HTTP 端点  
3) 两路都写入 `IngestQueue`；Micrometer 计数  
4) CI 构建与基础测试

## Test Plan
- Integration：POST 二进制到 `/v1/metrics`；grpcurl 调 4317  
- Unit：队列满/背压策略  
- E2E：本地跑服务 + 发送脚本冒烟测试

## Rollback
仅代码回滚，无 DB 变更
