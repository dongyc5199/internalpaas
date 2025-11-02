# 手动验证测试指南 - User Story 1: 嵌入模式导航无重复

## 测试环境

- **应用地址**: http://localhost:8080
- **测试日期**: 2025-11-02
- **测试人员**: Claude AI + 用户
- **浏览器要求**: Chrome/Edge/Firefox (最新版本,支持ES6)

## 前置条件

1. ✅ 应用已启动并运行在 http://localhost:8080
2. ✅ 前端代码已重新构建 (`npm run build`)
3. ✅ 所有单元测试通过 (66/66 tests ✅)
4. ✅ TypeScript编译无错误

## 测试任务 T018: 验证嵌入模式渲染

### 测试目标
验证React应用嵌入到主应用时,**不显示React应用自己的侧边栏**,仅使用主应用提供的侧边栏导航。

### 测试步骤

#### 步骤1: 访问主应用工作区
```
URL: http://localhost:8080/
操作: 登录系统 (如需要)
```

#### 步骤2: 导航到部署管理平台
```
操作: 在主应用左侧导航栏找到"部署管理平台"菜单项并点击
预期URL: http://localhost:8080/admin/deploy-platform 或类似路径
```

#### 步骤3: 检查页面DOM结构
打开浏览器开发者工具 (F12) → Elements标签页

**检查项A: 验证容器属性**
```javascript
// 在Console中执行:
const container = document.getElementById('deploy-platform-root');
console.log({
  hasSpringContext: container.getAttribute('data-spring-context'),
  hasEmbedded: container.getAttribute('data-embedded'),
  isEmbedded: window.__DEPLOY_PLATFORM_EMBEDDED__
});

// 预期输出:
// {
//   hasSpringContext: "true",
//   hasEmbedded: "true",
//   isEmbedded: undefined (或 true)
// }
```

**检查项B: 验证React应用侧边栏不存在**
```javascript
// 在Console中执行:
const reactSidebar = document.querySelector('.dp-sidebar'); // React侧边栏类名
const shellLayout = document.querySelector('.dp-shell'); // ShellLayout根元素
console.log({
  reactSidebarExists: !!reactSidebar,
  shellLayoutExists: !!shellLayout
});

// 预期输出:
// {
//   reactSidebarExists: false,  // ⚠️ 关键验证点!
//   shellLayoutExists: false    // 应使用ContentOnlyLayout
// }
```

**检查项C: 验证使用ContentOnlyLayout**
```javascript
// 在Console中执行:
const contentOnlyLayout = document.querySelector('.content-only-layout');
const mainContent = document.querySelector('.dp-main-content');
console.log({
  contentOnlyLayoutExists: !!contentOnlyLayout,
  mainContentExists: !!mainContent
});

// 预期输出:
// {
//   contentOnlyLayoutExists: true,  // ✅ 应该存在
//   mainContentExists: true          // ✅ 应该存在
// }
```

#### 步骤4: 目视检查页面布局
- [ ] **只有一个侧边栏** (主应用的侧边栏)
- [ ] React应用内容完全填充内容区域
- [ ] 无双侧边栏现象
- [ ] 页面响应流畅,无样式错误

### 验收标准

| 检查项 | 预期结果 | 实际结果 | 状态 |
|--------|----------|----------|------|
| 容器data-spring-context | "true" | _______ | [ ] |
| 容器data-embedded | "true" | _______ | [ ] |
| React侧边栏 (.dp-sidebar) | 不存在 (false) | _______ | [ ] |
| ShellLayout (.dp-shell) | 不存在 (false) | _______ | [ ] |
| ContentOnlyLayout | 存在 (true) | _______ | [ ] |
| 仅一个侧边栏 | 是 | _______ | [ ] |
| 页面正常渲染 | 是 | _______ | [ ] |

---

## 测试任务 T019: 验证独立模式渲染

### 测试目标
验证React应用独立运行时,**正常显示React应用自己的侧边栏** (ShellLayout)。

### 测试步骤

#### 步骤1: 直接访问React应用URL
```
URL: http://localhost:8080/admin/deploy-platform
操作: 在新标签页直接访问 (绕过主应用导航)
```

**注意**: 根据项目路由配置,独立访问URL可能不同。如果上述URL仍处于嵌入模式,请尝试:
- 打开新的隐身窗口/无痕模式
- 清除浏览器缓存
- 或者修改HTML模板临时移除嵌入属性进行测试

#### 步骤2: 检查页面DOM结构

**检查项A: 验证容器属性**
```javascript
// 在Console中执行:
const container = document.getElementById('deploy-platform-root');
console.log({
  hasSpringContext: container?.getAttribute('data-spring-context'),
  hasEmbedded: container?.getAttribute('data-embedded'),
  containerExists: !!container
});

// 预期输出 (独立模式):
// {
//   hasSpringContext: null,  // 或 "false"
//   hasEmbedded: null,       // 或 "false"
//   containerExists: true
// }
```

