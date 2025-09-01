/**
 * Application Detail Page JavaScript
 * 提供应用详情页面的交互功能和实时数据更新
 */

class ApplicationDetailManager {
    constructor(appConfig) {
        this.appConfig = appConfig;
        this.csrfToken = document.querySelector('meta[name="_csrf"]').getAttribute('content');
        this.csrfHeader = document.querySelector('meta[name="_csrf_header"]').getAttribute('content');
        
        // 组件实例
        this.metricsChart = null;
        this.logWebSocket = null;
        this.metricsUpdateInterval = null;
        this.statusCheckInterval = null;
        
        // 配置选项
        this.autoScrollEnabled = true;
        this.metricsUpdateFrequency = 5000; // 5秒
        this.statusCheckFrequency = 10000; // 10秒
        this.maxLogLines = 1000;
        this.maxMetricsPoints = 50;
        
        // 数据存储
        this.metricsHistory = {
            timestamps: [],
            cpuData: [],
            memoryData: [],
            diskData: []
        };
        
        this.init();
    }
    
    /**
     * 初始化应用详情管理器
     */
    init() {
        this.setupEventListeners();
        this.initializeCharts();
        this.startRealTimeUpdates();
        this.loadInitialData();
        
        console.log('Application Detail Manager initialized for app:', this.appConfig.name);
    }
    
    /**
     * 设置事件监听器
     */
    setupEventListeners() {
        // 时间范围选择器
        document.querySelectorAll('input[name="timeRange"]').forEach(radio => {
            radio.addEventListener('change', (e) => {
                this.changeTimeRange(e.target.value);
            });
        });
        
        // 应用状态检查
        document.addEventListener('visibilitychange', () => {
            if (document.hidden) {
                this.pauseUpdates();
            } else {
                this.resumeUpdates();
            }
        });
        
        // 窗口关闭前清理
        window.addEventListener('beforeunload', () => {
            this.cleanup();
        });
        
        // 键盘快捷键
        document.addEventListener('keydown', (e) => {
            if (e.ctrlKey) {
                switch(e.key) {
                    case 'r':
                        e.preventDefault();
                        this.refreshMetrics();
                        break;
                    case 's':
                        e.preventDefault();
                        if (this.appConfig.status === 'STOPPED') {
                            this.startApplication();
                        } else {
                            this.stopApplication();
                        }
                        break;
                }
            }
        });
    }
    
    /**
     * 初始化性能图表
     */
    initializeCharts() {
        const ctx = document.getElementById('metricsChart');
        if (!ctx) {
            console.warn('Metrics chart canvas not found');
            return;
        }
        
        this.metricsChart = new Chart(ctx.getContext('2d'), {
            type: 'line',
            data: {
                labels: [],
                datasets: [
                    {
                        label: 'CPU使用率 (%)',
                        data: [],
                        borderColor: '#007bff',
                        backgroundColor: 'rgba(0, 123, 255, 0.1)',
                        borderWidth: 2,
                        fill: true,
                        tension: 0.4,
                        pointRadius: 0,
                        pointHoverRadius: 6
                    },
                    {
                        label: '内存使用率 (%)',
                        data: [],
                        borderColor: '#28a745',
                        backgroundColor: 'rgba(40, 167, 69, 0.1)',
                        borderWidth: 2,
                        fill: true,
                        tension: 0.4,
                        pointRadius: 0,
                        pointHoverRadius: 6
                    },
                    {
                        label: '磁盘使用率 (%)',
                        data: [],
                        borderColor: '#ffc107',
                        backgroundColor: 'rgba(255, 193, 7, 0.1)',
                        borderWidth: 2,
                        fill: false,
                        tension: 0.4,
                        pointRadius: 0,
                        pointHoverRadius: 6
                    }
                ]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                interaction: {
                    intersect: false,
                    mode: 'index'
                },
                plugins: {
                    legend: {
                        display: true,
                        position: 'top',
                        labels: {
                            usePointStyle: true,
                            padding: 20
                        }
                    },
                    tooltip: {
                        backgroundColor: 'rgba(0, 0, 0, 0.8)',
                        titleColor: '#fff',
                        bodyColor: '#fff',
                        borderColor: '#007bff',
                        borderWidth: 1,
                        displayColors: true,
                        callbacks: {
                            title: (tooltipItems) => {
                                return new Date(tooltipItems[0].label).toLocaleTimeString();
                            }
                        }
                    }
                },
                scales: {
                    y: {
                        beginAtZero: true,
                        max: 100,
                        grid: {
                            color: 'rgba(0, 0, 0, 0.1)'
                        },
                        ticks: {
                            callback: function(value) {
                                return value + '%';
                            }
                        }
                    },
                    x: {
                        type: 'time',
                        time: {
                            displayFormats: {
                                minute: 'HH:mm',
                                hour: 'HH:mm'
                            }
                        },
                        grid: {
                            color: 'rgba(0, 0, 0, 0.1)'
                        }
                    }
                },
                elements: {
                    point: {
                        hoverBackgroundColor: '#fff',
                        hoverBorderWidth: 2
                    }
                },
                animation: {
                    duration: 750,
                    easing: 'easeInOutQuart'
                }
            }
        });
    }
    
