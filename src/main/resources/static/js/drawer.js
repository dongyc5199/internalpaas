/**
 * 抽屉组件管理器 - 现代化交互设计
 */
class DrawerManager {
    constructor() {
        this.currentDrawer = null;
        this.isSubmitting = false;
        this.hasUnsavedChanges = false;
        this.init();
    }

    init() {
        this.bindEvents();
        this.createDrawerContainers();
    }

    // 创建抽屉容器
    createDrawerContainers() {
        // 创建遮罩层
        this.overlay = document.createElement('div');
        this.overlay.className = 'drawer-overlay';
        this.overlay.addEventListener('click', () => this.closeDrawer());
        document.body.appendChild(this.overlay);

        // 创建服务器抽屉
        this.serverDrawer = this.createServerDrawer();
        document.body.appendChild(this.serverDrawer);

        // 创建用户抽屉
        this.userDrawer = this.createUserDrawer();
        document.body.appendChild(this.userDrawer);
    }

    // 创建服务器抽屉HTML
    createServerDrawer() {
        const drawer = document.createElement('div');
        drawer.className = 'drawer server-drawer drawer-compact drawer-modern';
        drawer.innerHTML = `
            <div class="drawer-header">
                <h2 class="drawer-title">
                    <div class="drawer-title-icon">🖥️</div>
                    <span class="drawer-title-text">添加服务器</span>
                </h2>
                <button type="button" class="drawer-close" onclick="drawerManager.closeDrawer()">
                    ✕
                </button>
            </div>
            <div class="drawer-content">
                <form class="drawer-form" id="serverDrawerForm">
                    <input type="hidden" name="_csrf" value="">
                    <input type="hidden" name="id" value="">
                    
                    <!-- 消息显示区域 -->
                    <div class="drawer-messages"></div>
                    
                    <!-- 基本信息 -->
                    <div class="form-section">
                        <h3 class="form-section-title">
                            <div class="form-section-icon basic">📝</div>
                            基本信息
                        </h3>
                        
                        <div class="form-group">
                            <label for="server-name" class="form-label">
                                服务器名称 <span class="required">*</span>
                            </label>
                            <div class="input-group">
                                <span class="input-group-icon">🏷️</span>
                                <input type="text" 
                                       class="form-control" 
                                       id="server-name" 
                                       name="name" 
                                       placeholder="例如：本地开发服务器" 
                                       required>
                            </div>
                            <div class="invalid-feedback"></div>
                        </div>
                        
                        <div class="form-group">
                            <label for="server-description" class="form-label">描述</label>
                            <textarea class="form-control" 
                                      id="server-description" 
                                      name="description" 
                                      rows="2" 
                                      placeholder="服务器的详细描述信息..."></textarea>
                        </div>
                        
                        <div class="form-group">
                            <label for="server-work-directory" class="form-label">
                                基础工作目录 <span class="required">*</span>
                            </label>
                            <div class="input-group">
                                <span class="input-group-icon">📁</span>
                                <input type="text" 
                                       class="form-control" 
                                       id="server-work-directory" 
                                       name="baseWorkDirectory" 
                                       placeholder="./workspaces" 
                                       required>
                            </div>
                            <div class="form-text">这是所有用户工作目录的基础路径</div>
                            <div class="invalid-feedback"></div>
                        </div>
                    </div>


                    <!-- SSH连接配置 -->
                    <div class="form-section">
                        <h3 class="form-section-title">
                            <div class="form-section-icon ssh">🔐</div>
                            SSH连接配置
                        </h3>
                        
                        <div class="form-group">
                            <label for="server-hostname" class="form-label">
                                主机名/IP地址 <span class="required">*</span>
                            </label>
                            <div class="input-group">
                                <span class="input-group-icon">🌍</span>
                                <input type="text" 
                                       class="form-control" 
                                       id="server-hostname" 
                                       name="hostname" 
                                       placeholder="localhost" 
                                       required>
                            </div>
                            <div class="invalid-feedback"></div>
                        </div>
                        
                        <div class="form-row">
                            <div class="col-4">
                                <div class="form-group compact">
                                    <label for="server-ssh-port" class="form-label">
                                        SSH端口 <span class="required">*</span>
                                    </label>
                                    <input type="number" 
                                           class="form-control" 
                                           id="server-ssh-port" 
                                           name="sshPort" 
                                           placeholder="22" 
                                           min="1" 
                                           max="65535" required>
                                    <div class="invalid-feedback"></div>
                                </div>
                            </div>
                            
                            <div class="col-8">
                                <div class="form-group compact">
                                    <label for="server-ssh-username" class="form-label">
                                        SSH用户名 <span class="required">*</span>
                                    </label>
                                    <input type="text" 
                                           class="form-control" 
                                           id="server-ssh-username" 
                                           name="sshUsername" 
                                           placeholder="root" required>
                                    <div class="invalid-feedback"></div>
                                </div>
                            </div>
                        </div>
                        
                        <div class="form-group">
                            <label for="server-ssh-password" class="form-label">
                                SSH密码 <span class="required">*</span>
                            </label>
                            <div class="input-group">
                                <span class="input-group-icon">🔑</span>
                                <input type="password" 
                                       class="form-control" 
                                       id="server-ssh-password" 
                                       name="sshPassword" 
                                       placeholder="必填：密码认证" 
                                       required>
                            </div>
                            <div class="form-text">仅支持密码认证，请填写SSH密码</div>
                            <div class="invalid-feedback"></div>
                        </div>
                    </div>

                    <!-- 服务器设置 -->
                    <div class="form-section">
                        <h3 class="form-section-title">
                            <div class="form-section-icon settings">⚙️</div>
                            服务器设置
                        </h3>
                        
                        <div class="form-check">
                            <input class="form-check-input" 
                                   type="checkbox" 
                                   id="server-active" 
                                   name="active" 
                                   checked>
                            <label class="form-check-label" for="server-active">
                                启用此服务器
                            </label>
                        </div>
                    </div>
                </form>
            </div>
            <div class="drawer-footer">
                <button type="button" class="drawer-btn drawer-btn-secondary" onclick="drawerManager.closeDrawer()">
                    <span>✕</span>
                    取消
                </button>
                <button type="button" class="drawer-btn drawer-btn-primary" onclick="drawerManager.submitServerForm()" id="serverSubmitBtn">
                    <span>💾</span>
                    <span class="submit-text">保存服务器</span>
                </button>
            </div>
            <div class="drawer-loading">
                <div class="loading-spinner"></div>
            </div>
        `;
        return drawer;
    }

