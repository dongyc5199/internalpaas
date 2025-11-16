/**
 * useTypewriter Hook
 * 打字机流式输出效果
 *
 * 移植自: src/main/resources/static/js/ai-assistant.js
 * TypewriterRenderer类 (line 5-70)
 */

import { useRef, useCallback, useEffect } from 'react';

/**
 * 打字机配置
 */
export interface TypewriterConfig {
  /** 速度 (字符/秒) */
  speed?: number;
  /** 完成回调 */
  onComplete?: () => void;
}

/**
 * 打字机控制器
 */
export interface TypewriterController {
  /** 开始打字 */
  start: (fullText: string) => void;
  /** 暂停 */
  pause: () => void;
  /** 继续 */
  resume: () => void;
  /** 跳到结尾 */
  skipToEnd: () => void;
  /** 是否正在打字 */
  isTyping: boolean;
  /** 是否已完成 */
  isComplete: boolean;
  /** 当前显示的文本 */
  displayedText: string;
}

/**
 * useTypewriter Hook
 *
 * 使用示例:
 * ```tsx
 * const typewriter = useTypewriter({
 *   speed: 50,
 *   onComplete: () => console.log('Done!'),
 * });
 *
 * // 开始打字
 * typewriter.start('Hello World!');
 *
 * // 暂停
 * typewriter.pause();
 *
 * // 跳到结尾
 * typewriter.skipToEnd();
 * ```
 */
export function useTypewriter(config: TypewriterConfig = {}): TypewriterController {
  const { speed = 50, onComplete } = config;

  // 状态
  const textRef = useRef<string>('');
  const positionRef = useRef<number>(0);
  const isPausedRef = useRef<boolean>(false);
  const isCompleteRef = useRef<boolean>(false);
  const lastFrameTimeRef = useRef<number>(0);
  const animationFrameRef = useRef<number | null>(null);
  const displayedTextRef = useRef<string>('');

  // 回调引用
  const onCompleteRef = useRef(onComplete);
  useEffect(() => {
    onCompleteRef.current = onComplete;
  }, [onComplete]);

  /**
   * 渲染帧 (移植自ai-assistant.js line 25-50)
   */
  const renderFrame = useCallback((): void => {
    if (isPausedRef.current || isCompleteRef.current) {
      return;
    }

    const now = performance.now();
    const deltaTime = (now - lastFrameTimeRef.current) / 1000; // 转换为秒
    const charsToAdd = Math.floor(deltaTime * speed);

    if (charsToAdd > 0) {
      positionRef.current = Math.min(
        positionRef.current + charsToAdd,
        textRef.current.length
      );
      displayedTextRef.current = textRef.current.substring(0, positionRef.current);
      lastFrameTimeRef.current = now;
    }

    if (positionRef.current < textRef.current.length) {
      animationFrameRef.current = requestAnimationFrame(renderFrame);
    } else {
      isCompleteRef.current = true;
      if (onCompleteRef.current) {
        onCompleteRef.current();
      }
    }
  }, [speed]);

  /**
   * 开始打字 (移植自ai-assistant.js line 17-23)
   */
  const start = useCallback(
    (fullText: string): void => {
      textRef.current = fullText;
      positionRef.current = 0;
      isCompleteRef.current = false;
      isPausedRef.current = false;
      displayedTextRef.current = '';
      lastFrameTimeRef.current = performance.now();

      // 取消之前的动画
      if (animationFrameRef.current !== null) {
        cancelAnimationFrame(animationFrameRef.current);
      }

      renderFrame();
    },
    [renderFrame]
  );

  /**
   * 暂停 (移植自ai-assistant.js line 52-54)
   */
  const pause = useCallback((): void => {
    isPausedRef.current = true;
    if (animationFrameRef.current !== null) {
      cancelAnimationFrame(animationFrameRef.current);
      animationFrameRef.current = null;
    }
  }, []);

  /**
   * 继续 (移植自ai-assistant.js line 56-62)
   */
  const resume = useCallback((): void => {
    if (isPausedRef.current && !isCompleteRef.current) {
      isPausedRef.current = false;
      lastFrameTimeRef.current = performance.now();
      renderFrame();
    }
  }, [renderFrame]);

  /**
   * 跳到结尾 (移植自ai-assistant.js line 64-69)
   */
  const skipToEnd = useCallback((): void => {
    positionRef.current = textRef.current.length;
    displayedTextRef.current = textRef.current;
    isCompleteRef.current = true;

    if (animationFrameRef.current !== null) {
      cancelAnimationFrame(animationFrameRef.current);
      animationFrameRef.current = null;
    }

    if (onCompleteRef.current) {
      onCompleteRef.current();
    }
  }, []);

  // 清理
  useEffect(() => {
    return () => {
      if (animationFrameRef.current !== null) {
        cancelAnimationFrame(animationFrameRef.current);
      }
    };
  }, []);

  return {
    start,
    pause,
    resume,
    skipToEnd,
    isTyping: !isPausedRef.current && !isCompleteRef.current,
    isComplete: isCompleteRef.current,
    displayedText: displayedTextRef.current,
  };
}
