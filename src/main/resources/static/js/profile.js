/**
 * 用户档案页面JavaScript文件
 * 处理表单验证、AJAX提交、主题切换等功能
 */

document.addEventListener('DOMContentLoaded', function() {
    initializeProfilePage();
});

/**
 * 初始化档案页面
 */
function initializeProfilePage() {
    initializeFormValidation();
    initializePreferencesForm();
    initializePasswordValidation();
    initializeThemeHandling();
}

/**
 * 初始化表单验证
 */
function initializeFormValidation() {
    // 个人信息表单验证
    const profileForm = document.querySelector('.profile-form');
    if (profileForm) {
        profileForm.addEventListener('submit', function(event) {
            if (!validateProfileForm()) {
                event.preventDefault();
                event.stopPropagation();
            }
            this.classList.add('was-validated');
        });
    }
}

/**
 * 验证个人信息表单
 */
function validateProfileForm() {
    let isValid = true;
    
    // 验证邮箱
    const emailField = document.getElementById('email');
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (emailField && !emailRegex.test(emailField.value)) {
        showFieldError(emailField, '请输入有效的邮箱地址');
        isValid = false;
    } else {
        clearFieldError(emailField);
    }
    
    // 验证工作目录
    const workDirField = document.getElementById('workDirectory');
    if (workDirField && workDirField.value.trim() === '') {
        showFieldError(workDirField, '工作目录不能为空');
        isValid = false;
    } else {
        clearFieldError(workDirField);
    }
    
    return isValid;
}

/**
 * 初始化密码修改验证
 */
function initializePasswordValidation() {
    const passwordForm = document.querySelector('.password-form');
    if (passwordForm) {
        const newPasswordField = document.getElementById('newPassword');
        const confirmPasswordField = document.getElementById('confirmNewPassword');
        
        // 实时验证密码匹配
        [newPasswordField, confirmPasswordField].forEach(field => {
            if (field) {
                field.addEventListener('input', validatePasswordMatch);
            }
        });
        
        passwordForm.addEventListener('submit', function(event) {
            if (!validatePasswordForm()) {
                event.preventDefault();
                event.stopPropagation();
            }
            this.classList.add('was-validated');
        });
    }
}

/**
 * 验证密码匹配
 */
function validatePasswordMatch() {
    const newPassword = document.getElementById('newPassword');
    const confirmPassword = document.getElementById('confirmNewPassword');
    
    if (newPassword && confirmPassword && newPassword.value && confirmPassword.value) {
        if (newPassword.value !== confirmPassword.value) {
            showFieldError(confirmPassword, '两次输入的密码不一致');
            return false;
        } else {
            clearFieldError(confirmPassword);
            return true;
        }
    }
    return true;
}

/**
 * 验证密码表单
 */
function validatePasswordForm() {
    let isValid = true;
    
    // 验证当前密码
    const currentPassword = document.getElementById('currentPassword');
    if (currentPassword && currentPassword.value.trim() === '') {
        showFieldError(currentPassword, '请输入当前密码');
        isValid = false;
    } else {
        clearFieldError(currentPassword);
    }
    
    // 验证新密码长度
    const newPassword = document.getElementById('newPassword');
    if (newPassword && newPassword.value.length < 6) {
        showFieldError(newPassword, '新密码长度至少6位');
        isValid = false;
    } else {
        clearFieldError(newPassword);
    }
    
    // 验证密码匹配
    if (!validatePasswordMatch()) {
        isValid = false;
    }
    
    return isValid;
}

/**
 * 初始化偏好设置表单
 */
function initializePreferencesForm() {
    const preferencesForm = document.getElementById('preferencesForm');
    if (preferencesForm) {
        // 监听主题变更
        const themeRadios = preferencesForm.querySelectorAll('input[name="theme"]');
        themeRadios.forEach(radio => {
            radio.addEventListener('change', function() {
                applyThemePreview(this.value);
            });
        });
        
        // 异步提交偏好设置
        preferencesForm.addEventListener('submit', function(event) {
            event.preventDefault();
            submitPreferences();
        });
    }
}

/**
 * 提交偏好设置
 */
