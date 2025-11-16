/**
 * Terminal Component
 * SSH终端组件，基于xterm.js实现
 *
 * 移植自: src/main/resources/templates/terminal/manager.html
 * 核心功能: 终端渲染、输入处理、WebSocket通信
 */

import { useEffect, useRef, forwardRef, useImperativeHandle } from 'react';
import { Terminal as XTerm } from '@xterm/xterm';
import { FitAddon } from '@xterm/addon-fit';
import '@xterm/xterm/css/xterm.css';
import type { TerminalConfig, TerminalTheme } from '../../../shared/types';
import styles from './Terminal.module.css';

/**
 * 终端组件属性
 */
export interface TerminalProps {
  /** 终端配置 */
  config?: Partial<TerminalConfig>;
  /** 自定义主题 */
  theme?: TerminalTheme;
  /** 数据输入回调 */
  onData?: (data: string) => void;
  /** 终端大小变化回调 */
  onResize?: (cols: number, rows: number) => void;
  /** 终端就绪回调 */
  onReady?: () => void;
  /** 是否自动聚焦 */
  autoFocus?: boolean;
  /** CSS类名 */
  className?: string;
}

/**
 * 终端实例暴露的方法
 */
export interface TerminalHandle {
  /** 写入数据到终端 */
  write: (data: string) => void;
  /** 清空终端 */
  clear: () => void;
  /** 聚焦终端 */
  focus: () => void;
  /** 调整终端大小 */
  fit: () => void;
  /** 获取终端尺寸 */
  getSize: () => { cols: number; rows: number };
  /** 获取xterm实例 (高级用法) */
  getXTerm: () => XTerm | null;
}

/**
 * 默认终端配置 (移植自manager.html line 616-632)
 */
const DEFAULT_CONFIG: Partial<TerminalConfig> = {
  fontSize: 14,
  fontFamily: 'Cascadia Code, Consolas, "Courier New", monospace',
  cursorBlink: true,
  cursorStyle: 'block',
  scrollback: 50000,
  bellStyle: 'none',
};

/**
 * VSCode暗色主题 (移植自manager.html line 617-622)
 */
const DEFAULT_THEME: TerminalTheme = {
  foreground: '#d4d4d4',
  background: '#1e1e1e',
  cursor: '#d4d4d4',
  cursorAccent: '#d4d4d4',
  selection: 'rgba(255, 255, 255, 0.3)',
  black: '#000000',
  red: '#cd3131',
  green: '#0dbc79',
  yellow: '#e5e510',
  blue: '#2472c8',
  magenta: '#bc3fbc',
  cyan: '#11a8cd',
  white: '#e5e5e5',
  brightBlack: '#666666',
  brightRed: '#f14c4c',
  brightGreen: '#23d18b',
  brightYellow: '#f5f543',
  brightBlue: '#3b8eea',
  brightMagenta: '#d670d6',
  brightCyan: '#29b8db',
  brightWhite: '#e5e5e5',
};

/**
 * Terminal组件
 *
 * 使用示例:
 * ```tsx
 * const terminalRef = useRef<TerminalHandle>(null);
 *
 * <Terminal
 *   ref={terminalRef}
 *   onData={(data) => sendToServer(data)}
 *   onReady={() => console.log('Terminal ready')}
 * />
 * ```
 */
