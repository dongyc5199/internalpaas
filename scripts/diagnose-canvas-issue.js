/**
 * Canvas 尺寸问题诊断脚本
 * 
 * 使用方法:
 * 1. 打开浏览器访问 http://localhost:9091/admin/workspace
 * 2. 按 F12 打开控制台
 * 3. 将此脚本完整复制粘贴到控制台执行
 */

(function diagnoseCanvasIssue() {
    console.log('🔍 开始诊断 Canvas 尺寸问题...\n');

    const canvas = document.getElementById('server-status-chart');
    if (!canvas) {
        console.error('❌ Canvas 元素不存在!');
        return;
    }

    console.log('✅ Canvas 元素存在\n');

    // 1. 检查 Canvas 本身的样式
    console.log('📋 Canvas 元素样式:');
    const canvasStyle = window.getComputedStyle(canvas);
    console.log('  - width:', canvasStyle.width);
    console.log('  - height:', canvasStyle.height);
    console.log('  - min-width:', canvasStyle.minWidth);
    console.log('  - min-height:', canvasStyle.minHeight);
    console.log('  - display:', canvasStyle.display);
    console.log('  - visibility:', canvasStyle.visibility);

    // 2. 检查父容器链
    console.log('\n📦 父容器链检查:');
    let currentEl = canvas;
    let level = 0;
    while (currentEl && level < 5) {
        currentEl = currentEl.parentElement;
        if (!currentEl) break;
        
        const style = window.getComputedStyle(currentEl);
        console.log(`\n  ${level + 1}. <${currentEl.tagName.toLowerCase()}> ${currentEl.className ? `.${currentEl.className.split(' ').join('.')}` : ''}`);
        console.log(`     - offsetWidth: ${currentEl.offsetWidth}px`);
        console.log(`     - offsetHeight: ${currentEl.offsetHeight}px`);
        console.log(`     - width: ${style.width}`);
        console.log(`     - height: ${style.height}`);
        console.log(`     - display: ${style.display}`);
        
        level++;
    }

    // 3. 检查 CSS 文件是否加载
    console.log('\n📄 CSS 文件加载检查:');
    const stylesheets = Array.from(document.styleSheets);
    const relevantCSS = stylesheets.filter(sheet => {
        try {
            return sheet.href && (sheet.href.includes('admin-dashboard') || sheet.href.includes('main.css'));
        } catch (e) {
            return false;
        }
    });
    
    if (relevantCSS.length > 0) {
        console.log(`  ✅ 找到 ${relevantCSS.length} 个相关 CSS 文件:`);
        relevantCSS.forEach(sheet => console.log(`     - ${sheet.href}`));
    } else {
        console.log('  ⚠️ 未找到 admin-dashboard.css 或 main.css');
    }

    // 4. 检查 chart-body 类的 CSS 规则
    console.log('\n🎨 CSS 规则检查 (.chart-body canvas):');
    let foundRule = false;
    for (const sheet of stylesheets) {
        try {
            const rules = Array.from(sheet.cssRules || []);
            for (const rule of rules) {
                if (rule.selectorText && rule.selectorText.includes('.chart-body') && rule.selectorText.includes('canvas')) {
                    foundRule = true;
                    console.log(`  ✅ 找到规则: ${rule.selectorText}`);
                    console.log(`     ${rule.cssText}`);
                }
            }
        } catch (e) {
            // CORS 可能阻止访问某些样式表
        }
    }
    
    if (!foundRule) {
        console.log('  ❌ 未找到 .chart-body canvas 的 CSS 规则!');
        console.log('  💡 这是问题的根源!');
    }

    // 5. 检查是否有行内样式覆盖
    console.log('\n💅 行内样式检查:');
    console.log(`  - Canvas style 属性: ${canvas.getAttribute('style') || '无'}`);
    const parent = canvas.parentElement;
    if (parent) {
        console.log(`  - 父容器 style 属性: ${parent.getAttribute('style') || '无'}`);
    }

    // 6. 建议修复方案
    console.log('\n💡 建议修复方案:');
    console.log('='.repeat(60));
    
    if (!foundRule) {
        console.log('📌 方案 1: CSS 文件未正确加载');
        console.log('   1. 检查 <head> 中是否包含:');
        console.log('      <link rel="stylesheet" href="/css/admin-dashboard.css">');
        console.log('   2. 或者通过 F12 → Network 检查 CSS 文件是否 404');
        console.log('   3. 确认 Spring Boot 应用已重启并加载最新资源\n');
    }

    if (parent && parent.offsetWidth === 0) {
        console.log('📌 方案 2: 父容器 .chart-body 没有尺寸');
        console.log('   1. 手动设置尺寸测试:');
        console.log('      执行以下代码:');
        console.log('      ---');
        console.log(`      const parent = document.querySelector('.chart-body');`);
        console.log(`      parent.style.width = '400px';`);
        console.log(`      parent.style.height = '300px';`);
        console.log(`      window.initDashboardCharts();`);
        console.log('      ---\n');
    }

    console.log('📌 方案 3: 直接修复 (临时测试)');
    console.log('   执行以下代码手动设置尺寸:');
    console.log('   ---');
    console.log('   document.querySelectorAll(".chart-body").forEach(el => {');
    console.log('     el.style.width = "100%";');
    console.log('     el.style.height = "300px";');
    console.log('   });');
    console.log('   document.querySelectorAll(".chart-body canvas").forEach(el => {');
    console.log('     el.style.width = "100%";');
    console.log('     el.style.height = "100%";');
    console.log('   });');
    console.log('   window.initDashboardCharts();');
    console.log('   ---');
    
    console.log('\n' + '='.repeat(60));
})();
