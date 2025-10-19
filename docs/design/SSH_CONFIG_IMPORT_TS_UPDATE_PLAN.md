# SSHConfigImportWizard.ts Tab版本更新计划

## 📋 更新概述

将 `SSHConfigImportWizard.ts` 从3步向导模式改造为Tab切换模式,对齐新版HTML模板。

---

## 🎯 核心改造点

### 1. 移除向导步骤逻辑 ❌

#### 移除的属性
```typescript
// 移除
private step: number = 1;  

// 保留
private servers: ServerImportPreview[] = [];
private selectedServers: ServerImportPreview[] = [];
private importResult: ServerImportResult | null = null;
```

#### 移除的方法
```typescript
// 移除
private nextStep()
private prevStep()
private updateStepDisplay()

// 移除相关逻辑
this.step = 2;
this.updateStepDisplay();
```

### 2. 新增Tab切换逻辑 ✅

#### 新增属性
```typescript
private currentTab: 'auto-scan' | 'upload' | 'manual' = 'auto-scan';
```

#### 新增方法
```typescript
/**
 * 切换到指定Tab
 */
private switchTab(tabName: 'auto-scan' | 'upload' | 'manual') {
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

### 3. 更新DOM选择器 🔄

#### 旧选择器 → 新选择器对照表

| 功能 | 旧选择器 | 新选择器 |
|------|----------|----------|
| 扫描按钮 | `#btnScanLocal` | `#btnStartScan` |
| 扫描状态 | `#scanLocalStatus` | `#scanningStatus` |
| 扫描结果 | `#scanLocalResult` | `#scanResult` |
| 文件上传区 | `#fileUploadArea` | `#uploadArea` |
| 文件输入 | `#sshConfigFileInput` | `#sshConfigFileInput` (不变) |
| 自定义路径输入 | `#customPathInput` | `#manualPathInput` |
| 路径示例 | `#pathExamples` | `.path-example-item` (新) |

#### 代码更新示例

```typescript
// 旧代码
const scanButton = document.getElementById('btnScanLocal');
const scanStatus = document.getElementById('scanLocalStatus');

// 新代码
const scanButton = document.getElementById('btnStartScan');
const scanningStatus = document.getElementById('scanningStatus');
const scanResult = document.getElementById('scanResult');
```

---

## 🔨 详细更新步骤

### Step 1: 更新私有属性

```typescript
export class SSHConfigImportWizard {
    // 移除
    // private step: number = 1;
    
    // 新增
    private currentTab: 'auto-scan' | 'upload' | 'manual' = 'auto-scan';
    
    // 保留
    private modal: HTMLElement | null = null;
    private servers: ServerImportPreview[] = [];
    private selectedServers: ServerImportPreview[] = [];
    private importResult: ServerImportResult | null = null;
    private uploadedFile: File | null = null;
    private customPath: string = "";
    private defaultPathInfo: DefaultPathInfo | null = null;
}
```

### Step 2: 更新 `attachEvents()` 方法

