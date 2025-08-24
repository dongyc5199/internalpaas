/**
 * 用户体验增强 JavaScript 库
 * 包含：通知系统、加载状态、操作反馈、表单验证等功能
 */

class UXEnhancement {
    constructor() {
        this.notificationContainer = null;
        this.loadingOverlay = null;
        this.operationFeedback = null;
        this.confirmDialog = null;
        this.init();
    }

    /**
     * 初始化UX增强组件
     */
    init() {
        this.createNotificationContainer();
        this.createLoadingOverlay();
        this.createOperationFeedback();
        this.createConfirmDialog();
        this.enhanceExistingElements();
        this.setupGlobalErrorHandling();
        console.log('UX Enhancement initialized');
    }

    /**
     * 创建通知容器
     */
    createNotificationContainer() {
        this.notificationContainer = document.createElement('div');
        this.notificationContainer.className = 'notification-container';
        document.body.appendChild(this.notificationContainer);
    }

    /**
     * 创建加载遮罩层
     */
    createLoadingOverlay() {
        this.loadingOverlay = document.createElement('div');
        this.loadingOverlay.className = 'loading-overlay';
        this.loadingOverlay.innerHTML = `
            <div class="loading-content">
                <div class="loading-spinner"></div>
                <div class="loading-text">处理中，请稍候...</div>
            </div>
        `;
        document.body.appendChild(this.loadingOverlay);
    }

    /**
     * 创建操作反馈
     */
    createOperationFeedback() {
        this.operationFeedback = document.createElement('div');
        this.operationFeedback.className = 'operation-feedback';
        document.body.appendChild(this.operationFeedback);
    }

    /**
     * 创建确认对话框
     */
    createConfirmDialog() {
        this.confirmDialog = document.createElement('div');
        this.confirmDialog.className = 'confirm-dialog';
        this.confirmDialog.innerHTML = `
            <div class="confirm-content">
                <div class="confirm-icon">⚠️</div>
                <div class="confirm-title">确认操作</div>
                <div class="confirm-message">您确定要执行此操作吗？</div>
                <div class="confirm-actions">
                    <button class="btn-secondary" onclick="ux.hideConfirm()">取消</button>
                    <button class="btn-primary confirm-ok">确定</button>
                </div>
            </div>
        `;
        document.body.appendChild(this.confirmDialog);

        // 点击遮罩层关闭
        this.confirmDialog.addEventListener('click', (e) => {
            if (e.target === this.confirmDialog) {
                this.hideConfirm();
            }
        });
    }

    /**
     * 显示通知
     * @param {string} message - 通知消息
     * @param {string} type - 通知类型：success, error, warning, info
     * @param {number} duration - 显示时长（毫秒）
     * @param {string} title - 通知标题
     */
    showNotification(message, type = 'info', duration = 5000, title = null) {
        const notification = document.createElement('div');
        notification.className = `notification ${type}`;

        const icons = {
            success: '✅',
            error: '❌',
            warning: '⚠️',
            info: 'ℹ️'
        };

        const titles = {
            success: '操作成功',
            error: '操作失败',
            warning: '警告',
            info: '提示'
        };

        notification.innerHTML = `
            <div class="notification-icon">${icons[type] || icons.info}</div>
            <div class="notification-content">
                ${title ? `<div class="notification-title">${title}</div>` : `<div class="notification-title">${titles[type] || titles.info}</div>`}
                <div class="notification-message">${message}</div>
            </div>
            <button class="notification-close" onclick="this.parentElement.remove()">×</button>
        `;

        this.notificationContainer.appendChild(notification);

        // 自动移除
        if (duration > 0) {
            setTimeout(() => {
                if (notification.parentElement) {
                    notification.style.animation = 'slideOutRight 0.3s ease-out';
                    setTimeout(() => notification.remove(), 300);
                }
            }, duration);
        }

        return notification;
    }

    /**
     * 显示加载状态
     * @param {string} text - 加载文本
     */
    showLoading(text = '处理中，请稍候...') {
        this.loadingOverlay.querySelector('.loading-text').textContent = text;
        this.loadingOverlay.classList.add('active');
    }

    /**
     * 隐藏加载状态
     */
    hideLoading() {
        this.loadingOverlay.classList.remove('active');
    }

