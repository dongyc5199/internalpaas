# AI助手双模式需求分析与设计

**文档编写时间**: 2025-10-20
**需求来源**: 用户功能扩展需求
**相关文档**: `terminal-manager-ai-integration-design.md`

---

## 📋 目录

1. [需求概述](#1-需求概述)
2. [模式对比分析](#2-模式对比分析)
3. [Ask模式设计](#3-ask模式设计)
4. [Agent模式设计](#4-agent模式设计)
5. [安全与风险控制](#5-安全与风险控制)
6. [技术架构](#6-技术架构)
7. [UI/UX设计](#7-uiux设计)
8. [实施路线图](#8-实施路线图)

---

## 1. 需求概述

### 1.1 核心需求

为SSH终端AI助手提供**两种互补的工作模式**：

| 模式 | 定位 | 核心能力 |
|------|------|---------|
| **Ask模式** | 智能问答助手 | 与AI聊天，获取建议和解释 |
| **Agent模式** | 自动化执行代理 | AI自主规划并执行终端任务 |

### 1.2 业务价值

**Ask模式价值**:
- 🎓 **学习辅助**: 帮助开发者理解复杂的终端输出和错误
- 🔍 **问题诊断**: 快速分析日志、定位问题根因
- 💡 **最佳实践**: 提供命令建议和操作指导
- 📚 **知识库**: 作为即时可用的运维知识助手

**Agent模式价值**:
- ⚡ **效率提升**: 自动化重复性任务（部署、备份、巡检）
- 🤖 **智能编排**: AI自主设计多步骤工作流
- 🛡️ **降低错误**: 减少手动输入命令的失误
- 📈 **可扩展**: 支持复杂场景的自动化执行

### 1.3 使用场景

#### Ask模式典型场景

**场景1: 错误诊断**
```
终端输出:
ERROR: Failed to start application
java.lang.OutOfMemoryError: Java heap space

用户问AI:
"这个错误是什么原因？如何解决？"

AI回复:
"这是Java堆内存溢出错误。可能原因：
1. 应用内存配置不足
2. 存在内存泄漏
3. 数据量过大

建议解决方案：
1. 增加JVM堆内存: -Xmx2g
2. 分析内存dump文件
3. 检查是否有内存泄漏
..."
```

**场景2: 命令学习**
```
用户问AI:
"如何查看占用CPU最高的前10个进程？"

AI回复:
"可以使用以下命令：
ps aux --sort=-%cpu | head -n 11

说明：
- ps aux: 显示所有进程
- --sort=-%cpu: 按CPU使用率降序排序
- head -n 11: 取前11行（含表头）
..."
```

**场景3: 日志分析**
```
终端显示大量nginx访问日志

用户问AI:
"分析这些日志，找出异常请求"

AI回复:
"根据日志分析：
1. 发现来自IP 192.168.1.100的大量404请求
2. /admin路径被频繁访问（可能是扫描）
3. 建议检查防火墙规则，封禁可疑IP
..."
```

#### Agent模式典型场景

**场景1: 应用部署**
```
用户指令:
"帮我部署最新版本的Spring Boot应用到生产环境"

AI执行流程:
[规划阶段]
✓ 1. 备份当前版本
✓ 2. 停止旧应用
✓ 3. 下载新版本jar
✓ 4. 启动新应用
✓ 5. 健康检查
✓ 6. 验证部署成功

[等待用户确认]
点击"执行"按钮后

[执行阶段]
> 正在备份 /app/current.jar → /backup/app-20251020.jar ✓
> 正在停止应用 (PID: 1234) ✓
> 正在下载新版本... ✓
> 正在启动应用... ✓
> 健康检查通过 ✓

部署完成！应用已成功更新到版本 v2.1.0
```

**场景2: 系统巡检**
```
用户指令:
"执行服务器健康检查"

AI执行流程:
[规划阶段]
✓ 1. 检查磁盘空间
✓ 2. 检查内存使用
✓ 3. 检查CPU负载
✓ 4. 检查关键服务状态
✓ 5. 生成巡检报告

[自动执行]
> 检查磁盘空间... ✓ (使用率 45%)
> 检查内存... ⚠️ (使用率 85%，建议清理缓存)
> 检查CPU... ✓ (负载正常)
> 检查服务状态... ✓ (nginx, mysql, redis 运行中)

[生成报告]
╔════════════════════════════════╗
║   服务器健康检查报告            ║
╠════════════════════════════════╣
║ 磁盘: ✓ 正常 (45%)             ║
║ 内存: ⚠️ 警告 (85%)            ║
║ CPU:  ✓ 正常 (15%)             ║
║ 服务: ✓ 全部运行中              ║
╚════════════════════════════════╝

建议: 清理内存缓存以优化性能
```

**场景3: 故障恢复**
```
用户指令:
"数据库连接失败了，帮我修复"

AI执行流程:
[诊断阶段]
> 检查MySQL服务状态... ✗ 未运行
> 检查端口占用... ✓ 3306端口空闲
> 检查日志文件... 发现错误: disk full

[规划修复]
✓ 1. 清理临时文件释放空间
✓ 2. 重启MySQL服务
✓ 3. 验证连接

[执行修复]
> 清理临时文件... ✓ (释放 2.5GB)
> 启动MySQL... ✓
> 验证连接... ✓

问题已解决！MySQL服务已恢复正常
```

---

## 2. 模式对比分析

### 2.1 核心差异

| 维度 | Ask模式 | Agent模式 |
|------|---------|-----------|
| **交互方式** | 问答对话 | 任务委托 |
| **输出形式** | 文本建议 | 自动执行 |
| **用户角色** | 主动操作 | 监督审批 |
| **AI角色** | 顾问助手 | 自主代理 |
| **风险级别** | 低（只读） | 高（写操作） |
| **适用场景** | 学习、诊断、咨询 | 自动化、编排、修复 |
| **技术复杂度** | 简单 | 复杂 |
| **依赖** | ChatService | ChatService + 执行引擎 |

### 2.2 工作流对比

#### Ask模式工作流
```
用户提问
    ↓
提取终端上下文（最近100行）
    ↓
构建Chat请求（问题 + 上下文）
    ↓
调用 ChatService.stream()
    ↓
流式返回AI回复
    ↓
用户阅读 → 自行操作终端
```

#### Agent模式工作流
```
用户发出任务指令
    ↓
AI分析任务 + 提取上下文
    ↓
生成执行计划（多步骤）
    ↓
展示计划给用户审核
    ↓
用户批准 ← [可中断]
    ↓
逐步执行脚本命令
    ↓
实时监控输出 + 错误处理
    ↓
自适应调整（如遇错误）
    ↓
完成总结报告
```

### 2.3 技术依赖对比

| 组件 | Ask模式 | Agent模式 |
|------|---------|-----------|
| **前端** | 聊天UI | 聊天UI + 执行计划UI + 进度追踪 |
| **后端API** | `/ai/chat/stream` | `/ai/agent/plan` + `/ai/agent/execute` |
| **AI能力** | 对话理解 | 对话理解 + 任务规划 + 脚本生成 |
| **终端集成** | 只读上下文 | 双向通信（读取 + 写入） |
| **安全机制** | Session认证 | Session认证 + 命令审查 + 权限控制 |

---

## 3. Ask模式设计

### 3.1 功能定义

**核心能力**:
- ✅ **上下文感知对话**: 自动提取当前终端输出作为上下文
- ✅ **智能问答**: 解释错误、提供建议、教授命令
- ✅ **多轮对话**: 支持连续追问和深入讨论
- ✅ **代码高亮**: 回复中的命令/代码自动高亮显示
- ✅ **快速插入**: 一键将AI建议的命令复制到终端

### 3.2 用户交互流程

```
┌─────────────────────────────────────────────────────────┐
│ AI助手 - Ask模式                          [Agent] [Ask]  │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  ┌─────────────────────────────────────────────────┐  │
│  │ 👤 User:                                         │  │
│  │ 这个错误是什么意思？                              │  │
│  └─────────────────────────────────────────────────┘  │
│                                                         │
│  ┌─────────────────────────────────────────────────┐  │
│  │ 🤖 AI:                                           │  │
│  │ 这是连接超时错误。可能原因：                       │  │
│  │ 1. 网络不通                                       │  │
│  │ 2. 防火墙阻止                                     │  │
│  │ 3. 服务未启动                                     │  │
│  │                                                  │  │
│  │ 建议执行以下命令检查：                             │  │
│  │ ┌──────────────────────────────────────────┐    │  │
│  │ │ ping target-server                       │ 📋 │  │
│  │ └──────────────────────────────────────────┘    │  │
│  │ ┌──────────────────────────────────────────┐    │  │
│  │ │ telnet target-server 8080               │ 📋 │  │
│  │ └──────────────────────────────────────────┘    │  │
│  └─────────────────────────────────────────────────┘  │
│                                                         │
├─────────────────────────────────────────────────────────┤
│ 📊 上下文: 自动关联终端最近100行输出         [展开详情] │
├─────────────────────────────────────────────────────────┤
│ [输入您的问题...]                                       │
│                                                         │
│ [发送 Ctrl+Enter]  [清空对话]  [保存会话]               │
└─────────────────────────────────────────────────────────┘
         ↑
    点击📋图标：
    命令自动复制到剪贴板
    或直接插入到终端
```

### 3.3 功能细节

#### 3.3.1 上下文提取策略

```javascript
// 智能上下文提取
function extractTerminalContext() {
    const session = terminalManager.terminals.get(activeTerminalId);
    const buffer = session.terminal.buffer.active;

    // 策略1: 提取最近N行
    let contextLines = [];
    const startLine = Math.max(0, buffer.length - 100);

    for (let i = startLine; i < buffer.length; i++) {
        const line = buffer.getLine(i).translateToString();
        contextLines.push(line);
    }

    // 策略2: 智能截断（去除敏感信息）
    contextLines = contextLines.map(line => {
        // 移除密码、token等敏感信息
        return line.replace(/password=\S+/gi, 'password=***');
    });

    // 策略3: 压缩空行
    let context = contextLines.join('\n').replace(/\n{3,}/g, '\n\n');

    return {
        content: context,
        lineCount: contextLines.length,
        timestamp: new Date().toISOString()
    };
}
```

#### 3.3.2 命令快速插入

```javascript
// 点击代码块旁边的"复制"按钮
function copyCommandToTerminal(command) {
    // 方式1: 复制到剪贴板
    navigator.clipboard.writeText(command);
    showToast('命令已复制到剪贴板', 'success');

    // 方式2: 直接插入到终端输入
    const session = terminalManager.terminals.get(activeTerminalId);
    if (session && session.ws.readyState === WebSocket.OPEN) {
        session.ws.send(JSON.stringify({
            type: 'input',
            data: command
        }));
    }
}
```

#### 3.3.3 代码高亮渲染

```javascript
// AI回复中的代码块自动高亮
function renderAiMessage(content) {
    // 解析Markdown格式
    const htmlContent = marked.parse(content);

    // 对代码块进行语法高亮
    const codeBlocks = htmlContent.querySelectorAll('pre code');
    codeBlocks.forEach(block => {
        hljs.highlightElement(block);

        // 添加复制按钮
        const copyBtn = document.createElement('button');
        copyBtn.className = 'code-copy-btn';
        copyBtn.innerHTML = '📋 复制';
        copyBtn.onclick = () => copyCommandToTerminal(block.textContent);
        block.parentElement.appendChild(copyBtn);
    });

    return htmlContent;
}
```

---

## 4. Agent模式设计

### 4.1 功能定义

**核心能力**:
- 🤖 **任务理解**: 解析用户自然语言指令
- 📋 **自动规划**: 生成多步骤执行计划
- 🔍 **环境感知**: 分析当前服务器状态和终端上下文
- ⚙️ **脚本生成**: 自动编写shell脚本
- 🚀 **自主执行**: 逐步在终端中执行命令
- 🛡️ **安全审查**: 危险命令拦截和用户确认
- 📊 **实时监控**: 监控执行状态和输出
- 🔄 **自适应调整**: 根据执行结果动态调整策略
- 📝 **执行报告**: 生成详细的任务执行总结

### 4.2 工作流设计

#### 4.2.1 完整执行流程

```
┌─────────────────────────────────────────────────────────────┐
│  第1阶段: 任务理解                                            │
├─────────────────────────────────────────────────────────────┤
│  用户输入: "帮我部署最新版本的应用"                           │
│      ↓                                                       │
│  AI分析:                                                     │
│  - 任务类型: 应用部署                                        │
│  - 目标对象: Spring Boot应用                                 │
│  - 关键要求: 最新版本、生产环境                              │
│  - 前置条件: 备份、停止旧版                                  │
└─────────────────────────────────────────────────────────────┘
                        ↓
┌─────────────────────────────────────────────────────────────┐
│  第2阶段: 环境探测                                            │
├─────────────────────────────────────────────────────────────┤
│  执行探测命令:                                               │
│  ✓ ps aux | grep java          → 发现进程PID 1234           │
│  ✓ ls /app/                     → 找到 current.jar          │
│  ✓ curl -I http://localhost:8080 → 应用正在运行             │
│  ✓ df -h                        → 磁盘空间充足               │
└─────────────────────────────────────────────────────────────┘
                        ↓
┌─────────────────────────────────────────────────────────────┐
│  第3阶段: 执行计划生成                                        │
├─────────────────────────────────────────────────────────────┤
│  AI生成计划:                                                 │
│  ┌─────────────────────────────────────────────────────┐   │
│  │ 步骤1: 备份当前版本                            [必需] │   │
│  │   cp /app/current.jar /backup/app-20251020.jar      │   │
│  │                                                      │   │
│  │ 步骤2: 停止旧应用                              [必需] │   │
│  │   kill -15 1234                                     │   │
│  │   sleep 5                                            │   │
│  │                                                      │   │
│  │ 步骤3: 下载新版本                              [必需] │   │
│  │   wget https://repo/app-v2.1.0.jar -O /app/new.jar │   │
│  │                                                      │   │
│  │ 步骤4: 替换文件                                [必需] │   │
│  │   mv /app/new.jar /app/current.jar                  │   │
│  │                                                      │   │
│  │ 步骤5: 启动新应用                              [必需] │   │
│  │   java -jar /app/current.jar &                      │   │
│  │                                                      │   │
│  │ 步骤6: 健康检查                                [必需] │   │
│  │   for i in {1..30}; do                              │   │
│  │     curl -f http://localhost:8080/health && break   │   │
│  │     sleep 2                                          │   │
│  │   done                                               │   │
│  │                                                      │   │
│  │ 步骤7: 验证部署                                [可选] │   │
│  │   curl http://localhost:8080/version                │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
│  风险评估:                                                  │
│  ⚠️ 步骤2涉及服务停止，可能影响业务                          │
│  ⚠️ 步骤3需要网络访问外部仓库                                │
│                                                             │
│  [ 批准执行 ]  [ 修改计划 ]  [ 取消 ]                        │
└─────────────────────────────────────────────────────────────┘
                        ↓
           [用户点击"批准执行"]
                        ↓
┌─────────────────────────────────────────────────────────────┐
│  第4阶段: 执行监控                                            │
├─────────────────────────────────────────────────────────────┤
│  执行进度: ████████████████░░░░ 60% (4/7)                   │
│                                                             │
│  ✅ 步骤1: 备份完成 (5秒)                                    │
│      /backup/app-20251020.jar [12.5 MB]                     │
│                                                             │
│  ✅ 步骤2: 旧应用已停止                                      │
│      进程 1234 已终止                                        │
│                                                             │
│  ✅ 步骤3: 新版本下载完成 (15秒)                             │
│      app-v2.1.0.jar [13.2 MB]                               │
│                                                             │
│  ⏳ 步骤4: 正在替换文件...                                   │
│      mv /app/new.jar /app/current.jar                       │
│                                                             │
│  ⏸️ 步骤5: 等待执行                                          │
│  ⏸️ 步骤6: 等待执行                                          │
│  ⏸️ 步骤7: 等待执行                                          │
│                                                             │
│  [ ⏸️ 暂停 ]  [ ⏹️ 停止 ]  [ 📋 查看日志 ]                    │
└─────────────────────────────────────────────────────────────┘
                        ↓
         [执行过程中检测到错误]
                        ↓
┌─────────────────────────────────────────────────────────────┐
│  第5阶段: 错误处理与自适应                                    │
├─────────────────────────────────────────────────────────────┤
│  ❌ 步骤6: 健康检查失败                                       │
│      错误: Connection refused                                │
│                                                             │
│  AI分析错误原因:                                             │
│  1. 应用启动时间较长，需要更多等待                           │
│  2. 端口可能被占用                                           │
│  3. 配置文件可能有误                                         │
│                                                             │
│  AI提出调整方案:                                             │
│  ┌─────────────────────────────────────────────────────┐   │
│  │ 方案A: 延长等待时间到60秒              [推荐]        │   │
│  │ 方案B: 检查端口占用并清理                           │   │
│  │ 方案C: 回滚到备份版本                               │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
│  [ 采用方案A ]  [ 采用方案B ]  [ 采用方案C ]                 │
└─────────────────────────────────────────────────────────────┘
                        ↓
         [用户选择方案A，继续执行]
                        ↓
┌─────────────────────────────────────────────────────────────┐
│  第6阶段: 完成总结                                            │
├─────────────────────────────────────────────────────────────┤
│  ✅ 任务执行成功！                                            │
│                                                             │
│  执行摘要:                                                  │
│  ┌─────────────────────────────────────────────────────┐   │
│  │ 任务: 部署Spring Boot应用                           │   │
│  │ 耗时: 2分15秒                                        │   │
│  │ 步骤: 7个步骤全部完成                                │   │
│  │ 状态: ✅ 成功                                         │   │
│  │                                                      │   │
│  │ 关键操作:                                            │   │
│  │ • 备份旧版本: /backup/app-20251020.jar              │   │
│  │ • 停止进程: PID 1234                                 │   │
│  │ • 部署版本: v2.1.0                                   │   │
│  │ • 新进程: PID 5678                                   │   │
│  │                                                      │   │
│  │ 验证结果:                                            │   │
│  │ ✓ 应用健康检查通过                                   │   │
│  │ ✓ 版本验证: v2.1.0                                   │   │
│  │ ✓ 响应时间: 45ms                                     │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
│  [ 查看完整日志 ]  [ 保存报告 ]  [ 开始新任务 ]              │
└─────────────────────────────────────────────────────────────┘
```

### 4.3 安全机制设计

#### 4.3.1 多层安全防护

```
┌─────────────────────────────────────────────────────────┐
│ 第1层: 权限控制                                          │
├─────────────────────────────────────────────────────────┤
│ • 只有ADMIN和SUPER_ADMIN角色可以使用Agent模式           │
│ • DEVELOPER角色只能使用Ask模式                          │
│ • 可配置按服务器级别的权限                              │
└─────────────────────────────────────────────────────────┘
                        ↓
┌─────────────────────────────────────────────────────────┐
│ 第2层: 命令白名单/黑名单                                 │
├─────────────────────────────────────────────────────────┤
│ 黑名单（禁止执行）:                                     │
│ • rm -rf /                                              │
│ • dd if=/dev/zero of=/dev/sda                          │
│ • mkfs.*                                                │
│ • shutdown, reboot, halt                               │
│ • iptables -F                                           │
│                                                         │
│ 需确认的命令（高风险）:                                 │
│ • rm, mv, cp (涉及重要目录)                             │
│ • kill, killall                                         │
│ • systemctl stop/restart                                │
│ • apt-get remove, yum remove                            │
└─────────────────────────────────────────────────────────┘
                        ↓
┌─────────────────────────────────────────────────────────┐
│ 第3层: 执行计划审核                                      │
├─────────────────────────────────────────────────────────┤
│ • 所有Agent任务必须先生成计划                           │
│ • 用户必须明确批准计划                                  │
│ • 高风险步骤会标注⚠️警告                                │
│ • 用户可以修改或拒绝计划                                │
└─────────────────────────────────────────────────────────┘
                        ↓
┌─────────────────────────────────────────────────────────┐
│ 第4层: 实时监控与中断                                    │
├─────────────────────────────────────────────────────────┤
│ • 执行过程中用户可随时暂停/停止                         │
│ • 检测到异常立即暂停并请求用户决策                      │
│ • 超时保护（单步骤最长5分钟）                           │
│ • 资源监控（防止CPU/内存占用过高）                      │
└─────────────────────────────────────────────────────────┘
                        ↓
┌─────────────────────────────────────────────────────────┐
│ 第5层: 审计日志                                          │
├─────────────────────────────────────────────────────────┤
│ • 记录所有Agent任务的详细日志                           │
│ • 包括: 用户、时间、任务、计划、执行结果                │
│ • 可追溯、可审计                                        │
│ • 与UserActivityService集成                             │
└─────────────────────────────────────────────────────────┘
```

#### 4.3.2 危险命令检测示例

```java
/**
 * Agent命令安全检查器
 */
@Service
public class AgentCommandValidator {

    // 黑名单（直接拒绝）
    private static final List<Pattern> BLACKLIST = List.of(
        Pattern.compile("rm\\s+-rf\\s+/"),
        Pattern.compile("dd\\s+if=/dev/(zero|random)\\s+of=/dev/sd[a-z]"),
        Pattern.compile("mkfs\\."),
        Pattern.compile("shutdown|reboot|halt"),
        Pattern.compile("iptables\\s+-F"),
        Pattern.compile(">(\\s+)?/dev/sd[a-z]")
    );

    // 高风险命令（需要用户确认）
    private static final List<Pattern> HIGH_RISK = List.of(
        Pattern.compile("rm\\s+"),
        Pattern.compile("kill(all)?\\s+"),
        Pattern.compile("systemctl\\s+(stop|restart)"),
        Pattern.compile("(apt-get|yum|dnf)\\s+remove")
    );

    public ValidationResult validate(String command) {
        // 检查黑名单
        for (Pattern pattern : BLACKLIST) {
            if (pattern.matcher(command).find()) {
                return ValidationResult.denied(
                    "检测到危险命令，已自动拦截: " + command
                );
            }
        }

        // 检查高风险命令
        for (Pattern pattern : HIGH_RISK) {
            if (pattern.matcher(command).find()) {
                return ValidationResult.requireConfirmation(
                    "此命令存在风险，需要用户确认: " + command
                );
            }
        }

        return ValidationResult.approved();
    }
}
```

### 4.4 Agent任务类型

#### 4.4.1 预定义任务模板

为常见场景提供内置模板，提高执行可靠性：

| 任务类型 | 描述 | 执行步骤 |
|---------|------|---------|
| **deploy** | 应用部署 | 备份→停止→下载→启动→验证 |
| **health-check** | 健康巡检 | 磁盘→内存→CPU→服务→报告 |
| **backup** | 数据备份 | 检查空间→压缩→传输→验证 |
| **log-analysis** | 日志分析 | 提取→过滤→统计→报告 |
| **troubleshoot** | 故障排查 | 诊断→定位→修复→验证 |
| **cleanup** | 清理维护 | 临时文件→旧日志→缓存→磁盘 |
| **update** | 系统更新 | 检查→下载→安装→重启→验证 |
| **rollback** | 版本回滚 | 停止→恢复备份→启动→验证 |

#### 4.4.2 自定义任务

用户可以提出任意自然语言任务，AI会动态生成计划：

```
示例任务:
"帮我找出占用磁盘最大的前10个文件并清理超过30天的日志"

AI生成计划:
1. du -ah / | sort -rh | head -n 10
2. find /var/log -name "*.log" -mtime +30 -type f
3. [需确认] 删除找到的旧日志文件
4. df -h (验证空间释放)
```

---

## 5. 安全与风险控制

### 5.1 风险矩阵

| 风险类型 | 风险级别 | 影响范围 | 缓解措施 |
|---------|---------|---------|---------|
| **数据丢失** | 🔴 高 | 业务数据 | 强制备份、二次确认、操作审计 |
| **服务中断** | 🟡 中 | 业务可用性 | 健康检查、自动回滚、通知告警 |
| **权限滥用** | 🟡 中 | 系统安全 | 角色控制、操作日志、会话审计 |
| **命令注入** | 🔴 高 | 系统安全 | 命令白名单、参数校验、沙箱执行 |
| **资源耗尽** | 🟡 中 | 系统性能 | 超时控制、资源限制、监控告警 |
| **配置错误** | 🟢 低 | 功能异常 | 配置验证、默认值、错误提示 |

### 5.2 审计与合规

#### 5.2.1 操作审计日志

```java
/**
 * Agent操作审计记录
 */
@Entity
@Table(name = "agent_task_audit")
public class AgentTaskAudit {
    @Id
    @GeneratedValue
    private Long id;

    // 任务信息
    private String taskId;           // 任务唯一ID
    private String taskType;         // 任务类型
    private String taskDescription;  // 用户原始指令

    // 执行信息
    private String executionPlan;    // JSON格式的执行计划
    private String executedCommands; // 实际执行的命令列表
    private String executionResult;  // 执行结果
    private AgentTaskStatus status;  // 成功/失败/中断

    // 用户信息
    private Long userId;             // 执行用户
    private String username;         // 用户名
    private String userRole;         // 用户角色
    private String ipAddress;        // 用户IP

    // 服务器信息
    private Long serverId;           // 目标服务器
    private String serverName;       // 服务器名称
    private String terminalId;       // 终端会话ID

    // 时间信息
    private Instant startTime;       // 开始时间
    private Instant endTime;         // 结束时间
    private Long durationMs;         // 执行时长(毫秒)

    // 风险标记
    private RiskLevel riskLevel;     // 风险等级
    private Boolean userApproved;    // 用户是否批准
    private String warnings;         // 警告信息
}
```

#### 5.2.2 实时通知

重要操作触发实时通知：

```javascript
// 高风险操作通知
function notifyHighRiskOperation(task) {
    // 1. WebSocket推送给管理员
    adminNotificationService.send({
        type: 'agent-high-risk',
        user: currentUser,
        server: targetServer,
        operation: task.description,
        timestamp: new Date()
    });

    // 2. 邮件通知（可选）
    if (config.emailNotifications.enabled) {
        emailService.send({
            to: config.adminEmails,
            subject: '[警告] Agent执行高风险操作',
            body: formatOperationDetails(task)
        });
    }
}
```

---

## 6. 技术架构

### 6.1 后端架构

#### 6.1.1 新增服务层

```java
/**
 * Agent任务管理服务
 */
@Service
public class AgentTaskService {

    private final ChatService chatService;
    private final AgentCommandValidator commandValidator;
    private final SSHTerminalService terminalService;
    private final AgentTaskAuditRepository auditRepository;

    /**
     * 生成任务执行计划
     */
    public AgentExecutionPlan generatePlan(AgentTaskRequest request) {
        // 1. 使用ChatService分析任务
        ChatRequest chatRequest = buildPlanningRequest(request);
        String aiResponse = chatService.chat(chatRequest);

        // 2. 解析AI返回的计划（JSON格式）
        AgentExecutionPlan plan = parseExecutionPlan(aiResponse);

        // 3. 验证每个步骤的命令
        for (PlanStep step : plan.getSteps()) {
            ValidationResult validation = commandValidator.validate(step.getCommand());
            step.setValidationResult(validation);
        }

        // 4. 评估整体风险
        plan.setRiskLevel(assessRiskLevel(plan));

        return plan;
    }

    /**
     * 执行任务计划
     */
    public AgentExecutionResult executePlan(
            String planId,
            String terminalId,
            ExecutionCallback callback) {

        AgentExecutionPlan plan = getPlanById(planId);
        AgentExecutionResult result = new AgentExecutionResult(planId);

        // 开始审计记录
        AgentTaskAudit audit = auditRepository.save(
            createAuditRecord(plan, terminalId)
        );

        try {
            // 逐步执行
            for (int i = 0; i < plan.getSteps().size(); i++) {
                PlanStep step = plan.getSteps().get(i);

                // 回调：步骤开始
                callback.onStepStart(i, step);

                // 执行命令
                StepResult stepResult = executeStep(step, terminalId);
                result.addStepResult(stepResult);

                // 回调：步骤完成
                callback.onStepComplete(i, stepResult);

                // 检查是否失败
                if (!stepResult.isSuccess()) {
                    if (step.isRequired()) {
                        // 必需步骤失败，终止执行
                        throw new AgentExecutionException(
                            "关键步骤执行失败: " + step.getDescription()
                        );
                    } else {
                        // 可选步骤失败，记录并继续
                        logger.warn("可选步骤失败: {}", step.getDescription());
                    }
                }
            }

            result.setStatus(AgentTaskStatus.SUCCESS);

        } catch (AgentExecutionException e) {
            result.setStatus(AgentTaskStatus.FAILED);
            result.setErrorMessage(e.getMessage());

            // 尝试自适应修复
            AgentRecoveryPlan recoveryPlan = generateRecoveryPlan(plan, result);
            callback.onRecoveryRequired(recoveryPlan);

        } finally {
            // 更新审计记录
            audit.setEndTime(Instant.now());
            audit.setStatus(result.getStatus());
            audit.setExecutionResult(result.toJson());
            auditRepository.save(audit);
        }

        return result;
    }

    /**
     * 执行单个步骤
     */
    private StepResult executeStep(PlanStep step, String terminalId) {
        StepResult result = new StepResult(step.getCommand());

        try {
            // 通过SSH WebSocket发送命令
            terminalService.executeCommand(terminalId, step.getCommand());

            // 等待并收集输出
            String output = terminalService.waitForOutput(
                terminalId,
                step.getTimeoutSeconds()
            );

            result.setOutput(output);
            result.setSuccess(true);

        } catch (TimeoutException e) {
            result.setSuccess(false);
            result.setErrorMessage("命令执行超时");
        } catch (Exception e) {
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
        }

        return result;
    }
}
```

#### 6.1.2 新增API端点

```java
/**
 * Agent模式API控制器
 */
@RestController
@RequestMapping("/ai/agent")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class AgentController {

    private final AgentTaskService agentTaskService;

    /**
     * 生成执行计划
     */
    @PostMapping("/plan")
    public ResponseEntity<AgentExecutionPlan> generatePlan(
            @RequestBody @Valid AgentTaskRequest request) {

        AgentExecutionPlan plan = agentTaskService.generatePlan(request);
        return ResponseEntity.ok(plan);
    }

    /**
     * 执行计划（流式返回进度）
     */
    @PostMapping(value = "/execute/{planId}",
                 produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter executePlan(
            @PathVariable String planId,
            @RequestParam String terminalId) {

        SseEmitter emitter = new SseEmitter(Duration.ofMinutes(10).toMillis());

        CompletableFuture.runAsync(() -> {
            try {
                agentTaskService.executePlan(planId, terminalId,
                    new ExecutionCallback() {
                        @Override
                        public void onStepStart(int stepIndex, PlanStep step) {
                            sendEvent(emitter, "step-start", Map.of(
                                "stepIndex", stepIndex,
                                "description", step.getDescription()
                            ));
                        }

                        @Override
                        public void onStepComplete(int stepIndex, StepResult result) {
                            sendEvent(emitter, "step-complete", Map.of(
                                "stepIndex", stepIndex,
                                "success", result.isSuccess(),
                                "output", result.getOutput()
                            ));
                        }

                        @Override
                        public void onRecoveryRequired(AgentRecoveryPlan plan) {
                            sendEvent(emitter, "recovery-required", plan);
                        }
                    }
                );

                emitter.complete();

            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    /**
     * 暂停/恢复执行
     */
    @PostMapping("/pause/{planId}")
    public ResponseEntity<Void> pauseExecution(@PathVariable String planId) {
        agentTaskService.pauseExecution(planId);
        return ResponseEntity.ok().build();
    }

    /**
     * 停止执行
     */
    @PostMapping("/stop/{planId}")
    public ResponseEntity<Void> stopExecution(@PathVariable String planId) {
        agentTaskService.stopExecution(planId);
        return ResponseEntity.ok().build();
    }

    /**
     * 获取执行历史
     */
    @GetMapping("/history")
    public ResponseEntity<Page<AgentTaskAudit>> getHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<AgentTaskAudit> history = agentTaskService.getHistory(
            PageRequest.of(page, size)
        );
        return ResponseEntity.ok(history);
    }
}
```

### 6.2 前端架构

#### 6.2.1 模式切换组件

```javascript
class AiModeManager {
    constructor() {
        this.currentMode = 'ask';  // 'ask' | 'agent'
        this.askPanel = new AskModePanel();
        this.agentPanel = new AgentModePanel();
    }

    switchMode(mode) {
        if (this.currentMode === mode) return;

        // 隐藏当前模式
        this.getCurrentPanel().hide();

        // 切换模式
        this.currentMode = mode;

        // 显示新模式
        this.getCurrentPanel().show();

        // 更新UI
        this.updateModeIndicator();
    }

    getCurrentPanel() {
        return this.currentMode === 'ask'
            ? this.askPanel
            : this.agentPanel;
    }
}
```

#### 6.2.2 Agent执行流程控制

```javascript
class AgentModePanel {
    async executeTask(userInstruction) {
        // 1. 生成执行计划
        const plan = await this.generatePlan(userInstruction);

        // 2. 显示计划给用户审核
        const approved = await this.showPlanForApproval(plan);
        if (!approved) return;

        // 3. 开始执行
        this.showExecutionProgress();

        // 4. 监听执行事件
        const eventSource = new EventSource(
            `/ai/agent/execute/${plan.id}?terminalId=${terminalId}`
        );

        eventSource.addEventListener('step-start', (e) => {
            const data = JSON.parse(e.data);
            this.updateProgress(data.stepIndex, 'running');
        });

        eventSource.addEventListener('step-complete', (e) => {
            const data = JSON.parse(e.data);
            this.updateProgress(data.stepIndex,
                data.success ? 'success' : 'failed'
            );
        });

        eventSource.addEventListener('recovery-required', (e) => {
            const recoveryPlan = JSON.parse(e.data);
            this.showRecoveryOptions(recoveryPlan);
        });

        eventSource.onerror = (e) => {
            this.showError('执行过程中发生错误');
            eventSource.close();
        };
    }

    async generatePlan(instruction) {
        const response = await fetch('/ai/agent/plan', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                [csrfHeader]: csrfToken
            },
            body: JSON.stringify({
                instruction: instruction,
                terminalContext: terminalManager.getActiveTerminalContext(),
                serverId: terminalManager.getCurrentServerId()
            })
        });

        return await response.json();
    }
}
```

---

## 7. UI/UX设计

### 7.1 模式切换UI

```
┌─────────────────────────────────────────────────────────┐
│ AI助手                                    [📋 Ask][🤖 Agent]│
├─────────────────────────────────────────────────────────┤
│                                                         │
│  [根据选中的模式显示不同的面板]                          │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

### 7.2 Agent模式界面

#### 7.2.1 任务输入界面

```
┌─────────────────────────────────────────────────────────┐
│ 🤖 Agent模式 - 自动化执行代理              [切换到Ask]   │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  描述您想要执行的任务:                                   │
│  ┌─────────────────────────────────────────────────┐   │
│  │ 例如：                                           │   │
│  │ • "帮我部署最新版本的应用"                        │   │
│  │ • "执行系统健康检查并生成报告"                    │   │
│  │ • "清理30天前的日志文件"                          │   │
│  │ • "备份数据库到/backup目录"                       │   │
│  └─────────────────────────────────────────────────┘   │
│                                                         │
│  您的任务:                                              │
│  ┌─────────────────────────────────────────────────┐   │
│  │ [输入任务描述...]                                 │   │
│  │                                                  │   │
│  │                                                  │   │
│  └─────────────────────────────────────────────────┘   │
│                                                         │
│  [ 生成执行计划 ]                                       │
│                                                         │
│  ⚙️ 高级选项:                                            │
│  ☐ 允许高风险操作（需要额外确认）                        │
│  ☐ 失败时自动重试                                       │
│  ☐ 发送邮件通知                                         │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

#### 7.2.2 计划审核界面

```
┌─────────────────────────────────────────────────────────┐
│ 执行计划审核                           [修改] [取消]     │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  任务: 部署Spring Boot应用到生产环境                     │
│  预计耗时: 约2-3分钟                                     │
│  风险级别: ⚠️ 中等（涉及服务重启）                       │
│                                                         │
│  执行步骤:                                              │
│  ┌─────────────────────────────────────────────────┐   │
│  │ ✓ 步骤1: 备份当前版本                 [必需]     │   │
│  │   cp /app/current.jar /backup/app-20251020.jar  │   │
│  │   预计: 5秒                                      │   │
│  ├─────────────────────────────────────────────────┤   │
│  │ ⚠️ 步骤2: 停止旧应用                   [必需]     │   │
│  │   kill -15 1234                                 │   │
│  │   预计: 10秒                                     │   │
│  │   警告: 此步骤将导致服务中断                     │   │
│  ├─────────────────────────────────────────────────┤   │
│  │ ✓ 步骤3: 下载新版本                   [必需]     │   │
│  │   wget https://repo/app-v2.1.0.jar              │   │
│  │   预计: 20秒                                     │   │
│  ├─────────────────────────────────────────────────┤   │
│  │ ✓ 步骤4: 启动新应用                   [必需]     │   │
│  │   java -jar /app/current.jar &                  │   │
│  │   预计: 5秒                                      │   │
│  ├─────────────────────────────────────────────────┤   │
│  │ ✓ 步骤5: 健康检查                     [必需]     │   │
│  │   curl -f http://localhost:8080/health          │   │
│  │   预计: 30秒（等待应用启动）                     │   │
│  ├─────────────────────────────────────────────────┤   │
│  │ ✓ 步骤6: 验证版本                     [可选]     │   │
│  │   curl http://localhost:8080/version            │   │
│  │   预计: 2秒                                      │   │
│  └─────────────────────────────────────────────────┘   │
│                                                         │
│  [ 批准并执行 ]  [ 修改计划 ]  [ 取消 ]                  │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

#### 7.2.3 执行监控界面

```
┌─────────────────────────────────────────────────────────┐
│ 正在执行: 部署Spring Boot应用           [ ⏸️ ][ ⏹️ ]     │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  总体进度: ████████████░░░░░░ 60% (3/6 步骤完成)       │
│  已用时间: 00:45 / 预计: 02:30                          │
│                                                         │
│  ┌─────────────────────────────────────────────────┐   │
│  │ ✅ 步骤1: 备份当前版本                            │   │
│  │    完成于 13:51:05 (用时 5秒)                    │   │
│  │    ✓ 备份文件: /backup/app-20251020.jar          │   │
│  ├─────────────────────────────────────────────────┤   │
│  │ ✅ 步骤2: 停止旧应用                              │   │
│  │    完成于 13:51:15 (用时 8秒)                    │   │
│  │    ✓ 进程 1234 已终止                            │   │
│  ├─────────────────────────────────────────────────┤   │
│  │ ✅ 步骤3: 下载新版本                              │   │
│  │    完成于 13:51:35 (用时 18秒)                   │   │
│  │    ✓ 已下载 app-v2.1.0.jar (13.2 MB)             │   │
│  ├─────────────────────────────────────────────────┤   │
│  │ ⏳ 步骤4: 启动新应用                              │   │
│  │    正在执行中...                                 │   │
│  │    > nohup java -jar /app/current.jar &         │   │
│  ├─────────────────────────────────────────────────┤   │
│  │ ⏸️ 步骤5: 健康检查                                │   │
│  │    等待步骤4完成                                 │   │
│  ├─────────────────────────────────────────────────┤   │
│  │ ⏸️ 步骤6: 验证版本                                │   │
│  │    等待步骤5完成                                 │   │
│  └─────────────────────────────────────────────────┘   │
│                                                         │
│  实时输出:                                              │
│  ┌─────────────────────────────────────────────────┐   │
│  │ [2025-10-20 13:51:45] Starting application...   │   │
│  │ [2025-10-20 13:51:46] Loading configuration...  │   │
│  │ [2025-10-20 13:51:48] Connecting to database... │   │
│  │ [2025-10-20 13:51:50] Application started.      │   │
│  │ _                                                │   │
│  └─────────────────────────────────────────────────┘   │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

#### 7.2.4 完成总结界面

```
┌─────────────────────────────────────────────────────────┐
│ ✅ 任务执行成功                        [ 关闭 ][ 详情 ] │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  任务: 部署Spring Boot应用                              │
│  耗时: 2分15秒                                          │
│  完成时间: 2025-10-20 13:53:20                          │
│                                                         │
│  ╔════════════════════════════════════════════════╗   │
│  ║            执行摘要                             ║   │
│  ╠════════════════════════════════════════════════╣   │
│  ║ 总步骤: 6                                       ║   │
│  ║ 成功: 6                                         ║   │
│  ║ 失败: 0                                         ║   │
│  ║ 跳过: 0                                         ║   │
│  ╚════════════════════════════════════════════════╝   │
│                                                         │
│  关键操作:                                              │
│  • 备份文件: /backup/app-20251020.jar (12.5 MB)        │
│  • 停止进程: PID 1234                                   │
│  • 部署版本: v2.1.0                                     │
│  • 新进程: PID 5678                                     │
│                                                         │
│  验证结果:                                              │
│  ✓ 应用健康检查通过                                     │
│  ✓ 版本验证: v2.1.0                                     │
│  ✓ 响应时间: 45ms                                       │
│  ✓ 内存使用: 512MB / 2GB                                │
│                                                         │
│  [ 查看完整日志 ]  [ 保存报告 ]  [ 开始新任务 ]          │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

---

## 8. 实施路线图

### Phase 1: Ask模式实现 (2天)

- [ ] **Day 1**: 基础功能
  - [ ] 模式切换UI设计与实现
  - [ ] Ask模式聊天界面
  - [ ] 上下文提取功能
  - [ ] 集成现有ChatService API

- [ ] **Day 2**: 功能增强
  - [ ] 代码高亮显示
  - [ ] 命令快速复制功能
  - [ ] 多轮对话历史管理
  - [ ] 测试与优化

### Phase 2: Agent模式基础 (3天)

- [ ] **Day 3**: 后端服务
  - [ ] AgentTaskService实现
  - [ ] AgentCommandValidator实现
  - [ ] 执行计划生成逻辑
  - [ ] 单元测试

- [ ] **Day 4**: API与安全
  - [ ] AgentController REST API
  - [ ] 权限控制实现
  - [ ] 审计日志记录
  - [ ] 集成测试

- [ ] **Day 5**: 前端UI
  - [ ] Agent模式界面
  - [ ] 任务输入组件
  - [ ] 计划审核界面
  - [ ] 执行进度监控

### Phase 3: Agent模式高级 (2天)

- [ ] **Day 6**: 执行引擎
  - [ ] 与SSH WebSocket集成
  - [ ] 实时输出监控
  - [ ] 错误处理与重试
  - [ ] 暂停/停止功能

- [ ] **Day 7**: 智能特性
  - [ ] 自适应错误恢复
  - [ ] 预定义任务模板
  - [ ] 执行历史查询
  - [ ] 性能优化

### Phase 4: 测试与文档 (1天)

- [ ] **Day 8**:
  - [ ] 端到端测试
  - [ ] 安全测试
  - [ ] 用户文档编写
  - [ ] 部署指南

**总计工时**: 8天

---

## 附录: 配置示例

### A.1 Agent权限配置

```yaml
# application.yml
ai:
  agent:
    enabled: true
    # 允许使用Agent模式的角色
    allowed-roles:
      - ADMIN
      - SUPER_ADMIN

    # 命令黑名单
    command-blacklist:
      - "rm -rf /"
      - "dd if=/dev/zero"
      - "mkfs.*"
      - "shutdown"
      - "reboot"

    # 高风险命令（需确认）
    high-risk-commands:
      - "rm.*"
      - "kill.*"
      - "systemctl (stop|restart)"
      - "(apt-get|yum) remove"

    # 执行限制
    execution:
      max-steps: 20                # 单个任务最多步骤数
      step-timeout-seconds: 300    # 单步骤超时
      task-timeout-minutes: 10     # 总任务超时
      max-concurrent-tasks: 3      # 最大并发任务数

    # 审计配置
    audit:
      enabled: true
      retention-days: 90           # 审计日志保留天数
      email-notifications: true    # 邮件通知
      admin-emails:
        - admin@example.com
```

### A.2 预定义任务模板

```json
{
  "templates": [
    {
      "id": "deploy-spring-boot",
      "name": "部署Spring Boot应用",
      "description": "标准的Spring Boot应用部署流程",
      "steps": [
        {
          "name": "备份",
          "command": "cp ${APP_PATH}/current.jar ${BACKUP_PATH}/app-$(date +%Y%m%d).jar",
          "required": true
        },
        {
          "name": "停止",
          "command": "kill -15 $(cat ${APP_PATH}/app.pid)",
          "required": true
        },
        {
          "name": "下载",
          "command": "wget ${DOWNLOAD_URL} -O ${APP_PATH}/new.jar",
          "required": true
        },
        {
          "name": "替换",
          "command": "mv ${APP_PATH}/new.jar ${APP_PATH}/current.jar",
          "required": true
        },
        {
          "name": "启动",
          "command": "nohup java -jar ${APP_PATH}/current.jar > ${APP_PATH}/app.log 2>&1 & echo $! > ${APP_PATH}/app.pid",
          "required": true
        },
        {
          "name": "健康检查",
          "command": "for i in {1..30}; do curl -f http://localhost:${APP_PORT}/health && break; sleep 2; done",
          "required": true
        }
      ]
    }
  ]
}
```

---

**文档版本**: v1.0
**最后更新**: 2025-10-20
**状态**: ✅ 需求分析与设计完成
**下一步**: 等待评审确认后开始实施
