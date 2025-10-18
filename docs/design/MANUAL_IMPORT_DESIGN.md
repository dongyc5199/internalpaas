# 手动导入功能设计文档

## 📋 概述

手动导入功能支持用户**先选择SSH客户端,再指定配置路径**的两步流程,适用于自动扫描失败或需要手动指定路径的场景。

---

## 🎯 设计目标

### 1. **支持多客户端**
- 用户可以从5个主流SSH客户端中选择
- 每个客户端有独立的默认路径配置
- 智能提示该客户端的常用路径

### 2. **两步式流程**
- **步骤1**: 选择SSH客户端类型
- **步骤2**: 指定配置路径(配置目录 or 安装目录)

### 3. **灵活路径指定**
- **配置目录**(推荐): 直接指向Sessions配置文件夹
- **安装目录**: 系统自动推断配置位置

---

## 🎨 界面设计

### 步骤1: 选择SSH客户端

```
┌─────────────────────────────────────────────────────────┐
│ 🎯 步骤 1/2：选择 SSH 客户端                              │
│ 请选择要导入配置的 SSH 客户端类型                          │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  ┌──────┐  ┌──────┐  ┌──────┐  ┌──────┐  ┌──────┐     │
│  │  🔐  │  │  🐚  │  │  🎯  │  │  ⚡  │  │  🔧  │     │
│  │SecureCRT Xshell  Tabby  MobaXterm PuTTY │     │
│  │VanDyke NetSarang 开源终端 Mobatek  Simon  │     │
│  └──────┘  └──────┘  └──────┘  └──────┘  └──────┘     │
│                                                         │
│                                    [ 下一步 → ]         │
└─────────────────────────────────────────────────────────┘
```

#### 交互特性:
- ✅ **卡片式选择** - 5个客户端以卡片网格展示
- ✅ **视觉反馈** - 选中卡片高亮显示紫色边框和勾选图标
- ✅ **悬浮效果** - 鼠标悬停时卡片上浮+阴影
- ✅ **按钮状态** - 选择客户端后"下一步"按钮才激活

---

### 步骤2: 指定配置路径

```
┌─────────────────────────────────────────────────────────┐
│ 📁 步骤 2/2：指定配置路径                                 │
│ 请指定 SecureCRT 的配置目录或安装目录                      │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  ⦿ [ 推荐 ] 配置目录                                     │
│  ┌─────────────────────────────────────────┐            │
│  │ C:\Users\...\AppData\Roaming\VanDyke... │ [ 📁 浏览 ] │
│  └─────────────────────────────────────────┘            │
│  Windows 默认: %APPDATA%\VanDyke\Config\Sessions        │
│                                                         │
│                       或                                 │
│                                                         │
│  ○ 安装目录                                              │
│  ┌─────────────────────────────────────────┐            │
│  │ C:\Program Files\VanDyke Software\...   │ [ 📁 浏览 ] │
│  └─────────────────────────────────────────┘            │
│  系统将从安装目录自动推断配置文件位置                       │
│                                                         │
│                      [ ← 上一步 ]  [ ✓ 确认 ]          │
└─────────────────────────────────────────────────────────┘
```

#### 交互特性:
- ✅ **单选按钮** - 配置目录/安装目录二选一
- ✅ **路径输入框** - 支持手动输入或浏览选择
- ✅ **智能提示** - 根据所选客户端显示对应的默认路径
- ✅ **动态验证** - 输入路径后"确认"按钮才激活
- ✅ **返回功能** - 可返回上一步重新选择客户端

---

## 🔧 技术实现

### 1. 客户端路径配置表

| 客户端 | 配置目录默认路径 | 安装目录默认路径 |
|--------|-----------------|-----------------|
| **SecureCRT** | `%APPDATA%\VanDyke\Config\Sessions` | `C:\Program Files\VanDyke Software\SecureCRT` |
| **Xshell** | `%USERPROFILE%\Documents\NetSarang Computer\7\Xshell\Sessions` | `C:\Program Files\NetSarang\Xshell 7` |
| **Tabby** | `%APPDATA%\tabby\config.yaml` | `C:\Users\%USERNAME%\AppData\Local\Programs\Tabby` |
| **MobaXterm** | `%USERPROFILE%\Documents\MobaXterm\Sessions` | `C:\Program Files\Mobatek\MobaXterm` |
| **PuTTY** | `HKEY_CURRENT_USER\Software\SimonTatham\PuTTY\Sessions` | `C:\Program Files\PuTTY` |

### 2. 状态管理

