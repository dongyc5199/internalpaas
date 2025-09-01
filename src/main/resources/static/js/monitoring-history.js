/**
 * 监控历史数据 - 高级图表管理系统
 * 使用Chart.js实现专业级监控数据可视化
 */

// 全局变量
let charts = {};
let currentData = {};
let realtimeInterval = null;
let colors = {
    primary: '#007bff',
    success: '#28a745',
    info: '#17a2b8',
    warning: '#ffc107',
    danger: '#dc3545',
    secondary: '#6c757d',
    light: '#f8f9fa',
    dark: '#343a40'
};

// Chart.js全局配置
Chart.defaults.font.family = '-apple-system,BlinkMacSystemFont,"Segoe UI",Roboto,"Helvetica Neue",Arial,sans-serif';
Chart.defaults.color = '#6c757d';
Chart.defaults.borderColor = '#dee2e6';
Chart.defaults.backgroundColor = 'rgba(0,0,0,0.1)';

/**
 * 初始化监控历史系统
 */
function initializeMonitoringHistory() {
    console.log('初始化监控历史系统...');
    
    // 初始化时间选择器
    initializeDateTimePickers();
    
    // 初始化图表
    initializeCharts();
    
    // 绑定事件监听器
    bindEventListeners();
    
    // 加载默认数据（最近24小时）
    loadDefaultData();
    
    console.log('监控历史系统初始化完成');
}

/**
 * 初始化日期时间选择器
 */
function initializeDateTimePickers() {
    const startTimePicker = flatpickr("#startTime", {
        enableTime: true,
        dateFormat: "Y-m-d H:i",
        locale: "zh",
        maxDate: "today",
        onChange: function(selectedDates, dateStr, instance) {
            // 自动调整结束时间
            const endTimePicker = document.querySelector("#endTime")._flatpickr;
            if (selectedDates[0]) {
                endTimePicker.set('minDate', selectedDates[0]);
            }
        }
    });
    
    const endTimePicker = flatpickr("#endTime", {
        enableTime: true,
        dateFormat: "Y-m-d H:i",
        locale: "zh",
        maxDate: "today"
    });
}

/**
 * 初始化所有图表
 */
function initializeCharts() {
    // CPU使用率图表
    charts.cpu = createAdvancedLineChart('cpuChart', {
        title: 'CPU使用率 (%)',
        color: colors.info,
        yAxisMax: 100,
        thresholds: [
            { value: 80, color: colors.warning, label: '警告阈值' },
            { value: 95, color: colors.danger, label: '危险阈值' }
        ]
    });
    
    // 内存使用率图表
    charts.memory = createAdvancedLineChart('memoryChart', {
        title: '内存使用率 (%)',
        color: colors.success,
        yAxisMax: 100,
        thresholds: [
            { value: 80, color: colors.warning, label: '警告阈值' },
            { value: 95, color: colors.danger, label: '危险阈值' }
        ]
    });
    
    // 磁盘使用率图表
    charts.disk = createAdvancedLineChart('diskChart', {
        title: '磁盘使用率 (%)',
        color: colors.warning,
        yAxisMax: 100,
        thresholds: [
            { value: 85, color: colors.warning, label: '警告阈值' },
            { value: 95, color: colors.danger, label: '危险阈值' }
        ]
    });
    
    // 服务器比较图表
    charts.comparison = createComparisonChart('comparisonChart');
    
    // 实时数据图表
    charts.realtime = createRealtimeChart('realtimeChart');
}

/**
 * 创建高级线性图表
 */