    // 创建用户抽屉HTML
    createUserDrawer() {
        const drawer = document.createElement('div');
        drawer.className = 'drawer user-drawer drawer-compact drawer-modern';
        drawer.innerHTML = `
            <div class="drawer-header">
                <h2 class="drawer-title">
                    <div class="drawer-title-icon">👥</div>
                    <span class="drawer-title-text">添加用户</span>
                </h2>
                <button type="button" class="drawer-close" onclick="drawerManager.closeDrawer()">
                    ✕
                </button>
            </div>
            <div class="drawer-content">
                    <div class="drawer-messages"></div>
                    
                    <form id="userDrawerForm" class="drawer-form">
                        <input type="hidden" name="_csrf" value="">
                        <input type="hidden" name="id" value="">
                        
                        <!-- 基本信息 -->
                        <div class="form-section">
                            <h3 class="form-section-title">
                                <div class="form-section-icon">👤</div>
                                基本信息
                            </h3>
                            
                            <div class="form-row">
                                <div class="col-6">
                                    <div class="form-group compact">
                                        <label class="form-label" for="userUsername">
                                            <span class="form-label-icon">📝</span>
                                            <span class="form-label-text">用户名</span>
                                            <span class="form-label-required">*</span>
                                        </label>
                                        <input type="text" id="userUsername" name="username" class="form-control" 
                                               placeholder="请输入用户名" required>
                                        <div class="invalid-feedback"></div>
                                    </div>
                                </div>
                                <div class="col-6">
                                    <div class="form-group compact">
                                        <label class="form-label" for="userEmail">
                                            <span class="form-label-icon">📧</span>
                                            <span class="form-label-text">邮箱地址</span>
                                            <span class="form-label-required">*</span>
                                        </label>
                                        <input type="email" id="userEmail" name="email" class="form-control" 
                                               placeholder="请输入邮箱地址" required>
                                        <div class="invalid-feedback"></div>
                                    </div>
                                </div>
                            </div>
                            
                            <div class="form-group compact">
                                <label class="form-label" for="userWorkDirectory">
                                    <span class="form-label-icon">📁</span>
                                    <span class="form-label-text">工作目录</span>
                                </label>
                                <input type="text" id="userWorkDirectory" name="workDirectory" class="form-control" 
                                       placeholder="自动生成或手动输入">
                                <div class="form-help">
                                    <span class="form-help-icon">💡</span>
                                    <span class="form-help-text">留空将根据用户名自动生成</span>
                                </div>
                                <div class="invalid-feedback"></div>
                            </div>
                        </div>
                        
                        <!-- 密码设置 -->
                        <div class="form-section">
                            <h3 class="form-section-title">
                                <div class="form-section-icon">🔐</div>
                                密码设置
                            </h3>
                            
                            <div class="form-row">
                                <div class="col-6">
                                    <div class="form-group compact">
                                        <label class="form-label" for="userPassword">
                                            <span class="form-label-icon">🔑</span>
                                            <span class="form-label-text">登录密码</span>
                                            <span class="form-label-required" id="passwordRequired">*</span>
                                        </label>
                                        <div class="password-input-group">
                                            <input type="password" id="userPassword" name="password" class="form-control" 
                                                   placeholder="请输入密码">
                                            <button type="button" class="password-toggle" onclick="drawerManager.togglePasswordVisibility('userPassword')">
                                                <i class="fas fa-eye"></i>
                                            </button>
                                        </div>
                                        <div class="form-help edit-mode-note" style="display: none;">
                                            <span class="form-help-icon">ℹ️</span>
                                            <span class="form-help-text">留空保持原密码不变</span>
                                        </div>
                                        <div class="invalid-feedback"></div>
                                    </div>
                                </div>
                                <div class="col-6">
                                    <div class="form-group compact">
                                        <label class="form-label" for="userPasswordConfirm">
                                            <span class="form-label-icon">🔒</span>
                                            <span class="form-label-text">确认密码</span>
                                            <span class="form-label-required" id="confirmRequired">*</span>
                                        </label>
                                        <input type="password" id="userPasswordConfirm" name="passwordConfirm" class="form-control" 
                                               placeholder="请再次输入密码">
                                        <div class="invalid-feedback"></div>
                                    </div>
                                </div>
                            </div>
                            
                            <div class="password-strength">
                                <div class="password-strength-bar">
                                    <div class="password-strength-fill"></div>
                                </div>
                                <span class="password-strength-text">密码强度</span>
                            </div>
                        </div>
                        
                        <!-- 权限设置 -->
                        <div class="form-section">
                            <h3 class="form-section-title">
                                <div class="form-section-icon">🛡️</div>
                                权限设置
                            </h3>
                            
                            <div class="form-group compact">
                                <label class="form-label">
                                    <span class="form-label-icon">👑</span>
                                    <span class="form-label-text">用户角色</span>
                                    <span class="form-label-required">*</span>
                                </label>
                                <div class="role-selection compact">
                                    <div class="role-card compact" data-role="USER">
                                        <div class="role-card-header">
                                            <input type="checkbox" name="roles" value="USER" id="roleUser">
                                            <label for="roleUser" class="role-card-checkbox"></label>
                                            <div class="role-icon">👨‍💻</div>
                                        </div>
                                        <div class="role-card-body">
                                            <h6 class="role-card-title">开发者</h6>
                                        </div>
                                    </div>
                                    
                                    <div class="role-card compact" data-role="ADMIN">
                                        <div class="role-card-header">
                                            <input type="checkbox" name="roles" value="ADMIN" id="roleAdmin">
                                            <label for="roleAdmin" class="role-card-checkbox"></label>
                                            <div class="role-icon">🛡️</div>
                                        </div>
                                        <div class="role-card-body">
                                            <h6 class="role-card-title">管理员</h6>
                                        </div>
                                    </div>
                                    
                                    <div class="role-card compact" data-role="SUPER_ADMIN">
                                        <div class="role-card-header">
                                            <input type="checkbox" name="roles" value="SUPER_ADMIN" id="roleSuperAdmin">
                                            <label for="roleSuperAdmin" class="role-card-checkbox"></label>
                                            <div class="role-icon">👑</div>
                                        </div>
                                        <div class="role-card-body">
                                            <h6 class="role-card-title">超级管理员</h6>
                                        </div>
                                    </div>
                                </div>
                                <div class="invalid-feedback"></div>
                            </div>
                        </div>
                        
                        <!-- 账户状态 -->
                        <div class="form-section">
                            <h3 class="form-section-title">
                                <div class="form-section-icon">⚡</div>
                                账户状态
                            </h3>
                            
                            <div class="form-check">
                                <input class="form-check-input" 
                                       type="checkbox" 
                                       id="userEnabled" 
                                       name="enabled" 
                                       checked>
                                <label class="form-check-label" for="userEnabled">
                                    启用此账户
                                </label>
                            </div>
                        </div>
                    </form>
                </div>
                <div class="drawer-footer">
                    <button type="button" class="drawer-btn drawer-btn-secondary" onclick="drawerManager.closeDrawer()">
                        <span>✕</span>
                        取消
                    </button>
                    <button type="button" class="drawer-btn drawer-btn-primary" onclick="drawerManager.submitUserForm()" id="userSubmitBtn">
                        <span>💾</span>
                        <span class="submit-text">保存用户</span>
                    </button>
                </div>
                <div class="drawer-loading">
                    <div class="loading-content">
                        <div class="loading-spinner"></div>
                        <span class="loading-text">处理中...</span>
                    </div>
                </div>
        `;
        return drawer;
    }

