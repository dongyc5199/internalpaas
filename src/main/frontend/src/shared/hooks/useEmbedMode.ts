import { useState, useEffect } from 'react';

type EmbedSignals = {
  hasSpringContext: boolean;
  hasEmbedFlag: boolean;
  hasWindowFlag: boolean;
};

const collectEmbedSignals = (): EmbedSignals => {
  if (typeof document === 'undefined') {
    return {
      hasSpringContext: false,
      hasEmbedFlag: false,
      hasWindowFlag:
        typeof window !== 'undefined' && window.__DEPLOY_PLATFORM_EMBEDDED__ === true,
    };
  }

  const container = document.getElementById('deploy-platform-root');

  if (!container) {
    return {
      hasSpringContext: false,
      hasEmbedFlag: false,
      hasWindowFlag:
        typeof window !== 'undefined' && window.__DEPLOY_PLATFORM_EMBEDDED__ === true,
    };
  }

  return {
    hasSpringContext: container.getAttribute('data-spring-context') === 'true',
    hasEmbedFlag: container.getAttribute('data-embedded') === 'true',
    hasWindowFlag: typeof window !== 'undefined' && window.__DEPLOY_PLATFORM_EMBEDDED__ === true,
  };
};

export const detectEmbedMode = (): boolean => {
  const signals = collectEmbedSignals();
  return signals.hasSpringContext || signals.hasEmbedFlag || signals.hasWindowFlag;
};

/**
 * 嵌入模式检测Hook
 *
 * 使用多层验证机制检测React应用是否运行在嵌入模式下：
 * 1. 信号1: 检查容器元素存在性（必要条件）
 * 2. 信号2: 检查Spring Boot上下文标志（主信号）
 * 3. 信号3: 检查显式嵌入标记（增强信号）
 * 4. 信号4: 检查Window全局标记（补充信号）
 *
 * @returns {boolean} true表示应用在嵌入模式下运行，false表示独立模式
 *
 * @example
 * ```tsx
 * function App() {
 *   const isEmbedded = useEmbedMode();
 *   return (
 *     <div>
 *       {isEmbedded ? '嵌入模式' : '独立模式'}
 *     </div>
 *   );
 * }
 * ```
 */
export function useEmbedMode(): boolean {
  const [isEmbedded, setIsEmbedded] = useState<boolean>(() => detectEmbedMode());

  useEffect(() => {
    const signals = collectEmbedSignals();
    const embedded = signals.hasSpringContext || signals.hasEmbedFlag || signals.hasWindowFlag;

    setIsEmbedded(embedded);

    // 调试日志（生产环境通过window.__DEPLOY_PLATFORM_DEBUG__控制）
    if (window.__DEPLOY_PLATFORM_DEBUG__) {
      console.log('[useEmbedMode] Detection signals:', {
        ...signals,
        result: embedded,
      });
    }
  }, []);

  return isEmbedded;
}
