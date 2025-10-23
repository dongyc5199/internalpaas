# 开发规范文档

本目录包含项目的开发规范和最佳实践。

---

## 📋 规范文档

### [TypeScript 编码规范](./TYPESCRIPT_CODING_STANDARDS.md) ⭐ **必读**

**状态**: 强制执行
**生效日期**: 2025-10-23

**核心要求**:
- ❌ **严格禁止** 使用 `any` 类型 (生产代码)
- ✅ **必须** 为导出函数添加返回类型
- ✅ **必须** 使用推荐的类型安全模式

**包含内容**:
- 禁止事项和处罚措施
- 6个推荐的类型安全模式
- Window全局属性类型化
- 第三方库类型集成
- 动态属性访问类型化
- 联合类型 + 类型守卫
- 泛型使用
- Unknown类型的正确用法
- 代码审查检查清单
- CI/CD集成说明
- 常见问题解答

**何时查看**:
- ✅ 开始开发前 (必须)
- ✅ 遇到类型错误时
- ✅ 代码审查时
- ✅ 不确定如何定义类型时

---

## 🔍 快速参考

### 遇到 ESLint `@typescript-eslint/no-explicit-any` 错误?

➡️ 查看 [TypeScript编码规范 - 推荐模式](./TYPESCRIPT_CODING_STANDARDS.md#-推荐模式)

### 需要访问 Window 全局属性?

➡️ 查看 [模式1: Window全局属性类型化](./TYPESCRIPT_CODING_STANDARDS.md#1-window全局属性类型化)

### 使用第三方库但没有类型?

➡️ 查看 [模式2: 第三方库类型集成](./TYPESCRIPT_CODING_STANDARDS.md#2-第三方库类型集成)

### 需要动态访问对象属性?

➡️ 查看 [模式3: 动态属性访问类型化](./TYPESCRIPT_CODING_STANDARDS.md#3-动态属性访问类型化)

---

## 📚 相关文档

- [Phase 4完成报告](../../specs/003-server-group-cleanup/PHASE4_COMPLETION_REPORT.md) - 详细的类型安全案例分析
- [前端架构指南](../architecture/frontend-architecture.md) - 整体前端架构说明
- [项目概述](../../CLAUDE.md) - 项目总体介绍

---

## 🚀 CI/CD

所有代码提交都会自动进行以下检查:

1. **ESLint** - 检查代码风格和类型安全
2. **TypeScript Compiler** - 类型检查
3. **Prettier** - 代码格式化
4. **Tests** - 单元测试和集成测试

**本地运行检查**:
```bash
# 运行ESLint
npm run lint

# 运行类型检查
npm run type-check

# 运行格式化
npm run format

# 运行测试
npm test
```

---

## 📊 当前代码质量指标

**TypeScript类型安全** (截至 2025-10-23):
- ✅ 活跃生产代码: 0个 `any` 类型
- ⚠️ 废弃文件: 3个 `any` 类型
- ⏳ 测试文件: 136个 `any` 类型 (Post-MVP处理)

**ESLint错误**:
- 生产代码: 149个错误 (主要在测试文件)
- 改进: 从170减少到149 (-12%)

**构建状态**: ✅ 通过
**测试通过率**: 93.1% (216/232)

---

## 💡 贡献规范

如需添加或修改开发规范:

1. 创建Issue说明变更建议
2. 提交PR包含规范文档更新
3. 至少3个团队成员review
4. 技术负责人批准
5. 更新本README

---

**维护**: 技术团队
**最后更新**: 2025-10-23
