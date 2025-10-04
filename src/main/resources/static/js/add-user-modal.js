/**
 * 用户添加模态弹窗管理器
 * User Addition Modal Manager
 *
 * 功能特性:
 * - 模态弹窗生命周期管理 (打开/关闭/销毁)
 * - 表单验证与提交
 * - 密码显示/隐藏切换
 * - 角色选择与权限分配
 * - 高级选项折叠/展开
 * - Toast通知提示
 * - 防止背景滚动
 * - ESC键关闭
 */

(function() {
    'use strict';

    class UserAddModal {
        constructor() {
            this.modal = null;
            this.overlay = null;
            this.isOpen = false;
            this.selectedRoles = new Set();
            this.selectedServers = new Set();
            this.toastTimer = null;
            this.initModal();
            this.bindEvents();
        }

        /**
         * 初始化模态弹窗HTML结构
         */
        initModal() {
            const modalHTML = `
                <div class="user-modal-overlay" id="userModalOverlay">
                    <div class="user-modal-container" id="userModalContainer">
                        <!-- 弹窗头部 -->
                        <div class="user-modal-header">
                            <h2 class="user-modal-title">
                                <i class="fas fa-user-plus"></i>
                                添加新用户
                            </h2>
                            <button class="user-modal-close" id="userModalClose" aria-label="关闭">
                                <i class="fas fa-times"></i>
                            </button>
                        </div>

                        <!-- 弹窗主体 (可滚动) -->
                        <div class="user-modal-body">
                            <form id="userAddForm">
                                <!-- 基本信息 -->
                                <div class="user-modal-section">
                                    <h3 class="user-modal-section-title">
                                        <i class="fas fa-info-circle"></i>
                                        基本信息
                                    </h3>
                                    <div class="user-modal-form-row">
                                        <div class="user-modal-form-group">
                                            <label class="user-modal-form-label">
                                                用户名 <span class="user-modal-required">*</span>
                                            </label>
                                            <div class="user-modal-input-wrapper">
                                                <input
                                                    type="text"
                                                    class="user-modal-form-control"
                                                    name="username"
                                                    id="username"
                                                    placeholder="请输入用户名"
                                                    required
                                                >
                                            </div>
                                            <div class="user-modal-validation" id="usernameValidation"></div>
                                            <div class="user-modal-hint">用户登录系统的唯一标识</div>
                                        </div>
                                        <div class="user-modal-form-group">
                                            <label class="user-modal-form-label">
                                                电子邮箱 <span class="user-modal-required">*</span>
                                            </label>
                                            <div class="user-modal-input-wrapper">
                                                <input
                                                    type="email"
                                                    class="user-modal-form-control"
                                                    name="email"
                                                    id="email"
                                                    placeholder="请输入电子邮箱"
                                                    required
                                                >
                                            </div>
                                            <div class="user-modal-validation" id="emailValidation"></div>
                                            <div class="user-modal-hint">用于账户验证和通知</div>
                                        </div>
                                    </div>
                                </div>

                                <!-- 密码设置 -->
                                <div class="user-modal-section">
                                    <h3 class="user-modal-section-title">
                                        <i class="fas fa-lock"></i>
                                        密码设置
                                    </h3>
                                    <div class="user-modal-form-row">
                                        <div class="user-modal-form-group">
                                            <label class="user-modal-form-label">
                                                密码 <span class="user-modal-required">*</span>
                                            </label>
                                            <div class="user-modal-password-wrapper">
                                                <input
                                                    type="password"
                                                    class="user-modal-form-control"
                                                    name="password"
                                                    id="password"
                                                    placeholder="请设置密码"
                                                    required
                                                >
                                                <button
                                                    type="button"
                                                    class="user-modal-password-toggle"
                                                    id="passwordToggle"
                                                    aria-label="显示/隐藏密码"
                                                >
                                                    <i class="fas fa-eye"></i>
                                                </button>
                                            </div>
                                            <div class="user-modal-validation" id="passwordValidation"></div>
                                            <div class="user-modal-hint">至少8个字符，包含字母和数字</div>
                                        </div>
                                        <div class="user-modal-form-group">
                                            <label class="user-modal-form-label">
                                                确认密码 <span class="user-modal-required">*</span>
                                            </label>
                                            <div class="user-modal-password-wrapper">
                                                <input
                                                    type="password"
                                                    class="user-modal-form-control"
                                                    name="confirmPassword"
                                                    id="confirmPassword"
                                                    placeholder="请再次输入密码"
                                                    required
                                                >
                                                <button
                                                    type="button"
                                                    class="user-modal-password-toggle"
                                                    id="confirmPasswordToggle"
                                                    aria-label="显示/隐藏密码"
                                                >
                                                    <i class="fas fa-eye"></i>
                                                </button>
                                            </div>
                                            <div class="user-modal-validation" id="confirmPasswordValidation"></div>
                                            <div class="user-modal-hint">两次输入的密码必须一致</div>
                                        </div>
                                    </div>
                                </div>

                                <!-- 角色分配 -->
                                <div class="user-modal-section">
                                    <h3 class="user-modal-section-title">
                                        <i class="fas fa-user-tag"></i>
                                        用户角色
                                    </h3>
                                    <div class="user-modal-role-matrix" id="roleMatrix">
                                        <label class="user-modal-role-chip">
                                            <input type="checkbox" name="roles" value="USER" id="role-user">
                                            <div class="user-modal-role-label">开发者</div>
                                        </label>
                                        <label class="user-modal-role-chip">
                                            <input type="checkbox" name="roles" value="ADMIN" id="role-admin">
                                            <div class="user-modal-role-label">管理员</div>
                                        </label>
                                        <label class="user-modal-role-chip">
                                            <input type="checkbox" name="roles" value="SUPER_ADMIN" id="role-super-admin">
                                            <div class="user-modal-role-label">超级管理员</div>
                                        </label>
                                    </div>
                                    <div class="user-modal-validation" id="rolesValidation"></div>
                                </div>

                                <!-- 高级选项 (可折叠) -->
                                <div class="user-modal-section">
                                    <button
                                        type="button"
                                        class="user-modal-advanced-toggle"
                                        id="advancedToggle"
                                        aria-expanded="false"
                                    >
                                        <i class="fas fa-cog"></i>
                                        高级选项
                                        <i class="fas fa-chevron-down user-modal-toggle-icon"></i>
                                    </button>
                                    <div class="user-modal-advanced-content" id="advancedContent">
                                        <div class="user-modal-form-group">
                                            <label class="user-modal-form-label">工作目录</label>
                                            <div class="user-modal-input-wrapper">
                                                <input
                                                    type="text"
                                                    class="user-modal-form-control"
                                                    name="workDirectory"
                                                    id="workDirectory"
                                                    placeholder="自动生成"
                                                >
                                            </div>
                                            <div class="user-modal-hint">用户的主要工作空间路径</div>
                                        </div>

                                        <label class="user-modal-checkbox-tile">
                                            <input type="checkbox" name="forceReset" id="forceReset">
                                            <div class="user-modal-checkbox-tile-content">
                                                <div class="user-modal-checkbox-tile-label">强制首次登录修改密码</div>
                                                <div class="user-modal-checkbox-tile-hint">用户首次登录系统时必须修改密码</div>
                                            </div>
                                        </label>
                                    </div>
                                </div>
                            </form>
                        </div>

                        <!-- 弹窗底部 -->
                        <div class="user-modal-footer">
                            <button type="button" class="user-modal-btn user-modal-btn-cancel" id="cancelBtn">
                                <i class="fas fa-times"></i>
                                取消
                            </button>
                            <button type="submit" class="user-modal-btn user-modal-btn-primary" id="submitBtn" form="userAddForm">
                                <i class="fas fa-check"></i>
                                添加用户
                            </button>
                        </div>
                    </div>
                </div>

                <!-- Toast通知容器 -->
                <div class="user-modal-toast-container" id="toastContainer"></div>
            `;

            // 插入DOM
            const modalElement = document.createElement('div');
            modalElement.innerHTML = modalHTML;
            document.body.appendChild(modalElement);

            this.overlay = document.getElementById('userModalOverlay');
            this.modal = document.getElementById('userModalContainer');
        }

        /**
         * 绑定所有事件处理器
         */
        bindEvents() {
            // 关闭按钮
            document.getElementById('userModalClose').addEventListener('click', () => this.close());
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
            document.getElementById('passwordToggle').addEventListener('click', () => this.togglePasswordVisibility('password'));
            document.getElementById('confirmPasswordToggle').addEventListener('click', () => this.togglePasswordVisibility('confirmPassword'));

            // 高级选项折叠
            document.getElementById('advancedToggle').addEventListener('click', this.toggleAdvanced.bind(this));

            // 角色选择
            const roleMatrix = document.getElementById('roleMatrix');
            roleMatrix.addEventListener('change', (e) => {
                if (e.target.type === 'checkbox' && e.target.name === 'roles') {
                    const role = e.target.value;
                    if (e.target.checked) {
                        this.selectedRoles.add(role);
                    } else {
                        this.selectedRoles.delete(role);
                    }

                    // 验证至少选择一个角色
                    if (this.selectedRoles.size === 0) {
                        this.updateValidation('roles', '请至少选择一个角色', false);
                    } else {
                        this.updateValidation('roles', '', true);
                    }
                }
            });

            // 表单验证事件
            document.getElementById('username').addEventListener('blur', async () => {
                const username = document.getElementById('username').value.trim();
                if (username) {
                    const result = await this.checkDuplicate('username', username);
                    this.updateValidation('username', result.message, result.valid);
                }
            });

            document.getElementById('email').addEventListener('blur', async () => {
                const email = document.getElementById('email').value.trim();
                if (email) {
                    const result = await this.checkDuplicate('email', email);
                    this.updateValidation('email', result.message, result.valid);
                }
            });

            document.getElementById('confirmPassword').addEventListener('input', () => {
                const password = document.getElementById('password').value;
                const confirmPassword = document.getElementById('confirmPassword').value;
                if (confirmPassword) {
                    if (password === confirmPassword) {
                        this.updateValidation('confirmPassword', '密码匹配', true);
                    } else {
                        this.updateValidation('confirmPassword', '密码不匹配', false);
                    }
                }
            });

            // 表单提交
            document.getElementById('userAddForm').addEventListener('submit', this.handleSubmit.bind(this));
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
                document.getElementById('username').focus();
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
         * 重置表单
         */
        resetForm() {
            document.getElementById('userAddForm').reset();
            this.selectedRoles.clear();
            this.selectedServers.clear();

            // 清除验证状态
            this.updateValidation('username', '', true);
            this.updateValidation('email', '', true);
            this.updateValidation('password', '', true);
            this.updateValidation('confirmPassword', '', true);
            this.updateValidation('roles', '', true);

            // 重置高级选项折叠状态
            const advancedContent = document.getElementById('advancedContent');
            const advancedToggle = document.getElementById('advancedToggle');
            const icon = advancedToggle.querySelector('.user-modal-toggle-icon');
            advancedContent.classList.remove('show');
            advancedContent.style.maxHeight = '0';
            advancedToggle.setAttribute('aria-expanded', 'false');
            icon.style.transform = 'rotate(0deg)';

            // 重置提交按钮
            const submitBtn = document.getElementById('submitBtn');
            submitBtn.disabled = false;
            submitBtn.innerHTML = '<i class="fas fa-check"></i> 添加用户';
        }

        /**
         * 更新验证状态
         */
        updateValidation(field, message, valid) {
            const indicator = document.getElementById(`${field}Validation`);
            if (!indicator) return;

            indicator.textContent = message;
            indicator.className = 'user-modal-validation';

            if (valid !== undefined) {
                indicator.classList.add(valid ? 'success' : 'error');
            }

            // 更新输入框样式
            const input = document.getElementById(field);
            if (input && valid !== undefined) {
                if (valid) {
                    input.classList.remove('error');
                    input.classList.add('success');
                } else {
                    input.classList.remove('success');
                    input.classList.add('error');
                }
            }
        }

        /**
         * 切换密码显示/隐藏
         */
        togglePasswordVisibility(inputId) {
            const passwordInput = document.getElementById(inputId);
            const toggleButton = document.getElementById(`${inputId}Toggle`);
            const icon = toggleButton.querySelector('i');

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
         * 切换高级选项
         */
        toggleAdvanced(e) {
            const toggle = e.currentTarget;
            const content = document.getElementById('advancedContent');
            const icon = toggle.querySelector('.user-modal-toggle-icon');
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
     * 检查用户名或邮箱是否已存在
     */
    async checkDuplicate(type, value) {
        if (!value) {
            return {
                valid: false,
                message: type === 'username' ? '请输入用户名' : '请输入邮箱'
            };
        }

        try {
            // 调用API检查重复
            const response = await fetch(`/admin/api/users/check-duplicate?${type}=${encodeURIComponent(value.trim())}`);
            const data = await response.json();

            if (data.exists) {
                return {
                    valid: false,
                    message: type === 'username' ? '该用户名已存在' : '该邮箱已被使用'
                };
            }

            return {
                valid: true,
                message: type === 'username' ? '用户名可用' : '邮箱可用'
            };
        } catch (error) {
            console.error('[UserAddModal] 检查重复时出错:', error);
            return {
                valid: true, // 网络错误时默认认为可用
                message: ''
            };
        }
    }

    /**
     * 构建表单提交数据
     */
    buildPayload(formData) {
        return {
            username: formData.get('username')?.trim(),
            email: formData.get('email')?.trim(),
            password: formData.get('password'),
            roles: Array.from(this.selectedRoles),
            workDirectory: document.getElementById('workDirectory')?.value.trim() || null,
            enabled: !document.getElementById('forceReset')?.checked,
            isFirstLogin: true
        };
    }
    
        /**
         * 处理表单提交
         */
        async handleSubmit(e) {
            e.preventDefault();

            const formData = new FormData(e.target);
            const password = document.getElementById('password').value;
            const confirmPassword = document.getElementById('confirmPassword').value;

            // 表单验证
            let isValid = true;

            if (!formData.get('username') || !formData.get('email')) {
                this.showToast('请填写必填字段', 'error');
                isValid = false;
            }

            if (!password || password.length < 8) {
                this.updateValidation('password', '密码至少需要8个字符', false);
                isValid = false;
            }

            if (password !== confirmPassword) {
                this.updateValidation('confirmPassword', '两次输入的密码不一致', false);
                isValid = false;
            }

            if (!this.selectedRoles.size) {
                this.updateValidation('roles', '请至少选择一个角色', false);
                isValid = false;
            }

            if (!isValid) {
                return;
            }

            // 构建提交数据
            const newUser = this.buildPayload(formData);

            const submitBtn = document.getElementById('submitBtn');
            submitBtn.disabled = true;
            submitBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> 添加中...';

            try {
                const response = await fetch('/admin/api/users', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify(newUser)
                });

                if (response.ok) {
                    const data = await response.json();
                    this.showToast('用户添加成功！', 'success');

                    // 延迟关闭并刷新列表
                    setTimeout(() => {
                        this.close();
                        if (typeof window.refreshUserList === 'function') {
                            window.refreshUserList();
                        }
                    }, 1500);
                } else {
                    const error = await response.json();
                    this.showToast(error.message || '添加用户失败', 'error');
                    submitBtn.disabled = false;
                    submitBtn.innerHTML = '<i class="fas fa-check"></i> 添加用户';
                }
            } catch (error) {
                console.error('Submit error:', error);
                this.showToast('添加用户失败: ' + error.message, 'error');
                submitBtn.disabled = false;
                submitBtn.innerHTML = '<i class="fas fa-check"></i> 添加用户';
            }
        }

        /**
         * 显示Toast通知
         */
        showToast(message, type = 'info') {
            const container = document.getElementById('toastContainer');
            const toast = document.createElement('div');
            toast.className = `user-modal-toast ${type} show`;

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
    window.userAddModal = null;

    // 初始化函数
    function initUserAddModal() {
        if (!window.userAddModal) {
            window.userAddModal = new UserAddModal();
        }
        return window.userAddModal;
    }

    // 如果DOM已加载完成，立即初始化；否则等待DOMContentLoaded事件
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initUserAddModal);
    } else {
        initUserAddModal();
    }

    // 提供全局打开方法
    window.openUserAddModal = function() {
        console.log('[UserAddModal] openUserAddModal called');
        console.log('[UserAddModal] window.userAddModal:', window.userAddModal);

        // 确保模态弹窗已初始化
        const modal = window.userAddModal || initUserAddModal();
        console.log('[UserAddModal] modal instance:', modal);

        if (modal) {
            modal.open();
        } else {
            console.error('[UserAddModal] Failed to initialize user modal');
        }
    };

    // 立即初始化，确保在任何调用之前模态弹窗已准备好
    console.log('[UserAddModal] Script loaded, initializing...');
    initUserAddModal();
    console.log('[UserAddModal] Initialization complete, window.openUserAddModal available:', typeof window.openUserAddModal === 'function');

})();