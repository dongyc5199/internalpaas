/**
 * TypewriterRenderer - 打字机渲染器
 * 实现字符级流式动画，逐字显示文本
 */
class TypewriterRenderer {
    constructor(targetElement, config = {}) {
        this.target = targetElement;
        this.charsPerSecond = config.speed || 50;  // 默认 50 字符/秒
        this.onComplete = config.onComplete || null;
        this.text = '';
        this.position = 0;
        this.isPaused = false;
        this.isComplete = false;
        this.lastFrameTime = 0;
    }

    start(fullText) {
        this.text = fullText;
        this.position = 0;
        this.isComplete = false;
        this.lastFrameTime = performance.now();
        this._renderFrame();
    }

    _renderFrame() {
        if (this.isPaused || this.isComplete) return;

        const now = performance.now();
        const deltaTime = (now - this.lastFrameTime) / 1000;  // 转换为秒
        const charsToAdd = Math.floor(deltaTime * this.charsPerSecond);

        if (charsToAdd > 0) {
            this.position = Math.min(this.position + charsToAdd, this.text.length);
            this.target.textContent = this.text.substring(0, this.position);
            this.lastFrameTime = now;

            // 自动滚动
            const messagesContainer = document.getElementById('aiChatMessages');
            if (messagesContainer) {
                messagesContainer.scrollTop = messagesContainer.scrollHeight;
            }
        }

        if (this.position < this.text.length) {
            requestAnimationFrame(() => this._renderFrame());
        } else {
            this.isComplete = true;
            if (this.onComplete) this.onComplete();
        }
    }

    pause() {
        this.isPaused = true;
    }

    resume() {
        if (this.isPaused && !this.isComplete) {
            this.isPaused = false;
            this.lastFrameTime = performance.now();
            this._renderFrame();
        }
    }

    skipToEnd() {
        this.position = this.text.length;
        this.target.textContent = this.text;
        this.isComplete = true;
        if (this.onComplete) this.onComplete();
    }
}

/**
 * 辅助工具函数 - 流式输出相关
 */

/**
 * 去除 HTML 标签，返回纯文本
 */
function stripHtmlTags(html) {
    const temp = document.createElement('div');
    temp.innerHTML = html;
    return temp.textContent || temp.innerText || '';
}

/**
 * 获取用户偏好的流式速度（字符/秒）
 */
function getUserStreamingSpeed() {
    const SPEED_MAP = {
        'slow': 30,
        'normal': 50,
        'fast': 80
    };
    const speedSetting = localStorage.getItem('ai.streaming.speed') || 'normal';
    return SPEED_MAP[speedSetting] || SPEED_MAP['normal'];
}

/**
 * 检查是否启用流式输出
 */
