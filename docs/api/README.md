# API测试文档

## 📋 Postman测试集

### SSH Config Import API

**文件**: `SSH-Config-Import-API.postman_collection.json`

#### 快速开始

1. **导入Postman Collection**
   - 打开Postman
   - 点击 `Import` → 选择文件 `SSH-Config-Import-API.postman_collection.json`
   - Collection将自动导入

2. **配置环境变量**
   ```
   baseUrl = http://localhost:9090
   ```

3. **配置认证**
   - Collection已包含Basic Auth认证
   - 默认用户名: `admin`
   - 默认密码: `admin123`
   - 如需修改，编辑Collection的Authorization设置

#### 测试用例清单

| # | 测试名称 | HTTP方法 | 端点 | 描述 |
|---|---------|---------|------|------|
| 1 | 上传SSH配置文件 | POST | `/api/ssh-config-import/upload` | 上传并解析SSH配置文件 |
| 2 | 解析本地配置（默认路径） | POST | `/api/ssh-config-import/parse-local` | 解析~/.ssh/config |
| 3 | 解析本地配置（自定义路径） | POST | `/api/ssh-config-import/parse-local?path=...` | 解析指定路径配置 |
| 4 | 预览导入（去重检查） | POST | `/api/ssh-config-import/preview` | 预览并检查重复 |
| 5 | 批量导入服务器 | POST | `/api/ssh-config-import/batch` | 批量导入服务器 |
| 6 | 获取默认配置路径 | GET | `/api/ssh-config-import/default-path` | 获取路径信息 |

#### 边界测试用例

| 测试名称 | 测试场景 | 预期结果 |
|---------|---------|---------|
| 空文件上传 | 上传空文件 | 400 Bad Request |
| 空服务器列表导入 | 导入空列表 | 400 Bad Request |
| 缺少必填字段 | 导入不完整数据 | 验证失败，返回错误详情 |

#### 自动化测试脚本

每个测试都包含Postman Test Scripts：
- ✅ 响应状态码验证
- ✅ 响应体结构验证
- ✅ 业务逻辑验证
- ✅ 数据传递（用于测试链）

#### 测试数据准备

**测试用SSH配置文件示例** (`test-ssh-config`):
```
Host test-server-1
    HostName 192.168.1.100
    User root
    Port 22
    IdentityFile ~/.ssh/id_rsa

Host test-server-2
    HostName 192.168.1.101
    User admin
    Port 22
```

#### 执行测试

**方式1: 手动测试**
1. 选择要测试的请求
2. 点击 `Send`
3. 查看响应和Test Results

**方式2: 批量运行**
1. 点击Collection右侧的 `...` → `Run collection`
2. 选择要运行的测试
3. 点击 `Run SSH Config Import API`
4. 查看测试报告

**方式3: Newman命令行**
```bash
# 安装Newman
npm install -g newman

# 运行测试
newman run SSH-Config-Import-API.postman_collection.json

# 生成HTML报告
newman run SSH-Config-Import-API.postman_collection.json -r html
```

#### 常见问题

**Q: 认证失败 401 Unauthorized**
- A: 检查用户名密码是否正确，确保用户有ADMIN或SUPER_ADMIN角色

**Q: 文件上传失败**
- A: 确保文件路径正确，文件大小不超过1MB

**Q: 服务器导入重复**
- A: 检查数据库中是否已存在相同hostname+sshPort的服务器

#### 测试覆盖率

- ✅ 功能测试：6个核心API
- ✅ 边界测试：3个边界场景
- ✅ 异常测试：空值、缺失字段、格式错误
- ✅ 安全测试：权限验证、认证检查

#### 测试结果示例

**成功响应 - 上传文件**:
```json
{
  "totalHosts": 2,
  "servers": [
    {
      "name": "test-server-1",
      "hostname": "192.168.1.100",
      "sshPort": 22,
      "sshUsername": "root",
      "sshKeyPath": "~/.ssh/id_rsa",
      "valid": true,
      "duplicate": false
    }
  ],
  "warnings": []
}
```

**成功响应 - 批量导入**:
```json
{
  "successCount": 2,
  "failedCount": 0,
  "successServers": [
    {
      "id": 1,
      "name": "test-server-1",
      "hostname": "192.168.1.100"
    },
    {
      "id": 2,
      "name": "test-server-2",
      "hostname": "192.168.1.101"
    }
  ],
  "failures": []
}
```

**错误响应示例**:
```json
{
  "error": true,
  "message": "文件大小超过限制，最大允许 1 MB",
  "timestamp": 1697654400000
}
```

---

**最后更新**: 2025-10-18
**维护者**: 开发团队
