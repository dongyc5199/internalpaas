---
title: "[Feature] T6 — Auto-deploy Agent on Server Add (SSH)"
labels: ["feature","backend","agent","ops"]
assignees: []
date: 2025-10-13
---

## Summary
管理员添加服务器后自动部署 **otelcol**：预检 → 上传 → 引导脚本 → systemd 启动 → WebSocket 推进度。

## Goals
- 监听 `ServerCreatedEvent(serverId)`
- 预检：SSH 可达/权限/磁盘
- 上传：`agent.tgz`、渲染 `otelcol.yaml.tmpl`、`bootstrap.sh`
- 执行：`SERVER_ID=<id> OTLP_ENDPOINT=<url> sh bootstrap.sh otelcol`
- `/topic/server-status` 推送进度

## Deliverables
- `AgentDeployer`、`ServerCreatedListener`
- `agent/bootstrap/bootstrap.sh`、`agent/otelcol/otelcol.yaml.tmpl`
- 重试/回滚策略

## Acceptance Criteria
- **add_server_auto_deploy_success_path** 通过
- 失败路径按 1m/5m/15m 重试并可回滚

## Dependencies
- SSH/远程命令子系统

## Branch
`feat/t6-auto-deploy-agent`

## Repo Changes
```
agent/bootstrap/bootstrap.sh
agent/otelcol/otelcol.yaml.tmpl
hub/src/main/java/com/example/hub/agent/AgentDeployer.java
hub/src/main/java/com/example/hub/agent/ServerCreatedListener.java
```

## Test Plan
- Integration：mock SSH，断言上传/执行序列  
- E2E：连测试 VM，确认服务运行 & Hub 收到指标

## Rollback
`systemctl disable --now otelcol && rm -rf /opt/metrics-agent`
