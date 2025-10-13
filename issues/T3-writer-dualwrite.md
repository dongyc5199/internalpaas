---
title: "[Feature] T3 — Dual-write to Redis (hot) & TSDB (raw)"
labels: ["feature","backend","storage"]
assignees: []
date: 2025-10-13
---

## Summary
把归一化 `MetricSample` 批量写入 **Redis**（最新值 + 短窗）与 **PostgreSQL/Timescale**（原始时序），保证幂等。

## Goals
- Redis：`metrics:latest:{serverId}`（HASH）；可选 ZSET 窗口
- DB：`server_metric_samples` 表 + JPA
- 写入流水线与基础背压/重试

## Deliverables
- `WritePipeline`、`RedisWriter`、`TsdbWriter`（含实体映射）
- Flyway：`V1__create_server_metric_samples.sql`
- 配置：`application.yaml`（DB/Redis）

## Acceptance Criteria
- 集成 **dual_write_consistency** 通过
- P95 写入时延在目标范围

## Dependencies
- T2

## Branch
`feat/t3-writer`

## Repo Changes
```
infra/db/migrations/V1__create_server_metric_samples.sql
hub/src/main/java/com/example/hub/write/RedisWriter.java
hub/src/main/java/com/example/hub/write/TsdbWriter.java
hub/src/main/java/com/example/hub/write/WritePipeline.java
hub/src/main/resources/application.yaml
```

## Test Plan
- Integration：写入 → 读 Redis/DB 比对  
- Perf(smoke)：小批量循环写，观测时延

## Rollback
保留表结构，代码回滚
