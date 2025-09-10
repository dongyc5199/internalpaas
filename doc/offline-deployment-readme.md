# Dev Debug Platform - 内网离线部署方案

## 问题概述

您提出的问题非常重要！当前项目确实依赖大量外部CDN资源，在内网环境部署时会导致：

- ❌ 页面样式无法加载（Bootstrap等CSS框架）
- ❌ JavaScript功能失效（Chart.js、XTerm.js等）
- ❌ 图标可能无法显示（Font Awesome已本地化）
- ❌ 图表和数据可视化无法工作
- ❌ 终端功能无法使用

## 解决方案

我们已经为您准备了完整的内网离线部署解决方案：

### 🚀 快速开始

#### 1. 检查当前状态
```bash
./test-offline-mode.sh
```

#### 2. 下载依赖资源（需要外网环境）
```bash
# Linux/Mac
./scripts/download-vendor-libs.sh

# Windows
scripts\download-vendor-libs.bat
```

#### 3. 离线部署
```bash
# Linux/Mac
./deploy-offline.sh

# Windows
deploy-offline.bat
```

#### 4. 启动应用
```bash
# Linux/Mac
./start-offline.sh

# Windows
start-offline.bat
```

### 📁 新增文件结构

```
├── src/main/resources/static/vendor/          # 本地化资源目录
│   ├── bootstrap/                            # Bootstrap框架
│   ├── chartjs/                              # Chart.js图表库
│   ├── xterm/                                # XTerm.js终端
│   ├── prism/                                # Prism.js代码高亮
│   ├── flatpickr/                            # Flatpickr日期选择器
│   ├── sockjs/                               # WebSocket库
│   └── hammerjs/                             # 触摸手势库
├── src/main/resources/application-offline.properties  # 离线配置
├── src/main/java/.../config/OfflineDeploymentConfig.java  # 离线配置类
├── src/main/java/.../service/OfflineDeploymentService.java # 离线服务
├── scripts/download-vendor-libs.sh           # 资源下载脚本(Linux)
├── scripts/download-vendor-libs.bat          # 资源下载脚本(Windows)
├── deploy-offline.sh                         # 离线部署脚本(Linux)
├── deploy-offline.bat                        # 离线部署脚本(Windows)
├── test-offline-mode.sh                      # 测试脚本
├── OFFLINE_DEPLOYMENT_GUIDE.md               # 详细部署指南
└── README_OFFLINE_DEPLOYMENT.md              # 本文件
```

## 🎯 核心特性

### 1. 自动CDN检测
- 应用启动时自动检测CDN可用性
- 根据网络环境自动切换本地/远程资源

### 2. 多环境支持
```bash
# 开发环境（默认，使用CDN）
mvn spring-boot:run -Dspring.profiles.active=dev

# 生产环境（在线）
mvn spring-boot:run -Dspring.profiles.active=prod

# 内网环境（离线）
mvn spring-boot:run -Dspring.profiles.active=offline
```

### 3. Maven Profile配置
```xml
<!-- 内网部署Profile -->
<profile>
    <id>offline</id>
    <properties>
        <spring.profiles.active>offline</spring.profiles.active>
        <offline.mode>true</offline.mode>
    </properties>
</profile>
```

### 4. 智能资源管理
- 离线模式下自动使用本地资源
- 资源缓存优化（30天缓存期）
- 资源压缩传输

## 📋 部署清单

### 准备工作（需要外网环境）
- [ ] 运行 `./scripts/download-vendor-libs.sh` 下载所有依赖
- [ ] 验证所有资源文件完整性

### 内网部署
- [ ] 将整个项目文件夹复制到内网服务器
- [ ] 确保Java 11+和Maven环境
- [ ] 运行 `./deploy-offline.sh` 进行部署
- [ ] 使用 `./start-offline.sh` 启动应用

### 验证步骤
- [ ] 访问 `http://localhost:8080` 检查页面样式
- [ ] 测试JavaScript功能是否正常
- [ ] 检查浏览器控制台是否有404错误
- [ ] 测试图表、终端等高级功能

## 🔧 配置说明

### 离线模式配置
```properties
# application-offline.properties
app.offline.enabled=true
app.offline.cdn-available=false
app.offline.vendor-path=/static/vendor/
app.offline.connection-timeout=3000
```

### 资源映射
系统会自动将CDN链接映射为本地路径：
```
https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/css/bootstrap.min.css
↓ 映射为
/vendor/bootstrap/bootstrap.min.css
```

## 📊 资源文件统计

| 库名称 | 文件大小 | 用途 |
|--------|----------|------|
| Bootstrap | ~200KB | UI框架 |
| Chart.js | ~300KB | 图表组件 |
| XTerm.js | ~400KB | 终端功能 |
| Prism.js | ~100KB | 代码高亮 |
| Flatpickr | ~50KB | 日期选择 |
| WebSocket库 | ~50KB | 实时通信 |
| **总计** | **~1.1MB** | |

## 🚨 注意事项

### 1. 依赖下载
- 必须在有外网的环境下先下载所有依赖
- 确保下载的文件完整且版本匹配

### 2. 部署环境
- 确保内网服务器有Java 11+环境
- 检查防火墙设置，开放8080端口
- 确保足够的磁盘空间（建议2GB+）

### 3. 版本维护
- 定期更新依赖库版本
- 关注安全漏洞修复
- 保持本地资源与HTML引用版本一致

### 4. 性能优化
- 内网环境建议配置反向代理（Nginx）
- 启用资源压缩和缓存
- 考虑使用CDN加速（如有内网CDN）

## 🔍 故障排除

### 样式加载问题
1. 检查vendor目录下文件是否完整
2. 验证文件大小是否正确
3. 检查浏览器控制台网络错误

### JavaScript功能失效
1. 确认所有JS文件已下载
2. 检查版本兼容性
3. 查看控制台错误信息

### 终端功能异常
1. 确认XTerm.js相关文件完整
2. 检查WebSocket连接状态
3. 验证SSH配置正确性

## 📞 技术支持

如遇到部署问题，请检查：
1. 运行 `./test-offline-mode.sh` 诊断环境
2. 查看应用日志文件
3. 检查系统资源使用情况
4. 确认网络连接和防火墙设置

## 🎉 部署成功标志

- ✅ 页面样式完整加载
- ✅ 导航和交互功能正常
- ✅ 图表和数据可视化显示正常
- ✅ 终端功能可正常使用
- ✅ 浏览器控制台无404错误
- ✅ 应用功能与在线版本一致

---

**总结：**通过本方案，您的Dev Debug Platform可以完美支持内网环境部署，所有外部CDN依赖都已本地化处理，确保在完全离线的环境下也能正常运行所有功能。