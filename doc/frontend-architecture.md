# Dev Debug Platform - 前端架构指南

## 🏗️ 架构概览

本项目采用**单页应用 (SPA) + 动态内容片段**的混合架构。

### 核心文件职责

#### 1. main-layout.html (主布局 - 唯一完整页面)
- **职责**: 完整HTML页面框架，所有JavaScript逻辑的容器
- **包含**:
  - 页面布局和导航
  - 所有全局JavaScript函数 
  - 事件处理器和业务逻辑
  - WebSocket连接和状态管理
- **🚨 重要**: 所有JavaScript修改必须在此文件中进行

#### 2. 内容片段文件 (templates/admin/*, templates/*.html)
- **职责**: 仅提供HTML内容片段，通过 `th:fragment` 定义
- **特点**:
  - 纯HTML+CSS，不包含JavaScript逻辑
  - 通过AJAX动态加载到main-layout
  - 示例: `servers.html`, `users.html`, `applications.html`
- **🚨 重要**: 在这些文件中修改JavaScript无效！

## 📋 开发规范

### ✅ 正确的开发流程
1. **HTML结构修改** → 在对应的片段文件 (如 `servers.html`)
2. **CSS样式修改** → 在对应的片段文件或 `main-layout.html`
3. **JavaScript逻辑修改** → 只在 `main-layout.html` 中
4. **事件处理器** → 只在 `main-layout.html` 中
5. **全局函数** → 只在 `main-layout.html` 中

### ❌ 常见错误
- 在片段文件中添加JavaScript逻辑
- 在片段文件中添加事件处理器
- 期望片段文件中的script标签生效

## 🔍 调试指南

### 如何确定修改位置？
1. **JavaScript相关** → 直接去 `main-layout.html`
2. **HTML结构** → 找到对应的片段文件
3. **CSS样式** → 可能在片段文件或main-layout中

### 调试技巧
```javascript
// 在浏览器控制台检查函数来源
console.log(initServerNameScrolling.toString());

// 检查当前页面加载的内容
console.log('当前页面:', currentPage);

// 查看动态加载的内容
console.log('内容容器:', document.getElementById('main-content'));
```

## 🎯 最佳实践

### 1. 文件命名约定
- `main-layout.html`: 主布局，包含所有JS
- `{module}.html`: 内容片段，仅HTML+CSS
- `{module}.js`: 独立JS文件（如果需要）

### 2. 注释规范
```html
<!-- main-layout.html -->
<script>
    // 🎯 [模块名] 相关功能
    function initServerNameScrolling() {
        // 功能实现
    }
</script>
```

```html
<!-- servers.html -->
<!-- 📋 内容片段: 服务器管理 -->
<!-- ⚠️  注意: JavaScript逻辑在 main-layout.html 中 -->
<div th:fragment="servers-content">
    <!-- 纯HTML内容 -->
</div>
```

### 3. 开发检查清单
- [ ] 确认修改的是否为JavaScript相关？
- [ ] 如果是JS相关，是否在main-layout.html中修改？
- [ ] 修改后是否硬刷新页面测试？
- [ ] 是否在浏览器控制台验证函数更新？

## 📊 文件关系图

```
main-layout.html (完整页面)
├── Navigation (导航)
├── Content Area (内容区域)
│   ├── loadContent('servers') → servers.html片段
│   ├── loadContent('users') → users.html片段
│   └── loadContent('applications') → applications.html片段
└── JavaScript Logic (JS逻辑)
    ├── initServerNameScrolling()
    ├── loadContent()
    ├── bindServerEvents()
    └── 所有事件处理器
```

## 🚨 重要提醒

**记住这个原则**: 
- **看到JavaScript问题** → 直接去 `main-layout.html`
- **不要在片段文件中写JavaScript**
- **修改后务必硬刷新测试**

这个架构设计的优势是代码集中管理，但需要开发者理解这种模式。