# Contract: BinaryValidator

**Purpose**: 定义Agent二进制文件验证的行为规范

**实现类**: `AgentDeployService` (私有方法)

---

## 方法签名

```java
/**
 * 验证二进制文件是否为有效的tar.gz格式
 *
 * @param data 文件字节数组
 * @return true表示验证通过，false表示无效
 */
private boolean validateBinary(byte[] data)
```

---

## 行为契约

### 1. 最小文件大小检查

**规则**: 文件必须至少1MB

**场景**:
```
Given: 字节数组长度 < 1,048,576 (1 MB)
When: 调用 validateBinary(data)
Then:
  - 返回 false
  - 记录 ERROR 日志 "二进制文件过小 ({size} bytes)，可能不是有效的tar.gz文件"
```

**理由**:
- OpenTelemetry Collector典型大小为20-50MB
- HTML错误页（404/500）通常<1MB
- 过滤掉明显错误的响应

### 2. Gzip魔数检查

**规则**: 文件前两个字节必须为 `0x1f 0x8b`

**场景**:
```
Given: data[0] != 0x1f 或 data[1] != 0x8b
When: 调用 validateBinary(data)
Then:
  - 返回 false
  - 记录 ERROR 日志 "文件签名不匹配，不是有效的gzip文件"
```

**技术细节**:
```java
if (data.length < 2 || data[0] != 0x1f || (data[1] & 0xff) != 0x8b) {
    return false;
}
```

**理由**:
- Gzip文件格式规范（RFC 1952）定义固定魔数
- 快速识别文件类型，无需解压
- 防止误将其他格式文件当作tar.gz

### 3. 最大文件大小检查

**规则**: 文件不得超过100MB

**场景**:
```
Given: 字节数组长度 > 104,857,600 (100 MB)
When: 调用 validateBinary(data)
Then:
  - 返回 false
  - 记录 ERROR 日志 "文件过大 ({size} MB)，超过100MB限制"
```

**理由**:
- 防止内存溢出（JVM堆保护）
- 异常大文件可能是配置错误
- OpenTelemetry Collector不应超过此大小

---

## 验证流程图

```
┌─────────────────┐
│ validateBinary  │
└────────┬────────┘
         │
    ┌────▼─────────────────┐
    │ 长度 >= 2 bytes?     │
    └─No─┬──────────Yes────┘
         │                 │
      返回false      ┌─────▼──────────────────┐
                     │ 长度 >= 1 MB?          │
                     └─No─┬────────Yes────────┘
                          │                   │
                       返回false       ┌──────▼──────────────────┐
                                       │ 魔数 == 0x1f 0x8b?      │
                                       └─No─┬────────Yes─────────┘
                                            │                    │
                                         返回false        ┌──────▼──────────────────┐
                                                          │ 长度 <= 100 MB?         │
                                                          └─No─┬────────Yes─────────┘
                                                               │                    │
                                                            返回false           返回true
```

---

## 错误场景与消息

| 场景 | 条件 | 错误日志 | 用户消息 |
|-----|------|---------|---------|
| 文件过小 | < 1 MB | `二进制文件过小 ({size} bytes)，可能不是有效的tar.gz文件` | 下载的文件无效（可能是错误页或损坏文件） |
| 魔数不匹配 | 不是0x1f 0x8b | `文件签名不匹配，不是有效的gzip文件` | 下载的文件无效（不是gzip格式） |
| 文件过大 | > 100 MB | `文件过大 ({size} MB)，超过100MB限制` | 文件过大，超过系统限制 |
| 数据为null | data == null | N/A（调用方检查） | 未能获取Agent二进制文件 |

---

## 测试用例要求

### 单元测试（必需）

1. **test_validate_whenValidGzipFile_returnsTrue**
   - 输入：40MB的有效tar.gz文件
   - 验证：返回true，无错误日志

2. **test_validate_whenFileTooSmall_returnsFalse**
   - 输入：500KB的文件
   - 验证：返回false，日志包含"过小"

3. **test_validate_whenWrongMagicNumber_returnsFalse**
   - 输入：以`0x50 0x4b`（ZIP魔数）开头的文件
   - 验证：返回false，日志包含"签名不匹配"

