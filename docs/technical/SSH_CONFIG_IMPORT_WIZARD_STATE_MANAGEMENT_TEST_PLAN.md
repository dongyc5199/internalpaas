# SSH 配置导入向导状态管理测试计划

## 概述

本文档定义 SSH 配置导入向导的状态管理系统测试计划，确保按钮控制逻辑、数据持久化和步骤转换功能正确。

## 状态管理架构

### WizardStep 枚举

```javascript
const WizardStep = {
    SCAN: 'scan',      // 步骤1：扫描服务器
    PREVIEW: 'preview', // 步骤2：预览并选择
    IMPORT: 'import'    // 步骤3：执行导入
};
```

### wizardState 对象

```javascript
const wizardState = {
    currentStep: WizardStep.SCAN,
    scanData: null,           // 扫描结果原始数据
    selectedServers: [],      // 用户选中的服务器列表
    importResult: null,       // 导入结果（成功/失败）
    hasScanned: false,        // 是否已完成首次扫描
    
    reset(),                  // 重置所有状态（包括 hasScanned）
    setStep(step),           // 切换步骤并更新 UI
    updateUI(),              // 根据当前状态更新按钮显示
    updateButton(btn, show, enabled), // 更新单个按钮状态
    updateSelectedServers(servers),   // 更新选中列表并刷新按钮
    setImportResult(result)           // 设置导入结果并更新 UI
};
```

## 按钮状态矩阵

| 步骤 | 当前阶段 | 上一步按钮 | 下一步按钮 | 取消按钮 | **开始扫描按钮** |
|-----|---------|----------|----------|---------|----------------|
| **SCAN**<br>(首次) | 初始扫描<br>hasScanned=false | `display: none`<br>`disabled: true` | `display: inline-flex`<br>`disabled: true` | `display: inline-flex`<br>`disabled: false` | **`display: inline-flex`**<br>**`disabled: false`** |
| **SCAN**<br>(返回) | 返回扫描页<br>hasScanned=true | `display: none`<br>`disabled: true` | `display: inline-flex`<br>`disabled: true` | `display: inline-flex`<br>`disabled: false` | **`display: none`**<br>**已完成首次扫描** |
| **PREVIEW** | 0 台选中 | `display: inline-flex`<br>`disabled: false` | `display: inline-flex`<br>`disabled: true` | `display: inline-flex`<br>`disabled: false` | **`display: none`** |
| **PREVIEW** | >0 台选中 | `display: inline-flex`<br>`disabled: false` | `display: inline-flex`<br>`disabled: false` | `display: inline-flex`<br>`disabled: false` | **`display: none`** |
| **IMPORT** | 导入进行中 | `display: none`<br>`disabled: true` | `display: none`<br>`disabled: true` | `display: inline-flex`<br>`disabled: true` | **`display: none`** |
| **IMPORT** | 导入完成 | `display: inline-flex`<br>`disabled: false` | `display: none`<br>`disabled: true` | `display: inline-flex`<br>`disabled: false` | **`display: none`** |

## 测试用例

### 测试组 1: 步骤转换逻辑

#### TC1.1 初始状态验证

**前置条件:** 打开 SSH 配置导入向导  
**测试步骤:**
1. 观察弹窗初始状态

**预期结果:**
- ✅ 显示 "自动扫描" 标签页（步骤1）
- ✅ **"开始扫描"按钮显示且启用** ← 首次打开向导
- ✅ "上一步" 按钮隐藏（`display: none`）
- ✅ "下一步" 按钮显示但禁用（灰色）
- ✅ "取消" 按钮显示且启用

---

#### TC1.2 扫描到预览转换

**前置条件:** 在步骤1点击 "开始扫描" 并等待扫描完成  
**测试步骤:**
1. 点击 "开始扫描" 按钮
2. 观察按钮状态变化（扫描时应显示旋转动画）
3. 等待扫描完成

**预期结果:**
- ✅ 扫描期间按钮显示 `.scan-spinner-lite` 动画（20px 旋转圆圈）
- ✅ 扫描成功后自动切换到步骤2（预览服务器列表）
- ✅ **`wizardState.hasScanned` 设置为 `true`** ← 关键状态变更
- ✅ "自动扫描" 标签页隐藏（`display: none`）
- ✅ "预览服务器列表" 标签页显示（`display: block`）
- ✅ **"开始扫描"按钮隐藏（`display: none`）** ← 首次扫描后永久隐藏
- ✅ "上一步" 按钮显示且启用
- ✅ "下一步" 按钮显示但禁用（因为未选中任何服务器）
- ✅ "取消" 按钮显示且启用

---

#### TC1.3 预览选择与按钮联动

