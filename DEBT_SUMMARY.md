# 技术债快速索引

> 📋 快速查看当前技术债务状态 | [完整文档](./TECHNICAL_DEBT.md)

## 当前状态

| 优先级 | 数量 | 状态 |
|--------|------|------|
| 🔴 P0 | 0 | ✅ 无关键债务 |
| 🟠 P1 | 2 | ⚠️ 需关注 |
| 🟡 P2 | 2 | 📝 待规划 |
| 🟢 P3 | 2 | 💡 优化项 |

**预计总工作量**: 11.5-20 小时

---

## 🟠 P1 - 需要关注的债务

### 1. WebSocket 测试边缘场景失败 (7个测试)
- **影响**: 边缘场景测试覆盖不完整
- **工作量**: 4-6 小时
- **文件**: `src/main/frontend/tests/websocket.test.ts`

### 2. Dashboard 自动刷新测试失败 (1个测试)
- **影响**: 自动刷新功能测试无效
- **工作量**: 1-2 小时
- **文件**: `src/main/frontend/tests/dashboard.test.ts`

---

## 测试状态概览

```
总测试数: 303
✅ 通过: 296 (97.7%)
❌ 失败: 7 (2.3%)
```

**测试模块覆盖**:
- ✅ Chart 工具 (27个)
- ✅ Storage (37个)
- ✅ HTTP (28个)
- ✅ Event Bus (23个)
- ✅ Format (39个)
- ✅ i18n (19个)
- ✅ ServerListManager (29个)
- ✅ ServerDetailOverlay (36个)
- ⚠️ WebSocketManager (22/29通过)
- ⚠️ Dashboard (8/9通过)

---

## 快速操作

```bash
# 查看所有测试状态
npm run test

# 运行测试并生成覆盖率报告
npm run test:run

# 查看完整技术债文档
cat TECHNICAL_DEBT.md

# 在浏览器中打开
start TECHNICAL_DEBT.md  # Windows
open TECHNICAL_DEBT.md   # macOS
xdg-open TECHNICAL_DEBT.md  # Linux
```

---

**最后更新**: 2025-10-17  
**下次审查**: 2025-10-24

📚 [查看完整技术债文档](./TECHNICAL_DEBT.md) | 📝 [项目 README](./README.md)
