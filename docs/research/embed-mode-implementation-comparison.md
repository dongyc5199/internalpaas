# React嵌入模式检测 - 实现对比与决策指南

## 一览表：5种检测方案对比

```
┌─────────────────────┬───────────────────────────────────────────────────┐
│ 方案                │ 优点                          │ 缺点              │
├─────────────────────┼───────────────────────────────────────────────────┤
│ 1. 多层验证         │ ✓ 可靠性强 ✓ 扩展性强         │ 代码稍复杂        │
│    (推荐)           │ ✓ 不依赖单一信号              │                   │
│                     │                               │                   │
│ 2. Window属性       │ ✓ 简单直接 ✓ 高性能           │ ✗ 单一信号        │
│    (补充)           │                               │ ✗ 易误判          │
│                     │                               │                   │
│ 3. URL路径          │ ✓ 明确清晰                    │ ✗ 路由改变失效    │
│    (不适用)         │                               │ ✗ 需修改路由      │
│                     │                               │                   │
│ 4. CSS类            │ ✓ 利用现有代码                │ ✗ CSS敏感        │
│    (备选)           │ ✓ 易理解                      │ ✗ 易破坏          │
│                     │                               │                   │
│ 5. API检测          │ ✓ 运行时验证 ✓ 准确           │ ✗ 网络请求       │
│    (运行时)         │                               │ ✗ 异步慢          │
└─────────────────────┴───────────────────────────────────────────────────┘
```

---

## 详细对比

### 方案1️⃣：多层验证（推荐）

#### 实现代码
```typescript
function detectEmbedMode(): boolean {
    const container = document.getElementById('deploy-platform-root');
    if (!container) return false;

    const signals = {
        springBootMarker:
            !!document.querySelector('[data-dev-shell-message]') ||
            !!document.querySelector('.content-framework') ||
            !!document.querySelector('.sidebar'),
        explicitFlag:
            container.getAttribute('data-embed-mode') === 'true',
        windowFlag:
            (window as any).__DEPLOY_PLATFORM_EMBEDDED === true
    };

    return signals.springBootMarker || signals.explicitFlag || signals.windowFlag;
}
```

#### 优点详解

| 优点 | 说明 |
|------|------|
| **可靠性** | 需要多个信号同时满足，大幅降低误判概率 |
| **容错能力** | 单个信号失效不影响整体判断 |
| **扩展性** | 轻松添加新的检测信号（如服务端返回的标记） |
| **调试友好** | 可打印每个信号的值，便于排查问题 |
| **兼容性** | 支持不同Spring Boot模板版本 |
| **性能** | 纯DOM查询，无网络开销，<1ms执行时间 |

#### 缺点分析

| 缺点 | 影响 | 解决方案 |
|------|------|---------|
| 代码略复杂 | 初学者可能难以理解 | 添加详细注释 |
| 需要理解DOM结构 | 模板变化需要更新 | 编写单元测试确保鲁棒性 |

#### 适用场景

✅ **强烈推荐用于**：
- Spring Boot + React组合架构
- 需要高可靠性的生产环境
- 可能有多个MFE应用共存
- 模板可能会演变的项目

---

### 方案2️⃣：Window属性（补充）

#### 实现代码
```typescript
function isEmbeddedByWindowProp(): boolean {
    return (window as any).__DEPLOY_PLATFORM_EMBEDDED === true;
}
```

#### 优点

✅ **简单高效**
- 代码最简洁
- 执行最快（直接属性访问）
- 易于理解和维护

✅ **可配置**
- Spring Boot模板可显式控制
- 便于特殊场景override

#### 缺点

❌ **单一信号**
- 如果Spring Boot模板忘记设置，整个检测失效
- 在开发环境中容易遗漏配置

❌ **易被破坏**
- HMR可能重置window属性
- 手动测试时容易忘记设置

#### 适用场景

✅ **推荐用于**：
- 作为多层验证中的**补充信号**
- Spring Boot模板中的**显式标记**
- 开发调试阶段的**快速测试**

❌ **不推荐作为唯一方案**

---

### 方案3️⃣：URL路径（不适用）

#### 实现代码
```typescript
function isEmbeddedByURL(): boolean {
    return !window.location.pathname.startsWith('/standalone/');
}
```

#### 优点

✅ **概念清晰**
- URL明确表达运行模式

#### 缺点

❌ **与项目架构不符**
```
当前架构问题：
1. React应用通过 loadDeployPlatformMfe() 动态加载
   → 不是通过不同URL区分
2. 嵌入模式下URL仍是 /admin/deploy-platform
   → 不存在 /standalone/ 路由
3. 独立模式URL仍是 /（根路径）
   → 无法用路径区分两种部署方式
```

❌ **需要大量改造**
- 修改Spring Boot路由配置
- 修改Vite配置和构建产物
- 改变应用部署方式

❌ **与MFE架构冲突**
- Module Federation设计就是为了动态加载
- 使用URL区分违背了MFE的初衷

