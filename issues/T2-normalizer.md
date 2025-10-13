---
title: "[Feature] T2 — OTLP Decoder & Normalizer (units/labels/schema)"
labels: ["feature","backend","ingestion"]
assignees: []
date: 2025-10-13
---

## Summary
解析 OTLP Export payload 映射为内部模型 `MetricSample`；统一单位（bytes/seconds）、白名单标签（server.id/region/group/env）。

## Goals
- OTLP → `List<MetricSample>` 解码
- 单位归一与默认值处理；标签白名单
- 未识别指标忽略并 debug 记录

## Deliverables
- `OtlpDecoder`、`Normalizer`
- 单测：`normalizer_unit_conversion`、`label_whitelist_filtering`
- 从 `IngestQueue` 拉取 → 解码 → 归一（不落盘）

## Acceptance Criteria
- 常见主机指标（cpu/mem/disk/filesystem/network/load）成功映射
- 单测绿色

## Dependencies
- T1

## Branch
`feat/t2-normalizer`

## Repo Changes
```
hub/src/main/java/com/example/hub/model/MetricSample.java
hub/src/main/java/com/example/hub/ingest/OtlpDecoder.java
hub/src/main/java/com/example/hub/ingest/Normalizer.java
hub/src/test/java/com/example/hub/ingest/NormalizerTests.java
```

## Test Plan
- Unit：单位换算、标签过滤、空值处理  
- Integration：真实 OTLP fixture → 列表长度与字段断言

## Rollback
仅代码回滚
