# React嵌入模式检测 - 快速参考

## 核心问题

如何让React应用知道它是运行在嵌入模式（Spring Boot主应用内）还是独立模式（直接URL访问）？

---

## 推荐方案：多层验证Hook

### 快速实现（仅需3个文件）

#### 1️⃣ 创建检测Hook

**文件**：`src/main/frontend/react-app/hooks/useEmbedMode.ts`

```typescript
import { useMemo } from 'react';

/**
 * 检测React应用是否运行在嵌入模式（被Spring Boot主应用包含）
 * @returns true 嵌入模式 | false 独立模式
 */
export function useIsEmbeddedMode(): boolean {
    return useMemo(() => {
        if (typeof document === 'undefined') return false;

        // 信号1：容器存在
        const container = document.getElementById('deploy-platform-root');
        if (!container) return false;

        // 信号2：Spring Boot标志
        const hasSpringBootMarker =
            !!document.querySelector('[data-dev-shell-message]') ||  // main.ts
            !!document.querySelector('.content-framework') ||         // 模板
            !!document.querySelector('.sidebar') ||                   // 侧边栏
            !!document.querySelector('meta[name="_csrf"]');          // Spring Security

        // 信号3：显式标记
        const hasExplicitFlag =
            container.hasAttribute('data-embed-mode') &&
            container.getAttribute('data-embed-mode') === 'true';

        // 信号4：window标记
        const hasWindowFlag = (window as any).__DEPLOY_PLATFORM_EMBEDDED === true;

        // 综合判断
        return container && (hasSpringBootMarker || hasExplicitFlag || hasWindowFlag);
    }, []);
}
```

#### 2️⃣ 创建条件Layout组件

**文件**：`src/main/frontend/react-app/layout/ConditionalLayout.tsx`

```typescript
import { PropsWithChildren } from 'react';
import { ShellLayout } from './ShellLayout';
import { useIsEmbeddedMode } from '../hooks/useEmbedMode';

/**
 * 条件式布局组件
 * - 嵌入模式：不渲染侧边栏（Spring Boot已提供）
 * - 独立模式：渲染完整的ShellLayout
 */
export function ConditionalLayout({ children }: PropsWithChildren): JSX.Element {
    const isEmbedded = useIsEmbeddedMode();

    if (isEmbedded) {
        return <>{children}</>;
    }

    return <ShellLayout>{children}</ShellLayout>;
}
```

#### 3️⃣ 更新App.tsx

**文件**：`src/main/frontend/react-app/App.tsx`（修改部分）

```typescript
// 替换 import { ShellLayout } from "./layout/ShellLayout";
import { ConditionalLayout } from "./layout/ConditionalLayout";

export function AppRoutes(): JSX.Element {
    return (
        <ConditionalLayout>  {/* 用这个替换 <ShellLayout> */}
            <Routes>
                {/* ... routes ... */}
            </Routes>
        </ConditionalLayout>
    );
}
```

---

## 验证检测是否工作

### 测试嵌入模式

```bash
# 访问Spring Boot应用中的deploy-platform路由
# 打开浏览器开发者工具，在Console中运行：
console.log('嵌入模式:', document.getElementById('deploy-platform-root') !== null)
console.log('Spring Boot标志:', document.querySelector('[data-dev-shell-message]') !== null)
```

**预期输出**（嵌入模式）：
```
嵌入模式: true
Spring Boot标志: true
```

### 测试独立模式

```bash
# 访问直接React应用URL（如果支持）
# 或使用Vite dev server
npm run dev
# 打开 http://localhost:5173/
# 在Console中运行相同的检查代码

# 预期输出（独立模式）：
嵌入模式: false
Spring Boot标志: false
```

---

## 可选增强：显式标记

为了更可靠，在Spring Boot模板中添加显式标记：

**文件**：`src/main/resources/templates/admin/deploy-platform-content.html`

```html
<div id="deploy-platform-root"
     class="deploy-platform-host"
     data-embed-mode="true"  <!-- 添加这行 -->
     data-auth-endpoint="/api/deploy-platform/token"
     data-summary-endpoint="/api/deploy-platform/dashboard/summary">
    Loading...
</div>

<script>
    // 添加显式window标记
    window.__DEPLOY_PLATFORM_EMBEDDED = true;
</script>
```

---

## 检测原理

```
嵌入模式（Spring Boot主应用）
├─ main-layout.html
│  ├─ <meta name="_csrf"> ← CSRF token (Spring Security标志)
│  ├─ <div data-dev-shell-message> ← main.ts标志
│  ├─ <div class="content-framework"> ← 内容框架
│  └─ <div class="sidebar"> ← 侧边栏
│
├─ main.ts 加载 loadDeployPlatformMfe()
│
└─ deploy-platform-content.html（内容片段）
   └─ <div id="deploy-platform-root"> ← React挂载点
      └─ React App
         └─ ConditionalLayout
            └─ 检测：是否在上述Spring Boot环境中？
               → YES: 不渲染ShellLayout（使用Spring Boot的侧边栏）
               → NO:  渲染ShellLayout（自提供导航）

独立模式（直接React）
└─ index.html (Vite)
   └─ <div id="deploy-platform-root"> ← React挂载点
      └─ React App
         └─ ConditionalLayout
            └─ 检测：是否在Spring Boot环境中？
               → YES: 不渲染ShellLayout
               → NO:  ✓ 渲染ShellLayout（必需）
```

