# SSH 导入弹窗状态重置完整修复

## 问题报告

### 问题 1：扫描按钮消失
**描述**：弹窗关闭后再打开，"开始自动扫描"按钮消失。

**复现步骤**：
1. 打开 SSH 配置导入弹窗
2. 点击"开始自动扫描"
3. 扫描完成后关闭弹窗
4. 再次打开弹窗
5. **问题**：看不到"开始自动扫描"按钮

### 问题 2：步骤按钮状态错误
**描述**：当弹窗在非第一个流程（预览或结果步骤）关闭后，再次打开发现"上一步"按钮还在，"开始自动扫描"按钮不见了。

**复现步骤**：
1. 打开 SSH 配置导入弹窗
2. 完成扫描，进入预览步骤
3. 在预览步骤关闭弹窗（此时"上一步"按钮可见）
4. 再次打开弹窗
5. **问题**：
   - "上一步"按钮仍然可见（应该隐藏）
   - "开始自动扫描"按钮不可见（应该显示）

## 根本原因分析

### 问题 1 的原因
扫描开始时，按钮状态被修改：
```javascript
// handleStartScan() 函数中
scanBtn.disabled = true;
scanBtn.classList.add('is-scanning');
scanBtn.innerHTML = '<span class="scan-spinner-lite"></span><span>扫描中...</span>';
```

但 `resetModalState()` 函数**没有恢复按钮的原始状态**。

### 问题 2 的原因
1. `wizardState.reset()` 重置了内部状态变量（`currentStep = SCAN`）
2. 但**没有调用 `updateUI()`** 来同步更新按钮显示
3. 导致：
   - 内部状态：第一步（SCAN）
   - UI 显示：保持上次关闭时的状态（PREVIEW 或 IMPORT）
   - 结果：按钮显示与步骤不匹配

## 解决方案

### 修复 1：重置扫描按钮
在 `resetModalState()` 的**步骤 2** 中添加：

```javascript
// 2. 重置扫描按钮状态
const startScanBtn = document.getElementById('startScanBtn');
if (startScanBtn) {
    startScanBtn.disabled = false;                    // ✅ 启用按钮
    startScanBtn.classList.remove('is-scanning');     // ✅ 移除扫描样式
    startScanBtn.innerHTML = '<span>🔍</span><span>开始自动扫描</span>';  // ✅ 恢复HTML
    startScanBtn.style.display = 'inline-flex';       // ✅ 确保可见
    console.log('[DEBUG] 扫描按钮已重置');
}
```

### 修复 2：同步 UI 状态
在 `resetModalState()` 的**步骤 13** 中添加：

```javascript
// 13. ✅ 新增：调用 updateUI() 确保按钮状态与步骤一致
if (typeof wizardState !== 'undefined' && wizardState.updateUI) {
    console.log('[DEBUG] 调用 wizardState.updateUI() 更新按钮显示');
    wizardState.updateUI();
}
```

### updateUI() 的作用

`wizardState.updateUI()` 根据当前步骤更新所有按钮：

```javascript
// 当 currentStep = WizardStep.SCAN 时
case WizardStep.SCAN:
    // ✅ 隐藏"上一步"按钮（第一步不需要）
    this.updateButton(prevBtn, { visible: false, disabled: true });
    
    // ✅ 显示"下一步"按钮但禁用（未扫描前不可用）
    this.updateButton(nextBtn, { visible: true, disabled: true, text: '下一步 →' });
    
    // ✅ 显示"取消"按钮
    this.updateButton(cancelBtn, { visible: true, disabled: false });
    
    // ✅ 显示"开始扫描"按钮（如果还未扫描）
    if (scanBtn) {
        scanBtn.style.display = this.hasScanned ? 'none' : 'inline-flex';
    }
    break;
```

## 完整的重置流程

### 重置状态清单（13 项）

| 序号 | 状态项 | 重置操作 | 解决的问题 |
|------|--------|----------|------------|
| 1 | 向导步骤状态 | `wizardState.reset()` → `SCAN` | 重置到第一步 |
| 2 | **扫描按钮** ⭐ | 恢复HTML、启用、显示 | **修复问题 1** |
| 3 | 扫描结果表格 | 清空 | - |
| 4 | 服务器预览表格 | 清空 | - |
| 5 | 扫描状态指示器 | 隐藏 | - |
| 6 | 扫描结果容器 | 隐藏 | - |
| 7 | 进度条 | 重置为 0% | - |
| 8 | 导入结果容器 | 隐藏 | - |
| 9 | 成功服务器列表 | 清空 | - |
| 10 | 失败服务器列表 | 清空 | - |
| 11 | 成功/失败区域 | 隐藏 | - |
| 12 | 步骤显示 | 显示第一步，隐藏其他 | - |
| 13 | **按钮UI状态** ⭐ | `wizardState.updateUI()` | **修复问题 2** |