**前置条件:** 已在步骤2显示服务器列表  
**测试步骤:**
1. 点击第一行复选框选中一台服务器
2. 观察 "下一步" 按钮状态
3. 再选中第二台服务器
4. 取消选中所有服务器

**预期结果:**
- ✅ 选中第1台后："下一步" 按钮变为启用状态（蓝色），显示 "下一步 (1)"
- ✅ 选中第2台后：按钮文字更新为 "下一步 (2)"
- ✅ 取消所有选中后："下一步" 按钮变为禁用状态，文字恢复为 "下一步"

---

#### TC1.4 从预览返回扫描

**前置条件:** 在步骤2（预览服务器列表）  
**测试步骤:**
1. 点击 "上一步" 按钮

**预期结果:**
- ✅ 返回步骤1（"自动扫描" 标签页显示）
- ✅ "预览服务器列表" 标签页隐藏
- ✅ **"开始扫描"按钮保持隐藏（`display: none`）** ← 核心验证点
- ✅ **`wizardState.hasScanned` 仍为 `true`** ← 状态持久化
- ✅ "上一步" 按钮隐藏
- ✅ "下一步" 按钮显示但禁用
- ✅ 扫描结果数据保留（`wizardState.scanData` 不为空）
- ✅ 用户可点击"下一步"直接进入预览（无需重新扫描）

---

#### TC1.5 预览到导入转换

**前置条件:** 在步骤2选中至少1台服务器  
**测试步骤:**
1. 选中2台服务器
2. 点击 "下一步" 按钮

**预期结果:**
- ✅ 切换到步骤3（导入进度页面）
- ✅ "预览服务器列表" 标签页隐藏
- ✅ "导入进度" 标签页显示
- ✅ **导入进行中:**
  - "上一步" 按钮隐藏
  - "下一步" 按钮隐藏
  - "取消" 按钮显示但禁用
- ✅ 显示导入进度指示器（loading spinner）

---

#### TC1.6 导入完成后按钮状态

**前置条件:** 步骤3导入进行中  
**测试步骤:**
1. 等待导入完成（成功或失败）

**预期结果:**
- ✅ **导入完成后:**
  - "上一步" 按钮显示且启用
  - "下一步" 按钮保持隐藏
  - "取消" 按钮显示且启用
- ✅ 显示导入结果统计（成功/失败数量）
- ✅ 成功时显示 ✓ 图标，失败时显示 ✗ 图标

---

#### TC1.7 扫描按钮一次性显示验证（关键特性）

**前置条件:** 完整的向导流程  
**测试步骤:**
1. 打开向导，观察"开始扫描"按钮（应显示）
2. 点击"开始扫描"，等待扫描完成
3. 自动跳转到步骤2（预览），观察"开始扫描"按钮（应隐藏）
4. 点击"上一步"返回步骤1
5. 观察"开始扫描"按钮（应保持隐藏）
6. 选择服务器并导入（成功或失败）
7. 点击"上一步"多次返回步骤1
8. 再次观察"开始扫描"按钮

**预期结果:**
- ✅ **步骤1（初次）:** "开始扫描"按钮显示
- ✅ **步骤2（预览）:** "开始扫描"按钮隐藏
- ✅ **步骤1（返回）:** "开始扫描"按钮隐藏 ← **核心验证**
- ✅ **步骤3（导入）:** "开始扫描"按钮隐藏
- ✅ **步骤1（再次返回）:** "开始扫描"按钮仍隐藏
- ✅ **浏览器控制台日志:** `[STATE] 首次扫描完成，hasScanned = true`
- ✅ **用户体验:** 用户无法重复扫描，避免覆盖已选择的服务器

**设计理由:**
- 防止用户在预览或导入阶段误触扫描按钮
- 避免重复扫描导致数据混乱
- 简化操作流程，扫描一次即可

---

### 测试组 2: 数据持久化

#### TC2.1 导入失败后返回预览（核心功能）

**前置条件:** 步骤3导入失败  
**测试步骤:**
1. 在步骤2选中5台服务器（其中第2、4台勾选复选框）
2. 点击 "下一步" 执行导入
3. 模拟导入失败（或等待真实失败）
4. 点击 "上一步" 按钮返回预览

**预期结果:**
- ✅ 返回步骤2（预览服务器列表）
- ✅ **数据持久化验证:**
  - 所有5台服务器仍在列表中显示
  - 第2、4台服务器的复选框仍保持选中状态
  - 其他服务器的复选框保持未选中状态
  - 表格不重新渲染（DOM 元素保持不变）
- ✅ "下一步" 按钮显示 "下一步 (2)"（因为2台已选中）
- ✅ 可以继续修改选择并重新尝试导入

---

#### TC2.2 导入成功后返回预览

