# SSH 配置导入向导 - 扫描按钮一次性显示功能

## 功能概述

**需求:** "开始自动扫描"按钮只在首次打开向导时显示，完成首次扫描后在所有步骤中永久隐藏。

**目标:**
- ✅ 避免用户重复扫描覆盖已选择的服务器
- ✅ 简化操作流程，用户只需扫描一次
- ✅ 防止在预览或导入阶段误触扫描按钮

## 技术实现

### 1. 状态标志

在 `wizardState` 中新增 `hasScanned` 布尔属性：

```javascript
const wizardState = {
    currentStep: WizardStep.SCAN,
    scanData: null,
    selectedServers: [],
    importResult: null,
    hasScanned: false,  // ← 新增：是否已完成首次扫描
    
    reset: function() {
        // ...
        this.hasScanned = false;  // 重置时恢复为 false
    }
};
```

### 2. 扫描成功时设置标志

在 `showScanResult()` 函数中，扫描成功后设置 `hasScanned = true`：

```javascript
function showScanResult(data) {
    // ... 显示扫描结果 UI ...
    
    // 标记首次扫描已完成
    wizardState.hasScanned = true;
    console.log('[STATE] 首次扫描完成，hasScanned = true');
    
    // 自动切换到预览步骤
    setTimeout(() => {
        switchToPreviewStep();
    }, 1000);
}
```

**文件位置:** `src/main/resources/templates/main-layout.html` 约 4485 行

### 3. 更新 UI 时控制按钮显示

在 `wizardState.updateUI()` 中检查 `hasScanned` 状态：

```javascript
updateUI: function() {
    const prevBtn = document.getElementById('wizardPrevBtn');
    const nextBtn = document.getElementById('wizardNextBtn');
    const cancelBtn = document.getElementById('wizardCancelBtn');
    const scanBtn = document.getElementById('startScanBtn');  // ← 获取扫描按钮
    
    switch (this.currentStep) {
        case WizardStep.SCAN:
            // 步骤1: 扫描页
            this.updateButton(prevBtn, { visible: false, disabled: true });
            this.updateButton(nextBtn, { visible: true, disabled: true });
            this.updateButton(cancelBtn, { visible: true, disabled: false });
            
            // 开始扫描按钮：只在首次扫描时显示
            if (scanBtn) {
                scanBtn.style.display = this.hasScanned ? 'none' : 'inline-flex';
            }
            break;
            
        case WizardStep.PREVIEW:
        case WizardStep.IMPORT:
            // 预览和导入页：隐藏扫描按钮
            if (scanBtn) {
                scanBtn.style.display = 'none';
            }
            break;
    }
}
```

**文件位置:** `src/main/resources/templates/main-layout.html` 约 5612-5670 行

## 按钮显示逻辑表

| 步骤 | hasScanned | 扫描按钮显示 |
|-----|-----------|------------|
| **SCAN（初次）** | `false` | ✅ `display: inline-flex` |
| **SCAN（返回）** | `true` | ❌ `display: none` |
| **PREVIEW** | `true` | ❌ `display: none` |
| **IMPORT** | `true` | ❌ `display: none` |

## 用户体验流程

```
┌─────────────────────────────────┐
│ 1. 打开向导（hasScanned = false） │
│    ✅ 显示"开始扫描"按钮           │
└──────────────┬──────────────────┘
               │ 用户点击扫描
               ↓
┌─────────────────────────────────┐
│ 2. 扫描成功（hasScanned = true）  │
│    ❌ 隐藏"开始扫描"按钮           │
└──────────────┬──────────────────┘
               │ 自动跳转预览
               ↓
┌─────────────────────────────────┐
│ 3. 预览服务器列表                 │
│    ❌ 扫描按钮保持隐藏             │
└──────────────┬──────────────────┘
               │ 用户点击"上一步"
               ↓
┌─────────────────────────────────┐
│ 4. 返回步骤1（hasScanned = true） │
│    ❌ 扫描按钮仍隐藏（永久）       │
│    ✅ 可点击"下一步"直接进入预览   │
└─────────────────────────────────┘
```

## 测试验证

### 测试步骤

1. **初次打开:**
   - 打开向导 → 观察"开始扫描"按钮 → **应显示**
   
2. **首次扫描:**
   - 点击"开始扫描" → 等待扫描完成 → **按钮隐藏**
   
3. **返回步骤1:**
   - 在预览页点击"上一步" → **按钮保持隐藏**
   
4. **再次进入预览:**
   - 点击"下一步" → **直接进入预览（无需重新扫描）**

### 浏览器控制台验证

扫描成功后应看到日志：

```
[STATE] 首次扫描完成，hasScanned = true
[STATE] 步骤切换: scan → preview
[STATE] 更新UI，当前步骤: preview
[STATE] Step2按钮: Prev(启用) Next(根据选择) Cancel(启用) Scan(隐藏)
```

返回步骤1时应看到：

```
[STATE] 步骤切换: preview → scan
[STATE] 更新UI，当前步骤: scan
[STATE] Step1按钮: Prev(隐藏) Next(禁用) Cancel(启用) Scan(隐藏)
```

## 边界情况

### 1. 关闭弹窗后重新打开

**行为:** 如果弹窗完全关闭后重新打开，`wizardState` 应重置，`hasScanned` 恢复为 `false`，扫描按钮重新显示。

**实现:** 在弹窗关闭事件中调用 `wizardState.reset()`（待实现）

### 2. 扫描失败场景

**行为:** 如果扫描失败，`hasScanned` 保持 `false`，用户可以再次点击扫描按钮。

**实现:** `showScanResult()` 仅在成功时调用，失败时不设置 `hasScanned = true`

### 3. 多次往返步骤

**行为:** 无论用户如何在步骤间切换，`hasScanned` 始终保持 `true`，扫描按钮不再显示。

**实现:** `hasScanned` 只在 `reset()` 或首次扫描成功时修改

## 代码修改文件

| 文件 | 修改内容 | 行号 |
|-----|---------|------|
| `main-layout.html` | 新增 `wizardState.hasScanned` 属性 | ~5586 |
| `main-layout.html` | `reset()` 中重置 `hasScanned` | ~5593 |
| `main-layout.html` | `showScanResult()` 中设置 `hasScanned = true` | ~4494 |
| `main-layout.html` | `updateUI()` 中控制扫描按钮显示 | ~5625, 5645, 5661 |

## 相关文档

- [状态管理设计文档](./SSH_CONFIG_IMPORT_WIZARD_STATE_MANAGEMENT_DESIGN.md)
- [状态管理测试计划](./SSH_CONFIG_IMPORT_WIZARD_STATE_MANAGEMENT_TEST_PLAN.md)

---

**版本:** 1.0  
**创建日期:** 2025年10月19日  
**最后更新:** 扫描按钮一次性显示功能完成  
**状态:** ✅ 已实现并构建成功
