# 导航图标资源规范

## 1. 图标编码规范

### 1.1 编码原则
- 使用 **两位大写字母/符号** 编码，便于识别和维护
- 编码需要直观反映功能含义
- 跨角色共享的图标使用相同编码
- 特殊功能使用符号编码（如 ? ! 等）

### 1.2 管理员图标编码
| 编码 | 功能模块 | 英文描述 | 中文描述 |
|------|----------|----------|----------|
| **OV** | Overview | 控制台概览 | 工作区概览 |
| **SV** | Servers | 服务器管理 | 服务器群组 |
| **US** | Users | 用户权限 | 用户与权限 |
| **AU** | Audit | 审计记录 | 审计日志 |
| **AL** | Alerts | 告警配置 | 告警设置 |
| **SC** | System Config | 系统配置 | 系统设置 |
| **?** | Help | 帮助反馈 | 帮助中心 |

### 1.3 开发者图标编码
| 编码 | 功能模块 | 英文描述 | 中文描述 |
|------|----------|----------|----------|
| **AP** | Applications | 应用管理 | 应用总览 |
| **DP** | Deployments | 部署任务 | 部署流水线 |
| **LG** | Logs | 日志中心 | 日志管理 |
| **EV** | Environments | 环境面板 | 环境管理 |
| **IN** | Integrations | 集成管理 | 第三方集成 |
| **PF** | Profile | 个人档案 | 用户档案 |
| **!** | Feedback | 问题反馈 | 意见反馈 |

### 1.4 底部链接图标编码
| 编码 | 功能模块 | 描述 |
|------|----------|------|
| **DC** | Docs Center | 文档中心 |
| **SP** | Support | 支持服务 |
| **API** | API Docs | API 文档 |
| **FB** | Feedback | 意见反馈 |
| **CI** | CI Status | CI 状态 |

## 2. 图标主题规范

### 2.1 管理员主题
- **主色渐变**：`linear-gradient(135deg, #2F9BFF, #3BC6B8)`
- **背景色**：`rgba(59, 130, 246, 0.12)`
- **文字色**：`rgba(59, 130, 246, 1)`
- **选中指示条**：与主色渐变一致

### 2.2 开发者主题
- **主色渐变**：`linear-gradient(135deg, #6C63FF, #3D8BFF)`
- **背景色**：`rgba(108, 99, 255, 0.14)`
- **文字色**：`rgba(108, 99, 255, 1)`
- **选中指示条**：与主色渐变一致

## 3. 图标映射策略

### 3.1 当前实现方式
```java
// NavigationItemDto.badge 字段存储图标编码
createMenuItem("工作区概览", "overview", "overview", "OV", true)
```

### 3.2 模板渲染
```html
<span class="nav-item-icon" th:text="${menu.badge}">OV</span>
```

### 3.3 CSS类定义
```css
.nav-item-icon {
    width: 28px;
    height: 28px;
    border-radius: 12px;
    background: var(--icon-bg-color);
    color: var(--icon-text-color);
    font-weight: 600;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    font-size: 12px;
    transition: all 0.2s ease;
}
```

## 4. 扩展策略

### 4.1 SVG 图标升级
- 预留 SVG 图标映射机制：`data-icon="OV"`
- 可通过 JavaScript 将编码映射为 SVG 图标
- 保持编码不变，仅更换渲染方式

### 4.2 字体图标集成
- 可集成 Lucide、Heroicons 等图标库
- 通过编码映射到具体图标名称
- 示例：`OV → home`，`SV → server`

### 4.3 动态主题支持
- 图标颜色跟随用户角色主题
- 支持浅色/暗色模式自动适配
- 保持一致的视觉层次

## 5. 维护指南

### 5.1 添加新图标
1. 在本文档中注册新编码
2. 更新 `MainLayoutController.java` 中的 `createMenuItem` 调用
3. 验证原型页面中的图例展示
4. 测试不同主题下的显示效果

### 5.2 编码变更
1. 优先考虑向后兼容
2. 批量更新相关模板和样式
3. 更新文档和原型页面
4. 执行回归测试

### 5.3 质量检查
- 图标编码唯一性检查
- 主题色彩对比度验证（>= 4.5:1）
- 响应式布局适配测试
- 键盘导航可访问性验证

## 6. 主题适配详细说明

### 6.1 暗色主题修复记录
**问题**: 初始实现中，图标组件在暗色主题下显示异常，侧边栏背景和导航项颜色未正确切换。

**根因**: CSS选择器不匹配。主模板使用 `.shell-theme-dark` 类，而图标组件CSS使用 `[data-theme="dark"]` 属性选择器。

**解决方案**:
1. **图标组件CSS适配** - 添加 `.shell-theme-dark` 选择器支持
2. **侧边栏暗色主题** - 在main-layout.html中添加完整的暗色主题样式
3. **导航项适配** - 确保所有导航元素在暗色模式下正确显示

### 6.2 暗色主题样式规范
```css
/* 图标组件暗色主题 */
.shell-theme-dark .nav-icon {
    background: rgba(59, 130, 246, 0.15);
    color: rgba(96, 165, 250, 1);
}

.shell-theme-dark .nav-icon--developer {
    background: rgba(108, 99, 255, 0.18);
    color: rgba(129, 140, 248, 1);
}

/* 侧边栏暗色主题 */
.shell-theme-dark .shell-sidebar {
    background: linear-gradient(180deg, rgba(30,41,59,0.95), rgba(17,24,39,0.92));
    border-right: 1px solid rgba(148,163,184,0.32);
}

/* 导航项暗色主题 */
.shell-theme-dark .nav-item {
    color: rgba(255,255,255,0.88);
    border-color: rgba(148,163,184,0.18);
}

.shell-theme-dark .nav-item:hover {
    background: rgba(96,165,250,0.18);
    border-color: rgba(96,165,250,0.35);
    color: rgba(147,197,253,1);
}
```

### 6.3 兼容性策略
为确保最大兼容性，同时支持两种主题选择器：
- `.shell-theme-dark` - 主模板系统使用
- `[data-theme="dark"]` - 测试页面和未来扩展使用

---

**文档版本**: v1.1
**最后更新**: 2025-09-25
**状态**: T7 导航图标定义 - 已完成规范制定及暗色主题修复