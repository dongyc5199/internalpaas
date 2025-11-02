# 导航同步可视化与事件流程图

## 1. 完整事件流程

### 1.1 场景1：用户点击侧边栏导航

```
┌─────────────────────────────────────────────────────────────┐
│ User clicks sidebar item → /releases                        │
└────────────────────┬────────────────────────────────────────┘
                     │
        ┌────────────▼──────────────┐
        │   Main App                │
        │ ─────────────────────────  │
        │                           │
        │  initNavigationBridge()   │
        │  Sidebar link click       │
        │  event listener fired     │
        │                           │
        └────────────┬──────────────┘
                     │
                     │ NavigationBridge.notifyReactNavigation('/releases')
                     │
        ┌────────────▼──────────────────────────────┐
        │ shouldProcess('app', '/releases')        │
        │  ├─ isSyncing? false ✓                   │
        │  ├─ path different? true ✓               │
        │  ├─ source different? true ✓             │
        │  └─ → return true                        │
        └────────────┬──────────────────────────────┘
                     │
                     │ beginSync('app', '/releases')
                     │ isSyncing = true
                     │ lastEventSource = 'app'
                     │
        ┌────────────▼──────────────────────────────┐
        │ Check: isReactReady?                      │
        │  ├─ Yes? Call ReactAppBridge.navigate()  │
        │  └─ No? Queue navigation                 │
        └────────────┬──────────────────────────────┘
                     │
                     │ window.ReactAppBridge.navigate('/releases')
                     │
        ┌────────────▼──────────────────────────────┐
        │  React App                                │
        │  ──────────────────────────────────────   │
        │                                           │
        │  ReactAppBridge.navigate('/releases')    │
        │  ├─ isSyncingRef.current = true          │
        │  └─ navigate('/releases') [React Router] │
        │                                           │
        │  React Router更新location.pathname       │
        │                                           │
        └────────────┬──────────────────────────────┘
                     │
                     │ useNavigationBridge Hook发现路由改变
                     │
        ┌────────────▼──────────────────────────────┐
        │ useEffect(() => {                        │
        │   if (isSyncingRef.current) {            │
        │     // 这是由React Bridge触发的           │
        │     isSyncingRef.current = false         │
        │     navigationBridge.endSync()           │
        │     return                               │
        │   }                                      │
        │   // 如果是用户直接操作，则通知主应用   │
        │ }, [location.pathname])                  │
        └────────────┬──────────────────────────────┘
                     │
                     │ 不发送回主应用 (避免循环)
                     │
        ┌────────────▼──────────────────────────────┐
        │ Main App sync timeout (300ms)             │
        │ isSyncing = false                         │
        │ lastEventSource = null                    │
        │ ✓ 状态复位，准备下一次导航              │
        └──────────────────────────────────────────┘
```

**时间轴：**
```
T0ms    : 用户点击侧边栏
T0-5ms  : NavigationBridge处理，准备React导航
T5-10ms : React.navigate() 调用
T10-50ms: React Router更新location
T50-80ms: useNavigationBridge Hook检查状态
T80-300ms: 等待sync超时
T300ms  : 同步完成，状态复位
```

---

### 1.2 场景2：React应用内部导航

