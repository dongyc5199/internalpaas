/**
 * 服务器状态标签管理 JavaScript
 * 负责在服务器卡片和抽屉中显示服务器状态标签
 */

class ServerStatusManager {
    constructor() {
        this.statusCache = new Map();
        this.updateInterval = null;
        this.isAutoUpdateEnabled = true;
        this.websocket = null;
        this.retryCount = 0;
        this.maxRetries = 3;
        
        this.init();
    }
    
    init() {
        this.loadAllServerStatus();
        this.startAutoUpdate();
        this.initWebSocket();
        this.bindEvents();
    }
    
    /**
     * 加载所有服务器的状态标签
     */
    async loadAllServerStatus() {
        try {
            const response = await fetch('/admin/api/servers/status-tags');
            const data = await response.json();
            
            if (data.status === 'success' && data.servers) {
                // 更新缓存
                Object.entries(data.servers).forEach(([serverId, serverStatus]) => {
                    this.statusCache.set(serverId, serverStatus);
                    this.renderServerStatusTags(serverId, serverStatus.tags || []);
                });
                
                console.log(`已加载 ${Object.keys(data.servers).length} 个服务器的状态标签`);
            }
            
        } catch (error) {
            console.error('加载服务器状态标签失败:', error);
            this.showErrorNotification('加载服务器状态失败，请刷新页面重试');
        }
    }
    
    /**
     * 加载单个服务器的状态标签
     */
    async loadServerStatus(serverId) {
        try {
            const response = await fetch(`/admin/api/servers/${serverId}/status-tags`);
            const data = await response.json();
            
            if (data.status === 'success' && data.tags) {
                this.statusCache.set(serverId.toString(), {
                    serverId: serverId,
                    tags: data.tags
                });
                this.renderServerStatusTags(serverId, data.tags);
                return data.tags;
            }
            
        } catch (error) {
            console.error(`加载服务器 ${serverId} 状态标签失败:`, error);
            this.renderErrorState(serverId);
        }
        
        return [];
    }
    
    /**
     * 刷新服务器状态
     */
    async refreshServerStatus(serverId) {
        try {
            const response = await fetch(`/admin/api/servers/${serverId}/refresh-status`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                }
            });
            
            const data = await response.json();
            