**前置条件:** 步骤3导入成功  
**测试步骤:**
1. 在步骤2选中3台服务器
2. 点击 "下一步" 执行导入
3. 等待导入成功
4. 点击 "上一步" 按钮

**预期结果:**
- ✅ 返回步骤2
- ✅ 服务器列表保持不变（3台服务器仍显示，复选框状态保留）
- ✅ 可以再次选择服务器并导入

---

#### TC2.3 多次往返不丢失数据

**前置条件:** 已完成扫描  
**测试步骤:**
1. 步骤1 → 步骤2（扫描完成自动跳转）
2. 选中2台服务器
3. 点击 "上一步" → 返回步骤1
4. 点击 "开始扫描" 重新扫描
5. 扫描完成后观察步骤2的服务器列表

**预期结果:**
- ✅ **场景1: 重新扫描覆盖数据**
  - 如果扫描结果相同，列表保持原样
  - 如果扫描结果不同，列表更新为新数据
- ✅ **场景2: 不重新扫描直接前进**
  - 如果从步骤1点击 "下一步" 但未扫描，应提示 "请先扫描服务器"

---

### 测试组 3: 边界条件

#### TC3.1 扫描零台服务器

**前置条件:** 配置文件中没有有效的 SSH 配置  
**测试步骤:**
1. 点击 "开始扫描"
2. 等待扫描完成

**预期结果:**
- ✅ 显示 "未扫描到任何服务器" 提示
- ✅ "下一步" 按钮保持禁用状态
- ✅ 不自动跳转到步骤2

---

#### TC3.2 全选后取消全选

**前置条件:** 步骤2有10台服务器  
**测试步骤:**
1. 点击表头的全选复选框
2. 观察按钮状态
3. 再次点击全选复选框取消选择

**预期结果:**
- ✅ 全选后："下一步" 按钮显示 "下一步 (10)"
- ✅ 取消全选后："下一步" 按钮禁用，文字恢复为 "下一步"

---

#### TC3.3 导入时关闭弹窗

**前置条件:** 步骤3导入进行中  
**测试步骤:**
1. 尝试点击弹窗外部区域或按 Esc 键
2. 尝试点击 "取消" 按钮

**预期结果:**
- ✅ 导入进行中时 "取消" 按钮禁用，无法关闭
- ✅ 弹窗外部点击不触发关闭（需后端配置 `data-bs-backdrop="static"`）

---

### 测试组 4: UI 视觉验证

#### TC4.1 扫描动画验证

**前置条件:** 步骤1  
**测试步骤:**
1. 点击 "开始扫描" 按钮
2. 观察按钮内的动画

**预期结果:**
- ✅ 按钮文字更改为 "扫描中..."
- ✅ 按钮内显示旋转动画（`.scan-spinner-lite`）
  - 圆圈直径 20px
  - 白色/蓝色边框
  - 顺滑旋转（0.8s 一圈）
- ✅ 扫描完成后动画消失，按钮恢复原状

---

#### TC4.2 复选框可见性验证

**前置条件:** 步骤2显示服务器列表  
**测试步骤:**
1. 观察表格第一列（`.col-checkbox`）
2. 点击复选框

**预期结果:**
- ✅ 复选框清晰可见（不是白底白框）
- ✅ 复选框有明确的边框样式（`border: 1px solid #dee2e6`）
- ✅ 悬停时有视觉反馈（光标变为 `pointer`）
- ✅ 选中时有明确的视觉变化（勾选标记）

---

#### TC4.3 导入进度布局验证

**前置条件:** 步骤3导入进行中  
**测试步骤:**
1. 观察 "导入进度" 容器布局

**预期结果:**
- ✅ 加载指示器水平居中（`display: flex; justify-content: center`）
- ✅ 容器垂直居中（`align-items: center`）
- ✅ 文字和图标对齐良好

---

## 状态管理函数调用链

### 扫描流程

```
handleStartScan()
  → fetch('/api/ssh-scan/scan-all')
  → populatePreviewTable(scanData)
  → switchToPreviewStep()
    → wizardState.setStep(WizardStep.PREVIEW)
      → wizardState.updateUI()
```

### 预览选择流程

```
Checkbox Click
  → updateSelectionCount()
    → wizardState.updateSelectedServers(selectedServers)
      → wizardState.updateUI()
```

### 导入流程

```
handleWizardNextClick()
  → importSelectedServers(selectedServers)
  → showImportProgress()
    → wizardState.setStep(WizardStep.IMPORT)
      → wizardState.updateUI()
  
  → [导入完成]
  → showImportResult(result) OR showImportError(result)
    → wizardState.setImportResult(result)
      → wizardState.updateUI()
```