export const Terminal = forwardRef<TerminalHandle, TerminalProps>(
  (
    {
      config = {},
      theme,
      onData,
      onResize,
      onReady,
      autoFocus = true,
      className = '',
    },
    ref
  ) => {
    const containerRef = useRef<HTMLDivElement>(null);
    const xtermRef = useRef<XTerm | null>(null);
    const fitAddonRef = useRef<FitAddon | null>(null);
    const resizeObserverRef = useRef<ResizeObserver | null>(null);

    // 合并配置
    const finalConfig = { ...DEFAULT_CONFIG, ...config };
    const finalTheme = theme || DEFAULT_THEME;

    // 暴露方法给父组件
    useImperativeHandle(ref, () => ({
      write: (data: string) => {
        xtermRef.current?.write(data);
      },
      clear: () => {
        xtermRef.current?.clear();
      },
      focus: () => {
        xtermRef.current?.focus();
      },
      fit: () => {
        if (fitAddonRef.current && xtermRef.current) {
          try {
            fitAddonRef.current.fit();
          } catch (error) {
            console.error('Error fitting terminal:', error);
          }
        }
      },
      getSize: () => {
        const xterm = xtermRef.current;
        return {
          cols: xterm?.cols || 80,
          rows: xterm?.rows || 24,
        };
      },
      getXTerm: () => xtermRef.current,
    }));

    useEffect(() => {
      if (!containerRef.current) return;

      // 创建xterm实例 (移植自manager.html line 616-632)
      const xterm = new XTerm({
        theme: finalTheme,
        fontSize: finalConfig.fontSize,
        fontFamily: finalConfig.fontFamily,
        cursorBlink: finalConfig.cursorBlink,
        cursorStyle: finalConfig.cursorStyle,
        scrollback: finalConfig.scrollback,
        convertEol: true,
        allowTransparency: false,
        rightClickSelectsWord: true,
        windowsMode: false,
      });

      // 挂载终端到DOM (移植自manager.html line 635)
      try {
        xterm.open(containerRef.current);
        console.log('Terminal opened successfully');
      } catch (error) {
        console.error('Error opening terminal:', error);
        return;
      }

      // 创建FitAddon (移植自manager.html line 644-645)
      const fitAddon = new FitAddon();
      xterm.loadAddon(fitAddon);

      // 监听输入事件 (移植自manager.html line 648-664)
      const dataDisposable = xterm.onData((data) => {
        // 重要: 移除本地回显，让服务器处理 (line 649-650)
        // xterm.write(data); // 不要本地回显

        // 发送到服务器
        if (onData) {
          onData(data);
        }
      });

      // 监听大小变化 (移植自manager.html line 701-702)
      const resizeDisposable = xterm.onResize(({ cols, rows }) => {
        if (onResize) {
          onResize(cols, rows);
        }
      });

      // 存储引用
      xtermRef.current = xterm;
      fitAddonRef.current = fitAddon;

      // 初始化完成后调整大小并聚焦 (移植自manager.html line 676-699)
      setTimeout(() => {
        try {
          fitAddon.fit();
          if (autoFocus) {
            xterm.focus();

            // 强制聚焦到输入区域 (移植自line 688-693)
            const textarea = containerRef.current?.querySelector(
              '.xterm-helper-textarea'
            ) as HTMLTextAreaElement;
            if (textarea) {
              textarea.focus();
              console.log('Focused on textarea');
            }
          }

          console.log('Terminal setup complete');

          // 触发就绪回调
          if (onReady) {
            onReady();
          }
        } catch (error) {
          console.error('Error in terminal setup:', error);
        }
      }, 200);

      // 设置ResizeObserver监听容器大小变化
      resizeObserverRef.current = new ResizeObserver(() => {
        if (fitAddonRef.current && xtermRef.current) {
          try {
            fitAddonRef.current.fit();
          } catch (error) {
            console.error('Error fitting terminal on resize:', error);
          }
        }
      });

      resizeObserverRef.current.observe(containerRef.current);

      // 清理函数
      return () => {
        dataDisposable.dispose();
        resizeDisposable.dispose();
        resizeObserverRef.current?.disconnect();
        xterm.dispose();
        xtermRef.current = null;
        fitAddonRef.current = null;
      };
    }, [
      finalConfig.fontSize,
      finalConfig.fontFamily,
      finalConfig.cursorBlink,
      finalConfig.cursorStyle,
      finalConfig.scrollback,
      autoFocus,
      onData,
      onResize,
      onReady,
    ]);

    // 主题变化时更新
    useEffect(() => {
      if (xtermRef.current && finalTheme) {
        xtermRef.current.options.theme = finalTheme;
      }
    }, [finalTheme]);

    return (
      <div className={`${styles.terminalWrapper} ${className}`}>
        <div ref={containerRef} className={styles.terminalContainer} />
      </div>
    );
  }
);

Terminal.displayName = 'Terminal';
