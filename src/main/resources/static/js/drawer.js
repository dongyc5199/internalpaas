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
        try {
            this.bindEvents();
            const success = this.createDrawerContainers();
            if (!success) {
                console.error('抽屉容器创建失败');
                return false;
            }
            return true;
        } catch (error) {
            console.error('DrawerManager初始化失败:', error);
            return false;
        }
    }

    // 创建抽屉容器
    createDrawerContainers() {
        // 检查body是否存在
        if (!document.body) {
            console.error('document.body不存在，无法初始化抽屉组件');
            return false;
        }
        
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
        
        return true; // 创建成功
    }

    // 创建服务器抽屉HTML
    createServerDrawer() {
        const drawer = document.createElement('div');
        drawer.className = 'drawer drawer-server drawer-compact drawer-modern';
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
                                <button type="button" class="password-toggle-btn" onclick="togglePasswordVisibility('server-ssh-password')" title="显示/隐藏密码">
                                    <i class="fas fa-eye"></i>
                                </button>
                            </div>
                            <div class="form-text">仅支持密码认证，请填写SSH密码</div>
                            <div class="invalid-feedback"></div>
                        </div>
                    </div>

                    <!-- 用户组管理 -->
                    <div class="form-section">
                        <h3 class="form-section-title">
                            <div class="form-section-icon users">👥</div>
                            用户组管理
                        </h3>
                        
                        <!-- 新建服务器时显示的初始化选项 -->
                        <div class="usergroup-init-section" id="usergroupInitSection">
                            <div class="form-check">
                                <input class="form-check-input" 
                                       type="checkbox" 
                                       id="server-auto-init-usergroups" 
                                       name="autoInitUserGroups" 
                                       checked>
                                <label class="form-check-label" for="server-auto-init-usergroups">
                                    创建后自动初始化默认用户组
                                </label>
                            </div>
                            <div class="form-text">
                                系统将根据服务器类型和权限级别自动创建相应的默认用户组（管理员、开发者、访客等）
                            </div>
                        </div>
                        
                        <!-- 编辑服务器时显示的用户组列表 -->
                        <div class="usergroup-list-section" id="usergroupListSection" style="display: none;">
                            <div class="usergroup-header">
                                <span class="usergroup-count">正在加载用户组...</span>
                                <div class="usergroup-actions">
                                    <button type="button" class="btn-refresh-usergroups" onclick="drawerManager.refreshServerUserGroups()" title="刷新用户组列表">
                                        <i class="fas fa-sync-alt"></i>
                                    </button>
                                    <button type="button" class="btn-init-usergroups" onclick="drawerManager.initializeServerUserGroups()" title="初始化默认用户组">
                                        <i class="fas fa-plus"></i> 初始化
                                    </button>
                                </div>
                            </div>
                            
                            <div class="usergroup-container" id="usergroupContainer">
                                <div class="usergroup-loading">
                                    <i class="fas fa-spinner fa-spin"></i> 加载中...
                                </div>
                            </div>
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
                    
                    <!-- 服务器状态 -->
                    <div class="form-section" id="serverStatusSection" style="display: none;">
                        <h3 class="form-section-title">
                            <div class="form-section-icon status">📊</div>
                            服务器状态
                        </h3>
                        
                        <div class="server-status-display">
                            <div class="status-loading" id="serverStatusLoading">
                                <i class="fas fa-spinner fa-spin"></i> 正在加载服务器状态...
                            </div>
                            
                            <div class="status-content" id="serverStatusContent" style="display: none;">
                                <!-- 状态标签容器 -->
                                <div class="server-status-tags" id="drawerServerStatusTags">
                                    <!-- 状态标签将在这里动态加载 -->
                                </div>
                            </div>
                            
                            <div class="status-error" id="serverStatusError" style="display: none;">
                                <i class="fas fa-exclamation-triangle"></i>
                                <span>无法获取服务器状态信息</span>
                                <button type="button" class="btn-retry-status" onclick="drawerManager.loadServerStatus()">重试</button>
                            </div>
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
                <div class="loading-content">
                    <div class="loading-spinner"></div>
                    <span class="loading-text">保存中...</span>
                </div>
            </div>
        `;
        return drawer;
    }

    // 创建用户抽屉HTML
    createUserDrawer() {
        const drawer = document.createElement('div');
        drawer.className = 'drawer drawer-user drawer-compact drawer-modern';
        drawer.innerHTML = `
            <style>
                /* 帮助信息提示样式 */
                .drawer-header-with-help {
                    display: flex;
                    justify-content: space-between;
                    align-items: center;
                    margin-bottom: 20px;
                    padding-bottom: 15px;
                    border-bottom: 1px solid #e1e5e9;
                }
                
                .help-tooltip {
                    position: relative;
                    display: inline-block;
                }
                
                .help-icon {
                    background: none;
                    border: none;
                    font-size: 20px;
                    color: #6b7280;
                    cursor: pointer;
                    padding: 5px;
                    border-radius: 50%;
                    transition: all 0.2s ease;
                }
                
                .help-icon:hover {
                    background-color: #f3f4f6;
                    color: #4b5563;
                }
                
                .help-tooltip-content {
                    position: absolute;
                    left: 0;
                    right: auto;
                    top: 100%;
                    margin-top: 5px;
                    width: 250px;
                    background-color: white;
                    border: 1px solid #e5e7eb;
                    border-radius: 8px;
                    padding: 15px;
                    box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06);
                    z-index: 9999; /* 增加z-index值以确保显示在最上层 */
                    opacity: 0;
                    visibility: hidden;
                    transition: opacity 0.2s ease, visibility 0.2s ease;
                }
                
                .help-tooltip-content.show {
                    opacity: 1;
                    visibility: visible;
                }
                
                .help-tooltip-content .help-item {
                    margin-bottom: 10px;
                }
                
                .help-tooltip-content .help-item:last-child {
                    margin-bottom: 0;
                }
                
                .help-tooltip-content h6 {
                    margin: 0 0 5px 0;
                    font-size: 14px;
                    font-weight: 600;
                    color: #374151;
                }
                
                .help-tooltip-content p {
                    margin: 0;
                    font-size: 13px;
                    color: #6b7280;
                    line-height: 1.4;
                }

                /* 服务器选择现在使用role-card样式，移除旧的server-item样式 */

                @media (max-width: 600px) {
                    .server-item {
                        flex-direction: row;
                        gap: 8px;
                    }

                    .server-info {
                        margin-left: 8px;
                    }
                }
                
                /* 两栏布局样式 */
                .drawer-dual-pane {
                    display: flex;
                    gap: 24px;
                    height: 100%; /* 使用100%高度，不减去固定值 */
                }
                
                .drawer-left-pane,
                .drawer-right-pane {
                    flex: 1;
                    overflow-y: auto;
                    padding-right: 10px;
                }
                
                .drawer-left-pane::-webkit-scrollbar,
                .drawer-right-pane::-webkit-scrollbar {
                    width: 6px;
                }
                
                .drawer-left-pane::-webkit-scrollbar-track,
                .drawer-right-pane::-webkit-scrollbar-track {
                    background: #f1f1f1;
                    border-radius: 3px;
                }
                
                .drawer-left-pane::-webkit-scrollbar-thumb,
                .drawer-right-pane::-webkit-scrollbar-thumb {
                    background: #c1c1c1;
                    border-radius: 3px;
                }
                
                .drawer-left-pane::-webkit-scrollbar-thumb:hover,
                .drawer-right-pane::-webkit-scrollbar-thumb:hover {
                    background: #a1a1a1;
                }
                
                /* 动画效果 */
                @keyframes spin {
                    from { transform: rotate(0deg); }
                    to { transform: rotate(360deg); }
                }
                
                .animate-spin {
                    animation: spin 1s linear infinite;
                }
                
                /* 移动端布局适配 */
                @media (max-width: 768px) {
                    .drawer-dual-pane {
                        flex-direction: column;
                        height: auto;
                    }
                    
                    .drawer-left-pane,
                    .drawer-right-pane {
                        width: 100%;
                        padding-right: 0;
                    }
                    
                    .help-tooltip-content {
                        right: auto;
                        left: 0;
                        width: 280px;
                    }
                }
            </style>
            
            <div class="drawer-header">
                <h2 class="drawer-title">
                    <div class="drawer-title-icon">👥</div>
                    <span class="drawer-title-text">添加用户</span>
                    <div class="help-tooltip" style="margin-left: 8px;">
                        <button type="button" class="help-icon" id="drawerHelpBtn">
                            <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                                <circle cx="12" cy="12" r="10"></circle>
                                <line x1="12" y1="16" x2="12" y2="12"></line>
                                <line x1="12" y1="8" x2="12.01" y2="8"></line>
                            </svg>
                        </button>
                        <div class="help-tooltip-content" id="drawerHelpContent">
                            <div class="help-item">
                                <h6>用户名规则</h6>
                                <p>3-20个字符，支持字母、数字、下划线</p>
                            </div>
                            <div class="help-item">
                                <h6>密码要求</h6>
                                <p>至少8位，包含字母和数字</p>
                            </div>
                            <div class="help-item">
                                <h6>服务器分配</h6>
                                <p>至少选择一个可用服务器</p>
                            </div>
                        </div>
                    </div>
                </h2>
                <button type="button" class="drawer-close" onclick="drawerManager.closeDrawer()">
                    ✕
                </button>
            </div>
            <div class="drawer-content">
                
                <!-- 双列布局容器 -->
                <div class="drawer-dual-pane">
                    <!-- 第一栏：基本信息、密码设置、权限设置 -->
                    <div class="drawer-left-pane">
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
                                
                                <div class="form-group">
                                    <label class="form-label" for="userUsername">
                                        用户名 <span class="required">*</span>
                                    </label>
                                    <input type="text" id="userUsername" name="username" class="form-control" 
                                           placeholder="请输入用户名" required>
                                    <div class="invalid-feedback"></div>
                                </div>
                                
                                <div class="form-group">
                                    <label class="form-label" for="userPassword">
                                        登录密码 <span class="required" id="passwordRequired">*</span>
                                    </label>
                                    <div class="password-input-group">
                                        <input type="password" id="userPassword" name="password" class="form-control" 
                                               placeholder="请输入密码">
                                        <button type="button" class="password-toggle" onclick="drawerManager.togglePasswordVisibility('userPassword')" title="显示密码">
                                            <i class="fas fa-eye"></i>
                                        </button>
                                    </div>
                                    <div class="form-text edit-mode-note" style="display: none;">
                                        留空保持原密码不变
                                    </div>
                                    <div class="invalid-feedback"></div>
                                </div>
                                
                                <div class="form-group">
                                    <label class="form-label" for="userPasswordConfirm">
                                        确认密码 <span class="required" id="confirmRequired">*</span>
                                    </label>
                                    <div class="password-input-group">
                                        <input type="password" id="userPasswordConfirm" name="passwordConfirm" class="form-control" 
                                               placeholder="请再次输入密码">
                                        <button type="button" class="password-toggle" onclick="drawerManager.togglePasswordVisibility('userPasswordConfirm')" title="显示密码">
                                            <i class="fas fa-eye"></i>
                                        </button>
                                    </div>
                                    <div class="invalid-feedback"></div>
                                </div>
                                
                                <div class="password-strength">
                                    <div class="password-strength-bar">
                                        <div class="password-strength-fill"></div>
                                    </div>
                                    <span class="password-strength-text">密码强度</span>
                                </div>
                                
                                <div class="form-group">
                                    <label class="form-label" for="userEmail">
                                        邮箱地址 <span class="required">*</span>
                                    </label>
                                    <input type="email" id="userEmail" name="email" class="form-control" 
                                           placeholder="请输入邮箱地址" required>
                                    <div class="invalid-feedback"></div>
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
                                        <span class="form-label-text">用户角色</span>
                                        <span class="form-label-required">*</span>
                                    </label>
                                    <div class="role-selection compact">
                                        <div class="role-card compact" data-role="USER">
                                            <div class="role-card-header">
                                                <input type="checkbox" name="roles" value="USER" id="roleUser" style="display: none;">
                                                <div class="role-icon">👨‍💻</div>
                                            </div>
                                            <div class="role-card-body">
                                                <h6 class="role-card-title">开发者</h6>
                                                <p class="role-card-description">可访问分配的服务器，执行开发测试任务</p>
                                            </div>
                                        </div>
                                        
                                        <div class="role-card compact" data-role="ADMIN">
                                            <div class="role-card-header">
                                                <input type="checkbox" name="roles" value="ADMIN" id="roleAdmin" style="display: none;">
                                                <div class="role-icon">🛡️</div>
                                            </div>
                                            <div class="role-card-body">
                                                <h6 class="role-card-title">管理员</h6>
                                                <p class="role-card-description">管理用户和服务器，查看系统状态</p>
                                            </div>
                                        </div>
                                        
                                        <div class="role-card compact" data-role="SUPER_ADMIN">
                                            <div class="role-card-header">
                                                <input type="checkbox" name="roles" value="SUPER_ADMIN" id="roleSuperAdmin" style="display: none;">
                                                <div class="role-icon">👑</div>
                                            </div>
                                            <div class="role-card-body">
                                                <h6 class="role-card-title">超级管理员</h6>
                                                <p class="role-card-description">拥有系统全部权限，管理所有资源</p>
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
                                
                                <div class="form-group compact">
                                    <label class="form-label">
                                        <span class="form-label-text">账户状态</span>
                                    </label>
                                    <div class="role-selection compact">
                                        <div class="role-card compact account-status-card selected" data-status="enabled">
                                            <div class="role-card-header">
                                                <input type="checkbox" 
                                                       class="form-check-input" 
                                                       id="userEnabled" 
                                                       name="enabled" 
                                                       checked
                                                       style="display: none;">
                                                <div class="role-icon">✅</div>
                                            </div>
                                            <div class="role-card-body">
                                                <h6 class="role-card-title">启用此账户</h6>
                                                <p class="role-card-description">用户可以正常登录和使用系统</p>
                                            </div>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </form>
                    </div>
                    
                    <!-- 简洁分隔线 -->
                    <div class="drawer-separator">
                        <div class="separator-line"></div>
                    </div>
                    
                    <!-- 第二栏：服务器资源分配 -->
                    <div class="drawer-right-pane">
                        <form id="userDrawerFormRight" class="drawer-form">
                            <!-- 服务器资源分配 -->
                            <div class="form-section">
                                <h3 class="form-section-title">
                                    <div class="form-section-icon">🖥️</div>
                                    服务器资源分配
                                    <button type="button" class="refresh-servers-btn" onclick="drawerManager.refreshServersStatus()" title="刷新服务器状态">
                                        <i class="fas fa-sync-alt"></i>
                                    </button>
                                </h3>
                                
                                <div class="form-group">
                                    <label class="form-label">
                                        <span class="form-label-text">可用服务器</span>
                                    </label>
                                    <div class="server-selection">
                                        <div id="serverSelectionList" class="role-selection">
                                            <!-- 服务器列表将通过JavaScript动态加载 -->
                                            <div class="server-loading">加载可用服务器...</div>
                                        </div>
                                    </div>
                                    <div class="form-help" style="margin-top: 8px;">
                                        <span class="form-help-icon">⚠️</span>
                                        <span class="form-help-text">离线服务器无法选择，请刷新重试</span>
                                    </div>
                                </div>
                            </div>
                        </form>
                    </div>
                </div>
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
        
        // 帮助信息提示按钮交互
        document.addEventListener('click', (e) => {
            const helpBtn = document.getElementById('drawerHelpBtn');
            const helpContent = document.getElementById('drawerHelpContent');
            
            if (helpBtn && e.target === helpBtn || helpBtn.contains(e.target)) {
                helpContent.classList.toggle('show');
            } else if (helpContent && !helpContent.contains(e.target)) {
                helpContent.classList.remove('show');
            }
        });

        // 防止页面关闭时丢失未保存数据
        window.addEventListener('beforeunload', (e) => {
            if (this.hasUnsavedChanges) {
                e.preventDefault();
                e.returnValue = '您有未保存的更改，确定要离开吗？';
            }
        });

        // 监听窗口大小变化，调整抽屉布局
        window.addEventListener('resize', () => {
            if (this.currentDrawer) {
                this.adjustLayoutForScreenSize();
            }
        });

        // 延迟绑定用户表单事件（等DOM创建完成）
        setTimeout(() => {
            this.bindUserFormEvents();
        }, 100);
    }
    
    // 根据屏幕大小调整抽屉布局
    adjustLayoutForScreenSize() {
        const dualPane = this.currentDrawer.querySelector('.drawer-dual-pane');
        if (!dualPane) return;
        
        const isMobile = window.innerWidth <= 768;
        const isMedium = window.innerWidth <= 1200;
        
        if (isMobile) {
            // 在移动设备上强制使用单列布局
            dualPane.classList.add('mobile-layout');
        } else {
            dualPane.classList.remove('mobile-layout');
        }
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
            
            // 编辑模式：显示用户组列表，隐藏初始化选项
            this.toggleUserGroupSections(true, serverId);
            
            // 编辑模式：显示服务器状态区域
            this.toggleServerStatusSection(true, serverId);
        } else {
            titleText.textContent = '添加服务器';
            submitBtn.textContent = '保存服务器';
            this.resetServerForm();
            
            // 新建模式：显示初始化选项，隐藏用户组列表
            this.toggleUserGroupSections(false);
            
            // 新建模式：隐藏服务器状态区域
            this.toggleServerStatusSection(false);
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
        
        // 设置用户组初始化选项默认值
        const autoInitField = form.querySelector('[name="autoInitUserGroups"]');
        if (autoInitField) autoInitField.checked = true;
    }

    // 切换用户组管理部分的显示
    toggleUserGroupSections(isEditMode, serverId = null) {
        const initSection = this.serverDrawer.querySelector('#usergroupInitSection');
        const listSection = this.serverDrawer.querySelector('#usergroupListSection');
        
        if (isEditMode) {
            // 编辑模式：显示用户组列表，隐藏初始化选项
            initSection.style.display = 'none';
            listSection.style.display = 'block';
            
            // 加载用户组数据
            if (serverId) {
                this.loadServerUserGroups(serverId);
            }
        } else {
            // 新建模式：显示初始化选项，隐藏用户组列表
            initSection.style.display = 'block';
            listSection.style.display = 'none';
        }
    }

    // 加载服务器用户组
    async loadServerUserGroups(serverId) {
        const container = this.serverDrawer.querySelector('#usergroupContainer');
        const countSpan = this.serverDrawer.querySelector('.usergroup-count');
        
        container.innerHTML = '<div class="usergroup-loading"><i class="fas fa-spinner fa-spin"></i> 加载中...</div>';
        countSpan.textContent = '正在加载用户组...';
        
        try {
            const response = await fetch(`/api/server-user-groups/server/${serverId}`);
            if (!response.ok) {
                throw new Error('获取用户组失败');
            }
            
            const data = await response.json();
            if (data.success) {
                this.displayServerUserGroups(data.userGroups || []);
                countSpan.textContent = `共 ${data.userGroups?.length || 0} 个用户组`;
            } else {
                throw new Error(data.error || '获取用户组失败');
            }
        } catch (error) {
            console.error('加载用户组失败:', error);
            container.innerHTML = `<div class="usergroup-empty"><i class="fas fa-exclamation-triangle"></i>加载失败: ${error.message}</div>`;
            countSpan.textContent = '加载失败';
        }
    }

    // 显示服务器用户组
    displayServerUserGroups(userGroups) {
        const container = this.serverDrawer.querySelector('#usergroupContainer');
        
        if (userGroups.length === 0) {
            container.innerHTML = `
                <div class="usergroup-empty">
                    <i class="fas fa-users-slash"></i>
                    <p>该服务器暂无用户组</p>
                    <small>点击"初始化"创建默认用户组</small>
                </div>
            `;
            return;
        }
        
        let html = '<div class="usergroup-list">';
        userGroups.forEach(group => {
            html += `
                <div class="usergroup-item">
                    <div class="usergroup-info">
                        <div class="usergroup-name ${group.isDefault ? 'default' : ''}">${group.groupName}</div>
                        <div class="usergroup-meta">${group.groupDescription || '无描述'} • ${group.permissionLevel}</div>
                    </div>
                    <div class="usergroup-actions-mini">
                        <button class="btn-edit-usergroup" onclick="drawerManager.editUserGroup(${group.id})" title="编辑">
                            <i class="fas fa-edit"></i>
                        </button>
                        ${!group.isDefault ? `<button class="btn-delete-usergroup" onclick="drawerManager.deleteUserGroup(${group.id})" title="删除">
                            <i class="fas fa-trash"></i>
                        </button>` : ''}
                    </div>
                </div>
            `;
        });
        html += '</div>';
        
        container.innerHTML = html;
    }

    // 刷新服务器用户组
    refreshServerUserGroups() {
        const form = this.serverDrawer.querySelector('#serverDrawerForm');
        const serverId = form.querySelector('[name="id"]').value;
        
        if (serverId) {
            this.loadServerUserGroups(serverId);
        }
    }

    // 初始化服务器用户组
    async initializeServerUserGroups() {
        const form = this.serverDrawer.querySelector('#serverDrawerForm');
        const serverId = form.querySelector('[name="id"]').value;
        
        if (!serverId) {
            alert('请先保存服务器后再初始化用户组');
            return;
        }
        
        const btn = event.target;
        const originalText = btn.innerHTML;
        btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> 初始化中...';
        btn.disabled = true;
        
        try {
            const response = await fetch(`/api/server-user-groups/server/${serverId}/initialize-defaults`, {
                method: 'POST',
                headers: {
                    'X-CSRF-TOKEN': this.getCSRFToken()
                }
            });
            
            const data = await response.json();
            if (data.success) {
                alert(`成功初始化 ${data.initializedCount} 个默认用户组`);
                this.loadServerUserGroups(serverId); // 重新加载用户组列表
            } else {
                alert('初始化失败: ' + data.error);
            }
        } catch (error) {
            console.error('初始化用户组失败:', error);
            alert('初始化失败: 网络错误或服务器无响应');
        } finally {
            btn.innerHTML = originalText;
            btn.disabled = false;
        }
    }

    // 编辑用户组
    editUserGroup(groupId) {
        // TODO: 实现编辑用户组功能
        alert('编辑用户组功能待实现: ' + groupId);
    }

    // 删除用户组
    async deleteUserGroup(groupId) {
        if (!confirm('确定要删除这个用户组吗？此操作不可逆。')) {
            return;
        }
        
        try {
            const response = await fetch(`/api/server-user-groups/${groupId}`, {
                method: 'DELETE',
                headers: {
                    'X-CSRF-TOKEN': this.getCSRFToken()
                }
            });
            
            const data = await response.json();
            if (data.success) {
                alert('用户组删除成功');
                // 重新加载用户组列表
                const form = this.serverDrawer.querySelector('#serverDrawerForm');
                const serverId = form.querySelector('[name="id"]').value;
                if (serverId) {
                    this.loadServerUserGroups(serverId);
                }
            } else {
                alert('删除失败: ' + data.error);
            }
        } catch (error) {
            console.error('删除用户组失败:', error);
            alert('删除失败: 网络错误或服务器无响应');
        }
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
            
            // 显示详细成功消息，包含目录信息
            let successMessage = isEdit ? '服务器更新成功！' : '服务器创建成功！';
            if (result.message) {
                successMessage = result.message;
            }
            
            // 如果有目录信息，添加额外提示
            if (result.directoryInfo) {
                const dirInfo = result.directoryInfo;
                if (dirInfo.created) {
                    successMessage += ' 🗂️ 已创建基础工作目录';
                } else if (dirInfo.success && dirInfo.message) {
                    console.log('基础工作目录状态:', dirInfo.message);
                }
            }
            
            this.showMessage(messageContainer, 'success', successMessage);
            
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

    // 防止页面滚动但保持宽度不变
    preventBodyScroll() {
        // 记录当前滚动位置
        this.scrollPosition = window.pageYOffset;
        
        // 获取滚动条宽度
        const scrollbarWidth = window.innerWidth - document.documentElement.clientWidth;
        
        // 设置body样式，用padding-right补偿滚动条消失的宽度
        document.body.style.overflow = 'hidden';
        document.body.style.position = 'fixed';
        document.body.style.top = `-${this.scrollPosition}px`;
        document.body.style.width = '100%';
        document.body.style.paddingRight = `${scrollbarWidth}px`;
    }

    // 恢复页面滚动
    restoreBodyScroll() {
        // 恢复body样式
        document.body.style.overflow = '';
        document.body.style.position = '';
        document.body.style.top = '';
        document.body.style.width = '';
        document.body.style.paddingRight = '';
        
        // 恢复滚动位置
        if (typeof this.scrollPosition === 'number') {
            window.scrollTo(0, this.scrollPosition);
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
        
        // 防止背景滚动，但保持页面宽度不变
        this.preventBodyScroll();
        
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
        this.restoreBodyScroll();
        
        // 重置状态
        setTimeout(() => {
            this.currentDrawer = null;
            this.hasUnsavedChanges = false;
            this.isSubmitting = false;
        }, 300);
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
            // 加载可用服务器列表
            this.loadAvailableServers();
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


    // 加载可用服务器列表
    async loadAvailableServers(userId = null) {
        const serverListContainer = this.userDrawer?.querySelector('#serverSelectionList');
        if (!serverListContainer) return;
        
        try {
            // 显示加载状态
            serverListContainer.innerHTML = '<div class="server-loading">加载可用服务器...</div>';
            
            // 获取可用服务器列表
            const response = await fetch('/admin/api/servers/available');
            if (!response.ok) {
                throw new Error('获取可用服务器失败');
            }
            
            const servers = await response.json();
            
            // 清空容器
            serverListContainer.innerHTML = '';
            
            if (servers.length === 0) {
                serverListContainer.innerHTML = `
                    <div class="no-servers">
                        <div class="no-servers-message">
                            <i class="fas fa-server" style="color: #999; margin-bottom: 8px;"></i>
                            <p>暂无可用服务器</p>
                            <small class="text-muted">请联系管理员添加服务器配置</small>
                        </div>
                    </div>
                `;
                return;
            }
            
            // 获取用户已分配的服务器（如果是编辑模式）
            let assignedServerIds = [];
            if (userId) {
                try {
                    const userServersResponse = await fetch(`/admin/api/users/${userId}/servers`);
                    if (userServersResponse.ok) {
                        const userServers = await userServersResponse.json();
                        assignedServerIds = userServers.map(server => server.id.toString());
                    }
                } catch (error) {
                    console.warn('获取用户已分配服务器失败:', error);
                }
            }
            
            // 创建服务器选项（采用role-card样式）
            servers.forEach(server => {
                const serverItem = document.createElement('div');
                serverItem.className = 'role-card compact server-card';
                serverItem.setAttribute('data-server', server.id);
                
                // 判断服务器是否在线
                const isOnline = server.connectionStatus === 'CONNECTED' || server.connectionStatus === 'MONITORING';
                const isChecked = assignedServerIds.includes(server.id.toString());
                
                // 离线服务器不允许选中且添加禁用样式
                if (!isOnline) {
                    serverItem.classList.add('server-offline');
                } else if (isChecked) {
                    serverItem.classList.add('selected');
                }
                
                // 获取状态描述和图标
                const getStatusInfo = (status) => {
                    switch(status) {
                        case 'CONNECTED':
                        case 'MONITORING':
                            return { text: '在线', icon: '🟢', color: '#10b981' };
                        case 'FAILED':
                            return { text: '离线', icon: '🔴', color: '#ef4444' };
                        case 'TIMEOUT':
                            return { text: '离线', icon: '🟡', color: '#f59e0b' };
                        case 'AUTH_FAILED':
                            return { text: '认证失败', icon: '🔴', color: '#ef4444' };
                        default:
                            return { text: '未知状态', icon: '⚪', color: '#6b7280' };
                    }
                };
                
                const statusInfo = getStatusInfo(server.connectionStatus);
                
                serverItem.innerHTML = `
                    <div class="role-card-header">
                        <input type="checkbox" 
                               name="drawerServerIds" 
                               value="${server.id}" 
                               id="drawer-server-${server.id}"
                               ${isChecked && isOnline ? 'checked' : ''}
                               ${!isOnline ? 'disabled' : ''}>
                        <div class="role-icon">🖥️</div>
                    </div>
                    <div class="role-card-body">
                        <h6 class="role-card-title">${server.name}</h6>
                        <p class="role-card-description">
                            ${server.hostname} • 
                            <span class="server-status" style="color: ${statusInfo.color}">
                                ${statusInfo.icon} ${statusInfo.text}
                            </span>
                        </p>
                    </div>
                `;
                
                // 检查服务器名称长度并添加滚动动画类
                const titleElement = serverItem.querySelector('.role-card-title');
                if (titleElement && server.name.length > 12) {
                    titleElement.classList.add('long-text');
                }
                
                // 添加点击事件（离线服务器不响应点击）
                if (isOnline) {
                    serverItem.addEventListener('click', (e) => {
                        if (e.target.type !== 'checkbox') {
                            const checkbox = serverItem.querySelector('input[type="checkbox"]');
                            if (checkbox && !checkbox.disabled) {
                                checkbox.checked = !checkbox.checked;
                                this.updateServerCardStyle(serverItem, checkbox.checked);
                            }
                        }
                    });
                    
                    // 监听复选框变化
                    const checkbox = serverItem.querySelector('input[type="checkbox"]');
                    checkbox.addEventListener('change', (e) => {
                        this.updateServerCardStyle(serverItem, e.target.checked);
                    });
                } else {
                    // 离线服务器添加提示
                    serverItem.title = '服务器离线，无法选择';
                    serverItem.style.cursor = 'not-allowed';
                }
                
                serverListContainer.appendChild(serverItem);
            });
            
        } catch (error) {
            serverListContainer.innerHTML = `<div class="server-error">加载失败：${error.message}</div>`;
            console.error('加载服务器列表失败:', error);
        }
    }
    
    // 更新服务器卡片样式（与角色卡片保持一致）
    updateServerCardStyle(serverCard, isSelected) {
        if (isSelected) {
            serverCard.classList.add('selected');
        } else {
            serverCard.classList.remove('selected');
        }
    }
    
    // 更新账户状态卡片样式
    updateAccountStatusCardStyle(card, isEnabled) {
        const icon = card.querySelector('.role-icon');
        const title = card.querySelector('.role-card-title');
        const description = card.querySelector('.role-card-description');
        
        if (isEnabled) {
            card.classList.add('selected');
            if (icon) icon.textContent = '✅';
            if (title) title.textContent = '启用此账户';
            if (description) description.textContent = '用户可以正常登录和使用系统';
        } else {
            card.classList.remove('selected');
            if (icon) icon.textContent = '❌';
            if (title) title.textContent = '禁用此账户';
            if (description) description.textContent = '用户无法登录系统';
        }
    }
    
    // 保持向后兼容性
    updateServerItemVisual(checkbox) {
        const serverCard = checkbox.closest('.server-card, .server-item');
        if (!serverCard) return;
        
        this.updateServerCardStyle(serverCard, checkbox.checked);
    }
    
    // 加载用户数据
    async loadUserData(userId) {
        try {
            this.showLoading(true);
            
            console.log('加载用户数据，userId:', userId); // 添加调试日志
            const response = await fetch(`/admin/users/${userId}/data`); // 正确的路径，包含/admin前缀
            
            if (!response.ok) {
                // 获取详细的错误信息
                const errorBody = await response.json().catch(() => ({error: '未知错误'}));
                console.log('API错误响应:', response.status, errorBody);
                throw new Error(`获取用户数据失败: ${errorBody.error || '未知错误'}`);
            }
            
            const userData = await response.json();
            this.fillUserForm(userData);
            // 加载用户可分配的服务器列表
            this.loadAvailableServers(userId);
            
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
            const roleCard = checkbox.closest('.role-card');
            if (roleCard) {
                this.updateRoleCardStyle(roleCard, false);
            }
        });
        if (data.roles && Array.isArray(data.roles)) {
            data.roles.forEach(role => {
                const checkbox = form.querySelector(`[name="roles"][value="${role}"]`);
                if (checkbox) {
                    checkbox.checked = true;
                    const roleCard = checkbox.closest('.role-card');
                    if (roleCard) {
                        this.updateRoleCardStyle(roleCard, true);
                    }
                }
            });
        }
        
        // 设置账户状态
        const enabledCheckbox = form.querySelector('[name="enabled"]');
        if (enabledCheckbox) {
            enabledCheckbox.checked = data.enabled !== false;
            // 更新账户状态卡片视觉
            const accountCard = form.querySelector('.account-status-card');
            if (accountCard) {
                this.updateAccountStatusCardStyle(accountCard, data.enabled !== false);
            }
        }
        

        
        // 编辑模式下显示密码提示
        const editModeNotes = form.querySelectorAll('.edit-mode-note');
        editModeNotes.forEach(note => note.style.display = 'block');
        
        // 编辑模式下密码不是必填
        const passwordRequired = form.querySelector('#passwordRequired');
        const confirmRequired = form.querySelector('#confirmRequired');
        if (passwordRequired) passwordRequired.style.display = 'none';
        if (confirmRequired) confirmRequired.style.display = 'none';
        
        // 编辑模式下用户名设为只读
        const usernameField = form.querySelector('[name="username"]');
        if (usernameField) {
            usernameField.readOnly = true;
            usernameField.classList.add('read-only');
        }
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
        const enabledCheckbox = form.querySelector('[name="enabled"]');
        const userRoleCheckbox = form.querySelector('#roleUser'); // 使用ID选择器更可靠
        if (enabledCheckbox) {
            enabledCheckbox.checked = true;
            // 更新账户状态卡片视觉
            const accountCard = form.querySelector('.account-status-card');
            if (accountCard) {
                this.updateAccountStatusCardStyle(accountCard, true);
            }
        }
        if (userRoleCheckbox) {
            userRoleCheckbox.checked = true;
            // 更新角色卡片视觉
            const roleCard = userRoleCheckbox.closest('.role-card');
            if (roleCard) {
                this.updateRoleCardStyle(roleCard, true);
            }
        }
        
        // 显示必填标记
        const passwordRequired = form.querySelector('#passwordRequired');
        const confirmRequired = form.querySelector('#confirmRequired');
        if (passwordRequired) passwordRequired.style.display = 'inline';
        if (confirmRequired) confirmRequired.style.display = 'inline';
        
        // 新建模式下用户名不是只读
        const usernameField = form.querySelector('[name="username"]');
        if (usernameField) {
            usernameField.readOnly = false;
            usernameField.classList.remove('read-only');
        }
    }

    // 提交用户表单
    async submitUserForm() {
        console.log('submitUserForm called'); // 调试日志
        if (this.isSubmitting) {
            console.log('Already submitting, returning'); // 调试日志
            return;
        }

        const form = this.userDrawer.querySelector('#userDrawerForm');
        const rightForm = this.userDrawer.querySelector('#userDrawerFormRight');
        const messageContainer = this.userDrawer.querySelector('.drawer-messages');
        
        // 清除之前的消息
        messageContainer.innerHTML = '';
        
        // 验证表单
        if (!this.validateUserForm(form)) {
            this.showMessage(messageContainer, 'error', '请检查并修正表单中的错误');
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
            
            // 收集用户选择的服务器ID（从右侧表单）
            const serverIds = [];
            if (rightForm) {
                rightForm.querySelectorAll('[name="drawerServerIds"]:checked').forEach(checkbox => {
                    serverIds.push(parseInt(checkbox.value));
                });
            } else {
                // 备用查询，在整个抽屉中查找
                this.userDrawer.querySelectorAll('[name="drawerServerIds"]:checked').forEach(checkbox => {
                    serverIds.push(parseInt(checkbox.value));
                });
            }
            
            // 构建请求数据
            const userData = {
                username: formData.get('username'),
                email: formData.get('email'),
                password: formData.get('password'),
                roles: roles,
                serverIds: serverIds,
                enabled: formData.get('enabled') === 'on'
            };
            
            // 调试日志
            console.log('发送的用户数据:', userData);
            console.log('选中的角色:', roles);
            console.log('选中的服务器ID:', serverIds);
            
            
            const url = userId ? `/admin/api/users/${userId}/update` : '/admin/api/users';
            const method = 'POST';
            
            const response = await fetch(url, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    [this.getCSRFHeaderName()]: this.getCSRFToken()
                },
                body: JSON.stringify(userData)
            });

            const result = await response.json();

            if (response.ok && result.status === 'success') {
                // 显示详细成功消息，包含服务器数量信息
                let successMessage = result.message || '用户操作成功';
                if (result.serverCount && result.serverCount > 0) {
                    successMessage += ` 🖥️ 涉及 ${result.serverCount} 台服务器`;
                }
                
                this.showMessage(messageContainer, 'success', successMessage);
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
        const passwordInput = form.querySelector('[name="password"]');
        const passwordConfirmInput = form.querySelector('[name="passwordConfirm"]');
        const password = passwordInput ? passwordInput.value : '';
        const passwordConfirm = passwordConfirmInput ? passwordConfirmInput.value : '';
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
        
        // 验证服务器选择（只在有在线服务器时验证）
        const allServerCheckboxes = this.userDrawer.querySelectorAll('[name="drawerServerIds"]');
        const onlineServerCheckboxes = this.userDrawer.querySelectorAll('[name="drawerServerIds"]:not(:disabled)');
        const checkedServers = this.userDrawer.querySelectorAll('[name="drawerServerIds"]:checked');
        
        if (onlineServerCheckboxes.length > 0 && checkedServers.length === 0) {
            errors.servers = '请至少选择一个在线的服务器';
            isValid = false;
            // 显示服务器选择错误
            this.validateServerSelection();
        } else if (onlineServerCheckboxes.length === 0) {
            if (allServerCheckboxes.length > 0) {
                errors.servers = '当前没有在线的服务器可供选择，请联系管理员检查服务器状态';
                isValid = false;
                this.validateServerSelection();
            } else {
                console.warn('没有可用的服务器进行选择，跳过服务器验证');
            }
        }

        // 显示字段验证错误
        for (const [field, message] of Object.entries(errors)) {
            if (field !== 'servers') { // 服务器错误已经单独处理
                const fieldElement = form.querySelector(`[name="${field}"]`);
                if (fieldElement) {
                    this.showFieldValidation(fieldElement, false, message);
                }
            }
        }
        
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
        const button = input.parentElement.querySelector('.password-toggle');
        const icon = button.querySelector('i');
        
        if (input.type === 'password') {
            // 显示密码
            input.type = 'text';
            icon.className = 'fas fa-eye-slash';
            button.title = '隐藏密码';
        } else {
            // 隐藏密码
            input.type = 'password';
            icon.className = 'fas fa-eye';
            button.title = '显示密码';
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
            
            emailInput.addEventListener('blur', (e) => {
                this.checkEmailAvailability(e.target, e.target.value);
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
        
        // 角色卡片点击选择（排除账户状态卡片）
        const roleCards = form.querySelectorAll('.role-selection .role-card:not(.account-status-card)');
        roleCards.forEach(card => {
            card.addEventListener('click', (e) => {
                if (e.target.type !== 'checkbox') {
                    const checkbox = card.querySelector('input[type="checkbox"]');
                    if (checkbox) {
                        checkbox.checked = !checkbox.checked;
                        this.updateRoleCardStyle(card, checkbox.checked);
                        this.validateRoleSelection();
                        this.updateStatusInfo();
                    }
                }
            });
            
            // 监听复选框变化
            const checkbox = card.querySelector('input[type="checkbox"]');
            if (checkbox) {
                checkbox.addEventListener('change', (e) => {
                    this.updateRoleCardStyle(card, e.target.checked);
                    this.validateRoleSelection();
                    this.updateStatusInfo();
                });
            }
        });
        
        // 账户状态卡片点击选择
        const accountStatusCard = form.querySelector('.account-status-card');
        if (accountStatusCard) {
            accountStatusCard.addEventListener('click', (e) => {
                if (e.target.type !== 'checkbox') {
                    const checkbox = accountStatusCard.querySelector('input[type="checkbox"]');
                    if (checkbox) {
                        checkbox.checked = !checkbox.checked;
                        this.updateAccountStatusCardStyle(accountStatusCard, checkbox.checked);
                        this.updateStatusInfo();
                    }
                }
            });
            
            // 监听复选框变化
            const enabledCheckbox = accountStatusCard.querySelector('input[type="checkbox"]');
            if (enabledCheckbox) {
                enabledCheckbox.addEventListener('change', (e) => {
                    this.updateAccountStatusCardStyle(accountStatusCard, e.target.checked);
                    this.updateStatusInfo();
                });
            }
        }
        
        // 监听所有表单字段的变化以更新状态信息
        const allInputs = form.querySelectorAll('input, select, textarea');
        allInputs.forEach(input => {
            input.addEventListener('input', () => {
                this.updateStatusInfo();
                this.updatePreviewContent();
            });
            
            input.addEventListener('change', () => {
                this.updateStatusInfo();
                this.updatePreviewContent();
            });
        });
        
        // 监听服务器选择变化
        const serverContainer = form.querySelector('#serverSelectionList');
        if (serverContainer) {
            // 使用MutationObserver监听服务器选择的动态变化
            const observer = new MutationObserver(() => {
                const serverCheckboxes = this.userDrawer.querySelectorAll('.server-card input[type="checkbox"], .server-item input[type="checkbox"]');
                serverCheckboxes.forEach(checkbox => {
                    checkbox.addEventListener('change', () => {
                        this.updateStatusInfo();
                        this.updatePreviewContent();
                        this.validateServerSelection(); // 验证服务器选择
                    });
                });
            });
            
            observer.observe(serverContainer, {
                childList: true,
                subtree: true
            });
        }
        
        // 初始化状态信息
        this.updateStatusInfo();
        this.updatePreviewContent();
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
                } else {
                    // 异步检查邮箱唯一性
                    this.checkEmailAvailability(field, value);
                    return; // 提前返回，等待异步验证结果
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
    
    // 验证服务器选择
    validateServerSelection() {
        const form = this.userDrawer.querySelector('form');
        if (!form) return true;
        
        const checkedServers = this.userDrawer.querySelectorAll('[name="drawerServerIds"]:checked');
        const isValid = checkedServers.length > 0;
        const serverGroup = form.querySelector('#serverSelectionList');
        
        if (serverGroup) {
            let feedback = serverGroup.querySelector('.invalid-feedback');
            if (!feedback) {
                // 如果没有错误提示元素，则创建一个
                feedback = document.createElement('div');
                feedback.className = 'invalid-feedback';
                serverGroup.appendChild(feedback);
                
                // 更新服务器组样式以支持错误提示
                serverGroup.classList.add('has-feedback');
            }
            
            // 统一设置反馈信息和样式
            feedback.textContent = isValid ? '' : '请至少选择一个可用服务器';
            feedback.style.display = isValid ? 'none' : 'block';
            
            // 更新服务器组样式
            if (isValid) {
                serverGroup.classList.remove('has-error');
            } else {
                serverGroup.classList.add('has-error');
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
    
    // 检查邮箱可用性（异步）
    async checkEmailAvailability(field, email) {
        if (!email || !email.includes('@')) return;
        
        const isEditMode = this.userDrawer.querySelector('[name="id"]').value;
        if (isEditMode) return; // 编辑模式不检查邮箱可用性
        
        try {
            const response = await fetch(`/admin/api/users/check-email?email=${encodeURIComponent(email)}`);
            if (response.ok) {
                const result = await response.json();
                if (result.available) {
                    this.showFieldValidation(field, true, '邮箱可用');
                } else {
                    this.showFieldValidation(field, false, '邮箱已存在');
                }
            }
        } catch (error) {
            console.warn('检查邮箱可用性失败:', error);
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

    // 填充默认值
    fillDefaultValues() {
        const form = this.userDrawer?.querySelector('#userDrawerForm');
        if (!form) return;
        
        // 填充一些默认值作为示例
        const workDirInput = form.querySelector('#userWorkDirectory');
        const usernameInput = form.querySelector('#userUsername');
        
        if (usernameInput && workDirInput && !workDirInput.value) {
            const username = usernameInput.value;
            if (username) {
                workDirInput.value = `/workspaces/${username}`;
            }
        }
        
        // 默认选择开发者角色
        const userRoleCheckbox = form.querySelector('#roleUser');
        if (userRoleCheckbox && !userRoleCheckbox.checked) {
            userRoleCheckbox.checked = true;
            const roleCard = userRoleCheckbox.closest('.role-card');
            if (roleCard) {
                this.updateRoleCardStyle(roleCard, true);
            }
        }
        
        this.updateStatusInfo();
        this.showMessage('success', '已填充默认值');
    }

    // 清空表单
    clearForm() {
        const form = this.userDrawer?.querySelector('#userDrawerForm');
        if (!form) return;
        
        // 清空所有输入框
        form.querySelectorAll('input[type="text"], input[type="email"], input[type="password"], textarea').forEach(input => {
            input.value = '';
        });
        
        // 取消所有复选框选择
        form.querySelectorAll('input[type="checkbox"]').forEach(checkbox => {
            if (checkbox.id !== 'userEnabled') { // 保持启用状态复选框
                checkbox.checked = false;
                const roleCard = checkbox.closest('.role-card');
                if (roleCard) {
                    this.updateRoleCardStyle(roleCard, false);
                }
            }
        });
        
        // 清空选择框
        form.querySelectorAll('select').forEach(select => {
            select.value = '';
        });
        
        // 清空服务器选择
        const serverCheckboxes = this.userDrawer.querySelectorAll('.server-card input[type="checkbox"], .server-item input[type="checkbox"]');
        serverCheckboxes.forEach(checkbox => {
            checkbox.checked = false;
            const card = checkbox.closest('.server-card, .server-item');
            if (card) {
                card.classList.remove('selected');
            }
        });
        
        this.updateStatusInfo();
        this.showMessage('success', '已清空表单');
    }

    // 验证表单
    validateForm() {
        const form = this.userDrawer?.querySelector('#userDrawerForm');
        if (!form) return false;
        
        let isValid = true;
        const errors = {};
        
        // 验证用户名
        const username = form.querySelector('#userUsername')?.value;
        if (!username || username.length < 3) {
            errors.username = '用户名至少需要3个字符';
            isValid = false;
        }
        
        // 验证邮箱
        const email = form.querySelector('#userEmail')?.value;
        if (!email || !email.includes('@')) {
            errors.email = '请输入有效的邮箱地址';
            isValid = false;
        }
        
        // 验证密码（仅在新增用户时）
        const isEditMode = form.querySelector('input[name="id"]')?.value;
        if (!isEditMode) {
            const password = form.querySelector('#userPassword')?.value;
            if (!password || password.length < 8) {
                errors.password = '密码至少需要8个字符';
                isValid = false;
            }
            
            const confirmPassword = form.querySelector('#userPasswordConfirm')?.value;
            if (password !== confirmPassword) {
                errors.confirmPassword = '两次密码输入不一致';
                isValid = false;
            }
        }
        
        // 验证角色选择
        const roles = form.querySelectorAll('input[name="roles"]:checked');
        if (roles.length === 0) {
            errors.roles = '至少需要选择一个用户角色';
            isValid = false;
        }
        
        // 验证服务器选择（只在有在线服务器时验证）
        const allServers = this.userDrawer.querySelectorAll('[name="drawerServerIds"]');
        const onlineServers = this.userDrawer.querySelectorAll('[name="drawerServerIds"]:not(:disabled)');
        const servers = this.userDrawer.querySelectorAll('[name="drawerServerIds"]:checked');
        
        if (onlineServers.length > 0 && servers.length === 0) {
            errors.servers = '至少需要选择一个在线的服务器';
            isValid = false;
            this.validateServerSelection(); // 显示服务器选择错误
        } else if (onlineServers.length === 0 && allServers.length > 0) {
            errors.servers = '当前没有在线的服务器可供选择，请联系管理员检查服务器状态';
            isValid = false;
            this.validateServerSelection();
        } else {
            // 如果服务器选择有效或没有可选服务器，确保错误状态被清除
            const serverGroup = form.querySelector('#serverSelectionList');
            if (serverGroup && serverGroup.classList.contains('has-error')) {
                serverGroup.classList.remove('has-error');
                const feedback = serverGroup.querySelector('.invalid-feedback');
                if (feedback) feedback.style.display = 'none';
            }
        }
        
        // 显示其他字段的错误
        for (const [fieldName, message] of Object.entries(errors)) {
            if (fieldName !== 'servers') { // 服务器错误已单独处理
                const field = form.querySelector(`#user${fieldName.charAt(0).toUpperCase() + fieldName.slice(1)}`) || form.querySelector(`[name="${fieldName}"]`);
                if (field) {
                    this.showFieldValidation(field, false, message);
                }
            }
        }
        
        if (isValid) {
            this.showMessage('success', '表单验证通过 ✓');
        } else {
            this.showMessage('error', `验证失败：${Object.values(errors).join('、')}`);
        }
        
        this.updateStatusInfo();
        return isValid;
    }

    // 更新状态信息
    updateStatusInfo() {
        if (!this.userDrawer) return;
        
        const form = this.userDrawer.querySelector('#userDrawerForm');
        if (!form) return;
        
        // 更新表单状态
        const formStatusEl = this.userDrawer.querySelector('#formStatus');
        const validationStatusEl = this.userDrawer.querySelector('#validationStatus');
        const serverCountEl = this.userDrawer.querySelector('#serverCount');
        
        // 检查表单填写情况
        const filledFields = form.querySelectorAll('input[required], select[required]');
        let filledCount = 0;
        filledFields.forEach(field => {
            if (field.type === 'checkbox' && field.checked) filledCount++;
            else if (field.value.trim()) filledCount++;
        });
        
        if (formStatusEl) {
            const percentage = Math.round((filledCount / filledFields.length) * 100);
            formStatusEl.textContent = `${percentage}% 已填写`;
        }
        
        // 更新验证状态
        if (validationStatusEl) {
            const isValid = this.validateFormSilently();
            validationStatusEl.textContent = isValid ? '验证通过' : '待完善';
            validationStatusEl.style.color = isValid ? 'var(--success-color, #10b981)' : 'var(--warning-color, #f59e0b)';
        }
        
        // 更新服务器计数
        if (serverCountEl) {
            const serverCount = this.userDrawer.querySelectorAll('[name="drawerServerIds"]:checked').length;
            serverCountEl.textContent = `${serverCount} 个`;
        }
    }

    // 静默验证表单（不显示错误消息）
    validateFormSilently() {
        const form = this.userDrawer?.querySelector('#userDrawerForm');
        if (!form) return false;
        
        const username = form.querySelector('#userUsername')?.value;
        const email = form.querySelector('#userEmail')?.value;
        const roles = form.querySelectorAll('input[name="roles"]:checked');
        const servers = this.userDrawer.querySelectorAll('[name="drawerServerIds"]:checked');
        
        const isEditMode = form.querySelector('input[name="id"]')?.value;
        let passwordValid = true;
        
        if (!isEditMode) {
            const password = form.querySelector('#userPassword')?.value;
            const confirmPassword = form.querySelector('#userPasswordConfirm')?.value;
            passwordValid = password && password.length >= 8 && password === confirmPassword;
        }
        
        return username && username.length >= 3 && 
               email && email.includes('@') && 
               passwordValid && 
               roles.length > 0 && 
               servers.length > 0;
    }

    // 更新预览内容
    updatePreviewContent() {
        if (!this.userDrawer) return;
        
        const form = this.userDrawer.querySelector('#userDrawerForm');
        const previewContent = this.userDrawer.querySelector('#previewContent');
        const previewStatus = this.userDrawer.querySelector('#previewStatus');
        const previewStatusDot = this.userDrawer.querySelector('#previewStatusDot');
        
        if (!form || !previewContent) return;
        
        const username = form.querySelector('#userUsername')?.value || '';
        const email = form.querySelector('#userEmail')?.value || '';
        const workDir = form.querySelector('#userWorkDirectory')?.value || '';
        const roles = Array.from(form.querySelectorAll('input[name="roles"]:checked')).map(cb => cb.value);
        const servers = Array.from(this.userDrawer.querySelectorAll('[name="drawerServerIds"]:checked')).map(cb => {
            const serverCard = cb.closest('.server-card');
            if (serverCard) {
                const label = serverCard.querySelector('.role-card-title');
                return label ? label.textContent : cb.value;
            } else {
                // 向后兼容旧的server-item结构
                const serverItem = cb.closest('.server-item');
                const label = serverItem ? serverItem.querySelector('.server-label') : null;
                return label ? label.textContent : cb.value;
            }
        });
        
        // 更新预览状态
        if (username || email) {
            if (previewStatus) previewStatus.textContent = '实时预览';
            if (previewStatusDot) {
                previewStatusDot.className = 'status-dot';
                if (this.validateFormSilently()) {
                    previewStatusDot.classList.add('success');
                } else {
                    previewStatusDot.classList.add('warning');
                }
            }
        } else {
            if (previewStatus) previewStatus.textContent = '等待输入...';
            if (previewStatusDot) previewStatusDot.className = 'status-dot';
        }
        
        // 构建预览内容
        let previewHtml = '';
        
        if (username || email || workDir || roles.length > 0 || servers.length > 0) {
            previewHtml = '<div class="preview-content">';
            
            if (username) {
                previewHtml += `<div class="preview-item"><strong>用户名:</strong> ${username}</div>`;
            }
            
            if (email) {
                previewHtml += `<div class="preview-item"><strong>邮箱:</strong> ${email}</div>`;
            }
            
            
            if (roles.length > 0) {
                const roleNames = roles.map(role => {
                    switch(role) {
                        case 'USER': return '开发者';
                        case 'ADMIN': return '管理员';
                        case 'SUPER_ADMIN': return '超级管理员';
                        default: return role;
                    }
                });
                previewHtml += `<div class="preview-item"><strong>角色:</strong> ${roleNames.join(', ')}</div>`;
            }
            
            if (servers.length > 0) {
                previewHtml += `<div class="preview-item"><strong>可用服务器:</strong> ${servers.join(', ')}</div>`;
            }
            
            previewHtml += '</div>';
        } else {
            previewHtml = '<p class="text-muted">请开始填写表单，实时预览将在此显示</p>';
        }
        
        previewContent.innerHTML = previewHtml;
    }
    
    // 刷新服务器状态
    async refreshServersStatus() {
        if (this.isRefreshingServers) {
            console.log('服务器状态刷新中，跳过重复请求');
            return;
        }
        
        const refreshBtn = this.userDrawer.querySelector('.refresh-servers-btn');
        const submitBtn = this.userDrawer.querySelector('#userSubmitBtn');
        const serverCards = this.userDrawer.querySelectorAll('.server-card, .role-card[data-server]');
        
        try {
            this.isRefreshingServers = true;
            
            // 设置刷新按钮状态
            if (refreshBtn) {
                refreshBtn.classList.add('refreshing');
                refreshBtn.disabled = true;
            }
            
            // 禁用保存按钮
            if (submitBtn) {
                submitBtn.disabled = true;
            }
            
            // 给所有服务器卡片添加刷新动画
            serverCards.forEach(card => {
                card.classList.add('server-refreshing');
            });
            
            // 获取所有服务器列表
            const response = await fetch('/admin/api/servers/available');
            if (!response.ok) {
                throw new Error('获取服务器列表失败');
            }
            
            const servers = await response.json();
            
            // 并发刷新所有服务器状态
            const refreshPromises = servers.map(async (server) => {
                try {
                    const refreshResponse = await fetch(`/admin/servers/${server.id}/refresh`, {
                        method: 'POST',
                        headers: {
                            'Content-Type': 'application/json',
                            [this.getCSRFHeaderName()]: this.getCSRFToken()
                        }
                    });
                    
                    if (refreshResponse.ok) {
                        const result = await refreshResponse.json();
                        console.log(`服务器 ${server.name} 状态刷新成功:`, result.connectionStatus);
                        return { serverId: server.id, success: true, status: result.connectionStatus };
                    } else {
                        console.warn(`服务器 ${server.name} 状态刷新失败`);
                        return { serverId: server.id, success: false };
                    }
                } catch (error) {
                    console.error(`刷新服务器 ${server.name} 状态时出错:`, error);
                    return { serverId: server.id, success: false };
                }
            });
            
            await Promise.all(refreshPromises);
            
            // 重新加载服务器列表以显示最新状态
            await this.loadAvailableServers();
            
            console.log('所有服务器状态刷新完成');
            
        } catch (error) {
            console.error('刷新服务器状态时出错:', error);
            
            // 显示错误消息
            const messageContainer = this.userDrawer.querySelector('.drawer-messages');
            if (messageContainer) {
                this.showMessage(messageContainer, 'error', '刷新服务器状态失败：' + error.message);
            }
        } finally {
            this.isRefreshingServers = false;
            
            // 恢复按钮状态
            if (refreshBtn) {
                refreshBtn.classList.remove('refreshing');
                refreshBtn.disabled = false;
            }
            
            // 恢复保存按钮
            if (submitBtn) {
                submitBtn.disabled = false;
            }
            
            // 移除所有服务器卡片的刷新动画
            serverCards.forEach(card => {
                card.classList.remove('server-refreshing');
            });
        }
    }

    // 服务器状态管理方法
    async loadServerStatus(serverId = null) {
        if (!serverId) {
            serverId = this.getCurrentServerId();
        }
        
        if (!serverId) {
            this.showServerStatusError('无法获取服务器ID');
            return;
        }

        try {
            this.showServerStatusLoading(true);
            
            const response = await fetch(`/admin/api/servers/${serverId}/status-tags`);
            if (!response.ok) {
                throw new Error('获取服务器状态失败');
            }
            
            const statusData = await response.json();
            if (statusData.status === 'success') {
                this.displayServerStatus(statusData);
            } else {
                throw new Error(statusData.message || '获取状态数据失败');
            }
            
        } catch (error) {
            console.error('加载服务器状态失败:', error);
            this.showServerStatusError(error.message);
        } finally {
            this.showServerStatusLoading(false);
        }
    }

    displayServerStatus(statusData) {
        const statusContainer = document.getElementById('drawerServerStatusTags');
        
        if (!statusContainer) return;
        
        // 清空容器
        statusContainer.innerHTML = '';
        
        if (statusData.tags && statusData.tags.length > 0) {
            statusData.tags.forEach(tag => {
                const tagElement = this.createStatusTagElement(tag);
                statusContainer.appendChild(tagElement);
            });
            
            // 显示状态内容
            this.showServerStatusContent(true);
        } else {
            statusContainer.innerHTML = '<div class="status-empty">暂无异常状态</div>';
            this.showServerStatusContent(true);
        }
    }

    createStatusTagElement(tag) {
        const tagDiv = document.createElement('div');
        tagDiv.className = `status-tag tag-${tag.colorScheme}`;
        
        // 添加特殊样式
        if (tag.isAlert) {
            tagDiv.classList.add('is-alert');
        }
        if (tag.tagType === 'MEMORY_USAGE' && tag.status === 'CRITICAL') {
            tagDiv.classList.add('memory-critical');
        }
        
        tagDiv.innerHTML = `
            <i class="fas ${this.getTagIcon(tag.tagType)}"></i>
            <span class="tag-text">${tag.displayText}</span>
            ${tag.value ? `<span class="tag-value">${tag.value}</span>` : ''}
        `;
        
        if (tag.details) {
            tagDiv.title = tag.details;
        }
        
        return tagDiv;
    }

    getTagIcon(tagType) {
        const iconMap = {
            'CONNECTION': 'fa-plug',
            'WORK_DIRECTORY': 'fa-folder',
            'ACTIVE_USERS': 'fa-users',
            'MONITORING': 'fa-chart-line',
            'PERMISSION': 'fa-lock',
            'MEMORY_USAGE': 'fa-memory',
            'CPU_USAGE': 'fa-microchip',
            'DISK_USAGE': 'fa-hdd',
            'SYSTEM_LOAD': 'fa-tachometer-alt',
            'SECURITY': 'fa-shield-alt',
            'MAINTENANCE': 'fa-tools'
        };
        return iconMap[tagType] || 'fa-info-circle';
    }


    async refreshServerStatus() {
        const serverId = this.getCurrentServerId();
        if (serverId) {
            await this.loadServerStatus(serverId);
        }
    }

    getCurrentServerId() {
        const idInput = this.serverDrawer.querySelector('input[name="id"]');
        return idInput ? idInput.value : null;
    }

    showServerStatusLoading(show) {
        const loadingElement = document.getElementById('serverStatusLoading');
        const contentElement = document.getElementById('serverStatusContent');
        const errorElement = document.getElementById('serverStatusError');
        
        if (show) {
            if (loadingElement) loadingElement.style.display = 'block';
            if (contentElement) contentElement.style.display = 'none';
            if (errorElement) errorElement.style.display = 'none';
        } else {
            if (loadingElement) loadingElement.style.display = 'none';
        }
    }

    showServerStatusContent(show) {
        const contentElement = document.getElementById('serverStatusContent');
        const loadingElement = document.getElementById('serverStatusLoading');
        const errorElement = document.getElementById('serverStatusError');
        
        if (show) {
            if (contentElement) contentElement.style.display = 'block';
            if (loadingElement) loadingElement.style.display = 'none';
            if (errorElement) errorElement.style.display = 'none';
        }
    }

    showServerStatusError(errorMessage) {
        const errorElement = document.getElementById('serverStatusError');
        const contentElement = document.getElementById('serverStatusContent');
        const loadingElement = document.getElementById('serverStatusLoading');
        
        if (errorElement) {
            errorElement.style.display = 'block';
            errorElement.querySelector('span').textContent = errorMessage;
        }
        if (contentElement) contentElement.style.display = 'none';
        if (loadingElement) loadingElement.style.display = 'none';
    }

    toggleServerStatusSection(show, serverId = null) {
        const statusSection = document.getElementById('serverStatusSection');
        if (statusSection) {
            statusSection.style.display = show ? 'block' : 'none';
            
            if (show && serverId) {
                // 延迟加载状态，确保抽屉已完全打开
                setTimeout(() => {
                    this.loadServerStatus(serverId);
                }, 300);
            }
        }
    }
}

// 全局函数定义
let drawerManager;

// 初始化抽屉管理器的函数
function initDrawerManager() {
    if (!drawerManager) {
        try {
            drawerManager = new DrawerManager();
            // 检查初始化是否成功
            if (!drawerManager.overlay || !drawerManager.serverDrawer || !drawerManager.userDrawer) {
                console.error('DrawerManager组件创建不完整');
                drawerManager = null;
                return null;
            }
        } catch (error) {
            console.error('DrawerManager创建失败:', error);
            drawerManager = null;
            return null;
        }
    }
    return drawerManager;
}

// DOM加载完成时初始化
document.addEventListener('DOMContentLoaded', function() {
    initDrawerManager();
});

// 如果DOM已经加载完成，立即初始化
if (document.readyState === 'loading') {
    // DOM还在加载中
} else {
    // DOM已经加载完成
    initDrawerManager();
}

// 调试函数
function debugDrawer() {
    console.log('drawerManager状态:', drawerManager);
    console.log('document.body存在:', !!document.body);
    console.log('尝试初始化drawerManager...');
    const manager = initDrawerManager();
    console.log('初始化结果:', manager);
    return manager;
}

// 全局函数供HTML调用
function addServer() {
    console.log('addServer被调用');
    const manager = drawerManager || initDrawerManager();
    console.log('获取到的manager:', manager);
    if (manager) {
        console.log('尝试打开服务器抽屉');
        manager.openServerDrawer();
    } else {
        console.error('DrawerManager初始化失败');
        alert('功能初始化失败，请刷新页面后重试');
    }
}

function editServer(element) {
    const manager = drawerManager || initDrawerManager();
    if (manager && element) {
        const serverId = element.getAttribute('data-id') || 
                        element.closest('[data-id]')?.getAttribute('data-id');
        if (serverId) {
            manager.openServerDrawer(serverId);
        }
    } else if (!element) {
        console.error('editServer调用时没有传入element参数');
    }
}

function addUser() {
    console.log('addUser被调用');
    const manager = drawerManager || initDrawerManager();
    console.log('获取到的manager:', manager);
    if (manager) {
        console.log('尝试打开用户抽屉');
        manager.openUserDrawer();
    } else {
        console.error('DrawerManager初始化失败');
        alert('功能初始化失败，请刷新页面后重试');
    }
}

function editUser(userId) {
    const manager = drawerManager || initDrawerManager();
    if (manager) {
        manager.openUserDrawer(userId);
    } else {
        console.error('DrawerManager初始化失败');
        alert('功能初始化失败，请刷新页面后重试');
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

// 密码显示/隐藏切换功能
function togglePasswordVisibility(inputId) {
    const passwordInput = document.getElementById(inputId);
    const toggleButton = passwordInput.parentElement.querySelector('.password-toggle-btn');
    const toggleIcon = toggleButton.querySelector('i');
    
    if (passwordInput.type === 'password') {
        // 显示密码
        passwordInput.type = 'text';
        toggleIcon.className = 'fas fa-eye-slash';
        toggleButton.title = '隐藏密码';
    } else {
        // 隐藏密码
        passwordInput.type = 'password';
        toggleIcon.className = 'fas fa-eye';
        toggleButton.title = '显示密码';
    }
}