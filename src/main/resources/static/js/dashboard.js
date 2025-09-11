// 通用仪表板功能
class Dashboard {
    constructor() {
        this.init();
    }
    
    init() {
        this.setupAutoRefresh();
        this.setupNotifications();
        this.setupProgressBars();
    }
    
    // 自动刷新功能
    setupAutoRefresh() {
        const refreshInterval = 30000; // 30秒
        setInterval(() => {
            this.updateStats();
        }, refreshInterval);
    }
    
    // 更新统计数据
    async updateStats() {
        try {
            const response = await fetch('/api/stats');
            const data = await response.json();
            this.updateStatCards(data);
        } catch (error) {
            console.error('更新统计数据失败:', error);
        }
    }
    
    // 更新统计卡片
    updateStatCards(data) {
        const cards = document.querySelectorAll('.stat-card');
        cards.forEach(card => {
            const number = card.querySelector('.stat-number');
            if (number) {
                this.animateNumber(number, parseInt(number.textContent), data.newValue || 0);
            }
        });
    }
    
    // 数字动画
    animateNumber(element, start, end) {
        const duration = 1000;
        const startTime = performance.now();
        
        const animate = (currentTime) => {
            const elapsed = currentTime - startTime;
            const progress = Math.min(elapsed / duration, 1);
            
            const current = Math.floor(start + (end - start) * progress);
            element.textContent = current;
            
            if (progress < 1) {
                requestAnimationFrame(animate);
            }
        };
        
        requestAnimationFrame(animate);
    }
    
    // 通知系统
    setupNotifications() {
        this.notificationContainer = document.createElement('div');
        this.notificationContainer.id = 'notification-container';
        document.body.appendChild(this.notificationContainer);
    }
    
    showNotification(message, type = 'info', duration = 3000) {
        const notification = document.createElement('div');
        notification.className = `notification notification-${type}`;
        notification.textContent = message;
        
        this.notificationContainer.appendChild(notification);
        
        setTimeout(() => {
            notification.style.animation = 'slideOutRight 0.3s ease-in';
            setTimeout(() => {
                if (notification.parentNode) {
                    notification.parentNode.removeChild(notification);
                }
            }, 300);
        }, duration);
    }
    
    // 进度条动画
    setupProgressBars() {
        const progressBars = document.querySelectorAll('.progress-bar-fill');
        progressBars.forEach(bar => {
            const width = bar.style.width || '0%';
            bar.style.width = '0%';
            setTimeout(() => {
                bar.style.width = width;
            }, 100);
        });
    }
}

// 初始化仪表板
document.addEventListener('DOMContentLoaded', () => {
    window.dashboard = new Dashboard();
});

// 全局工具函数
function showLoading(element) {
    const spinner = document.createElement('div');
    spinner.className = 'loading-spinner';
    element.appendChild(spinner);
}

function hideLoading(element) {
    const spinner = element.querySelector('.loading-spinner');
    if (spinner) {
        spinner.remove();
    }
}

function formatBytes(bytes) {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
}

function formatUptime(seconds) {
    const days = Math.floor(seconds / 86400);
    const hours = Math.floor((seconds % 86400) / 3600);
    const minutes = Math.floor((seconds % 3600) / 60);
    
    if (days > 0) {
        return `${days}天 ${hours}小时`;
    } else if (hours > 0) {
        return `${hours}小时 ${minutes}分钟`;
    } else {
        return `${minutes}分钟`;
    }
}

// 工作台JavaScript功能

// 主题切换功能已移至theme.js

// 模拟数据更新功能
function updateDashboardData() {
    // 这里可以添加通过AJAX获取实时数据的逻辑
}

// 页面加载完成后初始化
document.addEventListener('DOMContentLoaded', function() {
    // 可以在这里添加初始化代码
    
    // 模拟定期更新数据
    setInterval(updateDashboardData, 30000); // 每30秒更新一次
});