---

## 两种模式下的行为对比

| 特性 | 嵌入模式 | 独立模式 |
|------|---------|--------|
| **环境** | Spring Boot + React | 纯React应用 |
| **侧边栏提供者** | Spring Boot (main-layout.html) | React (ShellLayout.tsx) |
| **React输出** | 仅内容区域 | 完整应用框架 |
| **CSS** | 继承Spring Boot样式 | 独立样式 |
| **路由前缀** | 可能有 (data-router-base) | 通常是 "/" |
| **认证** | Spring Boot 会话 + Token Bridge | React自管理 |

---

## 常见问题

### Q: 如何在其他组件中访问嵌入模式标志？

**A:** 在任何组件中调用Hook：

```typescript
function MyComponent() {
    const isEmbedded = useIsEmbeddedMode();

    return (
        <div>
            {isEmbedded ? (
                <p>运行在Spring Boot内</p>
            ) : (
                <p>独立React应用</p>
            )}
        </div>
    );
}
```

---

### Q: 如果检测失败怎么办？

**A:** Hook会返回 `false`（假设为独立模式），此时：
- ✓ ShellLayout 会被渲染（多余但安全）
- ✓ React应用仍然能工作
- ⚠️ 在Spring Boot环境中会看到两个侧边栏

**如何调试**：
```typescript
// 在useIsEmbeddedMode()中添加日志
if (import.meta.env.DEV) {
    console.log('[embed-mode]', {
        containerExists: !!container,
        hasSpringBootMarker,
        hasExplicitFlag,
        hasWindowFlag,
        result: isEmbedded
    });
}
```

---

### Q: 支持多个嵌入式React应用吗？

**A:** 支持！为每个应用使用不同的容器ID：

```typescript
// 应用1：#deploy-platform-root
const container1 = document.getElementById('deploy-platform-root');

// 应用2：#other-app-root
const container2 = document.getElementById('other-app-root');

// 修改useIsEmbeddedMode使其参数化
export function useIsEmbeddedMode(containerId = 'deploy-platform-root'): boolean {
    const container = document.getElementById(containerId);
    // ... rest of logic ...
}
```

---

### Q: 在HMR (Hot Module Reload) 后会失效吗？

**A:** 不会！因为Hook基于**DOM结构的检测**而非易失的window属性：
- ✓ DOM结构在HMR后保持不变
- ✓ useMemo会重新计算（但结果相同）
- ✓ 应用状态保持一致

---

## 性能指标

| 操作 | 成本 | 执行时机 |
|------|------|---------|
| DOM查询 | ~0.1ms | 应用初始化 |
| useMemo缓存 | ~0.01ms | 组件装载 |
| 总体影响 | **可忽略** | 一次性 |

---

## 测试清单

- [ ] 在嵌入模式下测试（Spring Boot应用）
  - [ ] ShellLayout 未渲染
  - [ ] 内容区域正常显示
  - [ ] 路由正常工作

- [ ] 在独立模式下测试（Vite dev server）
  - [ ] ShellLayout 已渲染
  - [ ] 侧边栏可见
  - [ ] 导航正常工作

- [ ] 浏览器兼容性
  - [ ] Chrome/Edge
  - [ ] Firefox
  - [ ] Safari

- [ ] 网络隔离
  - [ ] 离线环境测试（如果有）
  - [ ] API不可用时的行为

---

## 文件清单

✅ 必需文件（最少实现）：
- `src/main/frontend/react-app/hooks/useEmbedMode.ts` - 检测Hook
- `src/main/frontend/react-app/layout/ConditionalLayout.tsx` - 条件Layout
- 修改 `src/main/frontend/react-app/App.tsx` - 使用ConditionalLayout

⭐ 推荐增强：
- 修改 `src/main/resources/templates/admin/deploy-platform-content.html` - 添加显式标记
- `src/main/frontend/tests/react-app/hooks/useEmbedMode.test.tsx` - 单元测试

---

## 相关文件引用

- 详细研究报告：`docs/research/react-embedded-detection-modes.md`
- React应用入口：`src/main/frontend/react-app/main.tsx`
- MFE加载器：`src/main/frontend/mfe/deploy-platform-loader.ts`
- Spring Boot主模板：`src/main/resources/templates/main-layout.html`

---

**快速参考版本**：1.0
**最后更新**：2025-11-02

💡 **TL;DR**：
1. 创建 `useIsEmbeddedMode()` Hook（检测DOM）
2. 创建 `ConditionalLayout` 组件（根据模式选择Layout）
3. 在 `App.tsx` 中使用 `ConditionalLayout` 替换 `ShellLayout`
4. ✅ 完成！应用现在支持两种模式
