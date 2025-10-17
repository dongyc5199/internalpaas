# CI/CD配置指南

**文档版本**: v1.0
**创建日期**: 2025-10-12
**更新日期**: 2025-10-12

---

## 📋 概述

本项目使用GitHub Actions作为CI/CD平台，自动化执行代码质量检查、测试、构建和部署流程。

---

## 🗂️ Workflow文件清单

### 1. ci.yml - 主CI流程
**触发条件**:
- Push到master或dev分支
- Pull Request到master或dev分支

**执行任务**:
1. **frontend-quality**: 前端代码质量检查（ESLint + Stylelint + Prettier）
2. **frontend-test**: 前端测试和覆盖率检查
3. **frontend-build**: 前端构建和Bundle大小检查
4. **backend-build**: 后端Maven构建
5. **quality-summary**: 质量汇总报告

**运行时间**: 约5-8分钟

---

### 2. pr-check.yml - Pull Request检查
**触发条件**:
- Pull Request打开/更新

**执行任务**:
- 仅检查变更的文件（增量检查）
- 运行测试
- 在PR中添加质量报告评论

**运行时间**: 约3-5分钟

---

### 3. bundle-size.yml - Bundle大小监控
**触发条件**:
- Pull Request修改前端代码

**执行任务**:
- 比较PR前后的Bundle大小
- 计算大小变化（绝对值和百分比）
- 在PR中添加Bundle大小报告
- 超过阈值时失败CI

**阈值**:
- ⚠️ 警告: +10%
- ❌ 失败: +20%

**运行时间**: 约4-6分钟

---

### 4. security.yml - 安全和依赖检查
**触发条件**:
- 每周一凌晨2点（定时）
- 手动触发

**执行任务**:
- npm audit安全审计
- 依赖版本检查
- 自动创建依赖更新Issue

**运行时间**: 约2-3分钟

---

## 🚀 使用指南

### 本地开发流程

#### 1. 提交前检查
```bash
# 代码格式化
npm run format

# 代码检查
npm run lint
npm run lint:style

# 运行测试
npm run test:run

# 构建验证
npm run build
```

#### 2. 提交代码
```bash
git add .
git commit -m "feat: 添加新功能"
git push origin feature-branch
```

#### 3. 创建Pull Request
- CI会自动运行所有检查
- 查看CI结果和报告
- 修复所有失败的检查
- 等待Code Review

---

### CI失败处理

#### ESLint失败
```bash
# 本地运行检查
npm run lint

# 自动修复（部分问题）
npm run lint -- --fix

# 查看具体错误
npx eslint src/main/frontend/**/*.ts --format=stylish
```

#### 测试失败
```bash
# 运行特定测试文件
npm run test tests/format.test.ts

# 运行所有测试
npm run test:run

# 查看覆盖率
npm run test:run
# 报告在: coverage/index.html
```

#### 构建失败
```bash
# 本地构建
npm run build

# 查看详细错误
npm run build -- --mode=development

# 清理后重新构建
rm -rf node_modules package-lock.json
npm install
npm run build
```

#### Bundle大小超限
1. 分析Bundle组成:
```bash
npm run build -- --mode=production

# 使用rollup-plugin-visualizer分析（需要配置）
```

2. 优化策略:
   - 移除未使用的依赖
   - 使用按需引入
   - 启用Tree Shaking
   - 代码分割（Code Splitting）

---

## 🔧 配置说明

### 修改CI行为

#### 调整测试覆盖率阈值
编辑 `.github/workflows/ci.yml`:
```yaml
- name: Check coverage threshold
  run: |
    MIN_COVERAGE=40  # 修改这里
    # ...
```

#### 调整Bundle大小限制
编辑 `.github/workflows/ci.yml`:
```yaml
- name: Check bundle size
  run: |
    MAX_SIZE_KB=300  # 修改这里
    # ...
```

