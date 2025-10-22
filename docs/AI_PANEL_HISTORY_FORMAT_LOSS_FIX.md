# AI 面板历史消息格式丢失问题修复

> 📅 修复时间：2025-01-21
> 🐛 问题类型：刷新页面后格式丢失
> ✅ 状态：已修复

---

## 🔴 问题描述

用户反馈：刷新页面后，之前 AI 输出的**表格格式消失了**，变成了一段普通文本。

### 问题表现

**刷新前（正常）**：
```
猫与狗的性格对比（简明表格）
┌────────┬──────────────┬────────────────────────┐
│  相性  │ 猫 (Feliscatus) │ 狗 (Canislupusfamiliaris) │
├────────┼──────────────┼────────────────────────┤
│ 社交取向 │ 独居偏冷，选性社交 │ 群居驱动，渴望依附人类 │
└────────┴──────────────┴────────────────────────┘
```

**刷新后（错误）**：
```
猫与狗的性格对比（简明表格） 相性 猫 (Feliscatus) 狗 (Canislupusfamiliaris)
社交取向 独居偏冷，选性社交 群居驱动，渴望依附人类 服从性 低，拒令看情
高，乐于配合，易驯练...
```

❌ 所有格式都丢失
❌ 表格变成了纯文本
❌ 代码块、列表等所有 Markdown 格式都消失

---

## 🔍 问题根因分析

### 数据流分析

1. **AI 输出** → 原始 Markdown 文本
2. **渲染显示** → Markdown → HTML（带格式）
3. **保存到 localStorage** → ❌ 提取 `.textContent`（纯文本）
4. **刷新页面加载** → 使用纯文本渲染 → ❌ 无法识别为 Markdown

### 关键代码问题

#### 问题代码 1：`persistCurrentSession()` (310行)

```javascript
// ❌ 错误：使用 textContent 获取纯文本
function persistCurrentSession(){
  const msgs = $$('.messages .msg').map(m => ({
    role: m.classList.contains('user')?'user':'ai',
    text: m.querySelector('.body')?.textContent || ''  // ❌ 丢失格式
  }));
  localStorage.setItem(LS_KEY_CUR, JSON.stringify(msgs));
}
```

**问题**：
- `.textContent` 只获取纯文本，丢失所有 HTML 标签
- 表格的 `<table>` 标签被移除，只剩下单元格文本
- 无法区分原始是 Markdown 还是普通文本

#### 问题代码 2：没有保存原始 Markdown

在 `appendMessage()` 和 `startAiStream()` 中，渲染后的 DOM 元素没有保存原始的 Markdown 源文本。

### 数据丢失路径

```
原始 Markdown:
| 列1 | 列2 |
|-----|-----|
| A   | B   |

↓ renderMarkdown()

渲染 HTML:
<table>
  <thead><tr><th>列1</th><th>列2</th></tr></thead>
  <tbody><tr><td>A</td><td>B</td></tr></tbody>
</table>

↓ .textContent

保存到 localStorage:
"列1 列2 A B"  ❌ 格式完全丢失！

↓ 重新加载

显示为纯文本:
列1 列2 A B  ❌ 无法还原为表格
```

---

## ✅ 修复方案

### 核心思路

**在 DOM 元素上保存原始 Markdown 文本**，持久化时优先使用原始文本而非渲染后的 `.textContent`。

### 修复 1：保存原始文本到 DOM (91-114行)

在 `appendMessage()` 函数中添加：

```javascript
function appendMessage(role, text) {
  const el = document.createElement('article');
  el.className = 'msg ' + role;
  el.innerHTML = '...';
  const body = el.querySelector('.body');

  // ✅ 新增：保存原始文本到 data 属性
  el.setAttribute('data-original-text', text);

  if (role === 'ai') {
    body.innerHTML = renderMarkdown(text);
    // ... 其他处理
  }
  else { body.textContent = text; }

  list?.appendChild(el);
  list?.scrollTo({ top: list.scrollHeight, behavior: 'smooth' });
}
```

**优点**：
- 使用 HTML5 `data-*` 属性存储元数据
- 不影响渲染显示
- 可随时获取原始内容

### 修复 2：从 data 属性读取原始文本 (310-316行)

修改 `persistCurrentSession()` 函数：

```javascript
function persistCurrentSession(){
  const msgs = $$('.messages .msg').map(m => ({
    role: m.classList.contains('user')?'user':'ai',
    // ✅ 优先使用原始文本，降级到 textContent
    text: m.getAttribute('data-original-text') || m.querySelector('.body')?.textContent || ''
  }));
  localStorage.setItem(LS_KEY_CUR, JSON.stringify(msgs));
}
```

**优点**：
- 保存完整的 Markdown 格式
- 向后兼容（旧消息没有 data 属性时降级）
- 简单可靠

### 修复 3：流式输出完成时保存原始文本 (269-283行)

在 `startAiStream()` 的 `event === 'done'` 处理中：

```javascript
} else if (event === 'done') {
  // ✅ 保存原始 Markdown 文本
  aiEl.setAttribute('data-original-text', acc);

  // 渲染 Markdown
  body.innerHTML = renderMarkdown(acc);
  try {
    postEnhanceTables(body);
    if (window.Prism) Prism.highlightAllUnder(aiEl);
    attachInteractiveListeners(aiEl); // 添加交互监听器
  } catch(e){
    console.warn('Enhanced content error:', e);
  }
  addCodeActions(aiEl);
  persistCurrentSession();
}
```

**优点**：
- 流式输出完成后立即保存原始文本
- 确保 `acc`（累积的完整响应）被保存
- 与 `appendMessage()` 保持一致

---

## 📊 修复效果对比

### localStorage 存储内容对比

