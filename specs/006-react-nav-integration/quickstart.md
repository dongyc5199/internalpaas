# Quickstart Guide: React Navigation Integration

**Feature**: 006-react-nav-integration
**目标**: 5分钟内理解核心实现，15分钟完成基础集成

---

## 🎯 核心概念（2分钟）

### 问题
React部署平台嵌入主应用时出现"双侧边栏"，用户体验混乱。

### 解决方案
1. **检测模式**: React应用自动检测是嵌入还是独立运行
2. **条件布局**: 嵌入时隐藏React侧边栏，独立时显示
3. **导航同步**: 主应用菜单 ↔ React Router双向同步
4. **主题继承**: CSS变量自动继承，视觉一致

---

## 🚀 快速实现（15分钟）

### Step 1: 嵌入模式检测（3分钟）

创建 `src/main/frontend/react-app/hooks/useEmbedMode.ts`:

```typescript
import { useState, useEffect } from 'react';

export function useEmbedMode(): boolean {
  const [isEmbedded, setIsEmbedded] = useState(false);

  useEffect(() => {
    const container = document.getElementById('deploy-platform-root');
    if (!container) {
      setIsEmbedded(false);
      return;
    }

    // 多层验证
    const hasSpringContext = container.getAttribute('data-spring-context') === 'true';
    const hasEmbedFlag = container.getAttribute('data-embedded') === 'true';
    const hasWindowFlag = (window as any).__DEPLOY_PLATFORM_EMBEDDED__ === true;

    setIsEmbedded(hasSpringContext || hasEmbedFlag || hasWindowFlag);
  }, []);

  return isEmbedded;
}
```

### Step 2: 布局Context（4分钟）

**2.1 创建Context** (`contexts/layoutContext.ts`):

```typescript
import { createContext, useContext } from 'react';

export type LayoutMode = 'shell' | 'content-only';

export interface LayoutContextValue {
  mode: LayoutMode;
  isEmbedded: boolean;
}

export const LayoutContext = createContext<LayoutContextValue | null>(null);

export function useLayout(): LayoutContextValue {
  const context = useContext(LayoutContext);
  if (!context) throw new Error('useLayout must be within LayoutProvider');
  return context;
}
```

**2.2 创建Provider** (`providers/LayoutProvider.tsx`):

```typescript
import { ReactNode, useMemo } from 'react';
import { LayoutContext } from '../contexts/layoutContext';
import { useEmbedMode } from '../hooks/useEmbedMode';

export function LayoutProvider({ children }: { children: ReactNode }) {
  const isEmbedded = useEmbedMode();

  const value = useMemo(
    () => ({
      mode: isEmbedded ? ('content-only' as const) : ('shell' as const),
      isEmbedded
    }),
    [isEmbedded]
  );

  return <LayoutContext.Provider value={value}>{children}</LayoutContext.Provider>;
}
```

### Step 3: 条件布局（4分钟）

**3.1 创建无侧边栏布局** (`components/ContentOnlyLayout.tsx`):

```typescript
import { ReactNode } from 'react';

export function ContentOnlyLayout({ children }: { children: ReactNode }) {
  return (
    <div className="content-only-layout">
      <main className="dp-main-content">{children}</main>
    </div>
  );
}
```

**3.2 创建布局选择器** (`components/LayoutSelector.tsx`):

```typescript
import { ReactNode, useMemo } from 'react';
import { useLayout } from '../contexts/layoutContext';
import { ShellLayout } from '../layout/ShellLayout';
import { ContentOnlyLayout } from './ContentOnlyLayout';

export function LayoutSelector({ children }: { children: ReactNode }) {
  const { mode } = useLayout();

  const LayoutComponent = useMemo(() => {
    return mode === 'shell' ? ShellLayout : ContentOnlyLayout;
  }, [mode]);

  return <LayoutComponent>{children}</LayoutComponent>;
}
```

### Step 4: 集成到App（2分钟）

修改 `App.tsx`:

```typescript
import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { LayoutProvider } from './providers/LayoutProvider';
import { LayoutSelector } from './components/LayoutSelector';
import { OverviewPage } from './pages/OverviewPage';
import { ReleasesPage } from './pages/ReleasesPage';
import { PoliciesPage } from './pages/PoliciesPage';

function App() {
  return (
    <LayoutProvider>
      <LayoutSelector>
        <BrowserRouter>
          <Routes>
            <Route path="/overview" element={<OverviewPage />} />
            <Route path="/releases" element={<ReleasesPage />} />
            <Route path="/policies" element={<PoliciesPage />} />
          </Routes>
        </BrowserRouter>
      </LayoutSelector>
    </LayoutProvider>
  );
}

export default App;
```

### Step 5: 主应用标记（2分钟）

修改 `src/main/resources/templates/admin/deploy-platform-content.html`:

```html
<div id="deploy-platform-root"
     class="deploy-platform-host"
     data-spring-context="true"
     data-embedded="true"
     data-auth-endpoint="/api/deploy-platform/token">
    Loading deployment console...
</div>
```

---

## ✅ 验证（2分钟）

### 测试嵌入模式
1. 启动应用: `./mvnw.cmd spring-boot:run`
2. 访问: `http://localhost:8080/admin/workspace`
3. 点击"部署管理平台"菜单
4. **预期**: 仅显示主应用侧边栏，无React侧边栏

### 测试独立模式
1. 直接访问: `http://localhost:8080/admin/deploy-platform`
2. **预期**: 显示React完整布局（含侧边栏）

---

## 📚 下一步

**Phase 1完成** ✅ 条件布局已实现

**继续Phase 2: 导航同步**
- 添加主应用导航事件发送器
- 添加React导航事件监听器
- 实现防循环触发机制

详见: `research.md` 第2部分 "双向导航同步"

---

## 🐛 故障排除

### 问题1: React仍显示侧边栏（嵌入模式）

**检查**:
```javascript
// 浏览器控制台
const container = document.getElementById('deploy-platform-root');
console.log(container.getAttribute('data-spring-context')); // 应该是 "true"
console.log(container.getAttribute('data-embedded'));       // 应该是 "true"
```

**修复**: 确保 `deploy-platform-content.html` 中的数据属性正确设置

### 问题2: 独立模式无侧边栏

**检查**:
```javascript
// 浏览器控制台
const isEmbedded = useEmbedMode(); // 通过React DevTools查看
// 应该是 false
```

**修复**: 确保独立访问时不存在嵌入标记

### 问题3: TypeScript类型错误

**检查**: `tsconfig.json` 包含正确的编译选项
```json
{
  "compilerOptions": {
    "strict": true,
    "jsx": "react-jsx"
  }
}
```

---

## 📖 完整文档

- **研究报告**: [research.md](./research.md) - 技术决策和最佳实践
- **数据模型**: [data-model.md](./data-model.md) - 状态结构和事件定义
- **实施计划**: [plan.md](./plan.md) - 完整的实施路线图

---

## 🎓 关键概念速查

| 概念 | 说明 | 文件 |
|------|------|------|
| 嵌入模式 | React应用无侧边栏，使用主应用导航 | `useEmbedMode.ts` |
| 独立模式 | React应用完整布局，独立运行 | `ShellLayout.tsx` |
| LayoutMode | `'shell'` 或 `'content-only'` | `layoutContext.ts` |
| LayoutProvider | 提供布局模式Context | `LayoutProvider.tsx` |
| LayoutSelector | 根据模式选择布局组件 | `LayoutSelector.tsx` |

---

**预计时间**: 实际15-20分钟完成基础集成 ✨