**检查项B: 验证ShellLayout正常渲染**
```javascript
// 在Console中执行:
const shellLayout = document.querySelector('.dp-shell');
const reactSidebar = document.querySelector('.dp-sidebar');
const contentOnlyLayout = document.querySelector('.content-only-layout');
console.log({
  shellLayoutExists: !!shellLayout,
  reactSidebarExists: !!reactSidebar,
  contentOnlyLayoutExists: !!contentOnlyLayout
});

// 预期输出:
// {
//   shellLayoutExists: true,         // ✅ 应该存在
//   reactSidebarExists: true,        // ✅ 应该存在
//   contentOnlyLayoutExists: false   // ⚠️ 不应存在
// }
```

#### 步骤3: 目视检查页面布局
- [ ] **React应用完整侧边栏显示** (包含"概览"、"发布"、"策略"等菜单项)
- [ ] 侧边栏功能正常 (点击菜单项可切换页面)
- [ ] 页面布局完整,无缺失元素
- [ ] 无主应用侧边栏 (因为是独立访问)

### 验收标准

| 检查项 | 预期结果 | 实际结果 | 状态 |
|--------|----------|----------|------|
| 容器data-spring-context | null/"false" | _______ | [ ] |
| 容器data-embedded | null/"false" | _______ | [ ] |
| ShellLayout (.dp-shell) | 存在 (true) | _______ | [ ] |
| React侧边栏 (.dp-sidebar) | 存在 (true) | _______ | [ ] |
| ContentOnlyLayout | 不存在 (false) | _______ | [ ] |
| 侧边栏菜单完整 | 是 | _______ | [ ] |
| 页面正常渲染 | 是 | _______ | [ ] |

---

## 调试工具

### 启用调试日志
```javascript
// 在Console中执行以启用详细日志:
window.__DEPLOY_PLATFORM_DEBUG__ = true;

// 刷新页面,观察Console输出:
// [useEmbedMode] Detection signals: { ... }
// [LayoutProvider] Mode changed: ...
// [LayoutSelector] Unknown mode: ... (仅在错误时)
```

### 手动切换布局模式 (仅用于调试)
```javascript
// 注意: 仅用于开发调试,生产环境不应手动切换
// 在Console中执行:
const { useLayout } = await import('/dist/assets/main.js'); // 路径可能不同
// 需要通过React DevTools访问组件实例
```

### 检查React DevTools
1. 安装 **React Developer Tools** 浏览器扩展
2. 打开DevTools → Components标签页
3. 查找 `LayoutProvider` 组件
4. 检查Props:
   ```
   mode: "content-only" (嵌入模式) 或 "shell" (独立模式)
   isEmbedded: true (嵌入) 或 false (独立)
   ```

---

## 问题排查

### 问题1: T018测试时仍然显示React侧边栏

**可能原因**:
1. 容器属性未正确设置
2. 前端代码未重新构建
3. 浏览器缓存未清除

**解决方法**:
```bash
# 1. 验证模板文件
grep -A 5 "deploy-platform-root" src/main/resources/templates/admin/deploy-platform-content.html

# 2. 重新构建前端
npm run build

# 3. 重启应用
./mvnw.cmd spring-boot:run

# 4. 清除浏览器缓存 (Ctrl+Shift+Delete)
```

### 问题2: T019测试时无法访问独立模式

**可能原因**:
当前项目架构中,React应用可能只能通过主应用嵌入访问,无独立访问路由。

**临时测试方法**:
```html
<!-- 临时修改 deploy-platform-content.html -->
<div id="deploy-platform-root"
     class="deploy-platform-host"
     data-auth-endpoint="/api/deploy-platform/token"
     data-summary-endpoint="/api/deploy-platform/dashboard/summary"
     data-spring-context="false"  <!-- 临时改为false -->
     data-embedded="false"         <!-- 临时改为false -->
     ...>
```
重启应用后重新测试,**测试完成后记得恢复为true**。

### 问题3: Console报错 "useLayout must be used within LayoutProvider"

**原因**: LayoutProvider未正确包裹组件树

**检查**:
```javascript
// 查看 App.tsx 结构应为:
<LayoutProvider>
  <BrowserRouter>
    <LayoutSelector>
      <Routes>...</Routes>
    </LayoutSelector>
  </BrowserRouter>
</LayoutProvider>
```

---

## 测试完成清单

### T018: 嵌入模式验证
- [ ] 所有7项检查通过
- [ ] 截图已保存 (可选)
- [ ] 问题已记录 (如有)

### T019: 独立模式验证
- [ ] 所有7项检查通过
- [ ] 截图已保存 (可选)
- [ ] 问题已记录 (如有)

### 最终结论
- [ ] User Story 1 完全实现 ✅
- [ ] 无阻塞性问题
- [ ] 可以进入Phase 4 (User Story 2)

---

**测试完成后,请将结果更新到 tasks.md 文件中:**
```markdown
- [x] T018 [US1] 验证嵌入模式渲染 - ✅ 通过 (日期: YYYY-MM-DD)
- [x] T019 [US1] 验证独立模式渲染 - ✅ 通过 (日期: YYYY-MM-DD)
```