#### 修复前（丢失格式）

```json
{
  "role": "ai",
  "text": "猫与狗的性格对比（简明表格） 相性 猫 狗 社交取向 独居偏冷 群居驱动..."
}
```

❌ 所有管道符和分隔符都消失了
❌ 无法识别为表格

#### 修复后（保留格式）

```json
{
  "role": "ai",
  "text": "猫与狗的性格对比（简明表格）\n| 相性 | 猫 (Feliscatus) | 狗 (Canislupusfamiliaris) |\n|------|----------------|---------------------------|\n| 社交取向 | 独居偏冷，选性社交 | 群居驱动，渴望依附人类 |\n..."
}
```

✅ 完整保存了 Markdown 格式
✅ 包含管道符和分隔符
✅ 重新加载后可以正确渲染

### 刷新页面后效果

#### 修复前

```
列1 列2 A B C D  ← 纯文本，无格式
```

#### 修复后

```
┌─────┬─────┐
│ 列1 │ 列2 │  ← 正确渲染为表格
├─────┼─────┤
│  A  │  B  │
│  C  │  D  │
└─────┴─────┘
```

---

## 🧪 测试验证

### 测试用例 1：表格

**操作步骤**：
1. 发送消息让 AI 输出表格
2. 观察表格正常显示
3. 刷新页面（F5）
4. 检查表格是否仍然正常显示

**预期结果**：
- ✅ 刷新后表格格式保持
- ✅ 表头、分隔符、单元格都正确

### 测试用例 2：代码块

**操作步骤**：
1. 发送消息让 AI 输出代码块
2. 观察代码高亮正常
3. 刷新页面
4. 检查代码高亮是否保持

**预期结果**：
- ✅ 刷新后代码块格式保持
- ✅ 语法高亮正常
- ✅ 语言标签显示

### 测试用例 3：混合内容

**操作步骤**：
1. 发送消息让 AI 输出混合内容（表格 + 代码 + 列表）
2. 观察所有格式正常
3. 刷新页面
4. 检查所有格式是否保持

**预期结果**：
- ✅ 所有 Markdown 格式都保持
- ✅ 不同类型内容互不影响

### 测试用例 4：增强内容格式

**操作步骤**：
1. 发送消息让 AI 输出提示框、步骤指示器等增强格式
2. 观察增强格式正常显示
3. 刷新页面
4. 检查增强格式是否保持

**预期结果**：
- ✅ 提示框（info/warning/success）正常
- ✅ 步骤指示器正常
- ✅ 标签、进度条等都正常

---

## 🔄 数据迁移

### 旧数据兼容性

对于修复前保存的历史消息（纯文本），修复后会怎样？

```javascript
text: m.getAttribute('data-original-text') || m.querySelector('.body')?.textContent || ''
```

**降级策略**：
- 优先读取 `data-original-text`（新格式）
- 如果不存在，降级到 `.textContent`（旧格式）
- 保证不会报错或显示空白

**旧数据表现**：
- ❌ 旧的纯文本数据仍然是纯文本（无法还原格式）
- ✅ 但不会报错或崩溃
- ✅ 新的消息会正确保存格式

### 建议清理

如果希望完全清除旧的无格式数据：

```javascript
// 在浏览器控制台执行
localStorage.removeItem('ai.chat.current');
localStorage.removeItem('ai.chat.history');
```

然后刷新页面，重新开始对话。

---

## 🎓 技术要点

### 1. HTML5 Data 属性

使用 `data-*` 属性存储自定义元数据：

```javascript
// 设置
el.setAttribute('data-original-text', text);

// 读取
const text = el.getAttribute('data-original-text');
```

**优点**：
- 标准 HTML5 特性
- 不污染 DOM 可见内容
- 可存储任意字符串

### 2. 降级策略

```javascript
value1 || value2 || defaultValue
```

**优点**：
- 向后兼容
- 防止 null/undefined 错误
- 提供合理的默认值

### 3. Markdown 原始文本重要性

在渲染管道中保留原始输入：

```
输入(Markdown) → [保存] → 渲染(HTML) → 显示
                    ↓
              持久化到 localStorage
                    ↓
              重新加载 → 渲染(HTML) → 显示
```

**关键**：保存 Markdown，而非 HTML 或纯文本

---

## ✅ 验证清单

- [x] 修改 `appendMessage()` 保存原始文本
- [x] 修改 `startAiStream()` 保存原始文本
- [x] 修改 `persistCurrentSession()` 读取原始文本
- [x] 测试表格格式刷新保持
- [x] 测试代码块格式刷新保持
- [x] 测试混合内容格式保持
- [x] 测试增强格式保持
- [x] 验证旧数据兼容性
- [x] 创建修复文档

---

## 🔗 相关文件

### 修改的文件
- **JavaScript**: `src/main/resources/static/js/ai-panel.js`
  - 修改 `appendMessage()` (91-114行)
  - 修改 `persistCurrentSession()` (310-316行)
  - 修改 `startAiStream()` 的 done 事件处理 (269-283行)

### 相关文档
- `docs/AI_PANEL_CONTENT_DISPLAY_GUIDE.md` - 内容显示指南
- `docs/AI_PANEL_TABLE_TITLE_FIX.md` - 表格标题修复

---

## 📞 使用建议

### 清除旧缓存

如果想清除旧的无格式历史记录，在浏览器控制台执行：

```javascript
localStorage.removeItem('ai.chat.current');
localStorage.removeItem('ai.chat.history');
```

### 验证修复

1. 刷新页面
2. 让 AI 输出一个表格
3. 再次刷新页面
4. 检查表格是否仍然正确显示

---

**修复完成！** ✨

现在刷新页面后，所有 Markdown 格式都能正确保持了。
