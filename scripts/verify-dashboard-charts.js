/**
 * P0-Day5 图表验证脚本
 * 
 * 使用方法:
 * 1. 打开浏览器访问 http://localhost:9091/admin/workspace
 * 2. 按 F12 打开控制台
 * 3. 将此脚本完整复制粘贴到控制台执行
 * 4. 查看输出的验证结果
 */

(async function verifyDashboardCharts() {
    console.log('🔍 开始验证 Dashboard 图表...\n');

    const results = {
        passed: [],
        failed: [],
        warnings: []
    };

    // ============================================
    // 测试 1: 检查 Canvas 元素存在性
    // ============================================
    console.log('📋 测试 1: 检查 Canvas 元素...');
    const canvas1 = document.getElementById('server-status-chart');
    const canvas2 = document.getElementById('resource-usage-chart');
    const canvas3 = document.getElementById('realtime-monitor-chart');

    if (canvas1 && canvas2 && canvas3) {
        results.passed.push('✅ 所有 3 个 Canvas 元素都存在');
        console.log('  ✅ server-status-chart: 存在');
        console.log('  ✅ resource-usage-chart: 存在');
        console.log('  ✅ realtime-monitor-chart: 存在');
    } else {
        results.failed.push('❌ 缺少 Canvas 元素');
        if (!canvas1) console.log('  ❌ server-status-chart: 缺失');
        if (!canvas2) console.log('  ❌ resource-usage-chart: 缺失');
        if (!canvas3) console.log('  ❌ realtime-monitor-chart: 缺失');
    }

    // ============================================
    // 测试 2: 检查 Canvas 尺寸
    // ============================================
    console.log('\n📐 测试 2: 检查 Canvas 尺寸...');
    const dimensions = [
        { name: 'server-status-chart', el: canvas1 },
        { name: 'resource-usage-chart', el: canvas2 },
        { name: 'realtime-monitor-chart', el: canvas3 }
    ].map(({ name, el }) => {
        if (!el) return null;
        return {
            name,
            offsetWidth: el.offsetWidth,
            offsetHeight: el.offsetHeight,
            canvasWidth: el.width,
            canvasHeight: el.height
        };
    }).filter(Boolean);

    let allHaveSize = true;
    dimensions.forEach(dim => {
        console.log(`  📊 ${dim.name}:`);
        console.log(`     - 显示尺寸: ${dim.offsetWidth} x ${dim.offsetHeight}`);
        console.log(`     - Canvas尺寸: ${dim.canvasWidth} x ${dim.canvasHeight}`);
        
        if (dim.offsetWidth === 0 || dim.offsetHeight === 0) {
            allHaveSize = false;
            results.failed.push(`❌ ${dim.name} 尺寸为 0`);
        }
    });

    if (allHaveSize) {
        results.passed.push('✅ 所有 Canvas 都有正确的尺寸');
    }

    // ============================================
    // 测试 3: 检查 Dashboard 实例
    // ============================================
    console.log('\n🔧 测试 3: 检查 Dashboard 实例...');
    if (typeof window.dashboard !== 'undefined') {
        results.passed.push('✅ window.dashboard 实例存在');
        console.log('  ✅ window.dashboard: 存在');

        const chartsCount = window.dashboard?.getCharts?.()?.size || 0;
        console.log(`  📊 图表实例数量: ${chartsCount}`);
        
        if (chartsCount === 3) {
            results.passed.push('✅ 3 个图表实例已创建');
        } else {
            results.failed.push(`❌ 图表实例数量不正确 (期望: 3, 实际: ${chartsCount})`);
        }
    } else {
        results.failed.push('❌ window.dashboard 实例不存在');
        console.log('  ❌ window.dashboard: 不存在');
    }

    // ============================================
    // 测试 4: 检查 Chart.js 全局函数
    // ============================================
    console.log('\n🌍 测试 4: 检查全局函数...');
    if (typeof window.initDashboardCharts === 'function') {
        results.passed.push('✅ window.initDashboardCharts 函数存在');
        console.log('  ✅ window.initDashboardCharts: 存在');
    } else {
        results.warnings.push('⚠️ window.initDashboardCharts 函数不存在 (可能已清理)');
        console.log('  ⚠️ window.initDashboardCharts: 不存在');
    }

    // ============================================
    // 测试 5: 检查 Canvas 渲染内容
    // ============================================
    console.log('\n🎨 测试 5: 检查 Canvas 渲染内容...');
    let hasRenderedContent = false;
    
    [canvas1, canvas2, canvas3].forEach((canvas, index) => {
        if (!canvas) return;
        
        const ctx = canvas.getContext('2d');
        if (!ctx) {
            console.log(`  ❌ Canvas ${index + 1}: 无法获取 2D 上下文`);
            return;
        }

        const imageData = ctx.getImageData(0, 0, canvas.width, canvas.height);
        const pixels = imageData.data;
        let hasPixels = false;

        // 检查前 1000 个像素点是否有非透明内容
        for (let i = 3; i < Math.min(pixels.length, 4000); i += 4) {
            if (pixels[i] > 0) {
                hasPixels = true;
                hasRenderedContent = true;
                break;
            }
        }

        const canvasName = ['server-status', 'resource-usage', 'realtime-monitor'][index];
        if (hasPixels) {
            console.log(`  ✅ ${canvasName}-chart: 包含渲染内容`);
        } else {
            console.log(`  ⚠️ ${canvasName}-chart: 未检测到渲染内容`);
            results.warnings.push(`⚠️ ${canvasName}-chart 可能未渲染`);
        }
    });

    if (hasRenderedContent) {
        results.passed.push('✅ 至少一个图表包含渲染内容');
    }

    // ============================================
    // 测试 6: 检查图表区域可见性
    // ============================================
    console.log('\n👁️ 测试 6: 检查图表区域可见性...');
    const chartsSection = document.querySelector('[data-dashboard-charts]');
    if (chartsSection) {
        const isVisible = chartsSection.offsetHeight > 0 && chartsSection.offsetWidth > 0;
        if (isVisible) {
            results.passed.push('✅ 图表区域可见');
            console.log(`  ✅ 图表区域尺寸: ${chartsSection.offsetWidth} x ${chartsSection.offsetHeight}`);
        } else {
            results.failed.push('❌ 图表区域不可见');
            console.log('  ❌ 图表区域不可见');
        }
    } else {
        results.failed.push('❌ 图表区域不存在');
        console.log('  ❌ 未找到 [data-dashboard-charts] 区域');
    }

    // ============================================
    // 输出测试报告
    // ============================================
    console.log('\n' + '='.repeat(60));
    console.log('📊 测试报告');
    console.log('='.repeat(60));
    
    console.log(`\n✅ 通过测试 (${results.passed.length}):`);
    results.passed.forEach(msg => console.log(`   ${msg}`));

    if (results.warnings.length > 0) {
        console.log(`\n⚠️ 警告 (${results.warnings.length}):`);
        results.warnings.forEach(msg => console.log(`   ${msg}`));
    }

    if (results.failed.length > 0) {
        console.log(`\n❌ 失败测试 (${results.failed.length}):`);
        results.failed.forEach(msg => console.log(`   ${msg}`));
    }

    const totalTests = 6;
    const passedTests = results.failed.length === 0 ? totalTests : totalTests - results.failed.length;
    const successRate = ((passedTests / totalTests) * 100).toFixed(1);

    console.log(`\n📈 测试通过率: ${passedTests}/${totalTests} (${successRate}%)`);
    
    if (results.failed.length === 0) {
        console.log('\n🎉 所有测试通过! 图表渲染正常!');
    } else {
        console.log('\n⚠️ 存在失败的测试项,请检查上述问题');
    }

    console.log('='.repeat(60) + '\n');

    return {
        success: results.failed.length === 0,
        passed: results.passed.length,
        failed: results.failed.length,
        warnings: results.warnings.length,
        details: results
    };
})();
