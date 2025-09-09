# 内网部署指南 - 离线资源本地化

## 问题分析

当前项目依赖大量外部CDN资源，在内网环境下会导致以下问题：
- 页面样式无法加载（Bootstrap）
- JavaScript功能失效（Chart.js、XTerm.js等）
- 图标无法显示（Font Awesome已本地化）
- 图表和数据可视化无法工作
- 终端功能无法使用

## 依赖库清单

### 必须本地化的库：
1. **Bootstrap** (5.1.3, 5.3.0, 5.3.2)
2. **Chart.js** (4.4.0) + 插件
3. **XTerm.js** (5.3.0) + 插件
4. **Prism.js** (1.29.0) - 代码高亮
5. **Flatpickr** - 日期选择器
6. **SockJS/STOMP** - WebSocket支持
7. **HammerJS** (2.0.8) - 触摸手势

### 已本地化的库：
- ✅ **Font Awesome** - 已在 `/static/vendor/fontawesome/`

## 解决方案

### 方案一：手动下载（推荐）

运行以下命令下载所有依赖到本地：

#### 1. 创建目录结构
```bash
mkdir -p src/main/resources/static/vendor/{bootstrap,chartjs,xterm,prism,flatpickr,sockjs,hammerjs}
```

#### 2. 下载Bootstrap
```bash
# Bootstrap 5.1.3 (主要版本)
curl -o src/main/resources/static/vendor/bootstrap/bootstrap.min.css "https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/css/bootstrap.min.css"
curl -o src/main/resources/static/vendor/bootstrap/bootstrap.bundle.min.js "https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/js/bootstrap.bundle.min.js"

# Bootstrap 5.3.0
curl -o src/main/resources/static/vendor/bootstrap/bootstrap-5.3.0.min.css "https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css"
curl -o src/main/resources/static/vendor/bootstrap/bootstrap-5.3.0.bundle.min.js "https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"

# Bootstrap 5.3.2
curl -o src/main/resources/static/vendor/bootstrap/bootstrap-5.3.2.min.css "https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css"
curl -o src/main/resources/static/vendor/bootstrap/bootstrap-5.3.2.bundle.min.js "https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/js/bootstrap.bundle.min.js"
```

#### 3. 下载Chart.js及插件
```bash
curl -o src/main/resources/static/vendor/chartjs/chart.min.css "https://cdn.jsdelivr.net/npm/chart.js@4.4.0/dist/chart.min.css"
curl -o src/main/resources/static/vendor/chartjs/chart.umd.js "https://cdn.jsdelivr.net/npm/chart.js@4.4.0/dist/chart.umd.js"
curl -o src/main/resources/static/vendor/chartjs/chartjs-adapter-date-fns.bundle.min.js "https://cdn.jsdelivr.net/npm/chartjs-adapter-date-fns/dist/chartjs-adapter-date-fns.bundle.min.js"
curl -o src/main/resources/static/vendor/chartjs/chartjs-plugin-zoom.min.js "https://cdn.jsdelivr.net/npm/chartjs-plugin-zoom/dist/chartjs-plugin-zoom.min.js"
```

#### 4. 下载XTerm.js及插件
```bash
curl -o src/main/resources/static/vendor/xterm/xterm.css "https://cdn.jsdelivr.net/npm/xterm@5.3.0/css/xterm.css"
curl -o src/main/resources/static/vendor/xterm/xterm.js "https://cdn.jsdelivr.net/npm/xterm@5.3.0/lib/xterm.js"
curl -o src/main/resources/static/vendor/xterm/xterm-addon-fit.js "https://cdn.jsdelivr.net/npm/xterm-addon-fit@0.8.0/lib/xterm-addon-fit.js"
curl -o src/main/resources/static/vendor/xterm/xterm-addon-web-links.js "https://cdn.jsdelivr.net/npm/xterm-addon-web-links@0.9.0/lib/xterm-addon-web-links.js"
curl -o src/main/resources/static/vendor/xterm/xterm-addon-search.js "https://cdn.jsdelivr.net/npm/xterm-addon-search@0.13.0/lib/xterm-addon-search.js"
```

#### 5. 下载Prism.js
```bash
curl -o src/main/resources/static/vendor/prism/prism.min.css "https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/themes/prism.min.css"
curl -o src/main/resources/static/vendor/prism/prism-tomorrow.min.css "https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/themes/prism-tomorrow.min.css"
curl -o src/main/resources/static/vendor/prism/prism.min.js "https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/prism.min.js"
curl -o src/main/resources/static/vendor/prism/prism-json.min.js "https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/components/prism-json.min.js"
curl -o src/main/resources/static/vendor/prism/prism-yaml.min.js "https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/components/prism-yaml.min.js"
```

