# SSH 配置导入向导状态管理设计

## 设计目标

统一管理向导弹窗的三个步骤（扫描、预览、导入）之间的状态转换，并集中控制底部三个操作按钮（上一步、下一步、取消）的可见性和启用/禁用状态。

## 核心架构

### 1. 状态枚举（WizardStep）

```javascript
const WizardStep = {
    SCAN: 'scan',      // 步骤1：扫描服务器
    PREVIEW: 'preview', // 步骤2：预览并选择服务器
    IMPORT: 'import'    // 步骤3：执行导入操作
};
```

### 2. 状态管理对象（wizardState）

```javascript
const wizardState = {
    // ========== 状态属性 ==========
    currentStep: WizardStep.SCAN,  // 当前所在步骤
    scanData: null,                // 扫描结果原始数据
    selectedServers: [],           // 用户选中的服务器列表
    importResult: null,            // 导入结果（成功/失败详情）
    hasScanned: false,             // 是否已完成首次扫描
    
    // ========== 核心方法 ==========
    
    /**
     * 重置所有状态到初始值
     */
    reset: function() {
        this.currentStep = WizardStep.SCAN;
        this.scanData = null;
        this.selectedServers = [];
        this.importResult = null;
        this.hasScanned = false;
        this.updateUI();
    },
    
    /**
     * 切换到指定步骤并更新 UI
     * @param {string} step - WizardStep 枚举值
     */
    setStep: function(step) {
        console.log('[DEBUG] 状态切换到:', step);
        this.currentStep = step;
        this.updateUI();
    },
    
    /**
     * 根据当前状态更新所有按钮的显示和启用状态
     */
    updateUI: function() {
        const prevBtn = document.getElementById('wizardPrevBtn');
        const nextBtn = document.getElementById('wizardNextBtn');
        const cancelBtn = document.getElementById('wizardCancelBtn');
        const scanBtn = document.getElementById('startScanBtn');
        
        switch (this.currentStep) {
            case WizardStep.SCAN:
                // 步骤1：扫描阶段
                this.updateButton(prevBtn, false, false);  // 隐藏 + 禁用
                this.updateButton(nextBtn, true, false);   // 显示 + 禁用
                this.updateButton(cancelBtn, true, true);  // 显示 + 启用
                // 开始扫描按钮：只在首次扫描时显示
                if (scanBtn) {
                    scanBtn.style.display = this.hasScanned ? 'none' : 'inline-flex';
                }
                break;
                
            case WizardStep.PREVIEW:
                // 步骤2：预览阶段
                this.updateButton(prevBtn, true, true);    // 显示 + 启用
                // 下一步按钮根据选中数量动态控制（由 updateSelectedServers 管理）
                const hasSelection = this.selectedServers.length > 0;
                this.updateButton(nextBtn, true, hasSelection); // 显示 + 条件启用
                this.updateButton(cancelBtn, true, true);  // 显示 + 启用
                // 预览页隐藏扫描按钮
                if (scanBtn) {
                    scanBtn.style.display = 'none';
                }
                break;
                
            case WizardStep.IMPORT:
                // 步骤3：导入阶段
                if (this.importResult === null) {
                    // 导入进行中
                    this.updateButton(prevBtn, false, false);  // 隐藏 + 禁用
                    this.updateButton(nextBtn, false, false);  // 隐藏 + 禁用
                    this.updateButton(cancelBtn, true, false); // 显示 + 禁用
                } else {
                    // 导入已完成（成功或失败）
                    this.updateButton(prevBtn, true, true);    // 显示 + 启用
                    this.updateButton(nextBtn, false, false);  // 隐藏 + 禁用
                    this.updateButton(cancelBtn, true, true);  // 显示 + 启用
                }
                // 导入页隐藏扫描按钮
                if (scanBtn) {
                    scanBtn.style.display = 'none';
                }
                break;
        }
    },
    
    /**
     * 更新单个按钮的状态
     * @param {HTMLElement} button - 按钮 DOM 元素
     * @param {boolean} show - 是否显示
     * @param {boolean} enabled - 是否启用
     */
    updateButton: function(button, show, enabled) {
        if (!button) return;
        button.style.display = show ? 'inline-flex' : 'none';
        button.disabled = !enabled;
    },
    
    /**
     * 更新选中的服务器列表（步骤2专用）
     * @param {Array} servers - 选中的服务器元素数组
     */
    updateSelectedServers: function(servers) {
        this.selectedServers = servers;
        
        const nextBtn = document.getElementById('wizardNextBtn');
        if (nextBtn && this.currentStep === WizardStep.PREVIEW) {
            const count = servers.length;
            nextBtn.disabled = count === 0;
            nextBtn.textContent = count > 0 ? `下一步 (${count})` : '下一步';
        }
    },
    
    /**
     * 设置导入结果（步骤3专用）
     * @param {Object} result - 导入结果对象（包含 successCount, failedCount 等）
     */
    setImportResult: function(result) {
        this.importResult = result;
        this.updateUI(); // 导入完成后更新按钮状态
    }
};
```

