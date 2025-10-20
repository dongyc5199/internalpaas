/**
 * AI助手管理类
 * 支持Ask模式（智能问答）和Agent模式（自动化执行，待实现）
 */
class AiAssistant {
    constructor(terminalManager) {
        this.terminalManager = terminalManager;
        this.currentMode = 'ask'; // 'ask' or 'agent'
        this.isOpen = false;
        this.messages = [];
        this.isStreaming = false;
        this.currentChatId = null;

        this.init();
    }

    init() {
        this.createUI();
        this.bindEvents();
    }

    createUI() {
        // 创建切换按钮
        const toggleBtn = document.createElement('button');
        toggleBtn.className = 'ai-toggle-btn';
        toggleBtn.innerHTML = '🤖';
        toggleBtn.title = 'AI助手 (Ctrl+Shift+A)';
        toggleBtn.id = 'aiToggleBtn';
        document.body.appendChild(toggleBtn);

        // 创建AI助手面板
        const panel = document.createElement('div');
        panel.className = 'ai-assistant-panel';
        panel.id = 'aiAssistantPanel';
        panel.innerHTML = `
            <!-- 头部 -->
            <div class="ai-assistant-header">
                <div class="ai-assistant-title">
                    <span class="ai-assistant-icon">🤖</span>
                    <span>AI助手</span>
                </div>
                <button class="btn btn-sm btn-outline-light" onclick="aiAssistant.close()">
                    ✕
                </button>
            </div>

            <!-- 模式切换 -->
            <div class="ai-mode-switcher">
                <button class="ai-mode-btn active" data-mode="ask">
                    <span>💬</span>
                    <span>Ask模式</span>
                </button>
                <button class="ai-mode-btn disabled" data-mode="agent" title="Agent模式即将上线">
                    <span>✨</span>
                    <span>Agent模式</span>
                </button>
            </div>

            <!-- Ask模式内容 -->
            <div class="ai-chat-container" id="aiChatContainer">
                <div class="ai-chat-messages" id="aiChatMessages">
                    <div class="ai-empty-state">
                        <div style="font-size: 48px; margin-bottom: 16px;">🤖</div>
                        <p>你好！我是AI助手。<br>我可以帮你理解终端输出、解释错误、提供命令建议。</p>
                    </div>
                </div>

                <div class="ai-chat-input-container">
                    <!-- 模型选择：去掉自动包含终端上下文，改为显式选择模型 -->
                    <div class="ai-model-selector" style="margin-bottom:8px; display:flex; gap:8px; align-items:center;">
                        <label for="aiModelSelector" style="font-size:12px; color:#bbb;">模型</label>
                        <select id="aiModelSelector" style="padding:4px 8px; font-size:13px;">
                            <option value="kimi-k2-0905-preview">Kimi K2</option>
                            <option value="echo">Echo (测试)</option>
                        </select>
                    </div>
                    <div class="ai-input-wrapper">
                        <textarea
                            id="aiChatInput"
                            class="ai-chat-input"
                            placeholder="输入你的问题..."
                            rows="2"
                        ></textarea>
                        <button id="aiSendBtn" class="ai-send-btn">
                            ➤
                        </button>
                    </div>
                </div>
            </div>
        `;
        document.body.appendChild(panel);
        // 异步加载后端提供的模型列表（若失败，保留内置选项）
        try {
            // call asynchronously but don't await here to avoid blocking UI creation
            this.loadModels();
        } catch (e) {
            // ignore - loadModels handles its own errors
        }
    }

    bindEvents() {
        // 切换按钮事件
        document.getElementById('aiToggleBtn').addEventListener('click', () => {
            this.toggle();
        });

        // 模式切换事件
        document.querySelectorAll('.ai-mode-btn').forEach(btn => {
            btn.addEventListener('click', (e) => {
                if (!btn.classList.contains('disabled')) {
                    this.switchMode(btn.dataset.mode);
                }
            });
        });

        // 发送按钮事件
        document.getElementById('aiSendBtn').addEventListener('click', () => {
            this.sendMessage();
        });

        // 输入框事件
        const input = document.getElementById('aiChatInput');
        input.addEventListener('keydown', (e) => {
            // Ctrl+Enter 或 Cmd+Enter 发送消息
            if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') {
                e.preventDefault();
                this.sendMessage();
            }
        });

        // 全局快捷键 Ctrl+Shift+A 切换面板
        document.addEventListener('keydown', (e) => {
            if (e.ctrlKey && e.shiftKey && e.key === 'A') {
                e.preventDefault();
                this.toggle();
            }
        });