    /**
     * 开始实时更新
     */
    startRealTimeUpdates() {
        // 指标更新
        this.metricsUpdateInterval = setInterval(() => {
            this.updateMetrics();
        }, this.metricsUpdateFrequency);
        
        // 状态检查
        this.statusCheckInterval = setInterval(() => {
            this.checkApplicationStatus();
        }, this.statusCheckFrequency);
        
        // WebSocket连接
        this.connectToLogWebSocket();
    }
    
    /**
     * 暂停更新
     */
    pauseUpdates() {
        if (this.metricsUpdateInterval) {
            clearInterval(this.metricsUpdateInterval);
            this.metricsUpdateInterval = null;
        }
        if (this.statusCheckInterval) {
            clearInterval(this.statusCheckInterval);
            this.statusCheckInterval = null;
        }
        if (this.logWebSocket) {
            this.logWebSocket.close();
        }
    }
    
    /**
     * 恢复更新
     */
    resumeUpdates() {
        if (!this.metricsUpdateInterval) {
            this.startRealTimeUpdates();
        }
    }
    
    /**
     * 加载初始数据
     */
    async loadInitialData() {
        try {
            await Promise.all([
                this.updateMetrics(),
                this.loadInitialLogs(),
                this.checkApplicationStatus()
            ]);
        } catch (error) {
            console.error('Failed to load initial data:', error);
            this.showAlert('加载初始数据失败', 'warning');
        }
    }
    
    /**
     * 更新监控指标
     */
    async updateMetrics() {
        if (this.appConfig.status !== 'RUNNING') {
            this.updateMetricCards(null);
            return;
        }
        
        try {
            const response = await fetch(`/apps/${this.appConfig.id}/metrics`, {
                method: 'GET',
                headers: {
                    [this.csrfHeader]: this.csrfToken
                }
            });
            
            if (!response.ok) {
                throw new Error(`HTTP ${response.status}`);
            }
            
            const metrics = await response.json();
            this.updateMetricCards(metrics);
            this.updateChart(metrics);
            
        } catch (error) {
            console.error('Failed to update metrics:', error);
            this.updateMetricCards(null);
        }
    }
    
    /**
     * 更新指标卡片显示
     */
    updateMetricCards(metrics) {
        const elements = {
            cpu: document.getElementById('cpuUsage'),
            memory: document.getElementById('memoryUsage'),
            disk: document.getElementById('diskUsage'),
            uptime: document.getElementById('uptime')
        };
        
        if (!metrics) {
            Object.values(elements).forEach(el => {
                if (el) el.textContent = '--';
            });
            return;
        }
        
        // 更新CPU使用率
        if (elements.cpu) {
            const cpu = metrics.cpuUsage || 0;
            elements.cpu.textContent = cpu.toFixed(1) + '%';
            this.updateMetricCardColor(elements.cpu.closest('.metric-card'), cpu, 'cpu');
        }
        
        // 更新内存使用率
        if (elements.memory) {
            const memory = metrics.memoryUsage || 0;
            elements.memory.textContent = memory.toFixed(1) + '%';
            this.updateMetricCardColor(elements.memory.closest('.metric-card'), memory, 'memory');
        }
        
        // 更新磁盘使用率
        if (elements.disk) {
            const disk = metrics.diskUsage || 0;
            elements.disk.textContent = disk.toFixed(1) + '%';
            this.updateMetricCardColor(elements.disk.closest('.metric-card'), disk, 'disk');
        }
        
        // 更新运行时间
        if (elements.uptime) {
            elements.uptime.textContent = metrics.uptime || '--';
        }
    }
    