#### 6. 下载Flatpickr
```bash
curl -o src/main/resources/static/vendor/flatpickr/flatpickr.min.css "https://cdn.jsdelivr.net/npm/flatpickr/dist/flatpickr.min.css"
curl -o src/main/resources/static/vendor/flatpickr/flatpickr.min.js "https://cdn.jsdelivr.net/npm/flatpickr/dist/flatpickr.min.js"
curl -o src/main/resources/static/vendor/flatpickr/zh.js "https://cdn.jsdelivr.net/npm/flatpickr/dist/l10n/zh.js"
```

#### 7. 下载WebSocket库
```bash
curl -o src/main/resources/static/vendor/sockjs/sockjs.min.js "https://cdn.jsdelivr.net/npm/sockjs-client@1/dist/sockjs.min.js"
curl -o src/main/resources/static/vendor/sockjs/stomp.min.js "https://cdn.jsdelivr.net/npm/stompjs@2.3.3/lib/stomp.min.js"
```

#### 8. 下载HammerJS
```bash
curl -o src/main/resources/static/vendor/hammerjs/hammer.min.js "https://cdn.jsdelivr.net/npm/hammerjs@2.0.8/hammer.min.js"
```

### 方案二：Maven/NPM管理（长期方案）

在pom.xml中添加WebJars依赖：

```xml
<dependencies>
    <!-- Bootstrap WebJar -->
    <dependency>
        <groupId>org.webjars</groupId>
        <artifactId>bootstrap</artifactId>
        <version>5.1.3</version>
    </dependency>
    
    <!-- Chart.js WebJar -->
    <dependency>
        <groupId>org.webjars.npm</groupId>
        <artifactId>chart.js</artifactId>
        <version>4.4.0</version>
    </dependency>
    
    <!-- 其他WebJars... -->
</dependencies>
```

## 修改HTML文件

下载完成后，需要修改所有HTML文件中的CDN链接为本地路径。

### 示例修改：

**修改前：**
```html
<link href="https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/css/bootstrap.min.css" rel="stylesheet">
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/js/bootstrap.bundle.min.js"></script>
```

**修改后：**
```html
<link href="/vendor/bootstrap/bootstrap.min.css" rel="stylesheet">
<script src="/vendor/bootstrap/bootstrap.bundle.min.js"></script>
```

## 需要修改的文件列表

以下文件需要将CDN链接替换为本地路径：

### 主要页面：
- `templates/applications.html`
- `templates/application-detail.html` 
- `templates/user-profile.html`

### 管理页面：
- `templates/admin/server-detail.html`
- `templates/admin/server-logs.html`
- `templates/admin/config-editor.html`

### 监控页面：
- `templates/monitoring/history-dashboard.html`
- `templates/monitoring/threshold-dashboard.html`
- `templates/monitoring/server-details.html`
- `templates/monitoring/user-activity.html`

### 终端页面：
- `templates/terminal/index.html`
- `templates/terminal/manager.html`

### 测试和调试页面：
- `templates/debug/user-group-sync-console.html`
- `templates/test/server-status-tags.html`

## 验证部署

1. **样式检查**：确保页面样式正常显示
2. **功能测试**：测试JavaScript功能是否正常
3. **网络监控**：检查浏览器控制台是否有404错误
4. **离线测试**：断开外网连接测试功能

## 部署建议

### 生产环境：
1. 使用方案一手动下载所有依赖
2. 修改所有HTML文件中的CDN链接
3. 压缩静态资源以减少传输大小
4. 配置Web服务器缓存策略

### 开发环境：
1. 可以保留CDN链接用于快速开发
2. 通过Profile配置切换本地/CDN资源
3. 使用构建工具自动化资源管理

## 自动化脚本

项目提供了以下脚本帮助自动化：
- `scripts/download-vendor-libs.sh` (Linux/Mac)
- `scripts/download-vendor-libs.bat` (Windows)

运行脚本后，还需要手动修改HTML文件中的链接。

## 注意事项

1. **版本一致性**：确保下载的库版本与HTML中引用的版本一致
2. **完整性检查**：验证下载的文件完整性
3. **更新维护**：定期更新依赖库的版本
4. **安全考虑**：定期检查依赖库的安全漏洞

## 文件大小参考

预计本地化后增加的文件大小：
- Bootstrap: ~200KB
- Chart.js: ~300KB  
- XTerm.js: ~400KB
- Prism.js: ~100KB
- Flatpickr: ~50KB
- WebSocket库: ~50KB
- 总计: ~1.1MB

对于内网部署来说，这个大小是可以接受的。