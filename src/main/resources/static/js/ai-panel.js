(() => {
  const $ = (sel, ctx = document) => ctx.querySelector(sel);
  const $$ = (sel, ctx = document) => Array.from(ctx.querySelectorAll(sel));

  /**
   * TypewriterRenderer - 打字机渲染器
   * 实现字符级流式动画，逐字显示文本
   */
  class TypewriterRenderer {
    constructor(targetElement, config = {}) {
      this.target = targetElement;
      this.charsPerSecond = config.speed || 50;  // 默认 50 字符/秒
      this.onComplete = config.onComplete || null;
      this.text = '';
      this.position = 0;
      this.isPaused = false;
      this.isComplete = false;
      this.lastFrameTime = 0;
    }

    start(fullText) {
      this.text = fullText;
      this.position = 0;
      this.isComplete = false;
      this.lastFrameTime = performance.now();
      this._renderFrame();
    }

    _renderFrame() {
      if (this.isPaused || this.isComplete) return;

      const now = performance.now();
      const deltaTime = (now - this.lastFrameTime) / 1000;  // 转换为秒
      const charsToAdd = Math.floor(deltaTime * this.charsPerSecond);

      if (charsToAdd > 0) {
        this.position = Math.min(this.position + charsToAdd, this.text.length);
        this.target.textContent = this.text.substring(0, this.position);
        this.lastFrameTime = now;

        // 自动滚动
        const messagesContainer = this.target.closest('.messages');
        if (messagesContainer) {
          messagesContainer.scrollTop = messagesContainer.scrollHeight;
        }
      }

      if (this.position < this.text.length) {
        requestAnimationFrame(() => this._renderFrame());
      } else {
        this.isComplete = true;
        if (this.onComplete) this.onComplete();
      }
    }

    pause() {
      this.isPaused = true;
    }

    resume() {
      if (this.isPaused && !this.isComplete) {
        this.isPaused = false;
        this.lastFrameTime = performance.now();
        this._renderFrame();
      }
    }

    skipToEnd() {
      this.position = this.text.length;
      this.target.textContent = this.text;
      this.isComplete = true;
      if (this.onComplete) this.onComplete();
    }
  }

  /**
   * 辅助工具函数 - 流式输出相关
   */

  /**
   * 去除 HTML 标签，返回纯文本
   */
  function stripHtmlTags(html) {
    const temp = document.createElement('div');
    temp.innerHTML = html;
    return temp.textContent || temp.innerText || '';
  }

  /**
   * 获取用户偏好的流式速度（字符/秒）
   */
  function getUserStreamingSpeed() {
    const SPEED_MAP = {
      'slow': 30,
      'normal': 50,
      'fast': 80
    };
    const speedSetting = localStorage.getItem('ai.streaming.speed') || 'normal';
    return SPEED_MAP[speedSetting] || SPEED_MAP['normal'];
  }

  /**
   * 检查是否启用流式输出
   */
  function isStreamingEnabled() {
    const enabled = localStorage.getItem('ai.streaming.enabled');
    return enabled !== 'false';  // 默认启用
  }

  /**
   * 应用格式化渲染 - 两阶段渲染的第二阶段
   * 将纯文本流式输出转换为完整的格式化 HTML 内容
   */
  function applyFormatting(aiEl, body, htmlContent) {
    // 移除之前的样式类，避免冲突
    body.classList.remove('format-applied');

    // 清空 body，显示完整格式化内容
    body.innerHTML = '';

    // 短暂延迟后设置内容，确保 DOM 完全清空
    requestAnimationFrame(() => {
      body.innerHTML = htmlContent;

      // 应用现有的格式化增强
      try {
        postEnhanceTables(body);
        if (window.Prism) Prism.highlightAllUnder(aiEl);
        attachInteractiveListeners(aiEl);
      } catch (e) {
        console.warn('Enhanced content error:', e);
      }

      addCodeActions(aiEl);

      // 淡入动画
      requestAnimationFrame(() => {
        body.classList.add('format-applied');
      });
    });
  }

  const ariaLive = $('#ariaLive');
  // History dropdown
  const btnHistory = $('#btnHistory');
  const historyMenu = $('#historyMenu');
  btnHistory?.addEventListener('click', (e) => {
    e.stopPropagation();
    const expanded = btnHistory.getAttribute('aria-expanded') === 'true';
    btnHistory.setAttribute('aria-expanded', String(!expanded));
    if (historyMenu) historyMenu.toggleAttribute('hidden');
    if (!expanded) renderHistoryLists();
  });
  document.addEventListener('click', (e) => {
    if (historyMenu && !historyMenu.contains(e.target) && e.target !== btnHistory) {
      historyMenu.setAttribute('hidden','');
      btnHistory?.setAttribute('aria-expanded','false');
    }
  });

  // New session → archive current and clear
  $('#btnNewSession')?.addEventListener('click', () => {
    archiveCurrentSession();
    clearMessages();
    announce('已新建会话，历史已归档');
    localStorage.removeItem('ai.chat.id');
  });

  // Drawer tabs
  $$('.drawer-bar .tab').forEach(btn => {
    btn.addEventListener('click', () => {
      $$('.drawer-bar .tab').forEach(b => b.classList.remove('active'));
      btn.classList.add('active');
      const target = btn.getAttribute('data-target');
      $$('.drawers .drawer').forEach(d => {
        if ('#' + d.id === target) { d.hidden = false; d.classList.add('open'); }
        else { d.hidden = true; d.classList.remove('open'); }
      });
    });
  });

  // Backend-driven model list
  let selectedClient = 'echo';
  async function loadModels() {
    try {
      const res = await fetch('/api/ai/models/detailed');
      const list = await res.json();
      const box = document.querySelector('.models');
      if (!box) return;
      box.innerHTML = '';
      list.forEach(m => {
        const btn = document.createElement('button');
        btn.className = 'model';
        btn.setAttribute('data-client', m.clientId);
        btn.textContent = labelForModel(m);
        btn.addEventListener('click', () => {
          selectedClient = m.clientId;
          $$('.models .model').forEach(b => b.classList.remove('active'));
          btn.classList.add('active');
          announce('已切换模型客户端：' + selectedClient);
        });
        box.appendChild(btn);
      });
      // default select first
      const first = box.querySelector('.model');
      if (first) { first.classList.add('active'); selectedClient = first.getAttribute('data-client') || 'echo'; }
    } catch (e) { console.warn('load models failed', e); }
  }
  function labelForModel(m){
    if (m.clientId === 'moonshot') return 'Kimi (' + (m.model||'') + ')';
    if (m.clientId === 'ollama') return 'Ollama (' + (m.model||'') + ')';
    if (m.clientId === 'echo') return 'Echo';
    return (m.provider||m.clientId||'model');
  }

  // Composer
  const composer = $('#composerInput');
  const btnSend = $('#btnSend');
  const btnStop = null; // 简化后不显示 Stop
  const list = $('#messageList');

  function sendMessage() {
    const text = (composer?.value || '').trim();
    if (!text) return;
    appendMessage('user', text);
    composer.value = '';
    startAiStream(text);
  }
  function appendMessage(role, text) {
    const el = document.createElement('article');
    el.className = 'msg ' + role;
    el.innerHTML = '<div class="avatar">'+(role==='ai'?'🤖':'👤')+'</div><div class="content"><div class="meta"></div><div class="body"></div></div>';
    const body = el.querySelector('.body');

    // 保存原始文本到 data 属性，用于持久化
    el.setAttribute('data-original-text', text);

    if (role === 'ai') {
      // 直接使用HTML渲染，不使用Markdown
      body.innerHTML = text;
      try {
        postEnhanceTables(body);
        if (window.Prism) Prism.highlightAllUnder(el);
        attachInteractiveListeners(el); // 添加交互监听器
      } catch(e){
        console.warn('Enhanced content error:', e);
      }
      addCodeActions(el);
    }
    else { body.textContent = text; }
    list?.appendChild(el);
    list?.scrollTo({ top: list.scrollHeight, behavior: 'smooth' });
  }
  btnSend?.addEventListener('click', sendMessage);
  composer?.addEventListener('keydown', (e) => {
    if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); sendMessage(); }
  });
  // Voice input toggle
  const btnVoice = $('#btnVoice');
  btnVoice?.addEventListener('click', () => {
    btnVoice.classList.toggle('recording');
    announce(btnVoice.classList.contains('recording') ? '已开始语音输入' : '已结束语音输入');
  });

  // Context hashtag picker
  const hashtagPop = $('#hashtagPop');
  const hashtagList = $('#hashtagList');
  const chips = $('#contextChips');
  let selectedContexts = [];
  composer?.addEventListener('input', () => {
    const v = composer.value;
    if (v.endsWith('#')) showHashtagPop(); else hideHashtagPop();

    // 自动调整高度
    composer.style.height = 'auto';
    composer.style.height = Math.min(composer.scrollHeight, 150) + 'px';

    // 动态启用/禁用发送按钮
    const hasContent = v.trim().length > 0;
    if (btnSend) {
      btnSend.disabled = !hasContent;
      btnSend.style.opacity = hasContent ? '1' : '0.5';
      btnSend.style.cursor = hasContent ? 'pointer' : 'not-allowed';
    }
  });
  composer?.addEventListener('keydown', (e) => {
    if (e.key === 'Backspace' && (composer.value === '' || composer.selectionStart === 0) && selectedContexts.length) {
      selectedContexts.pop();
      renderChips();
      e.preventDefault();
    }
  });
  function showHashtagPop(){
    renderHashtagSuggestions();
    hashtagPop?.removeAttribute('hidden');
  }
  function hideHashtagPop(){ hashtagPop?.setAttribute('hidden',''); }
  function renderHashtagSuggestions(){
    if (!hashtagList) return;
    const items = getTerminalConversations();
    hashtagList.innerHTML = '';
    items.forEach(t => {
      const li = document.createElement('li');
      li.textContent = t.title;
      li.addEventListener('click', () => { addContext(t); hideHashtagPop(); });
      hashtagList.appendChild(li);
    });
  }
  function addContext(item){
    if (selectedContexts.find(x => x.id === item.id)) return;
    selectedContexts.push(item);
    renderChips();
  }
  function renderChips(){
    if (!chips) return;
    chips.innerHTML = '';
    selectedContexts.forEach(c => {
      const el = document.createElement('span');
      el.className = 'chip';
      el.innerHTML = `<span class="i">#</span>${c.title}<span class="x" title="删除">×</span>`;
      el.querySelector('.x').addEventListener('click', () => {
        selectedContexts = selectedContexts.filter(x => x.id !== c.id);
        renderChips();
      });
      chips.appendChild(el);
    });
  }

  // Keyboard shortcuts
  document.addEventListener('keydown', (e) => {
    if ((e.ctrlKey || e.metaKey) && e.shiftKey && e.key.toLowerCase() === 'a') {
      e.preventDefault(); composer?.focus(); announce('已聚焦输入框');
    }
    if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') { e.preventDefault(); sendMessage(); }
    if (e.key === 'Escape') { e.preventDefault(); hideHashtagPop(); }
    if (e.altKey && (e.key === 'ArrowUp' || e.key === 'ArrowDown')) {
      e.preventDefault();
      const items = $$('.messages .msg');
      const idx = items.findIndex(n => n === document.activeElement?.closest('.msg'));
      const next = e.key === 'ArrowDown' ? Math.min(items.length - 1, idx + 1) : Math.max(0, idx - 1);
      (items[next] || items[0]).setAttribute('tabindex', '0');
      (items[next] || items[0]).focus();
    }
  });

  function announce(msg) {
    if (!ariaLive) return;
    ariaLive.textContent = msg;
  }

  // === Real AI streaming via POST /ai/chat/stream (SSE) ===
  function getCsrf(){
    const token = document.querySelector('meta[name="_csrf"]')?.content;
    const header = document.querySelector('meta[name="_csrf_header"]')?.content || 'X-CSRF-TOKEN';
    return { token, header };
  }
  function mapModelForRequest(){ return selectedClient || 'echo'; }
  function currentChatId(){
    let id = localStorage.getItem('ai.chat.id');
    if (!id){ id = 'chat-' + Math.random().toString(36).slice(2); localStorage.setItem('ai.chat.id', id); }
    return id;
  }
  function buildTerminalTail(){
    if (!selectedContexts || !selectedContexts.length) return '';
    return selectedContexts.map(c => `[${c.title}]`).join(' ');
  }
  async function startAiStream(text){
    // Create streaming assistant bubble
    const aiEl = document.createElement('article');
    aiEl.className = 'msg ai';
    aiEl.innerHTML = '<div class="avatar">AI</div><div class="content"><div class="meta"></div><div class="body"><div class="typing-indicator"><span>AI 正在思考</span><div class="dots"><span class="dot"></span><span class="dot"></span><span class="dot"></span></div></div></div></div>';
    const body = aiEl.querySelector('.body');
    let acc = '';
    list?.appendChild(aiEl);
    list?.scrollTo({ top: list.scrollHeight });

    const payload = {
      chatId: currentChatId(),
      sessionId: null,
      model: mapModelForRequest(),
      message: text,
      terminalTail: buildTerminalTail()
    };
    const { token, header } = getCsrf();
    try {
      const res = await fetch('/ai/chat/stream', {
        method: 'POST',
        headers: Object.assign({ 'Content-Type': 'application/json' }, token ? { [header]: token } : {}),
        body: JSON.stringify(payload)
      });
      if (!res.ok || !res.body) {
        body.textContent = `AI 调用失败（${res.status}）`;
        persistCurrentSession();
        return;
      }
      const reader = res.body.getReader();
      const decoder = new TextDecoder('utf-8');
      let buffer = '';
      while (true){
        const { done, value } = await reader.read();
        if (done) break;
        buffer += decoder.decode(value, { stream:true });
        // Parse SSE frames
        let idx;
        while ((idx = buffer.indexOf('\n\n')) !== -1){
          const chunk = buffer.slice(0, idx); buffer = buffer.slice(idx + 2);
          const lines = chunk.split('\n');
          let event = 'message'; let data = '';
          for (const line of lines){
            if (line.startsWith('event:')) event = line.slice(6).trim();
            if (line.startsWith('data:')) data += line.slice(5).trim();
          }
          if (event === 'token') {
            // 移除 typing indicator（仅在第一个 token 时）
            const typingIndicator = aiEl.querySelector('.typing-indicator');
            if (typingIndicator) {
              typingIndicator.remove();
              // 创建流式文本容器
              const streamingEl = document.createElement('div');
              streamingEl.className = 'streaming';
              body.appendChild(streamingEl);
            }

            acc += data;
            const s = aiEl.querySelector('.streaming'); if (s) s.textContent = acc;
            list?.scrollTo({ top: list.scrollHeight });
          } else if (event === 'error') {
            acc += `\n[错误] ${data}`;
            const s = aiEl.querySelector('.streaming'); if (s) s.textContent = acc;
          } else if (event === 'done') {
            // 移除所有加载指示器
            const typingIndicator = aiEl.querySelector('.typing-indicator');
            if (typingIndicator) typingIndicator.remove();

            const streamingEl = aiEl.querySelector('.streaming');
            if (streamingEl) streamingEl.remove();

            // 保存原始 HTML 文本
            aiEl.setAttribute('data-original-text', acc);

            // 检查是否启用流式输出
            if (isStreamingEnabled()) {
              // ✅ 启用打字机效果 - 两阶段渲染
              // 阶段 1: 创建流式容器，逐字符显示纯文本
              const streamingContainer = document.createElement('div');
              streamingContainer.className = 'streaming-text';
              body.appendChild(streamingContainer);

              // 启动打字机渲染（显示纯文本）
              const plainText = stripHtmlTags(acc);
              const typewriter = new TypewriterRenderer(streamingContainer, {
                speed: getUserStreamingSpeed(),
                onComplete: () => applyFormatting(aiEl, body, acc)
              });

              // 允许用户点击跳过
              streamingContainer.addEventListener('click', () => typewriter.skipToEnd());

              typewriter.start(plainText);
            } else {
              // ❌ 禁用流式输出，立即显示格式化内容
              applyFormatting(aiEl, body, acc);
            }

            persistCurrentSession();
          }
        }
      }
      persistCurrentSession();
    } catch (err){
      body.textContent += `\n[异常] ${err?.message || err}`;
      persistCurrentSession();
    }
  }

  // Simplified AI answer modes
  function getAiReply(text){
    const modes = [
      (t)=>({type:'text', content:`好的，我来帮你：${t}`}),
      ()=>({type:'steps', steps:['分析问题','生成方案','给出建议']}),
      ()=>({type:'code', lang:'js', code:'function demo(){\n  // TODO\n}'}),
      ()=>({type:'info', content:'已切换模型到 '+selectedModel})
    ];
    const pick = modes[Math.floor(Math.random()*modes.length)];
    return renderAiMessage(pick());
  }
  function renderAiMessage(payload){
    if (typeof payload === 'string') return payload;
    if (payload.type === 'text') return payload.content;
    if (payload.type === 'steps') return '建议步骤：\n- ' + payload.steps.join('\n- ');
    if (payload.type === 'code') return `代码示例(${payload.lang})\n\n`+payload.code;
    if (payload.type === 'info') return payload.content;
    return '收到。';
  }

  // History persistence (localStorage)
  const LS_KEY_CUR = 'ai.chat.current';
  const LS_KEY_HIST = 'ai.chat.history';
  function loadCurrentSession(){
    try { return JSON.parse(localStorage.getItem(LS_KEY_CUR)||'[]'); } catch { return []; }
  }
  function persistCurrentSession(){
    const msgs = $$('.messages .msg').map(m => ({
      role: m.classList.contains('user')?'user':'ai',
      text: m.getAttribute('data-original-text') || m.querySelector('.body')?.textContent || ''
    }));
    localStorage.setItem(LS_KEY_CUR, JSON.stringify(msgs));
  }
  function archiveCurrentSession(){
    const cur = loadCurrentSession(); if (!cur.length) return;
    const hist = loadHistory();
    hist.push({ id: 'h'+Date.now(), title: formatTitle(cur), date: Date.now(), messages: cur });
    localStorage.setItem(LS_KEY_HIST, JSON.stringify(hist));
    localStorage.removeItem(LS_KEY_CUR);
  }
  function loadHistory(){ try { return JSON.parse(localStorage.getItem(LS_KEY_HIST)||'[]'); } catch { return []; } }
  function formatTitle(msgs){ const u = msgs.find(m=>m.role==='user'); return (u?.text||'新会话').slice(0,20); }
  function renderHistoryLists(){
    const hist = loadHistory();
    const thisWeek = hist.filter(h => isThisWeek(h.date));
    const thisYear = hist.filter(h => !isThisWeek(h.date));
    const ulW = $('#histThisWeek'); const ulY = $('#histThisYear');
    if (ulW) ulW.innerHTML = ''; if (ulY) ulY.innerHTML = '';
    thisWeek.forEach(h => appendHistItem(ulW, h));
    thisYear.forEach(h => appendHistItem(ulY, h));
  }
  function appendHistItem(ul, h){ if(!ul) return; const li=document.createElement('li'); li.innerHTML=`<span>${h.title}</span><button class="btn sm link">打开</button>`; li.querySelector('button').addEventListener('click', ()=>{ loadSession(h); historyMenu?.setAttribute('hidden',''); }); ul.appendChild(li); }
  function loadSession(h){ clearMessages(); (h.messages||[]).forEach(m => appendMessage(m.role, m.text)); localStorage.setItem(LS_KEY_CUR, JSON.stringify(h.messages||[])); }
  function clearMessages(){ list.innerHTML=''; }
  function isThisWeek(ts){ const d=new Date(ts); const now=new Date(); const oneDay=86400000; const diff=(now - d)/oneDay; return diff<=7; }

  // Provide hashtag context options from history titles
  function getTerminalConversations(){
    const hist = loadHistory();
    const items = hist.slice(-10).reverse().map(h => ({ id:h.id, title:h.title }));
    if (items.length) return items;
    // fallback demo items
    return [
      { id:'t1', title:'最近部署日志' },
      { id:'t2', title:'Nginx 错误排查' },
      { id:'t3', title:'数据库连接告警' }
    ];
  }

  // === Completion suggest ===
  const suggestionBar = document.getElementById('suggestionBar');
  let suggestTimer = null;
  composer?.addEventListener('input', () => {
    if (suggestTimer) clearTimeout(suggestTimer);
    const text = composer.value.trim();
    if (!text) { renderSuggest([]); return; }
    suggestTimer = setTimeout(() => fetchSuggest(text), 200);
  });
  async function fetchSuggest(prompt){
    try {
      const { token, header } = getCsrf();
      const res = await fetch('/ai/completion/suggest', {
        method: 'POST',
        headers: Object.assign({ 'Content-Type':'application/json' }, token ? { [header]: token } : {}),
        body: JSON.stringify({ prompt })
      });
      const data = await res.json();
      renderSuggest((data && data.suggestions) || []);
    } catch (e) { /* ignore */ }
  }
  function renderSuggest(items){
    if (!suggestionBar) return;
    suggestionBar.innerHTML = '';
    if (!items.length) { suggestionBar.setAttribute('hidden',''); return; }
    suggestionBar.removeAttribute('hidden');
    items.forEach(s => {
      const chip = document.createElement('button');
      chip.className = 'suggestion';
      chip.textContent = s.text || '';
      chip.addEventListener('click', () => applySuggestion(s.text||''));
      suggestionBar.appendChild(chip);
    });
  }
  function applySuggestion(text){
    if (!composer) return;
    const start = composer.selectionStart || composer.value.length;
    const v = composer.value;
    composer.value = v.slice(0, start) + text + v.slice(start);
    composer.focus();
  }

  // === Markdown render & code actions ===
  function escapeHtml(s){
    return s.replace(/[&<>]/g, function(ch){
      return ch === '&' ? '&amp;' : (ch === '<' ? '&lt;' : '&gt;');
    });
  }
  function mdInline(s){
    // links
    s = s.replace(/\[([^\]]+)\]\(([^)]+)\)/g, '<a href="$2" target="_blank" rel="noopener">$1</a>');
    // bold/italic (simple)
    s = s.replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>');
    s = s.replace(/\*([^*]+)\*/g, '<em>$1</em>');
    // inline code
    s = s.replace(/`([^`]+)`/g, (m,code)=>`<code>${escapeHtml(code)}</code>`);
    return s;
  }

  // === 🎨 Enhanced Content Renderers ===

  /**
   * 增强内容渲染 - 识别特殊格式并转换为 HTML
   */
  function enhanceContent(html) {
    let enhanced = html;

    // 1. 识别并渲染提示框 (Alert Boxes)
    // 格式: :::info 内容 :::
    enhanced = enhanced.replace(/:::(info|warning|success|error|tip)\s+([\s\S]*?):::/gi, (match, type, content) => {
      return `<div class="alert alert-${type}"><div class="alert-content">${mdInline(content.trim())}</div></div>`;
    });

    // 2. 识别带标题的提示框
    // 格式: :::info[标题] 内容 :::
    enhanced = enhanced.replace(/:::(info|warning|success|error|tip)\[([^\]]+)\]\s+([\s\S]*?):::/gi, (match, type, title, content) => {
      return `<div class="alert alert-${type}"><div class="alert-content"><div class="alert-title">${escapeHtml(title)}</div>${mdInline(content.trim())}</div></div>`;
    });

    // 3. 识别步骤指示器
    // 格式: :::steps ... :::
    enhanced = enhanced.replace(/:::steps\s+([\s\S]*?):::/gi, (match, content) => {
      const steps = content.trim().split(/\n(?=\d+\.\s)/);
      let stepsHtml = '<div class="steps">';
      steps.forEach(step => {
        const stepMatch = step.match(/^\d+\.\s+(.+?)(?:\n(.+))?$/s);
        if (stepMatch) {
          const [, title, desc] = stepMatch;
          stepsHtml += `<div class="step">
            <div class="step-title">${escapeHtml(title.trim())}</div>
            ${desc ? `<div class="step-content">${mdInline(escapeHtml(desc.trim()))}</div>` : ''}
          </div>`;
        }
      });
      stepsHtml += '</div>';
      return stepsHtml;
    });

    // 4. 识别信息卡片
    // 格式: :::card[标题] 内容 :::footer:::
    enhanced = enhanced.replace(/:::card\[([^\]]+)\]\s+([\s\S]*?)(?:::footer(.+?))?:::/gi, (match, title, body, footer) => {
      let cardHtml = `<div class="card">
        <div class="card-header">${escapeHtml(title)}</div>
        <div class="card-body">${mdInline(escapeHtml(body.trim()))}</div>`;
      if (footer) {
        cardHtml += `<div class="card-footer">${mdInline(escapeHtml(footer.trim()))}</div>`;
      }
      cardHtml += '</div>';
      return cardHtml;
    });

    // 5. 识别标签组
    // 格式: :::tags tag1, tag2, tag3 :::
    enhanced = enhanced.replace(/:::tags\s+(.+?):::/gi, (match, tagsStr) => {
      const tags = tagsStr.split(',').map(t => t.trim()).filter(Boolean);
      let tagsHtml = '<div class="tags">';
      tags.forEach(tag => {
        // 检查是否有类型标识 (success:, warning:, error:)
        const typeMatch = tag.match(/^(success|warning|error):\s*(.+)$/);
        if (typeMatch) {
          tagsHtml += `<span class="tag tag-${typeMatch[1]}">${escapeHtml(typeMatch[2])}</span>`;
        } else {
          tagsHtml += `<span class="tag">${escapeHtml(tag)}</span>`;
        }
      });
      tagsHtml += '</div>';
      return tagsHtml;
    });

    // 6. 识别进度条
    // 格式: :::progress[标签] 75% :::
    enhanced = enhanced.replace(/:::progress\[([^\]]+)\]\s+(\d+)%\s*:::/gi, (match, label, percent) => {
      return `<div class="progress">
        <div class="progress-label">
          <span>${escapeHtml(label)}</span>
          <span>${percent}%</span>
        </div>
        <div class="progress-bar-container">
          <div class="progress-bar" style="width: ${percent}%"></div>
        </div>
      </div>`;
    });

    // 7. 识别徽章
    // 格式: [badge:primary]文本[/badge]
    enhanced = enhanced.replace(/\[badge:(primary|success|warning|danger)\](.+?)\[\/badge\]/gi, (match, type, text) => {
      return `<span class="badge badge-${type}">${escapeHtml(text)}</span>`;
    });

    // 8. 识别键盘快捷键
    // 格式: [[Ctrl+C]]
    enhanced = enhanced.replace(/\[\[([^\]]+)\]\]/g, (match, key) => {
      return `<kbd>${escapeHtml(key)}</kbd>`;
    });

    return enhanced;
  }

  /**
   * 添加交互式组件的事件监听器
   */
  function attachInteractiveListeners(scope) {
    // 可折叠面板
    scope.querySelectorAll('.collapsible-header').forEach(header => {
      header.addEventListener('click', () => {
        const collapsible = header.closest('.collapsible');
        collapsible?.classList.toggle('collapsed');
      });
    });

    // 选项卡
    scope.querySelectorAll('.tab-header').forEach(header => {
      header.addEventListener('click', () => {
        const tabs = header.closest('.tabs');
        if (!tabs) return;

        // 移除所有 active 状态
        tabs.querySelectorAll('.tab-header').forEach(h => h.classList.remove('active'));
        tabs.querySelectorAll('.tab-content').forEach(c => c.classList.remove('active'));

        // 添加当前 active 状态
        header.classList.add('active');
        const targetId = header.getAttribute('data-target');
        if (targetId) {
          tabs.querySelector(targetId)?.classList.add('active');
        }
      });
    });
  }

  function decodeEntities(str){
    if (!str) return '';
    let s = String(str);
    // collapse double-encoded ampersands first
    s = s.replace(/&amp;amp;/g, '&amp;');
    // common named entities
    s = s.replace(/&amp;/g, '&');
    s = s.replace(/&lt;/g, '<');
    s = s.replace(/&gt;/g, '>');
    s = s.replace(/&quot;/g, '"');
    s = s.replace(/&#39;/g, "'");
    s = s.replace(/&nbsp;/g, ' ');
    return s;
  }
  function normalizeMd(src){
    if (!src) return '';
    let s = decodeEntities(String(src));
    // Convert simple HTML emphasis to markdown to avoid raw tags showing
    s = s.replace(/<strong>([\s\S]*?)<\/strong>/gi, '**$1**');
    s = s.replace(/<em>([\s\S]*?)<\/em>/gi, '*$1*');
    // Ensure headers and lists start on new lines
    s = s.replace(/(?!^)###/g, '\n###');
    s = s.replace(/(?!^)##/g, '\n##');
    s = s.replace(/(?!^)# /g, '\n# ');
    s = s.replace(/(\S)(\d+\.\s)/g, '$1\n$2');
    s = s.replace(/(\S)(-\s)/g, '$1\n$2');
    // Auto-insert alignment row for simple pipe tables missing it (outside code fences)
    try {
      const unifyRow = (ln) => ln
        // fullwidth/box-drawing vertical bars → standard '|'
        .replace(/[\uFF5C\u2502]/g, '|')
        // various dash characters used as table rulers → '-'
        .replace(/[\u2014\u2015\u2500\u2501\u2212\uFF0D]+/g, '-');

      const lines = s.split(/\r?\n/);
      let out = [], inFence = false;
      const isPipeRow = (str) => (str.includes('|') && (str.split('|').length - 1) >= 2);
      const isAlignRow = (str) => /^\s*\|?\s*(?::?-{3,}:?\s*\|)+\s*:?-{3,}:?\s*\|?\s*$/.test(str);
      for (let i = 0; i < lines.length; i++) {
        let ln = lines[i];
        if (/^```/.test(ln.trim())) { inFence = !inFence; out.push(ln); continue; }
        // unify pseudo-table characters for detection + rendering
        if (!inFence) ln = unifyRow(ln);
        if (!inFence && isPipeRow(ln)) {
          const next = unifyRow(lines[i + 1] || '');
          if (!isAlignRow(next)) {
            // ensure at least one more pipe row below
            const below = unifyRow(lines[i + 1] || '');
            if (isPipeRow(below)) {
              const cols = ln.replace(/^\s*\|?/, '').replace(/\|\s*$/, '').split('|').length;
              const align = '|' + Array(cols).fill(' --- ').join('|') + '|';
              out.push(ln);
              out.push(align);
              continue;
            }
          }
        }
        out.push(ln);
      }
      s = out.join('\n');
    } catch(e){}
    // Fix common glued commands from model output
    s = s.replace(/\bcmdecho\b/gi, 'cmd echo');
    s = s.replace(/\bbashmkdir\b/gi, 'bash mkdir');
    s = s.replace(/powershell\s*new-?item-?path/gi, 'powershell New-Item -Path');
    s = s.replace(/powershellnew-?item-?path/gi, 'powershell New-Item -Path');
    return s;
  }
  function renderMarkdown(md){
    try {
      if (window.markdownit) {
        const mdlib = window.markdownit({ html:false, linkify:true, breaks:true, typographer:true });
        if (window.markdownitTaskLists) mdlib.use(window.markdownitTaskLists, {enabled:true});
        if (window.markdownitEmoji) mdlib.use(window.markdownitEmoji);
        const mmt = window.markdownItMultimdTable || window.markdownItMultiMdTable || window.markdownitMultimdTable;
        if (typeof mmt === 'function') mdlib.use(mmt);
        const norm = smartTableize(normalizeMd(md));
        let html = mdlib.render(norm);
        // Fallback B: if still no table but looks like a pipe table block, force convert
        if (!/\<table[\s>]/i.test(html) && /(\|.+\|)(\r?\n\|.+\|)/.test(norm)) {
          const forced = forcePipeTables(norm);
          if (forced) html = forced;
        }
        // 应用增强内容渲染
        html = enhanceContent(html);
        return html;
      }
    } catch (e) {}
    let html = mdToHtml(md);
    // 应用增强内容渲染
    html = enhanceContent(html);
    return html;
  }

  function forcePipeTables(src){
    try{
      const lines = src.split(/\r?\n/);
      let blocks = [], cur=[]; let inFence=false;
      const unifyRow = (ln)=> ln.replace(/[\uFF5C\u2502]/g,'|').replace(/[\u2014\u2015\u2500\u2501\u2212\uFF0D]+/g,'-'); const isPipeRow = (str)=> { const s = unifyRow(str); return (s.includes('|') && (s.split('|').length-1)>=2); };
      for (let i=0;i<lines.length;i++){
        const ln = lines[i];
        if (/^```/.test(ln.trim())) { inFence=!inFence; if(cur.length){blocks.push(cur);cur=[];} continue; }
        if (!inFence && isPipeRow(ln)) { cur.push(ln); continue; }
        if (cur.length){ blocks.push(cur); cur=[]; }
      }
      if (cur.length) blocks.push(cur);
      if (!blocks.length) return null;
      // If the whole content is a single pipe block, convert to table
      if (blocks.length===1 && blocks[0].length>=2){
        const rows = blocks[0].map(r=> r.replace(/^\s*\|?/, '').replace(/\|\s*$/, '').split('|').map(c=>c.trim()));
        const cols = Math.max(...rows.map(r=>r.length));
        const head = rows[0];
        const body = rows.slice(1);
        const ths = head.map(c=> `<th>${escapeHtml(c)}</th>`).join('');
        const trs = body.map(r=> '<tr>' + Array.from({length:cols}, (_,i)=> `<td>${escapeHtml(r[i]||'')}</td>`).join('') + '</tr>').join('');
        return `<div class="table-wrap"><table><thead><tr>${ths}</tr></thead><tbody>${trs}</tbody></table></div>`;
      }
      return null;
    }catch(e){ return null; }
  }
    // Convert colon-pairs blocks into 2-column markdown tables
    // Convert colon-pairs blocks into 2-column markdown tables
  function smartTableize(src){
    try{
      const lines = src.split(/\r?\n/);
      let out = [], i=0, inFence=false;
      const isColonLine = (s) => /[^:：\|]{1,30}[：:]+\s*.+/.test(s);
      const isEmpty = (s)=> /^\s*$/.test(s);
      const guessHeader = (prev) => {
        if (!prev) return ["项","说明"];
        if (/污渍|类型|类别/.test(prev) && /(处理|方法|说明)/.test(prev)) return ["污渍类型","处理方法"];
        if (/键|值|参数|属性/.test(prev)) return ["键","值"];
        return ["项","说明"];
      };
      while (i<lines.length){
        const ln = lines[i];
        if (/^```/.test(ln.trim())){ inFence=!inFence; out.push(ln); i++; continue; }
        if (!inFence && isColonLine(ln)){
          let block=[]; let j=i;
          while (j<lines.length && isColonLine(lines[j])){ block.push(lines[j]); j++; }
          if (block.length>=2){
            let k=i-1, prev=''; while (k>=0 && isEmpty(lines[k])) k--; if (k>=0) prev=lines[k];
            const [h1,h2] = guessHeader(prev);
            out.push(`| ${h1} | ${h2} |`);
            out.push(`| --- | --- |`);
            for (const row of block){
              const m = row.split(/[：:]/); const left=(m.shift()||'').trim(); const right=m.join(':').replace(/^\s+/, '');
              out.push(`| ${left} | ${right} |`);
            }
            i=j; continue;
          }
        }
        out.push(ln); i++;
      }
      return out.join('\n');
    }catch(e){ return src; }
  }
  // After HTML render, detect pseudo-table blocks and rebuild as <table>
  function postEnhanceTables(scope){
    const bodyEl = scope instanceof Element ? scope : null;
    if (!bodyEl) return;
    let text = bodyEl.innerText || "";
    text = text.replace(/\uFF5C|\u2502/g,'|').replace(/\u2014|\u2015|\u2500|\u2501|\u2212|\uFF0D+/g,'-').replace(/\|\|+/g,'||');
    if (!/\r?\n/.test(text) && text.includes('||')) { text = text.split('||').join('\n'); }
    const lines = text.split(/\r?\n/).map(s=>s.trim()).filter(Boolean);
    if (lines.length < 2) return;
    const unify = (s)=> s.replace(/[\uFF5C\u2502]/g,'|');
    const pipeLines = lines.filter(l => (unify(l).includes('|') && (unify(l).split('|').length-1)>=2));
    const colonLines = lines.filter(l => /[^:：\|]{1,30}[：:]+\s*.+/.test(l));
    // case 1: pure pipe table lines
    if (pipeLines.length >= 2 && pipeLines.length >= lines.length * 0.8){
      const norm = pipeLines.map(unify);
      // remove possible align row
      let rows = norm.slice();
      if (rows.length>=2 && /^\s*\|?\s*(?::?-{3,}:?\s*\|)+\s*:?-{3,}:?\s*\|?\s*$/.test(rows[1])){
        rows.splice(1,1);
      }
      const cells = rows.map(r=> r.replace(/^\s*\|?/, '').replace(/\|\s*$/, '').split('|').map(c=>c.trim()));
      const cols = Math.max(...cells.map(r=>r.length));
      const head = cells[0];
      const body = cells.slice(1);
      const ths = head.map(c=> `<th>${escapeHtml(c)}</th>`).join('');
      const trs = body.map(r=> '<tr>' + Array.from({length:cols}, (_,i)=> `<td>${escapeHtml(r[i]||'')}</td>`).join('') + '</tr>').join('');
      bodyEl.innerHTML = `<div class="table-wrap"><table><thead><tr>${ths}</tr></thead><tbody>${trs}</tbody></table></div>`;
      return;
    }
    // case 2: colon kv list → two-column table
    if (colonLines.length >= 2 && colonLines.length >= lines.length * 0.8){
      const header = `<tr><th>项</th><th>说明</th></tr>`;
      const rows = colonLines.map(l => { const m=l.split(/[：:]/); const left=(m.shift()||'').trim(); const right=m.join(':').replace(/^\s+/, ''); return `<tr><td>${escapeHtml(left)}</td><td>${escapeHtml(right)}</td></tr>`; }).join('');
      bodyEl.innerHTML = `<div class="table-wrap"><table><thead>${header}</thead><tbody>${rows}</tbody></table></div>`;
    }
  }function mdToHtml(md){
  const lines = normalizeMd(md).split(/\r?\n/);
  let html = '';
  let inCode = false, codeLang = 'text', codeLines = [];
  let inList = null; // 'ul' | 'ol'
  let para = [];
  function flushCode(){ if (!inCode) return; html += `<pre class="code"><code class="language-${codeLang}">${escapeHtml(codeLines.join('\n'))}</code></pre>`; inCode=false; codeLines=[]; codeLang='text'; }
  function flushList(){ if (!inList) return; html += `</${inList}>`; inList=null; }
  function flushPara(){ if (!para.length) return; html += `<p>${mdInline(escapeHtml(para.join('\n'))).replace(/\n/g,'<br>')}</p>`; para=[]; }
  function parseTable(i){
    const header = lines[i]; const sep = lines[i+1]||'';
    if (!header || header.indexOf('|')===-1) return 0;

    // 检查是否有标题行（非管道符开头的文本）
    let titleLine = null;
    let headerLine = header;
    let sepLine = sep;
    let startRow = i;

    // 如果第一行不包含管道符或管道符很少，可能是表格标题
    const pipeCount = (header.match(/\|/g) || []).length;
    if (pipeCount <= 1 && lines[i+1] && lines[i+1].indexOf('|') > -1) {
      // 第一行是标题，第二行是表头
      titleLine = header.trim();
      headerLine = lines[i+1];
      sepLine = lines[i+2] || '';
      startRow = i + 1;
    }

    // 验证分隔符行
    if (!/^\s*\|?\s*(:?-+:?\s*\|)+\s*:?-+:?\s*\|?\s*$/.test(sepLine)) return 0;

    const headCells = headerLine.replace(/^\s*\|?|\|\s*$/g,'').split('|').map(c=>c.trim());
    const aligns = sepLine.replace(/^\s*\|?|\|\s*$/g,'').split('|').map(c=>{ const t=c.trim(); const l=t.startsWith(':'); const r=t.endsWith(':'); return l&&r?'center': r?'right': l?'left': null; });
    let r=startRow+2, rows=[];
    while (r<lines.length){
      const row=lines[r];
      if (!row || row.trim()==='' || row.indexOf('|')===-1) break;
      rows.push(row.replace(/^\s*\|?|\|\s*$/g,'').split('|').map(c=>c.trim()));
      r++;
    }

    // 如果有标题行，先输出标题
    if (titleLine) {
      html += `<div class="table-title" style="font-weight:600;margin-bottom:8px;color:var(--text);">${escapeHtml(titleLine)}</div>`;
    }

    html += '<div class="table-wrap"><table><thead><tr>' + headCells.map((c,k)=>`<th${aligns[k]?` style="text-align:${aligns[k]}"`:''}>${mdInline(escapeHtml(c))}</th>`).join('') + '</tr></thead>';
    html += '<tbody>' + rows.map(row=>'<tr>'+ row.map((c,k)=>`<td${aligns[k]?` style=\"text-align:${aligns[k]}\"`:''}>${mdInline(escapeHtml(c))}</td>`).join('') + '</tr>').join('') + '</tbody></table></div>';
    return r - i;
  }
  for (let i=0;i<lines.length;i++){
    const line = lines[i];
    const chk = line.replace(/^ {0,3}/,'');
    const fence = line.match(/^```(\w+)?\s*$/);
    if (fence){ if (inCode){ flushCode(); } else { flushPara(); flushList(); inCode=true; codeLang=fence[1]||'text'; } continue; }
    if (inCode){ codeLines.push(line); continue; }
    // blockquotes
    if (/^>\s?/.test(chk)){
      flushPara(); flushList();
      let bq=[]; let j=i;
      while (j<lines.length){ const l=lines[j]; const cl=l.replace(/^ {0,3}/,''); if (!/^>\s?/.test(cl)) break; bq.push(cl.replace(/^>\s?/,'').replace(/^\s+$/,'')); j++; }
      i=j-1; html += `<blockquote>${mdToHtml(bq.join('\n'))}</blockquote>`; continue;
    }
    const consumed = parseTable(i);
    if (consumed>0){ i += (consumed-1); continue; }
    const trimmed = chk.trim();
    if (/^\s*$/.test(line)){ flushPara(); flushList(); continue; }
    if (trimmed === '---' || trimmed === '***' || trimmed === '___') { flushPara(); flushList(); html += '<hr />'; continue; }
    // headings
    let m;
    if ((m = chk.match(/^###\s*(.*)$/))){ flushPara(); flushList(); html += `<h3>${mdInline(escapeHtml(m[1]))}</h3>`; continue; }
    if ((m = chk.match(/^##\s*(.*)$/))){ flushPara(); flushList(); html += `<h2>${mdInline(escapeHtml(m[1]))}</h2>`; continue; }
    if ((m = chk.match(/^#\s*(.*)$/))){ flushPara(); flushList(); html += `<h1>${mdInline(escapeHtml(m[1]))}</h1>`; continue; }
    // lists & task lists
    if ((m = chk.match(/^[-*]\s+(.*)$/))){
      const task = m[1].match(/^\[( |x|X)\]\s+(.*)$/);
      flushPara(); if (!inList) { inList='ul'; html += '<ul>'; }
      if (task){ const checked=/x/i.test(task[1]); html += `<li class="task"><input type="checkbox" disabled ${checked?'checked':''}> ${mdInline(escapeHtml(task[2]))}</li>`; continue; }
      html += `<li>${mdInline(escapeHtml(m[1]))}</li>`; continue;
    }
    if ((m = chk.match(/^\d+\.\s+(.*)$/))){ flushPara(); if (!inList){ inList='ol'; html += '<ol>'; } html += `<li>${mdInline(escapeHtml(m[1]))}</li>`; continue; }
    // paragraph fallback
    para.push(line);
  }
  if (inCode) flushCode(); flushPara(); flushList();
  return html;
}
  function addCodeActions(scope){
    scope.querySelectorAll('pre code').forEach(code => {
      const bar = document.createElement('div');
      bar.style.display='flex'; bar.style.justifyContent='flex-end'; bar.style.gap='6px'; bar.style.marginTop='6px';
      const btnCopy = document.createElement('button'); btnCopy.className='btn sm'; btnCopy.textContent='复制'; btnCopy.addEventListener('click', ()=>{ navigator.clipboard?.writeText(code.textContent||''); announce('已复制'); });
      const btnInsert = document.createElement('button'); btnInsert.className='btn sm'; btnInsert.textContent='插入'; btnInsert.addEventListener('click', ()=>{ applySuggestion('\n'+(code.textContent||'')+'\n'); });
      bar.appendChild(btnCopy); bar.appendChild(btnInsert);
      code.parentElement.insertAdjacentElement('afterend', bar);
    });
  }

  /**
   * 流式输出偏好设置管理
   */

  // 初始化流式输出偏好设置
  function initStreamingPreferences() {
    const enabledCheckbox = document.getElementById('aiStreamingEnabled');
    const speedSelect = document.getElementById('aiStreamingSpeed');

    if (enabledCheckbox) {
      // 从 LocalStorage 加载
      const enabled = localStorage.getItem('ai.streaming.enabled');
      if (enabled !== null) {
        enabledCheckbox.checked = enabled === 'true';
      }

      // 监听变化
      enabledCheckbox.addEventListener('change', (e) => {
        localStorage.setItem('ai.streaming.enabled', String(e.target.checked));
        syncPreferencesToBackend();
      });
    }

    if (speedSelect) {
      const speed = localStorage.getItem('ai.streaming.speed');
      if (speed) {
        speedSelect.value = speed;
      }

      speedSelect.addEventListener('change', (e) => {
        localStorage.setItem('ai.streaming.speed', e.target.value);
        syncPreferencesToBackend();
      });
    }
  }

  // 同步偏好设置到后端
  async function syncPreferencesToBackend() {
    const enabled = localStorage.getItem('ai.streaming.enabled') !== 'false';
    const speed = localStorage.getItem('ai.streaming.speed') || 'normal';

    const csrf = getCsrf();

    try {
      await fetch('/profile/preferences', {
        method: 'POST',
        headers: Object.assign(
          { 'Content-Type': 'application/json' },
          csrf.token ? { [csrf.header]: csrf.token } : {}
        ),
        body: JSON.stringify({
          aiStreamingEnabled: enabled,
          aiStreamingSpeed: speed
        })
      });
    } catch (err) {
      console.warn('Failed to sync streaming preferences to backend:', err);
      // 失败不影响使用，LocalStorage 已保存
    }
  }

  // Bootstrap: focus input + load current session
  window.addEventListener('DOMContentLoaded', () => {
    composer?.focus();
    const cur = loadCurrentSession();
    if (cur.length) cur.forEach(m => appendMessage(m.role, m.text));
    loadModels();
    initStreamingPreferences();  // ✅ 初始化流式输出偏好设置
  });
})();