## 按钮状态矩阵

| 步骤 | 阶段说明 | 上一步按钮 | 下一步按钮 | 取消按钮 | **开始扫描按钮** |
|------|---------|-----------|-----------|---------|----------------|
| **SCAN**<br>(首次) | 初始扫描界面<br>未进行过扫描 | ❌ 隐藏<br>🔒 禁用 | ✅ 显示<br>🔒 禁用 | ✅ 显示<br>✅ 启用 | **✅ 显示**<br>**✅ 启用** |
| **SCAN**<br>(返回) | 扫描界面<br>已完成过扫描 | ❌ 隐藏<br>🔒 禁用 | ✅ 显示<br>🔒 禁用 | ✅ 显示<br>✅ 启用 | **❌ 隐藏** |
| **PREVIEW**<br>(0 台选中) | 预览服务器列表<br>未选择任何服务器 | ✅ 显示<br>✅ 启用 | ✅ 显示<br>🔒 禁用 | ✅ 显示<br>✅ 启用 | **❌ 隐藏** |
| **PREVIEW**<br>(>0 台选中) | 预览服务器列表<br>已选择 N 台服务器 | ✅ 显示<br>✅ 启用 | ✅ 显示<br>✅ 启用<br>📝 "下一步 (N)" | ✅ 显示<br>✅ 启用 | **❌ 隐藏** |
| **IMPORT**<br>(进行中) | 正在执行导入操作 | ❌ 隐藏<br>🔒 禁用 | ❌ 隐藏<br>🔒 禁用 | ✅ 显示<br>🔒 禁用 | **❌ 隐藏** |
| **IMPORT**<br>(已完成) | 导入成功或失败 | ✅ 显示<br>✅ 启用 | ❌ 隐藏<br>🔒 禁用 | ✅ 显示<br>✅ 启用 | **❌ 隐藏** |

**图例:**
- ✅ 显示/启用
- ❌ 隐藏
- 🔒 禁用
- 📝 动态文字

**关键设计要点:**
- **"开始扫描"按钮仅在首次打开向导时显示**
- 完成首次扫描后（`hasScanned = true`），该按钮在所有步骤都不再显示
- 用户只能在首次扫描后通过"上一步"/"下一步"按钮在步骤间导航
- 这避免了用户在预览或导入阶段误点扫描按钮导致状态混乱

## 状态转换流程图

```
┌─────────────┐
│   SCAN      │ 步骤1：扫描服务器 (hasScanned = false)
│  (初始状态)  │ 显示"开始扫描"按钮
└──────┬──────┘
       │ handleStartScan()
       │ → 调用 /api/ssh-scan/scan-all
       │ → showScanResult()
       │ → wizardState.hasScanned = true ← 标记首次扫描完成
       │ → populatePreviewTable()
       ↓
┌──────────────┐
│   PREVIEW    │ 步骤2：预览并选择服务器
│              │ 隐藏"开始扫描"按钮
└──┬────────┬──┘
   │        │
   │        └─→ handleWizardNextClick()
   │            → importSelectedServers()
   │            ↓
   │        ┌──────────────┐
   │        │   IMPORT     │ 步骤3a：导入进行中
   │        │ (进行中)      │ importResult = null
   │        └──────┬───────┘
   │               │ 导入完成
   │               ↓
   │        ┌──────────────┐
   │        │   IMPORT     │ 步骤3b：导入已完成
   │        │ (已完成)      │ importResult = {...}
   │        └──────┬───────┘
   │               │
   ↑←──────────────┘ wizardPrevBtn.click()
   │                → switchBackToPreviewStep()
   │                → 保持服务器列表和复选框状态
   │
   ↑
   │ wizardPrevBtn.click()
   │ → switchBackToScanStep()
   │
┌──┴───────────┐
│   SCAN       │ 返回步骤1 (hasScanned = true)
│              │ "开始扫描"按钮保持隐藏 ← 关键特性
└──────────────┘
```

