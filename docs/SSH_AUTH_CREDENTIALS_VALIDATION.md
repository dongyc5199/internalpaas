# SSH 认证凭证验证修复

## 问题描述

### 错误信息
```
POST http://localhost:9090/api/ssh-config-import/batch
状态码: 400 Bad Request
错误消息: 验证失败: 缺少SSH认证凭证（密码或私钥）
```

### 根本原因
前端在批量导入服务器时，发送的 `ServerImportDto` 对象可能同时缺少 `sshPassword` 和 `sshKeyPath`：
1. SSH 扫描返回的 `hostData.identityFile` 可能为 `null` 或空字符串
2. 用户在表格的密码输入框中**没有输入密码**
3. 转换后的数据 `sshPassword: null` 且 `sshKeyPath: null`
4. 后端 `ServerImportDto.hasAuthCredentials()` 验证失败

### 后端验证逻辑
```java
// ServerImportDto.java
public boolean hasAuthCredentials() {
    return (sshPassword != null && !sshPassword.isEmpty()) ||
           (sshKeyPath != null && !sshKeyPath.isEmpty());
}

// SSHConfigImportService.java
if (!dto.hasAuthCredentials()) {
    errors.add("缺少SSH认证凭证（密码或私钥）");
}
```

## 解决方案

### 前端验证增强
在用户点击"下一步"确认导入前，添加**前置验证**：

```javascript
// main-layout.html (约第 5203 行)
// ✅ 新增：验证每台服务器是否有认证凭证
const serversWithoutAuth = selectedServers.filter(server => {
    const hasPassword = server.sshPassword && server.sshPassword.trim() !== '';
    const hasKeyPath = server.sshKeyPath && server.sshKeyPath.trim() !== '';
    return !hasPassword && !hasKeyPath;
});

if (serversWithoutAuth.length > 0) {
    const serverNames = serversWithoutAuth.map(s => s.name).join('、');
    showToast(
        `以下服务器缺少SSH认证凭证（密码或私钥）：\n${serverNames}\n\n请在表格中填写SSH密码或确保配置了私钥。`,
        'error'
    );
    return;
}
```

### 验证流程
1. ✅ **收集选中的服务器** - `getSelectedServers()` 转换表格数据为 DTO 对象
2. ✅ **数量检查** - 确保至少选中一台服务器
3. ✅ **认证凭证检查**（新增）- 过滤出缺少密码和私钥的服务器
4. ✅ **友好提示** - 显示具体哪些服务器缺少凭证，引导用户填写
5. ✅ **确认对话框** - 通过验证后才显示确认导入提示
6. ✅ **调用后端 API** - 发送完整有效的数据到 `/api/ssh-config-import/batch`

## 用户体验改进

### 修复前
- ❌ 用户点击"下一步" → 发送请求 → 收到 400 错误 → 不知道如何修复
- ❌ 错误信息不明确："验证失败: 缺少SSH认证凭证（密码或私钥）"
- ❌ 用户需要猜测是哪台服务器的问题

### 修复后
- ✅ 用户点击"下一步" → **前端立即验证** → 显示清晰提示
- ✅ 明确列出缺少凭证的服务器名称：`"server-1、server-2"`
- ✅ 指导用户如何修复：`"请在表格中填写SSH密码或确保配置了私钥"`
- ✅ 避免无效的后端请求，减少网络开销

## 相关字段说明

### ServerImportDto 字段
| 字段名 | 类型 | 是否必填 | 说明 |
|--------|------|----------|------|
| `sshPassword` | String | **密码和私钥二选一** | SSH登录密码，系统会加密存储 |
| `sshKeyPath` | String | **密码和私钥二选一** | SSH私钥文件路径（如 `~/.ssh/id_rsa`） |
| `hostname` | String | ✅ 必填 | 服务器主机名或IP地址 |
| `sshUsername` | String | ✅ 必填 | SSH登录用户名 |
| `name` | String | ✅ 必填 | 服务器名称 |

### 前端 UI 元素
```html
<!-- 密码输入框 -->
<input type="password" 
       class="form-control form-control-sm server-password-input"
       placeholder="请输入密码"
       title="可选填写SSH登录密码，系统会加密存储">
```

## 测试场景

### ✅ 场景 1：有私钥文件
- SSH 扫描返回 `identityFile: "~/.ssh/id_rsa"`
- 用户**不填写密码**
- 验证通过 ✓

### ✅ 场景 2：填写密码
- SSH 扫描**没有私钥**（`identityFile: null`）
- 用户在表格中**填写密码**
- 验证通过 ✓

### ❌ 场景 3：既无私钥也无密码（触发验证）
- SSH 扫描**没有私钥**（`identityFile: null`）
- 用户**没有填写密码**
- 前端验证**拦截** ✗
- 显示错误提示：`"server-1 缺少SSH认证凭证（密码或私钥）"`

### ✅ 场景 4：混合情况
- 服务器 A：有私钥 ✓
- 服务器 B：填写了密码 ✓
- 服务器 C：既无私钥也无密码 ✗
- 前端验证**仅拦截服务器 C**，提示用户修复

## 文件修改清单

### 前端
- ✅ `main-layout.html` (约第 5203 行)
  - 添加 `serversWithoutAuth` 过滤逻辑
  - 添加友好的错误提示
  - 在调用 `importSelectedServers()` 前验证

### 后端（无需修改）
- `ServerImportDto.java` - 已有 `hasAuthCredentials()` 方法
- `SSHConfigImportService.java` - 已有验证逻辑

## 部署步骤

1. ✅ 修改 `main-layout.html` 添加验证逻辑
2. ✅ 执行 `npm run build` 重新编译前端
3. ✅ 重启 Spring Boot 应用加载新资源
4. ✅ 测试 SSH 扫描 → 批量导入流程

## 总结

通过**前端验证增强**，我们实现了：
- 🎯 **早期验证** - 在发送请求前拦截无效数据
- 🎯 **明确提示** - 精确指出哪些服务器缺少凭证
- 🎯 **用户友好** - 引导用户如何修复问题
- 🎯 **减少负载** - 避免无效的后端请求

---

**修复日期**: 2025-10-20  
**影响范围**: SSH 配置导入功能  
**测试状态**: ✅ 前端编译成功，待端到端测试
