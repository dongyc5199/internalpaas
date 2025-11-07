# Dev Debug Platform - React Frontend

React 18 + TypeScript 5 + Vite 5 前端应用

## 📋 前置要求

- **Node.js**: v18.x 或更高版本
- **npm**: v9.x 或更高版本  
- **Java**: JDK 17+ (运行后端)

## 🚀 快速开始

### 开发模式

```bash
# 安装依赖
npm install

# 启动开发服务器 (端口 5173)
npm run dev
```

访问 http://localhost:5173 查看应用

### 生产构建

```bash
# 构建前端
npm run build

# 或使用Maven构建
cd ../../..
./mvnw.cmd clean package
```

## 🛠️ 可用脚本

- `npm run dev` - 启动开发服务器
- `npm run build` - 生产构建
- `npm run lint` - 代码检查
- `npm run lint:fix` - 自动修复代码问题
- `npm run format` - 格式化代码
- `npm run test` - 运行测试
- `npm run test:coverage` - 测试覆盖率报告
- `npm run type-check` - TypeScript类型检查

## 🏗️ 架构设计

### 从现有 react-app 继承的模式

本项目复用了 `src/main/frontend/react-app` (部署平台模块) 的经过验证的架构模式:

#### 1. Session-based 认证 ✅
```typescript
// src/shared/api/fetcher.ts
const defaultFetcher: Fetcher = async <T>(input, init) => {
  const response = await fetch(input, {
    credentials: 'same-origin', // 自动发送 JSESSIONID cookie
    headers: { 'Content-Type': 'application/json', ...init?.headers },
  });
  // ...
};
```

#### 2. React Query 配置 ✅
```typescript
// src/shared/config/queryClient.ts
export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 5 * 60 * 1000,  // 5分钟数据保持新鲜
      gcTime: 10 * 60 * 1000,     // 缓存保留10分钟
      retry: 1,
      refetchOnWindowFocus: import.meta.env.PROD,
    },
  },
});
```

#### 3. 导航同步机制 ✅
```typescript
// src/shared/hooks/useNavSync.ts
export function useNavSync(navigate, location) {
  // 双向同步:
  // 1. 主应用 main-nav-change → React Router
  // 2. React Router → 主应用 react-nav-change
  // 防循环 + 智能去重
}
```

#### 4. 嵌入模式检测 ✅
```typescript
// src/shared/hooks/useEmbedMode.ts
export function useEmbedMode(): boolean {
  // 多层验证:
  // - Spring Boot上下文标志
  // - 容器data-embedded属性
  // - Window全局标记
  return detectEmbedMode();
}
```

详细架构分析请参考: [ARCHITECTURE_REFERENCE.md](../../specs/007-frontend-react-migration/ARCHITECTURE_REFERENCE.md)

### 项目结构

```
src/
├── features/               # 功能模块 (按业务领域组织)
│   ├── admin/             # 管理员功能
│   ├── monitoring/        # 监控功能
│   ├── terminal/          # 终端功能
│   ├── profile/           # 用户档案
│   ├── applications/      # 应用管理
│   └── config/            # 配置管理
├── shared/                # 共享资源
│   ├── api/              # API客户端 ✅
│   │   ├── client.ts     # API客户端实例
│   │   └── fetcher.ts    # HTTP抽象层
│   ├── components/       # 通用组件 ✅
│   │   ├── Button/       # 按钮组件
│   │   ├── Input/        # 输入框组件
│   │   ├── Modal/        # 模态框组件
│   │   ├── Table/        # 表格组件
│   │   └── index.ts      # 统一导出
│   ├── config/           # 配置 ✅
│   │   └── queryClient.ts # React Query配置
│   ├── hooks/            # 自定义Hooks ✅
│   │   ├── useNavSync.ts     # 导航同步
│   │   ├── useEmbedMode.ts   # 嵌入模式检测
│   │   └── index.ts
│   ├── stores/           # 状态管理 ✅
│   │   ├── authStore.ts  # 认证状态
│   │   └── index.ts
│   ├── types/            # TypeScript类型 ✅
│   │   ├── user.ts       # 用户类型
│   │   ├── server.ts     # 服务器类型
│   │   ├── application.ts # 应用类型
│   │   ├── navigation.ts # 导航类型
│   │   ├── auth.ts       # 认证类型
│   │   ├── common.ts     # 通用类型
│   │   └── index.ts
│   ├── utils/            # 工具函数
│   └── constants.ts      # 全局常量
├── App.tsx               # 根组件 ✅
├── main.tsx              # 应用入口
└── vite-env.d.ts         # Vite环境类型 ✅
```

## 📦 技术栈

### 核心框架
- **React**: 18.3.1
- **TypeScript**: 5.9.3
- **Vite**: 5.4.21

### 状态与数据管理
- **React Router**: 6.x - 路由管理
- **Zustand**: 4.x - 轻量级状态管理
- **TanStack Query**: 5.x - 服务端状态管理

### 表单与验证
- **React Hook Form**: 7.x - 表单管理
- **Zod**: 3.x - 数据验证

### 构建与集成
- **Maven**: frontend-maven-plugin - 自动化构建集成

## 🔧 开发规范

### TypeScript

- ✅ 启用严格模式 (`strict: true`)
- ✅ 禁止 `any` 类型 (ESLint规则)
- ✅ 要求显式函数返回类型
- ✅ 使用路径别名: `@/*`, `@shared/*`

### 测试

- 框架: Vitest + React Testing Library
- 覆盖率要求: 70% (lines, functions, branches, statements)

### 代码质量

```bash
# 代码检查
npm run lint

# 自动修复
npm run lint:fix

# 代码格式化
npm run format
```

## 📝 开发进度

查看完整任务列表: [tasks.md](../../specs/007-frontend-react-migration/tasks.md)

### ✅ 已完成 (Phase 1 + Phase 2 部分)

**Phase 1 - 项目初始化** (100%)
- ✅ Vite + React 18 + TypeScript 5 项目搭建
- ✅ ESLint + Prettier 代码规范
- ✅ Vitest 测试框架
- ✅ Maven 构建集成
- ✅ 从 react-app 继承架构模式
- ✅ API 客户端 (Session-based)
- ✅ React Query 配置
- ✅ 导航同步机制
- ✅ 嵌入模式检测

**Phase 2 - 核心基础设施** (60%)
- ✅ React Router 6 集成
- ✅ Zustand 认证状态管理
- ✅ 基础组件库 (Button, Input, Modal, Table)
- ⏳ 布局组件 (MainLayout, Header, Navigation)
- ⏳ 认证页面 (Login, ProtectedRoute)
- ⏳ i18n 国际化

### 🎯 下一步任务 (Phase 2 剩余)

1. **T011** - 实现 MainLayout 组件
2. **T012** - 创建 Navigation 组件
3. **T013** - 构建 Header 组件
4. **T018** - 实现认证 API 客户端
5. **T019** - 构建 ProtectedRoute 包装器
6. **T020** - 创建 Login 页面组件

### 📊 整体进度

- **总任务数**: 120 (8个阶段)
- **已完成**: 24 (~20%)
- **当前阶段**: Phase 2 - 核心基础设施
- **预计完成**: Phase 2 - Week 4

## 🤝 参考资源

- [架构参考文档](../../specs/007-frontend-react-migration/ARCHITECTURE_REFERENCE.md)
- [规范文档](../../specs/007-frontend-react-migration/spec.md)
- [实现计划](../../specs/007-frontend-react-migration/plan.md)

**分支**: `007-frontend-react-migration`
