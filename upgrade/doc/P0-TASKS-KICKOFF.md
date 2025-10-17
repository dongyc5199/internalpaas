# P0 任务启动计划

## 📅 任务信息
- **启动日期**: 2025年10月17日
- **预计完成**: 2025年10月31日 (2周)
- **优先级**: P0 (最高优先级)
- **总工时**: 6天

---

## 🎯 任务目标

完成前端升级项目的核心功能迁移,包括:
1. ✅ 仪表盘主功能迁移到 TypeScript
2. ✅ 监控历史功能迁移到 TypeScript  
3. ✅ 提升测试覆盖率至 60%+

---

## 📋 任务清单

### 任务1: 仪表盘主功能迁移 (3天)

#### Day 1: 迁移图表组件 🔄 进行中
**状态**: 🟢 IN PROGRESS
**负责人**: [待分配]
**截止日期**: 2025年10月18日

**子任务**:
- [ ] 迁移服务器状态图表
  - 位置: `src/main/resources/templates/admin/dashboard.html` 
  - 目标: `src/main/frontend/modules/dashboard.ts`
  - 使用 Chart.js 重构
  
- [ ] 迁移资源使用率图表
  - CPU、内存、磁盘使用率图表
  - 使用 `utils/chart.ts` 封装
  
- [ ] 迁移实时监控图表
  - 实时数据流图表
  - WebSocket 数据接收

**技术要点**:
```typescript
// 使用 utils/chart.ts 的工具函数
import { createTimeSeriesChartConfig, getChartColor } from '@/utils/chart';
import Chart from 'chart.js/auto';

// 创建图表实例
const config = createTimeSeriesChartConfig(labels, datasets);
const chart = new Chart(ctx, config);
```

**验收标准**:
- [ ] 所有图表组件迁移完成
- [ ] 图表正常渲染,无报错
- [ ] TypeScript 类型检查通过

---

#### Day 2: 迁移数据更新逻辑
**状态**: ⚪ NOT STARTED
**负责人**: [待分配]
**截止日期**: 2025年10月21日

**子任务**:
- [ ] 重构 WebSocket 连接逻辑
  - 创建 WebSocket 管理类
  - 实现自动重连机制
  - 添加心跳检测
  
- [ ] 实现实时数据更新
  - 数据接收和解析
  - 图表数据更新
  - 状态管理
  
- [ ] 添加错误处理
  - 连接失败处理
  - 数据格式错误处理
  - 超时处理

**技术要点**:
```typescript
// WebSocket 管理类示例
class DashboardWebSocket {
    private ws: WebSocket | null = null;
    private reconnectTimer: number | null = null;
    
    connect(url: string): void {
        this.ws = new WebSocket(url);
        this.ws.onmessage = this.handleMessage.bind(this);
        this.ws.onerror = this.handleError.bind(this);
        this.ws.onclose = this.handleClose.bind(this);
    }
    
    private handleMessage(event: MessageEvent): void {
        const data = JSON.parse(event.data);
        // 更新图表数据
    }
    
    private handleError(error: Event): void {
        console.error('WebSocket error:', error);
        this.reconnect();
    }
}
```

**验收标准**:
- [ ] WebSocket 连接稳定
- [ ] 实时数据正常更新
- [ ] 错误处理完善

---

#### Day 3: 测试与优化
**状态**: ⚪ NOT STARTED
**负责人**: [待分配]
**截止日期**: 2025年10月22日

**子任务**:
- [ ] 编写单元测试
  - 图表组件测试
  - WebSocket 逻辑测试
  - 数据更新测试
  
- [ ] 功能验证测试
  - 手动功能测试
  - 浏览器兼容性测试
  - 性能测试
  
- [ ] 性能优化
  - 减少不必要的重渲染
  - 优化数据更新频率
  - 内存泄漏检查

**测试文件**: `src/main/frontend/tests/dashboard.test.ts`

