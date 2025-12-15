# CodeHub Frontend

CodeHub的React前端应用

## 技术栈

- **React 18** - UI框架
- **TypeScript** - 类型安全
- **Vite** - 构建工具
- **React Router** - 路由管理
- **TanStack Query** - 数据获取与缓存
- **Zustand** - 状态管理
- **Tailwind CSS** - 样式框架
- **Axios** - HTTP客户端
- **Lucide React** - 图标库

## 开发

```bash
# 安装依赖
npm install

# 启动开发服务器
npm run dev

# 构建生产版本
npm run build

# 预览生产构建
npm run preview
```

## 项目结构

```
src/
├── components/      # 可复用组件
├── pages/          # 页面组件
├── services/       # API服务
├── stores/         # Zustand状态管理
├── hooks/          # 自定义React Hooks
├── types/          # TypeScript类型定义
├── utils/          # 工具函数
└── contexts/       # React Context
```

## 功能特性

- ✅ 用户认证（登录/注册）
- ✅ 项目管理
- ✅ 仓库管理
- ✅ 构建记录查看
- ✅ 实时构建日志（WebSocket）
- ✅ 质量报告展示
- ✅ 响应式设计

## API配置

开发服务器会自动代理API请求到后端：

- `/api` -> `http://localhost:8880/api`
- `/ws` -> `ws://localhost:8880/ws`

生产环境构建输出到 `../internal/static`，由Go后端静态文件服务器提供。