function createAdvancedLineChart(canvasId, config) {
    const ctx = document.getElementById(canvasId).getContext('2d');
    
    const chart = new Chart(ctx, {
        type: 'line',
        data: {
            labels: [],
            datasets: []
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            interaction: {
                mode: 'index',
                intersect: false,
            },
            plugins: {
                title: {
                    display: true,
                    text: config.title,
                    font: {
                        size: 14,
                        weight: 'bold'
                    }
                },
                legend: {
                    display: true,
                    position: 'bottom',
                    labels: {
                        usePointStyle: true,
                        padding: 20
                    }
                },
                tooltip: {
                    backgroundColor: 'rgba(0,0,0,0.8)',
                    titleColor: '#fff',
                    bodyColor: '#fff',
                    borderColor: config.color,
                    borderWidth: 1,
                    cornerRadius: 6,
                    displayColors: true,
                    callbacks: {
                        title: function(tooltipItems) {
                            return '时间: ' + tooltipItems[0].label;
                        },
                        label: function(context) {
                            return context.dataset.label + ': ' + context.parsed.y.toFixed(2) + '%';
                        },
                        afterBody: function(tooltipItems) {
                            const value = tooltipItems[0].parsed.y;
                            if (value > 95) return '🔴 危险水平';
                            if (value > 80) return '🟡 警告水平';
                            return '🟢 正常水平';
                        }
                    }
                },
                zoom: {
                    zoom: {
                        wheel: {
                            enabled: true,
                        },
                        pinch: {
                            enabled: true
                        },
                        mode: 'x',
                        onZoomComplete: function({chart}) {
                            updateChartLegend(chart, canvasId);
                        }
                    },
                    pan: {
                        enabled: true,
                        mode: 'x',
                        onPanComplete: function({chart}) {
                            updateChartLegend(chart, canvasId);
                        }
                    }
                }
            },
            scales: {
                x: {
                    type: 'time',
                    time: {
                        tooltipFormat: 'yyyy-MM-dd HH:mm',
                        displayFormats: {
                            hour: 'HH:mm',
                            day: 'MM-dd'
                        }
                    },
                    title: {
                        display: true,
                        text: '时间'
                    },
                    grid: {
                        color: 'rgba(0,0,0,0.1)'
                    }
                },
                y: {
                    min: 0,
                    max: config.yAxisMax || 100,
                    title: {
                        display: true,
                        text: config.title
                    },
                    grid: {
                        color: 'rgba(0,0,0,0.1)'
                    },
                    ticks: {
                        callback: function(value) {
                            return value + '%';
                        }
                    }
                }
            },
            elements: {
                line: {
                    tension: 0.2,
                    borderWidth: 2
                },
                point: {
                    radius: 3,
                    hoverRadius: 6,
                    borderWidth: 2,
                    hoverBorderWidth: 3
                }
            },
            animation: {
                duration: 750,
                easing: 'easeInOutQuart'
            }
        },
        plugins: [createThresholdPlugin(config.thresholds)]
    });
    
    return chart;
}

/**
 * 创建阈值插件
 */
function createThresholdPlugin(thresholds) {
    if (!thresholds || thresholds.length === 0) return null;
    
    return {
        id: 'thresholdLines',
        afterDatasetsDraw: function(chart) {
            const ctx = chart.ctx;
            const chartArea = chart.chartArea;
            
            thresholds.forEach(threshold => {
                const yPixel = chart.scales.y.getPixelForValue(threshold.value);
                
                ctx.save();
                ctx.strokeStyle = threshold.color;
                ctx.lineWidth = 2;
                ctx.setLineDash([5, 5]);
                ctx.beginPath();
                ctx.moveTo(chartArea.left, yPixel);
                ctx.lineTo(chartArea.right, yPixel);
                ctx.stroke();
                
                // 绘制标签
                ctx.fillStyle = threshold.color;
                ctx.font = '12px Arial';
                ctx.fillText(threshold.label + ' (' + threshold.value + '%)', 
                           chartArea.left + 5, yPixel - 5);
                ctx.restore();
            });
        }
    };
}

/**
 * 创建服务器比较图表
 */
function createComparisonChart(canvasId) {
    const ctx = document.getElementById(canvasId).getContext('2d');
    
    return new Chart(ctx, {
        type: 'line',
        data: {
            labels: [],
            datasets: []
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            interaction: {
                mode: 'index',
                intersect: false,
            },
            plugins: {
                title: {
                    display: true,
                    text: '服务器性能比较',
                    font: {
                        size: 14,
                        weight: 'bold'
                    }
                },
                legend: {
                    display: true,
                    position: 'bottom'
                },
                tooltip: {
                    backgroundColor: 'rgba(0,0,0,0.8)',
                    callbacks: {
                        title: function(tooltipItems) {
                            return '时间: ' + tooltipItems[0].label;
                        },
                        label: function(context) {
                            return context.dataset.label + ': ' + context.parsed.y.toFixed(2) + '%';
                        }
                    }
                }
            },
            scales: {
                x: {
                    type: 'time',
                    time: {
                        tooltipFormat: 'yyyy-MM-dd HH:mm'
                    },
                    title: {
                        display: true,
                        text: '时间'
                    }
                },
                y: {
                    min: 0,
                    max: 100,
                    title: {
                        display: true,
                        text: '使用率 (%)'
                    },
                    ticks: {
                        callback: function(value) {
                            return value + '%';
                        }
                    }
                }
            }
        }
    });
}