### 返回上一步流程

```
wizardPrevBtn Click
  → if (currentStep === PREVIEW) {
        switchBackToScanStep()
          → wizardState.setStep(WizardStep.SCAN)
      }
  → if (currentStep === IMPORT) {
        switchBackToPreviewStep()  // 不重新渲染表格
          → wizardState.setStep(WizardStep.PREVIEW)
      }
```

## 代码覆盖率检查

### 必须调用 `wizardState` 的函数

✅ 已实现:
- [x] `showScanResult()` → 设置 `wizardState.hasScanned = true`
- [x] `switchToPreviewStep()` → 调用 `wizardState.setStep(WizardStep.PREVIEW)`
- [x] `switchBackToScanStep()` → 调用 `wizardState.setStep(WizardStep.SCAN)`
- [x] `switchBackToPreviewStep()` → 调用 `wizardState.setStep(WizardStep.PREVIEW)`
- [x] `showImportProgress()` → 调用 `wizardState.setStep(WizardStep.IMPORT)`
- [x] `updateSelectionCount()` → 调用 `wizardState.updateSelectedServers()`
- [x] `showImportResult()` → 调用 `wizardState.setImportResult(result)`
- [x] `showImportError()` → 调用 `wizardState.setImportResult(result)`

### 数据持久化关键点

✅ **switchBackToPreviewStep() 实现:**
```javascript
function switchBackToPreviewStep() {
    // 隐藏导入进度
    const importProgressContainer = document.getElementById('importProgressContainer');
    if (importProgressContainer) importProgressContainer.style.display = 'none';
    
    // 显示预览步骤（但不重新填充表格，保持现有 DOM）
    const wizardStep2 = document.getElementById('wizardStep2');
    if (wizardStep2) wizardStep2.style.display = 'block';
    
    // 隐藏其他步骤
    const autoScanTab = document.getElementById('auto-scan');
    if (autoScanTab) autoScanTab.style.display = 'none';
    
    // 更新状态
    wizardState.setStep(WizardStep.PREVIEW);
}
```

**核心设计:**
- ❌ **不调用** `populatePreviewTable()`（避免重新渲染）
- ✅ 仅切换容器的 `display` 属性
- ✅ DOM 元素（包括复选框状态）完整保留

---

## 回归测试检查清单

测试前确保以下功能未被破坏:

- [ ] 手动服务器导入功能仍正常（非 SSH 扫描的原有功能）
- [ ] 其他模块的弹窗不受影响
- [ ] 页面加载时没有 JavaScript 错误（检查浏览器控制台）
- [ ] CSRF 令牌仍正确传递
- [ ] Toast 通知正常显示

---

## 日志调试指南

浏览器控制台应显示以下日志（按顺序）:

### 正常流程日志

```
[DEBUG] 初始化 SSH 扫描按钮事件监听器
[DEBUG] wizardState 初始化完成
[DEBUG] 开始扫描服务器...
[DEBUG] 扫描成功，解析到 N 台服务器
[DEBUG] 填充预览表格，共 N 行
[DEBUG] 切换到预览步骤
[DEBUG] 状态切换到: preview
[DEBUG] 更新选中服务器: 2 台
[DEBUG] 状态切换到: import
[DEBUG] 导入完成
[DEBUG] 状态切换到: import（importResult 已设置）
```

### 错误日志监控

如果看到以下日志，说明有问题:

```
❌ [ERROR] wizardState 未定义
❌ [ERROR] 无法找到按钮元素
❌ [ERROR] setStep() 接收到无效步骤
```

---

## 验收标准

✅ **功能完整性:**
- 所有按钮状态符合矩阵规则
- 数据在步骤间正确保留
- 导入失败后返回预览，列表和复选框状态不变

✅ **用户体验:**
- 扫描动画流畅（20px 旋转圆圈）
- 复选框清晰可见可点击
- 按钮文字准确反映选中数量

✅ **代码质量:**
- 所有步骤转换函数调用 `wizardState` 方法
- 没有重复的按钮操作代码（统一由 `updateUI()` 控制）
- 浏览器控制台无 JavaScript 错误

---

## 已知限制

1. **数据持久化范围:**
   - 仅在向导未关闭时有效
   - 关闭弹窗后重新打开会重置所有状态
   
2. **并发扫描:**
   - 当前不支持同时多次扫描
   - 扫描中点击 "开始扫描" 无效（按钮禁用）

3. **浏览器兼容性:**
   - CSS `flex` 布局需 IE 11+
   - `Array.from()` 需 ES6 支持

---

## 文档版本

- **版本:** 1.0
- **创建日期:** 2025年
- **最后更新:** 状态管理系统集成完成
- **维护者:** 开发团队
