# React Query DevTools 使用指南

## 概述

React Query DevTools 是一个强大的开发工具，帮助您可视化和调试 React Query 的状态管理。本指南将介绍如何使用 DevTools 来验证 Phase 6 实现的缓存策略和乐观更新功能。

## 集成说明

DevTools 已集成到应用中（`src/main/frontend/react-app/App.tsx`），在开发模式下自动可用。

### 安装的包

```json
{
  "@tanstack/react-query": "^5.90.5",
  "@tanstack/react-query-devtools": "^5.90.5"
}
```

### 集成代码

```typescript
import { ReactQueryDevtools } from "@tanstack/react-query-devtools";

export function App({ basename }: AppProps = {}): JSX.Element {
    return (
        <QueryClientProvider client={queryClient}>
            <I18nextProvider i18n={i18n}>
                <BrowserRouter basename={base}>
                    <AppRoutes />
                </BrowserRouter>
            </I18nextProvider>
            <ReactQueryDevtools initialIsOpen={false} />
        </QueryClientProvider>
    );
}
```

## 启动应用

### 1. 启动开发服务器

确保后端 Spring Boot 应用正在运行：

```bash
# 在项目根目录
./mvnw.cmd spring-boot:run
```

等待服务器启动完成（通常在 `http://localhost:8080`）。

### 2. 访问应用

在浏览器中打开：

```
http://localhost:8080
```

或者如果前端单独运行：

```bash
npm run dev
```

然后访问 `http://localhost:5173`（或 Vite 提供的端口）。

## 使用 DevTools

### 打开 DevTools

在浏览器中，您会在页面左下角看到一个 **React Query** 图标（花朵图标）。

- **点击图标**打开 DevTools 面板
- DevTools 默认关闭（`initialIsOpen={false}`），不会影响页面性能

### DevTools 界面说明

DevTools 面板包含以下主要区域：

#### 1. 查询列表（Query List）

显示所有活跃的查询：

```
🟢 queries/releases/undefined/1/20
   - fresh (新鲜状态)
   - stale (过期状态)
   - fetching (正在获取)
   - inactive (未激活)

🟢 queries/policies/undefined/1/20
   - 状态标识与上同
```

**查询键说明**：
- `['releases', filters, page, pageSize]` - 发布列表查询
- `['policies', filters, page, pageSize]` - 策略列表查询

**颜色含义**：
- 🟢 **绿色** - 查询有缓存数据
- 🟡 **黄色** - 查询正在获取数据
- 🔴 **红色** - 查询发生错误
- ⚪ **灰色** - 查询未激活

#### 2. 查询详情（Query Details）

点击任意查询查看详细信息：

- **Data** - 缓存的数据内容
- **Query Key** - 查询键数组
- **Observers** - 订阅此查询的组件数量
- **Last Updated** - 最后更新时间
- **Data Updated At** - 数据更新时间戳

#### 3. 操作按钮

- **Refetch** - 手动重新获取数据
- **Invalidate** - 使查询失效，触发重新获取
- **Reset** - 重置查询状态
- **Remove** - 从缓存中移除查询

## 验证 Phase 6 功能

### 任务 T069：验证缓存策略（5分钟缓存）

我们的配置（`src/main/frontend/react-app/config/queryClient.ts`）：

```typescript
const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 5 * 60 * 1000,  // 5分钟
      gcTime: 10 * 60 * 1000,    // 10分钟
      retry: 1,
      refetchOnWindowFocus: false,
    },
  },
});
```

#### 验证步骤

1. **打开 ReleasesPage**
   - 导航到 `/releases` 页面
   - 观察 DevTools 中出现 `['releases', ...]` 查询

2. **观察初始状态**
   - 查询状态应该是 `fetching` → `fresh`
   - 点击查询查看 **Last Updated** 时间戳

3. **测试缓存命中**
   - 切换到其他页面（如 `/overview`）
   - 立即切换回 `/releases`
   - 观察：**不应该**看到新的网络请求（在浏览器 Network 标签中）
   - DevTools 中查询仍为 `fresh` 状态

4. **测试 staleTime（5分钟）**
   - 等待 5 分钟后
   - 查询状态应该从 `fresh` 变为 `stale`
   - 当重新访问页面时，会自动触发后台刷新

5. **验证 gcTime（10分钟）**
   - 离开 `/releases` 页面
   - 等待 10 分钟
   - 查询应该从 DevTools 的列表中消失（垃圾回收）

#### 预期结果

✅ **5分钟内**：数据从缓存加载，无网络请求
✅ **5-10分钟**：数据标记为过期，重新访问时后台刷新
✅ **10分钟后**：缓存被清理

### 任务 T070：验证乐观更新

我们的实现（`src/main/frontend/react-app/hooks/useUpdatePolicy.ts`）：