/**
 * 创建实时数据图表
 */
function createRealtimeChart(canvasId) {
    const ctx = document.getElementById(canvasId).getContext('2d');
    
    return new Chart(ctx, {
        type: 'line',
        data: {
            labels: [],
            datasets: [{
                label: 'CPU使用率',
                data: [],
                borderColor: colors.info,
                backgroundColor: colors.info + '20',
                fill: true,
                tension: 0.2
            }, {
                label: '内存使用率',
                data: [],
                borderColor: colors.success,
                backgroundColor: colors.success + '20',
                fill: true,
                tension: 0.2
            }, {
                label: '磁盘使用率',
                data: [],
                borderColor: colors.warning,
                backgroundColor: colors.warning + '20',
                fill: true,
                tension: 0.2
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            animation: {
                duration: 0 // 实时图表禁用动画
            },
            interaction: {
                mode: 'index',
                intersect: false,
            },
            plugins: {
                title: {
                    display: true,
                    text: '实时监控数据',
                    font: {
                        size: 14,
                        weight: 'bold'
                    }
                },
                legend: {
                    display: true,
                    position: 'top'
                }
            },
            scales: {
                x: {
                    type: 'time',
                    time: {
                        tooltipFormat: 'HH:mm:ss'
                    },
                    title: {
                        display: true,
                        text: '时间'
                    }
                },
                y: {
                    min: 0,
                    max: 100,
                    title: {
                        display: true,
                        text: '使用率 (%)'
                    }
                }
            }
        }
    });
}

/**
 * 绑定事件监听器
 */
function bindEventListeners() {
    // 快捷时间范围选择
    document.querySelectorAll('input[name="quickRange"]').forEach(radio => {
        radio.addEventListener('change', function() {
            if (this.checked) {
                loadQuickRangeData(this.value);
            }
        });
    });
    
    // 服务器选择变化
    document.getElementById('serverSelect').addEventListener('change', function() {
        const selectedServers = Array.from(this.selectedOptions).map(option => ({
            id: parseInt(option.value),
            name: option.text
        }));
        
        if (selectedServers.length > 1) {
            loadComparisonData(selectedServers);
        } else if (selectedServers.length === 1) {
            loadServerData(selectedServers[0].id);
        }
    });
    
    // 聚合类型变化
    document.getElementById('aggregationType').addEventListener('change', function() {
        reloadCurrentData();
    });
    
    // 实时更新开关
    document.getElementById('realtimeUpdate').addEventListener('change', function() {
        if (this.checked) {
            startRealtimeUpdate();
        } else {
            stopRealtimeUpdate();
        }
    });
    
    // 比较指标选择
    document.getElementById('compareMetric').addEventListener('change', function() {
        updateComparisonChart();
    });
}

/**
 * 加载默认数据（最近24小时）
 */
function loadDefaultData() {
    const serverSelect = document.getElementById('serverSelect');
    if (serverSelect.options.length > 0) {
        serverSelect.selectedIndex = 0;
        const serverId = parseInt(serverSelect.value);
        loadQuickRangeData('24h', serverId);
    }
}

/**
 * 加载快捷时间范围数据
 */
function loadQuickRangeData(range, serverId = null) {
    if (!serverId) {
        const serverSelect = document.getElementById('serverSelect');
        serverId = serverSelect.value ? parseInt(serverSelect.value) : null;
    }
    
    if (!serverId) {
        showNotification('请先选择服务器', 'warning');
        return;
    }
    
    showLoading(true);
    
    fetch(`/monitoring/history/api/server/${serverId}/quick-range?range=${range}`, {
        method: 'GET',
        headers: getHeaders()
    })
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            currentData = data;
            updateCharts(data);
            loadStatistics(serverId, data.timeRange.start, data.timeRange.end);
            loadAnomalies(serverId, data.timeRange.start, data.timeRange.end);
        } else {
            throw new Error(data.error || '加载数据失败');
        }
    })
    .catch(error => {
        console.error('加载快捷范围数据失败:', error);
        showNotification('加载数据失败: ' + error.message, 'danger');
    })
    .finally(() => {
        showLoading(false);
    });
}

/**
 * 加载自定义时间范围数据
 */
