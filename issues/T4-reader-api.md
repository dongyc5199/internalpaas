---
title: "[Feature] T4 — Read API (/monitoring/*) & Source Selection"
labels: ["feature","backend","api"]
assignees: []
date: 2025-10-13
---

## Summary
暴露与现有前端兼容的读接口：近实时读 Redis，历史读 TSDB；提供 OpenAPI。

## Goals
- `GET /monitoring/server/{id}/metrics?from&to&step&fields`
- `GET /monitoring/servers/metrics?fields=cpu,mem,net`
- OpenAPI 文档；自动选择数据源

## Deliverables
- `MonitoringController` 源选择逻辑
- `docs/openapi/openapi.yaml`

## Acceptance Criteria
- 合同测试通过（响应结构/字段）
- P95：近实时 ≤ 300ms；大盘 ≤ 1.5s

## Dependencies
- T3

## Branch
`feat/t4-reader-api`

## Repo Changes
```
hub/src/main/java/com/example/hub/api/MonitoringController.java
docs/openapi/openapi.yaml
```

## Test Plan
- Integration：种数据 → 调用 API 校验  
- E2E：对接最小 UI 或 mock 图表渲染

## Rollback
仅代码回滚