```typescript
export function useUpdatePolicy(options?: UseUpdatePolicyOptions) {
  return useMutation({
    onMutate: async (variables) => {
      // 立即更新 UI（乐观更新）
      queryClient.setQueriesData<PolicyListResponse>(
        { queryKey: ['policies'] },
        (oldData) => {
          if (!oldData) return oldData;
          return {
            ...oldData,
            policies: oldData.policies.map((policy) =>
              policy.id === policyId ? { ...policy, ...request } : policy
            ),
          };
        }
      );
    },
    onError: (error, variables, context) => {
      // 失败时回滚
      context.previousPoliciesList.forEach(([queryKey, previousData]) => {
        queryClient.setQueryData(queryKey, previousData);
      });
    },
  });
}
```

#### 验证步骤

1. **打开 PoliciesPage**
   - 导航到 `/settings/policies` 页面
   - 观察策略列表加载

2. **测试成功的乐观更新**
   - 点击任意策略的 **激活/停用** 按钮
   - 观察 DevTools：
     - 策略列表数据**立即更新**（在按钮点击时）
     - 查看 **Data** 标签，状态字段已变化
     - 查看 **Last Updated** 时间戳更新了两次：
       1. 第一次：乐观更新（onMutate）
       2. 第二次：服务器响应（onSuccess）

3. **测试失败的回滚**（需要模拟错误）
   - 方法1：关闭后端服务器
   - 方法2：在浏览器 DevTools Network 标签中使用 "Offline" 模式
   - 点击 **激活/停用** 按钮
   - 观察：
     - UI 立即更新（乐观更新）
     - 几秒后显示错误提示
     - DevTools 中数据**自动回滚**到原始状态

4. **观察编辑模态框**
   - 点击策略的 **编辑** 按钮
   - 修改状态后点击 **保存**
   - 观察提示："💡 提示: 点击'保存'后，表格会立即更新（乐观更新）。如果保存失败，将自动回滚到原始状态。"
   - 验证行为符合提示

#### 预期结果

✅ **成功场景**：UI 立即更新 → 服务器确认 → 保持更新
✅ **失败场景**：UI 立即更新 → 服务器错误 → 自动回滚 → 显示错误消息

## DevTools 快捷键

- **`Ctrl + Shift + D`**（或 `Cmd + Shift + D` on Mac）- 切换 DevTools 显示/隐藏
- 可在 DevTools 面板右上角点击 **齿轮图标** 配置更多选项

## 调试技巧

### 1. 监控查询键设计

确保不同的筛选条件生成不同的查询键：

```typescript
// 不同的筛选条件 = 不同的缓存
['releases', { status: ['deployed'] }, 1, 20]
['releases', { status: ['pending'] }, 1, 20]
['releases', undefined, 1, 20]
```

在 DevTools 中应该看到多个查询实例。

### 2. 检查缓存命中

- 切换页面时，观察是否有不必要的网络请求
- 如果看到重复请求，检查查询键是否一致

### 3. 调试乐观更新

- 使用浏览器 DevTools 的 **Network** 标签配合 React Query DevTools
- 勾选 "Slow 3G" 模拟慢速网络，观察乐观更新和服务器响应之间的延迟

### 4. 查看查询依赖

- 在 DevTools 中点击查询
- 查看 **Observers** 数量，确认组件订阅情况
- 如果 Observers = 0，说明没有组件使用此查询（可能是 bug）

## 生产环境

DevTools **仅在开发环境中显示**。在生产构建中，DevTools 组件会被自动移除（tree-shaking），不会影响包大小和性能。

验证方式：

```bash
npm run build
# 检查生成的 bundle，不应包含 devtools 代码
```

## 常见问题

### Q1: DevTools 图标不显示

**解决方案**：
- 确认是开发模式（`npm run dev`）
- 检查浏览器控制台是否有错误
- 确认 `@tanstack/react-query-devtools` 已安装

### Q2: 查询显示为灰色（inactive）

**原因**：查询未被任何组件订阅

**解决方案**：
- 检查组件是否正确使用了 hook（如 `useReleases()`）
- 确认组件已渲染

### Q3: 缓存没有按预期工作

**排查步骤**：
1. 检查 `queryClient.ts` 中的 `staleTime` 和 `gcTime` 配置
2. 确认查询键是否一致
3. 在 DevTools 中手动点击 **Invalidate** 强制刷新

## 相关文件

- **配置**: `src/main/frontend/react-app/config/queryClient.ts`
- **Hooks**: `src/main/frontend/react-app/hooks/use*.ts`
- **页面**: `src/main/frontend/react-app/pages/*Page.tsx`
- **测试**: `src/main/frontend/tests/hooks/*.test.tsx`

## 下一步

完成 T069 和 T070 手动测试后，可以继续：

- **Phase 7**: 国际化（i18n）实施
- **Phase 8**: 最终优化和性能调优

---

**更新时间**: 2025-01-30
**相关任务**: T069, T070 (Phase 6 手动测试)