function loadCustomRange() {
    const startTime = document.getElementById('startTime').value;
    const endTime = document.getElementById('endTime').value;
    const serverSelect = document.getElementById('serverSelect');
    const serverId = serverSelect.value ? parseInt(serverSelect.value) : null;
    
    if (!serverId) {
        showNotification('请先选择服务器', 'warning');
        return;
    }
    
    if (!startTime || !endTime) {
        showNotification('请选择开始和结束时间', 'warning');
        return;
    }
    
    if (new Date(startTime) >= new Date(endTime)) {
        showNotification('开始时间必须早于结束时间', 'warning');
        return;
    }
    
    const aggregationType = document.getElementById('aggregationType').value;
    const aggregated = aggregationType !== 'none';
    
    showLoading(true);
    
    const params = new URLSearchParams({
        startTime: startTime,
        endTime: endTime,
        aggregated: aggregated,
        aggregationType: aggregationType
    });
    
    fetch(`/monitoring/history/api/server/${serverId}/data?${params}`, {
        method: 'GET',
        headers: getHeaders()
    })
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            currentData = data;
            updateCharts(data);
            loadStatistics(serverId, startTime, endTime);
            loadAnomalies(serverId, startTime, endTime);
        } else {
            throw new Error(data.error || '加载数据失败');
        }
    })
    .catch(error => {
        console.error('加载自定义范围数据失败:', error);
        showNotification('加载数据失败: ' + error.message, 'danger');
    })
    .finally(() => {
        showLoading(false);
    });
}

/**
 * 更新图表数据
 */
function updateCharts(data) {
    const chartData = data.data;
    
    // 更新单服务器图表
    if (data.dataType === 'aggregated') {
        updateAggregatedCharts(chartData);
    } else {
        updateRawDataCharts(chartData);
    }
    
    // 更新图表标题
    const serverInfo = data.serverInfo;
    if (serverInfo) {
        const title = `${serverInfo.name} (${serverInfo.hostname})`;
        charts.cpu.options.plugins.title.text = `CPU使用率 - ${title}`;
        charts.memory.options.plugins.title.text = `内存使用率 - ${title}`;
        charts.disk.options.plugins.title.text = `磁盘使用率 - ${title}`;
        
        charts.cpu.update();
        charts.memory.update();
        charts.disk.update();
    }
}

/**
 * 更新聚合数据图表
 */
function updateAggregatedCharts(chartData) {
    const timestamps = chartData.timestamps;
    
    // CPU图表
    charts.cpu.data.labels = timestamps;
    charts.cpu.data.datasets = [{
        label: 'CPU平均值',
        data: chartData.cpu.avg,
        borderColor: colors.info,
        backgroundColor: colors.info + '20',
        fill: false
    }, {
        label: 'CPU最大值',
        data: chartData.cpu.max,
        borderColor: colors.danger,
        backgroundColor: colors.danger + '20',
        borderDash: [5, 5],
        fill: false
    }, {
        label: 'CPU最小值',
        data: chartData.cpu.min,
        borderColor: colors.success,
        backgroundColor: colors.success + '20',
        borderDash: [2, 2],
        fill: false
    }];
    
    // 内存图表
    charts.memory.data.labels = timestamps;
    charts.memory.data.datasets = [{
        label: '内存平均值',
        data: chartData.memory.avg,
        borderColor: colors.success,
        backgroundColor: colors.success + '20',
        fill: false
    }, {
        label: '内存最大值',
        data: chartData.memory.max,
        borderColor: colors.danger,
        backgroundColor: colors.danger + '20',
        borderDash: [5, 5],
        fill: false
    }, {
        label: '内存最小值',
        data: chartData.memory.min,
        borderColor: colors.info,
        backgroundColor: colors.info + '20',
        borderDash: [2, 2],
        fill: false
    }];
    
    // 磁盘图表
    charts.disk.data.labels = timestamps;
    charts.disk.data.datasets = [{
        label: '磁盘平均值',
        data: chartData.disk.avg,
        borderColor: colors.warning,
        backgroundColor: colors.warning + '20',
        fill: false
    }, {
        label: '磁盘最大值',
        data: chartData.disk.max,
        borderColor: colors.danger,
        backgroundColor: colors.danger + '20',
        borderDash: [5, 5],
        fill: false
    }, {
        label: '磁盘最小值',
        data: chartData.disk.min,
        borderColor: colors.success,
        backgroundColor: colors.success + '20',
        borderDash: [2, 2],
        fill: false
    }];
    
    charts.cpu.update();
    charts.memory.update();
    charts.disk.update();
}

