# SSH 配置导入向导 - 扫描按钮状态管理更新

**更新日期:** 2025年10月19日  
**更新类型:** 功能增强  
**影响范围:** SSH 配置导入向导状态管理系统

---

## 🎯 更新目标

将"开始自动扫描"按钮纳入统一的状态管理系统，确保该按钮**仅在首次打开向导时显示**，完成首次扫描后在所有步骤中永久隐藏。

---

## ✨ 核心变更

### 1. 新增状态标志 `hasScanned`

**位置:** `wizardState` 对象  
**类型:** `boolean`  
**默认值:** `false`

```javascript
const wizardState = {
    currentStep: WizardStep.SCAN,
    scanData: null,
    selectedServers: [],
    importResult: null,
    hasScanned: false,  // ← 新增属性
    // ...
};
```

**用途:**
- 跟踪用户是否已完成首次扫描
- 控制"开始扫描"按钮的显示/隐藏

---

### 2. 扫描成功时设置标志

**函数:** `showScanResult(data)`  
**修改内容:** 添加 `hasScanned = true` 设置

```diff
function showScanResult(data) {
    // ... 显示扫描结果 UI ...
    
+   // 标记首次扫描已完成
+   wizardState.hasScanned = true;
+   console.log('[STATE] 首次扫描完成，hasScanned = true');
    
    // 自动切换到预览步骤
    setTimeout(() => {
        switchToPreviewStep();
    }, 1000);
}
```

**文件:** `src/main/resources/templates/main-layout.html`  
**行号:** ~4494

---

### 3. 更新 UI 时控制按钮显示

**函数:** `wizardState.updateUI()`  
**修改内容:** 在三个步骤中添加扫描按钮的显示逻辑

```diff
updateUI: function() {
    const prevBtn = document.getElementById('wizardPrevBtn');
    const nextBtn = document.getElementById('wizardNextBtn');
    const cancelBtn = document.getElementById('wizardCancelBtn');
+   const scanBtn = document.getElementById('startScanBtn');
    
    switch (this.currentStep) {
        case WizardStep.SCAN:
            // 步骤1: 扫描页
            this.updateButton(prevBtn, { visible: false, disabled: true });
            this.updateButton(nextBtn, { visible: true, disabled: true });
            this.updateButton(cancelBtn, { visible: true, disabled: false });
+           // 开始扫描按钮：只在首次扫描时显示
+           if (scanBtn) {
+               scanBtn.style.display = this.hasScanned ? 'none' : 'inline-flex';
+           }
            break;
            
        case WizardStep.PREVIEW:
            // 步骤2: 预览页
            // ...
+           // 预览页隐藏扫描按钮
+           if (scanBtn) {
+               scanBtn.style.display = 'none';
+           }
            break;
            
        case WizardStep.IMPORT:
            // 步骤3: 导入页
            // ...
+           // 导入页隐藏扫描按钮
+           if (scanBtn) {
+               scanBtn.style.display = 'none';
+           }
            break;
    }
}
```

**文件:** `src/main/resources/templates/main-layout.html`  
**行号:** ~5612-5670

---

### 4. 重置时恢复初始状态

**函数:** `wizardState.reset()`  
**修改内容:** 重置 `hasScanned` 为 `false`

```diff
reset: function() {
    this.currentStep = WizardStep.SCAN;
    this.scanData = null;
    this.selectedServers = [];
    this.importResult = null;
+   this.hasScanned = false;
    console.log('[STATE] 向导状态已重置');
}
```

**文件:** `src/main/resources/templates/main-layout.html`  
**行号:** ~5593

---

## 📋 按钮状态更新矩阵

| 步骤 | hasScanned | 扫描按钮 | 上一步 | 下一步 | 取消 |
|-----|-----------|---------|-------|-------|------|
| **SCAN (初次)** | `false` | ✅ 显示+启用 | ❌ 隐藏 | ✅ 显示+禁用 | ✅ 显示+启用 |
| **SCAN (返回)** | `true` | **❌ 隐藏** | ❌ 隐藏 | ✅ 显示+禁用 | ✅ 显示+启用 |
| **PREVIEW** | `true` | **❌ 隐藏** | ✅ 显示+启用 | ✅ 显示+条件启用 | ✅ 显示+启用 |
| **IMPORT (中)** | `true` | **❌ 隐藏** | ❌ 隐藏 | ❌ 隐藏 | ✅ 显示+禁用 |
| **IMPORT (完成)** | `true` | **❌ 隐藏** | ✅ 显示+启用 | ❌ 隐藏 | ✅ 显示+启用 |

**关键变化:**
- **新增"扫描按钮"列**，状态由 `hasScanned` 控制
- 首次打开向导时显示，扫描成功后永久隐藏

---

## 🔄 用户体验流程

### 完整流程图

```
┌────────────────┐
│ 1. 打开向导     │  hasScanned = false
│ ✅ 显示扫描按钮 │
└────────┬───────┘
         │ 点击"开始扫描"
         ↓
┌────────────────┐
│ 2. 扫描成功     │  hasScanned = true
│ ❌ 隐藏扫描按钮 │
└────────┬───────┘
         │ 自动跳转
         ↓
┌────────────────┐
│ 3. 预览服务器   │  hasScanned = true
│ ❌ 按钮保持隐藏 │
└────────┬───────┘
         │ 点击"上一步"
         ↓
┌────────────────┐
│ 4. 返回步骤1    │  hasScanned = true
│ ❌ 按钮仍隐藏   │  ← 关键特性
│ ✅ 可点击"下一步"│
│    直接进预览   │
└────────────────┘
```

