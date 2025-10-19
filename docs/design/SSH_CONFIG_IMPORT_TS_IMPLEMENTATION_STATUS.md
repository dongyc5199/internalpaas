# SSH配置导入Tab版本 - 实施进度

## 📝 实施摘要

将 `SSHConfigImportWizard.ts` 从3步向导改造为Tab切换模式,配合新HTML模板。

---

## 🎯 已完成的准备工作

### ✅ 文档准备
- [x] 创建迁移指南: `SSH_CONFIG_IMPORT_V2_MIGRATION_GUIDE.md`
- [x] 创建更新计划: `SSH_CONFIG_IMPORT_TS_UPDATE_PLAN.md`
- [x] 创建设计对比: `SSH_CONFIG_IMPORT_COMPARISON.md`

### ✅ HTML/CSS准备
- [x] 创建新HTML模板: `ssh-config-import-wizard-v2.html` (274行)
- [x] 创建新CSS样式: `ssh-config-import-wizard-v2.css` (659行)

---

## 🔧 TypeScript更新步骤

### Step 1: 更新类属性 ⏳

**位置**: Line ~110-120  
**操作**: 
- [ ] 移除 `private step: number = 1;`
- [ ] 添加 `private currentTab: 'auto-scan' | 'upload' | 'manual' = 'auto-scan';`

```typescript
export class SSHConfigImportWizard {
    private modal: HTMLElement | null = null;
    // private step: number = 1; // ❌ 移除
    private currentTab: 'auto-scan' | 'upload' | 'manual' = 'auto-scan'; // ✅ 新增
    private servers: ServerImportPreview[] = [];
    private selectedServers: ServerImportPreview[] = [];
    // ...
}
```

---

### Step 2: 更新 `attachEvents()` 方法 ⏳

**位置**: Line 213-235  
**操作**: 添加Tab切换、扫描、上传、路径示例等事件监听

**当前代码**:
```typescript
private attachEvents() {
    if (!this.modal) return;
    if (this.modal.hasAttribute("data-ssh-config-events-attached")) {
        return;
    }
    console.log("Attaching events to SSH Config Import Modal...");
    
    // TODO: 后续步骤会添加具体事件绑定
    
    this.modal.setAttribute("data-ssh-config-events-attached", "true");
}
```

**需要更新为**: 完整的Tab事件绑定 (参考更新计划Step 2)

---

### Step 3: 添加 `switchTab()` 方法 ⏳

**位置**: 在 `attachEvents()` 之后插入  
**操作**: 添加新方法

```typescript
/**
 * 切换Tab
 */
private switchTab(tabName: 'auto-scan' | 'upload' | 'manual') {
    console.log(`Switching to tab: ${tabName}`);
    this.currentTab = tabName;
    
    // 更新Tab按钮状态
    const tabs = this.modal?.querySelectorAll<HTMLButtonElement>('.import-tab');
    tabs?.forEach(tab => {
        if (tab.dataset.tab === tabName) {
            tab.classList.add('active');
        } else {
            tab.classList.remove('active');
        }
    });
    
    // 更新Tab内容显示
    const contents = this.modal?.querySelectorAll<HTMLDivElement>('.tab-content');
    contents?.forEach(content => {
        if (content.id === tabName) {
            content.classList.add('active');
        } else {
            content.classList.remove('active');
        }
    });
}
```

---

### Step 4: 添加 `startAutoScan()` 方法 ⏳

**位置**: 与现有 `scanLocalConfig()` 方法并列  
**操作**: 添加带进度动画的自动扫描方法

这个方法会:
1. 显示4步扫描进度动画
2. 调用后端API `/api/ssh-config-import/scan-local`
3. 显示扫描结果

---

### Step 5: 添加辅助方法 ⏳

添加以下方法:
- [ ] `displayScanResult(result: SSHConfigParseResult)` - 显示扫描结果
- [ ] `guessClientName(hostname: string)` - 猜测客户端名称
- [ ] `getClientIcon(clientName: string)` - 获取客户端图标
- [ ] `groupByClient(servers)` - 按客户端分组

