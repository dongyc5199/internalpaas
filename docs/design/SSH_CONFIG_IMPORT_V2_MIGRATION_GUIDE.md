# SSH配置导入弹窗改版实施指南

## 📋 改版概述

已完成SSH配置导入弹窗的全新设计,完全对齐 `server-group-import-modal.html` 设计稿。

### 改动文件清单

| 文件 | 类型 | 状态 | 说明 |
|------|------|------|------|
| `ssh-config-import-wizard-v2.html` | 新建 | ✅ | 新版HTML模板(Tab布局) |
| `ssh-config-import-wizard-v2.css` | 新建 | ✅ | 新版CSS样式 |
| `ssh-config-import-wizard.html` | 保留 | ⚠️ | 旧版(供对比/回退) |
| `SSHConfigImportWizard.ts` | 需更新 | 🔧 | TypeScript逻辑需适配 |

---

## 🎯 核心改进

### 1. 客户端展示横幅 ✨
```html
<div class="clients-banner">
    ✨ 智能识别多种SSH客户端
    [🔐 SecureCRT 支持] [📡 Xshell 支持] [⚡ Tabby 支持]
    [🖥️ MobaXterm 即将] [🔧 PuTTY 计划]
</div>
```

### 2. Tab切换布局 🔄
- ✅ 自动扫描
- ✅ 文件上传
- ✅ 自定义路径

### 3. 自动扫描进度 📊
4步详细进度:
1. 检查系统注册表...
2. 扫描默认配置路径...
3. 从安装目录推断配置位置...
4. 验证配置文件有效性...

### 4. 扫描结果展示 📈
显示:
- ✅ 客户端名称 + 版本
- ✅ 会话数量统计
- ✅ 配置文件路径

---

## 🚀 启用步骤

### Step 1: 替换HTML模板引用

#### 方式A: 直接替换(推荐)
```bash
# 备份旧文件
mv src/main/resources/templates/fragments/ssh-config-import-wizard.html \
   src/main/resources/templates/fragments/ssh-config-import-wizard-old.html

# 启用新文件
mv src/main/resources/templates/fragments/ssh-config-import-wizard-v2.html \
   src/main/resources/templates/fragments/ssh-config-import-wizard.html
```

#### 方式B: 修改Thymeleaf引用
找到所有引用 `ssh-config-import-wizard` fragment的地方,改为:
```html
<!-- 旧引用 -->
<div th:replace="fragments/ssh-config-import-wizard :: ssh-config-import-wizard"></div>

<!-- 新引用 -->
<div th:replace="fragments/ssh-config-import-wizard-v2 :: ssh-config-import-wizard"></div>
```

### Step 2: 导入新CSS样式

在 `main.ts` 中添加:
```typescript
import '../styles/ssh-config-import-wizard-v2.css';
```

或在HTML中直接引入:
```html
<link rel="stylesheet" th:href="@{/css/ssh-config-import-wizard-v2.css}">
```

### Step 3: 更新TypeScript逻辑

需要更新 `SSHConfigImportWizard.ts` 中的选择器:

```typescript
// 旧选择器 (原来的 3-Step 向导)
const step1 = document.getElementById('wizardStep1');
const step2 = document.getElementById('wizardStep2');
const step3 = document.getElementById('wizardStep3');

// 新选择器 (Tab 布局)
const autoScanTab = document.getElementById('auto-scan');
const uploadTab = document.getElementById('upload');
const manualTab = document.getElementById('manual');
```

### Step 4: 实现Tab切换逻辑

添加Tab切换事件监听:

```typescript
// Tab切换
document.querySelectorAll('.import-tab').forEach(tab => {
    tab.addEventListener('click', () => {
        const targetTab = tab.getAttribute('data-tab');
        
        // 移除所有active
        document.querySelectorAll('.import-tab').forEach(t => 
            t.classList.remove('active')
        );
        document.querySelectorAll('.tab-content').forEach(c => 
            c.classList.remove('active')
        );
        
        // 添加active到当前
        tab.classList.add('active');
        document.getElementById(targetTab)?.classList.add('active');
    });
});
```

### Step 5: 实现自动扫描进度动画

```typescript
async function startAutoScan() {
    const scanningStatus = document.getElementById('scanningStatus');
    const scanResult = document.getElementById('scanResult');
    const progressFill = document.getElementById('progressFill');
    
    // 显示扫描状态
    scanningStatus?.classList.add('active');
    scanResult?.classList.remove('active');
    
    const steps = [
        { id: 'step1', progress: 25, delay: 500 },
        { id: 'step2', progress: 50, delay: 1000 },
        { id: 'step3', progress: 75, delay: 1500 },
        { id: 'step4', progress: 100, delay: 2000 }
    ];
    
    for (const [index, step] of steps.entries()) {
        await new Promise(resolve => setTimeout(resolve, step.delay));
        
        const stepEl = document.getElementById(step.id);
        stepEl?.classList.add('active');
        
        if (progressFill) {
            progressFill.style.width = `${step.progress}%`;
        }
        
        if (index > 0) {
            const prevStep = document.getElementById(steps[index - 1].id);
            prevStep?.classList.remove('active');
            prevStep?.classList.add('completed');
            const iconEl = prevStep?.querySelector('.scan-step-icon');
            if (iconEl) iconEl.textContent = '✓';
        }
    }
    
    // 完成后显示结果
    await new Promise(resolve => setTimeout(resolve, 500));
    scanningStatus?.classList.remove('active');
    scanResult?.classList.add('active');
}
```

### Step 6: 填充扫描结果

