// 全局主题切换功能
(function() {
    // 页面加载时应用保存的主题
    function applySavedTheme() {
        const savedTheme = localStorage.getItem('theme');
        if (savedTheme === 'dark') {
            if (!document.body.classList.contains('dark-theme')) {
                document.body.classList.add('dark-theme');
            }
            if (!document.documentElement.classList.contains('theme-dark')) {
                document.documentElement.classList.remove('theme-light');
                document.documentElement.classList.add('theme-dark');
            }
            // 重新启用过渡效果
            setTimeout(() => {
                const elements = document.querySelectorAll('*');
                elements.forEach(el => {
                    el.style.transition = '';
                });
            }, 100);
        }
    }

    // 切换主题的函数
    window.toggleTheme = function() {
        const body = document.body;
        const isDark = body.classList.toggle('dark-theme');
        
        // 同步更新html元素的主题类
        if (isDark) {
            document.documentElement.classList.remove('theme-light');
            document.documentElement.classList.add('theme-dark');
        } else {
            document.documentElement.classList.remove('theme-dark');
            document.documentElement.classList.add('theme-light');
        }
        
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