```
┌────────────────────────────────────────────┐
│  React App                                 │
│  用户在React应用内导航 (NavLink)          │
│  React Router更新 location.pathname        │
└────────────┬───────────────────────────────┘
             │
             │ useNavigationBridge Hook检测到变化
             │
┌────────────▼────────────────────────────────────┐
│  useEffect(() => {                             │
│    if (isSyncingRef.current) {                 │
│      // 由React Bridge触发的导航              │
│      isSyncingRef.current = false              │
│      navigationBridge.endSync()               │
│      return                                   │
│    }                                          │
│                                              │
│    if (path === lastPath) return              │
│                                              │
│    // 用户在React内直接导航，需要通知主应用  │
│    navigationBridge.navigateFromReact(path)  │
│  }, [location.pathname])                     │
└────────────┬────────────────────────────────────┘
             │
             │ navigationBridge.navigateFromReact('/policies')
             │
┌────────────▼────────────────────────────────────┐
│ shouldProcess('react', '/policies')           │
│  ├─ isSyncing? false ✓                        │
│  ├─ path different? true ✓                    │
│  ├─ source different? true ✓                  │
│  └─ → return true                            │
└────────────┬────────────────────────────────────┘
             │
             │ beginSync('react', '/policies')
             │
┌────────────▼────────────────────────────────────┐
│ 通知主应用 (postMessage)                       │
│ {                                             │
│   type: 'navigation:from-react',             │
│   path: '/policies',                         │
│   timestamp: Date.now()                      │
│ }                                            │
└────────────┬────────────────────────────────────┘
             │
┌────────────▼──────────────────────────────┐
│  Main App (message listener)              │
│  ────────────────────────────────────────  │
│                                           │
│  if (event.data?.type === 'navigation:   │
│      from-react') {                      │
│                                          │
│    if (isSyncing) return  // 忽略        │
│                                          │
│    updateSidebarNavigation(path)         │
│    // 仅更新UI，不加载内容               │
│    // (React已负责显示内容)              │
│  }                                       │
└────────────┬──────────────────────────────┘
             │
┌────────────▼──────────────────────────────┐
│  侧边栏 NavLink视觉高亮更新               │
│  ✓ 导航同步完成                           │
└──────────────────────────────────────────┘
```

**关键点：**
- 只有React内部导航才会通知主应用
- 主应用收到通知后，只更新侧边栏，不加载内容
- 避免了不必要的重复加载

---

## 2. 防止循环触发的状态机

```
                    ┌──────────────────┐
                    │   READY STATE    │
                    │  isSyncing=false │
                    └──────┬───────────┘
                           │
                    ┌──────┴─────────┐
                    │               │
        ┌───────────▼────┐   ┌──────▼───────────┐
        │ App Event      │   │ React Event      │
        │ 主app导航      │   │ React导航        │
        └───────┬────────┘   └──────┬───────────┘
                │                   │
        ┌───────▼──────────────────┬┴──────────────┐
        │                          │               │
        │                    ┌─────▼────────────┐  │
        │                    │  SYNCING STATE   │  │
        │                    │ isSyncing=true   │  │
        │                    │ lastSource='app' │  │
        │                    │ lastPath='/x'    │  │
        │                    └─────┬────────────┘  │
        │                          │               │
        │       ┌──────────────────┴─────────────┐ │
        │       │                                │ │
        │  ┌────▼────────────────┐  ┌───────────▼─┴─────┐
        │  │ Event from React    │  │ Event from App   │
        │  │ shouldProcess=false │  │ (same source)    │
        │  │ (same source as     │  │ shouldProcess=   │
        │  │  stored 'app')      │  │ false            │
        │  │ → IGNORED           │  │ → IGNORED        │
        │  └───────┬─────────────┘  └──────┬───────────┘
        │          │                       │
        │          └──────────┬────────────┘
        │                     │
        │       ┌─────────────▼──────────┐
        │       │  SYNC_TIMEOUT (300ms)  │
        │       │  reset state           │
        │       └─────────────┬──────────┘
        │                     │
        └─────────────┬───────┘
                      │
                ┌─────▼──────────┐
                │  READY STATE   │
                │ (back to start) │
                └────────────────┘
```

**状态机规则：**

| 状态 | 条件 | 动作 | 下一状态 |
|-----|------|------|---------|
| READY | 收到App事件 | beginSync('app') | SYNCING |
| READY | 收到React事件 | beginSync('react') | SYNCING |
| SYNCING | 相同源事件 | ignore | SYNCING |
| SYNCING | 不同源事件 | endSync | READY |
| SYNCING | 超时(300ms) | reset | READY |

---

## 3. 浏览器历史记录管理流程

