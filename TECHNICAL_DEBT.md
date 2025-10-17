# 技术债务清单 (Technical Debt)

> 本文档记录项目中需要改进但不阻塞当前开发的技术债务项。所有债务按优先级分类,并定期审查。

## 📋 债务分类

- **P0 - 关键**: 严重影响系统稳定性或安全性,需尽快解决
- **P1 - 重要**: 影响开发效率或代码质量,应在下个迭代解决
- **P2 - 一般**: 优化改进,可在适当时机解决
- **P3 - 低优**: 理想状态,资源允许时解决

---

## 🔴 P0 - 关键债务

*当前无关键债务*

---

## 🟠 P1 - 重要债务

### 1. WebSocket 测试边缘场景失败

**描述**: `websocket.test.ts` 中 7 个测试用例失败,主要涉及:
- 连接超时重连机制
- 最大重连次数限制验证
- 心跳响应超时检测
- 无效 JSON 消息处理
- WebSocket 创建失败时的错误触发

**影响**: 
- WebSocket 核心功能正常工作
- 但边缘场景测试覆盖不完整,可能在异常情况下行为不符合预期

**原因**: 
- 测试环境模拟的 WebSocket 行为与实际浏览器实现存在差异
- 异步定时器处理在测试中的时序问题

**建议解决方案**:
1. 使用更精确的 WebSocket Mock 库 (如 `mock-socket`)
2. 调整测试中的异步时间控制逻辑
3. 考虑使用 Playwright 进行 E2E 级别的 WebSocket 测试

**涉及文件**:
- `src/main/frontend/tests/websocket.test.ts` (lines 162-637)

**创建日期**: 2025-10-17  
**预计工作量**: 4-6 小时  
**责任人**: 待分配

---

### 2. Dashboard 自动刷新测试失败

**描述**: `dashboard.test.ts` 中 "自动刷新统计数据" 测试失败

**影响**: 
- Dashboard 自动刷新功能在实际使用中正常
- 但测试无法验证定时器触发的 API 调用

**原因**: 
- 测试中的 `vi.useFakeTimers()` 与实际 Dashboard 的定时器交互存在问题
- Mock 的 `fetch` 可能在定时器触发前被检查

**建议解决方案**:
1. 改进测试的异步等待逻辑
2. 确保在检查 `fetch` 调用前,定时器已充分推进
3. 考虑使用 `waitFor` 工具等待异步操作

**涉及文件**:
- `src/main/frontend/tests/dashboard.test.ts` (lines 43-61)

**创建日期**: 2025-10-17  
**预计工作量**: 1-2 小时  
**责任人**: 待分配

---

## 🟡 P2 - 一般债务

### 3. 测试覆盖率报告缺失

**描述**: 运行 `npm run test:run --coverage` 未生成覆盖率报告文件

**影响**: 
- 无法量化测试覆盖率
- 难以识别未测试的代码路径

**原因**: 
- 可能是 Vitest 配置中缺少覆盖率报告配置
- Coverage 工具未正确安装或配置

**建议解决方案**:
1. 检查 `vitest.config.ts` 中的 coverage 配置
2. 确保安装了 `@vitest/coverage-v8` 或 `@vitest/coverage-istanbul`
3. 添加 HTML 覆盖率报告生成配置

**涉及文件**:
- `vitest.config.ts`
- `package.json`

**创建日期**: 2025-10-17  
**预计工作量**: 0.5-1 小时  
**责任人**: 待分配

---

### 4. 前端代码 ESLint 警告

**描述**: 测试文件中存在 ESLint 格式警告:
- `Delete ␍` (CRLF vs LF 行尾符问题)
- `Unexpected any` (使用了 `any` 类型)

**影响**: 
- 不影响功能,但降低代码质量
- 混合的行尾符可能导致 Git diff 混乱