/**
 * 更新原始数据图表
 */
function updateRawDataCharts(chartData) {
    const timestamps = chartData.timestamps;
    
    // CPU图表
    charts.cpu.data.labels = timestamps;
    charts.cpu.data.datasets = [{
        label: 'CPU使用率',
        data: chartData.cpu,
        borderColor: colors.info,
        backgroundColor: colors.info + '20',
        fill: true
    }];
    
    // 内存图表
    charts.memory.data.labels = timestamps;
    charts.memory.data.datasets = [{
        label: '内存使用率',
        data: chartData.memory,
        borderColor: colors.success,
        backgroundColor: colors.success + '20',
        fill: true
    }];
    
    // 磁盘图表
    charts.disk.data.labels = timestamps;
    charts.disk.data.datasets = [{
        label: '磁盘使用率',
        data: chartData.disk,
        borderColor: colors.warning,
        backgroundColor: colors.warning + '20',
        fill: true
    }];
    
    charts.cpu.update();
    charts.memory.update();
    charts.disk.update();
}

/**
 * 加载服务器比较数据
 */
function loadComparisonData(servers) {
    const serverIds = servers.map(s => s.id);
    const startTime = document.getElementById('startTime').value;
    const endTime = document.getElementById('endTime').value;
    const aggregationType = document.getElementById('aggregationType').value;
    
    if (!startTime || !endTime) {
        // 使用默认时间范围（最近24小时）
        const end = new Date();
        const start = new Date(end.getTime() - 24 * 60 * 60 * 1000);
        startTime = start.toISOString().slice(0, 16);
        endTime = end.toISOString().slice(0, 16);
    }
    
    const params = new URLSearchParams({
        serverIds: serverIds.join(','),
        startTime: startTime,
        endTime: endTime,
        aggregationType: aggregationType
    });
    
    showLoading(true);
    
    fetch(`/monitoring/history/api/servers/compare?${params}`, {
        method: 'GET',
        headers: getHeaders()
    })
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            updateComparisonChart(data);
        } else {
            throw new Error(data.error || '加载比较数据失败');
        }
    })
    .catch(error => {
        console.error('加载比较数据失败:', error);
        showNotification('加载比较数据失败: ' + error.message, 'danger');
    })
    .finally(() => {
        showLoading(false);
    });
}

/**
 * 更新比较图表
 */
function updateComparisonChart(data = null) {
    if (!data && !currentData.serversData) return;
    
    const serversData = data ? data.serversData : currentData.serversData;
    const metric = document.getElementById('compareMetric').value;
    
    const datasets = [];
    const serverColors = [colors.info, colors.success, colors.warning, colors.danger, colors.primary, colors.secondary];
    let colorIndex = 0;
    
    for (const [serverName, serverData] of Object.entries(serversData)) {
        const color = serverColors[colorIndex % serverColors.length];
        colorIndex++;
        
        let metricData;
        if (serverData[metric] && serverData[metric].avg) {
            // 聚合数据
            metricData = serverData[metric].avg;
        } else {
            // 原始数据
            metricData = serverData[metric];
        }
        
        datasets.push({
            label: serverName,
            data: metricData,
            borderColor: color,
            backgroundColor: color + '20',
            fill: false,
            tension: 0.2
        });
    }
    
    // 使用第一个服务器的时间戳
    const firstServerData = Object.values(serversData)[0];
    charts.comparison.data.labels = firstServerData.timestamps;
    charts.comparison.data.datasets = datasets;
    
    // 更新图表标题
    const metricNames = {
        cpu: 'CPU使用率比较',
        memory: '内存使用率比较',
        disk: '磁盘使用率比较'
    };
    charts.comparison.options.plugins.title.text = metricNames[metric];
    
    charts.comparison.update();
}

/**
 * 加载统计信息
 */
function loadStatistics(serverId, startTime, endTime) {
    const params = new URLSearchParams({
        startTime: startTime,
        endTime: endTime
    });
    
    fetch(`/monitoring/history/api/server/${serverId}/statistics?${params}`, {
        method: 'GET',
        headers: getHeaders()
    })
    .then(response => response.json())
    .then(data => {
        if (data.success || data.dataCount !== undefined) {
            displayStatistics(data);
        } else {
            throw new Error(data.error || '加载统计信息失败');
        }
    })
    .catch(error => {
        console.error('加载统计信息失败:', error);
        document.getElementById('statisticsContent').innerHTML = `
            <div class="alert alert-danger">
                <i class="fas fa-exclamation-triangle"></i> 
                加载统计信息失败: ${error.message}
            </div>`;
    });
}