#### 修改触发条件
```yaml
on:
  push:
    branches: [ master, dev, staging ]  # 添加分支
    paths:
      - 'src/main/frontend/**'  # 仅监控特定路径
```

---

## 📊 质量门禁

### 必须通过的检查

| 检查项 | 失败处理 | 绕过方式 |
|--------|---------|---------|
| ESLint | 阻断合并 | 无（必须修复） |
| Stylelint | 阻断合并 | 无（必须修复） |
| 单元测试 | 阻断合并 | 无（必须修复） |
| 覆盖率阈值 | 阻断合并 | 申请豁免 |
| TypeScript编译 | 阻断合并 | 无（必须修复） |
| Bundle大小 | 阻断合并 | 申请豁免 |

### 警告级检查

| 检查项 | 说明 | 建议 |
|--------|------|------|
| npm audit (moderate) | 中等漏洞 | 创建Issue跟踪 |
| Prettier格式 | 格式不一致 | 运行format修复 |
| 依赖过期 | 有新版本 | 定期更新 |

---

## 🎯 最佳实践

### 1. 提交前本地验证
```bash
# 一键检查脚本
npm run lint && npm run lint:style && npm run test:run && npm run build
```

建议配置为Git pre-commit hook。

### 2. 增量提交
- 小步提交，频繁集成
- 每个commit应该是可构建的
- commit message遵循规范

### 3. PR规范
- PR标题清晰描述变更
- 填写完整的PR描述模板
- 自检所有检查项
- 及时响应review意见

### 4. CI优化
- 利用缓存加速构建
- 并行执行独立任务
- 仅在必要时运行全量检查

---

## 🐛 常见问题

### Q1: CI运行很慢
**A**:
- 检查是否有缓存
- 减少不必要的依赖安装
- 使用增量检查（pr-check.yml）

### Q2: 本地通过但CI失败
**A**:
- Node版本不一致（CI使用Node 20）
- npm缓存问题（使用`npm ci`而非`npm install`）
- 环境变量差异

### Q3: 如何跳过CI
**A**:
- 不建议跳过CI
- 紧急情况可在commit message中加`[skip ci]`
- 需要团队leader审批

### Q4: Coverage突然下降
**A**:
- 新增代码未添加测试
- 删除了部分测试
- 重构导致原有测试失效

---

## 📚 相关资源

### 文档
- [质量标准文档](./quality-standards.md)
- [前端开发指南](./frontend-development.md)
- [测试编写指南](./testing-guide.md)

### 工具
- [GitHub Actions文档](https://docs.github.com/en/actions)
- [Vitest文档](https://vitest.dev/)
- [ESLint规则](https://eslint.org/docs/rules/)

---

## 🔄 CI流程图

```
┌─────────────────────────────────────────────────────────────┐
│                         Push / PR                            │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
        ┌─────────────────────────────────────────────────┐
        │          Trigger GitHub Actions                  │
        └─────────────────────────────────────────────────┘
                              │
        ┌─────────────────────┴────────────────────────┐
        │                                               │
        ▼                                               ▼
┌───────────────┐                                ┌───────────────┐
│  Code Quality │                                │     Tests     │
│  (ESLint)     │                                │  (Vitest)     │
└───────────────┘                                └───────────────┘
        │                                               │
        ├─ Pass ──────────────┬───────────── Pass ─────┤
        │                     │                         │
        ▼                     ▼                         ▼
┌───────────────┐      ┌───────────────┐      ┌───────────────┐
│ Frontend Build│      │ Backend Build │      │ Bundle Check  │
└───────────────┘      └───────────────┘      └───────────────┘
        │                     │                         │
        └──────────── All Pass ────────────────────────┘
                              │
                              ▼
                   ┌─────────────────────┐
                   │   ✅ CI Success     │
                   │  Ready to Merge     │
                   └─────────────────────┘
```

---

**维护者**: 技术团队
**联系方式**: GitHub Issues
**最后更新**: 2025-10-12