function submitPreferences() {
    const form = document.getElementById('preferencesForm');
    const formData = new FormData(form);
    const submitBtn = document.getElementById('savePreferences');
    
    // 获取CSRF令牌
    const csrfToken = document.querySelector('meta[name="_csrf"]').content;
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]').content;
    
    // 禁用提交按钮
    submitBtn.disabled = true;
    submitBtn.innerHTML = '<span class="btn-icon">⏳</span>保存中...';
    
    fetch('/profile/preferences', {
        method: 'POST',
        headers: {
            [csrfHeader]: csrfToken
        },
        body: formData
    })
    .then(response => {
        if (response.ok) {
            return response.text();
        } else {
            throw new Error('保存失败');
        }
    })
    .then(message => {
        showNotification('success', '偏好设置保存成功！');
        // 应用设置
        applyPreferences();
    })
    .catch(error => {
        console.error('Error:', error);
        showNotification('error', '保存失败：' + error.message);
    })
    .finally(() => {
        // 恢复提交按钮
        submitBtn.disabled = false;
        submitBtn.innerHTML = '<span class="btn-icon">💾</span>保存偏好设置';
    });
}

/**
 * 应用偏好设置
 */
function applyPreferences() {
    const form = document.getElementById('preferencesForm');
    if (!form) return;
    
    // 应用主题设置
    const selectedTheme = form.querySelector('input[name="theme"]:checked');
    if (selectedTheme) {
        applyTheme(selectedTheme.value);
    }
    
    // 应用终端设置（如果有终端页面）
    const terminalTheme = form.querySelector('#terminalTheme').value;
    const terminalFontSize = form.querySelector('#terminalFontSize').value;
    const terminalFontFamily = form.querySelector('#terminalFontFamily').value;
    
    // 保存到localStorage供其他页面使用
    localStorage.setItem('terminalTheme', terminalTheme);
    localStorage.setItem('terminalFontSize', terminalFontSize);
    localStorage.setItem('terminalFontFamily', terminalFontFamily);
}

/**
 * 预览主题效果
 */
function applyThemePreview(theme) {
    const html = document.documentElement;
    const currentTheme = html.className.match(/theme-\w+/);
    
    if (currentTheme) {
        html.classList.remove(currentTheme[0]);
    }
    
    html.classList.add('theme-' + theme);
    
    // 更新主题图标
    const themeIcon = document.querySelector('.theme-icon');
    if (themeIcon) {
        switch(theme) {
            case 'light':
                themeIcon.textContent = '☀️';
                break;
            case 'dark':
                themeIcon.textContent = '🌙';
                break;
            case 'auto':
                themeIcon.textContent = '🔄';
                break;
        }
    }
}

/**
 * 应用主题
 */
function applyTheme(theme) {
    applyThemePreview(theme);
    // 保存主题设置
    localStorage.setItem('theme', theme);
    
    // 如果是自动主题，根据系统主题设置
    if (theme === 'auto') {
        const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
        applyThemePreview(prefersDark ? 'dark' : 'light');
    }
}

/**
 * 初始化主题处理
 */
function initializeThemeHandling() {
    // 监听系统主题变化（仅当设置为auto时生效）
    const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)');
    mediaQuery.addEventListener('change', function(e) {
        const currentTheme = localStorage.getItem('theme');
        if (currentTheme === 'auto') {
            applyThemePreview(e.matches ? 'dark' : 'light');
        }
    });
}

/**
 * 重置偏好设置到默认值
 */
function resetPreferences() {
    if (confirm('确定要恢复默认设置吗？这将重置所有偏好设置。')) {
        const form = document.getElementById('preferencesForm');
        
        // 重置表单到默认值
        form.querySelectorAll('input[type="radio"]').forEach(radio => {
            if (radio.value === getDefaultValue(radio.name)) {
                radio.checked = true;
            }
        });
        
        form.querySelectorAll('input[type="checkbox"]').forEach(checkbox => {
            checkbox.checked = getDefaultCheckboxValue(checkbox.name);
        });
        
        form.querySelectorAll('select').forEach(select => {
            select.value = getDefaultSelectValue(select.name);
        });
        
        // 应用预览
        applyThemePreview('light');
        
        showNotification('info', '已重置为默认设置，请保存以应用更改');
    }
}