### 关键修复点

1. **步骤 2**：显式恢复扫描按钮的所有属性
   - `disabled = false`
   - `classList.remove('is-scanning')`
   - 恢复原始 `innerHTML`
   - `style.display = 'inline-flex'`

2. **步骤 13**：调用 `updateUI()` 同步所有按钮
   - 隐藏"上一步"按钮
   - 显示"开始扫描"按钮
   - 禁用"下一步"按钮
   - 显示"取消"按钮

## 测试验证

### 测试场景 1：扫描后关闭重开
**步骤**：
1. 打开弹窗 → 点击"开始自动扫描"
2. 扫描完成后关闭弹窗
3. 再次打开弹窗

**期望结果**：
- ✅ "开始自动扫描"按钮可见且可点击
- ✅ 按钮文本为"🔍 开始自动扫描"（不是"扫描中..."）
- ✅ "上一步"按钮隐藏
- ✅ "下一步"按钮禁用

### 测试场景 2：预览步骤关闭重开
**步骤**：
1. 打开弹窗 → 扫描 → 进入预览步骤
2. 在预览步骤关闭弹窗（此时"上一步"可见）
3. 再次打开弹窗

**期望结果**：
- ✅ "开始自动扫描"按钮可见
- ✅ "上一步"按钮**隐藏**（不应该还显示）
- ✅ "下一步"按钮禁用
- ✅ 所有按钮状态与第一步一致

### 测试场景 3：导入结果步骤关闭重开
**步骤**：
1. 打开弹窗 → 扫描 → 预览 → 导入完成
2. 在结果步骤关闭弹窗
3. 再次打开弹窗

**期望结果**：
- ✅ 回到第一步扫描界面
- ✅ 所有按钮状态正确
- ✅ 导入结果已清空

### 测试场景 4：自动关闭后重开
**步骤**：
1. 打开弹窗 → 扫描 → 预览 → 导入全部成功
2. 弹窗 2 秒后自动关闭
3. 再次打开弹窗

**期望结果**：
- ✅ 所有状态已重置
- ✅ 按钮显示正确

## 代码变更总结

### 文件：main-layout.html

**位置 1**：`resetModalState()` 函数（约第 5717 行）

**变更**：
```javascript
// 2. 重置扫描按钮状态（新增 style.display）
const startScanBtn = document.getElementById('startScanBtn');
if (startScanBtn) {
    startScanBtn.disabled = false;
    startScanBtn.classList.remove('is-scanning');
    startScanBtn.innerHTML = '<span>🔍</span><span>开始自动扫描</span>';
    startScanBtn.style.display = 'inline-flex';  // ✅ 新增
    console.log('[DEBUG] 扫描按钮已重置');
}
```

**位置 2**：`resetModalState()` 函数末尾（约第 5792 行）

**新增**：
```javascript
// 13. ✅ 新增：调用 updateUI() 确保按钮状态与步骤一致
if (typeof wizardState !== 'undefined' && wizardState.updateUI) {
    console.log('[DEBUG] 调用 wizardState.updateUI() 更新按钮显示');
    wizardState.updateUI();
}
```

## 技术要点

### 1. 状态与 UI 的分离与同步
- **状态**：`wizardState.currentStep` 等内部变量
- **UI**：按钮的显示、禁用等 DOM 属性
- **同步**：`updateUI()` 函数负责从状态到 UI 的映射

### 2. 重置的两个层次
- **数据层**：清空表格、重置变量
- **UI 层**：恢复按钮、同步显示

### 3. 调试日志
添加了详细的 `console.log`，便于追踪重置过程：
```javascript
console.log('[DEBUG] 重置弹窗状态');
console.log('[DEBUG] 扫描按钮已重置');
console.log('[DEBUG] 调用 wizardState.updateUI() 更新按钮显示');
console.log('[DEBUG] 弹窗状态重置完成');
```

## 部署清单

- ✅ 修改 `main-layout.html`
- ✅ 执行 `npm run build`（编译成功）
- ⏳ 重启 Spring Boot 应用
- ⏳ 测试所有场景

## 影响范围

**影响功能**：SSH 配置导入向导弹窗

**影响文件**：
- `main-layout.html`（前端逻辑）

**影响用户体验**：
- ✅ 修复弹窗状态重置不完整的问题
- ✅ 确保每次打开弹窗都是干净的初始状态
- ✅ 解决按钮显示错乱的 UI bug

---

**修复日期**：2025-10-20  
**开发者**：GitHub Copilot  
**验证状态**：✅ 代码已编译，待端到端测试