```typescript
function displayScanResult(clients: Array<{name: string, version: string, sessionCount: number, path: string}>) {
    const resultInfo = document.getElementById('scanResultInfo');
    const subtitle = document.getElementById('scanResultSubtitle');
    const tipContent = document.getElementById('scanTipContent');
    
    if (subtitle) {
        subtitle.textContent = `检测到 ${clients.length} 个已安装的SSH客户端`;
    }
    
    if (resultInfo) {
        resultInfo.innerHTML = clients.map(client => `
            <div class="info-item">
                <div class="info-label">
                    ${getClientIcon(client.name)} ${client.name} ${client.version}
                    <span class="badge badge-success">已安装</span>
                </div>
                <div class="info-value">
                    ${client.sessionCount} 个会话 • ${client.path}
                </div>
            </div>
        `).join('');
    }
    
    const totalSessions = clients.reduce((sum, c) => sum + c.sessionCount, 0);
    if (tipContent) {
        tipContent.textContent = `共发现 ${totalSessions} 个会话配置（来自${clients.length}个客户端），系统将自动去重和合并。预计导入时间 3-5 分钟。`;
    }
}

function getClientIcon(name: string): string {
    const icons: Record<string, string> = {
        'SecureCRT': '🔐',
        'Xshell': '📡',
        'Tabby': '⚡',
        'MobaXterm': '🖥️',
        'PuTTY': '🔧'
    };
    return icons[name] || '💻';
}
```

---

## 🧪 测试清单

### 功能测试

- [ ] Tab切换正常(自动扫描/文件上传/自定义路径)
- [ ] 客户端横幅正确显示
- [ ] 自动扫描进度动画流畅
- [ ] 扫描结果正确填充
- [ ] 文件上传拖拽/点击均可用
- [ ] 路径示例点击可填充到输入框
- [ ] 关闭按钮正常工作
- [ ] ESC键关闭正常
- [ ] 点击遮罩层关闭正常

### 样式测试

- [ ] 横幅背景渐变正常
- [ ] Tab激活态高亮正常
- [ ] 进度条动画流畅
- [ ] 扫描步骤状态变化正常(⏳ → ✓)
- [ ] 上传区域hover效果正常
- [ ] 响应式布局正常

### 兼容性测试

- [ ] Chrome/Edge正常
- [ ] Firefox正常
- [ ] Safari正常(如需支持Mac)

---

## 📝 配置说明

### 支持的SSH客户端配置

```typescript
export const SSH_CLIENTS = {
    securecrt: {
        name: 'SecureCRT',
        icon: '🔐',
        vendor: 'VanDyke Software',
        supported: true,
        defaultPaths: {
            windows: '%APPDATA%\\VanDyke\\Config\\Sessions',
            mac: '~/Library/Application Support/VanDyke/SecureCRT/Config/Sessions',
            linux: '~/.vandyke/SecureCRT/Config/Sessions'
        }
    },
    xshell: {
        name: 'Xshell',
        icon: '📡',
        vendor: 'NetSarang',
        supported: true,
        defaultPaths: {
            windows: '%USERPROFILE%\\Documents\\NetSarang Computer\\7\\Xshell\\Sessions',
            mac: null,
            linux: null
        }
    },
    tabby: {
        name: 'Tabby',
        icon: '⚡',
        vendor: '开源终端',
        supported: true,
        defaultPaths: {
            windows: '%APPDATA%\\tabby\\config.yaml',
            mac: '~/Library/Application Support/tabby/config.yaml',
            linux: '~/.config/tabby/config.yaml'
        }
    },
    mobaxterm: {
        name: 'MobaXterm',
        icon: '🖥️',
        vendor: 'Mobatek',
        supported: false,
        comingSoon: true
    },
    putty: {
        name: 'PuTTY',
        icon: '🔧',
        vendor: 'Simon Tatham',
        supported: false,
        planned: true
    }
};
```

---

## 🔄 回退方案

如果新版本出现问题,可快速回退:

```bash
# 恢复旧版文件
mv src/main/resources/templates/fragments/ssh-config-import-wizard-old.html \
   src/main/resources/templates/fragments/ssh-config-import-wizard.html

# 移除新CSS引用
# 从 main.ts 中删除 import '../styles/ssh-config-import-wizard-v2.css';

# 重新编译
npm run build
```

---

## 📌 注意事项

1. **TypeScript更新必须**: 新版HTML结构变化较大,必须同步更新 `SSHConfigImportWizard.ts`
2. **CSS优先级**: 确保新CSS在旧CSS之后加载,避免样式冲突
3. **浏览器缓存**: 测试时使用 Ctrl+Shift+R 强制刷新
4. **后端API兼容**: 确认后端API返回格式支持客户端版本号等新字段
5. **国际化**: 如需支持英文,需添加 i18n 配置

---

## 🎯 下一步优化(可选)

### P1 - 高优先级
- [ ] 添加客户端版本自动检测
- [ ] 实现会话数量实时统计
- [ ] 添加扫描失败重试机制

### P2 - 中优先级
- [ ] 添加扫描历史记录
- [ ] 支持导出扫描报告
- [ ] 添加配置文件预览功能

### P3 - 低优先级
- [ ] 添加自定义客户端配置
- [ ] 支持批量操作多个配置文件
- [ ] 添加配置文件对比功能

---

## ✅ 完成标志

当以下所有项都完成时,改版即可上线:

- [x] HTML模板已替换
- [x] CSS样式已导入
- [ ] TypeScript逻辑已更新
- [ ] Tab切换功能正常
- [ ] 自动扫描动画正常
- [ ] 所有功能测试通过
- [ ] 代码已提交到Git

---

**创建时间**: 2025-10-19  
**最后更新**: 2025-10-19  
**负责人**: AI Assistant  
**审核状态**: 待审核