```typescript
private attachEvents() {
    if (!this.modal) return;
    
    if (this.modal.hasAttribute("data-ssh-config-events-attached")) {
        return;
    }
    
    console.log("Attaching events to SSH Config Import Modal...");
    
    // 【新增】Tab切换事件
    this.modal.querySelectorAll<HTMLButtonElement>('.import-tab').forEach(tab => {
        tab.addEventListener('click', () => {
            const tabName = tab.dataset.tab as 'auto-scan' | 'upload' | 'manual';
            if (tabName) {
                this.switchTab(tabName);
            }
        });
    });
    
    // 【新增】自动扫描按钮
    const btnStartScan = this.modal.querySelector<HTMLButtonElement>('#btnStartScan');
    btnStartScan?.addEventListener('click', () => {
        this.startAutoScan();
    });
    
    // 【更新】文件上传事件
    const uploadArea = this.modal.querySelector<HTMLDivElement>('#uploadArea');
    const fileInput = this.modal.querySelector<HTMLInputElement>('#sshConfigFileInput');
    
    // 点击上传区域触发文件选择
    uploadArea?.addEventListener('click', (e) => {
        if ((e.target as HTMLElement).tagName !== 'INPUT') {
            fileInput?.click();
        }
    });
    
    // 文件选择事件
    fileInput?.addEventListener('change', (e) => {
        const file = (e.target as HTMLInputElement).files?.[0];
        if (file) {
            this.handleFileUpload(file);
        }
    });
    
    // 拖拽事件
    uploadArea?.addEventListener('dragover', (e) => {
        e.preventDefault();
        uploadArea.classList.add('dragover');
    });
    
    uploadArea?.addEventListener('dragleave', () => {
        uploadArea.classList.remove('dragover');
    });
    
    uploadArea?.addEventListener('drop', (e) => {
        e.preventDefault();
        uploadArea.classList.remove('dragover');
        const file = e.dataTransfer?.files[0];
        if (file) {
            this.handleFileUpload(file);
        }
    });
    
    // 【新增】路径示例点击事件
    this.modal.querySelectorAll<HTMLDivElement>('.path-example-item').forEach(item => {
        item.addEventListener('click', () => {
            const path = item.dataset.path;
            if (path) {
                const pathInput = this.modal?.querySelector<HTMLInputElement>('#manualPathInput');
                if (pathInput) {
                    pathInput.value = path;
                }
            }
        });
    });
    
    // 【新增】解析自定义路径按钮
    const btnParsePath = this.modal.querySelector<HTMLButtonElement>('#btnParsePath');
    btnParsePath?.addEventListener('click', () => {
        const pathInput = this.modal?.querySelector<HTMLInputElement>('#manualPathInput');
        if (pathInput?.value) {
            this.parseCustomPath(pathInput.value);
        } else {
            showError('请输入配置文件路径');
        }
    });
    
    // 【新增】确认导入按钮
    const btnConfirmImport = this.modal?.querySelector<HTMLButtonElement>('#btnConfirmImport');
    btnConfirmImport?.addEventListener('click', () => {
        this.confirmImport();
    });
    
    this.modal.setAttribute("data-ssh-config-events-attached", "true");
}
```

### Step 3: 实现 `switchTab()` 方法

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

### Step 4: 实现 `startAutoScan()` 方法

```typescript
/**
 * 开始自动扫描
 */
private async startAutoScan() {
    console.log("Starting auto-scan...");
    
    const scanningStatus = this.modal?.querySelector<HTMLDivElement>('#scanningStatus');
    const scanResult = this.modal?.querySelector<HTMLDivElement>('#scanResult');
    const progressFill = this.modal?.querySelector<HTMLDivElement>('#progressFill');
    
    if (!scanningStatus || !scanResult) {
        console.error("Scan status elements not found");
        return;
    }
    
    try {
        // 显示扫描状态
        scanningStatus.classList.add('active');
        scanResult.classList.remove('active');
        
        // 4步进度动画
        const steps = [
            { id: 'step1', progress: 25, delay: 500, api: null },
            { id: 'step2', progress: 50, delay: 1000, api: 'scan-registry' },
            { id: 'step3', progress: 75, delay: 1500, api: 'scan-paths' },
            { id: 'step4', progress: 100, delay: 2000, api: 'validate' }
        ];
        
        // 调用后端API（示例）
        const response = await fetch('/api/ssh-config-import/scan-local', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' }
        });
        
        if (!response.ok) {
            throw new Error(`扫描失败: HTTP ${response.status}`);
        }
        
        const result: SSHConfigParseResult = await response.json();
        
        // 执行进度动画
        for (let i = 0; i < steps.length; i++) {
            await new Promise(resolve => setTimeout(resolve, steps[i].delay));
            
            const stepEl = this.modal?.querySelector<HTMLDivElement>(`#${steps[i].id}`);
            if (stepEl) {
                stepEl.classList.add('active');
                if (progressFill) {
                    progressFill.style.width = `${steps[i].progress}%`;
                }
            }
            
            // 标记前一步完成
            if (i > 0) {
                const prevStep = this.modal?.querySelector<HTMLDivElement>(`#${steps[i-1].id}`);
                if (prevStep) {
                    prevStep.classList.remove('active');
                    prevStep.classList.add('completed');
                    const icon = prevStep.querySelector('.scan-step-icon');
                    if (icon) icon.textContent = '✓';
                }
            }
        }
        
        // 标记最后一步完成
        const lastStep = this.modal?.querySelector<HTMLDivElement>(`#step4`);
        if (lastStep) {
            lastStep.classList.remove('active');
            lastStep.classList.add('completed');
            const icon = lastStep.querySelector('.scan-step-icon');
            if (icon) icon.textContent = '✓';
        }
        
        // 显示结果
        await new Promise(resolve => setTimeout(resolve, 500));
        scanningStatus.classList.remove('active');
        scanResult.classList.add('active');
        
        // 填充服务器列表
        if (result.servers && result.servers.length > 0) {
            this.servers = result.servers.map(server => ({
                ...server,
                selected: server.valid && !server.duplicate,
                editing: false
            }));
            this.updateSelectedServers();
            this.displayScanResult(result);
            showSuccess(`扫描完成！发现 ${result.totalHosts} 个配置`);
        } else {
            showError('未发现SSH配置文件');
        }
        
    } catch (error) {
        console.error("Auto-scan failed:", error);
        scanningStatus.classList.remove('active');
        showError(error instanceof Error ? error.message : '自动扫描失败');
    }
}
```

### Step 5: 实现 `displayScanResult()` 方法

```typescript
/**
 * 显示扫描结果
 */