/**
 * 显示统计信息
 */
function displayStatistics(data) {
    if (data.dataCount === 0) {
        document.getElementById('statisticsContent').innerHTML = `
            <div class="alert alert-info">
                <i class="fas fa-info-circle"></i> 
                指定时间范围内暂无监控数据
            </div>`;
        return;
    }
    
    let html = `
        <div class="row mb-3">
            <div class="col-md-3">
                <div class="card bg-light">
                    <div class="card-body text-center">
                        <h5>${data.dataCount}</h5>
                        <small class="text-muted">数据点数量</small>
                    </div>
                </div>
            </div>
            <div class="col-md-3">
                <div class="card bg-light">
                    <div class="card-body text-center">
                        <h5>${data.timeSpan || '-'}</h5>
                        <small class="text-muted">时间跨度</small>
                    </div>
                </div>
            </div>
        </div>
        
        <div class="row">`;
    
    if (data.cpu) {
        html += `
            <div class="col-md-4 mb-3">
                <div class="card">
                    <div class="card-header">
                        <i class="fas fa-microchip text-info"></i> CPU统计
                    </div>
                    <div class="card-body">
                        <div class="row text-center">
                            <div class="col-6">
                                <h6 class="text-info">${data.cpu.avg.toFixed(2)}%</h6>
                                <small class="text-muted">平均值</small>
                            </div>
                            <div class="col-6">
                                <h6 class="text-danger">${data.cpu.max.toFixed(2)}%</h6>
                                <small class="text-muted">最大值</small>
                            </div>
                        </div>
                        <div class="row text-center mt-2">
                            <div class="col-6">
                                <h6 class="text-success">${data.cpu.min.toFixed(2)}%</h6>
                                <small class="text-muted">最小值</small>
                            </div>
                            <div class="col-6">
                                <h6 class="text-primary">${data.cpu.latest.toFixed(2)}%</h6>
                                <small class="text-muted">当前值</small>
                            </div>
                        </div>
                    </div>
                </div>
            </div>`;
    }
    
    if (data.memory) {
        html += `
            <div class="col-md-4 mb-3">
                <div class="card">
                    <div class="card-header">
                        <i class="fas fa-memory text-success"></i> 内存统计
                    </div>
                    <div class="card-body">
                        <div class="row text-center">
                            <div class="col-6">
                                <h6 class="text-info">${data.memory.avg.toFixed(2)}%</h6>
                                <small class="text-muted">平均值</small>
                            </div>
                            <div class="col-6">
                                <h6 class="text-danger">${data.memory.max.toFixed(2)}%</h6>
                                <small class="text-muted">最大值</small>
                            </div>
                        </div>
                        <div class="row text-center mt-2">
                            <div class="col-6">
                                <h6 class="text-success">${data.memory.min.toFixed(2)}%</h6>
                                <small class="text-muted">最小值</small>
                            </div>
                            <div class="col-6">
                                <h6 class="text-primary">${data.memory.latest.toFixed(2)}%</h6>
                                <small class="text-muted">当前值</small>
                            </div>
                        </div>
                    </div>
                </div>
            </div>`;
    }
    
    if (data.disk) {
        html += `
            <div class="col-md-4 mb-3">
                <div class="card">
                    <div class="card-header">
                        <i class="fas fa-hdd text-warning"></i> 磁盘统计
                    </div>
                    <div class="card-body">
                        <div class="row text-center">
                            <div class="col-6">
                                <h6 class="text-info">${data.disk.avg.toFixed(2)}%</h6>
                                <small class="text-muted">平均值</small>
                            </div>
                            <div class="col-6">
                                <h6 class="text-danger">${data.disk.max.toFixed(2)}%</h6>
                                <small class="text-muted">最大值</small>
                            </div>
                        </div>
                        <div class="row text-center mt-2">
                            <div class="col-6">
                                <h6 class="text-success">${data.disk.min.toFixed(2)}%</h6>
                                <small class="text-muted">最小值</small>
                            </div>
                            <div class="col-6">
                                <h6 class="text-primary">${data.disk.latest.toFixed(2)}%</h6>
                                <small class="text-muted">当前值</small>
                            </div>
                        </div>
                    </div>
                </div>
            </div>`;
    }
    
    html += '</div>';
    
    document.getElementById('statisticsContent').innerHTML = html;
}