## 关键设计决策

### 1. 数据持久化策略

**问题:** 导入失败后点击"上一步"返回预览页面时，如何保证服务器列表和复选框状态不丢失？

**解决方案:**
- ✅ **不重新渲染表格:** `switchBackToPreviewStep()` 仅切换容器 `display` 属性，不调用 `populatePreviewTable()`
- ✅ **DOM 元素保留:** 表格行和复选框的 DOM 元素完整保留，包括选中状态（`checked` 属性）
- ✅ **状态验证:** `wizardState.selectedServers` 数组保持不变，与 DOM 状态同步

```javascript
function switchBackToPreviewStep() {
    // 关键：仅切换容器显示，不重新渲染
    const wizardStep2 = document.getElementById('wizardStep2');
    if (wizardStep2) wizardStep2.style.display = 'block';
    
    const importProgressContainer = document.getElementById('importProgressContainer');
    if (importProgressContainer) importProgressContainer.style.display = 'none';
    
    // 更新状态（触发按钮更新）
    wizardState.setStep(WizardStep.PREVIEW);
}
```

### 2. 扫描按钮一次性显示策略

**问题:** 如何确保"开始扫描"按钮只在首次打开向导时显示，后续返回步骤1时不再显示？

**解决方案:**
- ✅ **hasScanned 标志:** 在 `wizardState` 中添加 `hasScanned` 布尔属性
- ✅ **扫描成功时设置:** 在 `showScanResult()` 中设置 `wizardState.hasScanned = true`
- ✅ **updateUI() 检查:** 在步骤1时检查 `hasScanned`，若为 `true` 则隐藏扫描按钮

```javascript
// 在 showScanResult() 中标记首次扫描
function showScanResult(data) {
    // ... 其他代码 ...
    
    // 标记首次扫描已完成
    wizardState.hasScanned = true;
    console.log('[STATE] 首次扫描完成，hasScanned = true');
    
    // 切换到预览步骤
    switchToPreviewStep();
}

// 在 updateUI() 中控制按钮显示
case WizardStep.SCAN:
    // 开始扫描按钮：只在首次扫描时显示
    if (scanBtn) {
        scanBtn.style.display = this.hasScanned ? 'none' : 'inline-flex';
    }
    break;
```

**用户体验优势:**
- ✅ 避免用户在预览或导入阶段误点扫描按钮
- ✅ 防止重复扫描覆盖已选择的服务器
- ✅ 简化操作流程，用户只需扫描一次
- ✅ 返回步骤1时通过"下一步"按钮直接进入预览（无需重新扫描）

### 3. 集中式按钮控制

**问题:** 之前按钮的显示/禁用逻辑分散在多个函数中，导致状态不一致。

**解决方案:**
- ✅ **单一职责原则:** 所有按钮状态由 `wizardState.updateUI()` 统一管理
- ✅ **状态驱动 UI:** 修改 `currentStep` 或 `importResult` 后自动触发 `updateUI()`
- ✅ **消除重复代码:** 删除各函数中的手动按钮操作代码

### 3. 步骤特定逻辑

#### 步骤2（PREVIEW）特殊处理

**选中数量联动:**
```javascript
updateSelectedServers: function(servers) {
    this.selectedServers = servers;
    
    // 仅在预览步骤更新下一步按钮
    if (this.currentStep === WizardStep.PREVIEW) {
        const count = servers.length;
        nextBtn.disabled = count === 0;
        nextBtn.textContent = count > 0 ? `下一步 (${count})` : '下一步';
    }
}
```

**调用位置:**
```javascript
function updateSelectionCount() {
    const selectedCheckboxes = previewTableBody.querySelectorAll('.row-checkbox:checked');
    wizardState.updateSelectedServers(Array.from(selectedCheckboxes));
}
```

#### 步骤3（IMPORT）特殊处理