```
┌──────────────────────────────────────┐
│  用户点击侧边栏 → /releases          │
└────────────┬─────────────────────────┘
             │
┌────────────▼─────────────────────────┐
│  Main App不直接改变URL               │
│  而是通过React Router来管理          │
└────────────┬─────────────────────────┘
             │
┌────────────▼─────────────────────────┐
│  window.ReactAppBridge.navigate()    │
│  调用React Router的navigate()         │
└────────────┬─────────────────────────┘
             │
┌────────────▼─────────────────────────────────────────┐
│  React Router内部：                                  │
│  navigate() → window.history.pushState()             │
│  location.pathname改变                              │
│                                                    │
│  BrowserRouter自动：                                │
│  window.history.pushState(                          │
│    { path: '/releases' },                          │
│    '',                                             │
│    '/releases'                                     │
│  )                                                 │
└────────────┬─────────────────────────────────────────┘
             │
┌────────────▼─────────────────────────┐
│  useNavigationBridge Hook检测        │
│  location.pathname变化               │
│  发送 'navigation:history-update'    │
│  给主应用                            │
└────────────┬─────────────────────────┘
             │
┌────────────▼─────────────────────────────────────────┐
│  Main App收到消息：                                 │
│  if (history.state?.path !== current) {             │
│    history.pushState(                              │
│      { path: '/releases' },                        │
│      '',                                           │
│      '/releases'                                   │
│    )                                               │
│  }                                                 │
│  这是冗余的检查，保证历史一致性                      │
└─────────────────────────────────────┘
```

**历史记录:**
```
用户操作序列：
1. 初始: /                      → history: [/]
2. 点击/overview               → history: [/, /overview]
3. 点击/releases               → history: [/, /overview, /releases]
4. 点击浏览器Back              → popstate事件
                              → history: [/, /overview]
                              → React navigate('/overview', {replace:true})
5. 继续Back                    → history: [/]
```

---

## 4. React未初始化时的导航队列

```
┌────────────────────────────────────────┐
│  Page Load                             │
│  Main App script loads                 │
│  React bundle downloading...          │
└────────────┬───────────────────────────┘
             │
┌────────────▼────────────────────────────────┐
│  用户点击侧边栏 → /overview                │
│  React未就绪                               │
└────────────┬────────────────────────────────┘
             │
┌────────────▼────────────────────────────────┐
│  NavigationBridge.notifyReactNavigation()   │
│  检查: isReactReady?                        │
│    NO → 加入队列 (pendingNavigationPath)  │
│  状态: ['/overview']                       │
└────────────┬────────────────────────────────┘
             │
      [等待React加载]
             │
┌────────────▼────────────────────────────────┐
│  React bundle完成                          │
│  React App挂载                             │
│  useNavigationBridge Hook初始化            │
└────────────┬────────────────────────────────┘
             │
┌────────────▼────────────────────────────────────────┐
│  发送 'deploy-platform:ready' 消息                 │
│  Main App监听器接收                                │
│  isReactReady = true                              │
│  检查: pendingNavigationPath?                      │
│    YES → processQueue()                          │
└────────────┬────────────────────────────────────────┘
             │
┌────────────▼────────────────────────────────┐
│  处理队列中的导航                          │
│  调用 ReactAppBridge.navigate('/overview') │
│  React Router更新location                  │
│  ✓ 导航完成                                │
└────────────────────────────────────────────┘
```

**队列处理时间轴：**
```
T0-100ms  : React加载中，用户点击导航1,2,3
           → 队列: [nav1, nav2, nav3]

T200ms    : React就绪，发送ready信号
           → Main App接收ready

T210ms    : 处理队列
           → nav1执行
           → nav2执行
           → nav3执行

T300ms    : 所有导航完成
           → 队列清空，状态: ['/nav3path']
```

---

## 5. 完整的双向同步时间线

