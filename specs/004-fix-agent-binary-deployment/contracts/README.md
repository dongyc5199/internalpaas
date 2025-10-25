# API Contracts: Agent Binary Deployment Enhancement

**Feature**: 004-fix-agent-binary-deployment
**Date**: 2025-10-25

---

## 概述

本功能主要涉及**内部服务层**的修改，不新增对外REST API。合约定义的是Java服务接口的行为规范，用于指导测试和实现。

---

## 合约列表

1. **AgentBinaryResolver Contract** - 二进制文件获取接口
2. **BinaryValidator Contract** - 文件验证接口
3. **DeploymentLogger Contract** - 部署日志接口

---

## 相关文件

- [AgentBinaryResolver.contract.md](./AgentBinaryResolver.contract.md) - 核心二进制获取逻辑
- [BinaryValidator.contract.md](./BinaryValidator.contract.md) - 文件验证规范
- [DeploymentLogger.contract.md](./DeploymentLogger.contract.md) - 日志格式规范

---

## 设计原则

1. **单一职责**: 每个合约专注一个明确的功能
2. **可测试性**: 所有合约都可独立进行单元测试
3. **错误透明**: 异常和错误码清晰定义
4. **向后兼容**: 不破坏现有AgentDeployService的公共接口
