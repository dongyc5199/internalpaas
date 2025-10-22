# SSH终端管理器 AI 能力集成设计方案

**文档编写时间**: 2025-10-20
**目标页面**: `http://localhost:9090/terminal/manager?serverId=1`
**对应模板**: `src/main/resources/templates/terminal/manager.html`

---

## 📋 目录

1. [现状分析](#1-现状分析)
2. [集成目标](#2-集成目标)
3. [AI助手双模式设计](#3-ai助手双模式设计)
4. [UI/UX 设计方案](#4-uiux-设计方案)
5. [技术实现方案](#5-技术实现方案)
6. [API 集成](#6-api-集成)
7. [实施步骤](#7-实施步骤)
8. [测试验证](#8-测试验证)

> **📚 相关文档**: 详细的双模式分析请参考 [`ai-assistant-modes-analysis.md`](./ai-assistant-modes-analysis.md)

---

## 1. 现状分析

### 1.1 现有页面架构

**文件**: `src/main/resources/templates/terminal/manager.html` (1315行)

**核心功能**:
- ✅ 多标签页SSH终端管理
- ✅ WebSocket连接 (`/ws/ssh-terminal`)
- ✅ xterm.js 终端渲染
- ✅ 服务器选择与连接
- ✅ 会话管理（复制、重连、关闭）
- ✅ 快捷键支持（Ctrl+Shift+T、Ctrl+Tab等）

**关键组件**:
```javascript
class SSHTerminalManager {
    terminals: Map<terminalId, {
        terminal: Terminal,        // xterm.js实例
        fitAddon: FitAddon,       // 自适应插件
        ws: WebSocket,            // SSH WebSocket连接
        connected: boolean,       // 连接状态
        serverId: string          // 服务器ID
    }>,
    activeTerminalId: string,     // 当前活跃终端
    sessionCounter: number        // 会话计数器
}
```

**页面布局**:
```
┌────────────────────────────────────────────────────────┐
│ 工具栏 (Toolbar)                                        │
│ [服务器选择▼] [新建连接] [复制会话] [重新连接]          │
│                                   [0个活跃会话] [信息]  │
├────────────────────────────────────────────────────────┤
│ 标签页 (Tabs)                                           │
│ [服务器1 ×] [服务器2 ×] [服务器3 ×]                    │
├────────────────────────────────────────────────────────┤
│                                                        │
│            终端内容区域 (Terminal Content)               │
│                                                        │
│                  [xterm.js 终端]                        │
│                                                        │
│                                                        │
└────────────────────────────────────────────────────────┘
```

### 1.2 已有AI能力

**后端服务**:
- ✅ `ChatService` - AI聊天服务
- ✅ `AiClientRegistry` - AI客户端注册表
- ✅ 支持多提供商（Moonshot、Ollama、Echo）
- ✅ 流式响应（SSE）

**API端点**:
- `POST /ai/chat/stream` - 流式聊天
- `POST /ai/completion/suggest` - 命令补全

**Demo页面**:
- ✅ `terminal/ai-assist-demo.html` - 双面板演示页面
  - 左侧：终端
  - 右侧：AI聊天

### 1.3 差距分析

| 功能需求 | 现有Demo | 目标终端管理器 | 差距 |
|---------|---------|---------------|-----|
| **AI聊天面板** | ✅ 独立右侧面板 | ❌ 无 | 需要在多标签终端中集成 |
| **命令补全** | ✅ 示例实现 | ❌ 无 | 需要与终端输入集成 |
| **上下文关联** | ✅ 可选终端尾部 | ❌ 无 | 需要自动提取当前终端输出 |
| **多会话支持** | ❌ 单会话 | ✅ 多标签 | 需要每个标签独立AI会话 |
| **UI集成** | ❌ 独立页面 | ❌ 无AI UI | 需要设计侧边抽屉/浮窗 |

---

## 2. 集成目标

### 2.1 功能目标

1. **AI助手面板**
   - 为每个终端会话提供独立的AI聊天助手
   - 支持实时流式对话
   - 自动关联当前终端上下文

2. **智能命令补全**
   - 基于历史命令和AI模型的智能建议
   - 实时显示补全选项
   - Tab键快速应用

3. **上下文感知**
   - 自动提取终端最近N行输出作为上下文
   - 支持用户手动选择上下文范围
   - 保持会话历史记录

4. **无缝集成**
   - 不影响现有SSH终端功能
   - 支持快捷键呼出/隐藏AI面板
   - 响应式布局适配

### 2.2 非功能目标

- **性能**: AI请求不阻塞终端交互
- **安全**: 复用现有Spring Security认证
- **可维护**: 模块化设计，易于扩展
- **用户体验**: 流畅的动画过渡，清晰的状态提示

---

## 3. AI助手双模式设计

> **📚 完整设计文档**: 详见 [`ai-assistant-modes-analysis.md`](./ai-assistant-modes-analysis.md)

### 3.1 模式概述

AI助手提供**两种互补的工作模式**，满足不同场景的需求：

| 模式 | 定位 | 核心能力 | 适用场景 |
|------|------|---------|---------|
| **Ask模式** 📋 | 智能问答助手 | 与AI聊天，获取建议和解释 | 学习、诊断、咨询 |
| **Agent模式** 🤖 | 自动化执行代理 | AI自主规划并执行终端任务 | 自动化、编排、修复 |

### 3.2 Ask模式（智能问答）

**定位**: 作为开发者的智能助手，提供即时的技术支持和建议。

**核心功能**:
- ✅ **上下文感知对话**: 自动提取当前终端最近100行输出
- ✅ **智能问答**: 解释错误、提供命令建议、教授最佳实践
- ✅ **代码高亮**: 回复中的命令/代码自动高亮显示
- ✅ **快速插入**: 一键将AI建议的命令复制到终端

**典型使用场景**:
```
场景: 错误诊断
终端显示: ERROR: Connection refused

用户问AI: "这个错误是什么原因？"

AI回复:
"这是连接被拒绝错误。可能原因：
1. 目标服务未启动
2. 端口配置错误
3. 防火墙阻止

建议检查命令：
┌────────────────────────────────┐
│ systemctl status target-service │ 📋 复制
└────────────────────────────────┘
┌────────────────────────────────┐
│ telnet target-host 8080         │ 📋 复制
└────────────────────────────────┘
```

**交互流程**:
```
用户提问 → 提取上下文 → 调用ChatService → 流式返回 → 用户阅读建议
```

### 3.3 Agent模式（自动化代理）

**定位**: 作为可信赖的自动化执行代理，自主完成复杂的多步骤任务。

**核心功能**:
- 🤖 **任务理解**: 解析自然语言指令
- 📋 **自动规划**: 生成多步骤执行计划
- 🔍 **环境感知**: 分析服务器状态和上下文
- ⚙️ **脚本生成**: 自动编写shell脚本
- 🚀 **自主执行**: 在终端中逐步执行命令
- 🛡️ **安全审查**: 危险命令拦截和用户确认
- 📊 **实时监控**: 监控执行状态和输出
- 🔄 **自适应调整**: 根据结果动态调整策略

**典型使用场景**:
```
场景: 应用部署
用户指令: "帮我部署最新版本的应用到生产环境"

AI生成计划:
┌─────────────────────────────────┐
│ 步骤1: 备份当前版本       [必需] │
│ 步骤2: 停止旧应用         [必需] │
│ 步骤3: 下载新版本         [必需] │
│ 步骤4: 启动新应用         [必需] │
│ 步骤5: 健康检查           [必需] │
│ 步骤6: 验证版本           [可选] │
└─────────────────────────────────┘
[用户审核并批准]

AI执行:
✅ 备份完成 (5秒)
✅ 旧应用已停止 (8秒)
✅ 新版本下载完成 (20秒)
✅ 新应用启动成功 (10秒)
✅ 健康检查通过 (30秒)
✅ 版本验证: v2.1.0

部署成功！总耗时: 2分15秒
```

**工作流程**:
```
用户指令 → AI分析 → 环境探测 → 生成计划 → 用户审核 →
批准执行 → 逐步执行 → 实时监控 → 错误处理 → 完成总结
```

### 3.4 安全机制

#### 多层安全防护

| 层级 | 措施 | 说明 |
|-----|------|------|
| **第1层** | 权限控制 | 只有ADMIN/SUPER_ADMIN可使用Agent模式 |
| **第2层** | 命令审查 | 黑名单拦截危险命令（rm -rf /等） |
| **第3层** | 执行审核 | 所有计划必须用户批准 |
| **第4层** | 实时监控 | 执行中可随时暂停/停止 |
| **第5层** | 审计日志 | 完整记录所有操作历史 |

**命令黑名单示例**:
```yaml
# 直接拒绝执行
blacklist:
  - "rm -rf /"
  - "dd if=/dev/zero of=/dev/sda"
  - "mkfs.*"
  - "shutdown|reboot|halt"
  - "iptables -F"

# 需要用户二次确认
high-risk:
  - "rm.*"
  - "kill.*"
  - "systemctl (stop|restart)"
  - "(apt-get|yum) remove"
```

### 3.5 模式对比

| 维度 | Ask模式 | Agent模式 |
|------|---------|-----------|
| **风险级别** | 🟢 低（只读） | 🔴 高（写操作） |
| **用户控制** | 完全手动 | 半自动（需审批） |
| **技术复杂度** | 简单 | 复杂 |
| **适用角色** | 所有用户 | ADMIN以上 |
| **依赖** | ChatService | ChatService + 执行引擎 |

### 3.6 实施优先级

**第一阶段** (优先实现):
- ✅ Ask模式完整功能
- ✅ Agent模式基础框架
- ✅ 安全机制核心功能

**第二阶段** (后续优化):
- ⏳ Agent模式高级特性（自适应、模板）
- ⏳ 性能优化与并发控制
- ⏳ 更多预定义任务模板

---

## 4. UI/UX 设计方案

### 4.1 方案对比

| 方案 | 优点 | 缺点 | 推荐度 |
|-----|------|------|--------|
| **A. 右侧抽屉面板** | 不遮挡终端；可调整宽度；类VSCode | 占用屏幕空间；小屏幕体验差 | ⭐⭐⭐⭐⭐ |
| **B. 底部浮层** | 节省横向空间；适合代码输出 | 遮挡终端内容；高度受限 | ⭐⭐⭐ |
| **C. 模态弹窗** | 聚焦对话；不占用布局 | 完全遮挡终端；交互割裂 | ⭐⭐ |
| **D. 内联集成** | 终端内显示；无需切换 | 混淆终端输出；实现复杂 | ⭐ |

**最终选择**: **方案A - 右侧抽屉面板**

### 3.2 最终UI设计

```
┌────────────────────────────────────────────────────────┬───────────────────┐
│ 工具栏                                              │ [AI助手 🤖]       │
│ [服务器▼] [新建] [复制] [重连]     [会话信息]        │                   │
├────────────────────────────────────────────────────────┤                   │
│ [终端1 ×] [终端2 ×]                                   │ [模型: Moonshot]  │
├────────────────────────────────────────────────────────┤                   │
│                                                        │  聊天历史区域      │
│            SSH 终端内容区域                             │  ┌──────────────┐ │
│                                                        │  │ User: 帮我... │ │
│      $ ls -lah                                         │  └──────────────┘ │
│      total 48K                                         │  ┌──────────────┐ │
│      -rw-r--r-- 1 root root 1.2K Oct 20 13:51 app.log │  │ AI: 你可以... │ │
│      drwxr-xr-x 5 root root 4.0K Oct 20 13:50 src/    │  └──────────────┘ │
│                                                        │                   │
│                                                        │  输入区域          │
│                                                        │  ┌──────────────┐ │
└────────────────────────────────────────────────────────┤  │ [发送消息...]  │ │
                                                         │  └──────────────┘ │
                                                         │  [发送]   [清空] │
                                                         └───────────────────┘
                        ↑                                            ↑
                    终端区域                                     AI助手抽屉
                 (可调整宽度)                                  (可展开/收起)
```

### 3.3 交互设计

#### 3.3.1 AI助手触发方式

1. **工具栏按钮**: 点击顶部 "AI助手 🤖" 按钮
2. **快捷键**: `Ctrl+Shift+A` 快速呼出/隐藏
3. **右键菜单**: 终端右键菜单增加 "询问AI" 选项
4. **智能提示**: 检测到错误时自动显示AI建议图标

#### 3.3.2 状态指示

```
🟢 AI助手就绪          - 绿色圆点，可以正常使用
🟡 AI正在思考...       - 黄色脉冲，等待响应
🔴 AI服务不可用        - 红色圆点，需要检查配置
⚙️ 正在连接模型...     - 灰色，初始化中
```

#### 3.3.3 动画效果

- **抽屉展开**: 280ms ease-out 平滑滑入
- **消息发送**: 淡入 + 上滑 200ms
- **AI回复**: 打字机效果（流式渲染）
- **错误提示**: 抖动动画 + 红色高亮

---

## 4. 技术实现方案

### 4.1 前端架构

#### 4.1.1 新增组件

```javascript
class AiAssistantPanel {
    constructor(terminalManager) {
        this.terminalManager = terminalManager;  // 引用终端管理器
        this.chatHistory = [];                   // 聊天历史
        this.currentModel = 'moonshot';          // 当前模型
        this.isOpen = false;                     // 面板状态
        this.eventSource = null;                 // SSE连接
    }

    // 核心方法
    toggle()                    // 展开/收起面板
    sendMessage(message)        // 发送消息到AI
    receiveStream(event)        // 处理流式响应
    extractContext()            // 提取终端上下文
    clearHistory()              // 清空聊天记录
    switchModel(modelName)      // 切换AI模型
}
```

#### 4.1.2 与终端管理器集成

```javascript
class SSHTerminalManager {
    constructor() {
        // ... 现有代码 ...
        this.aiAssistant = new AiAssistantPanel(this);  // 新增
    }

    // 新增方法
    getActiveTerminalContext() {
        const session = this.terminals.get(this.activeTerminalId);
        if (!session) return null;

        // 获取终端最近100行输出
        const buffer = session.terminal.buffer.active;
        let lines = [];
        for (let i = Math.max(0, buffer.length - 100); i < buffer.length; i++) {
            lines.push(buffer.getLine(i).translateToString());
        }
        return lines.join('\n');
    }
}
```

### 4.2 HTML结构

在 `terminal/manager.html` 的 `<body>` 末尾、`</div>` 之前添加：

```html
<!-- AI Assistant Drawer -->
<div class="ai-assistant-drawer" id="aiAssistantDrawer">
    <div class="ai-drawer-header">
        <div class="ai-drawer-title">
            <span class="ai-status-indicator"></span>
            <h3>AI 助手</h3>
        </div>
        <div class="ai-drawer-controls">
            <select id="aiModelSelector" class="ai-model-select">
                <option value="moonshot">Moonshot</option>
                <option value="ollama">Ollama</option>
                <option value="echo">Echo (测试)</option>
            </select>
            <button class="ai-control-btn" id="aiClearBtn" title="清空历史">
                <i class="fas fa-eraser"></i>
            </button>
            <button class="ai-control-btn" id="aiCloseBtn" title="关闭">
                <i class="fas fa-times"></i>
            </button>
        </div>
    </div>

    <div class="ai-chat-container" id="aiChatContainer">
        <div class="ai-welcome-message">
            <i class="fas fa-robot fa-3x"></i>
            <h4>你好！我是AI助手</h4>
            <p>我可以帮你分析终端输出、解释错误、建议命令等</p>
        </div>
        <!-- 聊天消息将动态插入这里 -->
    </div>

    <div class="ai-input-container">
        <div class="ai-context-info" id="aiContextInfo">
            <i class="fas fa-link"></i>
            <span>自动关联当前终端上下文</span>
        </div>
        <textarea
            id="aiMessageInput"
            class="ai-message-input"
            placeholder="输入消息... (Ctrl+Enter发送)"
            rows="3"
        ></textarea>
        <div class="ai-input-actions">
            <button class="btn-ai-send" id="aiSendBtn">
                <i class="fas fa-paper-plane"></i> 发送
            </button>
        </div>
    </div>
</div>

<!-- AI Toggle Button (Floating) -->
<button class="ai-toggle-btn" id="aiToggleBtn" title="AI助手 (Ctrl+Shift+A)">
    🤖
</button>
```

### 4.3 CSS样式

新建文件 `src/main/resources/static/css/ai-assistant.css`:

```css
/* AI Assistant Drawer */
.ai-assistant-drawer {
    position: fixed;
    top: 0;
    right: -400px;  /* 初始隐藏在右侧外 */
    width: 400px;
    height: 100vh;
    background: linear-gradient(135deg, #1a1a1a 0%, #242424 100%);
    border-left: 1px solid rgba(255, 255, 255, 0.1);
    display: flex;
    flex-direction: column;
    z-index: 1300;
    box-shadow: -4px 0 24px rgba(0, 0, 0, 0.4);
    transition: right 0.28s cubic-bezier(0.4, 0, 0.2, 1);
}

.ai-assistant-drawer.is-open {
    right: 0;
}

.ai-drawer-header {
    background: rgba(45, 45, 48, 0.8);
    padding: 16px 20px;
    border-bottom: 1px solid rgba(255, 255, 255, 0.08);
    display: flex;
    justify-content: space-between;
    align-items: center;
}

.ai-drawer-title {
    display: flex;
    align-items: center;
    gap: 12px;
}

.ai-drawer-title h3 {
    margin: 0;
    font-size: 16px;
    font-weight: 600;
    color: #e0e0e0;
}

.ai-status-indicator {
    width: 10px;
    height: 10px;
    border-radius: 50%;
    background: #4CAF50;
    animation: ai-pulse 2s infinite;
}

@keyframes ai-pulse {
    0%, 100% { opacity: 1; }
    50% { opacity: 0.5; }
}

.ai-chat-container {
    flex: 1;
    overflow-y: auto;
    padding: 20px;
    display: flex;
    flex-direction: column;
    gap: 16px;
}

.ai-welcome-message {
    text-align: center;
    color: rgba(255, 255, 255, 0.7);
    padding: 40px 20px;
}

.ai-message {
    max-width: 90%;
    padding: 12px 16px;
    border-radius: 12px;
    font-size: 14px;
    line-height: 1.6;
    animation: ai-message-in 0.2s ease-out;
}

@keyframes ai-message-in {
    from {
        opacity: 0;
        transform: translateY(10px);
    }
    to {
        opacity: 1;
        transform: translateY(0);
    }
}

.ai-message.user {
    align-self: flex-end;
    background: rgba(0, 122, 204, 0.2);
    border: 1px solid rgba(0, 122, 204, 0.4);
    color: #e0e0e0;
}

.ai-message.assistant {
    align-self: flex-start;
    background: rgba(76, 175, 80, 0.15);
    border: 1px solid rgba(76, 175, 80, 0.3);
    color: #e0e0e0;
}

.ai-input-container {
    border-top: 1px solid rgba(255, 255, 255, 0.08);
    padding: 16px;
    background: rgba(45, 45, 48, 0.6);
}

.ai-message-input {
    width: 100%;
    background: rgba(255, 255, 255, 0.08);
    border: 1px solid rgba(255, 255, 255, 0.12);
    border-radius: 8px;
    padding: 12px;
    color: #e0e0e0;
    font-size: 14px;
    resize: none;
    outline: none;
    transition: all 0.2s;
}

.ai-message-input:focus {
    border-color: rgba(0, 122, 204, 0.6);
    box-shadow: 0 0 0 3px rgba(0, 122, 204, 0.1);
}

.btn-ai-send {
    background: linear-gradient(135deg, #0e639c 0%, #1177bb 100%);
    color: white;
    border: none;
    padding: 10px 20px;
    border-radius: 8px;
    cursor: pointer;
    font-size: 14px;
    font-weight: 500;
    transition: all 0.2s;
    margin-top: 12px;
    width: 100%;
}

.btn-ai-send:hover {
    background: linear-gradient(135deg, #1177bb 0%, #1488cc 100%);
    transform: translateY(-1px);
    box-shadow: 0 4px 12px rgba(14, 99, 156, 0.4);
}

.ai-toggle-btn {
    position: fixed;
    top: 60px;
    right: 20px;
    width: 50px;
    height: 50px;
    border-radius: 50%;
    background: linear-gradient(135deg, #0e639c 0%, #1177bb 100%);
    border: none;
    font-size: 24px;
    cursor: pointer;
    box-shadow: 0 4px 16px rgba(14, 99, 156, 0.4);
    z-index: 1200;
    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.ai-toggle-btn:hover {
    transform: scale(1.1) rotate(5deg);
    box-shadow: 0 6px 20px rgba(14, 99, 156, 0.6);
}

/* 当抽屉打开时隐藏浮动按钮 */
.ai-assistant-drawer.is-open ~ .ai-toggle-btn {
    opacity: 0;
    pointer-events: none;
}
```

---

## 5. API 集成

### 5.1 聊天流式请求

```javascript
async function sendAiMessage(message, model = 'moonshot') {
    const context = terminalManager.getActiveTerminalContext();
    const chatId = `terminal-${terminalManager.activeTerminalId}`;

    const requestBody = {
        chatId: chatId,
        sessionId: terminalManager.activeTerminalId,
        message: message,
        model: model,
        terminalTail: context  // 终端上下文
    };

    // 获取CSRF Token
    const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;

    const headers = {
        'Content-Type': 'application/json'
    };
    if (csrfToken && csrfHeader) {
        headers[csrfHeader] = csrfToken;
    }

    // 创建EventSource连接
    const response = await fetch('/ai/chat/stream', {
        method: 'POST',
        headers: headers,
        body: JSON.stringify(requestBody)
    });

    if (!response.ok) {
        throw new Error(`AI请求失败: ${response.status}`);
    }

    // 处理SSE流式响应
    const reader = response.body.getReader();
    const decoder = new TextDecoder();
    let buffer = '';
    let aiMessageElement = createAiMessage('assistant', '');

    while (true) {
        const { value, done } = await reader.read();
        if (done) break;

        buffer += decoder.decode(value, { stream: true });
        const lines = buffer.split('\n');
        buffer = lines.pop(); // 保留未完成的行

        for (const line of lines) {
            if (line.startsWith('data: ')) {
                const data = line.slice(6);
                if (data === '[DONE]') continue;

                try {
                    const event = JSON.parse(data);
                    if (event.type === 'content') {
                        aiMessageElement.textContent += event.data;
                        // 自动滚动到底部
                        aiMessageElement.scrollIntoView({ behavior: 'smooth' });
                    }
                } catch (e) {
                    console.error('解析SSE消息失败:', e);
                }
            }
        }
    }
}
```

### 5.2 命令补全请求

```javascript
async function getCommandSuggestions(prompt) {
    const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;

    const headers = {
        'Content-Type': 'application/json'
    };
    if (csrfToken && csrfHeader) {
        headers[csrfHeader] = csrfToken;
    }

    const response = await fetch('/ai/completion/suggest', {
        method: 'POST',
        headers: headers,
        body: JSON.stringify({ prompt: prompt })
    });

    if (!response.ok) {
        throw new Error(`补全请求失败: ${response.status}`);
    }

    const result = await response.json();
    return result.suggestions || [];
}
```

---

## 6. 实施步骤

### Phase 1: 准备工作 (0.5天)

- [x] 分析现有页面结构
- [x] 设计UI/UX方案
- [x] 制定技术方案
- [ ] 准备CSS/JS文件结构

### Phase 2: 后端验证 (0.5天)

- [ ] 测试现有AI API (`/ai/chat/stream`, `/ai/completion/suggest`)
- [ ] 验证CSRF Token集成
- [ ] 确认Session认证流程
- [ ] 准备测试数据（模型配置、API密钥）

### Phase 3: 前端UI实现 (1天)

- [ ] 创建 `ai-assistant.css` 样式文件
- [ ] 在 `manager.html` 中添加AI抽屉HTML结构
- [ ] 实现抽屉展开/收起动画
- [ ] 添加AI浮动按钮
- [ ] 实现快捷键支持 (`Ctrl+Shift+A`)

### Phase 4: 功能集成 (1.5天)

- [ ] 创建 `AiAssistantPanel` 类
- [ ] 实现消息发送与接收
- [ ] 集成SSE流式响应处理
- [ ] 实现终端上下文提取
- [ ] 添加模型切换功能
- [ ] 实现聊天历史管理

### Phase 5: 优化与测试 (1天)

- [ ] 性能优化（防抖、节流）
- [ ] 错误处理与重试机制
- [ ] 响应式布局适配
- [ ] 浏览器兼容性测试
- [ ] 用户体验优化（加载动画、状态提示）

### Phase 6: 文档与部署 (0.5天)

- [ ] 编写用户使用文档
- [ ] 更新系统文档
- [ ] 准备配置指南
- [ ] 代码提交与审查

**总计工时**: 5天

---

## 7. 测试验证

### 7.1 功能测试清单

| 测试项 | 验证要点 | 状态 |
|--------|---------|-----|
| **AI面板展开** | 点击按钮、快捷键都能正常展开/收起 | ⬜ |
| **消息发送** | 输入消息后点击发送或Ctrl+Enter能正常发送 | ⬜ |
| **流式响应** | AI回复能流畅显示，无卡顿 | ⬜ |
| **上下文提取** | 能正确获取当前终端最近100行输出 | ⬜ |
| **模型切换** | 切换不同模型后能正常对话 | ⬜ |
| **多终端支持** | 每个终端标签有独立的AI会话 | ⬜ |
| **快捷键** | Ctrl+Shift+A能正常呼出/隐藏 | ⬜ |
| **清空历史** | 能清空当前聊天记录 | ⬜ |
| **错误处理** | AI服务不可用时有清晰提示 | ⬜ |
| **响应式** | 不同屏幕尺寸下布局正常 | ⬜ |

### 7.2 性能测试

- [ ] 测试100条消息历史记录的渲染性能
- [ ] 测试流式响应对终端交互的影响
- [ ] 测试并发多个AI请求的表现
- [ ] 测试内存占用情况

### 7.3 安全测试

- [ ] 验证CSRF Token正确传递
- [ ] 验证未登录用户无法访问AI API
- [ ] 测试敏感信息（密码、密钥）是否泄露到上下文

---

## 附录

### A. 文件清单

**新建文件**:
- `src/main/resources/static/css/ai-assistant.css` - AI助手样式
- `src/main/resources/static/js/ai-assistant.js` - AI助手逻辑（可选，也可内联）

**修改文件**:
- `src/main/resources/templates/terminal/manager.html` - 添加AI UI和脚本

**配置文件**:
- `src/main/resources/application.properties` - AI模型配置（已存在）
- `.env` - 密钥配置（已存在）

### B. 快捷键列表

| 快捷键 | 功能 | 范围 |
|--------|------|------|
| `Ctrl+Shift+A` | 展开/收起AI助手 | 全局 |
| `Ctrl+Enter` | 发送AI消息 | AI输入框聚焦时 |
| `Esc` | 关闭AI助手 | AI面板打开时 |

### C. 配置示例

```properties
# AI Provider Configuration
ai.providers.moonshot.enabled=true
ai.providers.moonshot.base-url=https://api.moonshot.cn
ai.providers.moonshot.api-key=${MOONSHOT_API_KEY}

ai.providers.ollama.enabled=false
ai.providers.ollama.base-url=http://localhost:11434

ai.chat.default-provider=moonshot
ai.chat.max-context-messages=20
ai.chat.streaming-enabled=true
```

---

**文档状态**: ✅ 设计完成，待实施
**下一步**: Phase 2 - 后端验证