```
USER → MAIN APP → REACT → MAIN APP [反馈] → READY

过程步骤化示意：

T0ms    ┌─────────────────────────────┐
        │ 用户点击 /releases 侧边栏   │
        └──────────────┬──────────────┘
                       │
T5ms    ┌──────────────▼──────────────┐
        │ Main App:                   │
        │ NavigationBridge.notify()   │
        │ isSyncing=true              │
        └──────────────┬──────────────┘
                       │
T10ms   ┌──────────────▼──────────────────┐
        │ React App:                      │
        │ ReactAppBridge.navigate()      │
        │ navigate('/releases')          │
        │ isSyncingRef=true              │
        └──────────────┬──────────────────┘
                       │
T20ms   ┌──────────────▼──────────────────┐
        │ React Router:                   │
        │ location.pathname → /releases   │
        │ component re-render             │
        └──────────────┬──────────────────┘
                       │
T50ms   ┌──────────────▼──────────────────┐
        │ useNavigationBridge:            │
        │ location change detected        │
        │ isSyncingRef=true →             │
        │ isSyncingRef=false (reset)      │
        │ don't notify parent             │
        └──────────────┬──────────────────┘
                       │
T100ms  ┌──────────────▼──────────────────┐
        │ Main App:                       │
        │ Sync timeout triggered          │
        │ isSyncing=false (reset)         │
        │ lastEventSource=null            │
        └──────────────┬──────────────────┘
                       │
        ┌──────────────▼──────────────────┐
        │ READY for next navigation       │
        └─────────────────────────────────┘

关键时间点：
• T0-T10    : 事件传播延迟
• T10-T50   : React更新延迟
• T50-T100  : 状态同步延迟
• 总耗时    : ~100ms
```

---

## 6. 错误恢复流程

```
异常情况处理链：

┌─────────────────────────────┐
│ 异常场景检测                 │
└────────────┬────────────────┘
             │
      ┌──────┴──────┬────────┬─────────┐
      │             │        │         │
  ┌───▼──┐   ┌─────▼──┐ ┌──▼───┐ ┌──▼───┐
  │React │   │Sync    │ │Loop  │ │Race  │
  │未加  │   │卡死    │ │触发  │ │条件  │
  │载    │   │        │ │      │ │      │
  └───┬──┘   └─────┬──┘ └──┬───┘ └──┬───┘
      │            │       │        │
  ┌───▼────┐  ┌────▼─────┐ │   ┌────▼────┐
  │加入    │  │超时重置  │ │   │差异    │
  │队列    │  │300ms    │ │   │检查    │
  │等待    │  │         │ │   │        │
  └────────┘  └─────────┘ │   └───┬────┘
                           │       │
                      ┌────▼───────▼──┐
                      │重新同步流程    │
                      │重试导航        │
                      └────┬───────────┘
                           │
                      ┌────▼────────────┐
                      │状态验证         │
                      │一致性检查       │
                      └────┬────────────┘
                           │
                      ┌────▼────────────┐
                      │恢复正常状态     │
                      └─────────────────┘
```

**错误恢复代码示例：**

```typescript
class ErrorRecoveryHandler {
    private recoveryAttempts = 0;
    private maxAttempts = 3;

    async handleSyncFailure(error: Error, path: string) {
        console.error('[Recovery] Sync failed:', error);

        if (this.recoveryAttempts >= this.maxAttempts) {
            console.error('[Recovery] Max attempts reached, giving up');
            this.resetState();
            return;
        }

        this.recoveryAttempts++;

        // 等待一段时间后重试
        await new Promise(resolve => setTimeout(resolve, 500 * this.recoveryAttempts));

        console.log(`[Recovery] Attempting recovery ${this.recoveryAttempts}/${this.maxAttempts}`);

        try {
            // 尝试重新同步
            navigationBridge.navigateFromApp(path);
        } catch (retryError) {
            console.error('[Recovery] Retry failed:', retryError);
            // 递归重试或给up
            this.handleSyncFailure(retryError, path);
        }
    }

    resetState() {
        this.recoveryAttempts = 0;
        navigationBridge.endSync();
        console.log('[Recovery] State reset, ready for next navigation');
    }
}
```

---

## 7. 性能指标追踪

