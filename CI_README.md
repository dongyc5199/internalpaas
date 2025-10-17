# CI/CD系统快速开始

## 🚀 快速开始

### 本地开发工作流

#### 1. 提交代码前检查（推荐）
```bash
# 一键运行所有检查（与CI相同）
npm run ci:check
```

#### 2. 或者分步执行
```bash
# 格式化代码
npm run format

# 修复lint问题
npm run lint:fix
npm run lint:style:fix

# 类型检查
npm run type-check

# 运行测试
npm run test:run

# 构建验证
npm run build
```

#### 3. 提交代码
```bash
git add .
git commit -m "feat: 添加新功能"
git push
```

---

## 📋 可用命令

### 开发命令
```bash
npm run dev              # 启动开发服务器
npm run build            # 生产构建
npm run preview          # 预览构建产物
```

### 代码质量
```bash
npm run lint             # ESLint检查
npm run lint:fix         # 自动修复ESLint问题
npm run lint:style       # Stylelint检查
npm run lint:style:fix   # 自动修复Stylelint问题
npm run format           # 格式化代码
npm run format:check     # 检查代码格式
npm run type-check       # TypeScript类型检查
```

### 测试
```bash
npm run test             # 启动测试（watch模式）
npm run test:run         # 运行测试（CI模式，带覆盖率）
npm run test:ui          # 测试UI界面
npm run test:watch       # 监视模式运行测试
```

### CI相关
```bash
npm run ci:check         # 运行完整CI检查（本地）
npm run precommit        # 提交前检查（格式化+修复+测试）
```

---

## 🔄 CI工作流程

### 自动触发时机

1. **Push到master/dev分支** → 运行完整CI
2. **创建/更新Pull Request** → 运行PR检查
3. **修改前端代码** → 额外运行Bundle大小检查
4. **每周一凌晨** → 运行安全和依赖检查

### CI检查项

| 检查项 | 说明 | 失败策略 |
|--------|------|---------|
| ✅ ESLint | 代码规范检查 | 阻断 |
| ✅ Stylelint | 样式规范检查 | 阻断 |
| ✅ TypeScript | 类型检查 | 阻断 |
| ✅ 单元测试 | 所有测试必须通过 | 阻断 |
| ✅ 测试覆盖率 | 当前最低40% | 阻断 |
| ✅ 前端构建 | Vite构建成功 | 阻断 |
| ✅ 后端构建 | Maven构建成功 | 阻断 |
| ✅ Bundle大小 | main.js < 300KB | 阻断 |
| ⚠️ 安全审计 | npm audit | 警告 |

---

## 🐛 常见问题

### Q: ESLint失败怎么办？
```bash
# 查看具体错误
npm run lint

# 自动修复
npm run lint:fix

# 如果还有错误，手动修复
```

### Q: 测试失败怎么办？
```bash
# 查看失败的测试
npm run test:run

# 运行特定测试文件
npm run test tests/format.test.ts

# 使用UI界面调试
npm run test:ui
```

### Q: 覆盖率不足怎么办？
1. 查看coverage报告: `coverage/index.html`
2. 为未覆盖的代码添加测试
3. 重新运行: `npm run test:run`

### Q: Bundle大小超限怎么办？
1. 分析是什么导致增加
2. 考虑代码分割或懒加载
3. 移除不必要的依赖
4. 申请阈值豁免（需要正当理由）

---

## 📊 查看CI结果

### GitHub上查看
1. 进入仓库的 "Actions" 标签
2. 选择对应的workflow run
3. 查看各个job的详细日志

### 本地模拟CI
```bash
# 运行与CI完全相同的检查
npm run ci:check
```

---

## 🎯 质量标准

详见: [质量标准文档](./docs/archives/frontend-upgrade/quality-standards.md)

---

## 📚 更多文档

- [CI/CD配置指南](./docs/archives/frontend-upgrade/ci-cd-guide.md)
- [前端开发指南](./docs/architecture/frontend-architecture.md)
- [项目概述](./CLAUDE.md)

---

**遇到问题？** 提交Issue或联系技术团队