    // 绑定事件
    bindEvents() {
        // ESC键关闭抽屉
        document.addEventListener('keydown', (e) => {
            if (e.key === 'Escape' && this.currentDrawer) {
                this.closeDrawer();
            }
        });

        // 防止页面关闭时丢失未保存数据
        window.addEventListener('beforeunload', (e) => {
            if (this.hasUnsavedChanges) {
                e.preventDefault();
                e.returnValue = '您有未保存的更改，确定要离开吗？';
            }
        });

        // 延迟绑定用户表单事件（等DOM创建完成）
        setTimeout(() => {
            this.bindUserFormEvents();
        }, 100);
    }

    // 获取CSRF令牌
    getCSRFToken() {
        const meta = document.querySelector('meta[name="_csrf"]');
        return meta ? meta.getAttribute('content') : '';
    }
    
    // 获取CSRF头名称
    getCSRFHeaderName() {
        const meta = document.querySelector('meta[name="_csrf_header"]');
        return meta ? meta.getAttribute('content') : 'X-CSRF-TOKEN';
    }

    // 显示消息
    showMessage(container, type, message) {
        const messageDiv = document.createElement('div');
        messageDiv.className = `drawer-message drawer-message-${type}`;
        
        const icon = type === 'success' ? '✅' : type === 'error' ? '❌' : '⚠️';
        messageDiv.innerHTML = `<span>${icon}</span><span>${message}</span>`;
        
        container.innerHTML = '';
        container.appendChild(messageDiv);
        
        // 自动隐藏成功消息
        if (type === 'success') {
            setTimeout(() => {
                if (messageDiv.parentNode) {
                    messageDiv.remove();
                }
            }, 3000);
        }
    }

