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
        drawer.className = 'drawer server-drawer';
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
                    <input type="hidden" name="_token" value="">
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
                                      rows="3" 
                                      placeholder="服务器的详细描述信息..."></textarea>
                        </div>
                    </div>

                    <!-- 应用服务配置 -->
                    <div class="form-section">
                        <h3 class="form-section-title">
                            <div class="form-section-icon basic">🌐</div>
                            应用服务配置
                        </h3>
                        
                        <div class="form-row">
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
                                <div class="form-text">用户访问此服务器的应用地址</div>
                                <div class="invalid-feedback"></div>
                            </div>
                            
                            <div class="form-group">
                                <label for="server-port" class="form-label">
                                    应用端口号 <span class="required">*</span>
                                </label>
                                <div class="input-group">
                                    <span class="input-group-icon">🔌</span>
                                    <input type="number" 
                                           class="form-control" 
                                           id="server-port" 
                                           name="port" 
                                           placeholder="8080" 
                                           min="1" 
                                           max="65535" 
                                           required>
                                </div>
                                <div class="form-text">应用服务运行的端口号</div>
                                <div class="invalid-feedback"></div>
                            </div>
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
                        
                        <div class="form-row">
                            <div class="form-group">
                                <label for="server-ssh-port" class="form-label">SSH端口</label>
                                <div class="input-group">
                                    <span class="input-group-icon">🚪</span>
                                    <input type="number" 
                                           class="form-control" 
                                           id="server-ssh-port" 
                                           name="sshPort" 
                                           placeholder="22" 
                                           min="1" 
                                           max="65535">
                                </div>
                            </div>
                            
                            <div class="form-group">
                                <label for="server-ssh-username" class="form-label">SSH用户名</label>
                                <div class="input-group">
                                    <span class="input-group-icon">👤</span>
                                    <input type="text" 
                                           class="form-control" 
                                           id="server-ssh-username" 
                                           name="sshUsername" 
                                           placeholder="root">
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

    // 创建用户抽屉HTML (占位符)
    createUserDrawer() {
        const drawer = document.createElement('div');
        drawer.className = 'drawer user-drawer';
        drawer.innerHTML = `
            <div class="drawer-header">
                <h2 class="drawer-title">
                    <div class="drawer-title-icon">👥</div>
                    <span class="drawer-title-text">用户管理</span>
                </h2>
                <button type="button" class="drawer-close" onclick="drawerManager.closeDrawer()">
                    ✕
                </button>
            </div>
            <div class="drawer-content">
                <div class="drawer-form">
                    <p>用户抽屉功能即将推出...</p>
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
    }

    // 获取CSRF令牌
    getCSRFToken() {
        const meta = document.querySelector('meta[name="_csrf"]');
        return meta ? meta.getAttribute('content') : '';
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

        // 验证端口
        const port = parseInt(form.querySelector('[name="port"]').value);
        if (!port || port < 1 || port > 65535) {
            errors.port = '端口号必须在1-65535之间';
            isValid = false;
        }

        // 验证工作目录
        const workDir = form.querySelector('[name="baseWorkDirectory"]').value.trim();
        if (!workDir) {
            errors.baseWorkDirectory = '工作目录不能为空';
            isValid = false;
        }

        // 验证SSH密码
        const sshPassword = form.querySelector('[name="sshPassword"]').value;
        if (!sshPassword) {
            errors.sshPassword = 'SSH密码不能为空';
            isValid = false;
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
        const tokenInput = this.serverDrawer.querySelector('[name="_token"]');
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
        
        // 设置默认值
        form.querySelector('[name="sshPort"]').value = '22';
        form.querySelector('[name="sshUsername"]').value = 'root';
        form.querySelector('[name="active"]').checked = true;
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
            formData.append('_token', this.getCSRFToken());
            
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