    /**
     * 根据使用率更新指标卡片颜色
     */
    updateMetricCardColor(card, value, type) {
        if (!card) return;
        
        const h3 = card.querySelector('h3');
        if (!h3) return;
        
        // 移除现有的颜色类
        h3.classList.remove('text-success', 'text-warning', 'text-danger', 'text-primary', 'text-info');
        
        // 根据使用率设置颜色
        if (value >= 90) {
            h3.classList.add('text-danger');
            card.style.animation = 'pulse 2s infinite';
        } else if (value >= 70) {
            h3.classList.add('text-warning');
            card.style.animation = '';
        } else {
            card.style.animation = '';
            switch(type) {
                case 'cpu':
                    h3.classList.add('text-primary');
                    break;
                case 'memory':
                    h3.classList.add('text-success');
                    break;
                case 'disk':
                    h3.classList.add('text-warning');
                    break;
                default:
                    h3.classList.add('text-info');
            }
        }
    }
    
    /**
     * 更新图表数据
     */
    updateChart(metrics) {
        if (!this.metricsChart || !metrics) return;
        
        const now = new Date();
        const history = this.metricsHistory;
        
        // 添加新数据点
        history.timestamps.push(now);
        history.cpuData.push(metrics.cpuUsage || 0);
        history.memoryData.push(metrics.memoryUsage || 0);
        history.diskData.push(metrics.diskUsage || 0);
        
        // 保持数据点数量限制
        if (history.timestamps.length > this.maxMetricsPoints) {
            history.timestamps.shift();
            history.cpuData.shift();
            history.memoryData.shift();
            history.diskData.shift();
        }
        
        // 更新图表
        this.metricsChart.data.labels = [...history.timestamps];
        this.metricsChart.data.datasets[0].data = [...history.cpuData];
        this.metricsChart.data.datasets[1].data = [...history.memoryData];
        this.metricsChart.data.datasets[2].data = [...history.diskData];
        
        this.metricsChart.update('none');
    }
    
    /**
     * 改变时间范围
     */
    changeTimeRange(range) {
        // 这里可以实现不同时间范围的数据加载逻辑
        console.log('Time range changed to:', range);
        
        // 清空当前数据
        this.metricsHistory = {
            timestamps: [],
            cpuData: [],
            memoryData: [],
            diskData: []
        };
        
        if (this.metricsChart) {
            this.metricsChart.data.labels = [];
            this.metricsChart.data.datasets.forEach(dataset => {
                dataset.data = [];
            });
            this.metricsChart.update();
        }
        
        // 根据时间范围调整更新频率
        const frequencies = {
            '5m': 2000,   // 2秒
            '30m': 5000,  // 5秒
            '1h': 10000   // 10秒
        };
        
        this.metricsUpdateFrequency = frequencies[range] || 5000;
        
        // 重启定时器
        if (this.metricsUpdateInterval) {
            clearInterval(this.metricsUpdateInterval);
            this.metricsUpdateInterval = setInterval(() => {
                this.updateMetrics();
            }, this.metricsUpdateFrequency);
        }
    }
    
    /**
     * 连接日志WebSocket
     */
    connectToLogWebSocket() {
        if (this.appConfig.status !== 'RUNNING') {
            return;
        }
        
        try {
            const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
            const wsUrl = `${protocol}//${window.location.host}/ws/logs/${this.appConfig.id}`;
            
            this.logWebSocket = new WebSocket(wsUrl);
            
            this.logWebSocket.onopen = () => {
                console.log('Log WebSocket connected');
                this.showConnectionStatus(true);
            };
            
            this.logWebSocket.onmessage = (event) => {
                this.appendLogMessage(event.data);
            };
            
            this.logWebSocket.onclose = () => {
                console.log('Log WebSocket disconnected');
                this.showConnectionStatus(false);
                
                // 尝试重连
                setTimeout(() => {
                    if (this.appConfig.status === 'RUNNING') {
                        this.connectToLogWebSocket();
                    }
                }, 5000);
            };
            
            this.logWebSocket.onerror = (error) => {
                console.error('Log WebSocket error:', error);
                this.showConnectionStatus(false);
            };
            
        } catch (error) {
            console.error('Failed to connect WebSocket:', error);
        }
    }
    