/**
 * 加载异常事件
 */
function loadAnomalies(serverId, startTime, endTime) {
    const cpuThreshold = document.getElementById('cpuThreshold').value;
    const memoryThreshold = document.getElementById('memoryThreshold').value;
    
    const params = new URLSearchParams({
        startTime: startTime,
        endTime: endTime,
        cpuThreshold: cpuThreshold,
        memoryThreshold: memoryThreshold
    });
    
    fetch(`/monitoring/history/api/server/${serverId}/anomalies?${params}`, {
        method: 'GET',
        headers: getHeaders()
    })
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            displayAnomalies(data);
        } else {
            throw new Error(data.error || '加载异常事件失败');
        }
    })
    .catch(error => {
        console.error('加载异常事件失败:', error);
        document.getElementById('anomaliesContent').innerHTML = `
            <div class="alert alert-danger">
                <i class="fas fa-exclamation-triangle"></i> 
                加载异常事件失败: ${error.message}
            </div>`;
    });
}

/**
 * 显示异常事件
 */
function displayAnomalies(data) {
    if (!data.anomalies || data.anomalies.length === 0) {
        document.getElementById('anomaliesContent').innerHTML = `
            <div class="alert alert-success">
                <i class="fas fa-check-circle"></i> 
                在指定时间范围和阈值内未发现异常事件
            </div>`;
        return;
    }
    
    let html = `
        <div class="row mb-3">
            <div class="col-md-4">
                <div class="card bg-danger text-white">
                    <div class="card-body text-center">
                        <h5>${data.totalCount}</h5>
                        <small>总异常事件</small>
                    </div>
                </div>
            </div>
            <div class="col-md-4">
                <div class="card bg-info text-white">
                    <div class="card-body text-center">
                        <h5>${data.cpuAnomalies}</h5>
                        <small>CPU异常</small>
                    </div>
                </div>
            </div>
            <div class="col-md-4">
                <div class="card bg-success text-white">
                    <div class="card-body text-center">
                        <h5>${data.memoryAnomalies}</h5>
                        <small>内存异常</small>
                    </div>
                </div>
            </div>
        </div>
        
        <div class="table-responsive">
            <table class="table table-hover">
                <thead>
                    <tr>
                        <th>时间</th>
                        <th>类型</th>
                        <th>值</th>
                        <th>阈值</th>
                        <th>严重程度</th>
                        <th>描述</th>
                    </tr>
                </thead>
                <tbody>`;
    
    data.anomalies.forEach(anomaly => {
        const severityClass = anomaly.severity === 'CRITICAL' ? 'danger' : 'warning';
        const typeIcon = anomaly.type === 'HIGH_CPU' ? 'microchip' : 'memory';
        
        html += `
            <tr>
                <td>${new Date(anomaly.timestamp).toLocaleString('zh-CN')}</td>
                <td>
                    <i class="fas fa-${typeIcon}"></i>
                    ${anomaly.type === 'HIGH_CPU' ? 'CPU异常' : '内存异常'}
                </td>
                <td><strong>${anomaly.value.toFixed(2)}%</strong></td>
                <td>${anomaly.threshold}%</td>
                <td>
                    <span class="badge bg-${severityClass}">
                        ${anomaly.severity === 'CRITICAL' ? '危险' : '警告'}
                    </span>
                </td>
                <td>${anomaly.message}</td>
            </tr>`;
    });
    
    html += `
                </tbody>
            </table>
        </div>`;
    
    document.getElementById('anomaliesContent').innerHTML = html;
}

/**
 * 工具函数
 */

// 获取请求头
function getHeaders() {
    const headers = {
        'Content-Type': 'application/json'
    };
    
    const csrfToken = document.querySelector('meta[name="_csrf"]');
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]');
    
    if (csrfToken && csrfHeader) {
        headers[csrfHeader.getAttribute('content')] = csrfToken.getAttribute('content');
    }
    
    return headers;
}

// 显示/隐藏加载遮罩
function showLoading(show) {
    const overlay = document.getElementById('loadingOverlay');
    if (overlay) {
        overlay.style.display = show ? 'flex' : 'none';
    }
}

