# 工具函数演示页面使用指南

## 📋 概述

我们创建了一个精美的演示页面来展示前端重构后的工具模块功能，包括：
- 格式化工具 (Format Utils)
- 国际化工具 (i18n Utils)
- 通知工具 (Notification Utils)
- 图表工具 (Chart Utils)

---

## 🚀 快速启动

### 方式一：启动完整Spring Boot应用

#### 1. 构建前端资源
```bash
cd E:\work\code\internalpaas
npm run build
```

#### 2. 启动Spring Boot应用
```bash
# 方式A: 使用Maven
mvn spring-boot:run

# 方式B: 使用Maven Wrapper
./mvnw.cmd spring-boot:run

# 方式C: 先打包再运行
mvn clean package -DskipTests
java -jar target/internalpaas-*.jar
```

#### 3. 访问演示页面
打开浏览器访问：
```
http://localhost:9090/demo/utils
```

如果端口被占用，检查 `application.properties` 中的 `server.port` 配置。

---

### 方式二：独立访问HTML页面（推荐用于快速预览）

由于演示页面依赖构建后的JS文件，需要先构建：

#### 1. 构建前端
```bash
cd E:\work\code\internalpaas
npm run build
```

#### 2. 使用本地服务器预览
```bash
# 使用Python启动简单HTTP服务器
cd src/main/resources/static
python -m http.server 8000

# 或使用Node.js的http-server (需要先安装: npm install -g http-server)
cd src/main/resources/static
http-server -p 8000
```

#### 3. 访问页面
```
http://localhost:8000/templates/utils-demo.html
```

---

## 🎯 演示页面功能

### 1. 格式化工具演示 (Format Utils)

展示6个格式化函数的实际效果：

| 函数 | 示例输入 | 示例输出 |
|------|---------|---------|
| formatBytes | 1048576 | 1 MB |
| formatUptime | 90000秒 | 1天 1小时 |
| formatPercentage | 85.678 | 85.7% |
| formatTimestamp | 当前时间戳 | 2025-10-11 12:34:56 |
| formatNumber | 1234567.89 | 1,234,567.89 |
| getUsageClass | 45%, 80%, 95% | 正常/警告/危险 |

### 2. 国际化工具演示 (i18n Utils)

- 动态语言切换（中文 ⇄ English）
- 实时更新页面文本
- 演示 `setLocalizedText` 和 `refreshContainerI18n` 功能

### 3. 通知工具演示 (Notification Utils)

4个交互按钮测试不同类型的通知：
- ✅ 成功通知 (showSuccess)
- ❌ 错误通知 (showError)
- ⚠️ 警告通知 (showWarning)
- ℹ️ 信息通知 (showInfo)

### 4. 图表工具演示 (Chart Utils)

实时渲染的Chart.js折线图：
- 使用 `createTimeSeriesChartConfig` 创建配置
- 使用 `getChartColor` 自动分配颜色
- 展示CPU和内存使用率趋势

---

## 📊 页面统计卡片

页面顶部展示重构成果统计：

| 指标 | 数值 |
|-----|------|
| 工具模块数量 | 7个 |
| 可复用函数 | 33个 |
| 测试用例 | 121个 |
| 测试覆盖率 | 93.1% |

---

## 🎨 页面特色

### 设计亮点
- 🎨 渐变紫色背景，现代化UI设计
- 📱 响应式布局，支持移动端
- ✨ 卡片悬停效果
- 🎯 清晰的功能分区
- 💫 流畅的交互动画

### 技术特点
- 使用重构后的工具函数
- TypeScript类型安全
- Chart.js图表渲染
- 实时交互演示

---

## 🔧 故障排查

### 问题1: 工具函数未定义
**原因**: 前端资源未构建或路径错误

**解决方案**:
```bash
# 重新构建前端
npm run build

# 检查构建产物
ls src/main/resources/static/dist/assets/main.js
```

### 问题2: 端口被占用
**错误信息**: `Port 8080 is already in use`

**解决方案**:
```bash
# 查找占用端口的进程
netstat -ano | findstr 8080

# 终止进程
taskkill /F /PID <进程ID>

# 或者修改端口
# 编辑 src/main/resources/application.properties
# 添加: server.port=8081
```

### 问题3: 页面样式不正常
**原因**: 静态资源路径配置问题

**解决方案**:
检查 `application.properties` 中的静态资源配置：
```properties
spring.web.resources.static-locations=classpath:/static/
spring.mvc.static-path-pattern=/static/**
```

### 问题4: 图表不显示
**原因**: Chart.js库加载失败

**解决方案**:
- 检查网络连接（CDN加载）
- 或下载Chart.js到本地：
```bash
npm install chart.js
# 然后在页面中引用本地文件
```

---

## 📝 代码示例

### 在其他页面中使用工具函数

```html
<!-- 引入构建后的工具模块 -->
<script src="/dist/assets/main.js"></script>

<script>
// 使用格式化工具
const sizeText = formatBytes(1048576); // "1 MB"
const timeText = formatUptime(3600);   // "1小时 0分钟"

// 使用通知工具
showSuccess("操作成功！");
showError("操作失败！");

// 使用国际化工具
const element = document.getElementById('title');
setLocalizedText(element, "服务器", "Server");

// 使用图表工具
const color = getChartColor(0); // "rgba(99, 102, 241, 1)"
</script>
```

---

## 🌟 扩展演示

### 添加新的演示项

1. 在 `utils-demo.html` 中添加新的演示区块
2. 引入相关工具函数
3. 添加交互逻辑

### 创建自定义演示页面

参考 `utils-demo.html` 的结构：
```html
<div class="demo-section">
    <h2>📦 新模块演示</h2>
    <div class="demo-grid">
        <!-- 演示项 -->
    </div>
</div>
```

---

## 📚 相关文档

- [前端工程化目录说明](../../src/main/frontend/README.md)
- [优化总结报告](./optimization-summary.md)
- [短期任务完成报告](./short-term-tasks-report.md)
- [重构计划](./server-group-refactor-plan.md)

---

## 🎯 下一步

### 在实际项目中使用
1. 在 `server-group-content.html` 中替换旧的格式化代码
2. 在 `admin-dashboard-content.html` 中使用新的通知工具
3. 在图表页面中使用新的Chart工具

### 持续改进
1. 添加更多交互演示
2. 补充使用文档
3. 录制演示视频

---

## 📞 支持

如有问题，请参考：
- 项目主文档: `CLAUDE.md`
- 技术方案: `upgrade/doc/tech-plan.md`
- 或提交Issue到项目仓库

---

**演示页面创建日期**: 2025-10-11
**版本**: v1.0
**作者**: Claude Code