    /**
     * 显示连接状态
     */
    showConnectionStatus(connected) {
        // 可以在页面上显示WebSocket连接状态
        const statusIndicator = document.getElementById('wsStatus');
        if (statusIndicator) {
            statusIndicator.textContent = connected ? '已连接' : '未连接';
            statusIndicator.className = connected ? 'text-success' : 'text-danger';
        }
    }
    
    /**
     * 加载初始日志
     */
    async loadInitialLogs() {
        try {
            const response = await fetch(`/apps/${this.appConfig.id}/logs?lines=100`, {
                method: 'GET',
                headers: {
                    [this.csrfHeader]: this.csrfToken
                }
            });
            
            if (response.ok) {
                const logs = await response.text();
                const logViewer = document.getElementById('logViewer');
                if (logViewer) {
                    logViewer.textContent = logs || '暂无日志内容';
                    this.scrollToBottom();
                }
            }
        } catch (error) {
            console.error('Failed to load initial logs:', error);
            const logViewer = document.getElementById('logViewer');
            if (logViewer) {
                logViewer.textContent = '加载日志失败: ' + error.message;
            }
        }
    }
    
    /**
     * 添加日志消息
     */
    appendLogMessage(message) {
        const logViewer = document.getElementById('logViewer');
        if (!logViewer) return;
        
        // 添加时间戳
        const timestamp = new Date().toLocaleTimeString();
        const logLine = `[${timestamp}] ${message}\n`;
        
        logViewer.textContent += logLine;
        
        // 限制日志行数
        const lines = logViewer.textContent.split('\n');
        if (lines.length > this.maxLogLines) {
            const trimmedLines = lines.slice(-this.maxLogLines);
            logViewer.textContent = trimmedLines.join('\n');
        }
        
        // 自动滚动
        if (this.autoScrollEnabled) {
            this.scrollToBottom();
        }
    }
    
    /**
     * 滚动到底部
     */
    scrollToBottom() {
        const logViewer = document.getElementById('logViewer');
        if (logViewer) {
            logViewer.scrollTop = logViewer.scrollHeight;
        }
    }
    
    /**
     * 检查应用状态
     */
    async checkApplicationStatus() {
        try {
            const response = await fetch(`/apps/${this.appConfig.id}/health`, {
                method: 'GET',
                headers: {
                    [this.csrfHeader]: this.csrfToken
                }
            });
            
            if (response.ok) {
                const health = await response.json();
                this.updateApplicationStatus(health);
            }
        } catch (error) {
            console.error('Failed to check application status:', error);
        }
    }
    
    /**
     * 更新应用状态显示
     */
    updateApplicationStatus(health) {
        const statusElement = document.querySelector('.app-status-indicator');
        if (!statusElement) return;
        
        const statusText = statusElement.querySelector('span:last-child');
        const statusDot = statusElement.querySelector('.status-dot');
        
        if (health.status !== this.appConfig.status) {
            // 状态发生变化，更新页面
            this.appConfig.status = health.status;
            
            // 更新状态显示
            const statusMap = {
                'RUNNING': { text: '运行中', class: 'running' },
                'STOPPED': { text: '已停止', class: 'stopped' },
                'STARTING': { text: '启动中', class: 'starting' },
                'ERROR': { text: '错误', class: 'stopped' }
            };
            
            const statusInfo = statusMap[health.status] || statusMap['STOPPED'];
            
            if (statusText) statusText.textContent = statusInfo.text;
            
            // 更新CSS类
            statusElement.className = `app-status-indicator ${statusInfo.class}`;
            
            // 更新按钮状态
            this.updateActionButtons();
            
            // 状态变化通知
            this.showAlert(`应用状态已更新为: ${statusInfo.text}`, 'info');
        }
    }
    
    /**
     * 更新操作按钮状态
     */
    updateActionButtons() {
        const startBtn = document.querySelector('.btn-success');
        const stopBtn = document.querySelector('.btn-warning');
        const restartBtn = document.querySelector('.btn-info');
        
        if (startBtn) {
            startBtn.disabled = (this.appConfig.status === 'RUNNING');
        }
        if (stopBtn) {
            stopBtn.disabled = (this.appConfig.status === 'STOPPED');
        }
        if (restartBtn) {
            restartBtn.disabled = (this.appConfig.status === 'STOPPED');
        }
    }
    