    /**
     * 显示操作反馈
     * @param {string} message - 反馈消息
     * @param {string} type - 反馈类型
     * @param {number} duration - 显示时长
     */
    showOperationFeedback(message, type = 'processing', duration = 3000) {
        const icons = {
            success: '✅',
            error: '❌',
            processing: '⏳'
        };

        this.operationFeedback.innerHTML = `
            <span>${icons[type] || icons.processing}</span>
            <span>${message}</span>
        `;
        
        this.operationFeedback.className = `operation-feedback ${type} active`;

        if (duration > 0) {
            setTimeout(() => {
                this.operationFeedback.classList.remove('active');
            }, duration);
        }
    }

    /**
     * 隐藏操作反馈
     */
    hideOperationFeedback() {
        this.operationFeedback.classList.remove('active');
    }

    /**
     * 显示确认对话框
     * @param {string} message - 确认消息
     * @param {string} title - 对话框标题
     * @param {Function} onConfirm - 确认回调
     * @param {Function} onCancel - 取消回调
     */
    showConfirm(message, title = '确认操作', onConfirm = null, onCancel = null) {
        this.confirmDialog.querySelector('.confirm-title').textContent = title;
        this.confirmDialog.querySelector('.confirm-message').textContent = message;

        const confirmBtn = this.confirmDialog.querySelector('.confirm-ok');
        const cancelBtn = this.confirmDialog.querySelector('.btn-secondary');

        // 清除之前的事件监听器
        const newConfirmBtn = confirmBtn.cloneNode(true);
        const newCancelBtn = cancelBtn.cloneNode(true);
        confirmBtn.parentNode.replaceChild(newConfirmBtn, confirmBtn);
        cancelBtn.parentNode.replaceChild(newCancelBtn, cancelBtn);

        // 添加新的事件监听器
        newConfirmBtn.addEventListener('click', () => {
            this.hideConfirm();
            if (onConfirm) onConfirm();
        });

        newCancelBtn.addEventListener('click', () => {
            this.hideConfirm();
            if (onCancel) onCancel();
        });

        this.confirmDialog.classList.add('active');
    }

    /**
     * 隐藏确认对话框
     */
    hideConfirm() {
        this.confirmDialog.classList.remove('active');
    }

    /**
     * 按钮加载状态
     * @param {HTMLElement} button - 按钮元素
     * @param {boolean} loading - 是否加载中
     * @param {string} loadingText - 加载时的文本
     */
    setButtonLoading(button, loading, loadingText = '处理中...') {
        if (loading) {
            button.dataset.originalText = button.textContent;
            button.textContent = loadingText;
            button.classList.add('btn-loading');
            button.disabled = true;
        } else {
            button.textContent = button.dataset.originalText || button.textContent;
            button.classList.remove('btn-loading');
            button.disabled = false;
            delete button.dataset.originalText;
        }
    }

    /**
     * 表单验证
     * @param {HTMLElement} input - 输入框元素
     * @param {boolean} isValid - 是否有效
     * @param {string} message - 反馈消息
     */
    setInputValidation(input, isValid, message = '') {
        const formGroup = input.closest('.form-group') || input.parentElement;
        let feedback = formGroup.querySelector('.form-feedback');

        // 移除之前的状态
        input.classList.remove('error', 'success');
        
        if (feedback) {
            feedback.remove();
        }

        if (message) {
            // 添加新状态
            input.classList.add(isValid ? 'success' : 'error');
            
            feedback = document.createElement('div');
            feedback.className = `form-feedback ${isValid ? 'success' : 'error'}`;
            feedback.innerHTML = `
                <span>${isValid ? '✅' : '❌'}</span>
                <span>${message}</span>
            `;
            
            formGroup.appendChild(feedback);
        }
    }

    /**
     * 增强现有元素
     */
    enhanceExistingElements() {
        // 为现有按钮添加悬停效果
        document.querySelectorAll('.btn, .action-btn, .upload-btn').forEach(btn => {
            if (!btn.classList.contains('enhanced-hover')) {
                btn.classList.add('enhanced-hover');
            }
        });

        // 为表单添加实时验证
        document.querySelectorAll('input[required]').forEach(input => {
            this.setupInputValidation(input);
        });

        // 增强表单提交
        document.querySelectorAll('form').forEach(form => {
            this.enhanceFormSubmission(form);
        });
    }

    /**
     * 设置输入框验证
     * @param {HTMLElement} input - 输入框元素
     */
    setupInputValidation(input) {
        input.addEventListener('blur', () => {
            this.validateInput(input);
        });

        input.addEventListener('input', () => {
            if (input.classList.contains('error')) {
                this.validateInput(input);
            }
        });
    }

