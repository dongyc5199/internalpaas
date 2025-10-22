# AI 助手 Composer 与视觉规范（2025-10-22）

本文档记录 AI 助手（Ask 模式）输入区域的结构、样式与近期优化变更，便于后续维护与二次开发。

## 文件位置
- JS：`src/main/resources/static/js/ai-assistant.js`
- CSS：`src/main/resources/static/css/ai-assistant.css`

## 结构概览（精简）
```
div.ai-assistant-panel#aiAssistantPanel
  div.ai-chat-messages#aiChatMessages
  div.ai-composer
    div.ai-input-shell
      div.ai-attach-row            // 附件行：回形针、列表、加号
        button#btnAttach
        div#aiAttachList
        button#btnAttachAdd
        input#aiFilePicker[type=file]
      div.ai-context-chips#aiContextChips
      textarea#aiChatInput.ai-chat-input
      div.ai-hashtag-popup#aiHashtagPopup
      div.ai-bottom-row.ai-input-toolbar
        div.ai-left-controls
          div.ai-mode-selector > select#aiModeSelector.ai-mode-select
          div.ai-models        > select#aiModelSelector.ai-model-select
        div.ai-right-controls
          button#btnVoice.ai-btn-voice.icon-only
          button#aiSendBtn.ai-btn-send.icon-only
```

## 关键交互
- 附件
  - `#btnAttach`/`#btnAttachAdd` → 触发 `#aiFilePicker`，所选文件渲染为 `.ai-attachment-chip`，可点击“×”移除。
- 发送
  - `#aiSendBtn` 点击或 `Ctrl/Cmd+Enter` 发送。
- 语音
  - `#btnVoice` 当前为占位（弹出提示）。
- Hashtag 上下文
  - 在 `#aiChatInput` 输入 `#` 打开 `#aiHashtagPopup`，选择会话追加上下文 Chip。

## 样式规范（要点）
- 纯文本下拉外观
  - 选择器：`.ai-mode-select`, `.ai-model-select` → 透明背景、无边框，容器伪元素绘制箭头：`.ai-mode-selector::after`, `.ai-models::after`
  - 模式下拉宽度自适应（`.ai-mode-select { width:auto; white-space:nowrap; }`）
- 一体化输入区域
  - `.ai-composer` 背景透明、无上边框，与消息区融为一体
  - `.ai-input-shell` 控制唯一边框与聚焦高亮（`focus-within`）
  - `.ai-input-toolbar` 取消分隔线
- 尺寸压缩
  - `textarea`：最小高度 44px，最大高度 120–140px；减少 padding
  - 语音/发送：`.icon-only` 小尺寸（34x34）

## 本次更新摘要（2025-10-22）
1) 模式/模型下拉使用纯文本视觉；模式文案为“Ask 模式 / Agent 模式”，模式宽度自适应。
2) 底部工具条移入输入壳内部，语音/发送为小图标按钮，形成单一整体视觉。
3) 新增附件行（回形针/加号/文件 chip）。
4) 压缩输入区高度，统一聊天与输入区背景；移除 composer 顶部分隔与内部分隔线。
5) 移除“发送更多”下拉与“裁剪”按钮，保留语音按钮（占位）。

## 快速修改指引
- 调整输入最小高度：`ai-assistant.css` 的 `.ai-chat-input` 与 `.ai-input-shell textarea`
- 切换下拉视觉：修改“Plain Text Dropdown 视觉”段；若需恢复原风格，去掉 `appearance:none` 与 `background:transparent`
- 添加/禁用附件：`ai-assistant.js` 中 `#aiFilePicker` 相关监听

## 兼容性说明
- 当前实现为纯原生 JS/CSS；不依赖外部库。
- 浏览器要求：现代 Chromium/Firefox（支持 `appearance` 与 `focus-within`）。

