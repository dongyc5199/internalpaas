# 🎨 可展示页面清单与访问指南

## 📋 概述

本文档列出了项目中所有可以用来展示重构成果的页面，包括新创建的演示页面和现有的管理页面。

---

## 🚀 快速启动

### 1. 构建前端资源
```bash
cd E:\work\code\internalpaas
npm run build
```
**构建产物**:
- `src/main/resources/static/dist/assets/main.js` (278.56 KB)
- `src/main/resources/static/dist/assets/main.css` (4.59 KB)

### 2. 启动Spring Boot应用
```bash
# 方式A: Maven直接运行
mvn spring-boot:run

# 方式B: Maven Wrapper
./mvnw.cmd spring-boot:run

# 方式C: 打包后运行
mvn clean package -DskipTests
java -jar target/internalpaas-*.jar
```

### 3. 默认端口
```
http://localhost:9090
```

---

## 🎯 演示页面清单

### 1. 🆕 工具函数演示页面 ⭐ **重点推荐**

**URL**: `http://localhost:9090/demo/utils`

**展示内容**:
- ✅ 格式化工具 (6个函数)
  - formatBytes: 1048576 → "1 MB"
  - formatUptime: 90000秒 → "1天 1小时"
  - formatPercentage: 85.678 → "85.7%"
  - formatTimestamp: 时间戳 → "2025-10-11 12:34:56"
  - formatNumber: 1234567.89 → "1,234,567.89"
  - getUsageClass: 使用率 → 样式类

- ✅ 国际化工具
  - 中英文动态切换
  - 实时文本更新

- ✅ 通知工具
  - 成功/错误/警告/信息 4种通知
  - 交互式按钮测试

- ✅ 图表工具
  - Chart.js折线图演示
  - CPU/内存使用率趋势

**特色**:
- 🎨 精美的渐变紫色UI
- 📱 响应式设计
- ✨ 流畅的交互动画
- 📊 实时统计数据展示

**截图位置**: 页面包含完整的可视化演示

---

### 2. 服务器群组管理页面

**URL**: `http://localhost:9090/admin/servers`

**展示内容**:
- 服务器列表（使用重构后的工具）
- 服务器详情弹窗
- 健康趋势图表
- 负载分布统计
- 批量操作功能

**使用的重构工具**:
- ✅ formatBytes - 显示磁盘大小
- ✅ formatUptime - 显示运行时间
- ✅ getChartColor - 图表配色
- ⚠️ 待迁移: 可以进一步使用新的i18n和notification工具

---

### 3. 管理员仪表板

**URL**: `http://localhost:9090/admin/dashboard`

**展示内容**:
- 系统概览
- 服务器摘要
- 统计图表
- 系统健康状态

**使用的重构工具**:
- ✅ dashboard.ts 模块（已优化）
- ✅ formatBytes / formatUptime（通过dashboard模块）
- ✅ 通知系统

---

### 4. 应用管理页面

**URL**: `http://localhost:9090/apps`
**需要登录**: 是

**展示内容**:
- 应用列表
- 应用状态
- 启动/停止控制

---

### 5. 监控历史页面

**URL**: `http://localhost:9090/monitoring/history`
**需要登录**: 是

**展示内容**:
- 历史监控数据
- 时间序列图表
- 性能趋势分析

**可使用的工具**:
- chart.ts - 图表配置和渲染
- format.ts - 数据格式化

---

## 📊 重构成果对比

### 页面功能对比表

| 页面 | 重构前 | 重构后 | 改进点 |
|------|--------|--------|--------|
| 服务器管理 | 内联格式化函数 | 使用utils工具 | 代码复用 |
| 仪表板 | 23行重复代码 | 导入工具函数 | -21行代码 |
| 图表页面 | 硬编码颜色 | Chart工具统一 | 配色一致 |
| 通知系统 | 多种实现 | 统一notification | 接口统一 |

---

## 🎮 演示操作流程

### 方案1: 新演示页面（推荐）

1. **启动应用**
   ```bash
   npm run build && mvn spring-boot:run
   ```

2. **访问演示页面**
   ```
   http://localhost:9090/demo/utils
   ```

3. **操作演示**
   - 查看顶部统计卡片（7个模块，33个函数，121个测试）
   - 浏览格式化工具演示区（6个实例）
   - 点击语言切换按钮（中文 ⇄ English）
   - 点击4个通知按钮测试不同类型
   - 查看底部Chart.js图表

### 方案2: 现有管理页面

1. **启动应用**
   ```bash
   npm run build && mvn spring-boot:run
   ```

2. **登录系统**
   ```
   http://localhost:9090/login
   默认账户: admin / admin
   ```

3. **访问管理页面**
   ```
   http://localhost:9090/admin/servers
   http://localhost:9090/admin/dashboard
   ```

4. **查看重构效果**
   - 服务器列表中的格式化显示（文件大小、运行时间）
   - 图表中的统一配色方案
   - 通知提示的一致性

---

## 📸 截图建议