**验收标准**:
- [ ] 测试覆盖率 ≥60%
- [ ] 所有测试通过
- [ ] 性能满足要求 (无卡顿)

---

### 任务2: 监控历史迁移 (2天)

#### Day 1: 迁移核心功能
**状态**: ⚪ NOT STARTED
**负责人**: [待分配]
**截止日期**: 2025年10月23日

**子任务**:
- [ ] 时间序列图表组件
  - 创建历史数据图表组件
  - 支持多种时间粒度 (小时/天/周/月)
  - 数据点交互 (tooltip, hover)
  
- [ ] 数据查询接口封装
  - HTTP 请求封装
  - 查询参数处理
  - 响应数据转换
  
- [ ] 时间范围选择器
  - 日期选择组件
  - 快捷时间范围 (今天/本周/本月)
  - 自定义时间范围

**技术要点**:
```typescript
// 历史数据查询接口
interface MonitoringHistoryQuery {
    serverId: string;
    startTime: Date;
    endTime: Date;
    interval: 'hour' | 'day' | 'week' | 'month';
}

async function fetchMonitoringHistory(query: MonitoringHistoryQuery) {
    const response = await http.get('/api/monitoring/history', {
        params: query
    });
    return response.data;
}
```

**验收标准**:
- [ ] 历史数据正常显示
- [ ] 时间范围选择正常
- [ ] 图表交互流畅

---

#### Day 2: 测试与集成
**状态**: ⚪ NOT STARTED
**负责人**: [待分配]
**截止日期**: 2025年10月24日

**子任务**:
- [ ] 单元测试
  - 数据查询逻辑测试
  - 时间范围计算测试
  - 图表渲染测试
  
- [ ] 集成测试
  - 端到端功能测试
  - 与后端 API 联调
  
- [ ] 性能优化
  - 大数据量渲染优化
  - 数据缓存策略
  - 懒加载实施

**测试文件**: `src/main/frontend/tests/monitoring-history.test.ts`

**验收标准**:
- [ ] 测试覆盖率 ≥60%
- [ ] 所有测试通过
- [ ] 大数据量场景性能正常

---

### 任务3: 工具模块测试补充 (1天)

**状态**: ⚪ NOT STARTED
**负责人**: [待分配]
**截止日期**: 2025年10月25日

**子任务**:
- [ ] 编写 `utils/http.ts` 测试
  - GET/POST/PUT/DELETE 请求测试
  - 错误处理测试
  - 超时处理测试
  - 拦截器测试
  
- [ ] 编写 `utils/event-bus.ts` 测试
  - 事件订阅/发布测试
  - 取消订阅测试
  - 多订阅者测试
  
- [ ] 编写 `utils/format.ts` 测试
  - 字节格式化测试
  - 时间格式化测试
  - 百分比格式化测试
  - 边界值测试
  
- [ ] 编写 `utils/chart.ts` 测试
  - 颜色生成测试
  - 配置生成测试
  - 图表工具函数测试

**技术要点**:
```typescript
// 测试示例
import { describe, it, expect, vi } from 'vitest';
import { http } from '@/utils/http';

describe('HTTP Client', () => {
    it('should make GET request successfully', async () => {
        const mockData = { id: 1, name: 'test' };
        global.fetch = vi.fn().mockResolvedValue({
            ok: true,
            json: async () => mockData
        });
        
        const result = await http.get('/api/test');
        expect(result.data).toEqual(mockData);
    });
});
```

**验收标准**:
- [ ] 4个工具模块都有完整测试
- [ ] 整体测试覆盖率提升至 60%+
- [ ] CI 构建通过

---

## 📊 进度跟踪

### 每日站会
- **时间**: 每天上午 9:30
- **时长**: 15分钟
- **内容**:
  - 昨天完成了什么
  - 今天计划做什么
  - 遇到什么阻碍

### 周进度报告
- **时间**: 每周五下午
- **内容**:
  - 本周完成情况
  - 下周计划
  - 风险与问题