    /**
     * 验证输入框
     * @param {HTMLElement} input - 输入框元素
     */
    validateInput(input) {
        const value = input.value.trim();
        const type = input.type;
        let isValid = true;
        let message = '';

        if (input.required && !value) {
            isValid = false;
            message = '此字段为必填项';
        } else if (type === 'email' && value && !this.isValidEmail(value)) {
            isValid = false;
            message = '请输入有效的邮箱地址';
        } else if (type === 'url' && value && !this.isValidUrl(value)) {
            isValid = false;
            message = '请输入有效的URL地址';
        } else if (input.minLength && value.length < input.minLength) {
            isValid = false;
            message = `最少需要${input.minLength}个字符`;
        } else if (input.maxLength && value.length > input.maxLength) {
            isValid = false;
            message = `最多只能输入${input.maxLength}个字符`;
        }

        this.setInputValidation(input, isValid, message);
        return isValid;
    }

    /**
     * 增强表单提交
     * @param {HTMLElement} form - 表单元素
     */
    enhanceFormSubmission(form) {
        form.addEventListener('submit', (e) => {
            // 验证所有必填字段
            const requiredInputs = form.querySelectorAll('input[required]');
            let isFormValid = true;

            requiredInputs.forEach(input => {
                if (!this.validateInput(input)) {
                    isFormValid = false;
                }
            });

            if (!isFormValid) {
                e.preventDefault();
                this.showNotification('请修正表单中的错误后重试', 'error');
                return;
            }

            // 显示加载状态
            const submitBtn = form.querySelector('button[type="submit"], input[type="submit"]');
            if (submitBtn) {
                this.setButtonLoading(submitBtn, true);
            }

            // 如果是AJAX表单，这里可以添加AJAX处理
            if (!form.hasAttribute('data-ajax')) {
                this.showOperationFeedback('正在提交...', 'processing');
            }
        });
    }

    /**
     * 设置全局错误处理
     */
    setupGlobalErrorHandling() {
        // 捕获未处理的Promise错误
        window.addEventListener('unhandledrejection', (event) => {
            console.error('未处理的Promise错误:', event.reason);
            this.showNotification('系统发生错误，请稍后重试', 'error');
        });

        // 捕获JavaScript错误
        window.addEventListener('error', (event) => {
            console.error('JavaScript错误:', event.error);
            // 对于用户友好的错误，可以选择性显示
            if (event.error && event.error.message && !event.error.message.includes('Script error')) {
                this.showNotification('页面发生错误，请刷新页面重试', 'error');
            }
        });
    }

    /**
     * 工具方法：验证邮箱
     * @param {string} email - 邮箱地址
     * @returns {boolean}
     */
    isValidEmail(email) {
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        return emailRegex.test(email);
    }

    /**
     * 工具方法：验证URL
     * @param {string} url - URL地址
     * @returns {boolean}
     */
    isValidUrl(url) {
        try {
            new URL(url);
            return true;
        } catch {
            return false;
        }
    }

    /**
     * 工具方法：防抖
     * @param {Function} func - 要防抖的函数
     * @param {number} wait - 等待时间
     * @returns {Function}
     */
    debounce(func, wait) {
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

    /**
     * 工具方法：节流
     * @param {Function} func - 要节流的函数
     * @param {number} limit - 限制时间
     * @returns {Function}
     */
    throttle(func, limit) {
        let inThrottle;
        return function(...args) {
            if (!inThrottle) {
                func.apply(this, args);
                inThrottle = true;
                setTimeout(() => inThrottle = false, limit);
            }
        };
    }

    /**
     * 销毁实例
     */
    destroy() {
        if (this.notificationContainer) this.notificationContainer.remove();
        if (this.loadingOverlay) this.loadingOverlay.remove();
        if (this.operationFeedback) this.operationFeedback.remove();
        if (this.confirmDialog) this.confirmDialog.remove();
    }
}

// 创建全局实例
const ux = new UXEnhancement();

// 兼容性方法，保持向后兼容
function showNotification(message, type = 'info', duration = 5000, title = null) {
    return ux.showNotification(message, type, duration, title);
}

function showLoading(text = '处理中，请稍候...') {
    ux.showLoading(text);
}

function hideLoading() {
    ux.hideLoading();
}

function showOperationFeedback(message, type = 'processing', duration = 3000) {
    ux.showOperationFeedback(message, type, duration);
}

// 导出到全局
window.UXEnhancement = UXEnhancement;
window.ux = ux;