    // 验证表单
    validateServerForm(form) {
        let isValid = true;
        const errors = {};

        // 验证服务器名称
        const name = form.querySelector('[name="name"]').value.trim();
        if (!name) {
            errors.name = '服务器名称不能为空';
            isValid = false;
        } else if (name.length < 2) {
            errors.name = '服务器名称至少需要2个字符';
            isValid = false;
        }

        // 验证主机名
        const hostname = form.querySelector('[name="hostname"]').value.trim();
        if (!hostname) {
            errors.hostname = '主机名不能为空';
            isValid = false;
        }

        // 验证工作目录
        const workDir = form.querySelector('[name="baseWorkDirectory"]').value.trim();
        if (!workDir) {
            errors.baseWorkDirectory = '工作目录不能为空';
            isValid = false;
        }

        // 验证SSH端口
        const sshPortField = form.querySelector('[name="sshPort"]');
        if (sshPortField) {
            const sshPort = parseInt(sshPortField.value);
            if (!sshPort || sshPort < 1 || sshPort > 65535) {
                errors.sshPort = 'SSH端口必须在1-65535之间';
                isValid = false;
            }
        }

        // 验证SSH用户名
        const sshUsernameField = form.querySelector('[name="sshUsername"]');
        if (sshUsernameField) {
            const sshUsername = sshUsernameField.value.trim();
            if (!sshUsername) {
                errors.sshUsername = 'SSH用户名不能为空';
                isValid = false;
            }
        }

        // 验证SSH密码
        const sshPasswordField = form.querySelector('[name="sshPassword"]');
        if (sshPasswordField) {
            const sshPassword = sshPasswordField.value;
            if (!sshPassword) {
                errors.sshPassword = 'SSH密码不能为空';
                isValid = false;
            }
        }

        // 显示验证错误
        this.displayValidationErrors(form, errors);
        
        return isValid;
    }

    // 显示验证错误
    displayValidationErrors(form, errors) {
        // 清除之前的错误状态
        form.querySelectorAll('.form-control').forEach(input => {
            input.classList.remove('is-invalid', 'is-valid');
        });
        form.querySelectorAll('.invalid-feedback').forEach(feedback => {
            feedback.textContent = '';
        });

        // 显示新的错误
        Object.keys(errors).forEach(field => {
            const input = form.querySelector(`[name="${field}"]`);
            const feedback = input.closest('.form-group').querySelector('.invalid-feedback');
            
            if (input && feedback) {
                input.classList.add('is-invalid');
                feedback.textContent = errors[field];
                
                // 添加摇晃动画
                input.closest('.form-group').classList.add('error-shake');
                setTimeout(() => {
                    input.closest('.form-group').classList.remove('error-shake');
                }, 500);
            }
        });

        // 为有效字段添加成功状态
        form.querySelectorAll('.form-control').forEach(input => {
            if (!input.classList.contains('is-invalid') && input.value.trim()) {
                input.classList.add('is-valid');
            }
        });
    }

    // 打开服务器抽屉
    openServerDrawer(serverId = null) {
        this.currentDrawer = this.serverDrawer;
        
        // 更新标题
        const titleText = this.serverDrawer.querySelector('.drawer-title-text');
        const submitBtn = this.serverDrawer.querySelector('#serverSubmitBtn .submit-text');
        
        if (serverId) {
            titleText.textContent = '编辑服务器';
            submitBtn.textContent = '更新服务器';
            this.loadServerData(serverId);
        } else {
            titleText.textContent = '添加服务器';
            submitBtn.textContent = '保存服务器';
            this.resetServerForm();
        }

        // 设置CSRF令牌
        const tokenInput = this.serverDrawer.querySelector('[name="_csrf"]');
        tokenInput.value = this.getCSRFToken();

        // 显示抽屉
        this.showDrawer(this.serverDrawer);
        
        // 聚焦第一个输入框
        setTimeout(() => {
            const firstInput = this.serverDrawer.querySelector('.form-control');
            if (firstInput) firstInput.focus();
        }, 300);
    }

    // 加载服务器数据
    async loadServerData(serverId) {
        try {
            this.showLoading(true);
            
            const response = await fetch(`/admin/servers/${serverId}/data`);
            if (!response.ok) {
                throw new Error('获取服务器数据失败');
            }
            
            const serverData = await response.json();
            this.fillServerForm(serverData);
            
        } catch (error) {
            this.showMessage(
                this.serverDrawer.querySelector('.drawer-messages'),
                'error',
                error.message
            );
        } finally {
            this.showLoading(false);
        }
    }

    // 填充服务器表单
    fillServerForm(data) {
        const form = this.serverDrawer.querySelector('#serverDrawerForm');
        
        // 设置隐藏字段
        form.querySelector('[name="id"]').value = data.id || '';
        
        // 填充表单字段
        const fields = ['name', 'description', 'hostname', 'port', 'baseWorkDirectory', 'sshPort', 'sshUsername', 'sshPassword'];
        fields.forEach(field => {
            const input = form.querySelector(`[name="${field}"]`);
            if (input && data[field] !== undefined) {
                input.value = data[field];
            }
        });
        
        // 设置复选框
        const activeCheckbox = form.querySelector('[name="active"]');
        if (activeCheckbox) {
            activeCheckbox.checked = data.active !== false;
        }
    }

    // 重置服务器表单
    resetServerForm() {
        const form = this.serverDrawer.querySelector('#serverDrawerForm');
        form.reset();
        form.querySelector('[name="id"]').value = '';
        
        // 清除验证状态
        form.querySelectorAll('.form-control').forEach(input => {
            input.classList.remove('is-invalid', 'is-valid');
        });
        form.querySelectorAll('.invalid-feedback').forEach(feedback => {
            feedback.textContent = '';
        });
        
        // 清除消息
        this.serverDrawer.querySelector('.drawer-messages').innerHTML = '';
        
        // 设置默认值（仅设置必要的默认值）
        const activeField = form.querySelector('[name="active"]');
        if (activeField) activeField.checked = true;
    }