```javascript
// 全局状态
let selectedClient = null;  // 当前选中的客户端

// 客户端配置字典
const clientPathConfig = {
    securecrt: {
        name: 'SecureCRT',
        configPath: '%APPDATA%\\VanDyke\\Config\\Sessions',
        installPath: 'C:\\Program Files\\VanDyke Software\\SecureCRT'
    },
    // ... 其他客户端
};
```

### 3. 步骤切换逻辑

```javascript
// 步骤1 → 步骤2
nextToPathBtn.addEventListener('click', () => {
    const config = clientPathConfig[selectedClient];
    
    // 更新提示文本
    selectedClientNameEl.textContent = config.name;
    configPathHelp.textContent = `Windows 默认: ${config.configPath}`;
    
    // 切换显示
    manualStep1.style.display = 'none';
    manualStep2.style.display = 'block';
});

// 步骤2 → 步骤1
backToClientBtn.addEventListener('click', () => {
    manualStep2.style.display = 'none';
    manualStep1.style.display = 'block';
});
```

### 4. 路径验证

```javascript
// 实时验证输入框
[configPathInput, installPathInput].forEach(input => {
    input.addEventListener('input', () => {
        const isConfigPath = pathTypeConfig.checked;
        const hasValue = isConfigPath 
            ? configPathInput.value.trim() 
            : installPathInput.value.trim();
        
        confirmPathBtn.disabled = !hasValue;
    });
});
```

---

## 📊 用户流程

```mermaid
graph TD
    A[点击"手动指定"Tab] --> B[步骤1: 显示5个客户端卡片]
    B --> C{选择客户端}
    C --> D["下一步"按钮激活]
    D --> E[点击"下一步"]
    E --> F[步骤2: 显示路径配置界面]
    F --> G[自动填充该客户端的默认路径提示]
    G --> H{选择路径类型}
    H --> I[配置目录/安装目录]
    I --> J[输入或浏览选择路径]
    J --> K["确认"按钮激活]
    K --> L[点击"确认"]
    L --> M[显示确认提示]
    M --> N["确认导入"按钮激活]
    
    F --> O[点击"上一步"]
    O --> B
```

---

## 🎯 设计亮点

### 1. **渐进式引导**
- 分两步完成,每步目标明确
- 避免一次性展示过多选项造成认知负担

### 2. **智能默认值**
- 根据所选客户端自动显示对应的默认路径
- 减少用户输入,提高效率

### 3. **灵活配置**
- 支持配置目录(精准)和安装目录(推断)两种方式
- 适应不同用户的使用习惯和权限限制

### 4. **视觉一致性**
- 沿用紫色渐变主题
- 卡片选择、单选框、按钮样式统一

### 5. **容错性设计**
- 支持返回上一步重新选择
- 实时验证确保路径有效才能继续

---

## 🔄 与其他导入方式的对比

| 特性 | 自动扫描 | **手动指定** | 文件上传 |
|------|---------|------------|---------|
| **适用场景** | 标准安装路径 | 自定义路径/便携版 | 离线配置文件 |
| **操作步骤** | 1步(点击扫描) | 2步(选客户端+指定路径) | 1步(上传文件) |
| **用户控制** | 低(自动检测) | 高(手动指定) | 中(选择文件) |
| **成功率** | 高(常规环境) | 100%(用户主导) | 高(文件有效) |
| **技术要求** | 文件系统扫描 | 路径解析+推断 | 文件解析 |

---

## 📝 实现清单

- ✅ **步骤1界面** - 5个客户端卡片网格布局
- ✅ **卡片选择交互** - 单选、高亮、勾选图标
- ✅ **步骤2界面** - 路径类型单选+输入框
- ✅ **步骤切换** - 下一步/上一步按钮逻辑
- ✅ **智能提示** - 根据客户端显示默认路径
- ✅ **路径验证** - 输入检测+按钮状态控制
- ✅ **浏览功能** - 文件夹选择(模拟)
- ✅ **确认提示** - 显示客户端+路径信息

---

## 🚀 未来增强

1. **真实文件夹选择** - 调用系统文件选择对话框
2. **路径有效性检测** - 检查路径是否存在+是否包含配置文件
3. **智能路径推荐** - 扫描用户系统显示可能的路径列表
4. **批量导入** - 支持一次选择多个客户端路径
5. **路径历史** - 记录用户常用路径,下次快速选择

---

## 📄 相关文件

- **原型文件**: `server-group-import-modal.html`
- **整体设计**: `SERVER_IMPORT_MODAL_DESIGN.md`
- **文档索引**: `PROTOTYPE_README.md`
