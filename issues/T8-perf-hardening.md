---
title: "[Feature] T8 — Performance Hardening @ 1k Hosts"
labels: ["feature","backend","performance"]
assignees: []
date: 2025-10-13
---

## Summary
在 1k 主机 × 10–15s 周期下优化：接入背压、批量、线程池、Redis/DB 连接池；提供负载发生器与性能报告。

## Goals
- 调优 OTLP 批处理 & 背压
- Redis/DB 连接池与批量写
- 负载脚本与观测面板

## Deliverables
- `infra/perf/otlp_load_gen.sh`、`hub/src/perf/LoadSimulator.java`
- 线程池/连接池配置参考
- 性能报告（时延/QPS/CPU/GC）

## Acceptance Criteria
- `otlp_high_fan_in_load`、`redis_recent_window_read_latency`、`tsdb_7d_query_latency` 通过
- P95 时延达到规范目标

## Dependencies
- T1、T3、T4、T5

## Branch
`feat/t8-perf-hardening`

## Repo Changes
```
infra/perf/otlp_load_gen.sh
hub/src/perf/LoadSimulator.java
docs/Perf.md
```

## Test Plan
- Perf：模拟 N=1000 主机；监控 Hub 指标/时延  
- Regression：确保写/读时延未退化

## Rollback
回退调优参数，保留生成器脚本