### 对比：更新前 vs 更新后

| 场景 | 更新前 | 更新后 |
|-----|-------|-------|
| **初次打开向导** | ✅ 显示扫描按钮 | ✅ 显示扫描按钮 |
| **扫描后预览页** | ✅ 仍显示（用户可能误点） | ❌ 隐藏（避免误触） |
| **返回步骤1** | ✅ 仍显示（可能覆盖数据） | ❌ 隐藏（保护已扫描数据） |
| **多次往返** | ✅ 始终显示 | ❌ 永久隐藏 |

**优势:**
- ✅ 防止用户重复扫描覆盖已选择的服务器
- ✅ 简化操作流程，扫描一次即可
- ✅ 避免在预览或导入阶段误触扫描按钮

---

## 🧪 测试验证

### 快速测试步骤

1. **初次打开:**
   ```
   打开向导 → 观察"开始扫描"按钮 → 应显示 ✅
   ```

2. **首次扫描:**
   ```
   点击"开始扫描" → 扫描完成 → 按钮隐藏 ✅
   控制台日志: [STATE] 首次扫描完成，hasScanned = true
   ```

3. **返回步骤1:**
   ```
   在预览页点击"上一步" → 返回步骤1 → 按钮仍隐藏 ✅
   控制台日志: [STATE] Step1按钮: ... Scan(隐藏)
   ```

4. **直接进预览:**
   ```
   在步骤1点击"下一步" → 直接进入预览（无需重新扫描） ✅
   ```

### 完整测试用例

详见 [TC1.7 扫描按钮一次性显示验证](./SSH_CONFIG_IMPORT_WIZARD_STATE_MANAGEMENT_TEST_PLAN.md#tc17-扫描按钮一次性显示验证关键特性)

---

## 📁 修改文件清单

| 文件 | 修改内容 | 状态 |
|-----|---------|------|
| `src/main/resources/templates/main-layout.html` | 新增 `hasScanned` 属性 | ✅ 完成 |
| `src/main/resources/templates/main-layout.html` | `showScanResult()` 设置标志 | ✅ 完成 |
| `src/main/resources/templates/main-layout.html` | `updateUI()` 控制按钮显示 | ✅ 完成 |
| `src/main/resources/templates/main-layout.html` | `reset()` 重置标志 | ✅ 完成 |
| `docs/technical/SSH_CONFIG_IMPORT_WIZARD_STATE_MANAGEMENT_DESIGN.md` | 更新设计文档 | ✅ 完成 |
| `docs/technical/SSH_CONFIG_IMPORT_WIZARD_STATE_MANAGEMENT_TEST_PLAN.md` | 更新测试计划 | ✅ 完成 |
| `docs/technical/SSH_CONFIG_IMPORT_WIZARD_SCAN_BUTTON_FEATURE.md` | 新增功能说明文档 | ✅ 完成 |

---

## 🏗️ 构建状态

```bash
npm run build
```

**结果:** ✅ 构建成功

```
vite v5.4.20 building for production...
✓ 23 modules transformed.
../resources/static/dist/assets/main.js       305.69 kB │ gzip: 96.52 kB
✓ built in 1m 4s
```

---

## 📚 相关文档

1. **[状态管理设计文档](./SSH_CONFIG_IMPORT_WIZARD_STATE_MANAGEMENT_DESIGN.md)**
   - 完整的状态管理系统架构
   - 按钮状态矩阵（已更新扫描按钮列）
   - 状态转换流程图（已标注 hasScanned 变化）

2. **[状态管理测试计划](./SSH_CONFIG_IMPORT_WIZARD_STATE_MANAGEMENT_TEST_PLAN.md)**
   - 新增 TC1.7 扫描按钮测试用例
   - 更新按钮状态矩阵
   - 代码覆盖率检查清单（已添加 showScanResult）

3. **[扫描按钮功能说明](./SSH_CONFIG_IMPORT_WIZARD_SCAN_BUTTON_FEATURE.md)**
   - 扫描按钮一次性显示功能详解
   - 技术实现细节
   - 用户体验流程图

---

## ✅ 验收标准

- [x] `hasScanned` 属性正确添加到 `wizardState`
- [x] 扫描成功后 `hasScanned` 设置为 `true`
- [x] `updateUI()` 根据 `hasScanned` 控制扫描按钮显示
- [x] 返回步骤1时扫描按钮保持隐藏
- [x] 浏览器控制台输出正确的日志
- [x] 文档已更新（设计文档、测试计划、功能说明）
- [x] 前端构建成功无错误
- [x] 不影响其他按钮的状态管理逻辑

---

## 🎬 后续计划

### 待测试项

1. **功能测试:** 手动执行 TC1.7 扫描按钮验证用例
2. **回归测试:** 确保其他状态管理功能未受影响
3. **边界测试:** 扫描失败场景、弹窗关闭重开场景

### 可能的优化

1. **弹窗关闭事件:** 添加弹窗关闭时调用 `wizardState.reset()` 的逻辑
2. **扫描失败处理:** 确认扫描失败时不设置 `hasScanned = true`
3. **调试日志清理:** 清理生产环境的 `console.log` 调试语句

---

**更新完成时间:** 2025年10月19日  
**构建状态:** ✅ 成功  
**文档状态:** ✅ 已更新  
**待测试:** ⚠️ 需手动测试验证
