/**
 * 服务器添加模态弹窗管理器
 * Server Addition Modal Manager
 *
 * 功能特性:
 * - 模态弹窗生命周期管理 (打开/关闭/销毁)
 * - 表单验证与提交
 * - 密码显示/隐藏切换
 * - 标签输入 (Enter添加, Backspace删除)
 * - 高级选项折叠/展开
 * - 连接测试功能
 * - Toast通知提示
 * - 防止背景滚动
 * - ESC键关闭
 */

(function() {
    'use strict';

    class ServerModal {
        constructor() {
            this.modal = null;
            this.overlay = null;
            this.isOpen = false;
            this.tags = [];
            this.initModal();
            this.bindEvents();
        }

        /**
         * 获取CSRF token
         */
        getCsrfToken() {
            const token = document.querySelector('meta[name="_csrf"]');
            const header = document.querySelector('meta[name="_csrf_header"]');
            return {
                token: token ? token.getAttribute('content') : '',
                header: header ? header.getAttribute('content') : 'X-CSRF-TOKEN'
            };
        }

        /**
         * 初始化模态弹窗HTML结构
         */
        initModal() {
            const modalHTML = `
                <div class="server-modal-overlay" id="serverModalOverlay">
                    <div class="server-modal-container" id="serverModalContainer">
                        <!-- 弹窗头部 -->
                        <div class="server-modal-header">
                            <h2 class="server-modal-title">
                                <i class="fas fa-server"></i>
                                添加新服务器
                            </h2>
                            <button class="server-modal-close" id="serverModalClose" aria-label="关闭">
                                <i class="fas fa-times"></i>
                            </button>
                        </div>

                        <!-- 弹窗主体 (可滚动) -->
                        <div class="server-modal-body">
                            <form id="serverAddForm">
                                <!-- 基本信息 -->
                                <div class="server-modal-section">
                                    <h3 class="server-modal-section-title">
                                        <i class="fas fa-info-circle"></i>
                                        基本信息
                                    </h3>
                                    <div class="server-modal-form-row">
                                        <div class="server-modal-form-group">
                                            <label class="server-modal-form-label">
                                                服务器名称 <span class="server-modal-required">*</span>
                                            </label>
                                            <input
                                                type="text"
                                                class="server-modal-form-control"
                                                name="name"
                                                id="serverName"
                                                placeholder="例: 生产服务器-01"
                                                required
                                            >
                                        </div>
                                        <div class="server-modal-form-group">
                                            <label class="server-modal-form-label">
                                                服务器类型 <span class="server-modal-required">*</span>
                                            </label>
                                            <select class="server-modal-form-control" name="type" id="serverType" required>
                                                <option value="">选择类型</option>
                                                <option value="production">生产环境</option>
                                                <option value="staging">预发布环境</option>
                                                <option value="development">开发环境</option>
                                                <option value="testing">测试环境</option>
                                            </select>
                                        </div>
                                    </div>

                                    <div class="server-modal-form-group">
                                        <label class="server-modal-form-label">描述</label>
                                        <textarea
                                            class="server-modal-form-control"
                                            name="description"
                                            id="serverDescription"
                                            rows="3"
                                            placeholder="输入服务器描述信息..."
                                        ></textarea>
                                    </div>
                                </div>

                                <!-- SSH连接配置 -->
                                <div class="server-modal-section">
                                    <h3 class="server-modal-section-title">
                                        <i class="fas fa-network-wired"></i>
                                        SSH连接配置
                                    </h3>
                                    <div class="server-modal-form-row">
                                        <div class="server-modal-form-group">
                                            <label class="server-modal-form-label">
                                                主机地址 <span class="server-modal-required">*</span>
                                            </label>
                                            <input
                                                type="text"
                                                class="server-modal-form-control"
                                                name="hostname"
                                                id="serverHostname"
                                                placeholder="例: 192.168.1.100 或 server.example.com"
                                                required
                                            >
                                        </div>
                                        <div class="server-modal-form-group">
                                            <label class="server-modal-form-label">
                                                SSH端口 <span class="server-modal-required">*</span>
                                            </label>
                                            <input
                                                type="number"
                                                class="server-modal-form-control"
                                                name="port"
                                                id="serverPort"
                                                value="22"
                                                min="1"
                                                max="65535"
                                                required
                                            >
                                        </div>
                                    </div>

                                    <div class="server-modal-form-row">
                                        <div class="server-modal-form-group">
                                            <label class="server-modal-form-label">
                                                SSH用户名 <span class="server-modal-required">*</span>
                                            </label>
                                            <input
                                                type="text"
                                                class="server-modal-form-control"
                                                name="username"
                                                id="serverUsername"
                                                placeholder="例: root"
                                                required
                                            >
                                        </div>
                                        <div class="server-modal-form-group">
                                            <label class="server-modal-form-label">
                                                SSH密码 <span class="server-modal-required">*</span>
                                            </label>
                                            <div class="server-modal-password-wrapper">
                                                <input
                                                    type="password"
                                                    class="server-modal-form-control"
                                                    name="password"
                                                    id="serverPassword"
                                                    placeholder="输入SSH密码"
                                                    required
                                                >
                                                <button
                                                    type="button"
                                                    class="server-modal-password-toggle"
                                                    id="passwordToggle"
                                                    aria-label="显示/隐藏密码"
                                                >
                                                    <i class="fas fa-eye"></i>
                                                </button>
                                            </div>
                                        </div>
                                    </div>

                                    <div class="server-modal-form-group">
                                        <button type="button" class="server-modal-test-btn" id="testConnectionBtn">
                                            <i class="fas fa-plug"></i>
                                            测试连接
                                        </button>
                                    </div>
                                </div>

                                <!-- 用户组与监控 -->
                                <div class="server-modal-section">
                                    <h3 class="server-modal-section-title">
                                        <i class="fas fa-users"></i>
                                        用户组与监控
                                    </h3>
                                    <label class="server-modal-checkbox-tile">
                                        <input type="checkbox" name="autoInitGroups" id="autoInitGroups" checked>
                                        <div class="server-modal-checkbox-tile-content">
                                            <div class="server-modal-checkbox-tile-label">自动初始化默认用户组</div>
                                            <div class="server-modal-checkbox-tile-hint">创建管理员、开发者、访客等基础权限组，后续可在用户管理中调整。</div>
                                        </div>
                                    </label>
                                </div>

                                <!-- 高级选项 (可折叠) -->
                                <div class="server-modal-section">
                                    <button
                                        type="button"
                                        class="server-modal-advanced-toggle"
                                        id="advancedToggle"
                                        aria-expanded="false"
                                    >
                                        <i class="fas fa-cog"></i>
                                        高级选项
                                        <i class="fas fa-chevron-down server-modal-toggle-icon"></i>
                                    </button>
                                    <div class="server-modal-advanced-content" id="advancedContent">
                                        <div class="server-modal-form-group">
                                            <label class="server-modal-form-label">最大并发会话</label>
                                            <input
                                                type="number"
                                                class="server-modal-form-control"
                                                name="maxSessions"
                                                id="maxSessions"
                                                value="5"
                                                min="1"
                                                max="50"
                                            >
                                            <small class="server-modal-hint">限制同一时间占用该服务器的工作空间数量。</small>
                                        </div>

                                        <div class="server-modal-form-group">
                                            <label class="server-modal-form-label">服务器标签</label>
                                            <div class="server-modal-tags-container" id="tagsContainer">
                                                <input
                                                    type="text"
                                                    class="server-modal-tags-input"
                                                    id="tagsInput"
                                                    placeholder="输入后回车添加，如 production"
                                                >
                                            </div>
                                        </div>

                                        <label class="server-modal-checkbox-tile">
                                            <input type="checkbox" name="enableMetrics" id="enableMetrics" checked>
                                            <div class="server-modal-checkbox-tile-content">
                                                <div class="server-modal-checkbox-tile-label">启用实时指标采集</div>
                                                <div class="server-modal-checkbox-tile-hint">同步 CPU / 内存 / GPU 使用率，为容量规划提供依据。</div>
                                            </div>
                                        </label>
                                    </div>
                                </div>
                            </form>
                        </div>

                        <!-- 弹窗底部 -->
                        <div class="server-modal-footer">
                            <button type="button" class="server-modal-btn server-modal-btn-cancel" id="cancelBtn">
                                <i class="fas fa-times"></i>
                                取消
                            </button>
                            <button type="submit" class="server-modal-btn server-modal-btn-primary" id="submitBtn" form="serverAddForm">
                                <i class="fas fa-check"></i>
                                添加服务器
                            </button>
                        </div>
                    </div>
                </div>

                <!-- Toast通知容器 -->
                <div class="server-modal-toast-container" id="toastContainer"></div>
            `;

            // 插入DOM
            const modalElement = document.createElement('div');
            modalElement.innerHTML = modalHTML;
            document.body.appendChild(modalElement);

            this.overlay = document.getElementById('serverModalOverlay');
            this.modal = document.getElementById('serverModalContainer');
        }

        /**
         * 绑定所有事件处理器
         */
        bindEvents() {
            // 关闭按钮
            document.getElementById('serverModalClose').addEventListener('click', () => this.close());
            document.getElementById('cancelBtn').addEventListener('click', () => this.close());

            // 点击遮罩层关闭
            this.overlay.addEventListener('click', (e) => {
                if (e.target === this.overlay) {
                    this.close();
                }
            });

            // ESC键关闭
            document.addEventListener('keydown', (e) => {
                if (e.key === 'Escape' && this.isOpen) {
                    this.close();
                }
            });

            // 密码显示/隐藏
            document.getElementById('passwordToggle').addEventListener('click', this.togglePassword.bind(this));

            // 标签输入
            const tagsInput = document.getElementById('tagsInput');
            tagsInput.addEventListener('keydown', this.handleTagInput.bind(this));

            // 高级选项折叠
            document.getElementById('advancedToggle').addEventListener('click', this.toggleAdvanced.bind(this));

            // 测试连接
            document.getElementById('testConnectionBtn').addEventListener('click', this.testConnection.bind(this));

            // 表单提交
            document.getElementById('serverAddForm').addEventListener('submit', this.handleSubmit.bind(this));
        }

        /**
         * 打开模态弹窗
         */
        open() {
            this.overlay.classList.add('show');
            this.isOpen = true;
            document.body.style.overflow = 'hidden'; // 防止背景滚动

            // 聚焦第一个输入框
            setTimeout(() => {
                document.getElementById('serverName').focus();
            }, 300);
        }

        /**
         * 关闭模态弹窗
         */
        close() {
            this.overlay.classList.remove('show');
            this.isOpen = false;
            document.body.style.overflow = ''; // 恢复背景滚动

            // 重置表单
            setTimeout(() => {
                this.resetForm();
            }, 300);
        }

        /**
         * 切换密码显示/隐藏
         */
        togglePassword(e) {
            const passwordInput = document.getElementById('serverPassword');
            const icon = e.currentTarget.querySelector('i');

            if (passwordInput.type === 'password') {
                passwordInput.type = 'text';
                icon.classList.remove('fa-eye');
                icon.classList.add('fa-eye-slash');
            } else {
                passwordInput.type = 'password';
                icon.classList.remove('fa-eye-slash');
                icon.classList.add('fa-eye');
            }
        }

        /**
         * 处理标签输入
         */
        handleTagInput(e) {
            const input = e.target;
            const tagsContainer = document.getElementById('tagsContainer');

            if (e.key === 'Enter' && input.value.trim()) {
                e.preventDefault();
                const tagText = input.value.trim();

                if (!this.tags.includes(tagText)) {
                    this.tags.push(tagText);
                    const tag = this.createTagElement(tagText);
                    tagsContainer.insertBefore(tag, input);
                    input.value = '';
                }
            } else if (e.key === 'Backspace' && !input.value && this.tags.length > 0) {
                e.preventDefault();
                this.removeTag(this.tags.length - 1);
            }
        }

        /**
         * 创建标签元素
         */
        createTagElement(text) {
            const tag = document.createElement('span');
            tag.className = 'server-modal-tag';
            tag.innerHTML = `
                ${text}
                <button type="button" class="server-modal-tag-remove" aria-label="移除标签">
                    <i class="fas fa-times"></i>
                </button>
            `;

            tag.querySelector('.server-modal-tag-remove').addEventListener('click', () => {
                const index = this.tags.indexOf(text);
                this.removeTag(index);
            });

            return tag;
        }

        /**
         * 移除标签
         */
        removeTag(index) {
            if (index >= 0 && index < this.tags.length) {
                this.tags.splice(index, 1);
                const tagsContainer = document.getElementById('tagsContainer');
                const tagElements = tagsContainer.querySelectorAll('.server-modal-tag');
                if (tagElements[index]) {
                    tagElements[index].remove();
                }
            }
        }

        /**
         * 切换高级选项
         */
        toggleAdvanced(e) {
            const toggle = e.currentTarget;
            const content = document.getElementById('advancedContent');
            const icon = toggle.querySelector('.server-modal-toggle-icon');
            const isExpanded = toggle.getAttribute('aria-expanded') === 'true';

            if (isExpanded) {
                content.classList.remove('show');
                content.style.maxHeight = '0';
                toggle.setAttribute('aria-expanded', 'false');
                icon.style.transform = 'rotate(0deg)';
            } else {
                content.classList.add('show');
                // 需要先获取内容高度，再设置maxHeight以触发动画
                setTimeout(() => {
                    content.style.maxHeight = content.scrollHeight + 'px';
                }, 10);
                toggle.setAttribute('aria-expanded', 'true');
                icon.style.transform = 'rotate(180deg)';
            }
        }

        /**
         * 测试连接
         */
        async testConnection() {
            const btn = document.getElementById('testConnectionBtn');
            const hostname = document.getElementById('serverHostname').value.trim();
            const port = document.getElementById('serverPort').value;
            const username = document.getElementById('serverUsername').value.trim();
            const password = document.getElementById('serverPassword').value;

            if (!hostname || !port || !username || !password) {
                this.showToast('请填写完整的SSH连接信息', 'warning');
                return;
            }

            // 禁用按钮并显示加载状态
            btn.disabled = true;
            btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> 测试中...';

            try {
                const csrf = this.getCsrfToken();
                const headers = {
                    'Content-Type': 'application/json',
                };
                if (csrf.token) {
                    headers[csrf.header] = csrf.token;
                }

                const response = await fetch('/admin/servers/test-connection', {
                    method: 'POST',
                    headers: headers,
                    body: JSON.stringify({
                        hostname: hostname,
                        port: parseInt(port),
                        username: username,
                        password: password
                    })
                });

                const result = await response.json();

                if (response.ok && result.success) {
                    this.showToast('连接测试成功！', 'success');
                } else {
                    this.showToast(result.message || '连接测试失败', 'error');
                }
            } catch (error) {
                console.error('Connection test error:', error);
                this.showToast('连接测试失败: ' + error.message, 'error');
            } finally {
                // 恢复按钮状态
                btn.disabled = false;
                btn.innerHTML = '<i class="fas fa-plug"></i> 测试连接';
            }
        }

        /**
         * 处理表单提交
         */
        async handleSubmit(e) {
            e.preventDefault();

            const formData = new FormData(e.target);
            const serverData = {
                name: formData.get('name'),
                type: formData.get('type'),
                description: formData.get('description'),
                hostname: formData.get('hostname'),
                port: parseInt(formData.get('port')),
                username: formData.get('username'),
                password: formData.get('password'),
                tags: this.tags,
                enableMonitoring: formData.get('enableMonitoring') === 'on',
                autoRestart: formData.get('autoRestart') === 'on',
                timeout: parseInt(formData.get('timeout')) || 30,
                retryCount: parseInt(formData.get('retryCount')) || 3,
                envVars: formData.get('envVars')
            };

            const submitBtn = document.getElementById('submitBtn');
            submitBtn.disabled = true;
            submitBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> 添加中...';

            try {
                const csrf = this.getCsrfToken();
                const headers = {
                    'Content-Type': 'application/json',
                };
                if (csrf.token) {
                    headers[csrf.header] = csrf.token;
                }

                const response = await fetch('/admin/servers/api/create', {
                    method: 'POST',
                    headers: headers,
                    body: JSON.stringify(serverData)
                });

                if (response.ok) {
                    const result = await response.json();
                    this.showToast('服务器添加成功！正在收集监控数据...', 'success');

                    // 立即关闭并刷新列表（显示加载状态）
                    setTimeout(() => {
                        this.close();
                        if (typeof window.refreshServerList === 'function') {
                            window.refreshServerList();
                        }

                        // 启动轮询检查指标是否收集完成
                        if (result.serverId && typeof window.startServerMetricsPolling === 'function') {
                            window.startServerMetricsPolling(result.serverId);
                        }
                    }, 800);
                } else {
                    const error = await response.json();
                    this.showToast(error.message || '添加服务器失败', 'error');
                    submitBtn.disabled = false;
                    submitBtn.innerHTML = '<i class="fas fa-check"></i> 添加服务器';
                }
            } catch (error) {
                console.error('Submit error:', error);
                this.showToast('添加服务器失败: ' + error.message, 'error');
                submitBtn.disabled = false;
                submitBtn.innerHTML = '<i class="fas fa-check"></i> 添加服务器';
            }
        }

        /**
         * 重置表单
         */
        resetForm() {
            document.getElementById('serverAddForm').reset();
            this.tags = [];
            document.querySelectorAll('.server-modal-tag').forEach(tag => tag.remove());

            // 重置高级选项折叠状态
            const advancedContent = document.getElementById('advancedContent');
            const advancedToggle = document.getElementById('advancedToggle');
            const icon = advancedToggle.querySelector('.server-modal-toggle-icon');
            advancedContent.classList.remove('show');
            advancedContent.style.maxHeight = '0';
            advancedToggle.setAttribute('aria-expanded', 'false');
            icon.style.transform = 'rotate(0deg)';

            // 重置提交按钮
            const submitBtn = document.getElementById('submitBtn');
            submitBtn.disabled = false;
            submitBtn.innerHTML = '<i class="fas fa-check"></i> 添加服务器';
        }

        /**
         * 显示Toast通知
         */
        showToast(message, type = 'info') {
            const container = document.getElementById('toastContainer');
            const toast = document.createElement('div');
            toast.className = `server-modal-toast ${type} show`;

            const icons = {
                success: 'fa-check-circle',
                error: 'fa-exclamation-circle',
                warning: 'fa-exclamation-triangle',
                info: 'fa-info-circle'
            };

            toast.innerHTML = `
                <i class="fas ${icons[type]}"></i>
                <span>${message}</span>
            `;

            container.appendChild(toast);

            // 自动移除
            setTimeout(() => {
                toast.classList.remove('show');
                setTimeout(() => toast.remove(), 300);
            }, 3000);
        }
    }

    // 全局实例
    window.serverModal = null;

    // 初始化函数
    function initServerModal() {
        if (!window.serverModal) {
            window.serverModal = new ServerModal();
        }
        return window.serverModal;
    }

    // 如果DOM已加载完成，立即初始化；否则等待DOMContentLoaded事件
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initServerModal);
    } else {
        initServerModal();
    }

    // 提供全局打开方法
    window.openServerModal = function() {
        console.log('[ServerModal] openServerModal called');
        console.log('[ServerModal] window.serverModal:', window.serverModal);

        // 确保模态弹窗已初始化
        const modal = window.serverModal || initServerModal();
        console.log('[ServerModal] modal instance:', modal);

        if (modal) {
            modal.open();
        } else {
            console.error('[ServerModal] Failed to initialize server modal');
        }
    };

    // 立即初始化，确保在任何调用之前模态弹窗已准备好
    console.log('[ServerModal] Script loaded, initializing...');
    initServerModal();
    console.log('[ServerModal] Initialization complete, window.openServerModal available:', typeof window.openServerModal === 'function');

})();