// 显示通知消息
function showNotification(message, type = 'info') {
    // 移除现有通知
    const existingAlert = document.querySelector('.alert-notification');
    if (existingAlert) {
        existingAlert.remove();
    }
    
    // 创建新通知
    const alert = document.createElement('div');
    alert.className = `alert alert-${type} alert-dismissible fade show alert-notification`;
    alert.style.position = 'fixed';
    alert.style.top = '20px';
    alert.style.right = '20px';
    alert.style.zIndex = '9999';
    alert.style.minWidth = '300px';
    alert.innerHTML = `
        ${message}
        <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
    `;
    
    document.body.appendChild(alert);
    
    // 5秒后自动隐藏
    setTimeout(() => {
        if (alert.parentNode) {
            alert.remove();
        }
    }, 5000);
}

// 切换图表类型
function toggleChartType(chartId) {
    const chart = charts[chartId.replace('Chart', '')];
    if (!chart) return;
    
    const currentType = chart.config.type;
    const newType = currentType === 'line' ? 'bar' : 'line';
    
    chart.config.type = newType;
    chart.update();
}

// 重置图表缩放
function resetZoom(chartId) {
    const chart = charts[chartId.replace('Chart', '')];
    if (chart && chart.resetZoom) {
        chart.resetZoom();
    }
}

// 导出当前数据
function exportCurrentData() {
    const serverSelect = document.getElementById('serverSelect');
    const serverId = serverSelect.value;
    
    if (!serverId) {
        showNotification('请先选择服务器', 'warning');
        return;
    }
    
    const startTime = document.getElementById('startTime').value;
    const endTime = document.getElementById('endTime').value;
    
    if (!startTime || !endTime) {
        showNotification('请设置时间范围', 'warning');
        return;
    }
    
    const params = new URLSearchParams({
        startTime: startTime,
        endTime: endTime
    });
    
    window.location.href = `/monitoring/history/api/server/${serverId}/export?${params}`;
    showNotification('数据导出已开始', 'success');
}

// 显示统计信息
function showStatistics() {
    const statisticsTab = document.querySelector('a[href="#statisticsTab"]');
    if (statisticsTab) {
        statisticsTab.click();
    }
}

// 显示异常事件
function showAnomalies() {
    const anomaliesTab = document.querySelector('a[href="#anomaliesTab"]');
    if (anomaliesTab) {
        anomaliesTab.click();
    }
}

// 重新加载当前数据
function reloadCurrentData() {
    const serverSelect = document.getElementById('serverSelect');
    const serverId = serverSelect.value;
    
    if (!serverId) return;
    
    const quickRange = document.querySelector('input[name="quickRange"]:checked');
    if (quickRange) {
        loadQuickRangeData(quickRange.value, parseInt(serverId));
    } else {
        loadCustomRange();
    }
}

// 启动实时更新
function startRealtimeUpdate() {
    const frequency = parseInt(document.getElementById('updateFrequency').value) * 1000;
    
    realtimeInterval = setInterval(() => {
        updateRealtimeData();
    }, frequency);
    
    // 立即更新一次
    updateRealtimeData();
}

// 停止实时更新
function stopRealtimeUpdate() {
    if (realtimeInterval) {
        clearInterval(realtimeInterval);
        realtimeInterval = null;
    }
}

// 更新实时数据
function updateRealtimeData() {
    const serverSelect = document.getElementById('serverSelect');
    const serverId = serverSelect.value;
    
    if (!serverId) return;
    
    const minutes = parseInt(document.getElementById('realtimeRange').value);
    
    fetch(`/monitoring/history/api/server/${serverId}/realtime?minutes=${minutes}`, {
        method: 'GET',
        headers: getHeaders()
    })
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            updateRealtimeChart(data.data);
        }
    })
    .catch(error => {
        console.error('更新实时数据失败:', error);
    });
}

// 更新实时图表
function updateRealtimeChart(chartData) {
    if (!charts.realtime) return;
    
    charts.realtime.data.labels = chartData.timestamps.map(ts => new Date(ts));
    charts.realtime.data.datasets[0].data = chartData.cpu;
    charts.realtime.data.datasets[1].data = chartData.memory;
    charts.realtime.data.datasets[2].data = chartData.disk;
    
    charts.realtime.update('none'); // 无动画更新
}

// 更新图表图例
function updateChartLegend(chart, canvasId) {
    // 这里可以添加自定义图例更新逻辑
    console.log('图表缩放/平移事件:', canvasId);
}