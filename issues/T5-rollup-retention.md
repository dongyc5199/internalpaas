---
title: "[Feature] T5 — Rollup Jobs (5m/1h) & Retention Policy"
labels: ["feature","backend","storage","batch"]
assignees: []
date: 2025-10-13
---

## Summary
创建 5m/1h 连续聚合并执行原始数据保留（30–90 天可配），查询按范围自动选粒度。

## Goals
- 聚合 SQL（Timescale/ClickHouse/MV）
- 定时作业：5m/1h 聚合与回填
- 查询步长自动选择（15s/5m/1h）

## Deliverables
- `V2__rollup_views.sql`
- `RollupJob` + CRON 配置

## Acceptance Criteria
- **rollup_generation_correctness** 通过
- 宽时间范围查询走聚合表

## Dependencies
- T3

## Branch
`feat/t5-rollup-retention`

## Repo Changes
```
infra/db/migrations/V2__rollup_views.sql
hub/src/main/java/com/example/hub/rollup/RollupJob.java
hub/src/main/resources/application.yaml
```

## Test Plan
- Integration：插入样本 → 跑作业 → 校验聚合  
- Perf：对比历史 7d 查询时延下降

## Rollback
禁用作业；开发环境删除 MV