function isStreamingEnabled() {
    const enabled = localStorage.getItem('ai.streaming.enabled');
    return enabled !== 'false';  // 默认启用
}

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
        this.activeTypewriter = null; // 当前活跃的打字机实例
        this.contextSessions = []; // 选中的SSH会话上下文
        this.isHashtagMenuOpen = false; // # 菜单是否打开

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
            <!-- 窄标题栏 -->
            <div class="ai-mini-topbar">
                <div class="ai-mini-left">
                    <span class="ai-icon">🤖</span>
                    <span class="ai-title">AI助手</span>
                </div>
                <div class="ai-mini-right">
                    <button class="ai-icon-btn" id="btnNewSession" title="新建会话">
                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                            <path d="M12 5v14M5 12h14"/>
                        </svg>
                    </button>
                    <button class="ai-icon-btn" id="btnHistory" title="历史会话">
                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                            <path d="M3 12a9 9 0 1 0 9-9 9.75 9.75 0 0 0-6.74 2.74L3 8"/>
                            <path d="M3 3v5h5"/>
                        </svg>
                    </button>
                    <button class="ai-icon-btn" id="btnSettings" title="设置">
                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                            <circle cx="12" cy="12" r="3"/>
                            <path d="M12 1v6m0 6v6M5.64 5.64l4.24 4.24m5.66 5.66l4.24 4.24M1 12h6m6 0h6M5.64 18.36l4.24-4.24m5.66-5.66l4.24-4.24"/>
                        </svg>
                    </button>
                    <button class="ai-icon-btn" id="btnClosePanel" title="关闭" onclick="aiAssistant.close()">
                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                            <path d="M18 6L6 18M6 6l12 12"/>
                        </svg>
                    </button>
                </div>
            </div>

            <!-- 消息区域 -->
            <div class="ai-chat-messages" id="aiChatMessages">
                <div class="ai-empty-state">
                    <div style="font-size: 48px; margin-bottom: 16px;">🤖</div>
                    <p>你好！我是AI助手。<br>我可以帮你理解终端输出、解释错误、提供命令建议。</p>
                </div>
            </div>

            <!-- 设置抽屉（默认隐藏） -->
            <div class="ai-settings-drawer" id="aiSettingsDrawer" style="display: none;">
                <div class="ai-settings-content">
                    <h3 style="margin: 0 0 16px 0; font-size: 14px; font-weight: 600; color: rgba(255,255,255,0.9);">⚙️ 聊天设置</h3>
                    <div style="margin-bottom: 16px;">
                        <label style="display: flex; align-items: center; cursor: pointer; font-size: 13px; color: rgba(255,255,255,0.9);">
                            <input type="checkbox" id="aiStreamingEnabled" checked style="margin-right: 8px; cursor: pointer;">
                            <span>启用打字机效果</span>
                        </label>
                        <p style="margin: 6px 0 0 24px; font-size: 12px; color: rgba(255,255,255,0.6);">
                            AI 回复将逐字符流式显示
                        </p>
                    </div>
                    <div style="margin-bottom: 16px;">
                        <label for="aiStreamingSpeed" style="display: block; margin-bottom: 6px; font-size: 13px; color: rgba(255,255,255,0.9);">流式速度</label>
                        <select id="aiStreamingSpeed" style="padding: 6px 10px; font-size: 13px; background: rgba(255,255,255,0.1); color: #fff; border: 1px solid rgba(255,255,255,0.2); border-radius: 4px; cursor: pointer; width: 100%;">
                            <option value="slow">慢速 (30 字符/秒)</option>
                            <option value="normal" selected>正常 (50 字符/秒)</option>
                            <option value="fast">快速 (80 字符/秒)</option>
                        </select>
                    </div>
                    <div style="padding: 10px; background: rgba(59, 130, 246, 0.1); border-left: 3px solid #3b82f6; border-radius: 4px;">
                        <p style="margin: 0; font-size: 12px; color: rgba(255,255,255,0.8); line-height: 1.5;">
                            💡 <strong>快捷键：</strong>流式输出时按 <kbd style="padding: 2px 5px; background: rgba(255,255,255,0.2); border-radius: 3px; font-family: monospace; font-size: 11px;">ESC</kbd> 或 <kbd style="padding: 2px 5px; background: rgba(255,255,255,0.2); border-radius: 3px; font-family: monospace; font-size: 11px;">Space</kbd> 跳过
                        </p>
                    </div>
                </div>
            </div>

            <!-- 输入区域（Composer） -->
            <div class="ai-composer">
                <div class="ai-input-shell">
                    <!-- 附件/上下文行（与截图一致的顶栏） -->
                    <div class="ai-attach-row" id="aiAttachRow">
                        <button class="ai-attach-btn" id="btnAttach" title="添加附件">
                            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                <path d="M21.44 11.05l-8.49 8.49a5.5 5.5 0 1 1-7.78-7.78l9.19-9.19a3.5 3.5 0 0 1 4.95 4.95l-9.55 9.55a1.5 1.5 0 0 1-2.12-2.12l8.84-8.84"/>
                            </svg>
                        </button>
                        <div class="ai-attach-list" id="aiAttachList"></div>
                        <button class="ai-attach-add" id="btnAttachAdd" title="添加">
                            <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                <path d="M12 5v14M5 12h14"/>
                            </svg>
                        </button>
                        <input type="file" id="aiFilePicker" style="display:none" multiple />
                    </div>

                    <!-- 上下文标签区域 -->
                    <div class="ai-context-chips" id="aiContextChips"></div>

                    <!-- 输入框 -->
                    <textarea id="aiChatInput" class="ai-chat-input" placeholder="添加上下文(#)、扩展(@)、命令(/)" rows="2"></textarea>

                    <!-- # 上下文选择弹出菜单 -->
                    <div class="ai-hashtag-popup" id="aiHashtagPopup" style="display: none;">
                        <div class="ai-hashtag-title">选择SSH会话作为上下文</div>
                        <ul class="ai-hashtag-list" id="aiHashtagList"></ul>
                    </div>
                    <!-- 输入框工具条（移入输入壳内部） -->
                    <div class="ai-bottom-row ai-input-toolbar">
                        <div class="ai-left-controls">
                            <!-- 模式选择器 -->
                            <div class="ai-mode-selector">
                                <select id="aiModeSelector" class="ai-mode-select">
                                    <option value="ask">Ask 模式</option>
                                    <option value="agent" disabled>Agent 模式</option>
                                </select>
                            </div>
                            <!-- 模型选择器 -->
                            <div class="ai-models">
                                <select id="aiModelSelector" class="ai-model-select">
                                    <option value="kimi-k2-0905-preview">Kimi K2</option>
                                    <option value="echo">Echo (测试)</option>
                                </select>
                            </div>
                        </div>
                        <div class="ai-right-controls">
                            <button class="ai-btn-voice icon-only" id="btnVoice" title="语音输入">
                                <span class="voice-dot"></span>
                                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                    <path d="M12 1a3 3 0 0 0-3 3v8a3 3 0 0 0 6 0V4a3 3 0 0 0-3-3z"/>
                                    <path d="M19 10v2a7 7 0 0 1-14 0v-2"/>
                                </svg>
                            </button>
                            <button class="ai-btn-send icon-only" id="aiSendBtn" title="发送 (Ctrl+Enter)">
                                <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor">
                                    <polygon points="5 3 19 12 5 21 5 3"/>
                                </svg>
                            </button>
                        </div>
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

        // 新建会话按钮
        document.getElementById('btnNewSession').addEventListener('click', () => {
            this.clearChat();
        });

        // 设置按钮 - 切换设置抽屉
        document.getElementById('btnSettings').addEventListener('click', () => {
            const drawer = document.getElementById('aiSettingsDrawer');
            if (drawer.style.display === 'none') {
                drawer.style.display = 'block';
            } else {
                drawer.style.display = 'none';
            }
        });

        // 模式选择器事件（下拉框）
        document.getElementById('aiModeSelector').addEventListener('change', (e) => {
            this.switchMode(e.target.value);
        });

        // 附件：触发文件选择
        const filePicker = document.getElementById('aiFilePicker');
        const attachHandler = () => filePicker.click();
        document.getElementById('btnAttach').addEventListener('click', attachHandler);
        document.getElementById('btnAttachAdd').addEventListener('click', attachHandler);

        // 附件：选择后渲染为 Chip
        filePicker.addEventListener('change', (e) => {
            const files = Array.from(e.target.files || []);
            if (!files.length) return;

            const list = document.getElementById('aiAttachList');
            files.forEach(f => {
                const chip = document.createElement('div');
                chip.className = 'ai-attachment-chip';
                chip.title = f.name;
                chip.innerHTML = `
                    <span class="chip-file-icon">📄</span>
                    <span class="chip-file-name">${this.escapeHtml(f.name)}</span>
                    <button class="chip-remove" title="移除" aria-label="移除">×</button>
                `;
                chip.querySelector('.chip-remove').addEventListener('click', () => chip.remove());
                list.appendChild(chip);
            });

            // 重置 input 以便可以选择相同文件
            filePicker.value = '';
        });

        // 发送按钮事件
        document.getElementById('aiSendBtn').addEventListener('click', () => {
            this.sendMessage();
        });

        // 语音输入按钮（占位符功能）
        document.getElementById('btnVoice').addEventListener('click', () => {
            // TODO: 实现语音输入功能
            alert('语音输入功能即将上线！');
        });

        // 输入框事件
        const input = document.getElementById('aiChatInput');

        // 监听输入 - 检测 # 字符
        input.addEventListener('input', (e) => {
            const value = e.target.value;
            const cursorPos = e.target.selectionStart;

            // 检查光标前是否有 # 字符
            const textBeforeCursor = value.substring(0, cursorPos);
            const lastHashIndex = textBeforeCursor.lastIndexOf('#');

            // 如果找到 # 且之后没有空格，显示菜单
            if (lastHashIndex !== -1) {
                const textAfterHash = textBeforeCursor.substring(lastHashIndex + 1);
                if (!textAfterHash.includes(' ') && !textAfterHash.includes('\n')) {
                    this.showHashtagMenu(textAfterHash);
                    return;
                }
            }

            // 否则隐藏菜单
            this.hideHashtagMenu();
        });

        input.addEventListener('keydown', (e) => {
            // 如果菜单打开，处理上下键和回车键
            if (this.isHashtagMenuOpen) {
                if (e.key === 'ArrowDown' || e.key === 'ArrowUp') {
                    e.preventDefault();
                    this.navigateHashtagMenu(e.key === 'ArrowDown' ? 1 : -1);
                    return;
                }
                if (e.key === 'Enter') {
                    e.preventDefault();
                    this.selectHighlightedSession();
                    return;
                }
                if (e.key === 'Escape') {
                    e.preventDefault();
                    this.hideHashtagMenu();
                    return;
                }
            }

            // Ctrl+Enter 或 Cmd+Enter 发送消息
            if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') {
                e.preventDefault();
                this.sendMessage();
            }
        });

        // 点击外部关闭菜单
        document.addEventListener('click', (e) => {
            const popup = document.getElementById('aiHashtagPopup');
            const input = document.getElementById('aiChatInput');
            if (this.isHashtagMenuOpen && !popup.contains(e.target) && e.target !== input) {
                this.hideHashtagMenu();
            }
        });

        // 全局快捷键 Ctrl+Shift+A 切换面板
        document.addEventListener('keydown', (e) => {
            if (e.ctrlKey && e.shiftKey && e.key === 'A') {
                e.preventDefault();
                this.toggle();
            }
        });

        // 全局快捷键 ESC/Space 跳过流式输出
        document.addEventListener('keydown', (e) => {
            // 只在AI助手打开且有活跃打字机时响应
            if (this.isOpen && this.activeTypewriter && !this.activeTypewriter.isComplete) {
                // ESC 键或 Space 键
                if (e.key === 'Escape' || e.key === ' ') {
                    // 如果当前焦点在输入框，Space 键不跳过（允许输入空格）
                    if (e.key === ' ' && e.target.id === 'aiChatInput') {
                        return;
                    }

                    e.preventDefault();
                    this.activeTypewriter.skipToEnd();
                }
            }
        });

        // 自动调整输入框高度 & 动态启用/禁用发送按钮
        const sendBtn = document.getElementById('aiSendBtn');

        // 初始状态：禁用发送按钮
        sendBtn.disabled = true;
        sendBtn.style.opacity = '0.5';
        sendBtn.style.cursor = 'not-allowed';

        input.addEventListener('input', () => {
            input.style.height = 'auto';
            input.style.height = Math.min(input.scrollHeight, 120) + 'px';

            // 根据输入内容启用/禁用发送按钮
            const hasContent = input.value.trim().length > 0;
            sendBtn.disabled = !hasContent;
            sendBtn.style.opacity = hasContent ? '1' : '0.5';
            sendBtn.style.cursor = hasContent ? 'pointer' : 'not-allowed';
        });

        // 流式输出设置事件
        this.initStreamingPreferences();
    }

    initStreamingPreferences() {
        const enabledCheckbox = document.getElementById('aiStreamingEnabled');
        const speedSelect = document.getElementById('aiStreamingSpeed');

        if (enabledCheckbox) {
            // 从 LocalStorage 加载设置
            const enabled = localStorage.getItem('ai.streaming.enabled');
            if (enabled !== null) {
                enabledCheckbox.checked = enabled === 'true';
            }

            // 监听变化
            enabledCheckbox.addEventListener('change', (e) => {
                localStorage.setItem('ai.streaming.enabled', String(e.target.checked));
                this.syncPreferencesToBackend();
            });
        }

        if (speedSelect) {
            // 从 LocalStorage 加载设置
            const speed = localStorage.getItem('ai.streaming.speed');
            if (speed) {
                speedSelect.value = speed;
            }

            // 监听变化
            speedSelect.addEventListener('change', (e) => {
                localStorage.setItem('ai.streaming.speed', e.target.value);
                this.syncPreferencesToBackend();
            });
        }
    }

    async syncPreferencesToBackend() {
        const enabled = localStorage.getItem('ai.streaming.enabled') !== 'false';
        const speed = localStorage.getItem('ai.streaming.speed') || 'normal';

        try {
            await fetch('/profile/preferences', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    aiStreamingEnabled: enabled,
                    aiStreamingSpeed: speed
                })
            });
        } catch (err) {
            console.warn('Failed to sync streaming preferences to backend:', err);
        }
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

        // 更新按钮状态（支持新旧两种类名）
        document.querySelectorAll('.ai-mode, .ai-mode-btn').forEach(btn => {
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

        // 收集上下文内容
        const contextContent = this.collectContextContent();

        // 构建完整消息（消息 + 上下文）
        const fullMessage = message + contextContent;

        // 添加用户消息到界面（仅显示用户输入的部分）
        this.addMessage('user', message);

        // 如果有上下文，显示上下文标签
        if (this.contextSessions.length > 0) {
            const contextIndicator = document.createElement('div');
            contextIndicator.className = 'ai-context-indicator';
            contextIndicator.innerHTML = `
                <span>📎 已包含 ${this.contextSessions.length} 个会话上下文</span>
            `;
            const lastMsg = document.getElementById('aiChatMessages').lastElementChild;
            if (lastMsg) {
                lastMsg.appendChild(contextIndicator);
            }
        }

        // 获取终端上下文
        const terminalContext = this.extractTerminalContext();

        // 设置流式状态
        this.isStreaming = true;
        this.updateSendButton(true);

        // 添加加载指示器
        const loadingId = this.addLoadingIndicator();

        try {
            // 发送请求到后端API（使用完整消息）
            await this.streamChat(fullMessage, terminalContext, loadingId);
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
                    }
                }
            }
        }

        // SSE 流结束，现在进行两阶段渲染
        if (isStreamingEnabled()) {
            // ✅ 启用打字机效果 - 两阶段渲染
            // Stage 1: 显示纯文本流式动画
            const plainText = stripHtmlTags(fullContent);

            // 创建流式文本容器
            contentElement.innerHTML = '';
            const streamingContainer = document.createElement('div');
            streamingContainer.className = 'streaming-text';
            contentElement.appendChild(streamingContainer);

            const typewriter = new TypewriterRenderer(streamingContainer, {
                speed: getUserStreamingSpeed(),
                onComplete: () => {
                    // Stage 2: 流式动画完成后，应用格式化
                    this.applyFormatting(contentElement, fullContent);
                    // 清除活跃的打字机引用
                    this.activeTypewriter = null;
                }
            });

            // 保存当前活跃的打字机实例
            this.activeTypewriter = typewriter;

            // 允许用户点击跳过
            streamingContainer.addEventListener('click', () => typewriter.skipToEnd());

            typewriter.start(plainText);
        } else {
            // ❌ 禁用流式输出，立即显示格式化内容
            this.applyFormatting(contentElement, fullContent);
        }
    }

    applyFormatting(contentElement, htmlContent) {
        // 清空容器
        contentElement.innerHTML = '';

        // 第一帧：设置内容
        requestAnimationFrame(() => {
            // 渲染格式化内容
            this.renderMessageContent(contentElement, htmlContent);

            // 处理代码块复制按钮
            this.setupCodeCopyButtons(contentElement);

            // 第二帧：添加淡入动画
            requestAnimationFrame(() => {
                contentElement.classList.add('format-applied');
            });
        });
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
                <div class="typing-indicator">
                    <span>AI 正在思考</span>
                    <div class="dots">
                        <span class="dot"></span>
                        <span class="dot"></span>
                        <span class="dot"></span>
                    </div>
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
            sendBtn.innerHTML = '<svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor"><circle cx="12" cy="12" r="2"><animate attributeName="r" from="2" to="8" dur="1s" repeatCount="indefinite"/><animate attributeName="opacity" from="1" to="0" dur="1s" repeatCount="indefinite"/></circle></svg>';
            sendBtn.style.opacity = '0.5';
            sendBtn.style.cursor = 'not-allowed';
        } else {
            const hasContent = document.getElementById('aiChatInput').value.trim().length > 0;
            sendBtn.disabled = !hasContent;
            sendBtn.innerHTML = '<svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor"><polygon points="5 3 19 12 5 21 5 3"/></svg>';
            sendBtn.style.opacity = hasContent ? '1' : '0.5';
            sendBtn.style.cursor = hasContent ? 'pointer' : 'not-allowed';
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

    // ========== 上下文（Hashtag）功能 ==========

    /**
     * 显示SSH会话选择菜单
     */
    showHashtagMenu(filterText = '') {
        const popup = document.getElementById('aiHashtagPopup');
        const list = document.getElementById('aiHashtagList');

        // 获取所有SSH会话
        const sessions = this.getAvailableSessions();

        // 过滤会话
        const filtered = sessions.filter(s =>
            s.name.toLowerCase().includes(filterText.toLowerCase()) ||
            s.id.toLowerCase().includes(filterText.toLowerCase())
        );

        // 渲染会话列表
        list.innerHTML = filtered.map((session, index) => `
            <li class="ai-hashtag-item ${index === 0 ? 'highlighted' : ''}" data-session-id="${session.id}">
                <div class="session-icon">🖥️</div>
                <div class="session-info">
                    <div class="session-name">${this.escapeHtml(session.name)}</div>
                    <div class="session-detail">${this.escapeHtml(session.server || 'localhost')}</div>
                </div>
            </li>
        `).join('');

        // 添加点击事件
        list.querySelectorAll('.ai-hashtag-item').forEach(item => {
            item.addEventListener('click', () => {
                this.addContextSession(item.dataset.sessionId);
            });
        });

        popup.style.display = 'block';
        this.isHashtagMenuOpen = true;
    }

    /**
     * 隐藏SSH会话选择菜单
     */
    hideHashtagMenu() {
        const popup = document.getElementById('aiHashtagPopup');
        popup.style.display = 'none';
        this.isHashtagMenuOpen = false;
    }

    /**
     * 导航hashtag菜单（上下键）
     */
    navigateHashtagMenu(direction) {
        const items = document.querySelectorAll('.ai-hashtag-item');
        if (items.length === 0) return;

        const currentIndex = Array.from(items).findIndex(item => item.classList.contains('highlighted'));
        let newIndex = currentIndex + direction;

        if (newIndex < 0) newIndex = items.length - 1;
        if (newIndex >= items.length) newIndex = 0;

        items.forEach((item, index) => {
            item.classList.toggle('highlighted', index === newIndex);
        });

        // 滚动到可见
        items[newIndex].scrollIntoView({ block: 'nearest' });
    }

    /**
     * 选择高亮的会话
     */
    selectHighlightedSession() {
        const highlighted = document.querySelector('.ai-hashtag-item.highlighted');
        if (highlighted) {
            this.addContextSession(highlighted.dataset.sessionId);
        }
    }

    /**
     * 添加上下文会话
     */
    addContextSession(sessionId) {
        const sessions = this.getAvailableSessions();
        const session = sessions.find(s => s.id === sessionId);
        if (!session) return;

        // 避免重复添加
        if (this.contextSessions.some(s => s.id === sessionId)) {
            this.hideHashtagMenu();
            this.removeHashtagFromInput();
            return;
        }

        this.contextSessions.push(session);
        this.renderContextChips();
        this.hideHashtagMenu();
        this.removeHashtagFromInput();

        // 聚焦回输入框
        document.getElementById('aiChatInput').focus();
    }

    /**
     * 从输入框中移除 # 标记
     */
    removeHashtagFromInput() {
        const input = document.getElementById('aiChatInput');
        const value = input.value;
        const cursorPos = input.selectionStart;

        // 找到光标前最后一个 #
        const textBeforeCursor = value.substring(0, cursorPos);
        const lastHashIndex = textBeforeCursor.lastIndexOf('#');

        if (lastHashIndex !== -1) {
            // 移除 # 及其后的文本（直到空格或换行）
            const beforeHash = value.substring(0, lastHashIndex);
            const afterCursor = value.substring(cursorPos);
            input.value = beforeHash + afterCursor;
            input.selectionStart = input.selectionEnd = lastHashIndex;
        }
    }

    /**
     * 渲染上下文标签
     */
    renderContextChips() {
        const container = document.getElementById('aiContextChips');
        container.innerHTML = this.contextSessions.map(session => `
            <div class="ai-context-chip" data-session-id="${session.id}">
                <span class="chip-icon">🖥️</span>
                <span class="chip-text">${this.escapeHtml(session.name)}</span>
                <button class="chip-remove" data-session-id="${session.id}" title="移除">×</button>
            </div>
        `).join('');

        // 添加移除按钮事件
        container.querySelectorAll('.chip-remove').forEach(btn => {
            btn.addEventListener('click', (e) => {
                e.stopPropagation();
                this.removeContextSession(btn.dataset.sessionId);
            });
        });
    }

    /**
     * 移除上下文会话
     */
    removeContextSession(sessionId) {
        this.contextSessions = this.contextSessions.filter(s => s.id !== sessionId);
        this.renderContextChips();
    }

    /**
     * 获取可用的SSH会话
     */
    getAvailableSessions() {
        // 从terminalManager获取所有打开的SSH会话
        const sessions = [];

        if (this.terminalManager && this.terminalManager.terminals) {
            Object.keys(this.terminalManager.terminals).forEach(sessionId => {
                const terminal = this.terminalManager.terminals[sessionId];
                if (terminal && terminal.serverInfo) {
                    sessions.push({
                        id: sessionId,
                        name: terminal.serverInfo.name || sessionId,
                        server: terminal.serverInfo.host || 'localhost',
                        history: terminal.getHistory ? terminal.getHistory() : []
                    });
                }
            });
        }

        return sessions;
    }

    /**
     * 收集上下文内容
     */
    collectContextContent() {
        if (this.contextSessions.length === 0) return '';

        let contextText = '\n\n[上下文信息]\n';

        this.contextSessions.forEach(session => {
            contextText += `\n## SSH会话: ${session.name} (${session.server})\n`;

            // 获取会话历史
            if (session.history && session.history.length > 0) {
                contextText += '最近的命令输出:\n```\n';
                // 获取最近20条历史记录
                const recentHistory = session.history.slice(-20);
                contextText += recentHistory.join('\n');
                contextText += '\n```\n';
            } else {
                contextText += '（暂无历史记录）\n';
            }
        });

        return contextText;
    }

    clearChat() {
        this.messages = [];
        this.currentChatId = null;
        this.contextSessions = []; // 清空上下文
        this.renderContextChips();

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