            if (data.status === 'success' && data.tags) {
                this.statusCache.set(serverId.toString(), {
                    serverId: serverId,
                    tags: data.tags
                });
                this.renderServerStatusTags(serverId, data.tags);
                this.showSuccessNotification(`服务器 ${serverId} 状态已刷新`);
                return data.tags;
            }
            
        } catch (error) {
            console.error(`刷新服务器 ${serverId} 状态失败:`, error);
            this.showErrorNotification(`刷新服务器 ${serverId} 状态失败`);
        }
        
        return [];
    }
    
    /**
     * 渲染服务器状态标签
     */
    renderServerStatusTags(serverId, tags) {
        const serverCard = document.querySelector(`.modern-server-card[data-server-id="${serverId}"]`);
        if (!serverCard) {
            console.warn(`未找到服务器 ${serverId} 的卡片`);
            return;
        }
        
        // 首先查找HTML模板中已存在的状态标签行
        let statusContainer = serverCard.querySelector('.server-status-tags-row');
        if (!statusContainer) {
            // 如果没找到，尝试查找旧的选择器作为备用
            statusContainer = serverCard.querySelector('.server-status-tags');
        }
        
        if (!statusContainer) {
            // 创建状态标签容器，使用与HTML模板一致的类名
            statusContainer = document.createElement('div');
            statusContainer.className = 'server-status-tags-row';
            statusContainer.setAttribute('data-server-id', serverId);
            
            // 插入到服务器元信息后面
            const serverMeta = serverCard.querySelector('.server-meta');
            if (serverMeta) {
                serverMeta.parentNode.insertBefore(statusContainer, serverMeta.nextSibling);
            } else {
                // 如果没有找到server-meta，插入到server-info末尾
                const serverInfo = serverCard.querySelector('.server-info');
                if (serverInfo) {
                    serverInfo.appendChild(statusContainer);
                }
            }
        }
        
        // 清空现有内容
        statusContainer.innerHTML = '';
        
        if (!tags || tags.length === 0) {
            statusContainer.innerHTML = '<span class="status-tags-empty">暂无状态信息</span>';
            return;
        }
        
        // 按优先级排序标签
        const sortedTags = tags.sort((a, b) => (b.priority || 0) - (a.priority || 0));
        
        // 限制显示数量
        const maxTags = 6;
        const visibleTags = sortedTags.slice(0, maxTags);
        const hiddenCount = sortedTags.length - maxTags;
        
        // 渲染标签
        visibleTags.forEach(tag => {
            const tagElement = this.createStatusTagElement(tag);
            statusContainer.appendChild(tagElement);
        });
        
        // 如果有隐藏的标签，显示省略按钮
        if (hiddenCount > 0) {
            const moreButton = document.createElement('button');
            moreButton.className = 'toggle-tags-btn';
            moreButton.textContent = `+${hiddenCount}`;
            moreButton.title = '点击查看更多标签';
            moreButton.onclick = () => this.toggleAllTags(serverId, statusContainer, sortedTags);
            statusContainer.appendChild(moreButton);
        }
    }
    
    /**
     * 创建状态标签元素
     */
    createStatusTagElement(tag) {
        const tagElement = document.createElement('span');
        tagElement.className = `status-tag ${this.getTagColorClass(tag.colorScheme)}`;
        tagElement.title = tag.details || tag.displayText;
        
        // 添加特殊样式
        if (tag.tagType === 'MEMORY_USAGE' && tag.status === 'CRITICAL') {
            tagElement.classList.add('memory-critical');
        }
        
        if (tag.tagType === 'MEMORY_USAGE') {
            tagElement.classList.add('memory-usage');
            if (tag.status === 'CRITICAL') {
                tagElement.classList.add('critical');
            } else if (tag.status === 'WARNING') {
                tagElement.classList.add('warning');
            }
        }
        
        if (tag.tagType === 'CPU_USAGE') {
            tagElement.classList.add('cpu-usage');
        }
        
        if (tag.tagType === 'DISK_USAGE') {
            tagElement.classList.add('disk-usage');
        }
        
        if (tag.tagType === 'CONNECTION') {
            tagElement.classList.add('connection-status');
            if (tag.status === 'ERROR' || tag.status === 'UNKNOWN') {
                tagElement.classList.add('offline');
            }
        }
        
        if (tag.isAlert) {
            tagElement.classList.add('is-alert');
        }
        
        if (tag.priority >= 8) {
            tagElement.classList.add('high-priority');
        }
        
        // 设置内容
        const icon = this.getTagIcon(tag.tagType, tag.status);
        const value = tag.value ? ` (${tag.value})` : '';
        
        tagElement.innerHTML = `
            ${icon ? `<i class="${icon}"></i>` : ''}
            <span class="tag-text">${tag.displayText}</span>
            ${value ? `<span class="tag-value">${value}</span>` : ''}
        `;
        
        return tagElement;
    }
    
    /**
     * 获取标签颜色类
     */
    getTagColorClass(colorScheme) {
        switch(colorScheme) {
            case 'success': return 'tag-success';
            case 'warning': return 'tag-warning';
            case 'danger': return 'tag-danger';
            case 'info': return 'tag-info';
            case 'secondary': return 'tag-secondary';
            default: return 'tag-secondary';
        }
    }
    
    /**
     * 获取标签图标
     */
    getTagIcon(tagType, status) {
        const icons = {
            'CONNECTION': status === 'ACTIVE' ? 'fas fa-wifi' : 'fas fa-wifi-slash',
            'WORK_DIRECTORY': 'fas fa-folder',
            'ACTIVE_USERS': 'fas fa-users',
            'MONITORING': 'fas fa-chart-line',
            'PERMISSION': 'fas fa-key',
            'MEMORY_USAGE': 'fas fa-memory',
            'CPU_USAGE': 'fas fa-microchip',
            'DISK_USAGE': 'fas fa-hdd',
            'SYSTEM_LOAD': 'fas fa-tachometer-alt',
            'SECURITY': 'fas fa-shield-alt'
        };
        
        return icons[tagType] || 'fas fa-tag';
    }
    
    /**
     * 切换显示所有标签
     */
    toggleAllTags(serverId, container, allTags) {
        const isExpanded = container.hasAttribute('data-expanded');
        
        if (isExpanded) {
            // 收起
            this.renderServerStatusTags(serverId, allTags);
        } else {
            // 展开
            container.innerHTML = '';
            allTags.forEach(tag => {
                const tagElement = this.createStatusTagElement(tag);
                container.appendChild(tagElement);
            });
            
            const collapseButton = document.createElement('button');
            collapseButton.className = 'toggle-tags-btn';
            collapseButton.textContent = '收起';
            collapseButton.onclick = () => this.toggleAllTags(serverId, container, allTags);
            container.appendChild(collapseButton);
            
            container.setAttribute('data-expanded', 'true');
        }
    }
    
    /**
     * 渲染错误状态
     */
    renderErrorState(serverId) {
        const serverCard = document.querySelector(`.modern-server-card[data-server-id="${serverId}"]`);
        if (!serverCard) return;
        
        // 首先查找HTML模板中已存在的状态标签行
        let statusContainer = serverCard.querySelector('.server-status-tags-row');
        if (!statusContainer) {
            // 如果没找到，尝试查找旧的选择器作为备用
            statusContainer = serverCard.querySelector('.server-status-tags');
        }
        
        if (!statusContainer) {
            statusContainer = document.createElement('div');
            statusContainer.className = 'server-status-tags-row';
            statusContainer.setAttribute('data-server-id', serverId);
            const serverInfo = serverCard.querySelector('.server-info');
            if (serverInfo) {
                serverInfo.appendChild(statusContainer);
            }
        }
        
        statusContainer.innerHTML = '<span class="status-tags-empty text-danger">状态加载失败</span>';
    }
    
    /**
     * 开始自动更新
     */
    startAutoUpdate() {
        if (this.updateInterval) {
            clearInterval(this.updateInterval);
        }
        
        // 每30秒自动更新一次
        this.updateInterval = setInterval(() => {
            if (this.isAutoUpdateEnabled) {
                this.loadAllServerStatus();
            }
        }, 30000);
    }
    
    /**
     * 停止自动更新
     */
    stopAutoUpdate() {
        if (this.updateInterval) {
            clearInterval(this.updateInterval);
            this.updateInterval = null;
        }
    }
    
    /**
     * 初始化WebSocket连接
     */
    initWebSocket() {
        if (typeof SockJS === 'undefined' || typeof Stomp === 'undefined') {
            console.warn('WebSocket库未加载，跳过实时通信');
            return;
        }
        
        try {
            const socket = new SockJS('/ws');
            this.websocket = Stomp.over(socket);
            
            this.websocket.connect({}, (frame) => {
                console.log('WebSocket已连接:', frame);
                this.retryCount = 0;
                
                // 订阅服务器状态更新
                this.websocket.subscribe('/topic/server-status', (message) => {
                    const statusUpdate = JSON.parse(message.body);
                    this.handleStatusUpdate(statusUpdate);
                });
                
                // 订阅告警通知
                this.websocket.subscribe('/topic/alerts', (message) => {
                    const alert = JSON.parse(message.body);
                    this.handleAlert(alert);
                });
                
            }, (error) => {
                console.error('WebSocket连接失败:', error);
                this.handleWebSocketError();
            });
            
        } catch (error) {
            console.error('初始化WebSocket失败:', error);
        }
    }
    
    /**
     * 处理状态更新
     */
    handleStatusUpdate(statusUpdate) {
        if (statusUpdate.serverId) {
            // 刷新特定服务器状态
            this.loadServerStatus(statusUpdate.serverId);
        }
    }
    
    /**
     * 处理告警信息
     */
    handleAlert(alert) {
        if (alert.type === 'critical_memory_alert' || alert.type === 'emergency_memory_alert') {
            // 显示内存告警通知
            this.showCriticalAlert(alert);
            
            // 立即刷新相关服务器状态
            if (alert.serverId) {
                this.loadServerStatus(alert.serverId);
            }
        }
    }
    
    /**
     * 处理WebSocket错误
     */
    handleWebSocketError() {
        if (this.retryCount < this.maxRetries) {
            this.retryCount++;
            setTimeout(() => {
                console.log(`尝试重连WebSocket (${this.retryCount}/${this.maxRetries})`);
                this.initWebSocket();
            }, 5000 * this.retryCount);
        }
    }
    
    /**
     * 绑定事件
     */
    bindEvents() {
        // 页面可见性变化
        document.addEventListener('visibilitychange', () => {
            if (document.hidden) {
                this.isAutoUpdateEnabled = false;
            } else {
                this.isAutoUpdateEnabled = true;
                this.loadAllServerStatus(); // 页面重新可见时立即更新
            }
        });
        
        // 窗口焦点事件
        window.addEventListener('focus', () => {
            this.loadAllServerStatus();
        });
    }
    
    /**
     * 显示成功通知
     */
    showSuccessNotification(message) {
        this.showNotification(message, 'success');
    }
    
    /**
     * 显示错误通知
     */
    showErrorNotification(message) {
        this.showNotification(message, 'error');
    }
    
    /**
     * 显示严重告警
     */
    showCriticalAlert(alert) {
        // 创建告警通知
        const alertEl = document.createElement('div');
        alertEl.className = 'alert alert-danger alert-dismissible fade show position-fixed';
        alertEl.style.cssText = 'top: 20px; right: 20px; z-index: 9999; max-width: 400px;';
        alertEl.innerHTML = `
            <strong><i class="fas fa-exclamation-triangle"></i> ${alert.title || '严重告警'}</strong><br>
            ${alert.message}<br>
            <small class="text-muted">时间: ${new Date(alert.timestamp).toLocaleString()}</small>
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        `;
        
        document.body.appendChild(alertEl);
        
        // 5秒后自动关闭
        setTimeout(() => {
            if (alertEl.parentNode) {
                alertEl.remove();
            }
        }, 5000);
        
        // 播放提示音（如果允许）
        if (alert.sound) {
            this.playNotificationSound();
        }
    }
    
    /**
     * 通用通知显示
     */
    showNotification(message, type = 'info') {
        // 简单的toast通知
        const toast = document.createElement('div');
        toast.className = `alert alert-${type === 'success' ? 'success' : 'danger'} position-fixed`;
        toast.style.cssText = 'top: 20px; right: 20px; z-index: 9999; max-width: 300px;';
        toast.textContent = message;
        
        document.body.appendChild(toast);
        
        setTimeout(() => {
            if (toast.parentNode) {
                toast.remove();
            }
        }, 3000);
    }
    
    /**
     * 播放通知声音
     */
    playNotificationSound() {
        try {
            // 创建简单的提示音
            const audioContext = new (window.AudioContext || window.webkitAudioContext)();
            const oscillator = audioContext.createOscillator();
            const gainNode = audioContext.createGain();
            
            oscillator.connect(gainNode);
            gainNode.connect(audioContext.destination);
            
            oscillator.frequency.setValueAtTime(800, audioContext.currentTime);
            gainNode.gain.setValueAtTime(0.1, audioContext.currentTime);
            
            oscillator.start();
            oscillator.stop(audioContext.currentTime + 0.1);
        } catch (error) {
            // 静默失败
        }
    }
    
    /**
     * 获取服务器状态缓存
     */
    getServerStatus(serverId) {
        return this.statusCache.get(serverId.toString());
    }
    
    /**
     * 销毁管理器
     */
    destroy() {
        this.stopAutoUpdate();
        
        if (this.websocket) {
            this.websocket.disconnect();
        }
        
        this.statusCache.clear();
    }
}

// 全局实例
window.serverStatusManager = null;

// DOM加载完成后初始化
document.addEventListener('DOMContentLoaded', function() {
    // 只在服务器管理页面初始化
    if (window.location.pathname.includes('/admin/servers')) {
        window.serverStatusManager = new ServerStatusManager();
        
        // 为现有的刷新按钮添加状态刷新功能
        document.addEventListener('click', function(event) {
            const refreshBtn = event.target.closest('[data-action="refresh"]');
            if (refreshBtn) {
                const serverId = refreshBtn.getAttribute('data-id');
                if (serverId && window.serverStatusManager) {
                    window.serverStatusManager.refreshServerStatus(serverId);
                }
            }
        });
    }
});

// 页面卸载时清理
window.addEventListener('beforeunload', function() {
    if (window.serverStatusManager) {
        window.serverStatusManager.destroy();
    }
});