private displayScanResult(result: SSHConfigParseResult) {
    const resultInfo = this.modal?.querySelector<HTMLDivElement>('#scanResultInfo');
    const subtitle = this.modal?.querySelector<HTMLParagraphElement>('#scanResultSubtitle');
    const tipContent = this.modal?.querySelector<HTMLSpanElement>('#scanTipContent');
    
    if (!resultInfo) return;
    
    // 更新副标题
    if (subtitle) {
        const clientCount = new Set(result.servers.map(s => this.guessClientName(s.hostname))).size;
        subtitle.textContent = `检测到 ${clientCount} 个已安装的SSH客户端`;
    }
    
    // 填充结果信息
    const clientSummary = this.groupByClient(result.servers);
    resultInfo.innerHTML = clientSummary.map(item => `
        <div class="info-item">
            <div class="info-label">
                ${this.getClientIcon(item.clientName)} ${item.clientName} ${item.version}
                <span class="badge badge-success">已安装</span>
            </div>
            <div class="info-value">
                ${item.sessionCount} 个会话 • ${item.configPath}
            </div>
        </div>
    `).join('');
    
    // 更新提示
    if (tipContent) {
        const totalSessions = result.servers.length;
        tipContent.textContent = `共发现 ${totalSessions} 个会话配置（来自${clientSummary.length}个客户端），系统将自动去重和合并。预计导入时间 3-5 分钟。`;
    }
}

/**
 * 根据hostname猜测客户端名称
 */
private guessClientName(hostname: string): string {
    if (hostname.includes('securecrt')) return 'SecureCRT';
    if (hostname.includes('xshell')) return 'Xshell';
    if (hostname.includes('tabby')) return 'Tabby';
    if (hostname.includes('mobaxterm')) return 'MobaXterm';
    if (hostname.includes('putty')) return 'PuTTY';
    return 'Unknown';
}

/**
 * 获取客户端图标
 */
private getClientIcon(clientName: string): string {
    const icons: Record<string, string> = {
        'SecureCRT': '🔐',
        'Xshell': '📡',
        'Tabby': '⚡',
        'MobaXterm': '🖥️',
        'PuTTY': '🔧',
        'Unknown': '💻'
    };
    return icons[clientName] || '💻';
}

/**
 * 按客户端分组
 */