    /**
     * 应用操作：启动
     */
    async startApplication() {
        await this.performApplicationAction('start', '启动');
    }
    
    /**
     * 应用操作：停止
     */
    async stopApplication() {
        await this.performApplicationAction('stop', '停止');
    }
    
    /**
     * 应用操作：重启
     */
    async restartApplication() {
        if (confirm('确定要重启应用吗？')) {
            await this.performApplicationAction('restart', '重启');
        }
    }
    
    /**
     * 执行应用操作
     */
    async performApplicationAction(action, actionName) {
        const btn = event?.target?.closest('button');
        let originalHtml = '';
        
        if (btn) {
            originalHtml = btn.innerHTML;
            btn.innerHTML = `<i class="fas fa-spinner fa-spin"></i> ${actionName}中...`;
            btn.disabled = true;
        }
        
        try {
            const response = await fetch(`/apps/${this.appConfig.id}/${action}`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded',
                    [this.csrfHeader]: this.csrfToken
                }
            });
            
            if (response.ok) {
                this.showAlert(`应用${actionName}操作已提交`, 'success');
                
                // 延迟重新加载页面以查看结果
                setTimeout(() => {
                    window.location.reload();
                }, 2000);
            } else {
                throw new Error(`HTTP ${response.status}`);
            }
            
        } catch (error) {
            console.error(`Application ${action} failed:`, error);
            this.showAlert(`应用${actionName}失败: ${error.message}`, 'danger');
        } finally {
            if (btn) {
                btn.innerHTML = originalHtml;
                btn.disabled = false;
            }
        }
    }
    
    /**
     * 刷新指标
     */
    async refreshMetrics() {
        await this.updateMetrics();
        this.showAlert('监控数据已刷新', 'info');
    }
    
    /**
     * 清空日志显示
     */
    clearLogs() {
        if (confirm('确定要清空日志显示吗？')) {
            const logViewer = document.getElementById('logViewer');
            if (logViewer) {
                logViewer.textContent = '';
            }
        }
    }
    
    /**
     * 下载日志
     */
    downloadLogs() {
        window.open(`/apps/${this.appConfig.id}/logs/download`, '_blank');
    }
    
    /**
     * 切换自动滚动
     */
    toggleAutoScroll() {
        this.autoScrollEnabled = !this.autoScrollEnabled;
        
        const btn = document.getElementById('toggleAutoScroll');
        if (btn) {
            if (this.autoScrollEnabled) {
                btn.innerHTML = '<i class="fas fa-arrow-down"></i> 自动滚动';
                btn.classList.remove('btn-outline-primary');
                btn.classList.add('btn-primary');
            } else {
                btn.innerHTML = '<i class="fas fa-pause"></i> 已暂停';
                btn.classList.remove('btn-primary');
                btn.classList.add('btn-outline-primary');
            }
        }
        
        if (this.autoScrollEnabled) {
            this.scrollToBottom();
        }
    }
    
    /**
     * 显示提示信息
     */
    showAlert(message, type = 'info', duration = 3000) {
        // 移除现有的提示
        document.querySelectorAll('.alert-floating').forEach(alert => {
            alert.remove();
        });
        
        const alertDiv = document.createElement('div');
        alertDiv.className = `alert alert-${type} alert-dismissible fade show position-fixed alert-floating`;
        alertDiv.style.cssText = `
            top: 20px; 
            right: 20px; 
            z-index: 9999; 
            min-width: 300px;
            box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
        `;
        alertDiv.innerHTML = `
            <i class="fas fa-${this.getAlertIcon(type)}"></i>
            ${message}
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        `;
        
        document.body.appendChild(alertDiv);
        
        // 自动移除
        setTimeout(() => {
            if (alertDiv.parentNode) {
                alertDiv.classList.remove('show');
                setTimeout(() => {
                    alertDiv.remove();
                }, 150);
            }
        }, duration);
        
        return alertDiv;
    }
    
    /**
     * 获取提示图标
     */
    getAlertIcon(type) {
        const icons = {
            success: 'check-circle',
            danger: 'exclamation-triangle',
            warning: 'exclamation-triangle',
            info: 'info-circle'
        };
        return icons[type] || 'info-circle';
    }
    
    /**
     * 清理资源
     */
    cleanup() {
        // 清理定时器
        if (this.metricsUpdateInterval) {
            clearInterval(this.metricsUpdateInterval);
        }
        if (this.statusCheckInterval) {
            clearInterval(this.statusCheckInterval);
        }
        
        // 关闭WebSocket
        if (this.logWebSocket) {
            this.logWebSocket.close();
        }
        
        // 销毁图表
        if (this.metricsChart) {
            this.metricsChart.destroy();
        }
        
        console.log('Application Detail Manager cleaned up');
    }
}

