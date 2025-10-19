/**
 * SSH 配置导入向导 - 按钮重复 ID 诊断脚本
 * 
 * 使用方法：
 * 1. 打开浏览器开发者工具（F12）
 * 2. 切换到 Console 标签
 * 3. 复制粘贴此脚本并回车运行
 * 4. 查看详细的诊断报告
 */

(function() {
    console.log('==================================================');
    console.log('  SSH 配置导入向导按钮诊断工具');
    console.log('==================================================\n');

    // 1. 检查所有向导按钮的数量
    console.log('📊 按钮数量统计:');
    console.log('─'.repeat(50));
    
    const buttons = {
        'startScanBtn': document.querySelectorAll('#startScanBtn'),
        'wizardPrevBtn': document.querySelectorAll('#wizardPrevBtn'),
        'wizardNextBtn': document.querySelectorAll('#wizardNextBtn'),
        'wizardCancelBtn': document.querySelectorAll('#wizardCancelBtn')
    };
    
    const summary = {};
    
    for (const [btnId, elements] of Object.entries(buttons)) {
        const count = elements.length;
        const status = count === 1 ? '✅' : '❌';
        summary[btnId] = { count, status };
        
        console.log(`${status} ${btnId.padEnd(20)} 数量: ${count} ${count > 1 ? '⚠️ 重复!' : ''}`);
    }
    
    console.log('\n');

    // 2. 详细检查 startScanBtn
    if (summary.startScanBtn.count > 1) {
        console.log('🔍 startScanBtn 详细分析（发现重复）:');
        console.log('─'.repeat(50));
        
        buttons.startScanBtn.forEach((btn, index) => {
            console.log(`\n【实例 ${index + 1}】`);
            console.log(`  父容器: ${btn.parentElement?.className || 'unknown'}`);
            console.log(`  祖父容器: ${btn.parentElement?.parentElement?.className || 'unknown'}`);
            console.log(`  可见性 (display): ${window.getComputedStyle(btn).display}`);
            console.log(`  可见性 (visibility): ${window.getComputedStyle(btn).visibility}`);
            console.log(`  offsetParent: ${btn.offsetParent ? btn.offsetParent.tagName : 'null (隐藏)'}`);
            console.log(`  实际可见: ${btn.offsetWidth > 0 && btn.offsetHeight > 0 ? '是' : '否'}`);
            console.log(`  位置: `, btn.getBoundingClientRect());
            
            // 检查是否在模态框中
            const modal = btn.closest('.modal, [role="dialog"]');
            if (modal) {
                console.log(`  所属模态框 ID: ${modal.id || '(无ID)'}`);
            }
            
            // 显示 HTML
            console.log(`  HTML 片段:`, btn.outerHTML.substring(0, 100) + '...');
        });
    } else if (summary.startScanBtn.count === 0) {
        console.log('⚠️ 未找到 startScanBtn 按钮！');
    } else {
        console.log('✅ startScanBtn 按钮唯一性检查通过');
        
        const btn = buttons.startScanBtn[0];
        console.log('\n📍 startScanBtn 位置信息:');
        console.log('─'.repeat(50));
        console.log(`  父容器: ${btn.parentElement?.className || 'unknown'}`);
        console.log(`  当前可见: ${btn.offsetWidth > 0 && btn.offsetHeight > 0 ? '是' : '否'}`);
        console.log(`  display: ${window.getComputedStyle(btn).display}`);
    }

    console.log('\n');

    // 3. 检查其他按钮的状态
    console.log('📋 其他按钮状态:');
    console.log('─'.repeat(50));
    
    ['wizardPrevBtn', 'wizardNextBtn', 'wizardCancelBtn'].forEach(btnId => {
        const elements = buttons[btnId];
        if (elements.length === 1) {
            const btn = elements[0];
            const visible = btn.offsetWidth > 0 && btn.offsetHeight > 0;
            const display = window.getComputedStyle(btn).display;
            const disabled = btn.disabled;
            
            console.log(`\n${btnId}:`);
            console.log(`  数量: ${elements.length} ✅`);
            console.log(`  可见: ${visible ? '是' : '否'}`);
            console.log(`  display: ${display}`);
            console.log(`  disabled: ${disabled}`);
        } else if (elements.length === 0) {
            console.log(`\n${btnId}: ❌ 未找到`);
        } else {
            console.log(`\n${btnId}: ❌ 数量异常 (${elements.length})`);
        }
    });

    console.log('\n');

    // 4. 检查 wizardState 状态
    console.log('🔧 状态管理对象检查:');
    console.log('─'.repeat(50));
    
    if (typeof wizardState !== 'undefined') {
        console.log('✅ wizardState 已定义');
        console.log(`  currentStep: ${wizardState.currentStep}`);
        console.log(`  hasScanned: ${wizardState.hasScanned}`);
        console.log(`  selectedServers: ${wizardState.selectedServers?.length || 0} 台`);
        console.log(`  importResult: ${wizardState.importResult ? '已设置' : 'null'}`);
    } else {
        console.log('❌ wizardState 未定义（可能脚本尚未加载）');
    }

    console.log('\n');

    // 5. 检查模态框状态
    console.log('🪟 模态框状态检查:');
    console.log('─'.repeat(50));
    
    const modal = document.getElementById('sshConfigImportModal');
    if (modal) {
        const isVisible = window.getComputedStyle(modal).display !== 'none';
        console.log('✅ sshConfigImportModal 已找到');
        console.log(`  可见: ${isVisible ? '是' : '否'}`);
        console.log(`  display: ${window.getComputedStyle(modal).display}`);
    } else {
        console.log('❌ sshConfigImportModal 未找到');
    }

    console.log('\n');

    // 6. 检查步骤容器
    console.log('📦 步骤容器状态:');
    console.log('─'.repeat(50));
    
    const steps = {
        'auto-scan': document.getElementById('auto-scan'),
        'wizardStep2': document.getElementById('wizardStep2'),
        'wizardStep3': document.getElementById('wizardStep3')
    };
    
    for (const [stepId, element] of Object.entries(steps)) {
        if (element) {
            const display = window.getComputedStyle(element).display;
            const visible = element.offsetWidth > 0 && element.offsetHeight > 0;
            console.log(`${stepId.padEnd(15)} display: ${display.padEnd(10)} 可见: ${visible ? '是' : '否'}`);
        } else {
            console.log(`${stepId.padEnd(15)} ❌ 未找到`);
        }
    }

    console.log('\n');

    // 7. 生成修复建议
    console.log('💡 修复建议:');
    console.log('─'.repeat(50));
    
    if (summary.startScanBtn.count > 1) {
        console.log('❌ 发现重复 ID 问题！');
        console.log('\n建议操作:');
        console.log('1. 检查是否有多个模态框实例被渲染');
        console.log('2. 删除冗余的 server-import-modal.html 文件');
        console.log('3. 清除浏览器缓存并硬刷新（Ctrl + F5）');
        console.log('4. 重新构建前端资源（npm run build）');
        console.log('\n参考文档: docs/technical/SSH_SCAN_BUTTON_DUPLICATE_ID_ISSUE.md');
    } else if (summary.startScanBtn.count === 0) {
        console.log('⚠️ 按钮未找到！');
        console.log('\n建议操作:');
        console.log('1. 确认模态框已打开');
        console.log('2. 检查 fragment 是否正确加载');
        console.log('3. 查看浏览器控制台是否有 JavaScript 错误');
    } else {
        console.log('✅ 所有按钮 ID 唯一性检查通过！');
        
        if (summary.wizardPrevBtn.count === 1 && 
            summary.wizardNextBtn.count === 1 && 
            summary.wizardCancelBtn.count === 1) {
            console.log('✅ 向导按钮结构正常');
        }
    }

    console.log('\n==================================================');
    console.log('  诊断完成');
    console.log('==================================================\n');

    // 返回诊断摘要
    return {
        summary,
        hasIssues: Object.values(summary).some(s => s.count !== 1),
        wizardState: typeof wizardState !== 'undefined' ? {
            currentStep: wizardState.currentStep,
            hasScanned: wizardState.hasScanned
        } : null
    };
})();