private groupByClient(servers: ServerImportPreview[]): {
    clientName: string;
    version: string;
    sessionCount: number;
    configPath: string;
}[] {
    const grouped = new Map<string, {
        clientName: string;
        version: string;
        sessionCount: number;
        configPath: string;
    }>();
    
    servers.forEach(server => {
        const clientName = this.guessClientName(server.hostname);
        if (!grouped.has(clientName)) {
            grouped.set(clientName, {
                clientName,
                version: '未知版本',
                sessionCount: 0,
                configPath: server.hostname
            });
        }
        grouped.get(clientName)!.sessionCount++;
    });
    
    return Array.from(grouped.values());
}
```

### Step 6: 更新 `handleFileUpload()` 方法

```typescript
public async handleFileUpload(file: File): Promise<void> {
    console.log("handleFileUpload called:", file.name);

    try {
        // 验证逻辑保持不变...
        const validTypes = ["text/plain", "application/octet-stream", ""];
        if (file.type && !validTypes.includes(file.type)) {
            showError(`文件类型不支持：${file.type}`);
            return;
        }

        const MAX_FILE_SIZE = 1024 * 1024;
        if (file.size > MAX_FILE_SIZE) {
            showError(`文件大小超过限制：${this.formatFileSize(file.size)}`);
            return;
        }

        this.uploadedFile = file;

        const formData = new FormData();
        formData.append("file", file);

        // 显示上传状态
        const uploadResult = this.modal?.querySelector<HTMLDivElement>('#uploadResult');
        const uploadArea = this.modal?.querySelector<HTMLDivElement>('#uploadArea');
        if (uploadArea) {
            uploadArea.classList.add('uploading');
        }

        const response = await fetch("/api/ssh-config-import/upload", {
            method: "POST",
            body: formData,
        });

        if (!response.ok) {
            const errorData = await response.json().catch(() => null);
            throw new Error(errorData?.message || `上传失败：HTTP ${response.status}`);
        }

        const result: SSHConfigParseResult = await response.json();

        if (!result.servers || result.servers.length === 0) {
            showError("配置文件中未找到有效的SSH Host配置");
            return;
        }

        this.servers = result.servers.map(server => ({
            ...server,
            selected: server.valid && !server.duplicate,
            editing: false
        }));

        this.updateSelectedServers();

        // 【更新】显示上传结果（新Tab模式下在当前页显示）
        if (uploadResult) {
            uploadResult.classList.add('active');
            uploadResult.innerHTML = `
                <div class="success-box">
                    <div class="success-icon">✓</div>
                    <h3>上传成功</h3>
                    <div class="info-item">
                        <div class="info-label">解析到的Host数量</div>
                        <div class="info-value">${result.totalHosts}</div>
                    </div>
                    <div class="info-item">
                        <div class="info-label">可导入的服务器</div>
                        <div class="info-value">${this.servers.filter(s => s.valid && !s.duplicate).length}</div>
                    </div>
                </div>
            `;
        }

        showSuccess(`文件上传成功！解析到 ${result.totalHosts} 个Host`);

        // 【移除】不再自动切换到Step 2
        // this.step = 2;
        // this.updateStepDisplay();

    } catch (error) {
        console.error("File upload failed:", error);
        const uploadArea = this.modal?.querySelector<HTMLDivElement>('#uploadArea');
        if (uploadArea) {
            uploadArea.classList.remove('uploading');
        }
        showError(error instanceof Error ? error.message : "文件上传失败");
    }
}
```

### Step 7: 实现 `parseCustomPath()` 方法

```typescript
/**
 * 解析自定义路径
 */
private async parseCustomPath(path: string) {
    console.log("Parsing custom path:", path);
    
    const parseResult = this.modal?.querySelector<HTMLDivElement>('#parseResult');
    
    try {
        // 显示加载状态
        if (parseResult) {
            parseResult.innerHTML = '<div class="loading">解析中...</div>';
            parseResult.classList.add('active');
        }
        
        const response = await fetch('/api/ssh-config-import/parse-path', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ path })
        });
        
        if (!response.ok) {
            const errorData = await response.json().catch(() => null);
            throw new Error(errorData?.message || `解析失败：HTTP ${response.status}`);
        }
        
        const result: SSHConfigParseResult = await response.json();
        
        if (!result.servers || result.servers.length === 0) {
            showError('未找到有效的SSH配置');
            return;
        }
        
        this.servers = result.servers.map(server => ({
            ...server,
            selected: server.valid && !server.duplicate,
            editing: false
        }));
        
        this.updateSelectedServers();
        
        // 显示解析结果
        if (parseResult) {
            parseResult.innerHTML = `
                <div class="success-box">
                    <div class="success-icon">✓</div>
                    <h3>解析成功</h3>
                    <div class="info-item">
                        <div class="info-label">配置文件路径</div>
                        <div class="info-value">${path}</div>
                    </div>
                    <div class="info-item">
                        <div class="info-label">解析到的Host数量</div>
                        <div class="info-value">${result.totalHosts}</div>
                    </div>
                    <div class="info-item">
                        <div class="info-label">可导入的服务器</div>
                        <div class="info-value">${this.servers.filter(s => s.valid && !s.duplicate).length}</div>
                    </div>
                </div>
            `;
        }
        
        showSuccess(`解析成功！发现 ${result.totalHosts} 个配置`);
        
    } catch (error) {
        console.error("Parse path failed:", error);
        if (parseResult) {
            parseResult.innerHTML = `<div class="error">${error instanceof Error ? error.message : '解析失败'}</div>`;
        }
        showError(error instanceof Error ? error.message : '解析失败');
    }
}
```

### Step 8: 实现 `confirmImport()` 方法

```typescript
/**
 * 确认导入
 */