4. **test_validate_whenFileTooLarge_returnsFalse**
   - 输入：150MB的文件
   - 验证：返回false，日志包含"过大"

5. **test_validate_whenHtmlErrorPage_returnsFalse**
   - 输入：10KB的HTML内容（模拟404错误页）
   - 验证：返回false（因为过小且魔数不匹配）

6. **test_validate_whenEmptyArray_returnsFalse**
   - 输入：长度为0的字节数组
   - 验证：返回false

7. **test_validate_whenPartialDownload_returnsFalse**
   - 输入：只有1个字节的数组
   - 验证：返回false（长度检查在魔数检查之前）

### 边界测试

8. **test_validate_whenExactly1MB_returnsTrue**
   - 输入：恰好1,048,576字节，有效魔数
   - 验证：返回true

9. **test_validate_whenExactly100MB_returnsTrue**
   - 输入：恰好104,857,600字节，有效魔数
   - 验证：返回true

10. **test_validate_when1MBMinus1Byte_returnsFalse**
    - 输入：1,048,575字节
    - 验证：返回false

---

## 性能要求

- **验证时间**: < 1ms（仅检查前两个字节和长度）
- **内存占用**: 0（不复制数组，只读操作）
- **CPU占用**: 极低（无解压或复杂计算）

---

## 已知限制

### 不验证的内容

1. **Tar归档结构**: 不检查tar头部格式
2. **文件完整性**: 不计算CRC32或其他校验和
3. **压缩内容**: 不解压验证内部文件
4. **文件名**: 不验证归档内包含正确的二进制文件名

**理由**: 这些深度验证由安装脚本的 `tar -xzf` 命令自然完成。如果文件损坏，tar命令会失败，部署流程会正确中止。

### 可能的误判场景

1. **非tar.gz的gzip文件**: 会通过验证（因为魔数正确）
   - 影响：安装脚本会拒绝，最终部署失败
   - 可接受：错误会被后续步骤捕获

2. **恶意构造的文件**: 前两个字节伪造为0x1f 0x8b
   - 影响：通过验证但安装失败
   - 可接受：依赖HTTPS保证文件来源可信

---

## 依赖关系

### 调用方

- `AgentDeployService.resolveAgentBinary()` - 下载后立即验证
- `AgentDeployService.loadBinaryFromResource()` - 资源加载后验证（可选）

### 被调用方

- 无（纯函数，无外部依赖）

---

## 实现示例

```java
private boolean validateBinary(byte[] data) {
    // 检查null和最小长度
    if (data == null || data.length < 2) {
        logger.error("二进制数据为空或过短");
        return false;
    }

    // 1. 最小文件大小检查
    if (data.length < 1_048_576) { // 1 MB
        logger.error("二进制文件过小 ({} bytes)，可能不是有效的tar.gz文件", data.length);
        return false;
    }

    // 2. Gzip魔数检查
    if (data[0] != 0x1f || (data[1] & 0xff) != 0x8b) {
        logger.error("文件签名不匹配，不是有效的gzip文件 (魔数: 0x{} 0x{})",
            Integer.toHexString(data[0] & 0xff),
            Integer.toHexString(data[1] & 0xff));
        return false;
    }

    // 3. 最大文件大小检查
    if (data.length > 104_857_600) { // 100 MB
        double sizeMB = data.length / 1_048_576.0;
        logger.error("文件过大 ({} MB)，超过100MB限制", String.format("%.2f", sizeMB));
        return false;
    }

    logger.debug("二进制文件验证通过 - 大小: {} MB, 魔数正确",
        String.format("%.2f", data.length / 1_048_576.0));
    return true;
}
```

---

## 实现检查清单

- [ ] 实现三层验证逻辑（大小下限 → 魔数 → 大小上限）
- [ ] 添加详细的错误日志（包含实际值）
- [ ] 处理null和空数组边界情况
- [ ] 实现所有10个单元测试用例
- [ ] 确保验证时间 < 1ms
- [ ] 代码覆盖率达到100%（方法简单，应全覆盖）