    // 提交服务器表单
    async submitServerForm() {
        if (this.isSubmitting) return;

        const form = this.serverDrawer.querySelector('#serverDrawerForm');
        const messageContainer = this.serverDrawer.querySelector('.drawer-messages');
        
        // 验证表单
        if (!this.validateServerForm(form)) {
            this.showMessage(messageContainer, 'error', '请检查并修正表单中的错误');
            return;
        }

        try {
            this.isSubmitting = true;
            this.showLoading(true);
            
            // 准备表单数据
            const formData = new FormData(form);
            const serverId = formData.get('id');
            
            // 确定请求URL和方法
            const isEdit = serverId && serverId !== '';
            const url = isEdit ? `/admin/api/servers/${serverId}/update` : '/admin/api/servers';
            const method = 'POST';
            
            // 添加CSRF令牌
            formData.append('_csrf', this.getCSRFToken());
            
            // 发送请求
            const response = await fetch(url, {
                method: method,
                body: formData,
                headers: {
                    'X-Requested-With': 'XMLHttpRequest'
                }
            });

            if (!response.ok) {
                const errorText = await response.text();
                throw new Error(errorText || '服务器请求失败');
            }

            // 处理成功响应
            const result = await response.json().catch(() => ({}));
            
            this.showMessage(messageContainer, 'success', 
                isEdit ? '服务器更新成功！' : '服务器创建成功！');
            
            // 添加成功动画
            form.classList.add('success-pulse');
            setTimeout(() => form.classList.remove('success-pulse'), 600);
            
            // 重置未保存标志
            this.hasUnsavedChanges = false;
            
            // 延迟关闭抽屉并刷新页面
            setTimeout(() => {
                this.closeDrawer();
                // 刷新服务器列表
                if (typeof refreshServerList === 'function') {
                    refreshServerList();
                } else {
                    window.location.reload();
                }
            }, 1500);

        } catch (error) {
            console.error('提交服务器表单失败:', error);
            this.showMessage(messageContainer, 'error', error.message);
        } finally {
            this.isSubmitting = false;
            this.showLoading(false);
        }
    }

    // 显示抽屉
    showDrawer(drawer) {
        // 隐藏其他抽屉
        document.querySelectorAll('.drawer').forEach(d => {
            if (d !== drawer) {
                d.classList.remove('show');
            }
        });

        // 显示遮罩和抽屉
        this.overlay.classList.add('show');
        drawer.classList.add('show');
        
        // 防止背景滚动
        document.body.style.overflow = 'hidden';
        
        // 监听表单变化
        this.monitorFormChanges(drawer);
    }

    // 关闭抽屉
    closeDrawer() {
        if (this.hasUnsavedChanges) {
            if (!confirm('您有未保存的更改，确定要关闭吗？')) {
                return;
            }
        }

        this.overlay.classList.remove('show');
        if (this.currentDrawer) {
            this.currentDrawer.classList.remove('show');
        }
        
        // 恢复背景滚动
        document.body.style.overflow = '';
        
        // 重置状态
        this.currentDrawer = null;
        this.hasUnsavedChanges = false;
        this.isSubmitting = false;
    }

    // ========== 用户抽屉相关方法 ==========
    
    // 打开用户抽屉
    openUserDrawer(userId = null) {
        this.currentDrawer = this.userDrawer;
        
        // 更新标题
        const titleText = this.userDrawer.querySelector('.drawer-title-text');
        const submitBtn = this.userDrawer.querySelector('#userSubmitBtn .submit-text');
        
        if (userId) {
            titleText.textContent = '编辑用户';
            submitBtn.textContent = '更新用户';
            this.loadUserData(userId);
        } else {
            titleText.textContent = '添加用户';
            submitBtn.textContent = '保存用户';
            this.resetUserForm();
        }

        // 设置CSRF令牌
        const tokenInput = this.userDrawer.querySelector('[name="_csrf"]');
        tokenInput.value = this.getCSRFToken();

        // 显示抽屉
        this.showDrawer(this.userDrawer);
        
        // 聚焦第一个输入框
        setTimeout(() => {
            const firstInput = this.userDrawer.querySelector('.form-control');
            if (firstInput) firstInput.focus();
        }, 300);
    }

    // 加载用户数据
    async loadUserData(userId) {
        try {
            this.showLoading(true);
            
            const response = await fetch(`/admin/users/${userId}/data`);
            if (!response.ok) {
                throw new Error('获取用户数据失败');
            }
            
            const userData = await response.json();
            this.fillUserForm(userData);
            
        } catch (error) {
            this.showMessage(
                this.userDrawer.querySelector('.drawer-messages'),
                'error',
                error.message
            );
        } finally {
            this.showLoading(false);
        }
    }