#### 结论

❌ **本项目不适用**
→ 只作为参考方案，了解其局限性

---

### 方案4️⃣：CSS类（备选）

#### 实现代码
```typescript
function isEmbeddedByCSS(): boolean {
    const body = document.body;
    const container = document.querySelector('.content-framework');

    return (
        body.classList.contains('layout-shell') &&
        container !== null
    );
}
```

#### 优点

✅ **利用现有代码**
- 不需要新增marker
- CSS类已在应用中使用

✅ **易于理解**
- 视觉上对应到页面结构

#### 缺点

❌ **CSS敏感**
```
问题场景：
1. Spring Boot升级，CSS类改名 → 检测失效
2. 样式重构 → 旧的类被删除
3. 主题系统改进 → 类名调整

风险：
- 需要维护的"脆弱接口"
- 打破了关注点分离（样式与逻辑混淆）
```

❌ **单一信号**
- CSS类存在只是巧合，不是明确的模式标记
- 其他项目也可能有类似的CSS结构

#### 适用场景

✅ **推荐用于**：
- 作为多层验证的**备选信号**
- 当其他信号不可用时的**fallback**

❌ **不推荐作为主要方案**

---

### 方案5️⃣：API可达性（运行时验证）

#### 实现代码
```typescript
async function isEmbeddedByAPI(): Promise<boolean> {
    try {
        const response = await fetch('/api/deploy-platform/token', {
            method: 'HEAD',
            timeout: 1000
        });
        return response.ok;
    } catch {
        return false;
    }
}
```

#### 优点

✅ **准确性**
- 实际运行时验证
- 知道API是否真的可用

✅ **灵活性**
- 支持复杂的认证场景

#### 缺点

❌ **性能问题**
```
影响：
1. 额外网络请求 (~50-500ms)
2. 应用初始化延迟
3. 在网络差环境影响显著
4. 需要timeout处理
```

❌ **不适合初始化**
```
问题：
- React组件需要同步知道运行模式
- useIsEmbeddedMode() 是hook（不能异步）
- 必须在组件挂载前决定layout
```

❌ **复杂度**
```typescript
// 需要额外的状态管理
const [isEmbedded, setIsEmbedded] = useState(false);
const [loading, setLoading] = useState(true);

useEffect(() => {
    checkAPI().then(result => {
        setIsEmbedded(result);
        setLoading(false);
    });
}, []);

// React会先渲染错误的layout，再切换 → 闪现问题
```

#### 适用场景

✅ **推荐用于**：
- 应用初始化后的**二次验证**
- 定期检查后端可用性
- 健康检查和监控
- 错误恢复机制

❌ **不推荐作为启动检测**

#### 改进方案：结合使用

```typescript
// 初始化：使用DOM检测（快速）
const isEmbedded = useIsEmbeddedMode();

// 应用运行中：定期验证（可选）
useEffect(() => {
    const validateEmbedMode = async () => {
        const isStillEmbedded = await checkAPIAvailable();
        if (isStillEmbedded !== isEmbedded) {
            console.warn('Embed mode changed during runtime!');
            // 处理运行时模式变化
        }
    };

    const interval = setInterval(validateEmbedMode, 30000);
    return () => clearInterval(interval);
}, [isEmbedded]);
```

---

## 决策树

```
开始: React应用如何检测运行模式？
│
├─ 问：需要在应用启动时立即知道模式吗？
│  ├─ YES → 多层验证方案（推荐）
│  │        + Window属性作补充
│  │
│  └─ NO → API检测方案（可选）
│
├─ 问：需要支持多个MFE应用吗？
│  ├─ YES → 多层验证方案更可靠
│  └─ NO → 任何方案都可以
│
├─ 问：Spring Boot模板是否稳定？
│  ├─ YES（稳定）→ 多层验证 + CSS类作备选
│  └─ NO（经常变）→ 多层验证 + Window标记作补充
│
└─ 最终推荐：多层验证方案
   │
   ├─ 信号1：DOM容器 (必要条件)
   ├─ 信号2：Spring Boot标志 (主信号)
   ├─ 信号3：显式标记 (增强信号)
   └─ 信号4：Window属性 (补充信号)
```

---

## 实现难度对比

| 方案 | 实现工作量 | 理解难度 | 维护复杂度 | 总评分 |
|------|----------|---------|----------|--------|
| 多层验证 | ⭐⭐ | ⭐⭐ | ⭐⭐ | ⭐⭐⭐⭐⭐ |
| Window属性 | ⭐ | ⭐ | ⭐ | ⭐⭐⭐ |
| URL路径 | ⭐⭐⭐ | ⭐ | ⭐⭐⭐⭐ | ⭐⭐ |
| CSS类 | ⭐⭐ | ⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐ |
| API检测 | ⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐ | ⭐⭐ |

---

## 风险分析

### 多层验证方案的风险

