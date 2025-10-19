# SSH配置导入弹窗 - Tab样式修复完成

## 🐛 问题根因

用户反馈Tab按钮样式不对，经过对比 `importServerBtn` 打开的弹窗后发现：

**根本原因**: **CSS类名不一致**
- ❌ SSH配置导入弹窗使用: `.import-tabs` 和 `.import-tab`  
- ✅ 服务器导入弹窗使用: `.tabs` 和 `.tab`

虽然我们定义了 `.import-tabs` 和 `.import-tab` 的样式，但这些样式可能优先级不够，或者与现有的 `.tab` 样式不兼容。

---

## ✅ 解决方案

### 修改策略
**统一使用相同的CSS类名**，与 `server-import-modal.html` 保持一致。

### 修改内容

#### 1. HTML模板 ✅
**文件**: `src/main/resources/templates/fragments/ssh-config-import-wizard.html`

```diff
- <div class="import-tabs">
-     <button class="import-tab active" data-tab="auto-scan">
+ <div class="tabs">
+     <button class="tab active" data-tab="auto-scan">
```

#### 2. CSS样式 ✅
**文件**: `src/main/frontend/styles/ssh-config-import-wizard-v2.css`

```diff
- .import-tabs {
+ .tabs {
      display: flex;
      gap: 8px;
      ...
  }

- .import-tab {
+ .tab {
      flex: 1;
      padding: 10px 16px;
      ...
  }

- .import-tab:hover {
+ .tab:hover {
      background: white;
      color: #667eea;
  }

- .import-tab.active {
+ .tab.active {
      background: white;
      color: #667eea;
      box-shadow: 0 2px 6px rgba(0,0,0,0.08);
  }
```

#### 3. TypeScript代码 ✅
**文件**: `src/main/frontend/modules/SSHConfigImportWizard.ts`

**attachEvents() 方法**:
```diff
- this.modal.querySelectorAll<HTMLButtonElement>('.import-tab').forEach(tab => {
+ this.modal.querySelectorAll<HTMLButtonElement>('.tab').forEach(tab => {
```

**switchTab() 方法**:
```diff
- const tabs = this.modal?.querySelectorAll<HTMLButtonElement>('.import-tab');
+ const tabs = this.modal?.querySelectorAll<HTMLButtonElement>('.tab');
```

---

## 🎨 预期样式效果

修复后，Tab按钮应该显示为：

### 外观特征
- ✅ **容器**: 浅灰色背景 (#f8f9fa)，圆角8px，内边距4px
- ✅ **Tab按钮**: 默认透明背景，文字灰色 (#666)
- ✅ **悬停效果**: 白色背景，紫色文字 (#667eea)
- ✅ **激活状态**: 白色背景 + 紫色文字 + 阴影效果

### CSS属性对照
```css
/* 容器 */
.tabs {
    display: flex;
    gap: 8px;
    background: #f8f9fa;
    padding: 4px;
    border-radius: 8px;
}

/* Tab按钮默认 */
.tab {
    flex: 1;
    padding: 10px 16px;
    background: transparent;
    color: #666;
    border-radius: 6px;
}

/* Tab按钮悬停 */
.tab:hover {
    background: white;
    color: #667eea;
}

/* Tab按钮激活 */
.tab.active {
    background: white;
    color: #667eea;
    box-shadow: 0 2px 6px rgba(0,0,0,0.08);
}
```

---

## 🔧 编译验证

```bash
npm run build
```

**结果**: ✅ 成功
- 编译时间: 30.30s
- main.css: 11.93 kB
- main.js: 318.30 kB
- 无编译错误

---

## 🧪 测试步骤

### 1. 重启后端服务 ⚠️ **必须**

```powershell
# 停止当前服务 (Ctrl+C)
# 重新运行
./mvnw.cmd spring-boot:run
```

### 2. 清除浏览器缓存

- 方法1: 按 `Ctrl + Shift + R` (硬刷新)
- 方法2: F12 → Network → 勾选 `Disable cache` → 刷新

### 3. 验证样式

打开SSH配置导入弹窗，检查：

- [ ] Tab按钮有圆角灰色容器
- [ ] 默认Tab透明背景
- [ ] 激活Tab白色背景 + 紫色文字
- [ ] 悬停时Tab变白色 + 紫色文字
- [ ] Tab之间有8px间距
- [ ] 容器内边距4px

### 4. 对比参考

打开"导入SSH客户端配置"弹窗（importServerBtn），对比两个弹窗的Tab样式应该**完全一致**。

---

## 📝 文件变更总结

| 文件 | 修改内容 | 行数 |
|------|---------|------|
| `ssh-config-import-wizard.html` | class名称: import-tabs → tabs, import-tab → tab | 4行 |
| `ssh-config-import-wizard-v2.css` | CSS选择器: .import-tabs → .tabs, .import-tab → .tab | 4处 |
| `SSHConfigImportWizard.ts` | JS选择器: '.import-tab' → '.tab' | 2处 |

---

## 💡 经验总结

### 问题原因
1. **类名不统一**: 不同弹窗使用了不同的CSS类名
2. **样式覆盖**: `.import-tab` 可能被其他样式覆盖

### 解决方法
1. **统一规范**: 同类组件使用相同的CSS类名
2. **参考现有**: 新组件应参考已有组件的样式命名

### 最佳实践
- ✅ 相同功能的组件使用相同的CSS类名
- ✅ 新增样式前先检查是否已有类似样式
- ✅ 优先复用现有样式类，避免重复定义
- ✅ 保持CSS命名的语义化和一致性

---

## 🔗 相关文档

- 服务器导入弹窗: `src/main/resources/templates/fragments/server-import-modal.html`
- SSH配置导入弹窗: `src/main/resources/templates/fragments/ssh-config-import-wizard.html`
- Tab样式CSS: `src/main/frontend/styles/ssh-config-import-wizard-v2.css`
- Tab逻辑TypeScript: `src/main/frontend/modules/SSHConfigImportWizard.ts`

---

**修复时间**: 2025-10-19  
**修复人员**: AI Assistant  
**影响范围**: SSH配置导入弹窗Tab按钮样式  
**风险等级**: 低 (仅CSS类名修改)  
**需要重启**: ✅ 是 (后端服务)  
**状态**: ✅ 已完成，待测试验证
