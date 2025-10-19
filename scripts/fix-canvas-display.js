/**
 * 修复 Canvas 显示问题
 * 强制显示被隐藏的图表容器
 */

(function fixCanvasDisplay() {
    console.log('🔧 开始修复 Canvas 显示问题...\n');

    // 1. 强制显示所有图表容器
    const containers = document.querySelectorAll('.chart-container');
    console.log(`📦 找到 ${containers.length} 个图表容器`);
    
    containers.forEach((container, index) => {
        const beforeDisplay = window.getComputedStyle(container).display;
        
        // 强制设置为 flex 布局
        container.style.display = 'flex';
        container.style.flexDirection = 'column';
        container.style.minHeight = '300px';
        
        const afterDisplay = window.getComputedStyle(container).display;
        console.log(`  ${index + 1}. 容器 ${container.id || '(无ID)'}`);
        console.log(`     修复前: display=${beforeDisplay}`);
        console.log(`     修复后: display=${afterDisplay}`);
    });

    // 2. 确保 chart-body 有尺寸
    const chartBodies = document.querySelectorAll('.chart-body');
    console.log(`\n📐 设置 ${chartBodies.length} 个 chart-body 尺寸`);
    
    chartBodies.forEach((body, index) => {
        body.style.width = '100%';
        body.style.height = '300px';
        body.style.display = 'flex';
        body.style.alignItems = 'center';
        body.style.justifyContent = 'center';
        
        console.log(`  ${index + 1}. chart-body 已设置为 100% x 300px`);
    });

    // 3. 设置 Canvas 样式
    const canvases = document.querySelectorAll('.chart-body canvas');
    console.log(`\n🎨 配置 ${canvases.length} 个 Canvas 元素`);
    
    canvases.forEach((canvas, index) => {
        canvas.style.width = '100%';
        canvas.style.height = '100%';
        canvas.style.minWidth = '280px';
        canvas.style.minHeight = '220px';
        canvas.style.display = 'block';
        
        console.log(`  ${index + 1}. ${canvas.id} 样式已配置`);
    });

    // 4. 重新初始化图表
    console.log('\n🔄 重新初始化图表...');
    if (typeof window.initDashboardCharts === 'function') {
        try {
            const dashboard = window.initDashboardCharts();
            console.log('  ✅ 图表初始化成功');
            
            // 验证尺寸
            setTimeout(() => {
                console.log('\n✨ 最终尺寸验证:');
                canvases.forEach((canvas, index) => {
                    console.log(`  ${index + 1}. ${canvas.id}:`);
                    console.log(`     - offsetWidth: ${canvas.offsetWidth}px`);
                    console.log(`     - offsetHeight: ${canvas.offsetHeight}px`);
                    console.log(`     - Canvas width: ${canvas.width}px`);
                    console.log(`     - Canvas height: ${canvas.height}px`);
                });
                
                // 检查是否有尺寸
                const allHaveSize = Array.from(canvases).every(c => c.offsetWidth > 0 && c.offsetHeight > 0);
                if (allHaveSize) {
                    console.log('\n🎉 修复成功! 所有 Canvas 都有尺寸了!');
                } else {
                    console.log('\n⚠️ 某些 Canvas 仍然没有尺寸,请检查父容器');
                }
            }, 1000);
            
        } catch (error) {
            console.error('  ❌ 初始化失败:', error);
        }
    } else {
        console.warn('  ⚠️ window.initDashboardCharts 函数不存在');
    }

    console.log('\n' + '='.repeat(60));
})();
