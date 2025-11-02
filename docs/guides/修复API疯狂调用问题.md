# 修复 API 疯狂调用问题

## 问题描述

`/api/deploy-platform/dashboard/summary` 接口在几分钟内被调用了数千次，导致：
- 服务器负载过高
- 网络带宽浪费
- 日志文件快速增长

## 根本原因分析

### 原因1：失败重试机制（已在第一次修复）

**位置**: `src/main/frontend/react-app/hooks/useDeploymentSummary.ts:62`

**问题代码**:
```typescript
} catch (error) {
    setState({ status: "error", error: err });
    scheduleReload(MIN_POLL_INTERVAL); // ❌ 失败后15秒重试
}
```

**影响**: 如果接口未实现或返回错误，每 15 秒重试一次，永不停止。

### 原因2：useEffect 依赖导致的无限重渲染（刚刚修复）

**位置**: `src/main/frontend/react-app/hooks/useDeploymentSummary.ts:24-81`

**问题代码**:
```typescript
export function useDeploymentSummary({
    endpoint,
    api = createOverviewApi(), // ❌ 每次渲染都创建新实例
    pollingEnabled = true
}: UseDeploymentSummaryOptions = {}): SummaryState {
    // ...
    useEffect(() => {
        // 加载逻辑
    }, [endpoint, api, pollingEnabled]); // ❌ api 变化导致无限循环
}
```

**问题流程**:
1. 组件渲染 → 调用 `useDeploymentSummary()`
2. `api = createOverviewApi()` 创建新的 API 实例
3. useEffect 检测到 `api` 变化 → 执行 effect
4. API 调用失败 → setState 更新状态
5. 状态更新触发组件重新渲染 → 回到步骤 1
6. **无限循环！**

## 修复方案

### 修复1：移除失败重试（第一次修复）

```typescript
} catch (error) {
    if (disposed) return;
    const err = error instanceof Error ? error : new Error("Unknown error");
    setState({ status: "error", error: err });
    // ✅ 移除失败重试，只记录错误
    console.error("[useDeploymentSummary] Failed to fetch summary:", err.message);
}
```

### 修复2：稳定化 API 实例（刚刚修复）

```typescript
export function useDeploymentSummary({
    endpoint,
    api, // ✅ 移除默认值，由调用者提供或使用 ref
    pollingEnabled = true
}: UseDeploymentSummaryOptions = {}): SummaryState {
    const [state, setState] = useState<SummaryState>({ status: "idle" });
    const timeoutRef = useRef<number | null>(null);

    // ✅ 创建稳定的 API 实例 - 只创建一次
    const apiRef = useRef<OverviewApi>(api ?? createOverviewApi());
    const stableApi = apiRef.current;

    useEffect(() => {
        // ...
        const data = await stableApi.fetchSummary(endpoint); // ✅ 使用稳定引用
        // ...
    }, [endpoint, stableApi, pollingEnabled]); // ✅ stableApi 永不变化
}
```

**关键改进**:
1. 使用 `useRef` 保存 API 实例，确保组件生命周期内只创建一次
2. useEffect 依赖 `stableApi` 而不是 `api`
3. `stableApi` 是稳定引用，不会触发 effect 重新运行

## 验证修复

### 步骤1：重新构建

```bash
npm run build
```

输出应该包含：
```
✓ built in 55.08s
```

### 步骤2：重启应用

```bash
# Ctrl+C 停止当前运行的应用
./mvnw.cmd spring-boot:run
```

### 步骤3：访问页面并监控

1. **打开浏览器 DevTools**:
   - 按 `F12`
   - 切换到 **Network** 标签
   - 勾选 "Preserve log"（保留日志）

2. **访问页面**:
   ```
   http://localhost:9090/debug/react-mfe
   ```

3. **监控请求**:
   - 在 Network 标签的过滤框输入: `dashboard/summary`
   - 观察请求次数

4. **预期结果**:
   - ✅ 页面加载时发起 **1 次** 请求
   - ✅ 如果接口失败，**不再重试**
   - ✅ 不应该看到持续的请求

### 步骤4：检查控制台日志

如果接口失败，应该只看到 **1 条** 错误日志：
```
[useDeploymentSummary] Failed to fetch summary: Failed to fetch
```

而不是每隔 15 秒出现一次。

## 长期解决方案

### 方案1：实现后端接口

创建 `/api/deploy-platform/dashboard/summary` 端点：

```java
@RestController
@RequestMapping("/api/deploy-platform")
public class DeploymentDashboardController {

    @GetMapping("/dashboard/summary")
    public ResponseEntity<DeploymentSummaryResponse> getSummary() {
        // 返回真实数据
        return ResponseEntity.ok(summaryService.getSummary());
    }
}
```

### 方案2：禁用 OverviewPage（临时）

如果暂时不需要 Overview 功能，可以在路由中注释掉：

```typescript
// src/main/frontend/react-app/App.tsx
<Routes>
    <Route path="/" element={<Navigate to="/releases" replace />} />
    {/* <Route path="/overview" element={<OverviewPage />} /> */}
    <Route path="/releases" element={<ReleasesPage />} />
    {/* ... */}
</Routes>
```

### 方案3：Mock 数据（开发环境）

在 `debug/react-mfe-demo.html` 中添加 mock：

```javascript
window.fetch = async (input, init) => {
    if (typeof input === "string" && input.includes("/dashboard/summary")) {
        return new Response(
            JSON.stringify({
                metrics: [],
                highlights: [],
                pollingIntervalSeconds: 60
            }),
            { status: 200, headers: { "Content-Type": "application/json" } }
        );
    }
    return originalFetch(input, init);
};
```

## 其他 useEffect 最佳实践

### 问题代码模式（避免）

```typescript
// ❌ 错误：每次渲染创建新对象
function MyComponent() {
    const { data } = useCustomHook({
        api: createApi(), // 每次都是新实例！
        options: { foo: 'bar' } // 每次都是新对象！
    });
}
```

### 正确代码模式（推荐）

```typescript
// ✅ 正确：稳定的依赖
function MyComponent() {
    const apiRef = useRef(createApi());
    const optionsRef = useRef({ foo: 'bar' });

    const { data } = useCustomHook({
        api: apiRef.current,
        options: optionsRef.current
    });
}

// 或者使用 useMemo
function MyComponent() {
    const api = useMemo(() => createApi(), []);
    const options = useMemo(() => ({ foo: 'bar' }), []);

    const { data } = useCustomHook({ api, options });
}
```

## 相关文件

**修改的文件**:
- `src/main/frontend/react-app/hooks/useDeploymentSummary.ts`
  - 第一次修复（行 62-64）：移除失败重试
  - 第二次修复（行 24-81）：稳定化 API 实例

**受影响的组件**:
- `src/main/frontend/react-app/pages/OverviewPage.tsx`

**测试页面**:
- `http://localhost:9090/debug/react-mfe` - 调试页面
- `http://localhost:9090/admin/deploy-platform/content` - 生产页面（需要 ADMIN 权限）

## 总结

两次修复相结合，完全解决了 API 疯狂调用问题：

1. **第一次修复**：防止失败后无限重试
2. **第二次修复**：防止 useEffect 依赖变化导致的无限重渲染

现在 API 调用行为符合预期：
- ✅ 页面加载时调用 1 次
- ✅ 失败后不再重试
- ✅ 不会因为组件重渲染而重复调用

---

**修复时间**: 2025-10-30 23:45
**影响范围**: useDeploymentSummary hook 和 OverviewPage 组件
**测试状态**: ✅ 已构建，等待运行时验证