---

### Step 6: 更新 `handleFileUpload()` 方法 ⏳

**位置**: Line 404  
**操作**: 移除Step切换逻辑,改为在当前Tab显示结果

**需要移除**:
```typescript
// 6. 进入步骤2（预览）
this.step = 2;
this.updateStepDisplay();
```

**需要添加**: 在上传Tab显示结果的逻辑

---

### Step 7: 更新 `scanLocalConfig()` 方法 ⏳

**位置**: Line 493  
**操作**: 类似文件上传,移除Step切换逻辑

---

### Step 8: 更新 `parseCustomPath()` 方法 ⏳

**位置**: Line 579  
**操作**: 移除Step切换,在当前Tab显示结果

---

### Step 9: 添加 `confirmImport()` 方法 ⏳

**位置**: 新方法  
**操作**: 处理确认导入按钮点击

这个方法会:
1. 检查是否有选中的服务器
2. 调用后端API `/api/ssh-config-import/confirm`
3. 显示导入结果
4. 关闭模态框并触发事件

---

### Step 10: 移除旧方法 ⏳

**位置**: 
- Line 324: `updateStepDisplay()`
- Line 379: `nextStep()`
- Line 389: `prevStep()`

**操作**: 完全删除这3个方法

---

### Step 11: 更新 `reset()` 方法 ⏳

**位置**: Line 306  
**当前代码**:
```typescript
private reset() {
    this.step = 1; // ❌ 需要移除
    this.servers = [];
    this.selectedServers = [];
    // ...
}
```

**需要更新为**:
```typescript
private reset() {
    this.currentTab = 'auto-scan';
    this.switchTab('auto-scan');
    this.servers = [];
    this.selectedServers = [];
    // ...清空所有结果显示
}
```

---

## ✅ 完成验证

更新完成后,运行以下命令测试:

```bash
# 编译TypeScript
npm run build

# 检查编译错误
# 应该没有任何错误

# 浏览器测试
# 1. 打开弹窗
# 2. Tab切换正常
# 3. 自动扫描有进度动画
# 4. 文件上传拖拽正常
# 5. 自定义路径解析正常
```

---

## 📊 工作量估算

| 任务 | 预计时间 | 难度 |
|------|----------|------|
| Step 1: 更新属性 | 5分钟 | 低 |
| Step 2: 更新attachEvents | 30分钟 | 中 |
| Step 3: 添加switchTab | 10分钟 | 低 |
| Step 4: 添加startAutoScan | 40分钟 | 高 |
| Step 5: 添加辅助方法 | 30分钟 | 中 |
| Step 6-8: 更新现有方法 | 45分钟 | 中 |
| Step 9: 添加confirmImport | 20分钟 | 低 |
| Step 10: 删除旧方法 | 5分钟 | 低 |
| Step 11: 更新reset | 10分钟 | 低 |
| **总计** | **3小时15分钟** | - |

---

## ⚠️ 注意事项

1. **向后兼容**: 如果现有代码调用了 `nextStep()`/`prevStep()`,需要先确认没有外部引用
2. **DOM选择器**: 确保新HTML模板的ID与TypeScript中的选择器匹配
3. **API接口**: 确认后端API `/api/ssh-config-import/*` 返回格式支持新需求
4. **事件总线**: 确保 `eventBus.emit('server:imported')` 事件监听方正常工作

---

## 🚀 下一步

确认以上计划后,我将开始执行更新。您可以选择:

1. **一次性全部更新**: 我执行所有11个步骤,最后一起测试
2. **分步更新并测试**: 每完成2-3个步骤,编译一次确保无错误
3. **先创建备份文件**: 复制一份 `SSHConfigImportWizard.ts` 为 `SSHConfigImportWizard-old.ts`

**建议**: 选择方案3 (先备份) + 方案1 (一次性更新)

您希望我如何继续?