### 演示页面截图要点

1. **整体页面**
   - 渐变背景 + 白色卡片布局
   - 顶部统计数据展示
   - 完整的4个演示区块

2. **格式化工具特写**
   - 6个格式化实例
   - 输入输出对比
   - 样式徽章展示

3. **交互功能**
   - 语言切换前后对比
   - 通知弹出效果
   - 图表动画

4. **代码对比**
   - 重构前的dashboard.ts（有重复代码）
   - 重构后的dashboard.ts（简洁导入）

---

## 🔍 代码对比展示

### 重构前 (dashboard.ts)
```typescript
// 260行，包含重复的工具函数

export function formatBytes(bytes: number): string {
    if (bytes === 0) return "0 Bytes";
    const k = 1024;
    const sizes = ["Bytes", "KB", "MB", "GB", "TB"];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + " " + sizes[i];
}

export function formatUptime(seconds: number): string {
    const days = Math.floor(seconds / 86400);
    const hours = Math.floor((seconds % 86400) / 3600);
    const minutes = Math.floor((seconds % 3600) / 60);

    if (days > 0) {
        return `${days}天 ${hours}小时`;
    } else if (hours > 0) {
        return `${hours}小时 ${minutes}分钟`;
    } else {
        return `${minutes}分钟`;
    }
}
```

### 重构后 (dashboard.ts)
```typescript
// 239行，使用统一工具

import { http, formatBytes, formatUptime } from "@/utils";

// ... 业务逻辑 ...

// 重新导出格式化工具函数 (保持向后兼容)
export { formatBytes, formatUptime };
```

**效果**: 减少21行代码，提升复用性

---

## 📈 测试覆盖率展示

### 运行测试
```bash
npm run test:run
```

### 预期结果
```
Test Files  8 passed (8)
Tests       121 passed (121)
Duration    ~10s

Coverage report:
---------------------------
format.ts      100% / 100% / 100%
dashboard.ts   82.32% / 86.95% / 88.23%
theme.ts       91.55% / 72.72% / 100%
chart.ts       100% / 96.66% / 100%
i18n.ts        100% / 72.91% / 100%
notification   100% / 100% / 100%
---------------------------
Average:       93.1%
```

---

## 🎯 演示重点

### 对技术受众

1. **代码质量提升**
   - 121个测试用例，93.1%覆盖率
   - 类型安全的TypeScript实现
   - 统一的工具函数接口

2. **工程化改进**
   - Vite构建系统
   - 模块化组织
   - 自动化测试

3. **可维护性**
   - 清晰的目录结构
   - 完整的文档
   - 可复用的组件

### 对非技术受众

1. **功能展示**
   - 精美的演示页面
   - 流畅的交互体验
   - 现代化的UI设计

2. **实际应用**
   - 服务器管理界面
   - 实时监控图表
   - 数据格式化展示

3. **项目成果**
   - 7个工具模块
   - 33个可复用函数
   - 零Breaking Changes

---

## 🛠️ 故障排查

### 页面无法访问

**问题**: 404 Not Found

**检查清单**:
1. ✅ 前端是否已构建？ `npm run build`
2. ✅ Spring Boot是否已启动？ `mvn spring-boot:run`
3. ✅ 端口是否正确？ 默认8080
4. ✅ 路径是否正确？ `/demo/utils`

### 样式显示异常

**问题**: 页面样式错乱

**解决方案**:
1. 检查构建产物是否存在
   ```bash
   ls src/main/resources/static/dist/assets/main.css
   ```
2. 清理缓存重新构建
   ```bash
   rm -rf src/main/resources/static/dist
   npm run build
   ```

### 工具函数未定义

**问题**: `formatBytes is not defined`

**解决方案**:
1. 确保main.js已加载
   ```html
   <script src="/dist/assets/main.js"></script>
   ```
2. 检查浏览器控制台错误
3. 验证构建产物完整性

---

## 📚 相关文档

- [演示指南](./upgrade/doc/demo-guide.md) - 详细启动步骤
- [优化总结](./upgrade/doc/optimization-summary.md) - 重构成果报告
- [短期任务报告](./upgrade/doc/short-term-tasks-report.md) - 任务完成情况
- [前端README](./src/main/frontend/README.md) - 工具使用文档

---

## 🎉 总结

我们提供了**2种展示方案**：

### 方案1: 新演示页面 ⭐ 推荐
- **URL**: `http://localhost:9090/demo/utils`
- **优势**: 专门设计，功能集中，视觉精美
- **适合**: 技术演示、项目汇报

### 方案2: 现有管理页面
- **URL**: `http://localhost:9090/admin/*`
- **优势**: 真实应用场景，实际功能展示
- **适合**: 功能验收、用户演示

**推荐流程**:
1. 先访问演示页面了解工具函数
2. 再访问管理页面查看实际应用
3. 查看代码对比了解优化效果

---

**文档创建**: 2025-10-11
**版本**: v1.0
**作者**: Claude Code