    // 填充用户表单
    fillUserForm(data) {
        const form = this.userDrawer.querySelector('#userDrawerForm');
        
        // 设置隐藏字段
        form.querySelector('[name="id"]').value = data.id || '';
        
        // 填充基本信息
        const fields = ['username', 'email', 'workDirectory'];
        fields.forEach(field => {
            const input = form.querySelector(`[name="${field}"]`);
            if (input && data[field] !== undefined) {
                input.value = data[field];
            }
        });
        
        // 设置角色复选框
        form.querySelectorAll('[name="roles"]').forEach(checkbox => {
            checkbox.checked = false;
        });
        if (data.roles && Array.isArray(data.roles)) {
            data.roles.forEach(role => {
                const checkbox = form.querySelector(`[name="roles"][value="${role}"]`);
                if (checkbox) {
                    checkbox.checked = true;
                }
            });
        }
        
        // 设置账户状态
        const enabledCheckbox = form.querySelector('[name="enabled"]');
        if (enabledCheckbox) {
            enabledCheckbox.checked = data.enabled !== false;
        }
        
        // 编辑模式下显示密码提示
        const editModeNotes = form.querySelectorAll('.edit-mode-note');
        editModeNotes.forEach(note => note.style.display = 'block');
        
        // 编辑模式下密码不是必填
        const passwordRequired = form.querySelector('#passwordRequired');
        const confirmRequired = form.querySelector('#confirmRequired');
        if (passwordRequired) passwordRequired.style.display = 'none';
        if (confirmRequired) confirmRequired.style.display = 'none';
    }

    // 重置用户表单
    resetUserForm() {
        const form = this.userDrawer.querySelector('#userDrawerForm');
        form.reset();
        form.querySelector('[name="id"]').value = '';
        
        // 清除验证状态
        form.querySelectorAll('.form-control').forEach(input => {
            input.classList.remove('is-invalid', 'is-valid');
        });
        form.querySelectorAll('.invalid-feedback').forEach(feedback => {
            feedback.textContent = '';
        });
        
        // 清除消息
        this.userDrawer.querySelector('.drawer-messages').innerHTML = '';
        
        // 隐藏编辑模式提示
        const editModeNotes = form.querySelectorAll('.edit-mode-note');
        editModeNotes.forEach(note => note.style.display = 'none');
        
        // 重置密码强度指示器
        this.updatePasswordStrength('');
        
        // 设置默认值
        form.querySelector('[name="enabled"]').checked = true;
        form.querySelector('[name="roles"][value="USER"]').checked = true;
        
        // 显示必填标记
        const passwordRequired = form.querySelector('#passwordRequired');
        const confirmRequired = form.querySelector('#confirmRequired');
        if (passwordRequired) passwordRequired.style.display = 'inline';
        if (confirmRequired) confirmRequired.style.display = 'inline';
    }

    // 提交用户表单
    async submitUserForm() {
        if (this.isSubmitting) return;

        const form = this.userDrawer.querySelector('#userDrawerForm');
        const messageContainer = this.userDrawer.querySelector('.drawer-messages');
        
        // 清除之前的消息
        messageContainer.innerHTML = '';
        
        // 验证表单
        if (!this.validateUserForm(form)) {
            return;
        }

        this.isSubmitting = true;
        this.showLoading(true);

        try {
            const formData = new FormData(form);
            const userId = formData.get('id');
            
            // 收集角色数据
            const roles = [];
            form.querySelectorAll('[name="roles"]:checked').forEach(checkbox => {
                roles.push(checkbox.value);
            });
            
            // 构建请求数据
            const userData = {
                username: formData.get('username'),
                email: formData.get('email'),
                password: formData.get('password'),
                workDirectory: formData.get('workDirectory'),
                roles: roles,
                enabled: formData.get('enabled') === 'on'
            };
            
            const url = userId ? `/admin/api/users/${userId}/update` : '/admin/api/users';
            const method = userId ? 'PUT' : 'POST';
            
            const response = await fetch(url, {
                method: method,
                headers: {
                    'Content-Type': 'application/json',
                    [this.getCSRFHeaderName()]: this.getCSRFToken()
                },
                body: JSON.stringify(userData)
            });

            const result = await response.json();

            if (response.ok && result.status === 'success') {
                this.showMessage(messageContainer, 'success', result.message);
                this.hasUnsavedChanges = false;
                
                // 延迟关闭抽屉并刷新页面
                setTimeout(() => {
                    this.closeDrawer();
                    // 刷新用户列表
                    if (typeof refreshUserList === 'function') {
                        refreshUserList();
                    } else {
                        window.location.reload();
                    }
                }, 1500);
            } else {
                this.showMessage(messageContainer, 'error', result.message || '操作失败');
            }
        } catch (error) {
            this.showMessage(messageContainer, 'error', '网络错误：' + error.message);
        } finally {
            this.isSubmitting = false;
            this.showLoading(false);
        }
    }

    // 验证用户表单
    validateUserForm(form) {
        let isValid = true;
        const errors = {};

        // 验证用户名
        const username = form.querySelector('[name="username"]').value.trim();
        if (!username) {
            errors.username = '用户名不能为空';
            isValid = false;
        } else if (username.length < 3) {
            errors.username = '用户名至少需要3个字符';
            isValid = false;
        } else if (!/^[a-zA-Z0-9_-]+$/.test(username)) {
            errors.username = '用户名只能包含字母、数字、下划线和连字符';
            isValid = false;
        }

        // 验证邮箱
        const email = form.querySelector('[name="email"]').value.trim();
        if (!email) {
            errors.email = '邮箱地址不能为空';
            isValid = false;
        } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
            errors.email = '请输入有效的邮箱地址';
            isValid = false;
        }

        // 验证密码（仅在新建或修改密码时）
        const password = form.querySelector('[name="password"]').value;
        const passwordConfirm = form.querySelector('[name="passwordConfirm"]').value;
        const isEditMode = form.querySelector('[name="id"]').value;
        