**导入中 vs 导入完成:**
```javascript
case WizardStep.IMPORT:
    if (this.importResult === null) {
        // 导入进行中：所有按钮禁用/隐藏
        this.updateButton(prevBtn, false, false);
        this.updateButton(nextBtn, false, false);
        this.updateButton(cancelBtn, true, false);
    } else {
        // 导入完成：允许返回预览
        this.updateButton(prevBtn, true, true);
        this.updateButton(cancelBtn, true, true);
    }
    break;
```

**调用位置:**
```javascript
function showImportProgress() {
    wizardState.importResult = null; // 重置结果
    wizardState.setStep(WizardStep.IMPORT);
    // 开始导入...
}

function showImportResult(result) {
    // 显示结果 UI...
    wizardState.setImportResult(result); // 设置结果并触发按钮更新
}
```

## 集成清单

### 已集成的函数

以下函数已完成状态管理集成：

| 函数名 | 调用的 wizardState 方法 | 集成状态 |
|-------|------------------------|---------|
| `showScanResult()` | 设置 `hasScanned = true` | ✅ 完成 |
| `switchToPreviewStep()` | `setStep(WizardStep.PREVIEW)` | ✅ 完成 |
| `switchBackToScanStep()` | `setStep(WizardStep.SCAN)` | ✅ 完成 |
| `switchBackToPreviewStep()` | `setStep(WizardStep.PREVIEW)` | ✅ 完成 |
| `showImportProgress()` | `setStep(WizardStep.IMPORT)` + 设置 `importResult = null` | ✅ 完成 |
| `updateSelectionCount()` | `updateSelectedServers(servers)` | ✅ 完成 |
| `showImportResult()` | `setImportResult(result)` | ✅ 完成 |
| `showImportError()` | `setImportResult(result)` | ✅ 完成 |

### 事件绑定

```javascript
// 上一步按钮事件委托（document 级别）
document.addEventListener('click', function(event) {
    if (event.target && event.target.id === 'wizardPrevBtn') {
        const currentStep = wizardState.currentStep;
        
        if (currentStep === WizardStep.PREVIEW) {
            switchBackToScanStep();
        } else if (currentStep === WizardStep.IMPORT) {
            switchBackToPreviewStep(); // 保持数据
        }
    }
});

// 下一步按钮事件委托
document.addEventListener('click', function(event) {
    if (event.target && event.target.id === 'wizardNextBtn') {
        if (wizardState.currentStep === WizardStep.PREVIEW) {
            handleWizardNextClick();
        }
    }
});
```

## 测试验证要点

### 核心功能测试

1. **按钮状态正确性**
   - 每个步骤的按钮状态符合矩阵规则
   - 选中/取消选中服务器时下一步按钮动态响应

2. **数据持久化验证**
   - 导入失败 → 点击上一步 → 服务器列表保持不变
   - 复选框选中状态保留
   - 可以重新选择并再次尝试导入

3. **状态转换流畅性**
   - 步骤间切换无延迟
   - 按钮显示/隐藏无闪烁
   - 控制台无 JavaScript 错误

### 边界条件测试

- 扫描零台服务器
- 全选后取消全选
- 导入进行中尝试关闭弹窗
- 多次往返步骤1和步骤2

## 代码位置

**文件:** `src/main/resources/templates/main-layout.html`

**关键代码块:**
- **状态管理系统:** 约 5580-5670 行（`WizardStep` 枚举 + `wizardState` 对象）
- **步骤转换函数:** 约 4900-5100 行
- **事件委托:** 约 5700-5800 行
- **导入结果显示:** 约 5419-5555 行

## 维护建议

1. **新增步骤:** 在 `WizardStep` 枚举添加新值，在 `updateUI()` 添加对应 `case`
2. **修改按钮逻辑:** 仅修改 `updateUI()` 中的 `switch` 分支，不要在其他函数中直接操作按钮
3. **调试:** 启用 `console.log('[DEBUG] ...')` 查看状态转换日志
4. **测试:** 参考 `SSH_CONFIG_IMPORT_WIZARD_STATE_MANAGEMENT_TEST_PLAN.md` 执行完整测试

---

**文档版本:** 1.0  
**创建日期:** 2025年  
**最后更新:** 状态管理系统集成完成  
**相关文档:** `SSH_CONFIG_IMPORT_WIZARD_STATE_MANAGEMENT_TEST_PLAN.md`