private async confirmImport() {
    if (this.selectedServers.length === 0) {
        showError('请先选择要导入的服务器');
        return;
    }
    
    console.log(`Confirming import of ${this.selectedServers.length} servers...`);
    
    try {
        const response = await fetch('/api/ssh-config-import/confirm', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ servers: this.selectedServers })
        });
        
        if (!response.ok) {
            const errorData = await response.json().catch(() => null);
            throw new Error(errorData?.message || `导入失败：HTTP ${response.status}`);
        }
        
        const result: ServerImportResult = await response.json();
        this.importResult = result;
        
        showSuccess(`导入成功！成功 ${result.successCount} 个，失败 ${result.failedCount} 个`);
        
        // 关闭模态框并刷新服务器列表
        this.close();
        eventBus.emit('server:imported', result);
        
    } catch (error) {
        console.error("Import failed:", error);
        showError(error instanceof Error ? error.message : '导入失败');
    }
}
```

### Step 9: 移除旧方法

```typescript
// 删除以下方法：
// - nextStep()
// - prevStep()
// - updateStepDisplay()
```

### Step 10: 更新 `reset()` 方法

```typescript
/**
 * 重置向导状态
 */
private reset() {
    // 重置为第一个Tab
    this.currentTab = 'auto-scan';
    this.switchTab('auto-scan');
    
    // 清空数据
    this.servers = [];
    this.selectedServers = [];
    this.importResult = null;
    this.uploadedFile = null;
    this.customPath = "";
    
    // 清空所有结果显示
    const scanResult = this.modal?.querySelector<HTMLDivElement>('#scanResult');
    const uploadResult = this.modal?.querySelector<HTMLDivElement>('#uploadResult');
    const parseResult = this.modal?.querySelector<HTMLDivElement>('#parseResult');
    
    scanResult?.classList.remove('active');
    uploadResult?.classList.remove('active');
    parseResult?.classList.remove('active');
    
    // 重置文件输入
    const fileInput = this.modal?.querySelector<HTMLInputElement>('#sshConfigFileInput');
    if (fileInput) {
        fileInput.value = '';
    }
    
    // 重置路径输入
    const pathInput = this.modal?.querySelector<HTMLInputElement>('#manualPathInput');
    if (pathInput) {
        pathInput.value = '';
    }
    
    console.log("Wizard reset to initial state");
}
```

---

## ✅ 验证清单

完成更新后,检查以下功能:

- [ ] Tab切换正常(auto-scan/upload/manual)
- [ ] 自动扫描按钮触发扫描
- [ ] 扫描进度4步动画正常
- [ ] 扫描结果正确显示
- [ ] 文件上传拖拽/点击正常
- [ ] 上传结果显示正常
- [ ] 路径示例点击填充输入框
- [ ] 自定义路径解析正常
- [ ] 确认导入触发API调用
- [ ] 关闭模态框重置所有状态

---

## 📄 完整文件结构

更新后的 `SSHConfigImportWizard.ts` 应包含:

1. **接口定义** (保持不变)
2. **私有属性** (移除step,新增currentTab)
3. **构造函数与初始化** (保持不变)
4. **事件绑定** (更新为Tab事件)
5. **Tab切换** (新增switchTab)
6. **自动扫描** (新增startAutoScan + displayScanResult)
7. **文件上传** (更新为Tab模式)
8. **自定义路径** (新增parseCustomPath)
9. **确认导入** (新增confirmImport)
10. **重置方法** (更新为Tab模式)
11. **工具方法** (保持不变)

---

**创建时间**: 2025-10-19  
**预计工时**: 3-4小时  
**风险等级**: 中 (大量DOM选择器更新)