        if (!isEditMode || password) { // 新建模式或编辑时输入了密码
            if (!password) {
                errors.password = '密码不能为空';
                isValid = false;
            } else if (password.length < 6) {
                errors.password = '密码至少需要6个字符';
                isValid = false;
            }
            
            if (password !== passwordConfirm) {
                errors.passwordConfirm = '两次输入的密码不一致';
                isValid = false;
            }
        }

        // 验证角色选择
        const checkedRoles = form.querySelectorAll('[name="roles"]:checked');
        if (checkedRoles.length === 0) {
            errors.roles = '请至少选择一个角色';
            isValid = false;
        }

        // 显示验证错误
        this.displayValidationErrors(form, errors);
        
        return isValid;
    }

    // 更新密码强度指示器
    updatePasswordStrength(password) {
        const strengthBar = this.userDrawer?.querySelector('.password-strength-fill');
        const strengthText = this.userDrawer?.querySelector('.password-strength-text');
        
        if (!strengthBar || !strengthText) return;
        
        const strength = this.calculatePasswordStrength(password);
        const strengthLevels = ['很弱', '弱', '中等', '强', '很强'];
        const strengthColors = ['#ef4444', '#f59e0b', '#3b82f6', '#10b981', '#059669'];
        
        strengthBar.style.width = `${(strength + 1) * 20}%`;
        strengthBar.style.background = strengthColors[strength];
        strengthText.textContent = `密码强度: ${strengthLevels[strength]}`;
    }

    // 计算密码强度
    calculatePasswordStrength(password) {
        if (!password) return 0;
        
        let strength = 0;
        
        // 长度检查
        if (password.length >= 8) strength++;
        if (password.length >= 12) strength++;
        
        // 字符类型检查
        if (/[a-z]/.test(password)) strength++;
        if (/[A-Z]/.test(password)) strength++;
        if (/[0-9]/.test(password)) strength++;
        if (/[^a-zA-Z0-9]/.test(password)) strength++;
        
        return Math.min(strength, 4);
    }

    // 切换密码可见性
    togglePasswordVisibility(inputId) {
        const input = document.getElementById(inputId);
        const button = input.parentElement.querySelector('.password-toggle i');
        
        if (input.type === 'password') {
            input.type = 'text';
            button.className = 'fas fa-eye-slash';
        } else {
            input.type = 'password';
            button.className = 'fas fa-eye';
        }
    }

    // 绑定用户表单事件
    bindUserFormEvents() {
        if (!this.userDrawer) return;
        
        const form = this.userDrawer.querySelector('#userDrawerForm');
        if (!form) return;
        
        // 用户名实时验证和自动生成工作目录
        const usernameInput = form.querySelector('[name="username"]');
        const workDirInput = form.querySelector('[name="workDirectory"]');
        
        if (usernameInput) {
            usernameInput.addEventListener('input', (e) => {
                const username = e.target.value.trim();
                this.validateFieldReal(e.target, 'username');
                
                // 自动生成工作目录
                if (workDirInput && username && (!workDirInput.value || workDirInput.value === `/home/${this.lastGeneratedUsername}`)) {
                    workDirInput.value = `/home/${username}`;
                    this.lastGeneratedUsername = username;
                }
            });
            
            usernameInput.addEventListener('blur', (e) => {
                this.checkUsernameAvailability(e.target.value);
            });
        }
        
        // 邮箱实时验证
        const emailInput = form.querySelector('[name="email"]');
        if (emailInput) {
            emailInput.addEventListener('input', (e) => {
                this.validateFieldReal(e.target, 'email');
            });
        }
        
        // 密码强度实时检测和验证
        const passwordInput = form.querySelector('[name="password"]');
        const passwordConfirmInput = form.querySelector('[name="passwordConfirm"]');
        
        if (passwordInput) {
            passwordInput.addEventListener('input', (e) => {
                this.updatePasswordStrength(e.target.value);
                this.validateFieldReal(e.target, 'password');
                
                // 同时验证确认密码
                if (passwordConfirmInput && passwordConfirmInput.value) {
                    this.validatePasswordMatch(e.target.value, passwordConfirmInput.value);
                }
            });
        }
        
        // 确认密码实时验证
        if (passwordConfirmInput) {
            passwordConfirmInput.addEventListener('input', (e) => {
                const password = passwordInput ? passwordInput.value : '';
                this.validatePasswordMatch(password, e.target.value);
            });
        }
        
        // 角色卡片点击选择
        const roleCards = form.querySelectorAll('.role-selection .role-card');
        roleCards.forEach(card => {
            card.addEventListener('click', (e) => {
                if (e.target.type !== 'checkbox') {
                    const checkbox = card.querySelector('input[type="checkbox"]');
                    if (checkbox) {
                        checkbox.checked = !checkbox.checked;
                        this.updateRoleCardStyle(card, checkbox.checked);
                        this.validateRoleSelection();
                    }
                }
            });
            
            // 监听复选框变化
            const checkbox = card.querySelector('input[type="checkbox"]');
            if (checkbox) {
                checkbox.addEventListener('change', (e) => {
                    this.updateRoleCardStyle(card, e.target.checked);
                    this.validateRoleSelection();
                });
            }
        });
        
    }

    // 更新角色卡片样式
    updateRoleCardStyle(card, isSelected) {
        if (isSelected) {
            card.classList.add('selected');
        } else {
            card.classList.remove('selected');
        }
    }
    
    // 实时字段验证
    validateFieldReal(field, type) {
        const value = field.value.trim();
        let isValid = true;
        let message = '';
        
        switch (type) {
            case 'username':
                if (!value) {
                    message = '用户名不能为空';
                    isValid = false;
                } else if (value.length < 3) {
                    message = '用户名至少需要3个字符';
                    isValid = false;
                } else if (!/^[a-zA-Z0-9_-]+$/.test(value)) {
                    message = '只能包含字母、数字、下划线和连字符';
                    isValid = false;
                }
                break;
                
            case 'email':
                if (!value) {
                    message = '邮箱地址不能为空';
                    isValid = false;
                } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value)) {
                    message = '请输入有效的邮箱地址';
                    isValid = false;
                }
                break;
                
            case 'password':
                if (value && value.length < 6) {
                    message = '密码至少需要6个字符';
                    isValid = false;
                }
                break;
        }
        
        this.showFieldValidation(field, isValid, message);
        return isValid;
    }
    
    // 验证密码匹配
    validatePasswordMatch(password, confirmPassword) {
        const confirmField = this.userDrawer.querySelector('[name="passwordConfirm"]');
        if (!confirmField) return true;
        
        const isValid = password === confirmPassword;
        const message = isValid ? '' : '两次输入的密码不一致';
        
        this.showFieldValidation(confirmField, isValid, message);
        return isValid;
    }
    
    // 验证角色选择
    validateRoleSelection() {
        const form = this.userDrawer.querySelector('form');
        if (!form) return true;
        
        const checkedRoles = form.querySelectorAll('[name="roles"]:checked');
        const isValid = checkedRoles.length > 0;
        const roleGroup = form.querySelector('.role-selection');
        
        if (roleGroup) {
            const feedback = roleGroup.parentNode.querySelector('.invalid-feedback');
            if (feedback) {
                feedback.textContent = isValid ? '' : '请至少选择一个角色';
                feedback.style.display = isValid ? 'none' : 'block';
            }
        }
        
        return isValid;
    }
    
    // 显示字段验证结果
    showFieldValidation(field, isValid, message) {
        const formGroup = field.closest('.form-group');
        if (!formGroup) return;
        
        const feedback = formGroup.querySelector('.invalid-feedback');
        if (feedback) {
            feedback.textContent = message;
            feedback.style.display = isValid ? 'none' : 'block';
        }
        
        if (isValid) {
            field.classList.remove('is-invalid');
            field.classList.add('is-valid');
        } else {
            field.classList.remove('is-valid');
            field.classList.add('is-invalid');
        }
    }
    
    // 检查用户名可用性（异步）
    async checkUsernameAvailability(username) {
        if (!username || username.length < 3) return;
        
        const field = this.userDrawer.querySelector('[name="username"]');
        const isEditMode = this.userDrawer.querySelector('[name="id"]').value;
        
        if (isEditMode) return; // 编辑模式不检查用户名可用性
        
        try {
            const response = await fetch(`/admin/api/users/check-username?username=${encodeURIComponent(username)}`);
            if (response.ok) {
                const result = await response.json();
                if (!result.available) {
                    this.showFieldValidation(field, false, '用户名已存在');
                }
            }
        } catch (error) {
            console.warn('检查用户名可用性失败:', error);
        }
    }

    // 监听表单变化
    monitorFormChanges(drawer) {
        const form = drawer.querySelector('form');
        if (!form) return;

        const inputs = form.querySelectorAll('input, textarea, select');
        inputs.forEach(input => {
            input.addEventListener('input', () => {
                this.hasUnsavedChanges = true;
            });
            input.addEventListener('change', () => {
                this.hasUnsavedChanges = true;
            });
        });
    }

    // 显示/隐藏加载状态
    showLoading(show) {
        if (this.currentDrawer) {
            const loading = this.currentDrawer.querySelector('.drawer-loading');
            const submitBtn = this.currentDrawer.querySelector('.drawer-btn-primary');
            
            if (show) {
                loading.classList.add('show');
                if (submitBtn) {
                    submitBtn.disabled = true;
                }
            } else {
                loading.classList.remove('show');
                if (submitBtn) {
                    submitBtn.disabled = false;
                }
            }
        }
    }
}

// 全局函数定义
let drawerManager;

// 初始化抽屉管理器
document.addEventListener('DOMContentLoaded', function() {
    drawerManager = new DrawerManager();
});

// 全局函数供HTML调用
function addServer() {
    if (drawerManager) {
        drawerManager.openServerDrawer();
    }
}

function editServer(element) {
    if (drawerManager && element) {
        const serverId = element.getAttribute('data-id') || 
                        element.closest('[data-id]')?.getAttribute('data-id');
        if (serverId) {
            drawerManager.openServerDrawer(serverId);
        }
    }
}

function addUser() {
    if (drawerManager) {
        drawerManager.openUserDrawer();
    }
}

function editUser(userId) {
    if (drawerManager) {
        drawerManager.openUserDrawer(userId);
    }
}

// 刷新服务器列表函数（可被页面重写）
function refreshServerList() {
    // 默认刷新页面，具体页面可以重写此函数
    window.location.reload();
}

// 工具函数
function debounce(func, wait) {
    let timeout;
    return function executedFunction(...args) {
        const later = () => {
            clearTimeout(timeout);
            func(...args);
        };
        clearTimeout(timeout);
        timeout = setTimeout(later, wait);
    };
}