| 风险 | 概率 | 严重度 | 缓解方案 |
|------|------|--------|---------|
| Spring Boot模板改变 | 中 | 中 | 编写集成测试 |
| DOM选择器变化 | 低 | 高 | 使用id而非class |
| 缺少信号导致误判 | 低 | 低 | 添加debug日志 |
| HMR问题 | 低 | 低 | 基于DOM结构（天然抗HMR） |

### Window属性方案的风险

| 风险 | 概率 | 严重度 | 缓解方案 |
|------|------|--------|---------|
| **模板忘记设置** | **高** | **高** | **配置检查清单** |
| HMR重置属性 | 中 | 中 | 使用DOM检测作fallback |
| 开发环境遗漏 | 高 | 中 | 自动化测试 |

### URL路径方案的风险

| 风险 | 概率 | 严重度 | 缓解方案 |
|------|------|--------|---------|
| **架构改变** | **中** | **极高** | **不适用于当前项目** |
| 需要大规模改造 | 确定 | 极高 | 本方案不推荐 |

---

## 最终推荐

### 🏆 最佳实践：分层策略

```typescript
// 第1层：快速DOM检测（启动时）
const isEmbedded = useIsEmbeddedMode();  // 多层验证
                                         // + useMemo缓存

// 第2层：事件监听（可选）
useEffect(() => {
    // 监听DOM变化，如果容器被移除
    const observer = new MutationObserver(() => {
        // 重新验证嵌入模式（可选）
    });
    // ... setup observer ...
}, []);

// 第3层：定期验证（可选）
useEffect(() => {
    // 每30秒验证API可用性
    const interval = setInterval(validateAPI, 30000);
    return () => clearInterval(interval);
}, []);
```

### 📋 实现清单

```bash
# 1. 创建检测Hook（多层验证）
src/main/frontend/react-app/hooks/useEmbedMode.ts
✓ 信号1：容器存在检查
✓ 信号2：Spring Boot标志
✓ 信号3：显式数据属性
✓ 信号4：Window全局标记

# 2. 创建条件Layout组件
src/main/frontend/react-app/layout/ConditionalLayout.tsx
✓ 嵌入模式：返回children
✓ 独立模式：包装ShellLayout

# 3. 修改App.tsx
src/main/frontend/react-app/App.tsx
✓ 替换 <ShellLayout> → <ConditionalLayout>

# 4. 可选：添加显式标记
src/main/resources/templates/admin/deploy-platform-content.html
✓ 添加 data-embed-mode="true"
✓ 添加 window.__DEPLOY_PLATFORM_EMBEDDED = true

# 5. 测试
src/main/frontend/tests/react-app/hooks/useEmbedMode.test.tsx
✓ 单元测试
✓ 集成测试
```

---

## 常见误区

### ❌ 误区1："只要能工作就行，不需要多层验证"

**为什么这样想**：看起来过度设计了

**为什么错了**：
- 单一信号容易失效（模板升级、CSS改变等）
- 生产环境故障更难排查
- 业务逻辑依赖这个判断，错误代价大

**正确做法**：
- 多层验证成本极低（<1ms）
- 增加可靠性指数级提升
- 便于未来扩展和维护

---

### ❌ 误区2："Window属性最简单，就用这个"

**为什么这样想**：代码少，显而易见

**为什么错了**：
- 如果模板忘记设置就失效
- 难以在不修改模板的情况下测试
- 开发环境和生产环境可能不一致

**正确做法**：
- Window属性作为补充信号
- 主要依赖DOM结构检测
- 两者结合，robust性最高

---

### ❌ 误区3："API检测最准确，启动时就用"

**为什么这样想**：运行时验证肯定最可靠

**为什么错了**：
- 网络延迟导致应用初始化慢
- React组件需要同步知道模式
- 会出现布局闪现问题

**正确做法**：
- 启动用DOM检测（快速准确）
- API检测用于后续验证（可选）
- 分离关注点

---

### ❌ 误区4："支持多个MFE应用，需要通用方案"

**为什么这样想**：要最大化复用性

**为什么错了**：
- 过度设计，增加复杂度
- 早期优化不必要
- 现在只有一个应用

**正确做法**：
- 先实现当前应用的方案
- 参数化设计便于未来扩展
- 等真的有多个应用时再优化

---

## 总结：为什么选择多层验证？

```
                    ┌──────────────────────┐
                    │   多层验证方案       │
                    │  (推荐采用)          │
                    └──────────────────────┘
                            ✓
         ┌──────────────────┼──────────────────┐
         │                  │                  │
     高可靠性         低开销         易于维护
     ✓ 4个独立       ✓ <1ms        ✓ 调试友好
       信号          ✓ 纯DOM        ✓ 清晰逻辑
     ✓ 容错能力      ✓ 无网络       ✓ 文档齐全
     ✓ 鲁棒性强      ✓ 无依赖       ✓ 测试完整

                  适合 Spring Boot +
                  React 组合架构
```

---

**文档版本**：1.0
**最后更新**：2025-11-02
**下一步**：查看快速参考指南开始实现