        // 自动调整输入框高度
        input.addEventListener('input', () => {
            input.style.height = 'auto';
            input.style.height = Math.min(input.scrollHeight, 120) + 'px';
        });
    }

    async loadModels() {
        const selector = document.getElementById('aiModelSelector');
        if (!selector) return;

        try {
            const resp = await fetch('/api/ai/models', { method: 'GET', headers: { 'Accept': 'application/json' } });
            if (!resp.ok) {
                console.warn('加载模型列表失败：', resp.status, resp.statusText);
                return;
            }

            const models = await resp.json();
            if (!Array.isArray(models) || models.length === 0) {
                return;
            }

            // 清空现有选项
            selector.innerHTML = '';

            // 映射已知模型到更友好的显示名
            const friendly = (id) => {
                if (id && id.toLowerCase().includes('kimi')) return 'Kimi K2';
                if (id === 'echo') return 'Echo (测试)';
                return id;
            };

            models.forEach(m => {
                const opt = document.createElement('option');
                opt.value = m;
                opt.textContent = friendly(m);
                selector.appendChild(opt);
            });

            // 如果当前没有选中，选择第一个
            if (!selector.value && selector.options.length > 0) {
                selector.selectedIndex = 0;
            }
        } catch (err) {
            console.warn('获取模型列表时出错：', err);
            // 保持内置选项
        }
    }

    toggle() {
        if (this.isOpen) {
            this.close();
        } else {
            this.open();
        }
    }

    open() {
        const panel = document.getElementById('aiAssistantPanel');
        const toggleBtn = document.getElementById('aiToggleBtn');
        panel.classList.add('open');
        toggleBtn.classList.add('active');
        this.isOpen = true;

        // 聚焦到输入框
        setTimeout(() => {
            document.getElementById('aiChatInput').focus();
        }, 300);
    }

    close() {
        const panel = document.getElementById('aiAssistantPanel');
        const toggleBtn = document.getElementById('aiToggleBtn');
        panel.classList.remove('open');
        toggleBtn.classList.remove('active');
        this.isOpen = false;
    }

    switchMode(mode) {
        if (this.currentMode === mode) return;

        this.currentMode = mode;

        // 更新按钮状态
        document.querySelectorAll('.ai-mode-btn').forEach(btn => {
            if (btn.dataset.mode === mode) {
                btn.classList.add('active');
            } else {
                btn.classList.remove('active');
            }
        });

        // TODO: 切换到不同模式的UI
        if (mode === 'agent') {
            // Agent模式UI（待实现）
            console.log('Agent模式即将上线');
        }
    }

    async sendMessage() {
        const input = document.getElementById('aiChatInput');
        const message = input.value.trim();

        if (!message || this.isStreaming) {
            return;
        }

        // 清空输入框
        input.value = '';
        input.style.height = 'auto';

        // 添加用户消息到界面
        this.addMessage('user', message);

        // 获取终端上下文
        const terminalContext = this.extractTerminalContext();

        // 设置流式状态
        this.isStreaming = true;
        this.updateSendButton(true);

        // 添加加载指示器
        const loadingId = this.addLoadingIndicator();

        try {
            // 发送请求到后端API
            await this.streamChat(message, terminalContext, loadingId);
        } catch (error) {
            console.error('AI请求失败:', error);
            this.removeMessage(loadingId);
            this.addMessage('assistant', `抱歉，请求失败：${error.message}`);
        } finally {
            this.isStreaming = false;
            this.updateSendButton(false);
        }
    }

    extractTerminalContext() {
        // 从当前活动的终端提取最近100行输出
        if (!this.terminalManager || !this.terminalManager.activeTerminalId) {
            return '';
        }

        const session = this.terminalManager.terminals.get(this.terminalManager.activeTerminalId);
        if (!session || !session.terminal) {
            return '';
        }

        // 获取终端缓冲区内容
        const buffer = session.terminal.buffer.active;
        const lines = [];
        const totalLines = buffer.length;
        const startLine = Math.max(0, totalLines - 100);

        for (let i = startLine; i < totalLines; i++) {
            const line = buffer.getLine(i);
            if (line) {
                lines.push(line.translateToString(true));
            }
        }

        return lines.join('\n');
    }

    async streamChat(message, terminalContext, loadingId) {
        const chatId = this.currentChatId || `chat-${Date.now()}`;
        this.currentChatId = chatId;

        const response = await fetch('/ai/chat/stream', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            // 不自动提交终端上下文；使用 UI 中的模型选择
            body: JSON.stringify({
                chatId: chatId,
                message: message,
                model: (
                    document.getElementById('aiModelSelector') &&
                    document.getElementById('aiModelSelector').value
                ) || 'kimi-k2-0905-preview'
            })
        });

        if (!response.ok) {
            throw new Error(`HTTP ${response.status}: ${response.statusText}`);
        }

        // 移除加载指示器
        this.removeMessage(loadingId);

        // 创建助手消息元素
        const assistantMsgId = this.addMessage('assistant', '');
        const contentElement = document.querySelector(`[data-message-id="${assistantMsgId}"] .ai-message-content`);

        // 读取SSE流
        const reader = response.body.getReader();
        const decoder = new TextDecoder();
        let buffer = '';
        let fullContent = '';

        while (true) {
            const { done, value } = await reader.read();

            if (done) {
                break;
            }

            buffer += decoder.decode(value, { stream: true });
            const lines = buffer.split('\n');
            buffer = lines.pop() || '';

            for (const line of lines) {
                if (line.startsWith('event:')) {
                    const eventType = line.substring(6).trim();
                    continue;
                }

                if (line.startsWith('data:')) {
                    const data = line.substring(5).trim();

                    if (data) {
                        fullContent += data;
                        // 实时更新消息内容，支持Markdown渲染
                        this.renderMessageContent(contentElement, fullContent);
                        // 滚动到底部
                        this.scrollToBottom();
                    }
                }
            }
        }

        // 处理代码块中的复制按钮
        this.setupCodeCopyButtons(contentElement);
    }

    addMessage(role, content) {
        const messagesContainer = document.getElementById('aiChatMessages');

        // 移除空状态
        const emptyState = messagesContainer.querySelector('.ai-empty-state');
        if (emptyState) {
            emptyState.remove();
        }

        const messageId = `msg-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
        const messageDiv = document.createElement('div');
        messageDiv.className = `ai-message ${role}`;
        messageDiv.dataset.messageId = messageId;

        const now = new Date();
        const timeStr = now.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' });

        messageDiv.innerHTML = `
            <div class="ai-message-content">${this.escapeHtml(content)}</div>
            <div class="ai-message-time">${timeStr}</div>
        `;

        messagesContainer.appendChild(messageDiv);
        this.scrollToBottom();

        this.messages.push({ id: messageId, role, content, time: now });

        return messageId;
    }

    removeMessage(messageId) {
        const message = document.querySelector(`[data-message-id="${messageId}"]`);
        if (message) {
            message.remove();
        }
        this.messages = this.messages.filter(m => m.id !== messageId);
    }

    addLoadingIndicator() {
        const messagesContainer = document.getElementById('aiChatMessages');
        const messageId = `loading-${Date.now()}`;

        const loadingDiv = document.createElement('div');
        loadingDiv.className = 'ai-message assistant';
        loadingDiv.dataset.messageId = messageId;
        loadingDiv.innerHTML = `
            <div class="ai-message-content">
                <div class="ai-loading">
                    <div class="ai-loading-dot"></div>
                    <div class="ai-loading-dot"></div>
                    <div class="ai-loading-dot"></div>
                </div>
            </div>
        `;

        messagesContainer.appendChild(loadingDiv);
        this.scrollToBottom();

        return messageId;
    }

    renderMessageContent(element, content) {
        // 简单的Markdown渲染：代码块、行内代码、换行
        let html = this.escapeHtml(content);

        // 代码块: ```language\ncode\n```
        html = html.replace(/```(\w+)?\n([\s\S]*?)```/g, (match, lang, code) => {
            lang = lang || 'plaintext';
            return `
                <div class="ai-code-block">
                    <div class="ai-code-header">
                        <span class="ai-code-lang">${this.escapeHtml(lang)}</span>
                        <button class="ai-code-copy-btn" data-code="${this.escapeHtml(code)}">
                            📋 复制
                        </button>
                    </div>
                    <div class="ai-code-content">
                        <pre><code>${this.escapeHtml(code)}</code></pre>
                    </div>
                </div>
            `;
        });

        // 行内代码: `code`
        html = html.replace(/`([^`]+)`/g, '<code style="background: rgba(255,255,255,0.1); padding: 2px 6px; border-radius: 3px; font-family: monospace; font-size: 12px;">$1</code>');

        // 换行
        html = html.replace(/\n/g, '<br>');

        element.innerHTML = html;
    }

    setupCodeCopyButtons(container) {
        const copyButtons = container.querySelectorAll('.ai-code-copy-btn');
        copyButtons.forEach(btn => {
            btn.addEventListener('click', async () => {
                const code = btn.dataset.code;
                try {
                    await navigator.clipboard.writeText(code);
                    const originalHtml = btn.innerHTML;
                    btn.innerHTML = '✓ 已复制';
                    btn.classList.add('copied');

                    setTimeout(() => {
                        btn.innerHTML = originalHtml;
                        btn.classList.remove('copied');
                    }, 2000);
                } catch (error) {
                    console.error('复制失败:', error);
                }
            });
        });
    }

    updateSendButton(isLoading) {
        const sendBtn = document.getElementById('aiSendBtn');
        if (isLoading) {
            sendBtn.disabled = true;
            sendBtn.innerHTML = '⏳';
        } else {
            sendBtn.disabled = false;
            sendBtn.innerHTML = '➤';
        }
    }

    scrollToBottom() {
        const messagesContainer = document.getElementById('aiChatMessages');
        messagesContainer.scrollTop = messagesContainer.scrollHeight;
    }

    escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }

    clearChat() {
        this.messages = [];
        this.currentChatId = null;
        const messagesContainer = document.getElementById('aiChatMessages');
        messagesContainer.innerHTML = `
            <div class="ai-empty-state">
                <div style="font-size: 48px; margin-bottom: 16px;">🤖</div>
                <p>你好！我是AI助手。<br>我可以帮你理解终端输出、解释错误、提供命令建议。</p>
            </div>
        `;
    }
}

// 全局变量，在终端管理器初始化后创建
let aiAssistant = null;