// 全局函数 - 兼容模板中的内联事件处理器
let appDetailManager = null;

// 应用操作函数
function startApplication() {
    if (appDetailManager) {
        appDetailManager.startApplication();
    }
}

function stopApplication() {
    if (appDetailManager) {
        appDetailManager.stopApplication();
    }
}

function restartApplication() {
    if (appDetailManager) {
        appDetailManager.restartApplication();
    }
}

// 日志操作函数
function clearLogs() {
    if (appDetailManager) {
        appDetailManager.clearLogs();
    }
}

function downloadLogs() {
    if (appDetailManager) {
        appDetailManager.downloadLogs();
    }
}

function toggleAutoScroll() {
    if (appDetailManager) {
        appDetailManager.toggleAutoScroll();
    }
}

// 配置相关函数
function editConfiguration() {
    const modal = new bootstrap.Modal(document.getElementById('editConfigModal'));
    modal.show();
}

function saveConfiguration() {
    if (!appDetailManager) return;
    
    const formData = new FormData();
    formData.append('jvmOptions', document.getElementById('configJvmOptions').value);
    formData.append('programArgs', document.getElementById('configProgramArgs').value);
    formData.append('envVars', document.getElementById('configEnvVars').value);
    formData.append('enableJmx', document.getElementById('configEnableJmx').checked);
    
    fetch(`/apps/${appDetailManager.appConfig.id}/config`, {
        method: 'POST',
        headers: {
            [appDetailManager.csrfHeader]: appDetailManager.csrfToken
        },
        body: formData
    })
    .then(response => {
        if (response.ok) {
            appDetailManager.showAlert('配置保存成功', 'success');
            bootstrap.Modal.getInstance(document.getElementById('editConfigModal')).hide();
            setTimeout(() => location.reload(), 1500);
        } else {
            throw new Error(`HTTP ${response.status}`);
        }
    })
    .catch(error => {
        console.error('Save configuration failed:', error);
        appDetailManager.showAlert('配置保存失败: ' + error.message, 'danger');
    });
}

function deleteApplication() {
    if (!appDetailManager) return;
    
    if (confirm('确定要删除此应用吗？此操作不可逆！')) {
        fetch(`/apps/${appDetailManager.appConfig.id}/delete`, {
            method: 'POST',
            headers: {
                [appDetailManager.csrfHeader]: appDetailManager.csrfToken
            }
        })
        .then(response => {
            if (response.ok) {
                appDetailManager.showAlert('应用删除成功', 'success');
                setTimeout(() => window.location.href = '/apps', 1500);
            } else {
                throw new Error(`HTTP ${response.status}`);
            }
        })
        .catch(error => {
            console.error('Delete application failed:', error);
            appDetailManager.showAlert('应用删除失败: ' + error.message, 'danger');
        });
    }
}

// 快捷操作函数
function openLogFile() {
    if (appDetailManager) {
        appDetailManager.showAlert('日志文件路径已复制到剪贴板', 'info');
    }
}

function viewSystemMetrics() {
    window.open('/monitoring/dashboard', '_blank');
}

function openJmxConsole() {
    if (appDetailManager && appDetailManager.appConfig.port) {
        window.open(`http://localhost:${appDetailManager.appConfig.port}/actuator`, '_blank');
    }
}

function healthCheck() {
    if (!appDetailManager) return;
    
    fetch(`/apps/${appDetailManager.appConfig.id}/health`, {
        method: 'GET',
        headers: {
            [appDetailManager.csrfHeader]: appDetailManager.csrfToken
        }
    })
    .then(response => response.json())
    .then(data => {
        if (data.healthStatus === 'UP') {
            appDetailManager.showAlert('应用健康状态良好', 'success');
        } else {
            appDetailManager.showAlert(`健康检查: ${data.message || '状态异常'}`, 'warning');
        }
    })
    .catch(error => {
        console.error('Health check failed:', error);
        appDetailManager.showAlert('健康检查失败: ' + error.message, 'danger');
    });
}