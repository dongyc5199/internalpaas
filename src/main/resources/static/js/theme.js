// 全局主题切换功能
(function() {
    // 页面加载时应用保存的主题
    function applySavedTheme() {
        const savedTheme = localStorage.getItem('theme');
        if (savedTheme === 'dark') {
            document.body.classList.add('dark-theme');
        }
    }

    // 切换主题的函数
    window.toggleTheme = function() {
        const body = document.body;
        const isDark = body.classList.toggle('dark-theme');
        
        // 保存主题设置到localStorage
        localStorage.setItem('theme', isDark ? 'dark' : 'light');
        
        // 触发自定义事件，允许其他组件响应主题变化
        const themeEvent = new CustomEvent('themeChanged', {
            detail: { theme: isDark ? 'dark' : 'light' }
        });
        document.dispatchEvent(themeEvent);
    };

    // 监听DOM加载完成事件
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', applySavedTheme);
    } else {
        applySavedTheme();
    }
})();