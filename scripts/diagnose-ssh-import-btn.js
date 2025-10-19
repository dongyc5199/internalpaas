/**
 * SSH配置导入按钮诊断脚本
 * 
 * 使用方法：在浏览器控制台运行此脚本
 * 或在页面加载后注入此脚本
 */

(function() {
    console.group("🔍 SSH配置导入按钮诊断");
    
    // 1. 检查按钮是否存在
    console.log("\n1️⃣ 检查按钮元素");
    const btn = document.getElementById("importSshConfigBtn");
    if (btn) {
        console.log("✅ 按钮元素存在:", btn);
        console.log("   - onclick属性:", btn.getAttribute("onclick"));
        console.log("   - 是否禁用:", btn.disabled);
        console.log("   - 是否可见:", window.getComputedStyle(btn).display !== "none");
    } else {
        console.error("❌ 按钮元素不存在！");
    }
    
    // 2. 检查全局函数是否存在
    console.log("\n2️⃣ 检查全局函数");
    if (typeof window.openSSHConfigImportWizard === "function") {
        console.log("✅ window.openSSHConfigImportWizard 函数存在");
        console.log("   - 函数:", window.openSSHConfigImportWizard);
    } else {
        console.error("❌ window.openSSHConfigImportWizard 函数不存在！");
        console.log("   - 可用的SSH相关函数:", 
            Object.keys(window).filter(k => k.toLowerCase().includes("ssh")));
    }
    
    // 3. 检查模态框元素是否存在
    console.log("\n3️⃣ 检查模态框元素");
    const modal = document.getElementById("sshConfigImportModal");
    if (modal) {
        console.log("✅ 模态框元素存在:", modal);
        console.log("   - display样式:", window.getComputedStyle(modal).display);
        console.log("   - 是否有内容:", modal.innerHTML.length > 100);
    } else {
        console.error("❌ 模态框元素不存在！");
        console.log("   - 检查是否在页面底部:");
        const allModals = document.querySelectorAll('[id*="modal"]');
        console.log("   - 找到的模态框:", Array.from(allModals).map(m => m.id));
    }
    
    // 4. 检查CSS文件是否加载
    console.log("\n4️⃣ 检查CSS文件");
    const cssLinks = Array.from(document.querySelectorAll('link[rel="stylesheet"]'));
    const sshWizardCSS = cssLinks.find(link => 
        link.href.includes("ssh-config-import-wizard.css")
    );
    if (sshWizardCSS) {
        console.log("✅ SSH向导CSS已加载:", sshWizardCSS.href);
    } else {
        console.warn("⚠️ SSH向导CSS未找到");
        console.log("   - 已加载的CSS:", cssLinks.map(l => l.href.split('/').pop()));
    }
    
    // 5. 检查JavaScript模块是否加载
    console.log("\n5️⃣ 检查JavaScript模块");
    const scripts = Array.from(document.querySelectorAll('script[src]'));
    const mainJS = scripts.find(s => s.src.includes("main") || s.src.includes("dist"));
    if (mainJS) {
        console.log("✅ 主JS文件已加载:", mainJS.src);
    } else {
        console.warn("⚠️ 主JS文件未找到");
    }
    
    // 6. 尝试手动触发
    console.log("\n6️⃣ 尝试手动触发");
    if (btn && typeof window.openSSHConfigImportWizard === "function") {
        console.log("尝试手动调用 window.openSSHConfigImportWizard()...");
        try {
            window.openSSHConfigImportWizard();
            console.log("✅ 手动调用成功");
        } catch (error) {
            console.error("❌ 手动调用失败:", error);
        }
    }
    
    // 7. 检查控制台是否有错误
    console.log("\n7️⃣ 检查模块加载日志");
    console.log("请检查控制台是否有以下日志:");
    console.log("   - 'SSHConfigImportWizard.ts loaded'");
    console.log("   - 'Initializing SSH Config Import Wizard...'");
    console.log("   - 'SSH Config Import Wizard initialized'");
    
    // 8. 生成诊断报告
    console.log("\n8️⃣ 诊断总结");
    const issues = [];
    
    if (!btn) issues.push("按钮元素不存在");
    if (typeof window.openSSHConfigImportWizard !== "function") {
        issues.push("全局函数不存在");
    }
    if (!modal) issues.push("模态框元素不存在");
    if (!sshWizardCSS) issues.push("CSS文件未加载");
    
    if (issues.length === 0) {
        console.log("✅ 所有检查通过，按钮应该可以工作");
        console.log("💡 如果仍然无响应，请:");
        console.log("   1. 刷新页面（Ctrl+F5 强制刷新）");
        console.log("   2. 检查浏览器控制台是否有JavaScript错误");
        console.log("   3. 确认Spring Boot应用已重启");
    } else {
        console.error("❌ 发现问题:", issues.join(", "));
        console.log("\n🔧 建议修复步骤:");
        
        if (!btn) {
            console.log("   1. 检查 server-group-content.html 是否包含 importSshConfigBtn");
        }
        
        if (typeof window.openSSHConfigImportWizard !== "function") {
            console.log("   2. 检查 main.ts 是否导入了 SSHConfigImportWizard 模块");
            console.log("      - import \"./modules/SSHConfigImportWizard\";");
            console.log("   3. 运行 npm run build 重新构建前端");
        }
        
        if (!modal) {
            console.log("   4. 检查 server-group-content.html 是否包含 ssh-config-import-wizard fragment");
            console.log("      - <div th:replace=\"fragments/ssh-config-import-wizard :: ssh-config-import-wizard\"></div>");
        }
        
        if (!sshWizardCSS) {
            console.log("   5. 检查 main-layout.html 是否包含 CSS 链接");
            console.log("      - <link rel=\"stylesheet\" href=\"/css/ssh-config-import-wizard.css\">");
        }
        
        console.log("\n   6. 重启Spring Boot应用");
        console.log("   7. 强制刷新浏览器（Ctrl+F5）");
    }
    
    console.groupEnd();
    
    return {
        button: btn,
        globalFunction: window.openSSHConfigImportWizard,
        modal: modal,
        cssLoaded: !!sshWizardCSS,
        issues: issues
    };
})();
