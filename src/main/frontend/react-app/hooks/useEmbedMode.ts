import { useState, useEffect } from 'react';

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
  const [isEmbedded, setIsEmbedded] = useState(false);

  useEffect(() => {
    // 信号1: 检查容器元素（必要条件）
    const container = document.getElementById('deploy-platform-root');

    if (!container) {
      // 容器不存在，认为是独立模式
      setIsEmbedded(false);
      return;
    }

    // 信号2: 检查Spring Boot上下文标志（主信号）
    const hasSpringContext = container.getAttribute('data-spring-context') === 'true';

    // 信号3: 检查显式嵌入标记（增强信号）
    const hasEmbedFlag = container.getAttribute('data-embedded') === 'true';

    // 信号4: 检查Window全局标记（补充信号）
    const hasWindowFlag = window.__DEPLOY_PLATFORM_EMBEDDED__ === true;

    // 综合判断：任一信号为true即认为是嵌入模式
    const embedded = hasSpringContext || hasEmbedFlag || hasWindowFlag;

    setIsEmbedded(embedded);

    // 调试日志（生产环境通过window.__DEPLOY_PLATFORM_DEBUG__控制）
    if (window.__DEPLOY_PLATFORM_DEBUG__) {
      console.log('[useEmbedMode] Detection signals:', {
        hasSpringContext,
        hasEmbedFlag,
        hasWindowFlag,
        result: embedded
      });
    }
  }, []);

  return isEmbedded;
}