---

## 🚨 风险与阻碍

### 已识别风险

1. **技术风险**:
   - 🟡 仪表盘逻辑复杂,可能需要更多时间
   - **缓解**: 提前技术预研,必要时调整时间

2. **依赖风险**:
   - 🟢 需要后端 API 配合测试
   - **缓解**: 提前与后端对齐接口

3. **质量风险**:
   - 🟡 测试覆盖率可能达不到目标
   - **缓解**: 强制要求新代码必须有测试

---

## ✅ 验收标准

### 功能完整性
- [ ] 仪表盘所有功能正常
- [ ] 监控历史所有功能正常
- [ ] 无明显 Bug

### 代码质量
- [ ] TypeScript 严格模式,无 `any`
- [ ] ESLint 检查通过
- [ ] Prettier 格式化通过

### 测试覆盖
- [ ] 整体覆盖率 ≥60%
- [ ] 核心功能覆盖率 ≥80%
- [ ] CI 构建通过

### 性能指标
- [ ] 首屏加载 <2s
- [ ] 图表渲染流畅,无卡顿
- [ ] 无内存泄漏

---

## 📝 开发检查清单

### 开发前检查
- [ ] 确认任务目标和验收标准
- [ ] 检查相关文档和代码
- [ ] 准备开发环境

### 开发中检查
- [ ] 遵循 TypeScript 规范
- [ ] 及时编写单元测试
- [ ] 定期提交代码

### 开发后检查
- [ ] 运行所有测试
- [ ] 代码格式化和 Lint
- [ ] 功能验证测试
- [ ] Code Review

---

## 🔧 工具与资源

### 开发环境
```bash
# 启动前端开发服务器
npm run dev

# 启动后端服务器
./mvnw.cmd spring-boot:run
```

### 测试命令
```bash
# 运行单元测试
npm test

# 运行测试 (带覆盖率)
npm run test:run

# 运行特定测试文件
npm test -- dashboard.test.ts
```

### 代码质量
```bash
# 格式化代码
npm run format

# 修复 Lint 问题
npm run lint:fix

# 类型检查
npm run type-check

# 完整检查
npm run ci:check
```

### 构建命令
```bash
# 开发构建
npm run build

# Maven 构建 (含前端)
./mvnw.cmd verify -Dbuild.frontend=true
```

---

## 📚 参考文档

### 内部文档
- [现状与工作计划](./current-status-and-plan.md)
- [前端开发指南](../../src/main/frontend/README.md)
- [前端架构指南](../../docs/architecture/frontend-architecture.md)
- [CI/CD 指南](../../CI_README.md)

### 技术文档
- [Vite 官方文档](https://vitejs.dev/)
- [TypeScript 官方文档](https://www.typescriptlang.org/)
- [Chart.js 官方文档](https://www.chartjs.org/)
- [Vitest 官方文档](https://vitest.dev/)

---

## 🎯 下一步行动

### 立即执行
1. **分配任务负责人**
   - [ ] 任务1负责人: [待分配]
   - [ ] 任务2负责人: [待分配]
   - [ ] 任务3负责人: [待分配]

2. **设置每日站会**
   - [ ] 确定会议时间
   - [ ] 创建会议邀请
   - [ ] 准备站会模板

3. **启动第一个任务**
   - [ ] 分析现有仪表盘代码
   - [ ] 设计 TypeScript 架构
   - [ ] 开始迁移图表组件

---

## 📞 联系方式

- **项目负责人**: [待填写]
- **技术负责人**: [待填写]
- **Slack 频道**: [待创建]
- **问题反馈**: [待确定]

---

**文档创建时间**: 2025年10月17日  
**最后更新**: 2025年10月17日  
**下次更新**: 每日更新进度

---

## 🎉 开始吧!

P0 任务已启动! 让我们专注目标,高质量完成核心功能迁移! 💪
