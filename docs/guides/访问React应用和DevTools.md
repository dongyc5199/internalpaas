# 访问 React 应用和 DevTools 指南

## 问题诊断

### 问题1：疯狂调用接口
**原因**：`useDeploymentSummary` hook 在 API 调用失败时会以 15 秒间隔无限重试。

**已修复**：修改了 `src/main/frontend/react-app/hooks/useDeploymentSummary.ts`，移除了失败重试逻辑。

### 问题2：看不到 DevTools 图标
**原因**：可能访问了错误的页面，或者 React 应用未正确加载。

## 正确的访问路径

### 方案1：通过部署管理平台页面（推荐）

1. **访问地址**：
   ```
   http://localhost:9090/admin/deploy-platform
   ```
   或者通过主菜单：
   - 登录系统
   - 点击管理员菜单
   - 选择 "部署管理平台"

2. **确认 React 应用加载**：
   - 页面应该显示 "Deployment Management Platform" 标题
   - 下方应该有一个加载中的面板区域
   - 打开浏览器控制台（F12），查看是否有 JavaScript 加载错误

3. **查找 DevTools 图标**：
   - 在页面 **左下角** 查找红色花朵图标
   - 如果没有看到，请检查：
     - 浏览器控制台是否有错误
     - React 应用是否成功加载
     - 是否在开发模式下运行

### 方案2：通过调试页面

访问专门的调试页面：
```
http://localhost:9090/debug/react-mfe-demo
```

这个页面只包含 React 应用，更容易定位问题。

## 验证 React 应用是否加载

### 步骤1：检查 HTML 元素

在浏览器控制台运行：
```javascript
document.getElementById('deploy-platform-root')
```

应该返回一个 `<div>` 元素，而不是 `null`。

### 步骤2：检查 React 组件是否挂载

在浏览器控制台运行：
```javascript
document.querySelector('.dp-page')
```

如果 React 应用正确加载，应该能找到这个元素。

### 步骤3：检查 DevTools 组件

在浏览器控制台运行：
```javascript
document.querySelector('[data-testid*="query"]') ||
document.querySelector('button[aria-label*="React Query"]')
```

如果返回元素，说明 DevTools 已加载但可能被 CSS 隐藏了。

## 重新构建应用

如果上述方法都无法看到 React 应用，请重新构建：

### 步骤1：清理并重新构建

```bash
# 清理旧的构建文件
rm -rf src/main/resources/static/dist

# 重新构建前端
npm run build

# 重启 Spring Boot 应用
# 在运行的终端按 Ctrl+C 停止，然后重新启动
./mvnw.cmd spring-boot:run
```

### 步骤2：验证构建文件

检查是否生成了必要的文件：
```bash
ls -lh src/main/resources/static/dist/assets/
```

应该看到：
- `deploy-platform.js` (约 183 KB)
- `main.js` (约 309 KB)
- `__federation_expose_App.js` (约 165 KB)
- `main.css` 和 `__federation_expose_App.css`

### 步骤3：检查浏览器缓存

强制刷新页面：
- Windows/Linux: `Ctrl + Shift + R` 或 `Ctrl + F5`
- Mac: `Cmd + Shift + R`

## Phase 6 测试的正确路径

### 测试发布管理（Releases）

**不要访问**: `http://localhost:9090/releases`（旧的 Thymeleaf 页面）

**应该访问**:
```
http://localhost:9090/admin/deploy-platform
```
然后在 React 应用内导航到 `/releases`，完整路径：
```
http://localhost:9090/admin/deploy-platform#/releases
```

### 测试策略配置（Policies）

**应该访问**:
```
http://localhost:9090/admin/deploy-platform#/settings/policies
```

## DevTools 使用

一旦看到左下角的花朵图标：

1. **打开 DevTools**
   - 点击图标
   - 或按 `Ctrl + Shift + D`（Mac: `Cmd + Shift + D`）

2. **查看查询**
   - 导航到 Releases 页面，应该看到 `['releases', ...]` 查询
   - 导航到 Policies 页面，应该看到 `['policies', ...]` 查询

3. **验证缓存**
   - 切换页面后回来，查询应该立即显示数据（从缓存）
   - Network 标签不应该有新请求

## 如果仍然看不到 DevTools

### 可能的原因和解决方案

#### 原因1：React 应用未启动

**症状**：页面只显示 "Loading deployment console..."

**解决方案**：
1. 检查浏览器控制台是否有 JavaScript 错误
2. 检查 Network 标签是否成功加载了 `deploy-platform.js`
3. 确认文件路径：`http://localhost:9090/dist/assets/deploy-platform.js`

#### 原因2：DevTools 被 CSS 隐藏

**症状**：React 应用正常工作，但看不到图标

**解决方案**：
在浏览器控制台运行：
```javascript
const devtools = document.querySelector('[class*="tsqd"]');
if (devtools) {
    devtools.style.zIndex = '999999';
    devtools.style.position = 'fixed';
    devtools.style.bottom = '10px';
    devtools.style.left = '10px';
    console.log('DevTools found and repositioned');
} else {
    console.log('DevTools component not found');
}
```

#### 原因3：生产模式构建

**症状**：一切正常但就是没有 DevTools

**解决方案**：
确认使用开发模式：
```bash
# 检查环境变量
echo $NODE_ENV

# 应该是空或 'development'
# 如果是 'production'，DevTools 会被 tree-shake 移除
```

重新以开发模式构建：
```bash
NODE_ENV=development npm run build
```

## 调试检查清单

- [ ] Spring Boot 应用运行在端口 9090
- [ ] 访问正确的 URL（`/admin/deploy-platform`）
- [ ] 页面加载完成，没有 JavaScript 错误
- [ ] `deploy-platform-root` 元素存在
- [ ] React 组件已挂载（能找到 `.dp-page`）
- [ ] 前端资源文件成功加载（检查 Network 标签）
- [ ] 浏览器缓存已清除（强制刷新）
- [ ] 开发者工具控制台无错误

## 快速测试命令

在浏览器控制台运行以下脚本进行全面检查：

```javascript
const checks = {
    '1. React Root': !!document.getElementById('deploy-platform-root'),
    '2. React Mounted': !!document.querySelector('.dp-page'),
    '3. DevTools Present': !!document.querySelector('[class*="tsqd"]'),
    '4. Query Client': !!window.__REACT_QUERY_CLIENT__,
};

console.table(checks);

// 如果都是 false，React 应用未加载
// 如果 1-2 是 true，3 是 false，DevTools 未加载
// 如果都是 true，DevTools 可能被隐藏
```

## 联系支持

如果以上所有方法都无效，请提供以下信息：

1. 浏览器控制台的完整错误日志
2. Network 标签的截图（显示资源加载情况）
3. `document.getElementById('deploy-platform-root').innerHTML` 的输出
4. 上述"快速测试命令"的输出

---

**更新时间**: 2025-10-30 23:30
**相关文件**:
- `src/main/frontend/react-app/App.tsx` (DevTools 集成)
- `src/main/frontend/react-app/hooks/useDeploymentSummary.ts` (已修复重试问题)
