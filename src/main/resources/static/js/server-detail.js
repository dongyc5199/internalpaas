// 服务器详情页面的JavaScript功能

let serverId;
let isExecuting = false;

// 页面加载后从data属性获取服务器ID
document.addEventListener('DOMContentLoaded', function() {
    const serverDataElement = document.getElementById('serverData');
    if (serverDataElement) {
        serverId = serverDataElement.getAttribute('data-id');
    }
});

function setCommand(cmd) {
    document.getElementById('commandInput').value = cmd;
    document.getElementById('commandInput').focus();
}

function handleKeyPress(event) {
    if (event.key === 'Enter' && !isExecuting) {
        executeCommand();
    }
}

async function executeCommand() {
    if (isExecuting) return;

    const command = document.getElementById('commandInput').value.trim();
    if (!command) {
        alert('请输入要执行的命令');
        return;
    }

    isExecuting = true;
    const executeBtn = document.getElementById('executeBtn');
    const executeBtnText = document.getElementById('executeBtnText');
    const loadingSpinner = document.getElementById('loadingSpinner');
    const commandOutput = document.getElementById('commandOutput');
    const outputContent = document.getElementById('outputContent');
    const outputMeta = document.getElementById('outputMeta');

    executeBtn.disabled = true;
    executeBtnText.style.display = 'none';
    loadingSpinner.style.display = 'inline-block';
    commandOutput.style.display = 'none';

    try {
        const response = await fetch(`/api/remote-command/execute/${serverId}`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({ command: command })
        });

        const result = await response.json();
        
        commandOutput.style.display = 'block';
        outputContent.textContent = result.output || result.error || '无输出';
        outputMeta.textContent = `执行时间: ${result.executionTime}ms | 退出码: ${result.exitCode}`;
        
        outputContent.className = `command-output ${result.exitCode === 0 ? 'success' : 'error'}`;
        
    } catch (error) {
        commandOutput.style.display = 'block';
        outputContent.textContent = `执行失败: ${error.message}`;
        outputMeta.textContent = '错误';
        outputContent.className = 'command-output error';
    } finally {
        isExecuting = false;
        executeBtn.disabled = false;
        executeBtnText.style.display = 'inline';
        loadingSpinner.style.display = 'none';
    }
}

async function checkConnection() {
    try {
        const response = await fetch(`/api/remote-command/check-connection/${serverId}`);
        const result = await response.json();
        
        if (result.success) {
            showNotification('SSH连接正常', 'success');
            location.reload();
        } else {
            showNotification('SSH连接失败: ' + result.error, 'error');
        }
    } catch (error) {
        showNotification('检查连接失败: ' + error.message, 'error');
    }
}

function deleteServer(element) {
    const id = element.getAttribute('data-id');
    const name = element.getAttribute('data-name');
    
    if (confirm('确定要删除服务器 "' + name + '" 吗？此操作不可恢复。')) {
        // 发送删除请求
        fetch(`/admin/servers/${id}/delete`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'X-CSRF-TOKEN': document.querySelector('meta[name="_csrf"]').getAttribute('content')
            }
        })
        .then(response => {
            if (response.ok) {
                showNotification('服务器已成功删除', 'success');
                // 重定向到服务器列表页面
                setTimeout(() => {
                    window.location.href = '/admin/servers';
                }, 1500);
            } else {
                showNotification('删除服务器失败', 'error');
            }
        })
        .catch(error => {
            showNotification('删除服务器时发生错误: ' + error.message, 'error');
        });
    }
}

// 显示通知的函数
function showNotification(message, type = 'info') {
    // 创建通知元素
    const notification = document.createElement('div');
    notification.className = `notification notification-${type}-color`;
    notification.textContent = message;
    
    // 添加到页面
    document.body.appendChild(notification);
    
    // 显示通知
    setTimeout(() => {
        notification.classList.add('show');
    }, 10);
    
    // 3秒后隐藏并移除
    setTimeout(() => {
        notification.classList.remove('show');
        setTimeout(() => {
            document.body.removeChild(notification);
        }, 300);
    }, 3000);
}