/**
 * 获取默认值
 */
function getDefaultValue(fieldName) {
    const defaults = {
        'theme': 'light'
    };
    return defaults[fieldName] || '';
}

function getDefaultCheckboxValue(fieldName) {
    const defaults = {
        'emailNotifications': true,
        'systemNotifications': true,
        'applicationStatusNotifications': true,
        'securityNotifications': true,
        'showWelcomeMessage': true,
        'showQuickActions': true,
        'showRecentActivity': true
    };
    return defaults[fieldName] || false;
}

function getDefaultSelectValue(fieldName) {
    const defaults = {
        'dashboardLayout': 'default',
        'terminalTheme': 'dark',
        'terminalFontSize': '14',
        'terminalFontFamily': 'Monaco',
        'language': 'zh_CN',
        'timeZone': 'Asia/Shanghai'
    };
    return defaults[fieldName] || '';
}

/**
 * 显示字段错误
 */
function showFieldError(field, message) {
    if (!field) return;
    
    field.classList.add('is-invalid');
    
    // 查找或创建错误消息元素
    let errorElement = field.nextElementSibling;
    if (!errorElement || !errorElement.classList.contains('invalid-feedback')) {
        errorElement = document.createElement('div');
        errorElement.classList.add('invalid-feedback');
        field.parentNode.appendChild(errorElement);
    }
    
    errorElement.textContent = message;
}

/**
 * 清除字段错误
 */
function clearFieldError(field) {
    if (!field) return;
    
    field.classList.remove('is-invalid');
    
    const errorElement = field.nextElementSibling;
    if (errorElement && errorElement.classList.contains('invalid-feedback')) {
        errorElement.remove();
    }
}

/**
 * 显示通知消息
 */
function showNotification(type, message, duration = 5000) {
    // 移除现有通知
    const existingNotifications = document.querySelectorAll('.profile-notification');
    existingNotifications.forEach(notification => notification.remove());
    
    // 创建通知元素
    const notification = document.createElement('div');
    notification.className = `alert alert-${type === 'success' ? 'success' : type === 'error' ? 'danger' : 'info'} alert-dismissible fade show profile-notification`;
    notification.style.cssText = `
        position: fixed;
        top: 80px;
        right: 20px;
        z-index: 1050;
        max-width: 400px;
        animation: slideInRight 0.3s ease-out;
    `;
    
    notification.innerHTML = `
        <span>${message}</span>
        <button type="button" class="btn-close" onclick="this.parentElement.remove()"></button>
    `;
    
    document.body.appendChild(notification);
    
    // 自动移除
    setTimeout(() => {
        if (notification.parentNode) {
            notification.remove();
        }
    }, duration);
}

/**
 * 格式化数字显示
 */
function formatNumber(num) {
    if (num >= 1000000) {
        return (num / 1000000).toFixed(1) + 'M';
    } else if (num >= 1000) {
        return (num / 1000).toFixed(1) + 'K';
    }
    return num.toString();
}

/**
 * 更新统计数字动画
 */
function animateStatNumbers() {
    const statNumbers = document.querySelectorAll('.stat-number');
    
    statNumbers.forEach(element => {
        const finalValue = parseInt(element.textContent);
        if (isNaN(finalValue)) return;
        
        let currentValue = 0;
        const increment = finalValue / 30; // 30帧动画
        const timer = setInterval(() => {
            currentValue += increment;
            if (currentValue >= finalValue) {
                element.textContent = formatNumber(finalValue);
                clearInterval(timer);
            } else {
                element.textContent = formatNumber(Math.floor(currentValue));
            }
        }, 50);
    });
}

// 页面加载完成后执行数字动画
document.addEventListener('DOMContentLoaded', function() {
    setTimeout(animateStatNumbers, 500);
});

// CSS动画样式
const style = document.createElement('style');
style.textContent = `
@keyframes slideInRight {
    from {
        opacity: 0;
        transform: translateX(100%);
    }
    to {
        opacity: 1;
        transform: translateX(0);
    }
}

.profile-notification {
    animation: slideInRight 0.3s ease-out;
}
`;
document.head.appendChild(style);