/**
 * 导航集成类型定义
 * 用于React应用与主应用导航系统的集成
 */

/**
 * 布局模式类型
 * - shell: 完整布局（包含侧边栏） - 用于独立模式
 * - content-only: 仅内容布局（无侧边栏） - 用于嵌入模式
 * - minimal: 最小化布局（可选，用于特殊场景如打印预览）
 */
export type LayoutMode = 'shell' | 'content-only' | 'minimal';

/**
 * 布局上下文值接口
 * 提供布局模式状态和控制方法
 */
export interface LayoutContextValue {
  /** 当前激活的布局模式 */
  mode: LayoutMode;
  /** 切换布局模式的方法 */
  setMode: (mode: LayoutMode) => void;
  /** 标识应用是否在主应用中嵌入运行 */
  isEmbedded: boolean;
}

/**
 * 导航事件详情数据结构
 * 用于主应用和React Router之间的双向通信
 */
export interface NavigationEventDetail {
  /** 目标路由路径，必须以 /deploy-platform/ 开头 */
  route: string;
  /** 事件源标识，用于防止循环触发 */
  source: 'main-app' | 'react';
  /** 可选：事件时间戳，用于调试和监控 */
  timestamp?: number;
}

/**
 * 扩展Window接口以支持部署平台特定属性
 */
declare global {
  interface Window {
    /** 部署平台嵌入模式标志 */
    __DEPLOY_PLATFORM_EMBEDDED__?: boolean;
    /** 部署平台调试模式标志 */
    __DEPLOY_PLATFORM_DEBUG__?: boolean;
  }
}

/**
 * 导航事件类型
 */
export type MainNavChangeEvent = CustomEvent<NavigationEventDetail>;
export type ReactNavChangeEvent = CustomEvent<NavigationEventDetail>;