```
关键指标可视化：

导航延迟分解：
┌──────────────────────────────────────┐
│ Total Navigation Time: ~100ms        │
│ ┌────────────────────────────────┐   │
│ │ Event Processing: 5ms          │   │
│ └────────────────────────────────┘   │
│ ┌────────────────────────────────┐   │
│ │ React Navigate: 15ms           │   │
│ └────────────────────────────────┘   │
│ ┌────────────────────────────────┐   │
│ │ Component Render: 30ms         │   │
│ └────────────────────────────────┘   │
│ ┌────────────────────────────────┐   │
│ │ Notification Back: 5ms         │   │
│ └────────────────────────────────┘   │
│ ┌────────────────────────────────┐   │
│ │ Sync Timeout Wait: 45ms        │   │
│ └────────────────────────────────┘   │
└──────────────────────────────────────┘

监控指标：

导航操作 (per 100 ops):
├─ 成功: 98
├─ 队列延迟: 1
├─ 超时: 0.5
└─ 失败: 0.5

内存使用 (长期运行):
├─ 初始: ~5MB
├─ 100次导航后: ~5.2MB
├─ 1000次导航后: ~5.5MB
└─ 峰值内存: ~6MB (GC清理前)

事件监听器数量:
├─ 初始: 4
├─ 运行时: 4 (稳定)
└─ 内存泄漏: 无检测
```

---

## 8. 调试流程图

```
问题诊断树：

┌─────────────────────────────────────┐
│ 导航不工作？                        │
└────────────┬────────────────────────┘
             │
      ┌──────┴──────┬──────────┐
      │             │          │
  ┌───▼─────┐  ┌────▼───┐  ┌─▼───────┐
  │React不  │  │React   │  │Browser  │
  │加载？   │  │加载但  │  │历史错误 │
  │         │  │无响应？│  │         │
  └───┬─────┘  └────┬───┘  └─┬───────┘
      │             │        │
  ┌───▼──────────┐  │    ┌────▼────┐
  │检查网络      │  │    │检查同步 │
  │检查控制台    │  │    │状态     │
  │错误          │  │    │         │
  └──────────────┘  │    └────┬────┘
                    │         │
                ┌───▼────┐ ┌──▼──┐
                │检查：   │ │检查：│
                │- DOM?  │ │-同步 │
                │- React?│ │标志？│
                │- Hook? │ │-事件 │
                │        │ │源？  │
                └────┬───┘ └──┬──┘
                     │       │
                     └───┬───┘
                         │
                    ┌────▼─────┐
                    │查看日志  │
                    │__navigation
                    │BridgeDebug│
                    │.getLogs()│
                    └────┬─────┘
                         │
                    ┌────▼────────────┐
                    │分析事件序列     │
                    │找出问题节点     │
                    │执行修复         │
                    └─────────────────┘
```

**调试命令：**

```javascript
// 浏览器控制台命令

// 1. 查看导航事件日志
__navigationBridgeDebug.getLogs()

// 2. 查看当前同步状态
__navigationBridgeDebug.getState()

// 3. 模拟主应用导航
__navigationBridgeDebug.simulateMainAppNav('/releases')

// 4. 模拟React导航
__navigationBridgeDebug.simulateReactNav('/overview')

// 5. 导出日志用于分析
const logs = __navigationBridgeDebug.exportLogs()
console.log(logs)

// 6. 检查React App是否准备好
console.log('React Ready:', typeof window.ReactAppBridge !== 'undefined')

// 7. 检查React Router Location
console.log('Current location:', window.location.pathname)

// 8. 手动触发事件用于测试
const event = new CustomEvent('spa:navigate', {
    detail: { route: 'overview' }
})
window.dispatchEvent(event)
```

---

## 总结：事件流流程图快速参考

| 场景 | 触发方 | 目标 | 返回反馈 | 同步时间 |
|-----|--------|------|---------|---------|
| 侧边栏点击 | Main App | React | 无返回 | 同步 |
| React内导航 | React | Main App | 侧边栏高亮 | 异步通知 |
| Back按钮 | Browser | React | 自动导航 | 同步 |
| 队列等待 | Main App | React | delayed | 延迟同步 |
| 错误恢复 | Both | Both | retry | 重试同步 |