**原因**: 
- Windows 系统默认使用 CRLF,而 ESLint 配置要求 LF
- 测试代码中为了访问私有属性使用了 `any` 类型

**建议解决方案**:
1. 配置 `.gitattributes` 统一行尾符
2. 使用 Prettier 自动格式化
3. 对于测试中的类型断言,使用更精确的类型工具

**涉及文件**:
- `src/main/frontend/tests/websocket.test.ts`
- `src/main/frontend/tests/ServerListManager.test.ts`

**创建日期**: 2025-10-17  
**预计工作量**: 1 小时  
**责任人**: 待分配

---

## 🟢 P3 - 低优先级债务

### 5. Chart.js 和 JSDOM 集成优化

**描述**: Dashboard 图表测试需要复杂的 JSDOM 环境设置和 Canvas Mock

**影响**: 
- 测试设置较复杂
- 可能限制图表交互的测试能力

**原因**: 
- Chart.js 依赖浏览器 Canvas API
- JSDOM 不完全支持 Canvas 渲染

**建议解决方案**:
1. 评估是否需要 E2E 测试来验证图表渲染
2. 考虑使用 Playwright 的视觉回归测试
3. 或使用 `jest-canvas-mock` 等更完善的 Mock 库

**涉及文件**:
- `src/main/frontend/tests/dashboard.test.ts`
- `src/main/frontend/modules/dashboard.ts`

**创建日期**: 2025-10-17  
**预计工作量**: 4-8 小时  
**责任人**: 待分配

---

### 6. WebSocket 测试的 Mock 实现可以提取为共享工具

**描述**: `websocket.test.ts` 中的 `MockWebSocket` 类可以提取为测试工具

**影响**: 
- 如果其他模块需要 WebSocket Mock,会导致代码重复

**原因**: 
- 当前 Mock 实现直接写在测试文件中

**建议解决方案**:
1. 创建 `src/main/frontend/tests/mocks/WebSocketMock.ts`
2. 将 MockWebSocket 移动到共享模块
3. 考虑发布为独立的测试工具包

**涉及文件**:
- `src/main/frontend/tests/websocket.test.ts` (lines 7-50)

**创建日期**: 2025-10-17  
**预计工作量**: 1-2 小时  
**责任人**: 待分配

---

## 📊 债务统计

| 优先级 | 数量 | 预计总工作量 |
|--------|------|--------------|
| P0     | 0    | 0 小时       |
| P1     | 2    | 5-8 小时     |
| P2     | 2    | 1.5-2 小时   |
| P3     | 2    | 5-10 小时    |
| **总计** | **6** | **11.5-20 小时** |

---

## 🔄 债务管理流程

### 新增债务
1. 使用本文档格式添加新条目
2. 分配适当的优先级
3. 标记创建日期和预计工作量
4. 在每周技术会议中审查

### 解决债务
1. 在相关迭代中安排解决任务
2. 完成后更新状态或移除条目
3. 记录解决日期和实际工作量
4. 总结经验教训

### 定期审查
- **每周**: 审查 P0/P1 债务,确保及时解决
- **每月**: 审查所有债务,调整优先级
- **每季度**: 评估债务趋势,制定改进计划

---

## 📝 历史记录

### 已解决的债务

#### 2025-10-17: TypeScript 类型错误 (P0)
- **问题**: 测试文件中存在 16 个 TypeScript 类型错误
- **解决方案**: 
  - 修复接口属性不匹配
  - 添加缺失的导入
  - 重命名冲突的导出
- **工作量**: 2 小时
- **结果**: 所有类型错误已修复,构建通过

---

## 📚 相关文档

- [开发指南](./docs/development/frontend-dev-summary-guide.md)
- [测试指南](./docs/guides/e2e-quickstart.md)
- [架构文档](./docs/architecture/overview.md)
- [项目路线图](./docs/development/roadmap.md)

---

**最后更新**: 2025-10-17  
**文档维护者**: 开发团队
